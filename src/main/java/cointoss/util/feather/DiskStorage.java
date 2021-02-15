/*
 * Copyright (C) 2021 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package cointoss.util.feather;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.SPARSE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.LocalDate;
import java.util.concurrent.locks.StampedLock;

import kiss.I;
import psychopath.File;

class DiskStorage<T> {

    /** The header size. */
    private static final int HEADER_SIZE = 0 //
            + 8 // offset time;
            + 8 // start time
            + 8 // end time
            + 104; // reserved space, use in future

    /** The item prefix. */
    private static final byte ITEM_UNDEFINED = 0;

    /** The item prefix. */
    private static final byte ITEM_DEFINED = 1;

    /** The actual file. */
    final File file;

    /** The actual channel. */
    private FileChannel channel;

    /** The process lock. */
    private final FileLock lockForProcess;

    /** The read-write lock. */
    private final StampedLock lock = new StampedLock();

    /** The data definition. */
    private final DataType<T> codec;

    /** The total byte size for each items. */
    private final int itemWidth;

    /** The time that one element has. */
    private final long duration;

    /** The flag. */
    private boolean headerModified;

    /** The logical starting point. (epoch seconds) */
    private long offsetTime;

    /** The start time of all records. (included) */
    private long startTime;

    /** The end time of all records. (excluded) */
    private long endTime;

    /**
     * Create disk storage.
     * 
     * @param databaseFile The actual storage file.
     * @param codec The data definition.
     * @param duration The time that one element has.
     */
    DiskStorage(File databaseFile, DataType<T> codec, long duration) {
        try {
            this.file = databaseFile;
            this.channel = databaseFile.isPresent() ? databaseFile.newFileChannel(READ, WRITE)
                    : databaseFile.newFileChannel(CREATE_NEW, SPARSE, READ, WRITE);
            this.lockForProcess = databaseFile.extension("lock").newFileChannel(CREATE, READ, WRITE).tryLock();
            this.codec = codec;
            this.itemWidth = codec.size() + 1;
            this.duration = duration;

            if (HEADER_SIZE < channel.size()) {
                readHeader();
            } else {
                offsetTime = Long.MAX_VALUE;
                startTime = Long.MAX_VALUE;
                endTime = 0;
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Close all related resources.
     */
    void close() {
        try {
            channel.close();
            if (lockForProcess != null) {
                lockForProcess.close();
                file.extension("lock").delete();
            }
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Parse header.
     */
    private void readHeader() {
        try {
            // read
            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
            channel.read(buffer, 0);
            buffer.flip();

            // decode
            offsetTime = buffer.getLong();
            startTime = buffer.getLong();
            endTime = buffer.getLong();
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Update header.
     * 
     * @throws IOException
     */
    private void writeHeader() {
        if (lockForProcess == null) {
            // The current process does not have write permission because it is being used by
            // another process.
            return;
        }

        if (headerModified) {
            try {
                channel.force(true);
                // encode
                ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
                buffer.putLong(offsetTime);
                buffer.putLong(startTime);
                buffer.putLong(endTime);

                // write
                buffer.flip();
                channel.write(buffer, 0);

                // update stataus
                headerModified = false;
            } catch (IOException e) {
                throw I.quiet(e);
            }
        }
    }

    /**
     * Update time information in header.
     * 
     * @param startTime A new starting time.
     * @param endTime A new ending time.
     */
    private void updateTime(long startTime, long endTime) {
        if (startTime < this.startTime) {
            this.startTime = startTime;
            headerModified = true;
        }

        if (this.endTime < endTime) {
            this.endTime = endTime;
            headerModified = true;
        }
    }

    private void updateOffsetTime(long candidate) {
        long truncated = LocalDate.ofEpochDay(candidate / (60 * 60 * 24)).withDayOfMonth(1).toEpochDay() * 60 * 60 * 24;

        if (offsetTime < truncated) {
            return;
        }

        // If none of the data exists yet, it will initialize the specified time as the offset time.
        if (offsetTime == Long.MAX_VALUE) {
            writeOffsetTime(truncated);
            return;
        }

        // Rebuild the database because a time earlier than the current offset time has been
        // specified.
        replaceBy(rebuild(truncated));
    }

    /**
     * Write the new offset time to file actually.
     * 
     * @param time
     */
    private void writeOffsetTime(long time) {
        try {
            channel.write(ByteBuffer.allocate(8).putLong(time), 0);
            offsetTime = time;
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    /**
     * Read data from disk cache.
     * 
     * @param truncatedTime
     */
    int read(long truncatedTime, T[] items) {
        if (endTime == 0) {
            return 0;
        }

        long stamp = lock.readLock();

        try {
            ByteBuffer buffer = ByteBuffer.allocate(itemWidth * items.length);
            int size = channel.read(buffer, HEADER_SIZE + (truncatedTime - offsetTime) / duration * itemWidth);
            if (size == -1) {
                return 0;
            }
            buffer.flip();

            int readableItemSize = size / itemWidth;
            int skip = 0;
            for (int i = 0; i < readableItemSize; i++) {
                if (buffer.get() == ITEM_UNDEFINED) {
                    buffer.position(buffer.position() + itemWidth - 1);
                    skip++;
                } else {
                    items[i] = codec.read(buffer);
                }
            }
            return readableItemSize - skip;
        } catch (IOException e) {
            throw I.quiet(e);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Write data to disk cache.
     * 
     * @param truncatedTime
     * @param segment
     */
    void write(long truncatedTime, T... items) {
        if (lockForProcess == null) {
            // The current process does not have write permission because it is being used by
            // another process.
            return;
        }

        long stamp = lock.writeLock();

        try {
            if (truncatedTime < offsetTime) {
                updateOffsetTime(truncatedTime);
            }

            long startPosition = HEADER_SIZE + (truncatedTime - offsetTime) / duration * itemWidth;
            ByteBuffer buffer = ByteBuffer.allocate(itemWidth * items.length);

            for (int i = 0; i < items.length; i++) {
                T item = items[i];
                if (item == null) {
                    if (buffer.position() != 0) {
                        buffer.flip();
                        channel.write(buffer, startPosition);
                        buffer.clear();
                    }
                    startPosition = HEADER_SIZE + ((truncatedTime - offsetTime) / duration + i + 1) * itemWidth;
                } else {
                    buffer.put(ITEM_DEFINED);
                    codec.write(item, buffer);
                }
            }

            if (buffer.position() != 0) {
                buffer.flip();
                channel.write(buffer, startPosition);
            }
        } catch (IOException e) {
            throw I.quiet(e);
        } finally {
            updateTime(truncatedTime, truncatedTime + items.length * duration);
            writeHeader();

            lock.unlockWrite(stamp);
        }
    }

    /**
     * Get the offset time.
     * 
     * @return
     */
    final long offsetTime() {
        return offsetTime;
    }

    /**
     * Get the starting time. (included)
     * 
     * @return
     */
    final long startTime() {
        return startTime;
    }

    /**
     * Get the ending time. (excluded)
     * 
     * @return
     */
    final long endTime() {
        return endTime;
    }

    /**
     * Returns a fully reconstructed database for a given offset time.
     * 
     * @param newOffsetTime
     * @return
     */
    DiskStorage<T> rebuild(long newOffsetTime) {
        if (lockForProcess == null) {
            // The current process does not have write permission because it is being used by
            // another process.
            return this;
        }

        try {
            DiskStorage rebuild = new DiskStorage(file.base(file.base() + "-rebuild"), codec, duration);

            // copy file header
            ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
            channel.read(header, 0);
            header.flip();
            rebuild.channel.write(header, 0);
            rebuild.writeOffsetTime(newOffsetTime); // rewrite offset time

            // copy object header
            rebuild.startTime = startTime;
            rebuild.endTime = endTime;
            rebuild.offsetTime = newOffsetTime;

            // copy file data
            int maxItemSize = 1024;
            int actualReadSize = 0;
            ByteBuffer data = ByteBuffer.allocate(itemWidth * maxItemSize);

            long inputPosition = HEADER_SIZE + (startTime - offsetTime) / duration * itemWidth;
            long outputPosition = inputPosition + (offsetTime - newOffsetTime) / duration * itemWidth;

            while ((actualReadSize = channel.read(data, inputPosition)) != -1) {
                data.flip();

                int readableItemSize = actualReadSize / itemWidth;
                for (int i = 0; i < readableItemSize; i++) {
                    int p = i * itemWidth;
                    if (data.get(p) != ITEM_UNDEFINED) {
                        rebuild.channel.write(data.slice(p, itemWidth), outputPosition);
                    }
                    outputPosition += itemWidth;
                }
                inputPosition += actualReadSize;
                data.clear();
            }

            return rebuild;
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }

    void replaceBy(DiskStorage storage) {
        // copy database file
        storage.file.renameTo(file.name(), REPLACE_EXISTING);

        // copy object header
        offsetTime = storage.offsetTime;
        startTime = storage.startTime;
        endTime = storage.endTime;

        // re-open
        try {
            storage.close();

            channel.close();
            channel = file.newFileChannel(READ, WRITE);
        } catch (IOException e) {
            throw I.quiet(e);
        }
    }
}