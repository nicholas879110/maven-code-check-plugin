/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.ui.popup.mock;

import com.gome.maven.openapi.ui.popup.ListPopupStep;
import com.gome.maven.ui.popup.list.ListPopupImpl;

import java.awt.*;

/**
 * User: Sergey.Vasiliev
 * Date: Nov 21, 2004
 */
public class MockConfirmation extends ListPopupImpl {
    String myOnYesText;
    public MockConfirmation(ListPopupStep aStep, String onYesText) {
        super(aStep);
        myOnYesText = onYesText;
    }

    public void showInCenterOf( Component aContainer) {
        getStep().onChosen(myOnYesText, true);
    }

    public void showUnderneathOf( Component aComponent) {
        getStep().onChosen(myOnYesText, true);
    }
}
