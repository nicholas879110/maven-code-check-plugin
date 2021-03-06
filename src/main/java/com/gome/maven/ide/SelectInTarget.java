/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.ide;

import com.gome.maven.openapi.extensions.ExtensionPointName;

public interface SelectInTarget {
    ExtensionPointName<SelectInTarget> EP_NAME = new ExtensionPointName<SelectInTarget>("com.gome.maven.selectInTarget");

    @Override
    String toString();

    /**
     * This should be called in an read action
     */
    boolean canSelect(SelectInContext context);

    void selectIn(SelectInContext context, final boolean requestFocus);

    /** Tool window this target is supposed to select in */
    
    String getToolWindowId();

    /** aux view id specific for tool window, e.g. Project/Packages/J2EE tab inside project View */
    
    String getMinorViewId();

    /**
     * Weight is used to provide an order in SelectIn popup. Lesser weights come first.
     * @return weight of this particular target.
     */
    float getWeight();
}
