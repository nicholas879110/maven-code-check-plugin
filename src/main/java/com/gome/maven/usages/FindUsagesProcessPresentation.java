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
package com.gome.maven.usages;

import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class FindUsagesProcessPresentation {
    
    public static final String NAME_WITH_MNEMONIC_KEY = "NameWithMnemonic";
    private final UsageViewPresentation myUsageViewPresentation;

    private List<Action> myNotFoundActions;
    private boolean myShowPanelIfOnlyOneUsage;
    private boolean myShowNotFoundMessage;
    private Factory<ProgressIndicator> myProgressIndicatorFactory;
    private Collection<VirtualFile> myLargeFiles;
    private boolean myShowFindOptionsPrompt = true;
    private Runnable mySearchWithProjectFiles;
    private boolean myCanceled;

    public FindUsagesProcessPresentation( UsageViewPresentation presentation) {
        myUsageViewPresentation = presentation;
    }

    public void addNotFoundAction( Action action) {
        if (myNotFoundActions == null) myNotFoundActions = new ArrayList<Action>();
        myNotFoundActions.add(action);
    }

    
    public List<Action> getNotFoundActions() {
        return myNotFoundActions == null ? Collections.<Action>emptyList() : myNotFoundActions;
    }

    public boolean isShowNotFoundMessage() {
        return myShowNotFoundMessage;
    }

    public void setShowNotFoundMessage(final boolean showNotFoundMessage) {
        myShowNotFoundMessage = showNotFoundMessage;
    }

    public boolean isShowPanelIfOnlyOneUsage() {
        return myShowPanelIfOnlyOneUsage;
    }

    public void setShowPanelIfOnlyOneUsage(final boolean showPanelIfOnlyOneUsage) {
        myShowPanelIfOnlyOneUsage = showPanelIfOnlyOneUsage;
    }

    public Factory<ProgressIndicator> getProgressIndicatorFactory() {
        return myProgressIndicatorFactory;
    }

    public void setProgressIndicatorFactory( Factory<ProgressIndicator> progressIndicatorFactory) {
        myProgressIndicatorFactory = progressIndicatorFactory;
    }

    
    public Runnable searchIncludingProjectFileUsages() {
        return mySearchWithProjectFiles;
    }

    public void projectFileUsagesFound( Runnable searchWithProjectFiles) {
        mySearchWithProjectFiles = searchWithProjectFiles;
    }

    public void setLargeFilesWereNotScanned( Collection<VirtualFile> largeFiles) {
        myLargeFiles = largeFiles;
    }

    
    public Collection<VirtualFile> getLargeFiles() {
        return myLargeFiles == null ? Collections.<VirtualFile>emptyList() : myLargeFiles;
    }

    public boolean isShowFindOptionsPrompt() {
        return myShowFindOptionsPrompt;
    }

    
    public UsageViewPresentation getUsageViewPresentation() {
        return myUsageViewPresentation;
    }

    public void setShowFindOptionsPrompt(boolean showFindOptionsPrompt) {
        myShowFindOptionsPrompt = showFindOptionsPrompt;
    }


    public void setCanceled(boolean canceled) {
        myCanceled = canceled;
    }

    public boolean isCanceled() {
        return myCanceled;
    }
}


