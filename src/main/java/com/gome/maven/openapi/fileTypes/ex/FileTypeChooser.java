/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gome.maven.openapi.fileTypes.ex;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.fileTypes.*;
import com.gome.maven.openapi.fileTypes.impl.FileTypeRenderer;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.ComboBox;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.ui.CollectionComboBoxModel;
import com.gome.maven.ui.DoubleClickListener;
import com.gome.maven.ui.ListScrollingUtil;
import com.gome.maven.util.FunctionUtil;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileTypeChooser extends DialogWrapper {
    private JList myList;
    private JLabel myTitleLabel;
    private ComboBox myPattern;
    private JPanel myPanel;
    private JRadioButton myOpenInIdea;
    private JRadioButton myOpenAsNative;
    private final String myFileName;

    private FileTypeChooser( List<String> patterns,  String fileName) {
        super(true);
        myFileName = fileName;

        myOpenInIdea.setText("Open matching files in " + ApplicationNamesInfo.getInstance().getFullProductName() + ":");

        FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
        Arrays.sort(fileTypes, new Comparator<FileType>() {
            @Override
            public int compare(final FileType fileType1, final FileType fileType2) {
                if (fileType1 == null) {
                    return 1;
                }
                if (fileType2 == null) {
                    return -1;
                }
                return fileType1.getDescription().compareToIgnoreCase(fileType2.getDescription());
            }
        });

        final DefaultListModel model = new DefaultListModel();
        for (FileType type : fileTypes) {
            if (!type.isReadOnly() && type != FileTypes.UNKNOWN && !(type instanceof NativeFileType)) {
                model.addElement(type);
            }
        }
        myList.setModel(model);
        myPattern.setModel(new CollectionComboBoxModel(ContainerUtil.map(patterns, FunctionUtil.<String>id()), patterns.get(0)));

        setTitle(FileTypesBundle.message("filetype.chooser.title"));
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        myTitleLabel.setText(FileTypesBundle.message("filetype.chooser.prompt", myFileName));

        myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myList.setCellRenderer(new FileTypeRenderer());

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                doOKAction();
                return true;
            }
        }.installOn(myList);

        myList.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        updateButtonsState();
                    }
                }
        );

        ListScrollingUtil.selectItem(myList, FileTypes.PLAIN_TEXT);

        return myPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myList;
    }

    private void updateButtonsState() {
        setOKActionEnabled(myList.getSelectedIndex() != -1 || myOpenAsNative.isSelected());
    }

    @Override
    protected String getDimensionServiceKey(){
        return "#com.gome.maven.fileTypes.FileTypeChooser";
    }

    public FileType getSelectedType() {
        return myOpenAsNative.isSelected() ? NativeFileType.INSTANCE : (FileType) myList.getSelectedValue();
    }

    /**
     * If fileName is already associated any known file type returns it.
     * Otherwise asks user to select file type and associates it with fileName extension if any selected.
     * @return Known file type or null. Never returns {@link com.gome.maven.openapi.fileTypes.FileTypes#UNKNOWN}.
     */
    
    public static FileType getKnownFileTypeOrAssociate( VirtualFile file,  Project project) {
        if (project != null && !(file instanceof FakeVirtualFile)) {
            ((PsiManagerEx)PsiManager.getInstance(project)).getFileManager().findFile(file); // autodetect text file if needed
        }
        FileType type = file.getFileType();
        if (type == FileTypes.UNKNOWN) {
            type = getKnownFileTypeOrAssociate(file.getName());
        }
        return type;
    }

    
    public static FileType getKnownFileTypeOrAssociate( String fileName) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType type = fileTypeManager.getFileTypeByFileName(fileName);
        if (type == FileTypes.UNKNOWN) {
            type = associateFileType(fileName);
        }
        return type;
    }

    
    public static FileType associateFileType( final String fileName) {
        final FileTypeChooser chooser = new FileTypeChooser(suggestPatterns(fileName), fileName);
        if (!chooser.showAndGet()) {
            return null;
        }
        final FileType type = chooser.getSelectedType();
        if (type == FileTypes.UNKNOWN || type == null) return null;

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                FileTypeManagerEx.getInstanceEx().associatePattern(type, (String)chooser.myPattern.getSelectedItem());
            }
        });

        return type;
    }

    
    static List<String> suggestPatterns( String fileName) {
        List<String> patterns = ContainerUtil.newLinkedList(fileName);

        int i = -1;
        while ((i = fileName.indexOf('.', i + 1)) > 0) {
            String extension = fileName.substring(i);
            if (!StringUtil.isEmpty(extension)) {
                patterns.add(0, "*" + extension);
            }
        }

        return patterns;
    }

    @Override
    protected String getHelpId() {
        return "reference.dialogs.register.association";
    }
}
