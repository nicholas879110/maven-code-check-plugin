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

package com.gome.maven.openapi.vcs.changes.ui;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.gome.maven.ide.DeleteProvider;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.fileChooser.actions.VirtualFileDeleteProvider;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vcs.VcsShowConfirmationOption;
import com.gome.maven.openapi.vcs.changes.actions.DeleteUnversionedFilesAction;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;

import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public class SelectFilesDialog extends AbstractSelectFilesDialog<VirtualFile> {

     private final VirtualFileList myFileList;
    private final boolean myDeletableFiles;

    protected SelectFilesDialog(Project project, List<VirtualFile> originalFiles, String prompt,
                                VcsShowConfirmationOption confirmationOption,
                                boolean selectableFiles, boolean showDoNotAskOption, boolean deletableFiles) {
        super(project, false, confirmationOption, prompt, showDoNotAskOption);
        myDeletableFiles = deletableFiles;
        myFileList = new VirtualFileList(project, originalFiles, selectableFiles, deletableFiles);
        myFileList.setChangesToDisplay(originalFiles);
    }

    
    public static SelectFilesDialog init(Project project, List<VirtualFile> originalFiles, String prompt,
                                         VcsShowConfirmationOption confirmationOption,
                                         boolean selectableFiles, boolean showDoNotAskOption, boolean deletableFiles) {
        SelectFilesDialog dialog = new SelectFilesDialog(project, originalFiles, prompt, confirmationOption, selectableFiles,
                showDoNotAskOption, deletableFiles);
        dialog.init();
        return dialog;
    }

    public Collection<VirtualFile> getSelectedFiles() {
        return myFileList.getIncludedChanges();
    }

    public void setSelectedFiles( final Collection<VirtualFile> selected) {
        myFileList.setIncludedChanges(selected);
    }

    
    @Override
    protected ChangesTreeList getFileList() {
        return myFileList;
    }

    
    @Override
    protected DefaultActionGroup createToolbarActions() {
        DefaultActionGroup defaultGroup = super.createToolbarActions();
        if (myDeletableFiles) {
            AnAction deleteAction = new DeleteUnversionedFilesAction() {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    super.actionPerformed(e);
                    myFileList.refresh();
                }
            };
            defaultGroup.add(deleteAction);
            deleteAction.registerCustomShortcutSet(CommonShortcuts.getDelete(), this.getFileList());
        }
        return defaultGroup;
    }

    public static class VirtualFileList extends ChangesTreeList<VirtualFile> {
        private final Project myProject;
         private final DeleteProvider myDeleteProvider;

        public VirtualFileList(Project project, List<VirtualFile> originalFiles, boolean selectableFiles, boolean deletableFiles) {
            super(project, originalFiles, selectableFiles, true, null, null);
            myProject = project;
            myDeleteProvider = (deletableFiles ?  new VirtualFileDeleteProvider() : null);
        }

        protected DefaultTreeModel buildTreeModel(final List<VirtualFile> changes, ChangeNodeDecorator changeNodeDecorator) {
            return new TreeModelBuilder(myProject, false).buildModelFromFiles(changes);
        }

        protected List<VirtualFile> getSelectedObjects(final ChangesBrowserNode node) {
            return node.getAllFilesUnder();
        }

        protected VirtualFile getLeadSelectedObject(final ChangesBrowserNode node) {
            final Object o = node.getUserObject();
            if (o instanceof VirtualFile) {
                return (VirtualFile) o;
            }
            return null;
        }

        @Override
        public void calcData(DataKey key, DataSink sink) {
            super.calcData(key, sink);
            if (key.equals(PlatformDataKeys.DELETE_ELEMENT_PROVIDER) && myDeleteProvider != null) {
                sink.put(key, myDeleteProvider);
            }
            else if (key.equals(CommonDataKeys.VIRTUAL_FILE_ARRAY)) {
                sink.put(key, ArrayUtil.toObjectArray(getSelectedChanges(), VirtualFile.class));
            }
        }

        public void refresh() {
            setChangesToDisplay(new ArrayList<VirtualFile>(Collections2.filter(getIncludedChanges(), new Predicate<VirtualFile>() {
                @Override
                public boolean apply( VirtualFile input) {
                    return input != null && input.isValid();
                }
            })));
        }

    }
}
