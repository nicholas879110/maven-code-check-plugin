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
package com.gome.maven.psi;

import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageParserDefinitions;
import com.gome.maven.lang.ParserDefinition;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.undo.UndoConstants;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.fileEditor.impl.LoadTextUtil;
import com.gome.maven.openapi.fileTypes.*;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.FileIndexFacade;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.vfs.NonPhysicalFileSystem;
import com.gome.maven.openapi.vfs.PersistentFSConstants;
import com.gome.maven.openapi.vfs.VFileProperty;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.PsiManagerImpl;
import com.gome.maven.psi.impl.file.PsiBinaryFileImpl;
import com.gome.maven.psi.impl.file.PsiLargeFileImpl;
import com.gome.maven.psi.impl.file.impl.FileManager;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.PsiPlainTextFileImpl;
import com.gome.maven.psi.impl.source.tree.FileElement;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.LocalTimeCounter;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class SingleRootFileViewProvider extends UserDataHolderBase implements FileViewProvider {
    private static final Key<Boolean> OUR_NO_SIZE_LIMIT_KEY = Key.create("no.size.limit");
    private static final Logger LOG = Logger.getInstance("#" + SingleRootFileViewProvider.class.getCanonicalName());
     private final PsiManager myManager;
     private final VirtualFile myVirtualFile;
    private final boolean myEventSystemEnabled;
    private final boolean myPhysical;
    private final AtomicReference<PsiFile> myPsiFile = new AtomicReference<PsiFile>();
    private volatile Content myContent;
    private volatile Reference<Document> myDocument;
     private final Language myBaseLanguage;

    public SingleRootFileViewProvider( PsiManager manager,  VirtualFile file) {
        this(manager, file, true);
    }

    public SingleRootFileViewProvider( PsiManager manager,  VirtualFile virtualFile, final boolean eventSystemEnabled) {
        this(manager, virtualFile, eventSystemEnabled, calcBaseLanguage(virtualFile, manager.getProject()));
    }

    protected SingleRootFileViewProvider( PsiManager manager,  VirtualFile virtualFile, final boolean eventSystemEnabled,  Language language) {
        myManager = manager;
        myVirtualFile = virtualFile;
        myEventSystemEnabled = eventSystemEnabled;
        myBaseLanguage = language;
        setContent(new VirtualFileContent());
        myPhysical = isEventSystemEnabled() &&
                !(virtualFile instanceof LightVirtualFile) &&
                !(virtualFile.getFileSystem() instanceof NonPhysicalFileSystem);
    }

    @Override
    
    public Language getBaseLanguage() {
        return myBaseLanguage;
    }

    private static Language calcBaseLanguage( VirtualFile file,  Project project) {
        if (file instanceof LightVirtualFile) {
            final Language language = ((LightVirtualFile)file).getLanguage();
            if (language != null) {
                return language;
            }
        }

        FileType fileType = file.getFileType();
        if (fileType.isBinary()) return Language.ANY;
        if (isTooLargeForIntelligence(file)) return PlainTextLanguage.INSTANCE;

        if (fileType instanceof LanguageFileType) {
            return LanguageSubstitutors.INSTANCE.substituteLanguage(((LanguageFileType)fileType).getLanguage(), file, project);
        }

        final ContentBasedFileSubstitutor[] processors = Extensions.getExtensions(ContentBasedFileSubstitutor.EP_NAME);
        for (ContentBasedFileSubstitutor processor : processors) {
            Language language = processor.obtainLanguageForFile(file);
            if (language != null) return language;
        }

        return PlainTextLanguage.INSTANCE;
    }

    @Override
    
    public Set<Language> getLanguages() {
        return Collections.singleton(getBaseLanguage());
    }

    @Override
    
    public final PsiFile getPsi( Language target) {
        if (!isPhysical()) {
            FileManager fileManager = ((PsiManagerEx)myManager).getFileManager();
            VirtualFile virtualFile = getVirtualFile();
            if (fileManager.findCachedViewProvider(virtualFile) == null) {
                fileManager.setViewProvider(virtualFile, this);
            }
        }
        return getPsiInner(target);
    }

    @Override
    
    public List<PsiFile> getAllFiles() {
        return ContainerUtil.createMaybeSingletonList(getPsi(getBaseLanguage()));
    }

    
    protected PsiFile getPsiInner( Language target) {
        if (target != getBaseLanguage()) {
            return null;
        }
        PsiFile psiFile = myPsiFile.get();
        if (psiFile == null) {
            psiFile = createFile();
            boolean set = myPsiFile.compareAndSet(null, psiFile);
            if (!set) {
                psiFile = myPsiFile.get();
            }
        }
        return psiFile;
    }

    @Override
    public void beforeContentsSynchronized() {
    }

    @Override
    public void contentsSynchronized() {
        unsetPsiContent();
    }

    private void unsetPsiContent() {
        if (!(myContent instanceof PsiFileContent)) return;
        final Document cachedDocument = getCachedDocument();
        setContent(cachedDocument == null ? new VirtualFileContent() : new DocumentContent());
    }

    public void beforeDocumentChanged( PsiFile psiCause) {
        PsiFile psiFile = psiCause != null ? psiCause : getPsi(getBaseLanguage());
        if (psiFile instanceof PsiFileImpl) {
            setContent(new PsiFileContent((PsiFileImpl)psiFile, psiCause == null ? getModificationStamp() : LocalTimeCounter.currentTime()));
        }
    }

    @Override
    public void rootChanged( PsiFile psiFile) {
        if (psiFile instanceof PsiFileImpl && ((PsiFileImpl)psiFile).isContentsLoaded()) {
            setContent(new PsiFileContent((PsiFileImpl)psiFile, LocalTimeCounter.currentTime()));
        }
    }

    @Override
    public boolean isEventSystemEnabled() {
        return myEventSystemEnabled;
    }

    @Override
    public boolean isPhysical() {
        return myPhysical;
    }

    @Override
    public long getModificationStamp() {
        return getContent().getModificationStamp();
    }

    @Override
    public boolean supportsIncrementalReparse( final Language rootLanguage) {
        return true;
    }


    public PsiFile getCachedPsi( Language target) {
        return myPsiFile.get();
    }

    
    public FileElement[] getKnownTreeRoots() {
        PsiFile psiFile = myPsiFile.get();
        if (psiFile == null || !(psiFile instanceof PsiFileImpl)) return new FileElement[0];
        if (((PsiFileImpl)psiFile).getTreeElement() == null) return new FileElement[0];
        return new FileElement[]{(FileElement)psiFile.getNode()};
    }

    private PsiFile createFile() {
        try {
            final VirtualFile vFile = getVirtualFile();
            if (vFile.isDirectory()) return null;
            if (isIgnored()) return null;

            final Project project = myManager.getProject();
            if (isPhysical() && vFile.isInLocalFileSystem()) { // check directories consistency
                final VirtualFile parent = vFile.getParent();
                if (parent == null) return null;
                final PsiDirectory psiDir = getManager().findDirectory(parent);
                if (psiDir == null) {
                    FileIndexFacade indexFacade = FileIndexFacade.getInstance(project);
                    if (!indexFacade.isInLibrarySource(vFile) && !indexFacade.isInLibraryClasses(vFile)) {
                        return null;
                    }
                }
            }

            FileType fileType = vFile.getFileType();
            return createFile(project, vFile, fileType);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
            return null;
        }
    }

    protected boolean isIgnored() {
        final VirtualFile file = getVirtualFile();
        return !(file instanceof LightVirtualFile) && FileTypeRegistry.getInstance().isFileIgnored(file);
    }

    
    protected PsiFile createFile( Project project,  VirtualFile file,  FileType fileType) {
        if (fileType.isBinary() || file.is(VFileProperty.SPECIAL)) {
            return new PsiBinaryFileImpl((PsiManagerImpl)getManager(), this);
        }
        if (!isTooLargeForIntelligence(file)) {
            final PsiFile psiFile = createFile(getBaseLanguage());
            if (psiFile != null) return psiFile;
        }

        if (isTooLargeForContentLoading(file)) {
            return new PsiLargeFileImpl((PsiManagerImpl)getManager(), this);
        }

        return new PsiPlainTextFileImpl(this);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated
    public static boolean isTooLarge( VirtualFile vFile) {
        return isTooLargeForIntelligence(vFile);
    }

    public static boolean isTooLargeForIntelligence( VirtualFile vFile) {
        if (!checkFileSizeLimit(vFile)) return false;
        return fileSizeIsGreaterThan(vFile, PersistentFSConstants.getMaxIntellisenseFileSize());
    }

    public static boolean isTooLargeForContentLoading( VirtualFile vFile) {
        return fileSizeIsGreaterThan(vFile, PersistentFSConstants.FILE_LENGTH_TO_CACHE_THRESHOLD);
    }

    private static boolean checkFileSizeLimit( VirtualFile vFile) {
        return !Boolean.TRUE.equals(vFile.getUserData(OUR_NO_SIZE_LIMIT_KEY));
    }
    public static void doNotCheckFileSizeLimit( VirtualFile vFile) {
        vFile.putUserData(OUR_NO_SIZE_LIMIT_KEY, Boolean.TRUE);
    }

    public static boolean isTooLargeForIntelligence( VirtualFile vFile, final long contentSize) {
        if (!checkFileSizeLimit(vFile)) return false;
        return contentSize > PersistentFSConstants.getMaxIntellisenseFileSize();
    }

    public static boolean isTooLargeForContentLoading( VirtualFile vFile, final long contentSize) {
        return contentSize > PersistentFSConstants.FILE_LENGTH_TO_CACHE_THRESHOLD;
    }

    private static boolean fileSizeIsGreaterThan( VirtualFile vFile, final long maxBytes) {
        if (vFile instanceof LightVirtualFile) {
            // This is optimization in order to avoid conversion of [large] file contents to bytes
            final int lengthInChars = ((LightVirtualFile)vFile).getContent().length();
            if (lengthInChars < maxBytes / 2) return false;
            if (lengthInChars > maxBytes ) return true;
        }

        return vFile.getLength() > maxBytes;
    }

    
    protected PsiFile createFile( Language lang) {
        if (lang != getBaseLanguage()) return null;
        final ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
        if (parserDefinition != null) {
            return parserDefinition.createFile(this);
        }
        return null;
    }

    @Override
    
    public PsiManager getManager() {
        return myManager;
    }

    @Override
    
    public CharSequence getContents() {
        return getContent().getText();
    }

    @Override
    
    public VirtualFile getVirtualFile() {
        return myVirtualFile;
    }

    
    private Document getCachedDocument() {
        final Document document = com.gome.maven.reference.SoftReference.dereference(myDocument);
        if (document != null) return document;
        return FileDocumentManager.getInstance().getCachedDocument(getVirtualFile());
    }

    @Override
    public Document getDocument() {
        Document document = com.gome.maven.reference.SoftReference.dereference(myDocument);
        if (document == null/* TODO[ik] make this change && isEventSystemEnabled()*/) {
            document = FileDocumentManager.getInstance().getDocument(getVirtualFile());
            myDocument = document == null ? null : new SoftReference<Document>(document);
        }
        if (document != null && getContent() instanceof VirtualFileContent) {
            setContent(new DocumentContent());
        }
        return document;
    }

    @Override
    public FileViewProvider clone() {
        final VirtualFile origFile = getVirtualFile();
        LightVirtualFile copy = new LightVirtualFile(origFile.getName(), origFile.getFileType(), getContents(), origFile.getCharset(), getModificationStamp());
        copy.setOriginalFile(origFile);
        copy.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
        copy.setCharset(origFile.getCharset());
        return createCopy(copy);
    }

    
    @Override
    public SingleRootFileViewProvider createCopy( final VirtualFile copy) {
        return new SingleRootFileViewProvider(getManager(), copy, false, myBaseLanguage);
    }

    @Override
    public PsiReference findReferenceAt(final int offset) {
        final PsiFile psiFile = getPsi(getBaseLanguage());
        return findReferenceAt(psiFile, offset);
    }

    @Override
    public PsiElement findElementAt(final int offset,  final Language language) {
        final PsiFile psiFile = getPsi(language);
        return psiFile != null ? findElementAt(psiFile, offset) : null;
    }

    @Override
    
    public PsiReference findReferenceAt(final int offset,  final Language language) {
        final PsiFile psiFile = getPsi(language);
        return psiFile != null ? findReferenceAt(psiFile, offset) : null;
    }

    
    private static PsiReference findReferenceAt( final PsiFile psiFile, final int offset) {
        if (psiFile == null) return null;
        int offsetInElement = offset;
        PsiElement child = psiFile.getFirstChild();
        while (child != null) {
            final int length = child.getTextLength();
            if (length <= offsetInElement) {
                offsetInElement -= length;
                child = child.getNextSibling();
                continue;
            }
            return child.findReferenceAt(offsetInElement);
        }
        return null;
    }

    @Override
    public PsiElement findElementAt(final int offset) {
        return findElementAt(getPsi(getBaseLanguage()), offset);
    }


    @Override
    public PsiElement findElementAt(int offset,  Class<? extends Language> lang) {
        if (!ReflectionUtil.isAssignable(lang, getBaseLanguage().getClass())) return null;
        return findElementAt(offset);
    }

    
    public static PsiElement findElementAt( final PsiElement psiFile, final int offset) {
        if (psiFile == null) return null;
        int offsetInElement = offset;
        PsiElement child = psiFile.getFirstChild();
        while (child != null) {
            final int length = child.getTextLength();
            if (length <= offsetInElement) {
                offsetInElement -= length;
                child = child.getNextSibling();
                continue;
            }
            return child.findElementAt(offsetInElement);
        }
        return null;
    }

    public void forceCachedPsi( PsiFile psiFile) {
        myPsiFile.set(psiFile);
        ((PsiManagerEx)myManager).getFileManager().setViewProvider(getVirtualFile(), this);
    }

    
    private Content getContent() {
        return myContent;
    }

    private void setContent( Content content) {
        // temporarily commented
        //if (myPhysical) {
        //  final Content oldContent = myContent;
        //  if (oldContent != null && content.getModificationStamp() != oldContent.getModificationStamp()) {
        //    ApplicationManager.getApplication().assertWriteAccessAllowed();
        //  }
        //}
        myContent = content;
    }

    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{myVirtualFile=" + myVirtualFile + ", content=" + getContent() + '}';
    }

    private interface Content {
        CharSequence getText();

        long getModificationStamp();
    }

    private class VirtualFileContent implements Content {
        @Override
        public CharSequence getText() {
            final VirtualFile virtualFile = getVirtualFile();
            if (virtualFile instanceof LightVirtualFile) {
                Document doc = getCachedDocument();
                if (doc != null) return getLastCommittedText(doc);
                return ((LightVirtualFile)virtualFile).getContent();
            }

            final Document document = getDocument();
            if (document == null) {
                return LoadTextUtil.loadText(virtualFile);
            }
            else {
                return getLastCommittedText(document);
            }
        }

        @Override
        public long getModificationStamp() {
            final VirtualFile virtualFile = getVirtualFile();
            if (virtualFile instanceof LightVirtualFile) {
                Document doc = getCachedDocument();
                if (doc != null) return getLastCommittedStamp(doc);
                return virtualFile.getModificationStamp();
            }

            final Document document = getDocument();
            if (document == null) {
                return virtualFile.getModificationStamp();
            }
            else {
                return getLastCommittedStamp(document);
            }
        }

        
        @Override
        public String toString() {
            return "VirtualFileContent{size=" + getVirtualFile().getLength() + "}";
        }
    }

    private CharSequence getLastCommittedText(Document document) {
        return PsiDocumentManager.getInstance(myManager.getProject()).getLastCommittedText(document);
    }
    private long getLastCommittedStamp(Document document) {
        return PsiDocumentManager.getInstance(myManager.getProject()).getLastCommittedStamp(document);
    }

    private class DocumentContent implements Content {
        
        @Override
        public String toString() {
            final Document document = getDocument();
            return "DocumentContent{size=" + (document == null ? null : document.getTextLength()) + "}";
        }

        
        @Override
        public CharSequence getText() {
            final Document document = getDocument();
            assert document != null;
            return getLastCommittedText(document);
        }

        @Override
        public long getModificationStamp() {
            Document document = getCachedDocument();
            if (document != null) return getLastCommittedStamp(document);
            return myVirtualFile.getModificationStamp();
        }
    }

    private class PsiFileContent implements Content {
        private final PsiFileImpl myFile;
        private volatile String myContent = null;
        private final long myModificationStamp;

        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private final List<FileElement> myFileElementHardRefs = new SmartList<FileElement>();

        private PsiFileContent(final PsiFileImpl file, final long modificationStamp) {
            myFile = file;
            myModificationStamp = modificationStamp;
            for (PsiFile aFile : getAllFiles()) {
                if (aFile instanceof PsiFileImpl) {
                    myFileElementHardRefs.add(((PsiFileImpl)aFile).calcTreeElement());
                }
            }
        }

        @Override
        public CharSequence getText() {
            String content = myContent;
            if (content == null) {
                myContent = content = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                    @Override
                    public String compute() {
                        return myFile.calcTreeElement().getText();
                    }
                });
            }
            return content;
        }

        @Override
        public long getModificationStamp() {
            return myModificationStamp;
        }
    }

    
    @Override
    public PsiFile getStubBindingRoot() {
        final PsiFile psi = getPsi(getBaseLanguage());
        assert psi != null;
        return psi;
    }
}
