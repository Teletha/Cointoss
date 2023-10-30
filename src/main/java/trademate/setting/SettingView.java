/*
 * Copyright (C) 2023 The COINTOSS Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package trademate.setting;

import kiss.Variable;
import stylist.Style;
import stylist.StyleDSL;
import viewtify.ui.UILabel;
import viewtify.ui.UISelectPane;
import viewtify.ui.View;
import viewtify.ui.ViewDSL;

public class SettingView extends View {

    private UILabel appearance;

    private UILabel chart;

    private UILabel notification;

    private UILabel bitflyer;

    private final Variable<View> main = Variable.empty();

    UISelectPane selection;

    /**
     * UI definition.
     */
    class view extends ViewDSL implements SettingStyles {
        {
            $(selection, style.select);
        }
    }

    /**
     * Style definition.
     */
    interface style extends StyleDSL {

        Style select = () -> {
            $.select(".select-buttons", () -> {
                padding.top(40, px).right(20, px);
            });

            $.select(".select-button", () -> {
                display.minWidth(200, px).height(20, px);
                padding.vertical(10, px).left(40, px);
                cursor.pointer();
                font.size(16, px);

                $.hover(() -> {
                    background.color("derive(-fx-base, 15%)");
                });

                $.with(".selected", () -> {
                    background.color("derive(-fx-base, 6%)");
                });
            });
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize() {
        selection.add(ui -> ui.text(en("Appearance")), AppearanceSetting.class)
                .add(ui -> ui.text(en("Chart")), ChartSetting.class)
                .add(ui -> ui.text(en("Notification")), NotificatorSetting.class)
                .add(ui -> ui.text(en("Bitflyer")), BitFlyerSetting.class)
                .selectAt(0);
    }
}