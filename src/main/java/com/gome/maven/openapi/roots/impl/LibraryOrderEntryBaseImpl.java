/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.gome.maven.openapi.roots.impl;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.roots.DependencyScope;
import com.gome.maven.openapi.roots.LibraryOrSdkOrderEntry;
import com.gome.maven.openapi.roots.OrderRootType;
import com.gome.maven.openapi.roots.RootProvider;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.ArrayUtil;

/**
 *  @author dsl
 */
abstract class LibraryOrderEntryBaseImpl extends OrderEntryBaseImpl implements LibraryOrSdkOrderEntry {
    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.LibraryOrderEntryBaseImpl");
    protected final ProjectRootManagerImpl myProjectRootManagerImpl;
     protected DependencyScope myScope = DependencyScope.COMPILE;
     private RootProvider myCurrentlySubscribedRootProvider = null;

    LibraryOrderEntryBaseImpl( RootModelImpl rootModel,  ProjectRootManagerImpl instanceImpl) {
        super(rootModel);
        myProjectRootManagerImpl = instanceImpl;
    }

    protected final void init() {
        updateFromRootProviderAndSubscribe();
    }

    @Override
    
    public VirtualFile[] getFiles( OrderRootType type) {
        return getRootFiles(type);
    }

    @Override
    
    public String[] getUrls( OrderRootType type) {
        LOG.assertTrue(!getRootModel().getModule().isDisposed());
        return getRootUrls(type);
    }

    @Override
    public VirtualFile[] getRootFiles( OrderRootType type) {
        RootProvider rootProvider = getRootProvider();
        return rootProvider != null ? rootProvider.getFiles(type) : VirtualFile.EMPTY_ARRAY;
    }

    
    protected abstract RootProvider getRootProvider();

    @Override
    
    public String[] getRootUrls( OrderRootType type) {
        RootProvider rootProvider = getRootProvider();
        return rootProvider == null ? ArrayUtil.EMPTY_STRING_ARRAY : rootProvider.getUrls(type);
    }

    @Override
    
    public final Module getOwnerModule() {
        return getRootModel().getModule();
    }

    protected void updateFromRootProviderAndSubscribe() {
        getRootModel().makeExternalChange(new Runnable() {
            @Override
            public void run() {
                resubscribe(getRootProvider());
            }
        });
    }

    private void resubscribe(RootProvider wrapper) {
        unsubscribe();
        subscribe(wrapper);
    }

    private void subscribe( RootProvider wrapper) {
        if (wrapper != null) {
            myProjectRootManagerImpl.subscribeToRootProvider(this, wrapper);
        }
        myCurrentlySubscribedRootProvider = wrapper;
    }


    private void unsubscribe() {
        if (myCurrentlySubscribedRootProvider != null) {
            myProjectRootManagerImpl.unsubscribeFromRootProvider(this, myCurrentlySubscribedRootProvider);
        }
        myCurrentlySubscribedRootProvider = null;
    }

    @Override
    public void dispose() {
        unsubscribe();
        super.dispose();
    }
}
