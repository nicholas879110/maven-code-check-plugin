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

package com.gome.maven.codeInsight.actions;

import com.gome.maven.codeInsight.CodeInsightBundle;
import com.gome.maven.lang.ImportOptimizer;
import com.gome.maven.lang.LanguageImportStatements;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.EmptyRunnable;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.impl.source.codeStyle.CodeStyleManagerImpl;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;

import static com.gome.maven.codeInsight.actions.OptimizeImportsProcessor.NotificationInfo.*;

public class OptimizeImportsProcessor extends AbstractLayoutCodeProcessor {
    private static final String PROGRESS_TEXT = CodeInsightBundle.message("progress.text.optimizing.imports");
    public static final String COMMAND_NAME = CodeInsightBundle.message("process.optimize.imports");
    private List<NotificationInfo> myOptimizerNotifications = ContainerUtil.newSmartList();

    public OptimizeImportsProcessor(Project project) {
        super(project, COMMAND_NAME, PROGRESS_TEXT, false);
    }

    public OptimizeImportsProcessor(Project project, Module module) {
        super(project, module, COMMAND_NAME, PROGRESS_TEXT, false);
    }

    public OptimizeImportsProcessor(Project project, PsiDirectory directory, boolean includeSubdirs) {
        super(project, directory, includeSubdirs, PROGRESS_TEXT, COMMAND_NAME, false);
    }

    public OptimizeImportsProcessor(Project project, PsiDirectory directory, boolean includeSubdirs, boolean processOnlyVcsChangedFiles) {
        super(project, directory, includeSubdirs, PROGRESS_TEXT, COMMAND_NAME, processOnlyVcsChangedFiles);
    }

    public OptimizeImportsProcessor(Project project, PsiFile file) {
        super(project, file, PROGRESS_TEXT, COMMAND_NAME, false);
    }

    public OptimizeImportsProcessor(Project project, PsiFile[] files, Runnable postRunnable) {
        this(project, files, COMMAND_NAME, postRunnable);
    }

    public OptimizeImportsProcessor(Project project, PsiFile[] files, String commandName, Runnable postRunnable) {
        super(project, files, PROGRESS_TEXT, commandName, postRunnable, false);
    }

    public OptimizeImportsProcessor(AbstractLayoutCodeProcessor processor) {
        super(processor, COMMAND_NAME, PROGRESS_TEXT);
    }

    @Override
    
    protected FutureTask<Boolean> prepareTask( PsiFile file, boolean processChangedTextOnly) {
        final Set<ImportOptimizer> optimizers = LanguageImportStatements.INSTANCE.forFile(file);
        final List<Runnable> runnables = new ArrayList<Runnable>();
        List<PsiFile> files = file.getViewProvider().getAllFiles();
        for (ImportOptimizer optimizer : optimizers) {
            for (PsiFile psiFile : files) {
                if (optimizer.supports(psiFile)) {
                    runnables.add(optimizer.processFile(psiFile));
                }
            }
        }

        Runnable runnable = !runnables.isEmpty() ? new Runnable() {
            @Override
            public void run() {
                CodeStyleManagerImpl.setSequentialProcessingAllowed(false);
                try {
                    for (Runnable runnable : runnables) {
                        runnable.run();
                        retrieveAndStoreNotificationInfo(runnable);
                    }
                    putNotificationInfoIntoCollector();
                }
                finally {
                    CodeStyleManagerImpl.setSequentialProcessingAllowed(true);
                }
            }
        } : EmptyRunnable.getInstance();
        return new FutureTask<Boolean>(runnable, true);
    }

    private void retrieveAndStoreNotificationInfo( Runnable runnable) {
        if (runnable instanceof ImportOptimizer.CollectingInfoRunnable) {
            String optimizerMessage = ((ImportOptimizer.CollectingInfoRunnable)runnable).getUserNotificationInfo();
            myOptimizerNotifications.add(optimizerMessage != null ? new NotificationInfo(optimizerMessage) : NOTHING_CHANGED_NOTIFICATION);
        }
        else if (runnable == EmptyRunnable.getInstance()) {
            myOptimizerNotifications.add(NOTHING_CHANGED_NOTIFICATION);
        }
        else {
            myOptimizerNotifications.add(SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION);
        }
    }

    private void putNotificationInfoIntoCollector() {
        LayoutCodeInfoCollector collector = getInfoCollector();
        if (collector == null) {
            return;
        }

        boolean atLeastOneOptimizerChangedSomething = false;
        for (NotificationInfo info : myOptimizerNotifications) {
            atLeastOneOptimizerChangedSomething |= info.isSomethingChanged();
            if (info.getMessage() != null) {
                collector.setOptimizeImportsNotification(info.getMessage());
                return;
            }
        }

        collector.setOptimizeImportsNotification(atLeastOneOptimizerChangedSomething ? "imports optimized" : null);
    }

    static class NotificationInfo {
        public static final NotificationInfo NOTHING_CHANGED_NOTIFICATION = new NotificationInfo(false, null);
        public static final NotificationInfo SOMETHING_CHANGED_WITHOUT_MESSAGE_NOTIFICATION = new NotificationInfo(true, null);

        private final boolean mySomethingChanged;
        private final String myMessage;

        NotificationInfo( String message) {
            this(true, message);
        }

        public boolean isSomethingChanged() {
            return mySomethingChanged;
        }

        public String getMessage() {
            return myMessage;
        }

        private NotificationInfo(boolean isSomethingChanged,  String message) {
            mySomethingChanged = isSomethingChanged;
            myMessage = message;
        }
    }
}
