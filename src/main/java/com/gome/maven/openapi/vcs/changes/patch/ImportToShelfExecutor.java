/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.gome.maven.openapi.diff.impl.patch.FilePatch;
import com.gome.maven.openapi.diff.impl.patch.PatchEP;
import com.gome.maven.openapi.diff.impl.patch.PatchSyntaxException;
import com.gome.maven.openapi.diff.impl.patch.TextFilePatch;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vcs.AbstractVcsHelper;
import com.gome.maven.openapi.vcs.ObjectsConvertor;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.CommitContext;
import com.gome.maven.openapi.vcs.changes.LocalChangeList;
import com.gome.maven.openapi.vcs.changes.TransparentlyFailedValueI;
import com.gome.maven.openapi.vcs.changes.shelf.ShelveChangesManager;
import com.gome.maven.openapi.vcs.changes.shelf.ShelvedChangeList;
import com.gome.maven.openapi.vcs.changes.shelf.ShelvedChangesViewManager;
import com.gome.maven.openapi.vcs.ui.VcsBalloonProblemNotifier;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.containers.Convertor;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.vcsUtil.VcsCatchingRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author irengrig
 *         Date: 2/25/11
 *         Time: 6:21 PM
 */
public class ImportToShelfExecutor implements ApplyPatchExecutor {
    public static final String IMPORT_TO_SHELF = "Import to shelf";
    private final Project myProject;

    public ImportToShelfExecutor(Project project) {
        myProject = project;
    }

    @Override
    public String getName() {
        return IMPORT_TO_SHELF;
    }

    @Override
    public void apply(final MultiMap<VirtualFile, FilePatchInProgress> patchGroups,
                      LocalChangeList localList,
                      final String fileName,
                      final TransparentlyFailedValueI<Map<String, Map<String, CharSequence>>, PatchSyntaxException> additionalInfo) {
        final VcsCatchingRunnable vcsCatchingRunnable = new VcsCatchingRunnable() {
            @Override
            public void runImpl() throws VcsException {
                final VirtualFile baseDir = myProject.getBaseDir();
                final File ioBase = new File(baseDir.getPath());
                final List<FilePatch> allPatches = new ArrayList<FilePatch>();
                for (VirtualFile virtualFile : patchGroups.keySet()) {
                    final File ioCurrentBase = new File(virtualFile.getPath());
                    allPatches.addAll(ObjectsConvertor.convert(patchGroups.get(virtualFile),
                            new Convertor<FilePatchInProgress, TextFilePatch>() {
                                public TextFilePatch convert(FilePatchInProgress o) {
                                    final TextFilePatch was = o.getPatch();
                                    was.setBeforeName(FileUtil.toSystemIndependentName(FileUtil.getRelativePath(ioBase,
                                            new File(ioCurrentBase, was.getBeforeName()))));
                                    was.setAfterName(FileUtil.toSystemIndependentName(FileUtil.getRelativePath(ioBase,
                                            new File(ioCurrentBase, was.getAfterName()))));
                                    return was;
                                }
                            }));
                }
                if (! allPatches.isEmpty()) {
                    PatchEP[] patchTransitExtensions = null;
                    if (additionalInfo != null) {
                        try {
                            final Map<String, PatchEP> extensions = new HashMap<String, PatchEP>();
                            for (Map.Entry<String, Map<String, CharSequence>> entry : additionalInfo.get().entrySet()) {
                                final String filePath = entry.getKey();
                                Map<String, CharSequence> extToValue = entry.getValue();
                                for (Map.Entry<String, CharSequence> innerEntry : extToValue.entrySet()) {
                                    TransitExtension patchEP = (TransitExtension)extensions.get(innerEntry.getKey());
                                    if (patchEP == null) {
                                        patchEP = new TransitExtension(innerEntry.getKey());
                                        extensions.put(innerEntry.getKey(), patchEP);
                                    }
                                    patchEP.put(filePath, innerEntry.getValue());
                                }
                            }
                            Collection<PatchEP> values = extensions.values();
                            patchTransitExtensions = values.toArray(new PatchEP[values.size()]);
                        }
                        catch (PatchSyntaxException e) {
                            VcsBalloonProblemNotifier
                                    .showOverChangesView(myProject, "Can not import additional patch info: " + e.getMessage(), MessageType.ERROR);
                        }
                    }
                    try {
                        final ShelvedChangeList shelvedChangeList = ShelveChangesManager.getInstance(myProject).
                                importFilePatches(fileName, allPatches, patchTransitExtensions);
                        ShelvedChangesViewManager.getInstance(myProject).activateView(shelvedChangeList);
                    }
                    catch (IOException e) {
                        throw new VcsException(e);
                    }
                }
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(vcsCatchingRunnable, "Import patch to shelf", true, myProject);
        if (! vcsCatchingRunnable.get().isEmpty()) {
            AbstractVcsHelper.getInstance(myProject).showErrors(vcsCatchingRunnable.get(), IMPORT_TO_SHELF);
        }
    }

    private static class TransitExtension implements PatchEP {
        private final String myName;
        private final Map<String, CharSequence> myMap;

        private TransitExtension(String name) {
            myName = name;
            myMap = new HashMap<String, CharSequence>();
        }

        
        @Override
        public String getName() {
            return myName;
        }

        @Override
        public CharSequence provideContent( String path, CommitContext commitContext) {
            return myMap.get(path);
        }

        @Override
        public void consumeContent( String path,  CharSequence content, CommitContext commitContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void consumeContentBeforePatchApplied( String path,
                                                      CharSequence content,
                                                     CommitContext commitContext) {
            throw new UnsupportedOperationException();
        }

        public void put(String fileName, CharSequence value) {
            myMap.put(fileName, value);
        }
    }
}
