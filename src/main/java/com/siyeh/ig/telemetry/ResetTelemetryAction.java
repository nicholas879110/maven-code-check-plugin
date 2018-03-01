/*
 * Copyright 2003-2010 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.telemetry;

import com.gome.maven.CommonBundle;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.siyeh.InspectionGadgetsBundle;

class ResetTelemetryAction extends AnAction {

    private final InspectionGadgetsTelemetry telemetry;
    private final TelemetryDisplay display;

    ResetTelemetryAction(InspectionGadgetsTelemetry telemetry,
                         TelemetryDisplay display) {
        super(CommonBundle.message("button.reset"),
                InspectionGadgetsBundle.message(
                        "action.reset.telemetry.description"),
                AllIcons.Actions.Reset);
        this.telemetry = telemetry;
        this.display = display;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        telemetry.reset();
        display.update(telemetry.buildList());
    }
}