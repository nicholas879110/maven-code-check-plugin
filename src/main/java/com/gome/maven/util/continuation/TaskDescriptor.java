/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.gome.maven.util.continuation;

import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;

import java.util.HashMap;
import java.util.Map;

public abstract class TaskDescriptor {
    // this also means that it would be called in case of chain cancel()
    private boolean myHaveMagicCure;
    private final String myName;
    
    private final Where myWhere;
    private final Map<Object, Object> mySurviveKit;

    public TaskDescriptor(final String name,  final Where where) {
        myName = name;
        myWhere = where;
        mySurviveKit = new HashMap<Object, Object>();
    }

    public abstract void run(final ContinuationContext context);

    public final void addCure(final Object disaster, final Object cure) {
        mySurviveKit.put(disaster, cure);
    }

    public final Object hasCure(final Object disaster) {
        return mySurviveKit.get(disaster);
    }

    public String getName() {
        return myName;
    }

    
    public Where getWhere() {
        return myWhere;
    }

    public boolean isHaveMagicCure() {
        return myHaveMagicCure;
    }

    public void setHaveMagicCure(boolean haveMagicCure) {
        myHaveMagicCure = haveMagicCure;
    }

    public void canceled() {
    }

    public static TaskDescriptor createForBackgroundableTask( final Task.Backgroundable backgroundable) {
        return new TaskDescriptor(backgroundable.getTitle(), Where.POOLED) {
            @Override
            public void run(ContinuationContext context) {
                final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                try {
                    backgroundable.run(indicator);
                } catch (ProcessCanceledException e) {
                    //
                }
                final boolean canceled = indicator.isCanceled();
                context.next(new TaskDescriptor("", Where.AWT) {
                    @Override
                    public void run(ContinuationContext context) {
                        if (canceled) {
                            backgroundable.onCancel();
                        } else {
                            backgroundable.onSuccess();
                        }
                    }
                });
            }
        };
    }
}
