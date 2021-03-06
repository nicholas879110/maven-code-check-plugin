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
package com.gome.maven.diagnostic;

import com.gome.maven.openapi.application.ApplicationNamesInfo;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.ui.JBColor;
import com.gome.maven.ui.components.JBLabel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class OutOfMemoryDialog extends DialogWrapper {
    public enum MemoryKind {
        HEAP, PERM_GEN, CODE_CACHE
    }

    private final MemoryKind myMemoryKind;

    private JPanel myContentPane;
    private JLabel myMessageLabel;
    private JTextField myHeapSizeField;
    private JTextField myPermGenSizeField;
    private JBLabel myHeapCurrentValueLabel;
    private JBLabel myPermGenCurrentValueLabel;
    private JLabel myHeapSizeLabel;
    private JLabel myPermGenSizeLabel;
    private JLabel myHeapUnitsLabel;
    private JLabel myPermGenUnitsLabel;
    private JBLabel mySettingsFileHintLabel;
    private JLabel myCodeCacheSizeLabel;
    private JTextField myCodeCacheSizeField;
    private JLabel myCodeCacheUnitsLabel;
    private JBLabel myCodeCacheCurrentValueLabel;
    private final Action myIgnoreAction;
    private final Action myShutdownAction;

    public OutOfMemoryDialog(MemoryKind memoryKind) {
        super(false);
        myMemoryKind = memoryKind;
        setTitle(DiagnosticBundle.message("diagnostic.out.of.memory.title"));

        myMessageLabel.setIcon(Messages.getErrorIcon());
        myMessageLabel.setText(DiagnosticBundle.message(
                "diagnostic.out.of.memory.error",
                memoryKind == MemoryKind.HEAP ? VMOptions.XMX_OPTION_NAME : memoryKind == MemoryKind.PERM_GEN ? VMOptions.PERM_GEN_OPTION_NAME : VMOptions.CODE_CACHE_OPTION_NAME,
                ApplicationNamesInfo.getInstance().getProductName()));

        File file = VMOptions.getWriteFile();
        if (file != null) {
            mySettingsFileHintLabel.setText(DiagnosticBundle.message("diagnostic.out.of.memory.willBeSavedTo", file.getPath()));
        }
        else {
            mySettingsFileHintLabel.setVisible(false);
        }

        myIgnoreAction = new AbstractAction(DiagnosticBundle.message("diagnostic.out.of.memory.ignore")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
                close(0);
            }
        };

        myShutdownAction = new AbstractAction(DiagnosticBundle.message("diagnostic.out.of.memory.shutdown")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
                System.exit(0);
            }
        };
        myShutdownAction.putValue(DialogWrapper.DEFAULT_ACTION, true);

        configControls(VMOptions.XMX_OPTION_NAME,
                VMOptions.readXmx(),
                memoryKind == MemoryKind.HEAP,
                myHeapSizeLabel,
                myHeapSizeField,
                myHeapUnitsLabel,
                myHeapCurrentValueLabel);

        configControls(VMOptions.PERM_GEN_OPTION_NAME,
                VMOptions.readMaxPermGen(),
                memoryKind == MemoryKind.PERM_GEN,
                myPermGenSizeLabel,
                myPermGenSizeField,
                myPermGenUnitsLabel,
                myPermGenCurrentValueLabel);

        configControls(VMOptions.CODE_CACHE_OPTION_NAME,
                VMOptions.readCodeCache(),
                memoryKind == MemoryKind.CODE_CACHE,
                myCodeCacheSizeLabel,
                myCodeCacheSizeField,
                myCodeCacheUnitsLabel,
                myCodeCacheCurrentValueLabel);

        init();
    }

    private static void configControls(String optionName,
                                       int value,
                                       boolean highlight,
                                       JLabel sizeLabel,
                                       JTextField sizeField,
                                       JLabel unitsLabel,
                                       JLabel currentValueLabel) {
        sizeLabel.setText(optionName);

        String formatted = value == -1 ? DiagnosticBundle.message("diagnostic.out.of.memory.currentValue.unknown") : String.valueOf(value);
        sizeField.setText(formatted);
        currentValueLabel.setText(DiagnosticBundle.message("diagnostic.out.of.memory.currentValue", formatted));

        if (highlight) {
            sizeLabel.setForeground(JBColor.RED);
            sizeField.setForeground(JBColor.RED);
            unitsLabel.setForeground(JBColor.RED);
            currentValueLabel.setForeground(JBColor.RED);
        }
    }

    private void save() {
        try {
            VMOptions.writeXmx(Integer.parseInt(myHeapSizeField.getText()));
        }
        catch (NumberFormatException ignored) { }

        try {
            VMOptions.writeMaxPermGen(Integer.parseInt(myPermGenSizeField.getText()));
        }
        catch (NumberFormatException ignored) { }

        try {
            VMOptions.writeCodeCache(Integer.parseInt(myCodeCacheSizeField.getText()));
        }
        catch (NumberFormatException ignored) { }
    }

    @Override
    protected JComponent createCenterPanel() {
        return myContentPane;
    }

    
    @Override
    protected Action[] createActions() {
        return new Action[]{myShutdownAction, myIgnoreAction};
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myMemoryKind == MemoryKind.HEAP ? myHeapSizeField :
                myMemoryKind == MemoryKind.PERM_GEN ? myPermGenSizeField :
                        myCodeCacheSizeField;
    }
}
