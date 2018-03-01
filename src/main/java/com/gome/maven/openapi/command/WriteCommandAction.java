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
package com.gome.maven.openapi.command;

import com.gome.maven.codeInsight.FileModificationService;
import com.gome.maven.openapi.application.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.ThrowableComputable;
import com.gome.maven.psi.PsiFile;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

public abstract class WriteCommandAction<T> extends BaseActionRunnable<T> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.command.WriteCommandAction");
    private final String myCommandName;
    private final String myGroupID;
    private final Project myProject;
    private final PsiFile[] myPsiFiles;

    protected WriteCommandAction( Project project, PsiFile... files) {
        this(project, "Undefined", files);
    }

    protected WriteCommandAction( Project project,   String commandName, PsiFile... files) {
        this(project, commandName, null, files);
    }

    protected WriteCommandAction( final Project project,  final String commandName,  final String groupID, PsiFile... files) {
        myCommandName = commandName;
        myGroupID = groupID;
        myProject = project;
        myPsiFiles = files == null || files.length == 0 ? PsiFile.EMPTY_ARRAY : files;
    }

    public final Project getProject() {
        return myProject;
    }

    public final String getCommandName() {
        return myCommandName;
    }

    public String getGroupID() {
        return myGroupID;
    }

    
    @Override
    public RunResult<T> execute() {
        Application application = ApplicationManager.getApplication();
        if (!application.isDispatchThread() && application.isReadAccessAllowed()) {
            LOG.error("Must not start write action from within read action in the other thread - deadlock is coming");
        }
        final RunResult<T> result = new RunResult<T>(this);

        try {
            if (application.isDispatchThread()) {
                performWriteCommandAction(result);
            }
            else {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        performWriteCommandAction(result);
                    }
                });
            }
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause()); // save both stacktraces: current & EDT
        }
        catch (InterruptedException ignored) { }
        return result;
    }

    public static boolean ensureFilesWritable( final Project project,  final Collection<PsiFile> psiFiles) {
        return FileModificationService.getInstance().preparePsiElementsForWrite(psiFiles);
    }

    private void performWriteCommandAction( final RunResult<T> result) {
        if (!FileModificationService.getInstance().preparePsiElementsForWrite(Arrays.asList(myPsiFiles))) return;

        // this is needed to prevent memory leak, since the command is put into undo queue
        final RunResult[] results = {result};

        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        results[0].run();
                        results[0] = null;
                    }
                });
            }
        }, getCommandName(), getGroupID(), getUndoConfirmationPolicy());
    }

    protected boolean isGlobalUndoAction() {
        return false;
    }

    protected UndoConfirmationPolicy getUndoConfirmationPolicy() {
        return UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION;
    }

    public void performCommand() throws Throwable {
        //this is needed to prevent memory leak, since command
        // is put into undo queue
        final RunResult[] results = {new RunResult<T>(this)};
        final Ref<Throwable> exception = new Ref<Throwable>();

        CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
            @Override
            public void run() {
                if (isGlobalUndoAction()) CommandProcessor.getInstance().markCurrentCommandAsGlobal(myProject);
                exception.set(results[0].run().getThrowable());
                results[0] = null;
            }
        }, getCommandName(), getGroupID(), getUndoConfirmationPolicy());

        Throwable throwable = exception.get();
        if (throwable != null) throw throwable;
    }

    /**
     * WriteCommandAction without result
     */
    public abstract static class Simple<T> extends WriteCommandAction<T> {
        protected Simple(final Project project, PsiFile... files) {
            super(project, files);
        }
        protected Simple(final Project project, final String commandName, final PsiFile... files) {
            super(project, commandName, files);
        }

        protected Simple(final Project project, final String name, final String groupID, final PsiFile... files) {
            super(project, name, groupID, files);
        }

        @Override
        protected void run( final Result<T> result) throws Throwable {
            run();
        }

        protected abstract void run() throws Throwable;
    }

    public static void runWriteCommandAction(Project project,  final Runnable runnable) {
        new Simple(project) {
            @Override
            protected void run() throws Throwable {
                runnable.run();
            }
        }.execute();
    }

    public static <T> T runWriteCommandAction(Project project,  final Computable<T> computable) {
        return new WriteCommandAction<T>(project) {
            @Override
            protected void run( Result<T> result) throws Throwable {
                result.setResult(computable.compute());
            }
        }.execute().getResultObject();
    }

    public static <T, E extends Throwable> T runWriteCommandAction(Project project,  final ThrowableComputable<T, E> computable) throws E {
        RunResult<T> result = new WriteCommandAction<T>(project,"") {
            @Override
            protected void run( Result<T> result) throws Throwable {
                result.setResult(computable.compute());
            }
        }.execute();
        if (result.getThrowable() instanceof Throwable) throw (E)result.getThrowable();
        return result.throwException().getResultObject();
    }
}

