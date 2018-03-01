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
package com.gome.maven.openapi.vcs.history;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vcs.RepositoryLocation;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ui.UIUtil;

import java.io.IOException;
import java.util.Date;


public class CurrentRevision implements VcsFileRevision {
    private final VirtualFile myFile;
    public static final String CURRENT = VcsBundle.message("vcs.revision.name.current");
    private final VcsRevisionNumber myRevisionNumber;

    public CurrentRevision(VirtualFile file, VcsRevisionNumber revision) {
        myFile = file;
        myRevisionNumber = revision;
    }

    public String getCommitMessage() {
        return "[" + CURRENT + "]";
    }

    public byte[] loadContent() throws IOException, VcsException {
        return getContent();
    }

    public Date getRevisionDate() {
        return new Date(myFile.getTimeStamp());
    }

    public byte[] getContent() throws IOException, VcsException {
        try {
            Document document = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
                public Document compute() {
                    return FileDocumentManager.getInstance().getDocument(myFile);
                }
            });
            if (document != null) {
                return document.getText().getBytes(myFile.getCharset().name());
            }
            else {
                return myFile.contentsToByteArray();
            }
        }
        catch (final IOException e) {
            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override public void run() {
                    Messages.showMessageDialog(e.getLocalizedMessage(), VcsBundle.message("message.text.could.not.load.file.content"),
                            Messages.getErrorIcon());
                }
            });
            return null;
        }

    }

    public String getAuthor() {
        return "";
    }

    public VcsRevisionNumber getRevisionNumber() {
        return myRevisionNumber;
    }

    public String getBranchName() {
        return null;
    }


    @Override
    public RepositoryLocation getChangedRepositoryPath() {
        return null;  // use initial url..
    }
}
