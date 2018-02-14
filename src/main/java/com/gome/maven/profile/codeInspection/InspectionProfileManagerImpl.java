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
package com.gome.maven.profile.codeInspection;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.InspectionProfileConvertor;
import com.gome.maven.codeInsight.daemon.impl.DaemonListeners;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfoType;
import com.gome.maven.codeInsight.daemon.impl.SeveritiesProvider;
import com.gome.maven.codeInsight.daemon.impl.SeverityRegistrar;
import com.gome.maven.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.InspectionToolRegistrar;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.components.*;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.options.BaseSchemeProcessor;
import com.gome.maven.openapi.options.Scheme;
import com.gome.maven.openapi.options.SchemesManager;
import com.gome.maven.openapi.options.SchemesManagerFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectManager;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.profile.Profile;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.messages.MessageBus;
import com.gome.maven.util.ui.UIUtil;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

@State(
        name = "InspectionProfileManager",
        storages = {
                @Storage(file = StoragePathMacros.APP_CONFIG + "/editor.xml"),
                @Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml", deprecated = true)
        },
        additionalExportFile = InspectionProfileManager.FILE_SPEC
)
public class InspectionProfileManagerImpl extends InspectionProfileManager implements SeverityProvider, PersistentStateComponent<Element> {
    private final InspectionToolRegistrar myRegistrar;
    private final SchemesManager<Profile, InspectionProfileImpl> mySchemesManager;
    private final AtomicBoolean myProfilesAreInitialized = new AtomicBoolean(false);
    private final SeverityRegistrar mySeverityRegistrar;

    protected static final Logger LOG = Logger.getInstance("#com.intellij.profile.DefaultProfileManager");

    public static InspectionProfileManagerImpl getInstanceImpl() {
        return (InspectionProfileManagerImpl)ServiceManager.getService(InspectionProfileManager.class);
    }

    public InspectionProfileManagerImpl( InspectionToolRegistrar registrar,  SchemesManagerFactory schemesManagerFactory,  MessageBus messageBus) {
        myRegistrar = registrar;
        registerProvidedSeverities();

        mySchemesManager = schemesManagerFactory.createSchemesManager(FILE_SPEC, new BaseSchemeProcessor<InspectionProfileImpl>() {
            
            @Override
            public InspectionProfileImpl readScheme( Element element) {
                final InspectionProfileImpl profile = new InspectionProfileImpl(InspectionProfileLoadUtil.getProfileName(element), myRegistrar, InspectionProfileManagerImpl.this);
                try {
                    profile.readExternal(element);
                }
                catch (Exception ignored) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            Messages.showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, profile.getName()),
                                    InspectionsBundle.message("inspection.errors.occurred.dialog.title"));
                        }
                    }, ModalityState.NON_MODAL);
                }
                return profile;
            }

            
            @Override
            public State getState( InspectionProfileImpl scheme) {
                return scheme.isProjectLevel() ? State.NON_PERSISTENT : (scheme.wasInitialized() ? State.POSSIBLY_CHANGED : State.UNCHANGED);
            }

            @Override
            public Element writeScheme( InspectionProfileImpl scheme) {
                Element root = new Element("inspections");
                root.setAttribute("profile_name", scheme.getName());
                scheme.serializeInto(root, false);
                return root;
            }

            @Override
            public void onSchemeAdded( final InspectionProfileImpl scheme) {
                updateProfileImpl(scheme);
                fireProfileChanged(scheme);
                onProfilesChanged();
            }

            @Override
            public void onSchemeDeleted( final InspectionProfileImpl scheme) {
                onProfilesChanged();
            }

            @Override
            public void onCurrentSchemeChanged(final Scheme oldCurrentScheme) {
                Profile current = mySchemesManager.getCurrentScheme();
                if (current != null) {
                    fireProfileChanged((Profile)oldCurrentScheme, current, null);
                }
                onProfilesChanged();
            }
        }, RoamingType.PER_USER);
        mySeverityRegistrar = new SeverityRegistrar(messageBus);
    }

    
    private static InspectionProfileImpl createSampleProfile() {
        return new InspectionProfileImpl("Default");
    }

    public static void registerProvidedSeverities() {
        for (SeveritiesProvider provider : Extensions.getExtensions(SeveritiesProvider.EP_NAME)) {
            for (HighlightInfoType t : provider.getSeveritiesHighlightInfoTypes()) {
                HighlightSeverity highlightSeverity = t.getSeverity(null);
                SeverityRegistrar.registerStandard(t, highlightSeverity);
                TextAttributesKey attributesKey = t.getAttributesKey();
                Icon icon = t instanceof HighlightInfoType.Iconable ? ((HighlightInfoType.Iconable)t).getIcon() : null;
                HighlightDisplayLevel.registerSeverity(highlightSeverity, attributesKey, icon);
            }
        }
    }

    @Override
    
    public Collection<Profile> getProfiles() {
        initProfiles();
        return mySchemesManager.getAllSchemes();
    }

    private volatile boolean LOAD_PROFILES = !ApplicationManager.getApplication().isUnitTestMode();
    
    public void forceInitProfiles(boolean flag) {
        LOAD_PROFILES = flag;
        myProfilesAreInitialized.set(false);
    }

    @Override
    public void initProfiles() {
        if (myProfilesAreInitialized.getAndSet(true)) {
            if (mySchemesManager.getAllSchemes().isEmpty()) {
                createDefaultProfile();
            }
            return;
        }
        if (!LOAD_PROFILES) return;

        mySchemesManager.loadSchemes();
        Collection<Profile> profiles = mySchemesManager.getAllSchemes();
        if (profiles.isEmpty()) {
            createDefaultProfile();
        }
        else {
            for (Profile profile : profiles) {
                addProfile(profile);
            }
        }
    }

    private void createDefaultProfile() {
        final InspectionProfileImpl defaultProfile = (InspectionProfileImpl)createProfile();
        defaultProfile.setBaseProfile(InspectionProfileImpl.getDefaultProfile());
        addProfile(defaultProfile);
    }


    @Override
    public Profile loadProfile( String path) throws IOException, JDOMException {
        final File file = new File(path);
        if (file.exists()) {
            try {
                return InspectionProfileLoadUtil.load(file, myRegistrar, this);
            }
            catch (IOException e) {
                throw e;
            }
            catch (JDOMException e) {
                throw e;
            }
            catch (Exception ignored) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showErrorDialog(InspectionsBundle.message("inspection.error.loading.message", 0, file),
                                InspectionsBundle.message("inspection.errors.occurred.dialog.title"));
                    }
                }, ModalityState.NON_MODAL);
            }
        }
        return getProfile(path, false);
    }

    @Override
    public void updateProfile( Profile profile) {
        mySchemesManager.addNewScheme(profile, true);
        updateProfileImpl(profile);
    }

    private static void updateProfileImpl( Profile profile) {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            InspectionProjectProfileManager.getInstance(project).initProfileWrapper(profile);
        }
    }

    
    @Override
    public SeverityRegistrar getSeverityRegistrar() {
        return mySeverityRegistrar;
    }

    
    @Override
    public SeverityRegistrar getOwnSeverityRegistrar() {
        return mySeverityRegistrar;
    }

    
    @Override
    public Element getState() {
        Element state = new Element("state");
        try {
            mySeverityRegistrar.writeExternal(state);
        }
        catch (WriteExternalException e) {
            throw new RuntimeException(e);
        }
        return state;
    }

    @Override
    public void loadState(Element state) {
        try {
            mySeverityRegistrar.readExternal(state);
        }
        catch (InvalidDataException e) {
            throw new RuntimeException(e);
        }
    }

    public InspectionProfileConvertor getConverter() {
        return new InspectionProfileConvertor(this);
    }

    @Override
    public Profile createProfile() {
        return createSampleProfile();
    }

    @Override
    public void setRootProfile(String rootProfile) {
        Profile current = mySchemesManager.getCurrentScheme();
        if (current != null && !Comparing.strEqual(rootProfile, current.getName())) {
            fireProfileChanged(current, getProfile(rootProfile), null);
        }
        mySchemesManager.setCurrentSchemeName(rootProfile);
    }

    @Override
    public Profile getProfile( final String name, boolean returnRootProfileIfNamedIsAbsent) {
        Profile found = mySchemesManager.findSchemeByName(name);
        if (found != null) return found;
        //profile was deleted
        if (returnRootProfileIfNamedIsAbsent) {
            return getRootProfile();
        }
        return null;
    }

    
    @Override
    public Profile getRootProfile() {
        Profile current = mySchemesManager.getCurrentScheme();
        if (current != null) return current;
        Collection<Profile> profiles = getProfiles();
        if (profiles.isEmpty()) return createSampleProfile();
        return profiles.iterator().next();
    }

    @Override
    public void deleteProfile( final String profile) {
        Profile found = mySchemesManager.findSchemeByName(profile);
        if (found != null) {
            mySchemesManager.removeScheme(found);
        }
    }

    @Override
    public void addProfile( final Profile profile) {
        mySchemesManager.addNewScheme(profile, true);
    }

    @Override
    
    public String[] getAvailableProfileNames() {
        return ArrayUtil.toStringArray(mySchemesManager.getAllSchemeNames());
    }

    @Override
    public Profile getProfile( final String name) {
        return getProfile(name, true);
    }

    public static void onProfilesChanged() {
        //cleanup caches blindly for all projects in case ide profile was modified
        for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
            //noinspection EmptySynchronizedStatement
            synchronized (HighlightingSettingsPerFile.getInstance(project)) {
            }

            UIUtil.invokeLaterIfNeeded(new Runnable() {
                @Override
                public void run() {
                    if (!project.isDisposed()) {
                        DaemonListeners.getInstance(project).updateStatusBar();
                    }
                }
            });
        }
    }
}
