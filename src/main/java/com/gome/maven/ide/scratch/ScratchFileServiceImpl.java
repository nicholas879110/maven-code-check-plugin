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
package com.gome.maven.ide.scratch;

import com.gome.maven.ide.FileIconProvider;
import com.gome.maven.ide.navigationToolbar.AbstractNavBarModelExtension;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageUtil;
import com.gome.maven.lang.PerFileMappings;
import com.gome.maven.lang.PerFileMappingsBase;
import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.components.State;
import com.gome.maven.openapi.components.Storage;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.FileEditorManagerAdapter;
import com.gome.maven.openapi.fileEditor.FileEditorManagerListener;
import com.gome.maven.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.gome.maven.openapi.fileEditor.impl.NonProjectFileWritingAccessExtension;
import com.gome.maven.openapi.fileTypes.*;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.project.ProjectManagerAdapter;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VfsUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileWithId;
import com.gome.maven.openapi.wm.IdeFrame;
import com.gome.maven.openapi.wm.StatusBar;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.openapi.wm.WindowManagerListener;
import com.gome.maven.psi.LanguageSubstitutor;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.util.PsiUtilCore;
import com.gome.maven.util.PairConsumer;
import com.gome.maven.util.PathUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.messages.MessageBus;
import org.jdom.Element;

import javax.swing.*;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@State(
        name = "ScratchFileService",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/scratches.xml")
        })
public class ScratchFileServiceImpl extends ScratchFileService implements PersistentStateComponent<Element>{

    private static final RootType NULL_TYPE = new RootType("", null) {};

    private final LightDirectoryIndex<RootType> myIndex;
    private final MyLanguages myScratchMapping = new MyLanguages();

    protected ScratchFileServiceImpl(WindowManager windowManager, MessageBus messageBus) {
        myIndex = new LightDirectoryIndex<RootType>(messageBus.connect(), NULL_TYPE) {

            @Override
            protected void collectRoots( PairConsumer<VirtualFile, RootType> consumer) {
                LocalFileSystem fileSystem = LocalFileSystem.getInstance();
                for (RootType r : RootType.getAllRootIds()) {
                    String root = getRootPath(r);
                    VirtualFile rootFile = fileSystem.findFileByPath(root);
                    if (rootFile != null) {
                        consumer.consume(rootFile, r);
                    }
                }
            }
        };
        initScratchWidget(windowManager);
        initFileOpenedListener(messageBus);
    }

    
    @Override
    public String getRootPath( RootType rootId) {
        return getRootPath() + "/" + rootId.getId();
    }

    
    @Override
    public RootType getRootType( VirtualFile file) {
        VirtualFile directory = file.isDirectory() ? file : file.getParent();
        if (!(directory instanceof VirtualFileWithId)) return null;
        RootType result = myIndex.getInfoForFile(directory);
        return result == NULL_TYPE ? null : result;
    }

    private void initFileOpenedListener(MessageBus messageBus) {
        final FileEditorManagerAdapter editorListener = new FileEditorManagerAdapter() {
            @Override
            public void fileOpened( FileEditorManager source,  VirtualFile file) {
                RootType rootType = getRootType(file);
                if (rootType != null) {
                    rootType.fileOpened(file, source);
                }
            }
        };
        ProjectManagerAdapter projectListener = new ProjectManagerAdapter() {
            @Override
            public void projectOpened(Project project) {
                project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorListener);
                FileEditorManager editorManager = FileEditorManager.getInstance(project);
                for (VirtualFile virtualFile : editorManager.getOpenFiles()) {
                    editorListener.fileOpened(editorManager, virtualFile);
                }
            }
        };
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            projectListener.projectOpened(project);
        }
        messageBus.connect().subscribe(ProjectManager.TOPIC, projectListener);
    }

    private static void initScratchWidget(WindowManager windowManager) {
        final WindowManagerListener windowListener = new WindowManagerListener() {
            @Override
            public void frameCreated(IdeFrame frame) {
                Project project = frame.getProject();
                StatusBar statusBar = frame.getStatusBar();
                if (project == null || statusBar == null || statusBar.getWidget(ScratchWidget.WIDGET_ID) != null) return;
                ScratchWidget widget = new ScratchWidget(project);
                statusBar.addWidget(widget, "before Encoding", project);
                statusBar.updateWidget(ScratchWidget.WIDGET_ID);
            }

            @Override
            public void beforeFrameReleased(IdeFrame frame) {
            }
        };
        for (IdeFrame frame : windowManager.getAllProjectFrames()) {
            windowListener.frameCreated(frame);
        }
        windowManager.addListener(windowListener);
    }

    
    protected String getRootPath() {
        return FileUtil.toSystemIndependentName(PathManager.getConfigPath());
    }

    
    @Override
    public PerFileMappings<Language> getScratchesMapping() {
        return myScratchMapping;
    }

    
    @Override
    public Element getState() {
        return myScratchMapping.getState();
    }

    @Override
    public void loadState(Element state) {
        myScratchMapping.loadState(state);
    }

    private static class MyLanguages extends PerFileMappingsBase<Language> {
        @Override
        protected List<Language> getAvailableValues() {
            return LanguageUtil.getFileLanguages();
        }

        
        @Override
        protected String serialize(Language language) {
            return language.getID();
        }

        
        @Override
        protected Language handleUnknownMapping(VirtualFile file, String value) {
            return PlainTextLanguage.INSTANCE;
        }

        
        @Override
        public Language getMapping( VirtualFile file) {
            Language language = super.getMapping(file);
            if (language == null && file != null && file.getFileType() == ScratchFileType.INSTANCE) {
                String extension = file.getExtension();
                FileType fileType = extension == null ? null : FileTypeManager.getInstance().getFileTypeByExtension(extension);
                language = fileType instanceof LanguageFileType ? ((LanguageFileType)fileType).getLanguage() : null;
            }
            return language;
        }
    }

    public static class TypeFactory extends FileTypeFactory {

        @Override
        public void createFileTypes( FileTypeConsumer consumer) {
            consumer.consume(ScratchFileType.INSTANCE);
        }
    }

    public static class Substitutor extends LanguageSubstitutor {
        
        @Override
        public Language getLanguage( VirtualFile file,  Project project) {
            RootType rootType = ScratchFileService.getInstance().getRootType(file);
            if (rootType == null) return null;
            return rootType.substituteLanguage(project, file);
        }
    }

    public static class Highlighter implements SyntaxHighlighterProvider {
        @Override
        
        public SyntaxHighlighter create( FileType fileType,  Project project,  VirtualFile file) {
            if (project == null || file == null || !(fileType instanceof ScratchFileType)) return null;
            RootType rootType = ScratchFileService.getInstance().getRootType(file);
            if (rootType == null) return null;
            Language language = rootType.substituteLanguage(project, file);
            SyntaxHighlighter highlighter = language == null ? null : SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
            if (highlighter != null) return highlighter;
            FileType originalFileType = RootType.getOriginalFileType(file);
            highlighter = originalFileType == null ? null : SyntaxHighlighterFactory.getSyntaxHighlighter(originalFileType, project, file);
            return highlighter;
        }
    }

    public static class FilePresentation implements FileIconProvider, EditorTabTitleProvider {

        
        @Override
        public Icon getIcon( VirtualFile file, @Iconable.IconFlags int flags,  Project project) {
            if (project == null || file.isDirectory()) return null;
            RootType rootType = ScratchFileService.getInstance().getRootType(file);
            if (rootType == null) return null;
            return rootType.substituteIcon(project, file);
        }

        
        @Override
        public String getEditorTabTitle( Project project,  VirtualFile file) {
            RootType rootType = ScratchFileService.getInstance().getRootType(file);
            if (rootType == null) return null;
            return rootType.substituteName(project, file);
        }
    }

    public static class AccessExtension implements NonProjectFileWritingAccessExtension {

        @Override
        public boolean isWritable( VirtualFile file) {
            return file.getFileType() == ScratchFileType.INSTANCE;
        }
    }

    public static class NavBarExtension extends AbstractNavBarModelExtension {

        
        @Override
        public String getPresentableText(Object object) {
            if (!(object instanceof PsiElement)) return null;
            Project project = ((PsiElement)object).getProject();
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile((PsiElement)object);
            if (virtualFile == null || !virtualFile.isValid()) return null;
            RootType rootType = ScratchFileService.getInstance().getRootType(virtualFile);
            if (rootType == null) return null;
            if (virtualFile.isDirectory() && additionalRoots(project).contains(virtualFile)) {
                return rootType.getDisplayName();
            }
            return rootType.substituteName(project, virtualFile);
        }

        
        @Override
        public Collection<VirtualFile> additionalRoots(Project project) {
            Set<VirtualFile> result = ContainerUtil.newLinkedHashSet();
            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            ScratchFileService app = ScratchFileService.getInstance();
            for (RootType r : RootType.getAllRootIds()) {
                ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(app.getRootPath(r)));
            }
            return result;
        }
    }

    @Override
    public VirtualFile findFile( final RootType rootType,  final String pathName, Option option) throws IOException {
        String fullPath = getRootPath(rootType) + "/" + pathName;
        if (option != Option.create_new_always) {
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(fullPath);
            if (file != null && !file.isDirectory()) return file;
            if (option == Option.existing_only) return null;
        }
        String ext = PathUtil.getFileExtension(pathName);
        String fileNameExt = PathUtil.getFileName(pathName);
        String fileName = StringUtil.trimEnd(fileNameExt, ext == null ? "" : "." + ext);
        AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
        try {
            VirtualFile dir = VfsUtil.createDirectories(PathUtil.getParentPath(fullPath));
            if (option == Option.create_new_always) {
                return VfsUtil.createChildSequent(LocalFileSystem.getInstance(), dir, fileName, StringUtil.notNullize(ext));
            }
            else {
                return dir.createChildData(LocalFileSystem.getInstance(), fileNameExt);
            }
        }
        finally {
            token.finish();
        }
    }
}