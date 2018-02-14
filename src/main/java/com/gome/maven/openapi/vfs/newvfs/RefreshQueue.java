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
package com.gome.maven.openapi.vfs.newvfs;

import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;


import java.util.Collection;

/**
 * @author max
 */
public abstract class RefreshQueue {
    public static RefreshQueue getInstance() {
        return ServiceManager.getService(RefreshQueue.class);
    }

    
    public final RefreshSession createSession(boolean async, boolean recursive,  Runnable finishRunnable) {
        return createSession(async, recursive, finishRunnable, getDefaultModalityState());
    }

    
    public abstract RefreshSession createSession(boolean async, boolean recursive,  Runnable finishRunnable,  ModalityState state);

    public final void refresh(boolean async, boolean recursive,  Runnable finishRunnable,  VirtualFile... files) {
        refresh(async, recursive, finishRunnable, getDefaultModalityState(), files);
    }

    public final void refresh(boolean async, boolean recursive,  Runnable finishRunnable,  Collection<VirtualFile> files) {
        refresh(async, recursive, finishRunnable, getDefaultModalityState(), files);
    }

    public final void refresh(boolean async,
                              boolean recursive,
                               Runnable finishRunnable,
                               ModalityState state,
                               VirtualFile... files) {
        RefreshSession session = createSession(async, recursive, finishRunnable, state);
        session.addAllFiles(files);
        session.launch();
    }

    public final void refresh(boolean async,
                              boolean recursive,
                               Runnable finishRunnable,
                               ModalityState state,
                               Collection<VirtualFile> files) {
        RefreshSession session = createSession(async, recursive, finishRunnable, state);
        session.addAllFiles(files);
        session.launch();
    }

    public abstract void processSingleEvent( VFileEvent event);

    public abstract void cancelSession(long id);

    
    protected ModalityState getDefaultModalityState() {
        return ModalityState.NON_MODAL;
    }
}
