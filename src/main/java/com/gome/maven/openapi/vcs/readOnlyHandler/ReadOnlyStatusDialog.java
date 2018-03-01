
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
package com.gome.maven.openapi.vcs.readOnlyHandler;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vfs.ReadonlyStatusHandler;
import com.gome.maven.ui.CollectionComboBoxModel;
import com.gome.maven.ui.ColoredListCellRendererWrapper;
import com.gome.maven.ui.SimpleTextAttributes;
import com.gome.maven.util.ui.OptionsDialog;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class ReadOnlyStatusDialog extends OptionsDialog {

    static final SimpleTextAttributes BOLD_ATTRIBUTES = new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getListForeground());
    static final SimpleTextAttributes SELECTED_BOLD_ATTRIBUTES =
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, UIUtil.getListSelectionForeground());

    private JPanel myTopPanel;
    private JList myFileList;
    private JRadioButton myUsingFileSystemRadioButton;
    private JRadioButton myUsingVcsRadioButton;
    private JComboBox myChangelist;
    private FileInfo[] myFiles;

    public ReadOnlyStatusDialog(Project project, final FileInfo[] files) {
        super(project);
        myFiles = files;
        initFileList();

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myChangelist.setEnabled(myUsingVcsRadioButton.isSelected());
            }
        };
        myUsingVcsRadioButton.addActionListener(listener);
        myUsingFileSystemRadioButton.addActionListener(listener);

        if (myUsingVcsRadioButton.isEnabled()) {
            myUsingVcsRadioButton.setSelected(true);
        }
        else {
            myUsingFileSystemRadioButton.setSelected(true);
        }
        myChangelist.setEnabled(myUsingVcsRadioButton.isSelected());
        myFileList.setCellRenderer(new FileListRenderer());
        setTitle(VcsBundle.message("dialog.title.clear.read.only.file.status"));

        init();
    }

    @Override
    public long getTypeAheadTimeoutMs() {
        return Registry.intValue("actionSystem.typeAheadTimeBeforeDialog");
    }

    private void initFileList() {
        myFileList.setModel(new AbstractListModel() {
            public int getSize() {
                return myFiles.length;
            }

            public Object getElementAt(final int index) {
                return myFiles [index].getFile();
            }
        });

        boolean hasVcs = false;
        for(FileInfo info: myFiles) {
            if (info.hasVersionControl()) {
                hasVcs = true;
                HandleType handleType = info.getSelectedHandleType();
                List<String> changelists = handleType.getChangelists();
                final String defaultChangelist = handleType.getDefaultChangelist();
                myChangelist.setModel(new CollectionComboBoxModel(changelists, defaultChangelist));

                myChangelist.setRenderer(new ColoredListCellRendererWrapper<String>() {
                    @Override
                    protected void doCustomize(JList list, String value, int index, boolean selected, boolean hasFocus) {
                        if (value == null) return;
                        String trimmed = StringUtil.first(value, 50, true);
                        if (value.equals(defaultChangelist)) {
                            append(trimmed, selected ? SELECTED_BOLD_ATTRIBUTES : BOLD_ATTRIBUTES);
                        }
                        else {
                            append(trimmed, selected ? SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES : SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES);
                        }
                    }
                });

                break;
            }
        }
        myUsingVcsRadioButton.setEnabled(hasVcs);
    }

    protected boolean isToBeShown() {
        return ((ReadonlyStatusHandlerImpl)ReadonlyStatusHandler.getInstance(myProject)).getState().SHOW_DIALOG;
    }

    protected void setToBeShown(boolean value, boolean onOk) {
        if (onOk) {
            ((ReadonlyStatusHandlerImpl)ReadonlyStatusHandler.getInstance(myProject)).getState().SHOW_DIALOG = value;
        }
    }

    protected boolean shouldSaveOptionsOnCancel() {
        return false;
    }

    
    protected JComponent createCenterPanel() {
        return myTopPanel;
    }

    @Override
    
    protected String getDimensionServiceKey() {
        return "vcs.readOnlyHandler.ReadOnlyStatusDialog";
    }

    protected void doOKAction() {
        for(FileInfo info: myFiles) {
            if (myUsingFileSystemRadioButton.isSelected()) {
                info.getHandleType().selectFirst();
            }
            else if (info.hasVersionControl()) {
                info.getHandleType().select(info.getHandleType().get(1));
            }
        }

        ArrayList<FileInfo> files = new ArrayList<FileInfo>();
        Collections.addAll(files, myFiles);
        String changelist = (String)myChangelist.getSelectedItem();
        ReadonlyStatusHandlerImpl.processFiles(files, changelist);

        if (files.isEmpty()) {
            super.doOKAction();
        }
        else {
            myFiles = files.toArray(new FileInfo[files.size()]);
            initFileList();
        }
    }

    @Override 
    public JComponent getPreferredFocusedComponent() {
        final JRootPane pane = getRootPane();
        return pane != null ? pane.getDefaultButton() : null;
    }

}