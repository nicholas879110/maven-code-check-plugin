/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.daemon;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.messages.Topic;

/**
 * Manages the background highlighting and auto-import for files displayed in editors.
 */
public abstract class DaemonCodeAnalyzer {
    public static DaemonCodeAnalyzer getInstance(Project project) {
        return project.getComponent(DaemonCodeAnalyzer.class);
    }

    public abstract void settingsChanged();

    @Deprecated
    public abstract void updateVisibleHighlighters( Editor editor);

    public abstract void setUpdateByTimerEnabled(boolean value);
    public abstract void disableUpdateByTimer( Disposable parentDisposable);

    public abstract boolean isHighlightingAvailable( PsiFile file);

    public abstract void setImportHintsEnabled( PsiFile file, boolean value);
    public abstract void resetImportHintsEnabledForProject();
    public abstract void setHighlightingEnabled( PsiFile file, boolean value);
    public abstract boolean isImportHintsEnabled( PsiFile file);
    public abstract boolean isAutohintsAvailable( PsiFile file);

    /**
     * Force rehighlighting for all files.
     */
    public abstract void restart();

    /**
     * Force rehighlighting for a specific file.
     * @param file the file to rehighlight.
     */
    public abstract void restart( PsiFile file);

    public abstract void autoImportReferenceAtCursor( Editor editor,  PsiFile file);

    public static final Topic<DaemonListener> DAEMON_EVENT_TOPIC = Topic.create("DAEMON_EVENT_TOPIC", DaemonListener.class);

    public interface DaemonListener {
        void daemonFinished();
        void daemonCancelEventOccurred();
    }
    public abstract static class DaemonListenerAdapter implements DaemonListener {
        @Override public void daemonFinished() {}
        @Override public void daemonCancelEventOccurred() {}
    }
}
