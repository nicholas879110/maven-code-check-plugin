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
package com.gome.maven.openapi.wm.ex;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.TaskInfo;
import com.gome.maven.openapi.ui.MessageType;
import com.gome.maven.openapi.ui.popup.BalloonHandler;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.wm.StatusBar;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;

/**
 * @author spleaner
 */
public interface StatusBarEx extends StatusBar, Disposable {
    void startRefreshIndication(String tooltipText);
    void stopRefreshIndication();

    BalloonHandler notifyProgressByBalloon( MessageType type,  String htmlBody);
    BalloonHandler notifyProgressByBalloon( MessageType type,  String htmlBody,  Icon icon,  HyperlinkListener listener);

    void addProgress( ProgressIndicatorEx indicator,  TaskInfo info);
    List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses();

    void updateWidgets();

    boolean isProcessWindowOpen();

    void setProcessWindowOpen(boolean open);

    @Deprecated
    void removeCustomIndicationComponents();

    Dimension getSize();
    boolean isVisible();

    
    String getInfoRequestor();
}
