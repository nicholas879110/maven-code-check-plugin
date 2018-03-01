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
package com.gome.maven.openapi.vcs.changes.shelf;

import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.BinaryContentRevision;
import com.gome.maven.openapi.vcs.history.VcsRevisionNumber;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 12/17/12
 * Time: 3:59 PM
 */
public class ShelvedBinaryContentRevision implements BinaryContentRevision {
    private final FilePath myPath;
    private final String myShelvedContentPath;

    public ShelvedBinaryContentRevision(FilePath path, final String shelvedContentPath) {
        myPath = path;
        myShelvedContentPath = shelvedContentPath;
    }

    
    @Override
    public byte[] getBinaryContent() throws VcsException {
        try {
            return FileUtil.loadFileBytes(new File(myShelvedContentPath));
        }
        catch (IOException e) {
            throw new VcsException(e);
        }
    }

    
    @Override
    public String getContent() throws VcsException {
        throw new IllegalStateException();
    }

    
    @Override
    public FilePath getFile() {
        return myPath;
    }

    
    @Override
    public VcsRevisionNumber getRevisionNumber() {
        return new VcsRevisionNumber() {
            @Override
            public String asString() {
                return VcsBundle.message("shelved.version.name");
            }

            @Override
            public int compareTo(VcsRevisionNumber o) {
                return -1;
            }
        };
    }
}
