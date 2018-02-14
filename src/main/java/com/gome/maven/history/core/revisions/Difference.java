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

package com.gome.maven.history.core.revisions;

import com.gome.maven.history.core.tree.Entry;
import com.gome.maven.history.integration.IdeaGateway;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;

import java.io.File;

public class Difference {
    private final boolean myIsFile;
    private final Entry myLeft;
    private final Entry myRight;

    public Difference(boolean isFile, Entry left, Entry right) {
        myIsFile = isFile;
        myLeft = left;
        myRight = right;
    }

    public boolean isFile() {
        return myIsFile;
    }

    public Entry getLeft() {
        return myLeft;
    }

    public Entry getRight() {
        return myRight;
    }

    public ContentRevision getLeftContentRevision(IdeaGateway gw) {
        return createContentRevision(getLeft(), gw);
    }

    public ContentRevision getRightContentRevision(IdeaGateway gw) {
        return createContentRevision(getRight(), gw);
    }

    private ContentRevision createContentRevision(final Entry e, final IdeaGateway gw) {
        if (e == null) return null;

        return new ContentRevision() {
            
            public String getContent() throws VcsException {
                if (e.isDirectory()) return null;
                return e.getContent().getString(e, gw);
            }

            
            public FilePath getFile() {
                return new FilePathImpl(new File(e.getPath()), e.isDirectory());
            }

            
            public VcsRevisionNumber getRevisionNumber() {
                return VcsRevisionNumber.NULL;
            }
        };
    }
}
