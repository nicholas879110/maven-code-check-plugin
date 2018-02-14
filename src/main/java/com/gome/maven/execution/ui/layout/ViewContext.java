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

package com.gome.maven.execution.ui.layout;

import com.gome.maven.execution.ui.RunnerLayoutUi;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.DataKey;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.ActionCallback;
import com.gome.maven.openapi.util.ActiveRunnable;
import com.gome.maven.openapi.wm.IdeFocusManager;
import com.gome.maven.ui.content.Content;
import com.gome.maven.ui.content.ContentManager;

public interface ViewContext extends Disposable {

    DataKey<Content[]> CONTENT_KEY = DataKey.create("runnerContents");
    DataKey<ViewContext> CONTEXT_KEY = DataKey.create("runnerUiContext");

    String CELL_TOOLBAR_PLACE = "debuggerCellToolbar";
    String TAB_TOOLBAR_PLACE = "debuggerTabToolbar";

    String CELL_POPUP_PLACE = "debuggerCellPopup";
    String TAB_POPUP_PLACE = "debuggerTabPopup";

    CellTransform.Facade getCellTransform();

    
    Tab getTabFor(final Grid grid);

    View getStateFor( Content content);

    void saveUiState();

    Project getProject();

    ContentManager getContentManager();

    
    ActionManager getActionManager();

    IdeFocusManager getFocusManager();

    RunnerLayoutUi getRunnerLayoutUi();

    GridCell findCellFor( final Content content);

    Grid findGridFor( Content content);

    ActionCallback select(Content content, boolean requestFocus);

    boolean isStateBeingRestored();

    void setStateIsBeingRestored(boolean state, final Object requestor);

    void validate(Content content, ActiveRunnable toRestore);

    void restoreLayout();

    boolean isMinimizeActionEnabled();

    boolean isMoveToGridActionEnabled();

    boolean isToDisposeRemovedContent();
}
