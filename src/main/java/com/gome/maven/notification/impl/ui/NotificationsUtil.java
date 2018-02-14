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
package com.gome.maven.notification.impl.ui;

import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.ui.ColorUtil;
import com.gome.maven.ui.JBColor;
import com.gome.maven.xml.util.XmlStringUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

/**
 * @author spleaner
 */
public class NotificationsUtil {

    private NotificationsUtil() {
    }

    public static String buildHtml( final Notification notification,  String style) {
        String result = "";
        if (style != null) {
            result += "<div style=\"" + style + "\">";
        }
        result += "<b color=\"#"+ ColorUtil.toHex(getMessageType(notification).getTitleForeground())+"\">" + notification.getTitle() + "</b>" +
                "<p>" + notification.getContent() + "</p>";
        if (style != null) {
            result += "</div>";
        }
        return XmlStringUtil.wrapInHtml(result);
    }

    public static String buildHtml( final String title,  final String content,  String style) {
        String result = "";
        if (style != null) {
            result += "<div style=\"" + style + "\">";
        }
        result += "<b>" + title + "</b><p>" + content + "</p>";
        if (style != null) {
            result += "</div>";
        }
        return XmlStringUtil.wrapInHtml(result);
    }

    
    public static HyperlinkListener wrapListener( final Notification notification) {
        final NotificationListener listener = notification.getListener();
        if (listener == null) return null;

        return new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    final NotificationListener listener1 = notification.getListener();
                    if (listener1 != null) {
                        listener1.hyperlinkUpdate(notification, e);
                    }
                }
            }
        };
    }

    public static Icon getIcon( final Notification notification) {
        Icon icon = notification.getIcon();

        if (icon == null) {
            icon = getMessageType(notification).getDefaultIcon();
        }

        return icon;
    }

    public static MessageType getMessageType( Notification notification) {
        switch (notification.getType()) {
            case WARNING: return MessageType.WARNING;
            case ERROR: return MessageType.ERROR;
            case INFORMATION:
            default: return MessageType.INFO;
        }
    }

    public static Color getBackground( final Notification notification) {
        return getMessageType(notification).getPopupBackground();
    }

    public static Color getBorderColor(Notification notification) {
        switch (notification.getType()) {
            case ERROR:
                return new JBColor(Color.gray, new Color(0xc8c8c8));
            case WARNING:
                return new JBColor(Color.gray, new Color(0x615f51));
            case INFORMATION:
            default:
                return new JBColor(Color.gray, new Color(0x205c00));
        }
    }
}
