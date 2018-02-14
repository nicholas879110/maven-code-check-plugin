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
package com.gome.maven.util.net;

import com.gome.maven.CommonBundle;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.ui.GuiUtils;
import com.gome.maven.util.ObjectUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;

public class IOExceptionDialog extends DialogWrapper {
    private static final Logger LOG = Logger.getInstance(IOExceptionDialog.class);
    private final JTextArea myErrorLabel;

    public IOExceptionDialog(String title, String errorText)  {
        super((Project)null, true);
        setTitle(title);
        setOKButtonText(CommonBundle.message("dialog.ioexception.tryagain"));

        myErrorLabel = new JTextArea();
        myErrorLabel.setText(errorText);
        myErrorLabel.setFont(UIManager.getFont("Label.font"));
        myErrorLabel.setBackground(UIManager.getColor("Label.background"));
        myErrorLabel.setForeground(UIManager.getColor("Label.foreground"));

        init();
    }

    
    @Override
    protected JComponent createCenterPanel() {
        return myErrorLabel;
    }

    
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[] {
                new AbstractAction(CommonBundle.message("dialog.ioexception.proxy")) {
                    @Override
                    public void actionPerformed( ActionEvent e) {
                        HttpConfigurable.editConfigurable(ObjectUtils.tryCast(e.getSource(), JComponent.class));
                    }
                }
        };
    }

    /**
     * Show the dialog
     * @return <code>true</code> if "Try Again" button pressed and <code>false</code> if "Cancel" button pressed
     */
    public static boolean showErrorDialog(final String title, final String text) {
        final Ref<Boolean> ok = Ref.create(false);
        try {
            GuiUtils.runOrInvokeAndWait(new Runnable() {
                @Override
                public void run() {
                    IOExceptionDialog dialog = new IOExceptionDialog(title, text);
                    dialog.show();
                    ok.set(dialog.isOK());
                }
            });
        }
        catch (InterruptedException e) {
            LOG.info(e);
        }
        catch (InvocationTargetException e) {
            LOG.info(e);
        }

        return ok.get();
    }
}
