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
 * User: Anna.Kozlova
 * Date: 31-Jul-2006
 * Time: 17:44:39
 */
package com.gome.maven.profile.codeInspection.ui.header;

import com.gome.maven.codeInsight.daemon.impl.HighlightInfoType;
import com.gome.maven.codeInsight.daemon.impl.SeverityRegistrar;
import com.gome.maven.codeInspection.ModifiableModel;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.InspectionToolRegistrar;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.colors.CodeInsightColors;
import com.gome.maven.openapi.editor.colors.TextAttributesKey;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.fileChooser.FileChooser;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptorFactory;
import com.gome.maven.openapi.fileTypes.StdFileTypes;
import com.gome.maven.openapi.options.BaseConfigurable;
import com.gome.maven.openapi.options.Configurable;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.options.SearchableConfigurable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.profile.Profile;
import com.gome.maven.profile.ProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProfileManager;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.profile.codeInspection.ui.ErrorsConfigurable;
import com.gome.maven.profile.codeInspection.ui.SingleInspectionProfilePanel;
import com.gome.maven.ui.IdeBorderFactory;
import com.gome.maven.ui.ListCellRendererWrapper;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.SystemProperties;
import com.gome.maven.util.containers.HashMap;
import com.gome.maven.util.ui.UIUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public abstract class InspectionToolsConfigurable extends BaseConfigurable
        implements ErrorsConfigurable, SearchableConfigurable, Configurable.NoScroll {
    public static final String ID = "Errors";
    public static final String DISPLAY_NAME = "Inspections";
    private static final String HEADER_TITLE = "Profile:";

    private static final Logger LOG = Logger.getInstance("#" + InspectionToolsConfigurable.class.getName());
    protected final InspectionProfileManager myProfileManager;
    protected final InspectionProjectProfileManager myProjectProfileManager;
    private final CardLayout myLayout = new CardLayout();
    private final AuxiliaryRightPanel myAuxiliaryRightPanel;
    private final Map<Profile, SingleInspectionProfilePanel> myPanels =
            new HashMap<Profile, SingleInspectionProfilePanel>();
    private final List<Profile> myDeletedProfiles = new ArrayList<Profile>();
    protected ProfilesConfigurableComboBox myProfiles;
    private JPanel myPanel;
    private JPanel myWholePanel;
    private Alarm mySelectionAlarm;

    public InspectionToolsConfigurable( final InspectionProjectProfileManager projectProfileManager,
                                       InspectionProfileManager profileManager) {
        myWholePanel = new JPanel();

        myWholePanel.setLayout(new BorderLayout());

        final JPanel toolbar = new JPanel();
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));

        myPanel = new JPanel();

        myWholePanel.add(toolbar, BorderLayout.PAGE_START);
        myWholePanel.add(myPanel, BorderLayout.CENTER);

        myProfiles = new ProfilesConfigurableComboBox(new ListCellRendererWrapper<Profile>() {
            @Override
            public void customize(final JList list, final Profile value, final int index, final boolean selected, final boolean hasFocus) {
                final SingleInspectionProfilePanel singleInspectionProfilePanel = myPanels.get(value);
                final boolean isShared = singleInspectionProfilePanel.isProfileShared();
                setIcon(isShared ? AllIcons.General.ProjectSettings : AllIcons.General.Settings);
                setText(singleInspectionProfilePanel.getCurrentProfileName());
            }
        }) {
            @Override
            public void onProfileChosen(InspectionProfileImpl inspectionProfile) {
                myLayout.show(myPanel, getCardName(inspectionProfile));
                myAuxiliaryRightPanel.showDescription(inspectionProfile.getDescription());
            }
        };
        JPanel profilesHolder = new JPanel();
        profilesHolder.setLayout(new CardLayout());


        JComponent manageButton = new ManageButton(new ManageButtonBuilder() {
            @Override
            public boolean isSharedToTeamMembers() {
                SingleInspectionProfilePanel panel = getSelectedPanel();
                return panel != null && panel.isProfileShared();
            }

            @Override
            public void setShareToTeamMembers(boolean shared) {
                final SingleInspectionProfilePanel selectedPanel = getSelectedPanel();
                LOG.assertTrue(selectedPanel != null, "No settings selectedPanel for: " + getSelectedObject());

                final String name = getSelectedPanel().getCurrentProfileName();
                for (SingleInspectionProfilePanel p : myPanels.values()) {
                    if (p != selectedPanel && Comparing.equal(p.getCurrentProfileName(), name)) {
                        final boolean curShared = p.isProfileShared();
                        if (curShared == shared) {
                            Messages.showErrorDialog((shared ? "Shared" : "Application level") + " profile with same name exists.", "Inspections Settings");
                            return;
                        }
                    }
                }

                selectedPanel.setProfileShared(shared);
                myProfiles.repaint();
            }

            @Override
            public void copy() {
                final InspectionProfileImpl newProfile = copyToNewProfile(getSelectedObject(), getProject());
                if (newProfile != null) {
                    final InspectionProfileImpl modifiableModel = (InspectionProfileImpl)newProfile.getModifiableModel();
                    modifiableModel.setModified(true);
                    modifiableModel.setProjectLevel(false);
                    addProfile(modifiableModel, newProfile);
                    rename(modifiableModel);
                }
            }

            @Override
            public boolean canRename() {
                final InspectionProfileImpl profile = getSelectedObject();
                return !profile.isProfileLocked();
            }

            @Override
            public void rename() {
                rename(getSelectedObject());
            }

            private void rename( final InspectionProfileImpl inspectionProfile) {
                final String initialName = getSelectedPanel().getCurrentProfileName();
                myProfiles.showEditCard(initialName, new SaveInputComponentValidator() {
                    @Override
                    public void doSave( String text) {
                        if (!text.equals(initialName)) {
                            getProfilePanel(inspectionProfile).setCurrentProfileName(text);
                        }
                        myProfiles.showComboBoxCard();
                    }

                    @Override
                    public boolean checkValid( String text) {
                        final SingleInspectionProfilePanel singleInspectionProfilePanel = myPanels.get(inspectionProfile);
                        if (singleInspectionProfilePanel == null) {
                            return false;
                        }
                        final boolean isValid = text.equals(initialName) || !hasName(text, singleInspectionProfilePanel.isProfileShared());
                        if (isValid) {
                            myAuxiliaryRightPanel.showDescription(getSelectedObject().getDescription());
                        }
                        else {
                            myAuxiliaryRightPanel.showError("Name is already in use. Please change name to unique.");
                        }
                        return isValid;
                    }

                    @Override
                    public void cancel() {
                        myProfiles.showComboBoxCard();
                        myAuxiliaryRightPanel.showDescription(getSelectedObject().getDescription());
                    }
                });
            }

            @Override
            public boolean canDelete() {
                return isDeleteEnabled(myProfiles.getSelectedProfile());
            }

            @Override
            public void delete() {
                final InspectionProfileImpl selectedProfile = myProfiles.getSelectedProfile();
                myProfiles.getModel().removeElement(selectedProfile);
                myDeletedProfiles.add(selectedProfile);
            }

            @Override
            public boolean canEditDescription() {
                return true;
            }

            @Override
            public void editDescription() {
                myAuxiliaryRightPanel.editDescription(getSelectedObject().getDescription());
            }

            @Override
            public boolean hasDescription() {
                return !StringUtil.isEmpty(getSelectedObject().getDescription());
            }

            @Override
            public void export() {
                final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                descriptor.setDescription("Choose directory to store profile file");
                FileChooser.chooseFile(descriptor, getProject(), myWholePanel, null, new Consumer<VirtualFile>() {
                    @Override
                    public void consume(VirtualFile file) {
                        final Element element = new Element("inspections");
                        try {
                            final SingleInspectionProfilePanel panel = getSelectedPanel();
                            LOG.assertTrue(panel != null);
                            final InspectionProfileImpl profile = getSelectedObject();
                            LOG.assertTrue(true);
                            profile.writeExternal(element);
                            final String filePath =
                                    FileUtil.toSystemDependentName(file.getPath()) + File.separator + FileUtil.sanitizeFileName(profile.getName()) + ".xml";
                            if (new File(filePath).isFile()) {
                                if (Messages
                                        .showOkCancelDialog(myWholePanel, "File \'" + filePath + "\' already exist. Do you want to overwrite it?", "Warning",
                                                Messages.getQuestionIcon()) != Messages.OK) {
                                    return;
                                }
                            }
                            JDOMUtil.writeDocument(new Document(element), filePath, SystemProperties.getLineSeparator());
                        }
                        catch (WriteExternalException e1) {
                            LOG.error(e1);
                        }
                        catch (IOException e1) {
                            LOG.error(e1);
                        }
                    }
                });
            }

            @Override
            public void doImport() {
                final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
                    @Override
                    public boolean isFileSelectable(VirtualFile file) {
                        return file.getFileType().equals(StdFileTypes.XML);
                    }
                };
                descriptor.setDescription("Choose profile file");
                FileChooser.chooseFile(descriptor, getProject(), myWholePanel, null, new Consumer<VirtualFile>() {
                    @Override
                    public void consume(VirtualFile file) {
                        if (file == null) return;
                        InspectionProfileImpl profile =
                                new InspectionProfileImpl("TempProfile", InspectionToolRegistrar.getInstance(), myProfileManager);
                        try {
                            Element rootElement = JDOMUtil.loadDocument(VfsUtilCore.virtualToIoFile(file)).getRootElement();
                            if (Comparing.strEqual(rootElement.getName(), "component")) {//import right from .idea/inspectProfiles/xxx.xml
                                rootElement = (Element)rootElement.getChildren().get(0);
                            }
                            final Set<String> levels = new HashSet<String>();
                            for (Object o : rootElement.getChildren("inspection_tool")) {
                                final Element inspectElement = (Element)o;
                                levels.add(inspectElement.getAttributeValue("level"));
                                for (Object s : inspectElement.getChildren("scope")) {
                                    levels.add(((Element)s).getAttributeValue("level"));
                                }
                            }
                            for (Iterator<String> iterator = levels.iterator(); iterator.hasNext(); ) {
                                String level = iterator.next();
                                if (myProfileManager.getOwnSeverityRegistrar().getSeverity(level) != null) {
                                    iterator.remove();
                                }
                            }
                            if (!levels.isEmpty()) {
                                if (Messages.showYesNoDialog(myWholePanel, "Undefined severities detected: " +
                                        StringUtil.join(levels, ", ") +
                                        ". Do you want to create them?", "Warning", Messages.getWarningIcon()) ==
                                        Messages.YES) {
                                    for (String level : levels) {
                                        final TextAttributes textAttributes = CodeInsightColors.WARNINGS_ATTRIBUTES.getDefaultAttributes();
                                        HighlightInfoType.HighlightInfoTypeImpl info =
                                                new HighlightInfoType.HighlightInfoTypeImpl(new HighlightSeverity(level, 50),
                                                        TextAttributesKey
                                                                .createTextAttributesKey(level));
                                        myProfileManager.getOwnSeverityRegistrar()
                                                .registerSeverity(new SeverityRegistrar.SeverityBasedTextAttributes(textAttributes.clone(), info),
                                                        textAttributes.getErrorStripeColor());
                                    }
                                }
                            }
                            profile.readExternal(rootElement);
                            profile.setProjectLevel(false);
                            profile.initInspectionTools(getProject());
                            if (getProfilePanel(profile) != null) {
                                if (Messages.showOkCancelDialog(myWholePanel, "Profile with name \'" +
                                                profile.getName() +
                                                "\' already exists. Do you want to overwrite it?", "Warning",
                                        Messages.getInformationIcon()) != Messages.OK) {
                                    return;
                                }
                            }
                            final ModifiableModel model = profile.getModifiableModel();
                            model.setModified(true);
                            addProfile((InspectionProfileImpl)model, profile);

                            //TODO myDeletedProfiles ? really need this
                            myDeletedProfiles.remove(profile);
                        }
                        catch (InvalidDataException e1) {
                            LOG.error(e1);
                        }
                        catch (JDOMException e1) {
                            LOG.error(e1);
                        }
                        catch (IOException e1) {
                            LOG.error(e1);
                        }
                    }
                });
            }
        }).build();

        myAuxiliaryRightPanel = new AuxiliaryRightPanel(new AuxiliaryRightPanel.DescriptionSaveListener() {
            @Override
            public void saveDescription( String description) {
                final InspectionProfileImpl inspectionProfile = getSelectedObject();
                if (!Comparing.strEqual(description, inspectionProfile.getDescription())) {
                    inspectionProfile.setDescription(description);
                    inspectionProfile.setModified(true);
                }
                myAuxiliaryRightPanel.showDescription(description);
            }

            @Override
            public void cancel() {
                myAuxiliaryRightPanel.showDescription(getSelectedObject().getDescription());
            }
        });

        toolbar.setLayout(new GridBagLayout());
        final JLabel headerTitleLabel = new JLabel(HEADER_TITLE);
        headerTitleLabel.setBorder(IdeBorderFactory.createEmptyBorder(10, 0, 0, 0));
        toolbar.add(headerTitleLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        toolbar.add(myProfiles, new GridBagConstraints(1, 0, 1, 1, 0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 6, 0, 0), 0, 0));

        toolbar.add(withBorderOnTop(manageButton), new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0, 10, 0, 0), 0, 0));

        toolbar.add(myAuxiliaryRightPanel, new GridBagConstraints(3, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 15, 0, 0), 0, 0));

        myProjectProfileManager = projectProfileManager;
        myProfileManager = profileManager;
    }

    private static JComponent withBorderOnTop(final JComponent component) {
        final JPanel panel = new JPanel();
        panel.add(component);
        panel.setBorder(IdeBorderFactory.createEmptyBorder(UIUtil.isUnderDarcula() ? 10 : 13, 0, 0, 0));
        return panel;
    }

    private Project getProject() {
        return myProjectProfileManager.getProject();
    }


    private InspectionProfileImpl copyToNewProfile(ModifiableModel selectedProfile,  Project project) {
        String profileDefaultName = selectedProfile.getName();
        do {
            profileDefaultName += " (copy)";
        }
        while (hasName(profileDefaultName, myPanels.get(selectedProfile).isProfileShared()));

        final ProfileManager profileManager = selectedProfile.getProfileManager();
        InspectionProfileImpl inspectionProfile =
                new InspectionProfileImpl(profileDefaultName, InspectionToolRegistrar.getInstance(), profileManager);

        inspectionProfile.copyFrom(selectedProfile);
        inspectionProfile.setName(profileDefaultName);
        inspectionProfile.initInspectionTools(project);
        inspectionProfile.setModified(true);
        return inspectionProfile;
    }

    private void addProfile(InspectionProfileImpl model, InspectionProfileImpl profile) {
        final String modelName = model.getName();
        final SingleInspectionProfilePanel panel = createPanel(model, profile, modelName);
        myPanel.add(getCardName(model), panel);

        myProfiles.getModel().addElement(model);
        putProfile(model, panel);
        myProfiles.selectProfile(model);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getHelpTopic() {
        return "preferences.inspections";
    }

    @Override
    
    public String getId() {
        return ID;
    }

    @Override
    public Runnable enableSearch(final String option) {
        return new Runnable() {
            @Override
            public void run() {
                SingleInspectionProfilePanel panel = getSelectedPanel();
                if (panel != null) {
                    panel.setFilter(option);
                }
            }
        };
    }

    @Override
    public JComponent createComponent() {
        myPanel.setLayout(myLayout);
        return myWholePanel;
    }

    protected abstract InspectionProfileImpl getCurrentProfile();

    @Override
    public boolean isModified() {
        final InspectionProfileImpl selectedProfile = getSelectedObject();
        final InspectionProfileImpl currentProfile = getCurrentProfile();
        if (!Comparing.equal(selectedProfile, currentProfile)) {
            return true;
        }
        for (SingleInspectionProfilePanel panel : myPanels.values()) {
            if (panel.isModified()) return true;
        }
        if (getProfiles().size() != myPanels.size()) return true;
        return !myDeletedProfiles.isEmpty();
    }

    @Override
    public void apply() throws ConfigurationException {
        final SingleInspectionProfilePanel selectedPanel = getSelectedPanel();
        for (final Profile inspectionProfile : myPanels.keySet()) {
            if (myDeletedProfiles.remove(inspectionProfile)) {
                deleteProfile(getProfilePanel(inspectionProfile).getSelectedProfile());
            }
            else {
                final SingleInspectionProfilePanel panel = getProfilePanel(inspectionProfile);
                panel.apply();
                if (panel == selectedPanel) {
                    applyRootProfile(panel.getCurrentProfileName(), panel.isProfileShared());
                }
            }
        }
        doReset();
    }

    protected abstract void applyRootProfile(final String name, final boolean isShared);

    private SingleInspectionProfilePanel getProfilePanel(Profile inspectionProfile) {
        return myPanels.get(inspectionProfile);
    }

    private void putProfile(Profile profile, SingleInspectionProfilePanel panel) {
        myPanels.put(profile, panel);
    }

    protected void deleteProfile(Profile profile) {
        final String name = profile.getName();
        if (profile.getProfileManager() == myProfileManager) {
            if (myProfileManager.getProfile(name, false) != null) {
                myProfileManager.deleteProfile(name);
            }
            return;
        }
        if (profile.getProfileManager() == myProjectProfileManager) {
            if (myProjectProfileManager.getProfile(name, false) != null) {
                myProjectProfileManager.deleteProfile(name);
            }
        }
    }

    protected boolean acceptTool(InspectionToolWrapper entry) {
        return true;
    }

    @Override
    public void reset() {
        doReset();
    }

    private void doReset() {
        myDeletedProfiles.clear();
        myPanels.clear();
        final Collection<Profile> profiles = getProfiles();
        final List<Profile> modifiableProfiles = new ArrayList<Profile>(profiles.size());
        for (Profile profile : profiles) {
            final String profileName = profile.getName();
            final ModifiableModel modifiableProfile = ((InspectionProfileImpl)profile).getModifiableModel();
            modifiableProfiles.add(modifiableProfile);
            final InspectionProfileImpl inspectionProfile = (InspectionProfileImpl)modifiableProfile;
            final SingleInspectionProfilePanel panel = createPanel(inspectionProfile, profile, profileName);
            putProfile(modifiableProfile, panel);
            myPanel.add(getCardName(inspectionProfile), panel);
        }
        myProfiles.reset(modifiableProfiles);
        myAuxiliaryRightPanel.showDescription(getSelectedObject().getDescription());

        final InspectionProfileImpl inspectionProfile = getCurrentProfile();
        myProfiles.selectProfile(inspectionProfile);
        myLayout.show(myPanel, getCardName(inspectionProfile));
        final SingleInspectionProfilePanel panel = getSelectedPanel();
        if (panel != null) {
            panel.setVisible(true);//make sure that UI was initialized
            mySelectionAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (mySelectionAlarm != null) {
                        mySelectionAlarm.addRequest(new Runnable() {
                            @Override
                            public void run() {
                                panel.updateSelection();
                            }
                        }, 200);
                    }
                }
            });
        }
    }

    private static String getCardName(final InspectionProfileImpl inspectionProfile) {
        return (inspectionProfile.isProjectLevel() ? "s" : "a") + inspectionProfile.getName();
    }

    private SingleInspectionProfilePanel createPanel(InspectionProfileImpl profile, Profile original, String profileName) {
        return new SingleInspectionProfilePanel(myProjectProfileManager, profileName, profile, original) {
            @Override
            protected boolean accept(InspectionToolWrapper entry) {
                return super.accept(entry) && acceptTool(entry);
            }
        };
    }

    private boolean isDeleteEnabled( InspectionProfileImpl inspectionProfile) {
        final ProfileManager profileManager = inspectionProfile.getProfileManager();

        boolean projectProfileFound = false;
        boolean ideProfileFound = false;

        final ComboBoxModel model = myProfiles.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Profile profile = (Profile)model.getElementAt(i);
            if (inspectionProfile == profile) continue;
            final boolean isProjectProfile = profile.getProfileManager() == myProjectProfileManager;
            projectProfileFound |= isProjectProfile;
            ideProfileFound |= !isProjectProfile;

            if (ideProfileFound && projectProfileFound) break;
        }

        return profileManager == myProjectProfileManager ? projectProfileFound : ideProfileFound;
    }

    protected Collection<Profile> getProfiles() {
        final Collection<Profile> result = new ArrayList<Profile>();
        result.addAll(new TreeSet<Profile>(myProfileManager.getProfiles()));
        result.addAll(myProjectProfileManager.getProfiles());
        return result;
    }

    @Override
    public void disposeUIResources() {
        for (SingleInspectionProfilePanel panel : myPanels.values()) {
            panel.disposeUI();
        }
        myPanels.clear();
        if (mySelectionAlarm != null) {
            Disposer.dispose(mySelectionAlarm);
            mySelectionAlarm = null;
        }
    }

    @Override
    public void selectProfile(Profile profile) {
        myProfiles.selectProfile(profile);
    }

    @Override
    public void selectInspectionTool(String selectedToolShortName) {
        final InspectionProfileImpl inspectionProfile = getSelectedObject();
        final SingleInspectionProfilePanel panel = getProfilePanel(inspectionProfile);
        LOG.assertTrue(panel != null, "No settings panel for: " + inspectionProfile + "; " + configuredProfiles());
        panel.selectInspectionTool(selectedToolShortName);
    }

    protected SingleInspectionProfilePanel getSelectedPanel() {
        final InspectionProfileImpl inspectionProfile = getSelectedObject();
        return getProfilePanel(inspectionProfile);
    }

    private String configuredProfiles() {
        return "configured profiles: " + StringUtil.join(myPanels.keySet(), ", ");
    }

    private boolean hasName(final  String name, boolean shared) {
        for (SingleInspectionProfilePanel p : myPanels.values()) {
            if (name.equals(p.getCurrentProfileName()) && shared == p.isProfileShared()) {
                return true;
            }
        }
        return false;
    }

    
    @Override
    public InspectionProfileImpl getSelectedObject() {
        return myProfiles.getSelectedProfile();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        final InspectionProfileImpl inspectionProfile = getSelectedObject();
        return getProfilePanel(inspectionProfile).getPreferredFocusedComponent();
    }
}
