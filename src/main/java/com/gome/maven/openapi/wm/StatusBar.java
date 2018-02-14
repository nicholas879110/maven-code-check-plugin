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
package com.gome.maven.openapi.wm;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.messages.Topic;

import javax.swing.*;
import java.awt.*;

/**
 * @author spleaner
 */
public interface StatusBar extends StatusBarInfo, Disposable {

    @SuppressWarnings({"AbstractClassNeverImplemented"})
    abstract class Info implements StatusBarInfo {
        public static final Topic<StatusBarInfo> TOPIC = Topic.create("IdeStatusBar.Text", StatusBarInfo.class);

        private Info() {
        }

        public static void set( final String text,  final Project project) {
            set(text, project, null);
        }

        public static void set( final String text,  final Project project,  final String requestor) {
            if (project != null) {
                if (project.isDisposed()) return;
                if (!project.isInitialized()) {
                    StartupManager.getInstance(project).runWhenProjectIsInitialized(new Runnable() {
                        public void run() {
                            project.getMessageBus().syncPublisher(TOPIC).setInfo(text, requestor);
                        }
                    });
                    return;
                }
            }

            final MessageBus bus = project == null ? ApplicationManager.getApplication().getMessageBus() : project.getMessageBus();
            bus.syncPublisher(TOPIC).setInfo(text, requestor);
        }
    }

    void addWidget( StatusBarWidget widget);

    void addWidget( StatusBarWidget widget,  String anchor);

    void addWidget( StatusBarWidget widget,  Disposable parentDisposable);

    void addWidget( StatusBarWidget widget,  String anchor,  Disposable parentDisposable);

    /**
     * @deprecated use addWidget instead
     */
    @Deprecated
    void addCustomIndicationComponent( JComponent c);

    /**
     * @deprecated use removeWidget instead
     */
    @Deprecated
    void removeCustomIndicationComponent( JComponent c);

    void removeWidget( String id);

    void updateWidget( String id);

    
    StatusBarWidget getWidget(String id);

    void fireNotificationPopup( JComponent content, Color backgroundColor);

    StatusBar createChild();

    JComponent getComponent();

    StatusBar findChild(Component c);

    IdeFrame getFrame();

    void install(IdeFrame frame);

}
