/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.ide.util;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.ExporterToTextFile;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.components.PathMacroManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.EditorSettings;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.impl.DocumentImpl;
import com.gome.maven.openapi.editor.impl.EditorFactoryImpl;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptorFactory;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.ui.TextFieldWithBrowseButton;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.ui.JBUI;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TooManyListenersException;

public class ExportToFileUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.ide.util.ExportToFileUtil");

    public static void exportTextToFile(Project project, String fileName, String textToExport) {
        String prepend = "";
        File file = new File(fileName);
        if (file.exists()) {
            int result = Messages.showYesNoCancelDialog(
                    project,
                    IdeBundle.message("error.text.file.already.exists", fileName),
                    IdeBundle.message("title.warning"),
                    IdeBundle.message("action.overwrite"),
                    IdeBundle.message("action.append"),
                    CommonBundle.getCancelButtonText(),
                    Messages.getWarningIcon()
            );

            if (result != Messages.NO && result != Messages.YES) {
                return;
            }
            if (result == Messages.NO) {
                char[] buf = new char[(int)file.length()];
                try {
                    FileReader reader = new FileReader(fileName);
                    try {
                        reader.read(buf, 0, (int)file.length());
                        prepend = new String(buf) + SystemProperties.getLineSeparator();
                    }
                    finally {
                        reader.close();
                    }
                }
                catch (IOException ignored) {
                }
            }
        }

        try {
            FileWriter writer = new FileWriter(fileName);
            try {
                writer.write(prepend + textToExport);
            }
            finally {
                writer.close();
            }
        }
        catch (IOException e) {
            Messages.showMessageDialog(
                    project,
                    IdeBundle.message("error.writing.to.file", fileName),
                    CommonBundle.getErrorTitle(),
                    Messages.getErrorIcon()
            );
        }
    }

    public static class ExportDialogBase extends DialogWrapper {
        private final Project myProject;
        private final ExporterToTextFile myExporter;
        protected Editor myTextArea;
        protected TextFieldWithBrowseButton myTfFile;
        private ChangeListener myListener;

        public ExportDialogBase(Project project, ExporterToTextFile exporter) {
            super(project, true);
            myProject = project;
            myExporter = exporter;

            myTfFile = new TextFieldWithBrowseButton();
            myTfFile.addBrowseFolderListener(null, null, myProject, FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor());

            setTitle(IdeBundle.message("title.export.preview"));
            setOKButtonText(IdeBundle.message("button.save"));
            setButtonsMargin(null);
            init();
            try {
                myListener = new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        initText();
                    }
                };
                myExporter.addSettingsChangedListener(myListener);
            }
            catch (TooManyListenersException e) {
                LOG.error(e);
            }
            initText();
        }

        public void dispose() {
            myExporter.removeSettingsChangedListener(myListener);
            EditorFactory.getInstance().releaseEditor(myTextArea);
            super.dispose();
        }

        private void initText() {
            myTextArea.getDocument().setText(myExporter.getReportText());
        }

        protected JComponent createCenterPanel() {
            final Document document = ((EditorFactoryImpl)EditorFactory.getInstance()).createDocument(true);
            ((DocumentImpl)document).setAcceptSlashR(true);

            myTextArea = EditorFactory.getInstance().createEditor(document, myProject, FileTypes.PLAIN_TEXT, true);
            final EditorSettings settings = myTextArea.getSettings();
            settings.setLineNumbersShown(false);
            settings.setLineMarkerAreaShown(false);
            settings.setFoldingOutlineShown(false);
            settings.setRightMarginShown(false);
            settings.setAdditionalLinesCount(0);
            settings.setAdditionalColumnsCount(0);
            settings.setAdditionalPageAtBottom(false);
            ((EditorEx)myTextArea).setBackgroundColor(UIUtil.getInactiveTextFieldBackgroundColor());
            myTextArea.getComponent().setPreferredSize(new Dimension(700, 400));
            return myTextArea.getComponent();
        }

        protected JComponent createNorthPanel() {
            JPanel filePanel = createFilePanel(myTfFile.getTextField(), myTfFile.getButton());
            JComponent settingsPanel = myExporter.getSettingsEditor();
            if (settingsPanel == null) {
                return filePanel;
            }
            JPanel northPanel = new JPanel(new BorderLayout());
            northPanel.add(filePanel, BorderLayout.NORTH);
            northPanel.add(settingsPanel, BorderLayout.CENTER);
            return northPanel;
        }

        protected JPanel createFilePanel(JTextField textField, JButton button) {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbConstraints = new GridBagConstraints();
            gbConstraints.fill = GridBagConstraints.HORIZONTAL;
            JLabel promptLabel = new JLabel(IdeBundle.message("editbox.export.to.file"));
            gbConstraints.weightx = 0;
            panel.add(promptLabel, gbConstraints);
            gbConstraints.weightx = 1;
            panel.add(myTfFile, gbConstraints);

            String defaultFilePath = myExporter.getDefaultFilePath();
            if (! new File(defaultFilePath).isAbsolute()) {
                defaultFilePath = PathMacroManager.getInstance(myProject).collapsePath(defaultFilePath).replace('/', File.separatorChar);
            } else {
                defaultFilePath = defaultFilePath.replace('/', File.separatorChar);
            }
            myTfFile.setText(defaultFilePath);

            panel.setBorder(JBUI.Borders.emptyBottom(5));

            return panel;
        }

        public String getText() {
            return myTextArea.getDocument().getText();
        }

        public void setFileName(String s) {
            myTfFile.setText(s);
        }

        public String getFileName() {
            return myTfFile.getText();
        }


        protected Action[] createActions() {
            return new Action[]{getOKAction(), new CopyToClipboardAction(), getCancelAction()};
        }

        protected String getDimensionServiceKey() {
            return "#com.intellij.ide.util.ExportDialog";
        }

        protected class CopyToClipboardAction extends AbstractAction {
            public CopyToClipboardAction() {
                super(IdeBundle.message("button.copy"));
                putValue(Action.SHORT_DESCRIPTION, IdeBundle.message("description.copy.text.to.clipboard"));
            }

            public void actionPerformed(ActionEvent e) {
                String s = StringUtil.convertLineSeparators(getText());
                CopyPasteManager.getInstance().setContents(new StringSelection(s));
            }
        }
    }
}
