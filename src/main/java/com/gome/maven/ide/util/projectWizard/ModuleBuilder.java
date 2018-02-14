/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.ide.util.projectWizard;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.highlighter.ModuleFileType;
import com.gome.maven.ide.util.frameworkSupport.FrameworkRole;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPointName;
import com.gome.maven.openapi.module.*;
import com.gome.maven.openapi.options.ConfigurationException;
import com.gome.maven.openapi.project.DumbAwareRunnable;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectType;
import com.gome.maven.openapi.project.ProjectTypeService;
import com.gome.maven.openapi.projectRoots.Sdk;
import com.gome.maven.openapi.roots.ContentEntry;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.roots.ui.configuration.ModulesProvider;
import com.gome.maven.openapi.startup.StartupManager;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.ThrowableComputable;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.EventDispatcher;
import com.gome.maven.util.containers.ContainerUtil;
import org.jdom.JDOMException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class ModuleBuilder extends AbstractModuleBuilder {

    public static final ExtensionPointName<ModuleBuilderFactory> EP_NAME = ExtensionPointName.create("com.intellij.moduleBuilder");

    private static final Logger LOG = Logger.getInstance("#com.intellij.ide.util.projectWizard.ModuleBuilder");
    private final Set<ModuleConfigurationUpdater> myUpdaters = new HashSet<ModuleConfigurationUpdater>();
    private final EventDispatcher<ModuleBuilderListener> myDispatcher = EventDispatcher.create(ModuleBuilderListener.class);
    protected Sdk myJdk;
    private String myName;
     private String myModuleFilePath;
    private String myContentEntryPath;

    
    public static List<ModuleBuilder> getAllBuilders() {
        final ArrayList<ModuleBuilder> result = new ArrayList<ModuleBuilder>();
        for (final ModuleType moduleType : ModuleTypeManager.getInstance().getRegisteredTypes()) {
            result.add(moduleType.createModuleBuilder());
        }
        for (ModuleBuilderFactory factory : EP_NAME.getExtensions()) {
            result.add(factory.createBuilder());
        }
        return ContainerUtil.filter(result, new Condition<ModuleBuilder>() {

            @Override
            public boolean value(ModuleBuilder moduleBuilder) {
                return moduleBuilder.isAvailable();
            }
        });
    }

    public static void deleteModuleFile(String moduleFilePath) {
        final File moduleFile = new File(moduleFilePath);
        if (moduleFile.exists()) {
            FileUtil.delete(moduleFile);
        }
        final VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(moduleFile);
        if (file != null) {
            file.refresh(false, false);
        }
    }

    protected boolean isAvailable() {
        return true;
    }

    
    protected final String acceptParameter(String param) {
        return param != null && param.length() > 0 ? param : null;
    }

    public String getName() {
        return myName;
    }

    @Override
    public void setName(String name) {
        myName = acceptParameter(name);
    }

    @Override
    
    public String getBuilderId() {
        ModuleType moduleType = getModuleType();
        return moduleType == null ? null : moduleType.getId();
    }

    @Override
    public ModuleWizardStep[] createWizardSteps( WizardContext wizardContext,  ModulesProvider modulesProvider) {
        ModuleType moduleType = getModuleType();
        return moduleType == null ? ModuleWizardStep.EMPTY_ARRAY : moduleType.createWizardSteps(wizardContext, this, modulesProvider);
    }

    /**
     * Typically delegates to ModuleType (e.g. JavaModuleType) that is more generic than ModuleBuilder
     *
     * @param settingsStep step to be modified
     * @return callback ({@link com.gome.maven.ide.util.projectWizard.ModuleWizardStep#validate()}
     *         and {@link com.gome.maven.ide.util.projectWizard.ModuleWizardStep#updateDataModel()}
     *         will be invoked)
     */
    @Override
    
    public ModuleWizardStep modifySettingsStep( SettingsStep settingsStep) {
        return modifyStep(settingsStep);
    }

    public ModuleWizardStep modifyStep(SettingsStep settingsStep) {
        ModuleType type = getModuleType();
        if (type == null) {
            return null;
        }
        else {
            final ModuleWizardStep step = type.modifySettingsStep(settingsStep, this);
            final List<WizardInputField> fields = getAdditionalFields();
            for (WizardInputField field : fields) {
                field.addToSettings(settingsStep);
            }
            return new ModuleWizardStep() {
                @Override
                public JComponent getComponent() {
                    return null;
                }

                @Override
                public void updateDataModel() {
                    if (step != null) {
                        step.updateDataModel();
                    }
                }

                @Override
                public boolean validate() throws ConfigurationException {
                    for (WizardInputField field : fields) {
                        if (!field.validate()) {
                            return false;
                        }
                    }
                    return step == null || step.validate();
                }
            };
        }
    }

    public ModuleWizardStep modifyProjectTypeStep( SettingsStep settingsStep) {
        ModuleType type = getModuleType();
        return type == null ? null : type.modifyProjectTypeStep(settingsStep, this);
    }

    protected List<WizardInputField> getAdditionalFields() {
        return Collections.emptyList();
    }

    public String getModuleFilePath() {
        return myModuleFilePath;
    }

    @Override
    public void setModuleFilePath( String path) {
        myModuleFilePath = acceptParameter(path);
    }

    public void addModuleConfigurationUpdater(ModuleConfigurationUpdater updater) {
        myUpdaters.add(updater);
    }

    
    public String getContentEntryPath() {
        if (myContentEntryPath == null) {
            final String directory = getModuleFileDirectory();
            if (directory == null) {
                return null;
            }
            new File(directory).mkdirs();
            return directory;
        }
        return myContentEntryPath;
    }

    @Override
    public void setContentEntryPath(String moduleRootPath) {
        final String path = acceptParameter(moduleRootPath);
        if (path != null) {
            try {
                myContentEntryPath = FileUtil.resolveShortWindowsName(path);
            }
            catch (IOException e) {
                myContentEntryPath = path;
            }
        }
        else {
            myContentEntryPath = null;
        }
        if (myContentEntryPath != null) {
            myContentEntryPath = myContentEntryPath.replace(File.separatorChar, '/');
        }
    }

    protected  ContentEntry doAddContentEntry(ModifiableRootModel modifiableRootModel) {
        final String contentEntryPath = getContentEntryPath();
        if (contentEntryPath == null) return null;
        new File(contentEntryPath).mkdirs();
        final VirtualFile moduleContentRoot = LocalFileSystem.getInstance().refreshAndFindFileByPath(contentEntryPath.replace('\\', '/'));
        if (moduleContentRoot == null) return null;
        return modifiableRootModel.addContentEntry(moduleContentRoot);
    }

    
    public String getModuleFileDirectory() {
        if (myModuleFilePath == null) {
            return null;
        }
        final String parent = new File(myModuleFilePath).getParent();
        if (parent == null) {
            return null;
        }
        return parent.replace(File.separatorChar, '/');
    }

    
    public Module createModule( ModifiableModuleModel moduleModel)
            throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
        LOG.assertTrue(myName != null);
        LOG.assertTrue(myModuleFilePath != null);

        deleteModuleFile(myModuleFilePath);
        final ModuleType moduleType = getModuleType();
        final Module module = moduleModel.newModule(myModuleFilePath, moduleType.getId());
        setupModule(module);

        return module;
    }

    protected void setupModule(Module module) throws ConfigurationException {
        final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
        setupRootModel(modifiableModel);
        for (ModuleConfigurationUpdater updater : myUpdaters) {
            updater.update(module, modifiableModel);
        }
        modifiableModel.commit();
        setProjectType(module);
    }

    private void onModuleInitialized(final Module module) {
        myDispatcher.getMulticaster().moduleCreated(module);
    }

    public abstract void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException;

    public abstract ModuleType getModuleType();

    protected ProjectType getProjectType() {
        return null;
    }

    protected void setProjectType(Module module) {
        ProjectType projectType = getProjectType();
        if (projectType != null && ProjectTypeService.getProjectType(module.getProject()) == null) {
            ProjectTypeService.setProjectType(module.getProject(), projectType);
        }
    }

    
    public Module createAndCommitIfNeeded( Project project,  ModifiableModuleModel model, boolean runFromProjectWizard)
            throws InvalidDataException, ConfigurationException, IOException, JDOMException, ModuleWithNameAlreadyExists {
        final ModifiableModuleModel moduleModel = model != null ? model : ModuleManager.getInstance(project).getModifiableModel();
        final Module module = createModule(moduleModel);
        if (model == null) moduleModel.commit();

        if (runFromProjectWizard) {
            StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(new DumbAwareRunnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {
                            onModuleInitialized(module);
                        }
                    });
                }
            });
        }
        else {
            onModuleInitialized(module);
        }
        return module;
    }

    public void addListener(ModuleBuilderListener listener) {
        myDispatcher.addListener(listener);
    }

    public void removeListener(ModuleBuilderListener listener) {
        myDispatcher.removeListener(listener);
    }

    public boolean canCreateModule() {
        return true;
    }

    @Override
    
    public List<Module> commit( final Project project, final ModifiableModuleModel model, final ModulesProvider modulesProvider) {
        final Module module = commitModule(project, model);
        return module != null ? Collections.singletonList(module) : null;
    }

    
    public Module commitModule( final Project project,  final ModifiableModuleModel model) {
        if (canCreateModule()) {
            if (myName == null) {
                myName = project.getName();
            }
            if (myModuleFilePath == null) {
                myModuleFilePath = project.getBaseDir().getPath() + File.separator + myName + ModuleFileType.DOT_DEFAULT_EXTENSION;
            }
            try {
                return ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<Module, Exception>() {
                    @Override
                    public Module compute() throws Exception {
                        return createAndCommitIfNeeded(project, model, true);
                    }
                });
            }
            catch (Exception ex) {
                LOG.warn(ex);
                Messages.showErrorDialog(IdeBundle.message("error.adding.module.to.project", ex.getMessage()), IdeBundle.message("title.add.module"));
            }
        }
        return null;
    }

    public Icon getBigIcon() {
        return getModuleType().getBigIcon();
    }

    public Icon getNodeIcon() {
        return getModuleType().getNodeIcon(false);
    }

    public String getDescription() {
        return getModuleType().getDescription();
    }

    public String getPresentableName() {
        return getModuleTypeName();
    }

    protected String getModuleTypeName() {
        String name = getModuleType().getName();
        return StringUtil.trimEnd(name, " Module");
    }

    public String getGroupName() {
        return getPresentableName().split(" ")[0];
    }

    public String getParentGroup() {
        return null;
    }

    public int getWeight() { return 0; }

    public boolean isTemplate() {
        return false;
    }

    public boolean isTemplateBased() {
        return false;
    }

    public void updateFrom(ModuleBuilder from) {
        myName = from.getName();
        myContentEntryPath = from.getContentEntryPath();
        myModuleFilePath = from.getModuleFilePath();
    }

    public Sdk getModuleJdk() {
        return myJdk;
    }

    public void setModuleJdk(Sdk jdk) {
        myJdk = jdk;
    }

    
    public FrameworkRole getDefaultAcceptableRole() {
        return getModuleType().getDefaultAcceptableRole();
    }

    public static abstract class ModuleConfigurationUpdater {

        public abstract void update( Module module,  ModifiableRootModel rootModel);

    }
}
