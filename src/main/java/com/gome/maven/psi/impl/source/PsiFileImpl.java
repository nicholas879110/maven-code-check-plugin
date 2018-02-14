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

package com.gome.maven.psi.impl.source;

import com.gome.maven.extapi.psi.StubBasedPsiElementBase;
import com.gome.maven.ide.util.PsiNavigationSupport;
import com.gome.maven.lang.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.navigation.ItemPresentation;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.Project;
//import com.gome.maven.openapi.ui.Queryable;
import com.gome.maven.openapi.util.Getter;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileWithId;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.*;
import com.gome.maven.psi.impl.file.PsiFileImplUtil;
import com.gome.maven.psi.impl.source.codeStyle.CodeEditUtil;
import com.gome.maven.psi.impl.source.resolve.FileContextUtil;
import com.gome.maven.psi.impl.source.text.BlockSupportImpl;
import com.gome.maven.psi.impl.source.tree.*;
import com.gome.maven.psi.scope.PsiScopeProcessor;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.search.PsiElementProcessor;
import com.gome.maven.psi.search.SearchScope;
import com.gome.maven.psi.stubs.*;
import com.gome.maven.psi.tree.*;
import com.gome.maven.reference.SoftReference;
import com.gome.maven.util.FileContentUtilCore;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.PatchedWeakReference;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.text.CharArrayUtil;

import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.util.*;

public abstract class PsiFileImpl extends ElementBase implements PsiFileEx, PsiFileWithStubSupport/*, Queryable*/ {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.psi.impl.source.PsiFileImpl");
    public static final String STUB_PSI_MISMATCH = "stub-psi mismatch";

    private IElementType myElementType;
    protected IElementType myContentElementType;
    private long myModificationStamp;

    protected PsiFile myOriginalFile = null;
    private final FileViewProvider myViewProvider;
    private volatile Reference<StubTree> myStub;
    protected final PsiManagerEx myManager;
    private volatile Getter<FileElement> myTreeElementPointer; // SoftReference/WeakReference to ASTNode or a strong reference to a tree if the file is a DummyHolder
    public static final Key<Boolean> BUILDING_STUB = new Key<Boolean>("Don't use stubs mark!");

    protected PsiFileImpl( IElementType elementType, IElementType contentElementType,  FileViewProvider provider) {
        this(provider);
        init(elementType, contentElementType);
    }

    protected PsiFileImpl( FileViewProvider provider ) {
        myManager = (PsiManagerEx)provider.getManager();
        myViewProvider = provider;
    }

    public void setContentElementType(final IElementType contentElementType) {
        LOG.assertTrue(contentElementType instanceof ILazyParseableElementType, contentElementType);
        myContentElementType = contentElementType;
    }

    public IElementType getContentElementType() {
        return myContentElementType;
    }

    protected void init( final IElementType elementType, final IElementType contentElementType) {
        myElementType = elementType;
        setContentElementType(contentElementType);
    }

    public TreeElement createContentLeafElement(CharSequence leafText) {
        if (myContentElementType instanceof ILazyParseableElementType) {
            return ASTFactory.lazy((ILazyParseableElementType)myContentElementType, leafText);
        }
        return ASTFactory.leaf(myContentElementType, leafText);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public FileElement getTreeElement() {
        FileElement node = derefTreeElement();
        if (node != null) return node;

        if (!getViewProvider().isPhysical()) {
            return loadTreeElement();
        }

        return null;
    }

    private FileElement derefTreeElement() {
        Getter<FileElement> pointer = myTreeElementPointer;
        FileElement treeElement = SoftReference.deref(pointer);
        if (treeElement != null) return treeElement;

        synchronized (PsiLock.LOCK) {
            if (myTreeElementPointer == pointer) {
                myTreeElementPointer = null;
            }
        }
        return null;
    }

    @Override
    public VirtualFile getVirtualFile() {
        return getViewProvider().isEventSystemEnabled() ? getViewProvider().getVirtualFile() : null;
    }

    @Override
    public boolean processChildren(final PsiElementProcessor<PsiFileSystemItem> processor) {
        return true;
    }

    @Override
    public boolean isValid() {
        FileViewProvider provider = getViewProvider();
        final VirtualFile vFile = provider.getVirtualFile();
        if (!vFile.isValid()) return false;
        if (!provider.isEventSystemEnabled()) return true; // "dummy" file
        if (myManager.getProject().isDisposed()) return false;
        return isPsiUpToDate(vFile);
    }

    protected boolean isPsiUpToDate( VirtualFile vFile) {
        final FileViewProvider provider = myManager.findViewProvider(vFile);
        Language language = getLanguage();
        if (provider == null || provider.getPsi(language) == this) { // provider == null in tests
            return true;
        }
        Language baseLanguage = provider.getBaseLanguage();
        return baseLanguage != language && provider.getPsi(baseLanguage) == this;
    }

    @Override
    public boolean isContentsLoaded() {
        return derefTreeElement() != null;
    }

    
    private FileElement loadTreeElement() {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        final FileViewProvider viewProvider = getViewProvider();
        if (viewProvider.isPhysical() && myManager.isAssertOnFileLoading(viewProvider.getVirtualFile())) {
            LOG.error("Access to tree elements not allowed in tests. path='" + viewProvider.getVirtualFile().getPresentableUrl()+"'");
        }

        Document cachedDocument = FileDocumentManager.getInstance().getCachedDocument(getViewProvider().getVirtualFile());

        FileElement treeElement = createFileElement(viewProvider.getContents());
        treeElement.setPsi(this);

        List<Pair<StubBasedPsiElementBase, CompositeElement>> bindings = calcStubAstBindings(treeElement, cachedDocument);

        synchronized (PsiLock.LOCK) {
            FileElement existing = derefTreeElement();
            if (existing != null) {
                return existing;
            }

            switchFromStubToAst(bindings);
            myStub = null;
            myTreeElementPointer = createTreeElementPointer(treeElement);

            if (LOG.isDebugEnabled() && viewProvider.isPhysical()) {
                LOG.debug("Loaded text for file " + viewProvider.getVirtualFile().getPresentableUrl());
            }

            return treeElement;
        }
    }

    @Override
    public ASTNode findTreeForStub(StubTree tree, StubElement<?> stub) {
        final Iterator<StubElement<?>> stubs = tree.getPlainList().iterator();
        final StubElement<?> root = stubs.next();
        final CompositeElement ast = calcTreeElement();
        if (root == stub) return ast;

        return findTreeForStub(ast, stubs, stub);
    }

    
    private static ASTNode findTreeForStub(ASTNode tree, final Iterator<StubElement<?>> stubs, final StubElement stub) {
        final IElementType type = tree.getElementType();

        if (type instanceof IStubElementType && ((IStubElementType) type).shouldCreateStub(tree)) {
            final StubElement curStub = stubs.next();
            if (curStub == stub) return tree;
        }

        for (ASTNode node : tree.getChildren(null)) {
            final ASTNode treeForStub = findTreeForStub(node, stubs, stub);
            if (treeForStub != null) return treeForStub;
        }

        return null;
    }

    private static void switchFromStubToAst(List<Pair<StubBasedPsiElementBase, CompositeElement>> pairs) {
        for (Pair<StubBasedPsiElementBase, CompositeElement> pair : pairs) {
            pair.second.setPsi(pair.first);
            pair.first.setNode(pair.second);
            pair.first.setStub(null);
        }
    }

    private List<Pair<StubBasedPsiElementBase, CompositeElement>> calcStubAstBindings(final ASTNode root, final Document cachedDocument) {
        final StubTree stubTree = derefStub();
        if (stubTree == null) {
            return Collections.emptyList();
        }

        final Iterator<StubElement<?>> stubs = stubTree.getPlainList().iterator();
        stubs.next(); // Skip file stub;
        final List<Pair<StubBasedPsiElementBase, CompositeElement>> result = ContainerUtil.newArrayList();
        final IStubFileElementType elementType = getElementTypeForStubBuilder();
        assert elementType != null;
        final StubBuilder builder = elementType.getBuilder();

        LazyParseableElement.setSuppressEagerPsiCreation(true);
        try {
            ((TreeElement)root).acceptTree(new RecursiveTreeElementWalkingVisitor() {
                @Override
                protected void visitNode(TreeElement node) {
                    CompositeElement parent = node.getTreeParent();
                    if (parent != null && builder.skipChildProcessingWhenBuildingStubs(parent, node)) {
                        return;
                    }


                    IElementType type = node.getElementType();
                    if (type instanceof IStubElementType && ((IStubElementType)type).shouldCreateStub(node)) {
                        if (!stubs.hasNext()) {
                            reportStubAstMismatch("Stub list is less than AST, last AST element: " + node.getElementType() + " " + node, stubTree, cachedDocument);
                        }

                        final StubElement stub = stubs.next();
                        if (stub.getStubType() != node.getElementType()) {
                            reportStubAstMismatch("Stub and PSI element type mismatch in " + getName() + ": stub " + stub + ", AST " +
                                    node.getElementType() + "; " + node, stubTree, cachedDocument);
                        }

                        PsiElement psi = stub.getPsi();
                        assert psi != null : "Stub " + stub + " (" + stub.getClass() + ") has returned null PSI";
                        result.add(Pair.create((StubBasedPsiElementBase)psi, (CompositeElement)node));
                    }

                    super.visitNode(node);
                }
            });
        }
        finally {
            LazyParseableElement.setSuppressEagerPsiCreation(false);
        }
        if (stubs.hasNext()) {
            reportStubAstMismatch("Stub list in " + getName() + " has more elements than PSI", stubTree, cachedDocument);
        }
        return result;
    }

    
    public IStubFileElementType getElementTypeForStubBuilder() {
        final IFileElementType type = LanguageParserDefinitions.INSTANCE.forLanguage(getLanguage()).getFileNodeType();
        return type instanceof IStubFileElementType ? (IStubFileElementType)type : null;
    }

    protected void reportStubAstMismatch(String message, StubTree stubTree, Document cachedDocument) {
        rebuildStub();
        clearStub(STUB_PSI_MISMATCH);
        scheduleDropCachesWithInvalidStubPsi();

        String msg = message;
        msg += "\n file=" + this;
        msg += ", modStamp=" + getModificationStamp();
        msg += "\n stub debugInfo=" + stubTree.getDebugInfo();
        msg += "\n document before=" + cachedDocument;

        ObjectStubTree latestIndexedStub = StubTreeLoader.getInstance().readFromVFile(getProject(), getVirtualFile());
        msg += "\nlatestIndexedStub=" + latestIndexedStub;
        if (latestIndexedStub != null) {
            msg += "\n   same size=" + (stubTree.getPlainList().size() == latestIndexedStub.getPlainList().size());
            msg += "\n   debugInfo=" + latestIndexedStub.getDebugInfo();
        }

        FileViewProvider viewProvider = getViewProvider();
        msg += "\n viewProvider=" + viewProvider;
        msg += "\n viewProvider stamp: " + viewProvider.getModificationStamp();

        VirtualFile file = viewProvider.getVirtualFile();
        msg += "; file stamp: " + file.getModificationStamp();
        msg += "; file modCount: " + file.getModificationCount();
        msg += "; file length: " + file.getLength();

        Document document = FileDocumentManager.getInstance().getCachedDocument(file);
        if (document != null) {
            msg += "\n doc saved: " + !FileDocumentManager.getInstance().isDocumentUnsaved(document);
            msg += "; doc stamp: " + document.getModificationStamp();
            msg += "; doc size: " + document.getTextLength();
            msg += "; committed: " + PsiDocumentManager.getInstance(getProject()).isCommitted(document);
        }

        msg += "\nindexing info: " + StubTreeLoader.getInstance().getIndexingStampDebugInfo(file);

        throw new AssertionError(msg + "\n------------\n");
    }

    private void scheduleDropCachesWithInvalidStubPsi() {
        // invokeLater even if already on EDT, because
        // we might be inside an index query and write actions might result in deadlocks there (https://youtrack.jetbrains.com/issue/IDEA-123118)
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        ((PsiModificationTrackerImpl)getManager().getModificationTracker()).incCounter();
                    }
                });
            }
        });
    }

    
    protected FileElement createFileElement(CharSequence docText) {
        final FileElement treeElement;
        final TreeElement contentLeaf = createContentLeafElement(docText);

        if (contentLeaf instanceof FileElement) {
            treeElement = (FileElement)contentLeaf;
        }
        else {
            final CompositeElement xxx = ASTFactory.composite(myElementType);
            assert xxx instanceof FileElement : "BUMM";
            treeElement = (FileElement)xxx;
            treeElement.rawAddChildrenWithoutNotifications(contentLeaf);
        }

        return treeElement;
    }

    public void unloadContent() {
        ApplicationManager.getApplication().assertWriteAccessAllowed();
        clearCaches();
        myViewProvider.beforeContentsSynchronized();
        synchronized (PsiLock.LOCK) {
            FileElement treeElement = derefTreeElement();
            DebugUtil.startPsiModification("unloadContent");
            try {
                if (treeElement != null) {
                    myTreeElementPointer = null;
                    treeElement.detachFromFile();
                    DebugUtil.onInvalidated(treeElement);
                }
                clearStub("unloadContent");
            }
            finally {
                DebugUtil.finishPsiModification();
            }
        }
        myViewProvider.contentsSynchronized();
    }

    private void clearStub( String reason) {
        StubTree stubHolder = SoftReference.dereference(myStub);
        if (stubHolder != null) {
            ((PsiFileStubImpl<?>)stubHolder.getRoot()).clearPsi(reason);
        }
        myStub = null;
    }

    public void clearCaches() {
        myModificationStamp ++;
    }

    @Override
    public String getText() {
        return getViewProvider().getContents().toString();
    }

    @Override
    public int getTextLength() {
        final ASTNode tree = derefTreeElement();
        if (tree != null) return tree.getTextLength();

        return getViewProvider().getContents().length();
    }

    @Override
    public TextRange getTextRange() {
        return new TextRange(0, getTextLength());
    }

    @Override
    public PsiElement getNextSibling() {
        return SharedPsiElementImplUtil.getNextSibling(this);
    }

    @Override
    public PsiElement getPrevSibling() {
        return SharedPsiElementImplUtil.getPrevSibling(this);
    }

    @Override
    public long getModificationStamp() {
        return myModificationStamp;
    }

    @Override
    public void subtreeChanged() {
        doClearCaches("subtreeChanged");
        getViewProvider().rootChanged(this);
    }

    private void doClearCaches(String reason) {
        final FileElement tree = getTreeElement();
        if (tree != null) {
            tree.clearCaches();
        }

        synchronized (PsiLock.LOCK) {
            clearStub(reason);
        }
        if (tree != null) {
            tree.putUserData(STUB_TREE_IN_PARSED_TREE, null);
        }

        clearCaches();
    }

    @Override
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
    protected PsiFileImpl clone() {
        FileViewProvider viewProvider = getViewProvider();
        FileViewProvider providerCopy = viewProvider.clone();
        final Language language = getLanguage();
        if (providerCopy == null) {
            throw new AssertionError("Unable to clone the view provider: " + viewProvider + "; " + language);
        }
        PsiFileImpl clone = BlockSupportImpl.getFileCopy(this, providerCopy);
        copyCopyableDataTo(clone);

        if (getTreeElement() != null) {
            // not set by provider in clone
            final FileElement treeClone = (FileElement)calcTreeElement().clone();
            clone.setTreeElementPointer(treeClone); // should not use setTreeElement here because cloned file still have VirtualFile (SCR17963)
            treeClone.setPsi(clone);
        }

        if (viewProvider.isEventSystemEnabled()) {
            clone.myOriginalFile = this;
        }
        else if (myOriginalFile != null) {
            clone.myOriginalFile = myOriginalFile;
        }

        return clone;
    }

    @Override
     public String getName() {
        return getViewProvider().getVirtualFile().getName();
    }

    @Override
    public PsiElement setName( String name) throws IncorrectOperationException {
        checkSetName(name);
        doClearCaches("setName");
        return PsiFileImplUtil.setName(this, name);
    }

    @Override
    public void checkSetName(String name) throws IncorrectOperationException {
        if (!getViewProvider().isEventSystemEnabled()) return;
        PsiFileImplUtil.checkSetName(this, name);
    }

    @Override
    public boolean isWritable() {
        return getViewProvider().getVirtualFile().isWritable();
    }

    @Override
    public PsiDirectory getParent() {
        return getContainingDirectory();
    }

    @Override
    
    public PsiDirectory getContainingDirectory() {
        final VirtualFile parentFile = getViewProvider().getVirtualFile().getParent();
        if (parentFile == null) return null;
        return getManager().findDirectory(parentFile);
    }

    @Override
    
    public PsiFile getContainingFile() {
        return this;
    }

    @Override
    public void delete() throws IncorrectOperationException {
        checkDelete();
        PsiFileImplUtil.doDelete(this);
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        if (!getViewProvider().isEventSystemEnabled()) {
            throw new IncorrectOperationException();
        }
        CheckUtil.checkWritable(this);
    }

    @Override
    
    public PsiFile getOriginalFile() {
        return myOriginalFile == null ? this : myOriginalFile;
    }

    public void setOriginalFile( final PsiFile originalFile) {
        myOriginalFile = originalFile.getOriginalFile();
    }

    @Override
    
    public PsiFile[] getPsiRoots() {
        final FileViewProvider viewProvider = getViewProvider();
        final Set<Language> languages = viewProvider.getLanguages();

        final PsiFile[] roots = new PsiFile[languages.size()];
        int i = 0;
        for (Language language : languages) {
            PsiFile psi = viewProvider.getPsi(language);
            if (psi == null) {
                LOG.error("PSI is null for "+language+"; in file: "+this);
            }
            roots[i++] = psi;
        }
        if (roots.length > 1) {
            Arrays.sort(roots, FILE_BY_LANGUAGE_ID);
        }
        return roots;
    }
    private static final Comparator<PsiFile> FILE_BY_LANGUAGE_ID = new Comparator<PsiFile>() {
        @Override
        public int compare(PsiFile o1, PsiFile o2) {
            return o1.getLanguage().getID().compareTo(o2.getLanguage().getID());
        }
    };

    @Override
    public boolean isPhysical() {
        // TODO[ik] remove this shit with dummy file system
        return getViewProvider().isEventSystemEnabled();
    }

    @Override
    
    public Language getLanguage() {
        return myElementType.getLanguage();
    }

    @Override
    
    public FileViewProvider getViewProvider() {
        return myViewProvider;
    }

    public void setTreeElementPointer(FileElement element) {
        myTreeElementPointer = element;
    }

    @Override
    public PsiElement findElementAt(int offset) {
        return getViewProvider().findElementAt(offset);
    }

    @Override
    public PsiReference findReferenceAt(int offset) {
        return getViewProvider().findReferenceAt(offset);
    }

    @Override
    
    public char[] textToCharArray() {
        return CharArrayUtil.fromSequence(getViewProvider().getContents());
    }

    
    public <T> T[] findChildrenByClass(Class<T> aClass) {
        List<T> result = new ArrayList<T>();
        for (PsiElement child : getChildren()) {
            if (aClass.isInstance(child)) result.add((T)child);
        }
        return result.toArray((T[]) Array.newInstance(aClass, result.size()));
    }

    
    public <T> T findChildByClass(Class<T> aClass) {
        for (PsiElement child : getChildren()) {
            if (aClass.isInstance(child)) return (T)child;
        }
        return null;
    }

    public boolean isTemplateDataFile() {
        return false;
    }

    @Override
    public PsiElement getContext() {
        return FileContextUtil.getFileContext(this);
    }

    @Override
    public void onContentReload() {
        subtreeChanged(); // important! otherwise cached information is not released
        if (isContentsLoaded()) {
            unloadContent();
        }
    }

    
    public StubElement getStub() {
        StubTree stubHolder = getStubTree();
        return stubHolder != null ? stubHolder.getRoot() : null;
    }

    @Override
    
    public StubTree getStubTree() {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        if (Boolean.TRUE.equals(getUserData(BUILDING_STUB))) return null;

        final StubTree derefd = derefStub();
        if (derefd != null) return derefd;
        if (getTreeElement() != null) return null;

        if (!(getContentElementType() instanceof IStubFileElementType)) return null;

        final VirtualFile vFile = getVirtualFile();
        if (!(vFile instanceof VirtualFileWithId)) return null;

        ObjectStubTree tree = StubTreeLoader.getInstance().readOrBuild(getProject(), vFile, this);
        if (!(tree instanceof StubTree)) return null;
        StubTree stubHolder = (StubTree)tree;
        final List<Pair<IStubFileElementType, PsiFile>> roots = StubTreeBuilder.getStubbedRoots(getViewProvider());

        synchronized (PsiLock.LOCK) {
            if (getTreeElement() != null) return null;

            final StubTree derefdOnLock = derefStub();
            if (derefdOnLock != null) return derefdOnLock;

            final PsiFileStub[] stubRoots = stubHolder.getRoot().getStubRoots();
            int matchingRoot = 0;
            for (Pair<IStubFileElementType, PsiFile> root : roots) {
                final PsiFileStub matchingStub = stubRoots[matchingRoot++];
                //noinspection unchecked
                ((StubBase)matchingStub).setPsi(root.second);
                final StubTree stubTree = new StubTree(matchingStub);
                stubTree.setDebugInfo("created in getStubTree()");
                if (root.second == this) stubHolder = stubTree;
                ((PsiFileImpl)root.second).myStub = new SoftReference<StubTree>(stubTree);
            }
            return stubHolder;
        }
    }

    
    private StubTree derefStub() {
        if (myStub == null) return  null;

        synchronized (PsiLock.LOCK) {
            return SoftReference.dereference(myStub);
        }
    }

    protected PsiFileImpl cloneImpl(FileElement treeElementClone) {
        PsiFileImpl clone = (PsiFileImpl)super.clone();
        clone.setTreeElementPointer(treeElementClone); // should not use setTreeElement here because cloned file still have VirtualFile (SCR17963)
        treeElementClone.setPsi(clone);
        return clone;
    }

    private boolean isKeepTreeElementByHardReference() {
        return !getViewProvider().isEventSystemEnabled();
    }

    
    private Getter<FileElement> createTreeElementPointer( FileElement treeElement) {
        if (isKeepTreeElementByHardReference()) {
            return treeElement;
        }
        return myManager.isBatchFilesProcessingMode()
                ? new PatchedWeakReference<FileElement>(treeElement)
                : new SoftReference<FileElement>(treeElement);
    }

    @Override
    public PsiManager getManager() {
        return myManager;
    }

    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public PsiElement getOriginalElement() {
        return this;
    }

    
    public final FileElement calcTreeElement() {
        // Attempt to find (loaded) tree element without taking lock first.
        FileElement treeElement = getTreeElement();
        if (treeElement != null) return treeElement;

        return loadTreeElement();
    }

    @Override
    
    public PsiElement[] getChildren() {
        return calcTreeElement().getChildrenAsPsiElements((TokenSet)null, PsiElement.ARRAY_FACTORY);
    }

    @Override
    public PsiElement getFirstChild() {
        return SharedImplUtil.getFirstChild(getNode());
    }

    @Override
    public PsiElement getLastChild() {
        return SharedImplUtil.getLastChild(getNode());
    }

    @Override
    public void acceptChildren( PsiElementVisitor visitor) {
        SharedImplUtil.acceptChildren(visitor, getNode());
    }

    @Override
    public int getStartOffsetInParent() {
        return calcTreeElement().getStartOffsetInParent();
    }

    @Override
    public int getTextOffset() {
        return calcTreeElement().getTextOffset();
    }

    @Override
    public boolean textMatches( CharSequence text) {
        return calcTreeElement().textMatches(text);
    }

    @Override
    public boolean textMatches( PsiElement element) {
        return calcTreeElement().textMatches(element);
    }

    @Override
    public boolean textContains(char c) {
        return calcTreeElement().textContains(c);
    }

    @Override
    public final PsiElement copy() {
        return clone();
    }

    @Override
    public PsiElement add( PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, null, null);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public PsiElement addBefore( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public PsiElement addAfter( PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        calcTreeElement().addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    public final void checkAdd( PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    public PsiElement addRangeBefore( PsiElement first,  PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        if (first == null) {
            LOG.assertTrue(last == null);
            return;
        }
        ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
        ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);
        CompositeElement treeElement = calcTreeElement();
        LOG.assertTrue(firstElement.getTreeParent() == treeElement);
        LOG.assertTrue(lastElement.getTreeParent() == treeElement);
        CodeEditUtil.removeChildren(treeElement, firstElement, lastElement);
    }

    @Override
    public PsiElement replace( PsiElement newElement) throws IncorrectOperationException {
        CompositeElement treeElement = calcTreeElement();
        return SharedImplUtil.doReplace(this, treeElement, newElement);
    }

    @Override
    public PsiReference getReference() {
        return null;
    }

    @Override
    
    public PsiReference[] getReferences() {
        return SharedPsiElementImplUtil.getReferences(this);
    }

    @Override
    public boolean processDeclarations( PsiScopeProcessor processor,
                                        ResolveState state,
                                       PsiElement lastParent,
                                        PsiElement place) {
        return true;
    }

    @Override
    
    public GlobalSearchScope getResolveScope() {
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @Override
    
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public String getPresentableText() {
                return getName();
            }

            @Override
            public String getLocationString() {
                final PsiDirectory psiDirectory = getParent();
                if (psiDirectory != null) {
                    return psiDirectory.getVirtualFile().getPresentableUrl();
                }
                return null;
            }

            @Override
            public Icon getIcon(final boolean open) {
                return null ;//PsiFileImpl.this.getIcon(0);
            }
        };
    }

    @Override
    public void navigate(boolean requestFocus) {
        assert canNavigate() : this;
        //noinspection ConstantConditions
        PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this);
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    
    public Project getProject() {
        final PsiManager manager = getManager();
        if (manager == null) throw new PsiInvalidElementAccessException(this);

        return manager.getProject();
    }

    
    @Override
    public FileASTNode getNode() {
        return calcTreeElement();
    }

    @Override
    public boolean isEquivalentTo(final PsiElement another) {
        return this == another;
    }

    private static final Key<SoftReference<StubTree>> STUB_TREE_IN_PARSED_TREE = Key.create("STUB_TREE_IN_PARSED_TREE");
    private final Object myStubFromTreeLock = new Object();

    public StubTree calcStubTree() {
        FileElement fileElement = calcTreeElement();
        synchronized (myStubFromTreeLock) {
            SoftReference<StubTree> ref = fileElement.getUserData(STUB_TREE_IN_PARSED_TREE);
            StubTree tree = SoftReference.dereference(ref);

            if (tree == null) {
                ApplicationManager.getApplication().assertReadAccessAllowed();
                IStubFileElementType contentElementType = getElementTypeForStubBuilder();
                if (contentElementType == null) {
                    VirtualFile vFile = getVirtualFile();
                    String message = "ContentElementType: " + getContentElementType() + "; file: " + this +
                            "\n\t" + "Boolean.TRUE.equals(getUserData(BUILDING_STUB)) = " + Boolean.TRUE.equals(getUserData(BUILDING_STUB)) +
                            "\n\t" + "getTreeElement() = " + getTreeElement() +
                            "\n\t" + "vFile instanceof VirtualFileWithId = " + (vFile instanceof VirtualFileWithId) +
                            "\n\t" + "StubUpdatingIndex.canHaveStub(vFile) = " + StubTreeLoader.getInstance().canHaveStub(vFile);
                    rebuildStub();
                    throw new AssertionError(message);
                }

                StubElement currentStubTree = contentElementType.getBuilder().buildStubTree(this);
                if (currentStubTree == null) {
                    throw new AssertionError("Stub tree wasn't built for " + contentElementType + "; file: " + this);
                }

                tree = new StubTree((PsiFileStub)currentStubTree);
                tree.setDebugInfo("created in calcStubTree");
                try {
                    TreeUtil.bindStubsToTree(this, tree);
                }
                catch (TreeUtil.StubBindingException e) {
                    rebuildStub();
                    throw new RuntimeException("Stub and PSI element type mismatch in " + getName(), e);
                }

                fileElement.putUserData(STUB_TREE_IN_PARSED_TREE, new SoftReference<StubTree>(tree));
            }

            return tree;
        }
    }

    private void rebuildStub() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                myManager.dropResolveCaches();

                final VirtualFile vFile = getVirtualFile();
                if (vFile != null && vFile.isValid()) {
                    final Document doc = FileDocumentManager.getInstance().getCachedDocument(vFile);
                    if (doc != null) {
                        FileDocumentManager.getInstance().saveDocument(doc);
                    }

                    FileContentUtilCore.reparseFiles(vFile);
                    StubTreeLoader.getInstance().rebuildStubTree(vFile);
                }
            }
        }, ModalityState.NON_MODAL);
    }

//    @Override
//    public void putInfo( Map<String, String> info) {
//        putInfo(this, info);
//    }

    public static void putInfo(PsiFile psiFile, Map<String, String> info) {
        info.put("fileName", psiFile.getName());
        info.put("fileType", psiFile.getFileType().toString());
    }

    @Override
    public String toString() {
        return myElementType.toString();
    }
}
