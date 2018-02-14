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
package com.gome.maven.openapi.command;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;


public abstract class CommandProcessor {
    public static CommandProcessor getInstance() {
        return ServiceManager.getService(CommandProcessor.class);
    }

    /**
     * @deprecated use {@link #executeCommand(com.gome.maven.openapi.project.Project, java.lang.Runnable, java.lang.String, java.lang.Object)}
     */
    public abstract void executeCommand( Runnable runnable,
                                         String name,
                                         Object groupId);

    public abstract void executeCommand( Project project,
                                         Runnable runnable,
                                         String name,
                                         Object groupId);

    public abstract void executeCommand( Project project,
                                         Runnable runnable,
                                         String name,
                                         Object groupId,
                                         Document document);

    public abstract void executeCommand( Project project,
                                         Runnable runnable,
                                         String name,
                                         Object groupId,
                                         UndoConfirmationPolicy confirmationPolicy);

    public abstract void executeCommand( Project project,
                                         Runnable command,
                                         String name,
                                         Object groupId,
                                         UndoConfirmationPolicy confirmationPolicy,
                                         Document document);

    public abstract void setCurrentCommandName( String name);

    public abstract void setCurrentCommandGroupId( Object groupId);

    
    public abstract Runnable getCurrentCommand();

    
    public abstract String getCurrentCommandName();

    
    public abstract Object getCurrentCommandGroupId();

    
    public abstract Project getCurrentCommandProject();

    public abstract void runUndoTransparentAction( Runnable action);

    public abstract boolean isUndoTransparentActionInProgress();

    public abstract void markCurrentCommandAsGlobal( Project project);

    public abstract void addAffectedDocuments( Project project,  Document... docs);

    public abstract void addAffectedFiles( Project project,  VirtualFile... files);

    public abstract void addCommandListener( CommandListener listener);

    public abstract void addCommandListener( CommandListener listener,  Disposable parentDisposable);

    public abstract void removeCommandListener( CommandListener listener);
}
