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
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.xml.util.XmlStringUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.charset.Charset;

public class IncompatibleEncodingDialog extends DialogWrapper {
     private final VirtualFile virtualFile;
     private final Charset charset;
     private final EncodingUtil.Magic8 safeToReload;
     private final EncodingUtil.Magic8 safeToConvert;

    public IncompatibleEncodingDialog( VirtualFile virtualFile,
                                       final Charset charset,
                                       EncodingUtil.Magic8 safeToReload,
                                       EncodingUtil.Magic8 safeToConvert) {
        super(false);
        this.virtualFile = virtualFile;
        this.charset = charset;
        this.safeToReload = safeToReload;
        this.safeToConvert = safeToConvert;
        setTitle(virtualFile.getName() + ": Reload or Convert to "+charset.displayName());
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        JLabel label = new JLabel(XmlStringUtil.wrapInHtml(
                "The encoding you've chosen ('" + charset.displayName() + "') may change the contents of '" + virtualFile.getName() + "'.<br>" +
                        "Do you want to reload the file from disk or<br>" +
                        "convert the text and save in the new encoding?"));
        label.setIcon(Messages.getQuestionIcon());
        label.setIconTextGap(10);
        return label;
    }

    
    @Override
    protected Action[] createActions() {
        DialogWrapperAction reloadAction = new DialogWrapperAction("Reload") {
            @Override
            protected void doAction(ActionEvent e) {
                if (safeToReload == EncodingUtil.Magic8.NO_WAY) {
                    Pair<Charset,String> detected = EncodingUtil.checkCanReload(virtualFile);
                    String failReason = detected.second;
                    Charset autoDetected = detected.first;
                    int res;
                    byte[] bom = virtualFile.getBOM();
                    if (bom != null) {
                        Messages
                                .showErrorDialog(XmlStringUtil.wrapInHtml(
                                        "File '" + virtualFile.getName() + "' can't be reloaded in the '" + charset.displayName() + "' encoding.<br><br>" +
                                                (failReason == null ? "" : "Why: "+ failReason +"<br>") +
                                                (autoDetected == null ? "" : "Detected encoding: '"+ autoDetected.displayName()+"'")),
                                        "Incompatible Encoding: " + charset.displayName()
                                );
                        res = -1;
                    }
                    else {
                        res = Messages
                                .showDialog(XmlStringUtil.wrapInHtml(
                                        "File '" + virtualFile.getName() + "' most likely isn't stored in the '" + charset.displayName() + "' encoding." +
                                                "<br><br>" +
                                                (failReason == null ? "" : "Why: " + failReason + "<br>") +
                                                (autoDetected == null ? "" : "Detected encoding: '" + autoDetected.displayName() + "'")),
                                        "Incompatible Encoding: " + charset.displayName(), new String[]{"Reload anyway", "Cancel"}, 1,
                                        AllIcons.General.WarningDialog);
                    }
                    if (res != 0) {
                        doCancelAction();
                        return;
                    }
                }
                close(RELOAD_EXIT_CODE);
            }
        };
        if (!SystemInfo.isMac && safeToReload == EncodingUtil.Magic8.NO_WAY) {
            reloadAction.putValue(Action.SMALL_ICON, AllIcons.General.Warning);
        }
        reloadAction.putValue(Action.MNEMONIC_KEY, (int)'R');
        DialogWrapperAction convertAction = new DialogWrapperAction("Convert") {
            @Override
            protected void doAction(ActionEvent e) {
                if (safeToConvert == EncodingUtil.Magic8.NO_WAY) {
                    String error = EncodingUtil.checkCanConvert(virtualFile);
                    int res = Messages.showDialog(XmlStringUtil.wrapInHtml(
                            "Please do not convert to '"+charset.displayName()+"'.<br><br>" +
                                    (error == null ? "Encoding '" + charset.displayName() + "' does not support some characters from the text." : error)),
                            "Incompatible Encoding: " + charset.displayName(), new String[]{"Convert anyway", "Cancel"}, 1,
                            AllIcons.General.WarningDialog);
                    if (res != 0) {
                        doCancelAction();
                        return;
                    }
                }
                close(CONVERT_EXIT_CODE);
            }
        };
        if (!SystemInfo.isMac && safeToConvert == EncodingUtil.Magic8.NO_WAY) {
            convertAction.putValue(Action.SMALL_ICON, AllIcons.General.Warning);
        }
        convertAction.putValue(Action.MNEMONIC_KEY, (int)'C');
        Action cancelAction = getCancelAction();
        cancelAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
        return new Action[]{reloadAction, convertAction, cancelAction};
    }

    public static final int RELOAD_EXIT_CODE = 10;
    public static final int CONVERT_EXIT_CODE = 20;
}
