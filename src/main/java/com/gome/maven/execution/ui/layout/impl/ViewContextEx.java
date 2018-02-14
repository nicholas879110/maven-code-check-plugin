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

package com.gome.maven.execution.ui.layout.impl;

import com.gome.maven.execution.ui.layout.GridCell;
import com.gome.maven.execution.ui.layout.ViewContext;
import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.util.ActionCallback;

public interface ViewContextEx extends ViewContext {
    RunnerLayout getLayoutSettings();

    ActionGroup getCellPopupGroup(String place);

    boolean isOriginal();

    int getWindow();

    ActionCallback detachTo(int window, GridCell cell);
}