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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Aug 20, 2006
 * Time: 8:40:15 PM
 */
package com.gome.maven.openapi.progress.impl;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.progress.PerformInBackgroundOption;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.progress.TaskInfo;
//import com.gome.maven.openapi.progress.util.ProgressWindow;
import com.gome.maven.openapi.progress.util.ProgressIndicatorBase;
import com.gome.maven.openapi.project.*;
import com.gome.maven.openapi.util.Disposer;
//import com.gome.maven.openapi.wm.IdeFrame;
//import com.gome.maven.openapi.wm.WindowManager;
//import com.gome.maven.openapi.wm.ex.StatusBarEx;
//import com.gome.maven.openapi.wm.ex.WindowManagerEx;
//import org.jetbrains.annotations.Nls;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;

public class BackgroundableProcessIndicator extends ProgressIndicatorBase   /*extends ProgressWindow*/ {
//    protected StatusBarEx myStatusBar;

    @SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized"})

    private PerformInBackgroundOption myOption;
    private TaskInfo myInfo;

    private boolean myDisposed;
    private DumbModeAction myDumbModeAction = DumbModeAction.NOTHING;

    public BackgroundableProcessIndicator( Task.Backgroundable task) {
        this(task.getProject(), task, task);

        myDumbModeAction = task.getDumbModeAction();
//        if (myDumbModeAction == DumbModeAction.CANCEL) {
//            task.getProject().getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
//
//                @Override
//                public void enteredDumbMode() {
//                    cancel();
//                }
//
//                @Override
//                public void exitDumbMode() {
//                }
//            });
//        }
    }

    public BackgroundableProcessIndicator( final Project project,  TaskInfo info,  PerformInBackgroundOption option) {
//        super(info.isCancellable(), true, project, info.getCancelText());
//        if (project != null) {
//            final ProjectManagerAdapter myListener = new ProjectManagerAdapter() {
//                @Override
//                public void projectClosing(Project closingProject) {
//                    if (isRunning()) {
//                        cancel();
//                    }
//                }
//            };
//            ProjectManager.getInstance().addProjectManagerListener(project, myListener);
//            Disposer.register(this, new Disposable() {
//                @Override
//                public void dispose() {
//                    ProjectManager.getInstance().removeProjectManagerListener(project, myListener);
//                }
//            });
//        }
//        setOwnerTask(info);
//        setProcessId(info.getProcessId());
//        myOption = option;
//        myInfo = info;
//        setTitle(info.getTitle());
//        final Project nonDefaultProject = project == null || project.isDisposed() ? null : project.isDefault() ? null : project;
//        final IdeFrame frame = ((WindowManagerEx)WindowManager.getInstance()).findFrameFor(nonDefaultProject);
//        myStatusBar = frame != null ? (StatusBarEx)frame.getStatusBar() : null;
//        myBackgrounded = shouldStartInBackground();
//        if (myBackgrounded) {
//            doBackground();
//        }
    }

    private boolean shouldStartInBackground() {
//        return myOption.shouldStartInBackground() && myStatusBar != null;
        return false;
    }

    public BackgroundableProcessIndicator(Project project,
                                           final String progressTitle,
                                           PerformInBackgroundOption option,
                                           final String cancelButtonText,
                                           final String backgroundStopTooltip, final boolean cancellable) {
        this(project, new TaskInfo() {
            @Override
            public String getProcessId() {
                return "<unknown>";
            }

            @Override
            
            public String getTitle() {
                return progressTitle;
            }

            @Override
            public String getCancelText() {
                return cancelButtonText;
            }

            @Override
            public String getCancelTooltipText() {
                return backgroundStopTooltip;
            }

            @Override
            public boolean isCancellable() {
                return cancellable;
            }
        }, option);
    }

    public DumbModeAction getDumbModeAction() {
        return myDumbModeAction;
    }

//    @Override
//    protected void showDialog() {
//        if (myDisposed) return;
//
//        if (shouldStartInBackground()) {
//            return;
//        }
//
//        super.showDialog();
//    }
//
//    @Override
//    public void background() {
//        if (myDisposed) return;
//
//        myOption.processSentToBackground();
//        doBackground();
//        super.background();
//    }

//    private void doBackground() {
//        if (myStatusBar != null) { //not welcome screen
//            myStatusBar.addProgress(this, myInfo);
//        }
//    }

//    @Override
//    public void dispose() {
//        super.dispose();
//        myDisposed = true;
//        myInfo = null;
//        myStatusBar = null;
//        myOption = null;
//    }
//
//    @Override
//    public boolean isShowing() {
//        return isModal() || ! isBackgrounded();
//    }
}
