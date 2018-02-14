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

/*
 * @author: Eugene Zhuravlev
 * Date: Jan 20, 2003
 * Time: 5:34:19 PM
 */
package com.gome.maven.compiler.impl;

import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndex;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;

public class ProjectCompileScope extends FileIndexCompileScope {
    private final Project myProject;

    public ProjectCompileScope(final Project project) {
        myProject = project;
    }

    protected FileIndex[] getFileIndices() {
        return new FileIndex[] {ProjectRootManager.getInstance(myProject).getFileIndex()};
    }

    public boolean belongs(String url) {
        final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file != null) {
            for (FileIndex index : getFileIndices()) {
                if (index.isInSourceContent(file)) {
                    return true;
                }
            }
        }
        else {
            // the file might be deleted
            for (VirtualFile root : ProjectRootManager.getInstance(myProject).getContentSourceRoots()) {
                final String rootUrl = root.getUrl();
                if (FileUtil.startsWith(url, rootUrl.endsWith("/")? rootUrl : rootUrl + "/")) {
                    return true;
                }
            }
        }
        return false;
        //return !FileUtil.startsWith(url, myTempDirUrl);
    }


    public Module[] getAffectedModules() {
        return ModuleManager.getInstance(myProject).getModules();
    }
}
