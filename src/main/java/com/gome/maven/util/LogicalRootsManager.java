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

package com.gome.maven.util;

import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.messages.Topic;

import java.util.EventListener;
import java.util.List;

/**
 * // TODO: merge with FileReferenceHelper & drop
 *
 * @author spleaner
 * @deprecated use {@link com.gome.maven.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper} instead
 */
@Deprecated
public abstract class LogicalRootsManager {

    public static final Topic<LogicalRootListener> LOGICAL_ROOTS = new Topic<LogicalRootListener>("logical root changes", LogicalRootListener.class);

    public static LogicalRootsManager getLogicalRootsManager( final Project project) {
        return ServiceManager.getService(project, LogicalRootsManager.class);
    }

    
    public abstract LogicalRoot findLogicalRoot( final VirtualFile file);

    public abstract List<LogicalRoot> getLogicalRoots();

    public abstract List<LogicalRoot> getLogicalRoots( final Module module);

    public abstract List<LogicalRoot> getLogicalRootsOfType( final Module module,  final LogicalRootType... types);

    public abstract <T extends LogicalRoot> List<T> getLogicalRootsOfType( final Module module,  final LogicalRootType<T> type);

    
    public abstract LogicalRootType[] getRootTypes( final FileType type);

    public abstract void registerRootType( final FileType fileType,  final LogicalRootType... rootTypes);

    public abstract <T extends LogicalRoot> void registerLogicalRootProvider( final LogicalRootType<T> rootType,  NotNullFunction<Module,List<T>> provider);

    public interface LogicalRootListener extends EventListener {
        void logicalRootsChanged();
    }
}
