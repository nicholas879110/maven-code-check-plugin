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
package com.gome.maven.openapi.vcs.changes.patch;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diff.impl.patch.TextFilePatch;
import com.gome.maven.openapi.diff.impl.patch.apply.GenericPatchApplier;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;
import com.gome.maven.openapi.vfs.VirtualFile;

public class LazyPatchContentRevision implements ContentRevision {
    private volatile String myContent;
    private final VirtualFile myVf;
    private final FilePath myNewFilePath;
    private final String myRevision;
    private final TextFilePatch myPatch;
    private volatile boolean myPatchApplyFailed;

    public LazyPatchContentRevision(final VirtualFile vf, final FilePath newFilePath, final String revision, final TextFilePatch patch) {
        myVf = vf;
        myNewFilePath = newFilePath;
        myRevision = revision;
        myPatch = patch;
    }

    public String getContent() {
        if (myContent == null) {
            final String localContext = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                @Override
                public String compute() {
                    final Document doc = FileDocumentManager.getInstance().getDocument(myVf);
                    return doc == null ? null : doc.getText();
                }
            });
            if (localContext == null) {
                myPatchApplyFailed = true;
                return null;
            }

            final GenericPatchApplier applier = new GenericPatchApplier(localContext, myPatch.getHunks());
            if (applier.execute()) {
                myContent = applier.getAfter();
            } else {
                myPatchApplyFailed = true;
            }
        }
        return myContent;
    }

    public boolean isPatchApplyFailed() {
        return myPatchApplyFailed;
    }

    
    public FilePath getFile() {
        return myNewFilePath;
    }

    
    public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber() {
            public String asString() {
                return myRevision;
            }

            public int compareTo(final VcsRevisionNumber o) {
                return 0;
            }
        };
    }
}
