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
package com.gome.maven.openapi.vcs.changes;

import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.actions.VcsContextFactory;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;

import java.io.File;

public class FakeRevision implements ContentRevision {
    private final FilePath myFile;

    public FakeRevision(String path) throws ChangeListManagerSerialization.OutdatedFakeRevisionException {
        final FilePath file = VcsContextFactory.SERVICE.getInstance().createFilePathOn(new File(path));
        if (file == null) throw new ChangeListManagerSerialization.OutdatedFakeRevisionException();
        myFile = file;
    }

    
    public String getContent() { return null; }

    
    public FilePath getFile() {
        return myFile;
    }

    
    public VcsRevisionNumber getRevisionNumber() {
        return VcsRevisionNumber.NULL;
    }
}
