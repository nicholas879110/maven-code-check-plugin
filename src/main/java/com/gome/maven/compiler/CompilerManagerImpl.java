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
package com.gome.maven.compiler;

import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.compiler.impl.*;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.compiler.*;
import com.gome.maven.openapi.compiler.Compiler;
import com.gome.maven.openapi.compiler.util.InspectionValidator;
import com.gome.maven.openapi.compiler.util.InspectionValidatorWrapper;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.messages.MessageBusConnection;

import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Semaphore;

public class CompilerManagerImpl extends CompilerManager {
    private final Project myProject;

    private final List<Compiler> myCompilers = new ArrayList<Compiler>();

    private final List<CompileTask> myBeforeTasks = new ArrayList<CompileTask>();
    private final List<CompileTask> myAfterTasks = new ArrayList<CompileTask>();
    private final Set<FileType> myCompilableTypes = new HashSet<FileType>();
    private final CompilationStatusListener myEventPublisher;
    private final Semaphore myCompilationSemaphore = new Semaphore(1, true);
    private final Set<ModuleType> myValidationDisabledModuleTypes = new HashSet<ModuleType>();
    private final Set<LocalFileSystem.WatchRequest> myWatchRoots;

    public CompilerManagerImpl(final Project project, MessageBus messageBus) {
        myProject = project;
        myEventPublisher = messageBus.syncPublisher(CompilerTopics.COMPILATION_STATUS);

        // predefined compilers
        for(Compiler compiler: Extensions.getExtensions(Compiler.EP_NAME, myProject)) {
            addCompiler(compiler);
        }
        for(CompilerFactory factory: Extensions.getExtensions(CompilerFactory.EP_NAME, myProject)) {
            Compiler[] compilers = factory.createCompilers(this);
            for (Compiler compiler : compilers) {
                addCompiler(compiler);
            }
        }

        for (InspectionValidator validator : Extensions.getExtensions(InspectionValidator.EP_NAME, myProject)) {
            addCompiler(new InspectionValidatorWrapper(this, InspectionManager.getInstance(project), InspectionProjectProfileManager.getInstance(project), PsiDocumentManager.getInstance(project), PsiManager.getInstance(project), validator));
        }
        addCompilableFileType(StdFileTypes.JAVA);

        final File projectGeneratedSrcRoot = CompilerPaths.getGeneratedDataDirectory(project);
        projectGeneratedSrcRoot.mkdirs();
        final LocalFileSystem lfs = LocalFileSystem.getInstance();
        myWatchRoots = lfs.addRootsToWatch(Collections.singletonList(FileUtil.toCanonicalPath(projectGeneratedSrcRoot.getPath())), true);
        Disposer.register(project, new Disposable() {
            public void dispose() {
                lfs.removeWatchedRoots(myWatchRoots);
                if (ApplicationManager.getApplication().isUnitTestMode()) {    // force cleanup for created compiler system directory with generated sources
                    FileUtil.delete(CompilerPaths.getCompilerSystemDirectory(project));
                }
            }
        });
    }

    public Semaphore getCompilationSemaphore() {
        return myCompilationSemaphore;
    }

    public boolean isCompilationActive() {
        return myCompilationSemaphore.availablePermits() == 0;
    }

    public final void addCompiler( Compiler compiler) {
        myCompilers.add(compiler);
        // supporting file instrumenting compilers and validators for external build
        // Since these compilers are IDE-specific and use PSI, it is ok to run them before and after the build in the IDE
        if (compiler instanceof SourceInstrumentingCompiler) {
            addBeforeTask(new FileProcessingCompilerAdapterTask((FileProcessingCompiler)compiler));
        }
        else if (compiler instanceof Validator) {
            addAfterTask(new FileProcessingCompilerAdapterTask((FileProcessingCompiler)compiler));
        }
    }

    @Deprecated
    public void addTranslatingCompiler( TranslatingCompiler compiler, Set<FileType> inputTypes, Set<FileType> outputTypes) {
        // empty
    }

    public final void removeCompiler( Compiler compiler) {
        for (List<CompileTask> tasks : Arrays.asList(myBeforeTasks, myAfterTasks)) {
            for (Iterator<CompileTask> iterator = tasks.iterator(); iterator.hasNext(); ) {
                CompileTask task = iterator.next();
                if (task instanceof FileProcessingCompilerAdapterTask && ((FileProcessingCompilerAdapterTask)task).getCompiler() == compiler) {
                    iterator.remove();
                }
            }
        }
    }

    
    public <T  extends Compiler> T[] getCompilers( Class<T> compilerClass) {
        return getCompilers(compilerClass, CompilerFilter.ALL);
    }

    
    public <T extends Compiler> T[] getCompilers( Class<T> compilerClass, CompilerFilter filter) {
        final List<T> compilers = new ArrayList<T>(myCompilers.size());
        for (final Compiler item : myCompilers) {
            if (compilerClass.isAssignableFrom(item.getClass()) && filter.acceptCompiler(item)) {
                compilers.add((T)item);
            }
        }
        final T[] array = (T[])Array.newInstance(compilerClass, compilers.size());
        return compilers.toArray(array);
    }

    public void addCompilableFileType( FileType type) {
        myCompilableTypes.add(type);
    }

    public void removeCompilableFileType( FileType type) {
        myCompilableTypes.remove(type);
    }

    public boolean isCompilableFileType( FileType type) {
        return myCompilableTypes.contains(type);
    }

    public final void addBeforeTask( CompileTask task) {
        myBeforeTasks.add(task);
    }

    public final void addAfterTask( CompileTask task) {
        myAfterTasks.add(task);
    }

    
    public CompileTask[] getBeforeTasks() {
        return getCompileTasks(myBeforeTasks, CompileTaskBean.CompileTaskExecutionPhase.BEFORE);
    }

    private CompileTask[] getCompileTasks(List<CompileTask> taskList, CompileTaskBean.CompileTaskExecutionPhase phase) {
        List<CompileTask> beforeTasks = new ArrayList<CompileTask>(taskList);
        for (CompileTaskBean extension : CompileTaskBean.EP_NAME.getExtensions(myProject)) {
            if (extension.myExecutionPhase == phase) {
                beforeTasks.add(extension.getTaskInstance());
            }
        }
        return beforeTasks.toArray(new CompileTask[beforeTasks.size()]);
    }

    
    public CompileTask[] getAfterTasks() {
        return getCompileTasks(myAfterTasks, CompileTaskBean.CompileTaskExecutionPhase.AFTER);
    }

    public void compile( VirtualFile[] files, CompileStatusNotification callback) {
        compile(createFilesCompileScope(files), callback);
    }

    public void compile( Module module, CompileStatusNotification callback) {
        new CompileDriver(myProject).compile(createModuleCompileScope(module, false), new ListenerNotificator(callback));
    }

    public void compile( CompileScope scope, CompileStatusNotification callback) {
        new CompileDriver(myProject).compile(scope, new ListenerNotificator(callback));
    }

    public void make(CompileStatusNotification callback) {
        new CompileDriver(myProject).make(createProjectCompileScope(myProject), new ListenerNotificator(callback));
    }

    public void make( Module module, CompileStatusNotification callback) {
        new CompileDriver(myProject).make(createModuleCompileScope(module, true), new ListenerNotificator(callback));
    }

    public void make( Project project,  Module[] modules, CompileStatusNotification callback) {
        new CompileDriver(myProject).make(createModuleGroupCompileScope(project, modules, true), new ListenerNotificator(callback));
    }

    public void make( CompileScope scope, CompileStatusNotification callback) {
        new CompileDriver(myProject).make(scope, new ListenerNotificator(callback));
    }

    public void make( CompileScope scope, CompilerFilter filter,  CompileStatusNotification callback) {
        final CompileDriver compileDriver = new CompileDriver(myProject);
        compileDriver.setCompilerFilter(filter);
        compileDriver.make(scope, new ListenerNotificator(callback));
    }

    public boolean isUpToDate( final CompileScope scope) {
        return new CompileDriver(myProject).isUpToDate(scope);
    }

    public void rebuild(CompileStatusNotification callback) {
        new CompileDriver(myProject).rebuild(new ListenerNotificator(callback));
    }

    public void executeTask( CompileTask task,  CompileScope scope, String contentName, Runnable onTaskFinished) {
        final CompileDriver compileDriver = new CompileDriver(myProject);
        compileDriver.executeCompileTask(task, scope, contentName, onTaskFinished);
    }

    private final Map<CompilationStatusListener, MessageBusConnection> myListenerAdapters = new HashMap<CompilationStatusListener, MessageBusConnection>();

    public void addCompilationStatusListener( final CompilationStatusListener listener) {
        final MessageBusConnection connection = myProject.getMessageBus().connect();
        myListenerAdapters.put(listener, connection);
        connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener);
    }

    @Override
    public void addCompilationStatusListener( CompilationStatusListener listener,  Disposable parentDisposable) {
        final MessageBusConnection connection = myProject.getMessageBus().connect(parentDisposable);
        connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener);
    }

    public void removeCompilationStatusListener( final CompilationStatusListener listener) {
        final MessageBusConnection connection = myListenerAdapters.remove(listener);
        if (connection != null) {
            connection.disconnect();
        }
    }

    public boolean isExcludedFromCompilation( VirtualFile file) {
        return CompilerConfiguration.getInstance(myProject).isExcludedFromCompilation(file);
    }

    
    public CompileScope createFilesCompileScope( final VirtualFile[] files) {
        CompileScope[] scopes = new CompileScope[files.length];
        for(int i = 0; i < files.length; i++){
            scopes[i] = new OneProjectItemCompileScope(myProject, files[i]);
        }
        return new CompositeScope(scopes);
    }

    
    public CompileScope createModuleCompileScope( final Module module, final boolean includeDependentModules) {
        return createModulesCompileScope(new Module[] {module}, includeDependentModules);
    }

    
    public CompileScope createModulesCompileScope( final Module[] modules, final boolean includeDependentModules) {
        return createModulesCompileScope(modules, includeDependentModules, false);
    }

    
    public CompileScope createModulesCompileScope( Module[] modules, boolean includeDependentModules, boolean includeRuntimeDependencies) {
        return new ModuleCompileScope(myProject, modules, includeDependentModules, includeRuntimeDependencies);
    }

    
    public CompileScope createModuleGroupCompileScope( final Project project,  final Module[] modules, final boolean includeDependentModules) {
        return new ModuleCompileScope(project, modules, includeDependentModules);
    }

    
    public CompileScope createProjectCompileScope( final Project project) {
        return new ProjectCompileScope(project);
    }

    @Override
    public void setValidationEnabled(ModuleType moduleType, boolean enabled) {
        if (enabled) {
            myValidationDisabledModuleTypes.remove(moduleType);
        }
        else {
            myValidationDisabledModuleTypes.add(moduleType);
        }
    }

    @Override
    public boolean isValidationEnabled(Module module) {
        if (myValidationDisabledModuleTypes.isEmpty()) {
            return true; // optimization
        }
        return !myValidationDisabledModuleTypes.contains(ModuleType.get(module));
    }

    private class ListenerNotificator implements CompileStatusNotification {
        private final  CompileStatusNotification myDelegate;

        private ListenerNotificator( CompileStatusNotification delegate) {
            myDelegate = delegate;
        }

        public void finished(boolean aborted, int errors, int warnings, final CompileContext compileContext) {
            if (!myProject.isDisposed()) {
                myEventPublisher.compilationFinished(aborted, errors, warnings, compileContext);
            }
            if (myDelegate != null) {
                myDelegate.finished(aborted, errors, warnings, compileContext);
            }
        }
    }
}
