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
package com.gome.maven.openapi.vcs;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

import java.awt.*;

/**
 * @author mike
 */
public abstract class FileStatusManager {
    public static FileStatusManager getInstance(Project project) {
        return project.getComponent(FileStatusManager.class);
    }

    public abstract FileStatus getStatus( VirtualFile virtualFile);

    public abstract void fileStatusesChanged();
    public abstract void fileStatusChanged(VirtualFile file);

    public abstract void addFileStatusListener( FileStatusListener listener);
    public abstract void addFileStatusListener( FileStatusListener listener,  Disposable parentDisposable);
    public abstract void removeFileStatusListener( FileStatusListener listener);

    /**
     * @deprecated Use getStatus(file).getText()} instead
     */
    public String getStatusText( VirtualFile file){
        return getStatus(file).getText();
    }

    /**
     * @deprecated Use getStatus(file).getColor()} instead
     */
    public Color getStatusColor( VirtualFile file){
        return getStatus(file).getColor();
    }

    public abstract Color getNotChangedDirectoryColor( VirtualFile vf);
}
