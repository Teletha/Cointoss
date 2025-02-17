package cointoss.order;

import cointoss.Direction;
import cointoss.order.Position;
import hypatia.Num;
import java.lang.Override;
import java.lang.StringBuilder;
import java.lang.Throwable;
import java.lang.UnsupportedOperationException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Generated model for {@link PositionModel}.
 * 
 * @see <a href="https://github.com/teletha/icymanipulator">Icy Manipulator (Code Generator)</a>
 */
public class Position extends PositionModel {

    /**
     * Deceive complier that the specified checked exception is unchecked exception.
     *
     * @param <T> A dummy type for {@link RuntimeException}.
     * @param throwable Any error.
     * @return A runtime error.
     * @throws T Dummy error to deceive compiler.
     */
    private static final <T extends Throwable> T quiet(Throwable throwable) throws T {
        throw (T) throwable;
    }

    /**
     * Create special property updater.
     *
     * @param name A target property name.
     * @return A special property updater.
     */
    private static final Field updater(String name)  {
        try {
            Field field = Position.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Create fast property updater.
     *
     * @param field A target field.
     * @return A fast property updater.
     */
    private static final MethodHandle handler(Field field)  {
        try {
            return MethodHandles.lookup().unreflectSetter(field);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /** The final property updater. */
    private static final Field orientationField = updater("orientation");

    /** The fast final property updater. */
    private static final MethodHandle orientationUpdater = handler(orientationField);

    /** The final property updater. */
    private static final Field priceField = updater("price");

    /** The fast final property updater. */
    private static final MethodHandle priceUpdater = handler(priceField);

    /** The final property updater. */
    private static final Field sizeField = updater("size");

    /** The fast final property updater. */
    private static final MethodHandle sizeUpdater = handler(sizeField);

    /** The final property updater. */
    private static final Field dateField = updater("date");

    /** The fast final property updater. */
    private static final MethodHandle dateUpdater = handler(dateField);

    /** The exposed property. */
    public final Direction orientation;

    /** The exposed property. */
    public final Num price;

    /** The exposed property. */
    public final Num size;

    /** The exposed property. */
    public final ZonedDateTime date;

    /**
     * HIDE CONSTRUCTOR
     */
    protected Position() {
        this.orientation = null;
        this.price = null;
        this.size = null;
        this.date = null;
    }

    /**
     * Return the orientation property.
     *
     * @return A value of orientation property.
     */
    @Override
    public final Direction orientation() {
        return this.orientation;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of orientation property.
     */
    @SuppressWarnings("unused")
    private final Direction getOrientation() {
        return this.orientation;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of orientation property to assign.
     */
    private final void setOrientation(Direction value) {
        if (value == null) {
            throw new IllegalArgumentException("The orientation property requires non-null value.");
        }
        try {
            orientationUpdater.invoke(this, value);
        } catch (UnsupportedOperationException e) {
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Return the price property.
     *
     * @return A value of price property.
     */
    @Override
    public final Num price() {
        return this.price;
    }

    /**
     * Assign the new value of price property.
     *
     * @paran value The new price property value to assign.
     * @return Chainable API.
     */
    public final Position assignPrice(Num value) {
        setPrice(value);
        return this;
    }

    /**
     * Assign the new value of price property.
     *
     * @paran value The price property assigner which accepts the current value and returns new value.
     * @return Chainable API.
     */
    public final Position assignPrice(UnaryOperator<Num> value) {
        setPrice(value.apply(this.price));
        return this;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of price property.
     */
    @SuppressWarnings("unused")
    private final Num getPrice() {
        return this.price;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of price property to assign.
     */
    private final void setPrice(Num value) {
        if (value == null) {
            throw new IllegalArgumentException("The price property requires non-null value.");
        }
        try {
            priceUpdater.invoke(this, value);
        } catch (UnsupportedOperationException e) {
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Return the size property.
     *
     * @return A value of size property.
     */
    @Override
    public final Num size() {
        return this.size;
    }

    /**
     * Assign the new value of size property.
     *
     * @paran value The new size property value to assign.
     * @return Chainable API.
     */
    public final Position assignSize(Num value) {
        setSize(value);
        return this;
    }

    /**
     * Assign the new value of size property.
     *
     * @paran value The size property assigner which accepts the current value and returns new value.
     * @return Chainable API.
     */
    public final Position assignSize(UnaryOperator<Num> value) {
        setSize(value.apply(this.size));
        return this;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of size property.
     */
    @SuppressWarnings("unused")
    private final Num getSize() {
        return this.size;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of size property to assign.
     */
    private final void setSize(Num value) {
        if (value == null) {
            throw new IllegalArgumentException("The size property requires non-null value.");
        }
        try {
            sizeUpdater.invoke(this, value);
        } catch (UnsupportedOperationException e) {
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Return the date property.
     *
     * @return A value of date property.
     */
    @Override
    public final ZonedDateTime date() {
        return this.date;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of date property.
     */
    @SuppressWarnings("unused")
    private final ZonedDateTime getDate() {
        return this.date;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of date property to assign.
     */
    private final void setDate(ZonedDateTime value) {
        if (value == null) {
            throw new IllegalArgumentException("The date property requires non-null value.");
        }
        try {
            dateUpdater.invoke(this, value);
        } catch (UnsupportedOperationException e) {
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Show all property values.
     *
     * @return All property values.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Position [");
        builder.append("orientation=").append(orientation).append(", ");
        builder.append("price=").append(price).append(", ");
        builder.append("size=").append(size).append(", ");
        builder.append("date=").append(date).append("]");
        return builder.toString();
    }

    /**
     * Generates a hash code for a sequence of property values. The hash code is generated as if all the property values were placed into an array, and that array were hashed by calling Arrays.hashCode(Object[]). 
     *
     * @return A hash value of the sequence of property values.
     */
    @Override
    public int hashCode() {
        return Objects.hash(orientation, price, size, date);
    }

    /**
     * Returns true if the all properties are equal to each other and false otherwise. Consequently, if both properties are null, true is returned and if exactly one property is null, false is returned. Otherwise, equality is determined by using the equals method of the base model. 
     *
     * @return true if the all properties are equal to each other and false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Position == false) {
            return false;
        }

        Position other = (Position) o;
        if (!Objects.equals(orientation, other.orientation)) return false;
        if (!Objects.equals(price, other.price)) return false;
        if (!Objects.equals(size, other.size)) return false;
        if (!Objects.equals(date, other.date)) return false;
        return true;
    }

    /** The singleton builder. */
    public static final  Ìnstantiator<?> with = new Ìnstantiator();

    /**
     * Namespace for {@link Position}  builder methods.
     */
    public static class Ìnstantiator<Self extends Position & ÅssignableÅrbitrary<Self>> {

        /**
         * Create new {@link Position} with the specified orientation property.
         * 
         * @return The next assignable model.
         */
        public ÅssignablePrice<ÅssignableSize<ÅssignableDate<Self>>> orientation(Direction orientation) {
            Åssignable o = new Åssignable();
            o.orientation(orientation);
            return o;
        }

        /**
         * Create new {@link Position} with the specified orientation property.
         * 
         * @return The next assignable model.
         */
        public ÅssignablePrice<ÅssignableSize<ÅssignableDate<Self>>> buy() {
            Åssignable o = new Åssignable();
            o.buy();
            return o;
        }

        /**
         * Create new {@link Position} with the specified orientation property.
         * 
         * @return The next assignable model.
         */
        public ÅssignablePrice<ÅssignableSize<ÅssignableDate<Self>>> sell() {
            Åssignable o = new Åssignable();
            o.sell();
            return o;
        }
    }

    /**
     * Property assignment API.
     */
    public static interface ÅssignableOrientation<Next> {

        /**
         * Assign orientation property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next orientation(Direction value) {
            ((Position) this).setOrientation(value);
            return (Next) this;
        }

        /**
         * Assign orientation property.
         * 
         * @return The next assignable model.
         */
        default Next buy() {
            return orientation(Direction.BUY);
        }

        /**
         * Assign orientation property.
         * 
         * @return The next assignable model.
         */
        default Next sell() {
            return orientation(Direction.SELL);
        }
    }

    /**
     * Property assignment API.
     */
    public static interface ÅssignablePrice<Next> {

        /**
         * Assign price property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next price(Num value) {
            ((Position) this).setPrice(value);
            return (Next) this;
        }
    }

    /**
     * Property assignment API.
     */
    public static interface ÅssignableSize<Next> {

        /**
         * Assign size property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next size(Num value) {
            ((Position) this).setSize(value);
            return (Next) this;
        }
    }

    /**
     * Property assignment API.
     */
    public static interface ÅssignableDate<Next> {

        /**
         * Assign date property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next date(ZonedDateTime value) {
            ((Position) this).setDate(value);
            return (Next) this;
        }
    }

    /**
     * Property assignment API.
     */
    public static interface ÅssignableÅrbitrary<Next extends Position> {
    }

    /**
     * Internal aggregated API.
     */
    protected static interface ÅssignableAll extends ÅssignableOrientation, ÅssignablePrice, ÅssignableSize, ÅssignableDate {
    }

    /**
     * Mutable Model.
     */
    private static final class Åssignable extends Position implements ÅssignableAll, ÅssignableÅrbitrary {
    }

    /**
     * The identifier for properties.
     */
    static final class My {
        static final String Orientation = "orientation";
        static final String Price = "price";
        static final String Size = "size";
        static final String Date = "date";
    }
}
