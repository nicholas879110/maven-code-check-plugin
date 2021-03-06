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

package com.gome.maven.openapi.vcs.vfs;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.WeakHashMap;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author yole
 */
public class ContentRevisionVirtualFile extends AbstractVcsVirtualFile {
    private final ContentRevision myContentRevision;
    private byte[] myContent;
    private boolean myContentLoadFailed;

    private static final WeakHashMap<ContentRevision, ContentRevisionVirtualFile> ourMap = new WeakHashMap<ContentRevision, ContentRevisionVirtualFile>();

    public static ContentRevisionVirtualFile create(ContentRevision contentRevision) {
        synchronized(ourMap) {
            ContentRevisionVirtualFile revisionVirtualFile = ourMap.get(contentRevision);
            if (revisionVirtualFile == null) {
                revisionVirtualFile = new ContentRevisionVirtualFile(contentRevision);
                ourMap.put(contentRevision, revisionVirtualFile);
            }
            return revisionVirtualFile;
        }
    }

    private ContentRevisionVirtualFile(ContentRevision contentRevision) {
        super(contentRevision.getFile().getPath(), VcsFileSystem.getInstance());
        myContentRevision = contentRevision;
        setCharset(CharsetToolkit.UTF8_CHARSET);
    }

    public boolean isDirectory() {
        return false;
    }


    public byte[] contentsToByteArray() throws IOException {
        if (myContentLoadFailed || myProcessingBeforeContentsChange) {
            return ArrayUtil.EMPTY_BYTE_ARRAY;
        }
        if (myContent == null) {
            loadContent();
        }
        return myContent;
    }

    private void loadContent() {
        final VcsFileSystem vcsFileSystem = ((VcsFileSystem)getFileSystem());

        try {
            final String content = myContentRevision.getContent();
            if (content == null) {
                throw new VcsException("Could not load content");
            }
            fireBeforeContentsChange();

            myModificationStamp++;
            setRevision(myContentRevision.getRevisionNumber().asString());
            myContent = content.getBytes(getCharset());
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    vcsFileSystem.fireContentsChanged(this, ContentRevisionVirtualFile.this, 0);
                }
            });

        }
        catch (VcsException e) {
            myContentLoadFailed = true;
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    vcsFileSystem.fireBeforeFileDeletion(this, ContentRevisionVirtualFile.this);
                }
            });
            myContent = ArrayUtil.EMPTY_BYTE_ARRAY;
            setRevision("0");

            Messages.showMessageDialog(
                    VcsBundle.message("message.text.could.not.load.virtual.file.content", getPresentableUrl(), e.getLocalizedMessage()),
                    VcsBundle.message("message.title.could.not.load.content"),
                    Messages.getInformationIcon());

            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    vcsFileSystem.fireFileDeleted(this, ContentRevisionVirtualFile.this, getName(), getParent());
                }
            });

        }
        catch (ProcessCanceledException ex) {
            myContent = ArrayUtil.EMPTY_BYTE_ARRAY;
        }
    }
}