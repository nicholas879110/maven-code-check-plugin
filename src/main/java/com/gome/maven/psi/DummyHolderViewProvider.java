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
package com.gome.maven.psi;

import com.gome.maven.lang.Language;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.impl.PsiManagerEx;
import com.gome.maven.psi.impl.SharedPsiElementImplUtil;
import com.gome.maven.psi.impl.source.DummyHolder;
import com.gome.maven.psi.impl.source.PsiFileImpl;
import com.gome.maven.psi.impl.source.tree.LeafElement;
import com.gome.maven.testFramework.LightVirtualFile;
import com.gome.maven.util.LocalTimeCounter;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DummyHolderViewProvider extends UserDataHolderBase implements FileViewProvider{
    private DummyHolder myHolder;
    private final PsiManager myManager;
    private final long myModificationStamp;
    private final LightVirtualFile myLightVirtualFile = new LightVirtualFile("DummyHolder");

    public DummyHolderViewProvider( PsiManager manager) {
        myManager = manager;
        myModificationStamp = LocalTimeCounter.currentTime();
    }

    @Override
    
    public PsiManager getManager() {
        return myManager;
    }

    @Override
    
    public Document getDocument() {
        return FileDocumentManager.getInstance().getDocument(getVirtualFile());
    }

    @Override
    
    public CharSequence getContents() {
        return myHolder != null ? myHolder.getNode().getText() : "";
    }

    @Override
    
    public VirtualFile getVirtualFile() {
        return myLightVirtualFile;
    }

    @Override
    
    public Language getBaseLanguage() {
        return myHolder.getLanguage();
    }

    @Override
    
    public Set<Language> getLanguages() {
        return Collections.singleton(getBaseLanguage());
    }

    @Override
    public PsiFile getPsi( Language target) {
        ((PsiManagerEx)myManager).getFileManager().setViewProvider(getVirtualFile(), this);
        return target == getBaseLanguage() ? myHolder : null;
    }

    @Override
    
    public List<PsiFile> getAllFiles() {
        return Collections.singletonList(getPsi(getBaseLanguage()));
    }

    @Override
    public void beforeContentsSynchronized() {}

    @Override
    public void contentsSynchronized() {}

    @Override
    public boolean isEventSystemEnabled() {
        return false;
    }

    @Override
    public boolean isPhysical() {
        return false;
    }

    @Override
    public long getModificationStamp() {
        return myModificationStamp;
    }

    @Override
    public boolean supportsIncrementalReparse( final Language rootLanguage) {
        return true;
    }

    @Override
    public void rootChanged( PsiFile psiFile) {
    }

    public void setDummyHolder( DummyHolder dummyHolder) {
        myHolder = dummyHolder;
        myLightVirtualFile.setFileType(dummyHolder.getFileType());
    }

    @Override
    public FileViewProvider clone(){
        throw new RuntimeException("Clone is not supported for DummyHolderProviders. Use DummyHolder clone directly.");
    }

    @Override
    public PsiReference findReferenceAt(final int offset) {
        return SharedPsiElementImplUtil.findReferenceAt(getPsi(getBaseLanguage()), offset);
    }

    @Override
    
    public PsiElement findElementAt(final int offset,  final Language language) {
        return language == getBaseLanguage() ? findElementAt(offset) : null;
    }


    @Override
    public PsiElement findElementAt(int offset,  Class<? extends Language> lang) {
        if (!lang.isAssignableFrom(getBaseLanguage().getClass())) return null;
        return findElementAt(offset);
    }

    @Override
    public PsiReference findReferenceAt(final int offsetInElement,  final Language language) {
        return language == getBaseLanguage() ? findReferenceAt(offsetInElement) : null;
    }

    
    @Override
    public FileViewProvider createCopy( final VirtualFile copy) {
        throw new RuntimeException("Clone is not supported for DummyHolderProviders. Use DummyHolder clone directly.");
    }

    
    @Override
    public PsiFile getStubBindingRoot() {
        return getPsi(getBaseLanguage());
    }

    @Override
    public PsiElement findElementAt(final int offset) {
        final LeafElement element = ((PsiFileImpl)getPsi(getBaseLanguage())).calcTreeElement().findLeafElementAt(offset);
        return element != null ? element.getPsi() : null;
    }
}
