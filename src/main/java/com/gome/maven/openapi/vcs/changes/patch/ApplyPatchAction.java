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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 17.11.2006
 * Time: 17:08:11
 */
package com.gome.maven.openapi.vcs.changes.patch;

import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.PlatformDataKeys;
import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.diff.*;
import com.gome.maven.openapi.diff.impl.patch.*;
import com.gome.maven.openapi.diff.impl.patch.apply.ApplyFilePatch;
import com.gome.maven.openapi.diff.impl.patch.apply.ApplyFilePatchBase;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.impl.DocumentImpl;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileChooser.FileChooser;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vcs.FilePathImpl;
import com.gome.maven.openapi.vcs.VcsApplicationSettings;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.changes.ChangeListManager;
import com.gome.maven.openapi.vcs.changes.CommitContext;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Consumer;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ApplyPatchAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.vcs.changes.patch.ApplyPatchAction");

    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) return;
        if (ChangeListManager.getInstance(project).isFreezedWithNotification("Can not apply patch now")) return;

        if (e.getPlace().equals(ActionPlaces.PROJECT_VIEW_POPUP)) {
            VirtualFile vFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
            if (vFile != null && vFile.getFileType() == StdFileTypes.PATCH) {
                showApplyPatch(project, vFile);
                return;
            }
        }

        showApplyPatch(project, null);
    }

    public static void showApplyPatch(final Project project, final VirtualFile file) {
        FileDocumentManager.getInstance().saveAllDocuments();
        if (file != null) {
            final ApplyPatchDifferentiatedDialog dialog = new ApplyPatchDifferentiatedDialog(project, new ApplyPatchDefaultExecutor(project),
                    Collections.<ApplyPatchExecutor>singletonList(new ImportToShelfExecutor(project)), ApplyPatchMode.APPLY, file);
            dialog.show();
        }
        else {
            final FileChooserDescriptor descriptor = ApplyPatchDifferentiatedDialog.createSelectPatchDescriptor();
            final VcsApplicationSettings settings = VcsApplicationSettings.getInstance();
            final VirtualFile toSelect = settings.PATCH_STORAGE_LOCATION == null ? null :
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(settings.PATCH_STORAGE_LOCATION));
            FileChooser.chooseFiles(descriptor, project, toSelect, new Consumer<List<VirtualFile>>() {
                @Override
                public void consume(List<VirtualFile> files) {
                    if (files.size() != 1) return;
                    final VirtualFile parent = files.get(0).getParent();
                    if (parent != null) {
                        settings.PATCH_STORAGE_LOCATION = FileUtil.toSystemDependentName(parent.getPath());
                    }
                    new ApplyPatchDifferentiatedDialog(
                            project, new ApplyPatchDefaultExecutor(project),
                            Collections.<ApplyPatchExecutor>singletonList(new ImportToShelfExecutor(project)), ApplyPatchMode.APPLY, files.get(0)
                    ).show();
                }
            });
        }
    }

    public static void applySkipDirs(final List<FilePatch> patches, final int skipDirs) {
        if (skipDirs < 1) {
            return;
        }
        for (FilePatch patch : patches) {
            patch.setBeforeName(skipN(patch.getBeforeName(), skipDirs));
            patch.setAfterName(skipN(patch.getAfterName(), skipDirs));
        }
    }

    private static String skipN(final String path, final int num) {
        final String[] pieces = path.split("/");
        final StringBuilder sb = new StringBuilder();
        for (int i = num; i < pieces.length; i++) {
            final String piece = pieces[i];
            sb.append('/').append(piece);
        }
        return sb.toString();
    }

    public static<T extends FilePatch> ApplyPatchStatus applyOnly(final Project project,
                                                                  final ApplyFilePatchBase<T> patch,
                                                                  final ApplyPatchContext context,
                                                                  final VirtualFile file,
                                                                  final CommitContext commitContext,
                                                                  boolean reverse,
                                                                   String leftPanelTitle,  String rightPanelTitle) {
        final T patchBase = patch.getPatch();
        final Application application = ApplicationManager.getApplication();
        final ApplyFilePatch.Result result = application.runWriteAction(new Computable<ApplyFilePatch.Result>() {
            @Override
            public ApplyFilePatch.Result compute() {
                try {
                    return patch.apply(file, context, project, new FilePathImpl(file), new Getter<CharSequence>() {
                        @Override
                        public CharSequence get() {
                            return getBaseContents((TextFilePatch)patchBase, commitContext, project);
                        }
                    }, commitContext);
                }
                catch (IOException e) {
                    LOG.error(e);
                    return ApplyFilePatch.Result.createThrow(e);
                }
            }
        });

        final ApplyPatchStatus status;
        try {
            status = result.getStatus();
        }
        catch (IOException e) {
            showIOException(project, patchBase.getBeforeName(), e);
            return ApplyPatchStatus.FAILURE;
        }
        if (ApplyPatchStatus.ALREADY_APPLIED.equals(status) || ApplyPatchStatus.SUCCESS.equals(status)) {
            return status;
        }
        final ApplyPatchForBaseRevisionTexts mergeData = result.getMergeData();
        if (mergeData != null) {
            if (mergeData.getBase() != null) {
                return showMergeDialog(project, file, mergeData.getBase(), mergeData.getPatched(), ApplyPatchMergeRequestFactory.INSTANCE,
                        reverse, leftPanelTitle, rightPanelTitle);
            } else {
                try {
                    return showBadDiffDialog(project, file, mergeData, false);
                }
                catch (final IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showIOException(project, patchBase.getBeforeName(), e);
                        }
                    });
                }
                return ApplyPatchStatus.FAILURE;
            }
        }
        return status;
    }

    private static <T extends FilePatch> void showIOException(Project project, String name, IOException e) {
        Messages.showErrorDialog(project, VcsBundle.message("patch.apply.error", name, e.getMessage()),
                VcsBundle.message("patch.apply.dialog.title"));
    }

    
    private static CharSequence getBaseContents(final TextFilePatch patchBase, final CommitContext commitContext, final Project project) {
        final BaseRevisionTextPatchEP baseRevisionTextPatchEP = Extensions.findExtension(PatchEP.EP_NAME, project, BaseRevisionTextPatchEP.class);
        if (baseRevisionTextPatchEP != null) {
            final String path = patchBase.getBeforeName() == null ? patchBase.getAfterName() : patchBase.getBeforeName();
            return baseRevisionTextPatchEP.provideContent(path, commitContext);
        }
        return null;
    }

    public static ApplyPatchStatus showBadDiffDialog(final Project project, final VirtualFile file, final ApplyPatchForBaseRevisionTexts texts, final boolean readonly)
            throws IOException {
        if (texts.getLocal() == null) {
            return ApplyPatchStatus.FAILURE;
        }
        final DiffTool tool = DiffManager.getInstance().getDiffTool();

        final SimpleDiffRequest simpleRequest = createBadDiffRequest(project, file, texts, readonly);
        tool.show(simpleRequest);

        return ApplyPatchStatus.SUCCESS;
    }

    public static SimpleDiffRequest createBadDiffRequest(final Project project,
                                                         final VirtualFile file,
                                                         ApplyPatchForBaseRevisionTexts texts,
                                                         boolean readonly) {
        final SimpleDiffRequest simpleRequest =
                new SimpleDiffRequest(project, "Result Of Patch Apply To " + file.getName() + " (" +
                        (file.getParent() == null ? file.getPath() : file.getParent().getPath()) +
                        ")");
        final DocumentImpl patched = new DocumentImpl(texts.getPatched());
        patched.setReadOnly(false);

        final DocumentContent mergedDocument = new DocumentContent(project, patched, file.getFileType());
        mergedDocument.getDocument().setReadOnly(readonly);
        simpleRequest.setContents(new SimpleContent(texts.getLocal().toString(), file.getFileType()),
                mergedDocument);
        simpleRequest.setContentTitles(VcsBundle.message("diff.title.local"), "Patched (with problems)");
        simpleRequest.addHint(DiffTool.HINT_SHOW_MODAL_DIALOG);
        simpleRequest.addHint(DiffTool.HINT_DIFF_IS_APPROXIMATE);

        if (! readonly) {
            simpleRequest.setOnOkRunnable(new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            final String resultText = mergedDocument.getDocument().getText();
                            final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
                            final Document document = fileDocumentManager.getDocument(file);
                            if (document == null) {
                                try {
                                    VfsUtil.saveText(file, resultText);
                                }
                                catch (IOException e) {
                                    showIOException(project, file.getName(), e);          // todo bad: we had already returned success by now
                                }
                            } else {
                                document.setText(resultText);
                                fileDocumentManager.saveDocument(document);
                            }
                        }
                    });
                }
            });
        }
        return simpleRequest;
    }

    private static ApplyPatchStatus showMergeDialog(Project project,
                                                    VirtualFile file,
                                                    CharSequence content,
                                                    final String patchedContent,
                                                    final PatchMergeRequestFactory mergeRequestFactory,
                                                    boolean reverse,
                                                     String leftPanelTitle,  String rightPanelTitle) {
        final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        if (document != null) {
            fileDocumentManager.saveDocument(document);
        }
        CharSequence fileContent = LoadTextUtil.loadText(file);
        if (fileContent == null || content == null) {
            return ApplyPatchStatus.FAILURE;
        }
        final MergeRequest request = mergeRequestFactory.createMergeRequest(fileContent.toString(), patchedContent, content.toString(), file,
                project, reverse, leftPanelTitle, rightPanelTitle);
        DiffManager.getInstance().getDiffTool().show(request);
        if (request.getResult() == DialogWrapper.OK_EXIT_CODE) {
            return ApplyPatchStatus.SUCCESS;
        }
        request.restoreOriginalContent();
        return ApplyPatchStatus.FAILURE;
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        if (e.getPlace().equals(ActionPlaces.PROJECT_VIEW_POPUP)) {
            VirtualFile vFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
            e.getPresentation().setVisible(project != null && vFile != null && vFile.getFileType() == StdFileTypes.PATCH);
        }
        else {
            e.getPresentation().setEnabled(project != null);
        }
    }

    public static class ApplyPatchMergeRequestFactory implements PatchMergeRequestFactory {
        private final boolean myReadOnly;

        public static final ApplyPatchMergeRequestFactory INSTANCE = new ApplyPatchMergeRequestFactory(false);
        public static final ApplyPatchMergeRequestFactory INSTANCE_READ_ONLY = new ApplyPatchMergeRequestFactory(true);

        public ApplyPatchMergeRequestFactory(final boolean readOnly) {
            myReadOnly = readOnly;
        }

        public MergeRequest createMergeRequest(final String leftText, final String rightText, final String originalContent,
                                                final VirtualFile file, final Project project, boolean reverse,
                                                String leftPanelTitle,  String rightPanelTitle) {
            MergeRequest request;
            if (myReadOnly) {
                request = DiffRequestFactory.getInstance()
                        .create3WayDiffRequest(leftText, rightText, originalContent, file.getFileType(), project, null, null);
            } else {
                request = DiffRequestFactory.getInstance().createMergeRequest(reverse ? rightText : leftText,
                        reverse ? leftText : rightText, originalContent,
                        file, project, ActionButtonPresentation.APPLY,
                        ActionButtonPresentation.CANCEL_WITH_PROMPT);
            }

            request.setVersionTitles(new String[] {
                    leftPanelTitle == null ? VcsBundle.message("patch.apply.conflict.local.version") : leftPanelTitle,
                    rightPanelTitle == null ? VcsBundle.message("patch.apply.conflict.merged.version") : rightPanelTitle,
                    VcsBundle.message("patch.apply.conflict.patched.version")
            });
            request.setWindowTitle(VcsBundle.message("patch.apply.conflict.title", file.getPresentableUrl()));
            return request;
        }
    }
}
