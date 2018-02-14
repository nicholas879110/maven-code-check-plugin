/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.find.impl;

import com.gome.maven.BundleBase;
import com.gome.maven.find.*;
import com.gome.maven.find.findInProject.FindInProjectManager;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.ide.DataManager;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.FileEditor;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.util.ProgressWrapper;
import com.gome.maven.openapi.progress.util.TooManyUsagesStatus;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.DumbServiceImpl;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.ProjectFileIndex;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Factory;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.*;
import com.gome.maven.openapi.vfs.ex.VirtualFileManagerEx;
import com.gome.maven.psi.*;
import com.gome.maven.psi.search.*;
import com.gome.maven.ui.content.Content;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.usageView.UsageViewManager;
import com.gome.maven.usages.ConfigurableUsageTarget;
import com.gome.maven.usages.FindUsagesProcessPresentation;
import com.gome.maven.usages.UsageView;
import com.gome.maven.usages.UsageViewPresentation;
import com.gome.maven.util.Function;
import com.gome.maven.util.PatternUtil;
import com.gome.maven.util.Processor;

import javax.swing.*;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class FindInProjectUtil {
    private static final int USAGES_PER_READ_ACTION = 100;

    private FindInProjectUtil() {}

    public static void setDirectoryName( FindModel model,  DataContext dataContext) {
        PsiElement psiElement = null;
        Project project = CommonDataKeys.PROJECT.getData(dataContext);

        if (project != null && !DumbServiceImpl.getInstance(project).isDumb()) {
            try {
                psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
            }
            catch (IndexNotReadyException ignore) {}
        }

        String directoryName = null;

        if (psiElement instanceof PsiDirectory) {
            directoryName = ((PsiDirectory)psiElement).getVirtualFile().getPresentableUrl();
        }

        if (directoryName == null && psiElement instanceof PsiDirectoryContainer) {
            final PsiDirectory[] directories = ((PsiDirectoryContainer)psiElement).getDirectories();
            directoryName = directories.length == 1 ? directories[0].getVirtualFile().getPresentableUrl():null;
        }

        Module module = LangDataKeys.MODULE_CONTEXT.getData(dataContext);
        if (module != null) {
            model.setModuleName(module.getName());
        }

        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (model.getModuleName() == null || editor == null) {
            model.setDirectoryName(directoryName);
            model.setProjectScope(directoryName == null && module == null && !model.isCustomScope() || editor != null);
            if (directoryName != null) {
                model.setCustomScope(false); // to select "Directory: " radio button
            }

            // for convenience set directory name to directory of current file, note that we doesn't change default projectScope
            if (directoryName == null) {
                VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
                if (virtualFile != null && !virtualFile.isDirectory()) virtualFile = virtualFile.getParent();
                if (virtualFile != null) model.setDirectoryName(virtualFile.getPresentableUrl());
            }
        }
    }

    
    public static PsiDirectory getPsiDirectory( final FindModel findModel,  Project project) {
        String directoryName = findModel.getDirectoryName();
        if (findModel.isProjectScope() || StringUtil.isEmpty(directoryName)) {
            return null;
        }

        final PsiManager psiManager = PsiManager.getInstance(project);
        String path = directoryName.replace(File.separatorChar, '/');
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
        if (virtualFile == null || !virtualFile.isDirectory()) {
            virtualFile = null;
            for (LocalFileProvider provider : ((VirtualFileManagerEx)VirtualFileManager.getInstance()).getLocalFileProviders()) {
                VirtualFile file = provider.findLocalVirtualFileByPath(path);
                if (file != null && file.isDirectory()) {
                    if (file.getChildren().length > 0) {
                        virtualFile = file;
                        break;
                    }
                    if(virtualFile == null){
                        virtualFile = file;
                    }
                }
            }
        }
        return virtualFile == null ? null : psiManager.findDirectory(virtualFile);
    }


    
    public static Pattern createFileMaskRegExp( String filter) {
        if (filter == null) {
            return null;
        }
        String pattern;
        final List<String> strings = StringUtil.split(filter, ",");
        if (strings.size() == 1) {
            pattern = PatternUtil.convertToRegex(filter.trim());
        }
        else {
            pattern = StringUtil.join(strings, new Function<String, String>() {
                
                @Override
                public String fun( String s) {
                    return "(" + PatternUtil.convertToRegex(s.trim()) + ")";
                }
            }, "|");
        }
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    public static void findUsages( FindModel findModel,
                                  final PsiDirectory psiDirectory,
                                   final Project project,
                                   final Processor<UsageInfo> consumer,
                                   FindUsagesProcessPresentation processPresentation) {
        new FindInProjectTask(findModel, project, psiDirectory).findUsages(consumer, processPresentation);
    }

    static int processUsagesInFile( final PsiFile psiFile,
                                    final FindModel findModel,
                                    final Processor<UsageInfo> consumer) {
        if (findModel.getStringToFind().isEmpty()) {
            if (!ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
                @Override
                public Boolean compute() {
                    return consumer.process(new UsageInfo(psiFile,0,0,true));
                }
            })) {
                throw new ProcessCanceledException();
            }
            return 1;
        }
        final VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) return 0;
        if (virtualFile.getFileType().isBinary()) return 0; // do not decompile .class files
        final Document document = ApplicationManager.getApplication().runReadAction(new Computable<Document>() {
            @Override
            public Document compute() {
                return virtualFile.isValid() ? FileDocumentManager.getInstance().getDocument(virtualFile) : null;
            }
        });
        if (document == null) return 0;
        final int[] offset = {0};
        int count = 0;
        int found;
        ProgressIndicator indicator = ProgressWrapper.unwrap(ProgressManager.getInstance().getProgressIndicator());
        TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.getFrom(indicator);
        do {
            tooManyUsagesStatus.pauseProcessingIfTooManyUsages(); // wait for user out of read action
            found = DumbService.getInstance(psiFile.getProject()).runReadActionInSmartMode(new Computable<Integer>() {
                @Override
                
                public Integer compute() {
                    if (!psiFile.isValid()) return 0;
                    return addToUsages(document, consumer, findModel, psiFile, offset, USAGES_PER_READ_ACTION);
                }
            });
            count += found;
        }
        while (found != 0);
        return count;
    }

    private static int addToUsages( Document document,  Processor<UsageInfo> consumer,  FindModel findModel,
                                    final PsiFile psiFile,  int[] offsetRef, int maxUsages) {
        int count = 0;
        CharSequence text = document.getCharsSequence();
        int textLength = document.getTextLength();
        int offset = offsetRef[0];

        Project project = psiFile.getProject();

        FindManager findManager = FindManager.getInstance(project);
        while (offset < textLength) {
            FindResult result = findManager.findString(text, offset, findModel, psiFile.getVirtualFile());
            if (!result.isStringFound()) break;

            final SearchScope customScope = findModel.getCustomScope();
            if (customScope instanceof LocalSearchScope) {
                final TextRange range = new TextRange(result.getStartOffset(), result.getEndOffset());
                if (!((LocalSearchScope)customScope).containsRange(psiFile, range)) break;
            }
            UsageInfo info = new FindResultUsageInfo(findManager, psiFile, offset, findModel, result);
            if (!consumer.process(info)){
                throw new ProcessCanceledException();
            }
            count++;

            final int prevOffset = offset;
            offset = result.getEndOffset();

            if (prevOffset == offset) {
                // for regular expr the size of the match could be zero -> could be infinite loop in finding usages!
                ++offset;
            }
            if (maxUsages > 0 && count >= maxUsages) {
                break;
            }
        }
        offsetRef[0] = offset;
        return count;
    }

    
    private static String getTitleForScope( final FindModel findModel) {
        String scopeName;
        if (findModel.isProjectScope()) {
            scopeName = FindBundle.message("find.scope.project.title");
        }
        else if (findModel.getModuleName() != null) {
            scopeName = FindBundle.message("find.scope.module.title", findModel.getModuleName());
        }
        else if(findModel.getCustomScopeName() != null) {
            scopeName = findModel.getCustomScopeName();
        }
        else {
            scopeName = FindBundle.message("find.scope.directory.title", findModel.getDirectoryName());
        }

        String result = scopeName;
        if (findModel.getFileFilter() != null) {
            result += " "+FindBundle.message("find.scope.files.with.mask", findModel.getFileFilter());
        }

        return result;
    }

    
    public static UsageViewPresentation setupViewPresentation(final boolean toOpenInNewTab,  FindModel findModel) {
        final UsageViewPresentation presentation = new UsageViewPresentation();

        final String scope = getTitleForScope(findModel);
        final String stringToFind = findModel.getStringToFind();
        presentation.setScopeText(scope);
        if (stringToFind.isEmpty()) {
            presentation.setTabText("Files");
            presentation.setToolwindowTitle(BundleBase.format("Files in {0}", scope));
            presentation.setUsagesString("files");
        }
        else {
            FindModel.SearchContext searchContext = findModel.getSearchContext();
            String contextText = "";
            if (searchContext != FindModel.SearchContext.ANY) {
                contextText = FindBundle.message("find.context.presentation.scope.label", FindDialog.getPresentableName(searchContext));
            }
            presentation.setTabText(FindBundle.message("find.usage.view.tab.text", stringToFind, contextText));
            presentation.setToolwindowTitle(FindBundle.message("find.usage.view.toolwindow.title", stringToFind, scope, contextText));
            presentation.setUsagesString(FindBundle.message("find.usage.view.usages.text", stringToFind));
            presentation.setUsagesWord(FindBundle.message("occurrence"));
            presentation.setCodeUsagesString(FindBundle.message("found.occurrences"));
            presentation.setContextText(contextText);
        }
        presentation.setOpenInNewTab(toOpenInNewTab);
        presentation.setCodeUsages(false);
        presentation.setUsageTypeFilteringAvailable(true);

        return presentation;
    }

    
    public static FindUsagesProcessPresentation setupProcessPresentation( final Project project,
                                                                         final boolean showPanelIfOnlyOneUsage,
                                                                          final UsageViewPresentation presentation) {
        FindUsagesProcessPresentation processPresentation = new FindUsagesProcessPresentation(presentation);
        processPresentation.setShowNotFoundMessage(true);
        processPresentation.setShowPanelIfOnlyOneUsage(showPanelIfOnlyOneUsage);
        processPresentation.setProgressIndicatorFactory(
                new Factory<ProgressIndicator>() {
                    
                    @Override
                    public ProgressIndicator create() {
                        return new FindProgressIndicator(project, presentation.getScopeText());
                    }
                }
        );
        return processPresentation;
    }

    public static class StringUsageTarget implements ConfigurableUsageTarget, ItemPresentation, TypeSafeDataProvider {
         protected final Project myProject;
         protected final FindModel myFindModel;

        public StringUsageTarget( Project project,  FindModel findModel) {
            myProject = project;
            myFindModel = findModel;
        }

        @Override
        
        public String getPresentableText() {
            UsageViewPresentation presentation = setupViewPresentation(false, myFindModel);
            return presentation.getToolwindowTitle();
        }

        
        @Override
        public String getLongDescriptiveName() {
            return getPresentableText();
        }

        @Override
        public String getLocationString() {
            return myFindModel + "!!";
        }

        @Override
        public Icon getIcon(boolean open) {
            return AllIcons.Actions.Menu_find;
        }

        @Override
        public void findUsages() {
            FindInProjectManager.getInstance(myProject).startFindInProject(myFindModel);
        }

        @Override
        public void findUsagesInEditor( FileEditor editor) {}
        @Override
        public void highlightUsages( PsiFile file,  Editor editor, boolean clearHighlights) {}

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        
        public VirtualFile[] getFiles() {
            return null;
        }

        @Override
        public void update() {
        }

        @Override
        public String getName() {
            return myFindModel.getStringToFind().isEmpty() ? myFindModel.getFileFilter() : myFindModel.getStringToFind();
        }

        @Override
        public ItemPresentation getPresentation() {
            return this;
        }

        @Override
        public void navigate(boolean requestFocus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }

        @Override
        public void showSettings() {
            Content selectedContent = UsageViewManager.getInstance(myProject).getSelectedContent(true);
            JComponent component = selectedContent == null ? null : selectedContent.getComponent();
            FindInProjectManager findInProjectManager = FindInProjectManager.getInstance(myProject);
            findInProjectManager.findInProject(DataManager.getInstance().getDataContext(component));
        }

        @Override
        public KeyboardShortcut getShortcut() {
            return ActionManager.getInstance().getKeyboardShortcut("FindInPath");
        }

        @Override
        public void calcData(DataKey key, DataSink sink) {
            if (UsageView.USAGE_SCOPE.equals(key)) {
                SearchScope scope = getScopeFromModel(myProject, myFindModel);
                sink.put(UsageView.USAGE_SCOPE, scope);
            }
        }
    }

    private static void addSourceDirectoriesFromLibraries( Project project,
                                                           VirtualFile file,
                                                           Collection<VirtualFile> outSourceRoots) {
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(project);
        // if we already are in the sources, search just in this directory only
        if (index.isInLibrarySource(file)) return;
        VirtualFile classRoot = index.getClassRootForFile(file);
        if (classRoot == null) return;
        String relativePath = VfsUtil.getRelativePath(file, classRoot);
        if (relativePath == null) return;
        for (OrderEntry orderEntry : index.getOrderEntriesForFile(file)) {
            for (VirtualFile sourceRoot : orderEntry.getFiles(OrderRootType.SOURCES)) {
                VirtualFile sourceFile = sourceRoot.findFileByRelativePath(relativePath);
                if (sourceFile != null) {
                    outSourceRoots.add(sourceFile);
                }
            }
        }
    }

    
    static SearchScope getScopeFromModel( Project project,  FindModel findModel) {
        SearchScope customScope = findModel.getCustomScope();
        PsiDirectory psiDir = getPsiDirectory(findModel, project);
        VirtualFile directory = psiDir == null ? null : psiDir.getVirtualFile();
        Module module = findModel.getModuleName() == null ? null : ModuleManager.getInstance(project).findModuleByName(findModel.getModuleName());
        return findModel.isCustomScope() && customScope != null ? customScope.intersectWith(GlobalSearchScope.allScope(project)) :
                // we don't have to check for myProjectFileIndex.isExcluded(file) here like FindInProjectTask.collectFilesInScope() does
                // because all found usages are guaranteed to be not in excluded dir
                directory != null ? forDirectory(project, findModel.isWithSubdirectories(), directory) :
                        module != null ? module.getModuleContentScope() :
                                findModel.isProjectScope() ? ProjectScope.getContentScope(project) :
                                        GlobalSearchScope.allScope(project);
    }

    
    private static GlobalSearchScope forDirectory( Project project,
                                                  boolean withSubdirectories,
                                                   VirtualFile directory) {
        Set<VirtualFile> result = new LinkedHashSet<VirtualFile>();
        result.add(directory);
        addSourceDirectoriesFromLibraries(project, directory, result);
        VirtualFile[] array = result.toArray(new VirtualFile[result.size()]);
        return GlobalSearchScopesCore.directoriesScope(project, withSubdirectories, array);
    }
}
