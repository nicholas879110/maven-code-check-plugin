/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.usages;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.usageView.UsageInfo;

import javax.swing.*;
import java.util.List;

/**
 * Component showing additional information for the selected usage in the Usage View.
 * Examples: Preview, Data flow, Call hierarchy
 */
public interface UsageContextPanel extends Disposable {
    // usage selection changes, panel should update its view for the newly select usages
    void updateLayout(List<UsageInfo> infos);

    
    JComponent createComponent();

    interface Provider {
        ExtensionPointName<Provider> EP_NAME = ExtensionPointName.create("com.intellij.usageContextPanelProvider");

        
        UsageContextPanel create( UsageView usageView);

        /**
         * E.g. Call hierarchy is not available for variable usages
         */
        boolean isAvailableFor( UsageView usageView);

        
        String getTabTitle();
    }
}