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

import com.gome.maven.openapi.diff.impl.patch.PatchHunk;
import com.gome.maven.openapi.diff.impl.patch.TextFilePatch;
import com.gome.maven.openapi.diff.impl.patch.apply.GenericPatchApplier;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Processor;

import java.util.ArrayList;
import java.util.List;

public class ApplyPatchForBaseRevisionTexts {
    private final CharSequence myLocal;
    private CharSequence myBase;
    private String myPatched;
    private List<String> myWarnings;
    private boolean myBaseRevisionLoaded;

    
    public static ApplyPatchForBaseRevisionTexts create(final Project project, final VirtualFile file, final FilePath pathBeforeRename,
                                                        final TextFilePatch patch, final Getter<CharSequence> baseContents) {
        assert ! patch.isNewFile();
        final String beforeVersionId = patch.getBeforeVersionId();
        DefaultPatchBaseVersionProvider provider = null;
        if (beforeVersionId != null) {
            provider = new DefaultPatchBaseVersionProvider(project, file, beforeVersionId);
        }
        if (provider != null && provider.canProvideContent()) {
            return new ApplyPatchForBaseRevisionTexts(provider, pathBeforeRename, patch, file, baseContents);
        } else {
            return new ApplyPatchForBaseRevisionTexts(null, pathBeforeRename, patch, file, baseContents);
        }
    }

    private ApplyPatchForBaseRevisionTexts(final DefaultPatchBaseVersionProvider provider,
                                           final FilePath pathBeforeRename,
                                           final TextFilePatch patch,
                                           final VirtualFile file,
                                           Getter<CharSequence> baseContents) {
        myWarnings = new ArrayList<String>();
        final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        if (document != null) {
            fileDocumentManager.saveDocument(document);
        }
        myLocal = LoadTextUtil.loadText(file);

        final List<PatchHunk> hunks = patch.getHunks();

        if (provider != null) {
            try {
                provider.getBaseVersionContent(pathBeforeRename, new Processor<CharSequence>() {
                    public boolean process(final CharSequence text) {
                        final GenericPatchApplier applier = new GenericPatchApplier(text, hunks);
                        if (! applier.execute()) {
                            return true;
                        }
                        myBase = text;
                        setPatched(applier.getAfter());
                        return false;
                    }
                }, myWarnings);
            }
            catch (VcsException e) {
                myWarnings.add(e.getMessage());
            }
            myBaseRevisionLoaded = myPatched != null;
            if (myBaseRevisionLoaded) return;
        }

        CharSequence contents = baseContents.get();
        if (contents != null) {
            contents = StringUtil.convertLineSeparators(contents.toString());
            myBase = contents;
            myBaseRevisionLoaded = true;
            final GenericPatchApplier applier = new GenericPatchApplier(contents, hunks);
            if (! applier.execute()) {
                applier.trySolveSomehow();
            }
            setPatched(applier.getAfter());
            return;
        }

        final GenericPatchApplier applier = new GenericPatchApplier(myLocal, hunks);
        if (! applier.execute()) {
            applier.trySolveSomehow();
        }
        setPatched(applier.getAfter());
    }

    public CharSequence getLocal() {
        return myLocal;
    }

    public CharSequence getBase() {
        return myBase;
    }

    private void setPatched(final String text) {
        myPatched = StringUtil.convertLineSeparators(text);
    }

    public String getPatched() {
        return myPatched;
    }

    public static String getCannotLoadBaseMessage(final String filePatch) {
        return VcsBundle.message("patch.load.base.revision.error", filePatch,"");
    }
}
