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
package com.gome.maven.profile.codeInspection;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.profile.ApplicationProfileManager;
import com.gome.maven.profile.Profile;
import com.gome.maven.profile.ProfileChangeAdapter;
import com.gome.maven.profile.ProfileEx;
import com.gome.maven.psi.search.scope.packageSet.NamedScope;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.JDOMException;

import java.io.IOException;
import java.util.List;

/**
 * User: anna
 * Date: 29-Nov-2005
 */
public abstract class InspectionProfileManager extends ApplicationProfileManager implements SeverityProvider {
     public static final String INSPECTION_DIR = "inspection";
     protected static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + '/' + INSPECTION_DIR;

    private final List<ProfileChangeAdapter> myProfileChangeAdapters = ContainerUtil.createLockFreeCopyOnWriteList();

    protected static final Logger LOG = Logger.getInstance("#com.gome.maven.profile.DefaultProfileManager");

    public static InspectionProfileManager getInstance() {
        return ServiceManager.getService(InspectionProfileManager.class);
    }

    public InspectionProfileManager() {
    }

    protected abstract void initProfiles();

    public abstract Profile loadProfile( String path) throws IOException, JDOMException;

    @Override
    public void addProfileChangeListener( final ProfileChangeAdapter listener) {
        myProfileChangeAdapters.add(listener);
    }

    @Override
    public void addProfileChangeListener( ProfileChangeAdapter listener,  Disposable parentDisposable) {
        ContainerUtil.add(listener, myProfileChangeAdapters, parentDisposable);
    }

    @Override
    public void removeProfileChangeListener( final ProfileChangeAdapter listener) {
        myProfileChangeAdapters.remove(listener);
    }

    @Override
    public void fireProfileChanged(final Profile profile) {
        if (profile instanceof ProfileEx) {
            ((ProfileEx)profile).profileChanged();
        }
        for (ProfileChangeAdapter adapter : myProfileChangeAdapters) {
            adapter.profileChanged(profile);
        }
    }

    @Override
    public void fireProfileChanged(final Profile oldProfile, final Profile profile, final NamedScope scope) {
        for (ProfileChangeAdapter adapter : myProfileChangeAdapters) {
            adapter.profileActivated(oldProfile, profile);
        }
    }

    @Override
    public Profile getProfile( final String name) {
        return getProfile(name, true);
    }
}
