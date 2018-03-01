/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.diff;

import com.gome.maven.diff.requests.ContentDiffRequest;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

/*
 * Use ProgressManager.executeProcessUnderProgress() to pass modality state if needed
 */
public abstract class DiffRequestFactory {
    
    public static DiffRequestFactory getInstance() {
        return ServiceManager.getService(DiffRequestFactory.class);
    }

    
    public abstract ContentDiffRequest createFromFiles( Project project,  VirtualFile file1,  VirtualFile file2);

    
    public abstract ContentDiffRequest createClipboardVsValue( String value);


    
    public abstract String getContentTitle( VirtualFile file);

    
    public abstract String getTitle( VirtualFile file1,  VirtualFile file2);

    
    public abstract String getTitle( VirtualFile file);
}
