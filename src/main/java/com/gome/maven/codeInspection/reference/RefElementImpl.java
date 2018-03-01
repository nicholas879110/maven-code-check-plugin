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
 * Date: Oct 21, 2001
 * Time: 4:28:53 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.reference;

import com.gome.maven.codeInspection.SuppressionUtil;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleUtilCore;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Iconable;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.SmartPsiElementPointer;
import com.gome.maven.util.containers.ContainerUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class RefElementImpl extends RefEntityImpl implements RefElement {
    protected static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.reference.RefElement");

    private static final int IS_ENTRY_MASK = 0x80;
    private static final int IS_PERMANENT_ENTRY_MASK = 0x100;


    private final SmartPsiElementPointer myID;

    private List<RefElement> myOutReferences;
    private List<RefElement> myInReferences;

    private String[] mySuppressions = null;

    private boolean myIsDeleted ;
    private final Module myModule;
    protected static final int IS_REACHABLE_MASK = 0x40;

    protected RefElementImpl(String name,  RefElement owner) {
        super(name, owner.getRefManager());
        myID = null;
        myFlags = 0;
        myModule = ModuleUtilCore.findModuleForPsiElement(owner.getElement());
    }

    protected RefElementImpl(PsiFile file, RefManager manager) {
        this(file.getName(), file, manager);
    }

    protected RefElementImpl(String name,  PsiElement element,  RefManager manager) {
        super(name, manager);
        myID = SmartPointerManager.getInstance(manager.getProject()).createSmartPsiElementPointer(element);
        myFlags = 0;
        myModule = ModuleUtilCore.findModuleForPsiElement(element);
    }

    @Override
    public boolean isValid() {
        if (myIsDeleted) return false;
        return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
            @Override
            public Boolean compute() {

                final PsiFile file = myID.getContainingFile();
                //no need to check resolve in offline mode
                if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
                    return file != null && file.isPhysical();
                }

                final PsiElement element = getElement();
                return element != null && element.isPhysical();
            }
        }).booleanValue();
    }

    @Override
    
    public Icon getIcon(final boolean expanded) {
        final PsiElement element = getElement();
        if (element != null && element.isValid()) {
            return element.getIcon(Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS);
        }
        return null;
    }

    @Override
    public RefModule getModule() {
        return myManager.getRefModule(myModule);
    }

    @Override
    public String getExternalName() {
        return getName();
    }

    @Override
    
    public PsiElement getElement() {
        return myID.getElement();
    }

    
    public PsiFile getContainingFile() {
        return myID.getContainingFile();
    }

    public VirtualFile getVirtualFile() {
        return myID.getVirtualFile();
    }

    @Override
    public SmartPsiElementPointer getPointer() {
        return myID;
    }

    public void buildReferences() {
    }

    @Override
    public boolean isReachable() {
        return checkFlag(IS_REACHABLE_MASK);
    }

    @Override
    public boolean isReferenced() {
        return !getInReferences().isEmpty();
    }

    public boolean hasSuspiciousCallers() {
        for (RefElement refCaller : getInReferences()) {
            if (((RefElementImpl)refCaller).isSuspicious()) return true;
        }

        return false;
    }

    @Override
    
    public Collection<RefElement> getOutReferences() {
        if (myOutReferences == null){
            return ContainerUtil.emptyList();
        }
        return myOutReferences;
    }

    @Override
    
    public Collection<RefElement> getInReferences() {
        if (myInReferences == null){
            return ContainerUtil.emptyList();
        }
        return myInReferences;
    }

    public void addInReference(RefElement refElement) {
        if (!getInReferences().contains(refElement)) {
            if (myInReferences == null){
                myInReferences = new ArrayList<RefElement>(1);
            }
            myInReferences.add(refElement);
        }
    }

    public void addOutReference(RefElement refElement) {
        if (!getOutReferences().contains(refElement)) {
            if (myOutReferences == null){
                myOutReferences = new ArrayList<RefElement>(1);
            }
            myOutReferences.add(refElement);
        }
    }

    public void setEntry(boolean entry) {
        setFlag(entry, IS_ENTRY_MASK);
    }

    @Override
    public boolean isEntry() {
        return checkFlag(IS_ENTRY_MASK);
    }

    @Override
    public boolean isPermanentEntry() {
        return checkFlag(IS_PERMANENT_ENTRY_MASK);
    }


    @Override
    
    public RefElement getContainingEntry() {
        return this;
    }

    public void setPermanentEntry(boolean permanentEntry) {
        setFlag(permanentEntry, IS_PERMANENT_ENTRY_MASK);
    }

    public boolean isSuspicious() {
        return !isReachable();
    }

    public void referenceRemoved() {
        myIsDeleted = true;
        if (getOwner() != null) {
            ((RefEntityImpl)getOwner()).removeChild(this);
        }

        for (RefElement refCallee : getOutReferences()) {
            refCallee.getInReferences().remove(this);
        }

        for (RefElement refCaller : getInReferences()) {
            refCaller.getOutReferences().remove(this);
        }
    }

    
    public String getURL() {
        final PsiElement element = getElement();
        if (element == null || !element.isPhysical()) return null;
        final PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) return null;
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) return null;
        return virtualFile.getUrl() + "#" + element.getTextOffset();
    }

    protected abstract void initialize();

    public void addSuppression(final String text) {
        mySuppressions = text.split("[, ]");
    }

    public boolean isSuppressed( String... toolId) {
        if (mySuppressions != null) {
            for ( String suppression : mySuppressions) {
                for (String id : toolId) {
                    if (suppression.equals(id)) return true;
                }
                if (suppression.equalsIgnoreCase(SuppressionUtil.ALL)){
                    return true;
                }
            }
        }
        final RefEntity entity = getOwner();
        return entity instanceof RefElementImpl && ((RefElementImpl)entity).isSuppressed(toolId);
    }
}
