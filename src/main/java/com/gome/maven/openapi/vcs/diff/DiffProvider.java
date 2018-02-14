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
package com.gome.maven.openapi.vcs.diff;

import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsProviderMarker;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vfs.VirtualFile;

public interface DiffProvider extends VcsProviderMarker {

    
    VcsRevisionNumber getCurrentRevision(VirtualFile file);

    
    ItemLatestState getLastRevision(VirtualFile virtualFile);

    
    ItemLatestState getLastRevision(final FilePath filePath);

    
    ContentRevision createFileContent(VcsRevisionNumber revisionNumber, VirtualFile selectedFile);

    
    VcsRevisionNumber getLatestCommittedRevision(VirtualFile vcsRoot);
}
