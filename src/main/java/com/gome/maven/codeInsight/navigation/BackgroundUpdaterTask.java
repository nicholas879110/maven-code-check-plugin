/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.navigation;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.progress.PerformInBackgroundOption;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopupAdapter;
import com.gome.maven.openapi.ui.popup.LightweightWindowEvent;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.ui.popup.AbstractPopup;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usages.UsageInfo2UsageAdapter;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.impl.UsageViewImpl;
import com.gome.maven.util.Alarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: anna
 */
public abstract class BackgroundUpdaterTask<T> extends Task.Backgroundable {
    protected AbstractPopup myPopup;
    protected T myComponent;
    private Ref<UsageView> myUsageView;
    private final List<PsiElement> myData = new ArrayList<PsiElement>();

    private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
    private final Object lock = new Object();

    private volatile boolean myCanceled = false;

    public BackgroundUpdaterTask(Project project, String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    public BackgroundUpdaterTask(Project project, String title) {
        super(project, title);
    }

    public BackgroundUpdaterTask(Project project,
                                 String title,
                                 boolean canBeCancelled,
                                 PerformInBackgroundOption backgroundOption) {
        super(project, title, canBeCancelled, backgroundOption);
    }

    public void init( AbstractPopup popup, T component, Ref<UsageView> usageView) {
        myPopup = popup;
        myComponent = component;
        myUsageView = usageView;

    }

    public abstract String getCaption(int size);
    protected abstract void replaceModel( List<PsiElement> data);
    protected abstract void paintBusy(boolean paintBusy);

    public boolean setCanceled() {
        boolean canceled = myCanceled;
        myCanceled = true;
        return canceled;
    }

    public boolean isCanceled() {
        return myCanceled;
    }

    public boolean updateComponent(final PsiElement element,  final Comparator comparator) {
        final UsageView view = myUsageView.get();
        if (view != null && !((UsageViewImpl)view).isDisposed()) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    view.appendUsage(new UsageInfo2UsageAdapter(new UsageInfo(element)));
                }
            });
            return true;
        }

        if (myCanceled) return false;
        if (myPopup.isDisposed()) return false;

        synchronized (lock) {
            if (myData.contains(element)) return true;
            myData.add(element);
        }

        myAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                myAlarm.cancelAllRequests();
                if (myCanceled) return;
                if (myPopup.isDisposed()) return;
                ArrayList<PsiElement> data = new ArrayList<PsiElement>();
                synchronized (lock) {
                    if (comparator != null) {
                        Collections.sort(myData, comparator);
                    }
                    data.addAll(myData);
                }
                replaceModel(data);
                myPopup.setCaption(getCaption(getCurrentSize()));
                myPopup.pack(true, true);
            }
        }, 200, ModalityState.stateForComponent(myPopup.getContent()));
        return true;
    }

    public int getCurrentSize() {
        synchronized (lock) {
            return myData.size();
        }
    }

    @Override
    public void run( ProgressIndicator indicator) {
        paintBusy(true);
    }

    @Override
    public void onSuccess() {
        myPopup.setCaption(getCaption(getCurrentSize()));
        paintBusy(false);
    }
}
