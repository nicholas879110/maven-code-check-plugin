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
package com.gome.maven.openapi.wm.ex;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.wm.ToolWindowAnchor;
import com.gome.maven.openapi.wm.ToolWindowEP;
import com.gome.maven.openapi.wm.ToolWindowManager;
import com.gome.maven.openapi.wm.impl.DesktopLayout;

import javax.swing.*;
import java.util.List;

public abstract class ToolWindowManagerEx extends ToolWindowManager {
    public abstract void initToolWindow( ToolWindowEP bean);

    public static ToolWindowManagerEx getInstanceEx(final Project project){
        return (ToolWindowManagerEx)getInstance(project);
    }

    public abstract void addToolWindowManagerListener( ToolWindowManagerListener l);
    public abstract void addToolWindowManagerListener( ToolWindowManagerListener l,  Disposable parentDisposable);
    public abstract void removeToolWindowManagerListener( ToolWindowManagerListener l);

    /**
     * @return <code>ID</code> of tool window that was activated last time.
     */
    
    public abstract String getLastActiveToolWindowId();

    /**
     * @return <code>ID</code> of tool window which was last activated among tool windows satisfying the current condition
     */
    
    public abstract String getLastActiveToolWindowId( Condition<JComponent> condition);

    /**
     * @return layout of tool windows.
     */
    public abstract DesktopLayout getLayout();

    public abstract void setLayoutToRestoreLater(DesktopLayout layout);

    public abstract DesktopLayout getLayoutToRestoreLater();

    /**
     * Copied <code>layout</code> into internal layout and rearranges tool windows.
     */
    public abstract void setLayout( DesktopLayout layout);

    public abstract void clearSideStack();

    public abstract void hideToolWindow( String id, boolean hideSide);

    public abstract List<String> getIdsOn( ToolWindowAnchor anchor);
}
