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
package com.gome.maven.openapi.updateSettings.impl;

import com.gome.maven.CommonBundle;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.openapi.application.ex.ApplicationEx;
import com.gome.maven.openapi.application.ex.ApplicationManagerEx;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.ui.DialogWrapper;
import com.gome.maven.ui.ColorUtil;
import com.gome.maven.ui.IdeBorderFactory;
import com.gome.maven.ui.LicensingFacade;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

/**
 * @author anna
 */
public abstract class AbstractUpdateDialog extends DialogWrapper {
    private final boolean myEnableLink;

    protected String myLicenseInfo = null;
    protected boolean myPaidUpgrade;
    protected boolean mySubscriptionLicense = false;

    protected AbstractUpdateDialog(boolean enableLink) {
        super(true);
        myEnableLink = enableLink;
        setTitle(IdeBundle.message("update.notifications.title"));
    }

    protected AbstractUpdateDialog(Component parent, boolean enableLink) {
        super(parent, true);
        myEnableLink = enableLink;
        setTitle(IdeBundle.message("update.notifications.title"));
    }

    @Override
    protected void init() {
        setOKButtonText(getOkButtonText());
        setCancelButtonText(getCancelButtonText());
        super.init();
    }

    protected String getOkButtonText() {
        return CommonBundle.getOkButtonText();
    }

    protected String getCancelButtonText() {
        return CommonBundle.getCancelButtonText();
    }

    protected void restart() {
        final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        // do not stack several modal dialogs (native & swing)
        app.invokeLater(new Runnable() {
            @Override
            public void run() {
                app.restart(true);
            }
        });
    }

    protected void initLicensingInfo( UpdateChannel channel,  BuildInfo build) {
        LicensingFacade facade = LicensingFacade.getInstance();
        if (facade != null) {
            mySubscriptionLicense = facade.isSubscriptionLicense();
            if (!channel.getLicensing().equals(UpdateChannel.LICENSING_EAP)) {
                int majorVersion = build.getMajorVersion();
                if (majorVersion < 0) {
                    majorVersion = channel.getMajorVersion(); // fallback
                }
                final Boolean paidUpgrade = facade.isPaidUpgrade(majorVersion, build.getReleaseDate());
                if (paidUpgrade == Boolean.TRUE) {
                    myPaidUpgrade = true;
                    myLicenseInfo = IdeBundle.message("updates.channel.key.needed", channel.getEvalDays());
                }
                else if (paidUpgrade == Boolean.FALSE) {
                    myLicenseInfo = IdeBundle.message("updates.channel.existing.key");
                }
            }
            else {
                myLicenseInfo = IdeBundle.message("updates.channel.bundled.key");
            }
        }
    }


    protected void configureMessageArea( JEditorPane area) {
        String messageBody = myEnableLink ? IdeBundle.message("updates.configure.label", ShowSettingsUtil.getSettingsMenuName()) : "";
        configureMessageArea(area, messageBody, null, null);
    }

    protected void configureMessageArea(final  JEditorPane area,
                                         String messageBody,
                                         Color fontColor,
                                         HyperlinkListener listener) {
        String text = "<html><head>" +
                UIUtil.getCssFontDeclaration(UIUtil.getLabelFont(), fontColor, null, null) +
                "<style>body {background: #" + ColorUtil.toHex(UIUtil.getPanelBackground()) + ";}</style>" +
                "</head><body>" +
                messageBody +
                "</body></html>";

        area.setBackground(UIUtil.getPanelBackground());
        area.setBorder(IdeBorderFactory.createEmptyBorder());
        area.setText(text);
        area.setEditable(false);

        if (listener == null && myEnableLink) {
            listener = new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(final HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        UpdateSettingsConfigurable settings = new UpdateSettingsConfigurable();
                        settings.setCheckNowEnabled(false);
                        ShowSettingsUtil.getInstance().editConfigurable(area, settings);
                    }
                }
            };
        }
        if (listener != null) {
            area.addHyperlinkListener(listener);
        }
    }
}
