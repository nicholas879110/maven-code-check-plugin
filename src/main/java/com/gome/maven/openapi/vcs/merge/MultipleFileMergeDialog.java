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

package com.gome.maven.openapi.vcs.merge;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.presentation.VirtualFilePresentation;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.diff.ActionButtonPresentation;
import com.gome.maven.openapi.diff.DiffManager;
import com.gome.maven.openapi.diff.DiffRequestFactory;
import com.gome.maven.openapi.diff.MergeRequest;
import com.gome.maven.openapi.diff.impl.mergeTool.MergeVersion;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ex.ProjectManagerEx;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.VcsDirtyScopeManager;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.ColoredTableCellRenderer;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.ui.components.JBLabel;
import com.gome.maven.ui.table.TableView;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.ui.ColumnInfo;
import com.gome.maven.util.ui.ListTableModel;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * @author yole
 */
public class MultipleFileMergeDialog extends DialogWrapper {
    private JPanel myRootPanel;
    private JButton myAcceptYoursButton;
    private JButton myAcceptTheirsButton;
    private JButton myMergeButton;
    private TableView<VirtualFile> myTable;
    private JBLabel myDescriptionLabel;
    private final MergeProvider myProvider;
    private final MergeSession myMergeSession;
    private final List<VirtualFile> myFiles;
    private final ListTableModel<VirtualFile> myModel;
    
    private final Project myProject;
    private final ProjectManagerEx myProjectManager;
    private final List<VirtualFile> myProcessedFiles = new SmartList<VirtualFile>();
    private final Set<VirtualFile> myBinaryFiles = new HashSet<VirtualFile>();
    private final MergeDialogCustomizer myMergeDialogCustomizer;

    private final VirtualFileRenderer myVirtualFileRenderer = new VirtualFileRenderer();

    public MultipleFileMergeDialog( Project project,  final List<VirtualFile> files,  final MergeProvider provider,
                                    MergeDialogCustomizer mergeDialogCustomizer) {
        super(project, false);

        myProject = project;
        myProjectManager = ProjectManagerEx.getInstanceEx();
        myProjectManager.blockReloadingProjectOnExternalChanges();
        myFiles = new ArrayList<VirtualFile>(files);
        myProvider = provider;
        myMergeDialogCustomizer = mergeDialogCustomizer;

        final String description = myMergeDialogCustomizer.getMultipleFileMergeDescription(files);
        if (!StringUtil.isEmptyOrSpaces(description)) {
            myDescriptionLabel.setText(description);
        }

        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        columns.add(new ColumnInfo<VirtualFile, VirtualFile>(VcsBundle.message("multiple.file.merge.column.name")) {
            @Override
            public VirtualFile valueOf(final VirtualFile virtualFile) {
                return virtualFile;
            }

            @Override
            public TableCellRenderer getRenderer(final VirtualFile virtualFile) {
                return myVirtualFileRenderer;
            }
        });
        columns.add(new ColumnInfo<VirtualFile, String>(VcsBundle.message("multiple.file.merge.column.type")) {
            @Override
            public String valueOf(final VirtualFile virtualFile) {
                return myBinaryFiles.contains(virtualFile)
                        ? VcsBundle.message("multiple.file.merge.type.binary")
                        : VcsBundle.message("multiple.file.merge.type.text");
            }

            @Override
            public String getMaxStringValue() {
                return VcsBundle.message("multiple.file.merge.type.binary");
            }

            @Override
            public int getAdditionalWidth() {
                return 10;
            }
        });
        if (myProvider instanceof MergeProvider2) {
            myMergeSession = ((MergeProvider2)myProvider).createMergeSession(files);
            Collections.addAll(columns, myMergeSession.getMergeInfoColumns());
        }
        else {
            myMergeSession = null;
        }
        myModel = new ListTableModel<VirtualFile>(columns.toArray(new ColumnInfo[columns.size()]));
        myModel.setItems(files);
        myTable.setModelAndUpdateColumns(myModel);
        myVirtualFileRenderer.setFont(UIUtil.getListFont());
        myTable.setRowHeight(myVirtualFileRenderer.getPreferredSize().height);
        setTitle(myMergeDialogCustomizer.getMultipleFileDialogTitle());
        init();
        myAcceptYoursButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e) {
                acceptRevision(true);
            }
        });
        myAcceptTheirsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e) {
                acceptRevision(false);
            }
        });
        myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged( final ListSelectionEvent e) {
                updateButtonState();
            }
        });
        for (VirtualFile file : files) {
            if (file.getFileType().isBinary() || provider.isBinary(file)) {
                myBinaryFiles.add(file);
            }
        }
        myTable.getSelectionModel().setSelectionInterval(0, 0);
    }

    private void updateButtonState() {
        boolean haveSelection = myTable.getSelectedRowCount() > 0;
        boolean haveUnmergeableFiles = false;
        for (VirtualFile file : myTable.getSelection()) {
            if (myBinaryFiles.contains(file)) {
                haveUnmergeableFiles = true;
                break;
            }
            if (myMergeSession != null) {
                boolean canMerge = myMergeSession.canMerge(file);
                if (!canMerge) {
                    haveUnmergeableFiles = true;
                    break;
                }
            }
        }
        myAcceptYoursButton.setEnabled(haveSelection);
        myAcceptTheirsButton.setEnabled(haveSelection);
        myMergeButton.setEnabled(haveSelection && !haveUnmergeableFiles);
    }

    @Override
    
    protected JComponent createCenterPanel() {
        return myRootPanel;
    }

    
    @Override
    protected Action[] createActions() {
        return new Action[]{getCancelAction()};
    }

    
    @Override
    protected Action getCancelAction() {
        Action action = super.getCancelAction();
        action.putValue(Action.NAME, CommonBundle.getCloseButtonText());
        return action;
    }

    @Override
    protected void dispose() {
        myProjectManager.unblockReloadingProjectOnExternalChanges();
        super.dispose();
    }

    @Override

    protected String getDimensionServiceKey() {
        return "MultipleFileMergeDialog";
    }

    private void acceptRevision(final boolean isCurrent) {
        FileDocumentManager.getInstance().saveAllDocuments();
        final Collection<VirtualFile> files = myTable.getSelection();
        for (final VirtualFile file : files) {
            final Ref<Exception> ex = new Ref<Exception>();
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (!(myProvider instanceof MergeProvider2) || myMergeSession.canMerge(file)) {
                                    MergeData data = myProvider.loadRevisions(file);
                                    if (isCurrent) {
                                        file.setBinaryContent(data.CURRENT);
                                    }
                                    else {
                                        file.setBinaryContent(data.LAST);
                                        checkMarkModifiedProject(file);
                                    }
                                }
                                markFileProcessed(file, isCurrent ? MergeSession.Resolution.AcceptedYours : MergeSession.Resolution.AcceptedTheirs);
                            }
                            catch (Exception e) {
                                ex.set(e);
                            }
                        }
                    }, "Accept " + (isCurrent ? "Yours" : "Theirs"), null);
                }
            });
            if (!ex.isNull()) {
                //noinspection ThrowableResultOfMethodCallIgnored
                Messages.showErrorDialog(myRootPanel, "Error saving merged data: " + ex.get().getMessage());
                break;
            }
        }
        updateModelFromFiles();
    }

    private void markFileProcessed( VirtualFile file,  MergeSession.Resolution resolution) {
        myFiles.remove(file);
        if (myProvider instanceof MergeProvider2) {
            myMergeSession.conflictResolvedForFile(file, resolution);
        }
        else {
            myProvider.conflictResolvedForFile(file);
        }
        myProcessedFiles.add(file);
        if (myProject != null) {
            VcsDirtyScopeManager.getInstance(myProject).fileDirty(file);
        }
    }

    private void updateModelFromFiles() {
        if (myFiles.isEmpty()) {
            doCancelAction();
        }
        else {
            int selIndex = myTable.getSelectionModel().getMinSelectionIndex();
            myModel.setItems(myFiles);
            if (selIndex >= myFiles.size()) {
                selIndex = myFiles.size() - 1;
            }
            myTable.getSelectionModel().setSelectionInterval(selIndex, selIndex);
        }
    }

    private void showMergeDialog() {
        for (VirtualFile file : myTable.getSelection()) {
            final MergeData mergeData;
            try {
                mergeData = myProvider.loadRevisions(file);
            }
            catch (VcsException ex) {
                Messages.showErrorDialog(myRootPanel, "Error loading revisions to merge: " + ex.getMessage());
                break;
            }

            if (mergeData.CURRENT == null || mergeData.LAST == null || mergeData.ORIGINAL == null) {
                Messages.showErrorDialog(myRootPanel, "Error loading revisions to merge");
                break;
            }

            String leftText = decodeContent(file, mergeData.CURRENT);
            String rightText = decodeContent(file, mergeData.LAST);
            String originalText = decodeContent(file, mergeData.ORIGINAL);

            DiffRequestFactory diffRequestFactory = DiffRequestFactory.getInstance();
            MergeRequest request = diffRequestFactory
                    .createMergeRequest(leftText, rightText, originalText, file, myProject, ActionButtonPresentation.APPLY,
                            ActionButtonPresentation.CANCEL_WITH_PROMPT);
            request.setVersionTitles(new String[] {
                    myMergeDialogCustomizer.getLeftPanelTitle(file),
                    myMergeDialogCustomizer.getCenterPanelTitle(file),
                    myMergeDialogCustomizer.getRightPanelTitle(file, mergeData.LAST_REVISION_NUMBER)
            });
            request.setWindowTitle(myMergeDialogCustomizer.getMergeWindowTitle(file));

            DiffManager.getInstance().getDiffTool().show(request);
            if (request.getResult() == DialogWrapper.OK_EXIT_CODE) {
                markFileProcessed(file, MergeSession.Resolution.Merged);
                checkMarkModifiedProject(file);
            }
            else {
                request.restoreOriginalContent();
            }
        }
        updateModelFromFiles();
    }

    private void checkMarkModifiedProject( VirtualFile file) {
        MergeVersion.MergeDocumentVersion.reportProjectFileChangeIfNeeded(myProject, file);
    }

    private void createUIComponents() {
        Action mergeAction = new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e) {
                showMergeDialog();
            }
        };
        mergeAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        myMergeButton = createJButtonForAction(mergeAction);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myTable;
    }

    private static String decodeContent(final VirtualFile file, final byte[] content) {
        return StringUtil.convertLineSeparators(CharsetToolkit.bytesToString(content, file.getCharset()));
    }

    
    public List<VirtualFile> getProcessedFiles() {
        return myProcessedFiles;
    }

    private static class VirtualFileRenderer extends ColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            VirtualFile vf = (VirtualFile)value;
            setIcon(VirtualFilePresentation.getIcon(vf));
            append(vf.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            final VirtualFile parent = vf.getParent();
            if (parent != null) {
                append(" (" + FileUtil.toSystemDependentName(parent.getPresentableUrl()) + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    }
}
