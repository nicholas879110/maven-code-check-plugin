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
package com.gome.maven.openapi.vfs;

import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;

import java.util.Collection;

/**
 * @author Dmitry Avdeev
 */
public abstract class WritingAccessProvider {

    public static final ExtensionPointName<WritingAccessProvider> EP_NAME = ExtensionPointName.create("com.gome.maven.writingAccessProvider");

    /**
     * @param files files to be checked
     * @return set of files that cannot be accessed
     */
    
    public abstract Collection<VirtualFile> requestWriting(VirtualFile... files);

    public abstract boolean isPotentiallyWritable( VirtualFile file);

    public static WritingAccessProvider[] getProvidersForProject(Project project) {
        return project == null || project.isDefault() ? new WritingAccessProvider[0] : Extensions.getExtensions(EP_NAME, project);
    }

    public static boolean isPotentiallyWritable(VirtualFile file, Project project) {
        WritingAccessProvider[] providers = getProvidersForProject(project);
        for (WritingAccessProvider provider : providers) {
            if (!provider.isPotentiallyWritable(file)) {
                return false;
            }
        }
        return true;
    }
}
