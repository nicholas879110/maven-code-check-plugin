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
package com.gome.maven.profile;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.MainConfigurationStateSplitter;
import com.gome.maven.openapi.components.PersistentStateComponent;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Comparing;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.packageDependencies.DependencyValidationManager;
import com.gome.maven.psi.search.scope.packageSet.NamedScopesHolder;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.xmlb.XmlSerializer;
import com.gome.maven.util.xmlb.annotations.OptionTag;
import gnu.trove.THashMap;
import org.jdom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: anna
 * Date: 30-Nov-2005
 */
public abstract class DefaultProjectProfileManager extends ProjectProfileManager implements PersistentStateComponent<Element> {
    protected static final Logger LOG = Logger.getInstance("#com.gome.maven.profile.DefaultProjectProfileManager");

     public static final String SCOPES = "scopes";
     protected static final String SCOPE = "scope";
     public static final String PROFILE = "profile";
     protected static final String NAME = "name";

    private static final String VERSION = "1.0";

    
    protected final Project myProject;

    protected String myProjectProfile;
    /** This field is used for serialization. Do not rename it or make access weaker */
    public boolean USE_PROJECT_PROFILE = true;

    private final ApplicationProfileManager myApplicationProfileManager;

    private final Map<String, Profile> myProfiles = new THashMap<String, Profile>();
    protected final DependencyValidationManager myHolder;
    private final List<ProfileChangeAdapter> myProfilesListener = ContainerUtil.createLockFreeCopyOnWriteList();
     private static final String PROJECT_DEFAULT_PROFILE_NAME = "Project Default";

    public DefaultProjectProfileManager( final Project project,
                                         ApplicationProfileManager applicationProfileManager,
                                         DependencyValidationManager holder) {
        myProject = project;
        myHolder = holder;
        myApplicationProfileManager = applicationProfileManager;
    }

    
    public Project getProject() {
        return myProject;
    }

    @Override
    public synchronized Profile getProfile( String name, boolean returnRootProfileIfNamedIsAbsent) {
        return myProfiles.containsKey(name) ? myProfiles.get(name) : myApplicationProfileManager.getProfile(name, returnRootProfileIfNamedIsAbsent);
    }

    @Override
    public synchronized void updateProfile( Profile profile) {
        myProfiles.put(profile.getName(), profile);
        for (ProfileChangeAdapter profileChangeAdapter : myProfilesListener) {
            profileChangeAdapter.profileChanged(profile);
        }
    }

    
    @Override
    public synchronized Element getState() {
        Element state = new Element("settings");

        String[] sortedProfiles = myProfiles.keySet().toArray(new String[myProfiles.size()]);
        Arrays.sort(sortedProfiles);
        for (String profile : sortedProfiles) {
            final Profile projectProfile = myProfiles.get(profile);
            if (projectProfile != null) {
                Element profileElement = new Element(PROFILE);
                try {
                    projectProfile.writeExternal(profileElement);
                }
                catch (WriteExternalException e) {
                    LOG.error(e);
                }
                boolean hasSmthToSave = sortedProfiles.length > 1 || isDefaultProfileUsed();
                if (!hasSmthToSave) {
                    for (Element child : (List<Element>)profileElement.getChildren()) {
                        if (!child.getName().equals("option")) {
                            hasSmthToSave = true;
                            break;
                        }
                    }
                }
                if (!hasSmthToSave) {
                    continue;
                }

                state.addContent(profileElement);
            }
        }

        if (!state.getChildren().isEmpty() || isDefaultProfileUsed()) {
            XmlSerializer.serializeInto(this, state);
            state.addContent(new Element("version").setAttribute("value", VERSION));
        }
        return state;
    }

    @Override
    public synchronized void loadState(Element state) {
        myProfiles.clear();
        XmlSerializer.deserializeInto(this, state);
        for (Element o : (List<Element>)state.getChildren(PROFILE)) {
            Profile profile = myApplicationProfileManager.createProfile();
            profile.setProfileManager(this);
            try {
                profile.readExternal(o);
            }
            catch (InvalidDataException e) {
                LOG.error(e);
            }
            profile.setProjectLevel(true);
            myProfiles.put(profile.getName(), profile);
        }
        if (state.getChild("version") == null || !Comparing.strEqual(state.getChild("version").getAttributeValue("value"), VERSION)) {
            boolean toConvert = true;
            for (Element o : (List<Element>)state.getChildren("option")) {
                if (Comparing.strEqual(o.getAttributeValue("name"), "USE_PROJECT_LEVEL_SETTINGS")) {
                    toConvert = Boolean.parseBoolean(o.getAttributeValue("value"));
                    break;
                }
            }
            if (toConvert) {
                convert(state);
            }
        }
    }

    protected void convert(Element element) {
    }

    private boolean isDefaultProfileUsed() {
        return myProjectProfile != null && !Comparing.strEqual(myProjectProfile, PROJECT_DEFAULT_PROFILE_NAME);
    }

    
    @Override
    public NamedScopesHolder getScopesManager() {
        return myHolder;
    }

    
    @Override
    public synchronized Collection<Profile> getProfiles() {
        getProjectProfileImpl();
        return myProfiles.values();
    }

    
    @Override
    public synchronized String[] getAvailableProfileNames() {
        return ArrayUtil.toStringArray(myProfiles.keySet());
    }

    @Override
    public synchronized void deleteProfile( String name) {
        myProfiles.remove(name);
    }

    @Override
    @OptionTag("PROJECT_PROFILE")
    public synchronized String getProjectProfile() {
        return myProjectProfile;
    }

    @Override
    public synchronized void setProjectProfile( String newProfile) {
        if (Comparing.strEqual(newProfile, myProjectProfile)) {
            return;
        }

        String oldProfile = myProjectProfile;
        myProjectProfile = newProfile;
        USE_PROJECT_PROFILE = newProfile != null;
        if (oldProfile != null) {
            for (ProfileChangeAdapter adapter : myProfilesListener) {
                adapter.profileActivated(getProfile(oldProfile), newProfile != null ?  getProfile(newProfile) : null);
            }
        }
    }

    
    public synchronized Profile getProjectProfileImpl(){
        if (!USE_PROJECT_PROFILE) {
            return myApplicationProfileManager.getRootProfile();
        }
        if (myProjectProfile == null || myProfiles.isEmpty()){
            setProjectProfile(PROJECT_DEFAULT_PROFILE_NAME);
            final Profile projectProfile = myApplicationProfileManager.createProfile();
            projectProfile.copyFrom(myApplicationProfileManager.getRootProfile());
            projectProfile.setProjectLevel(true);
            projectProfile.setName(PROJECT_DEFAULT_PROFILE_NAME);
            myProfiles.put(PROJECT_DEFAULT_PROFILE_NAME, projectProfile);
        }
        else if (!myProfiles.containsKey(myProjectProfile)){
            setProjectProfile(myProfiles.keySet().iterator().next());
        }
        final Profile profile = myProfiles.get(myProjectProfile);
        profile.setProfileManager(this);
        return profile;
    }

    public void addProfilesListener( final ProfileChangeAdapter profilesListener,  Disposable parent) {
        myProfilesListener.add(profilesListener);
        Disposer.register(parent, new Disposable() {
            @Override
            public void dispose() {
                myProfilesListener.remove(profilesListener);
            }
        });
    }

    public void removeProfilesListener( ProfileChangeAdapter profilesListener) {
        myProfilesListener.remove(profilesListener);
    }

    public static class ProfileStateSplitter extends MainConfigurationStateSplitter {
        
        @Override
        protected String getSubStateFileName( Element element) {
            for (Element option : (List<Element>)element.getChildren("option")) {
                if (option.getAttributeValue("name").equals("myName")) {
                    return option.getAttributeValue("value");
                }
            }
            throw new IllegalStateException();
        }

        
        @Override
        protected String getComponentStateFileName() {
            return "profiles_settings";
        }

        
        @Override
        protected String getSubStateTagName() {
            return PROFILE;
        }
    }

    protected void fireProfilesInitialized() {
        for (ProfileChangeAdapter profileChangeAdapter : myProfilesListener) {
            profileChangeAdapter.profilesInitialized();
        }
    }
    protected void fireProfilesShutdown() {
        for (ProfileChangeAdapter profileChangeAdapter : myProfilesListener) {
            profileChangeAdapter.profilesShutdown();
        }
    }
}
