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

package com.gome.maven.openapi.roots.ui.configuration.actions;

import com.gome.maven.ide.DeleteProvider;
import com.gome.maven.ide.TitledHandler;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.actionSystem.LangDataKeys;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.module.ModifiableModuleModel;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.roots.impl.ModifiableModelCommitter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.project.ProjectBundle;
import com.gome.maven.openapi.roots.ModifiableRootModel;
import com.gome.maven.openapi.roots.ModuleOrderEntry;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.roots.OrderEntry;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.projectImport.ProjectAttachProcessor;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.Function;
import com.gome.maven.util.PlatformUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModuleDeleteProvider  implements DeleteProvider, TitledHandler  {
    @Override
    public boolean canDeleteElement( DataContext dataContext) {
        final Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
        return modules != null && !isPrimaryModule(modules);
    }

    private static boolean isPrimaryModule(Module[] modules) {
        if (!ProjectAttachProcessor.canAttachToProject()) {
            return !PlatformUtils.isIntelliJ();
        }
        for (Module module : modules) {
            final File moduleFile = new File(module.getModuleFilePath());
            final File projectFile = new File(module.getProject().getProjectFilePath());
            if (moduleFile.getParent().equals(projectFile.getParent()) &&
                    moduleFile.getParentFile().getName().equals(Project.DIRECTORY_STORE_FOLDER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void deleteElement( DataContext dataContext) {
        final Module[] modules = LangDataKeys.MODULE_CONTEXT_ARRAY.getData(dataContext);
        assert modules != null;
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        assert project != null;
        String names = StringUtil.join(Arrays.asList(modules), new Function<Module, String>() {
            @Override
            public String fun(final Module module) {
                return "\'" + module.getName() + "\'";
            }
        }, ", ");
        int ret = Messages.showOkCancelDialog(getConfirmationText(modules, names), getActionTitle(), Messages.getQuestionIcon());
        if (ret != Messages.OK) return;
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                final Runnable action = new Runnable() {
                    @Override
                    public void run() {
                        final ModuleManager moduleManager = ModuleManager.getInstance(project);
                        final Module[] currentModules = moduleManager.getModules();
                        final ModifiableModuleModel modifiableModuleModel = moduleManager.getModifiableModel();
                        final Map<Module, ModifiableRootModel> otherModuleRootModels = new HashMap<Module, ModifiableRootModel>();
                        for (final Module module : modules) {
                            final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                            for (final Module otherModule : currentModules) {
                                if (otherModule == module || ArrayUtilRt.find(modules, otherModule) != -1) continue;
                                if (!otherModuleRootModels.containsKey(otherModule)) {
                                    otherModuleRootModels.put(otherModule, ModuleRootManager.getInstance(otherModule).getModifiableModel());
                                }
                            }
                            removeModule(module, modifiableModel, otherModuleRootModels.values(), modifiableModuleModel);
                        }
                        final ModifiableRootModel[] modifiableRootModels = otherModuleRootModels.values().toArray(new ModifiableRootModel[otherModuleRootModels.size()]);
                        ModifiableModelCommitter.multiCommit(modifiableRootModels, modifiableModuleModel);
                    }
                };
                ApplicationManager.getApplication().runWriteAction(action);
            }
        }, ProjectBundle.message("module.remove.command"), null);
    }

    private static String getConfirmationText(Module[] modules, String names) {
        if (ProjectAttachProcessor.canAttachToProject()) {
            return ProjectBundle.message("project.remove.confirmation.prompt", names, modules.length);
        }
        return ProjectBundle.message("module.remove.confirmation.prompt", names, modules.length);
    }

    @Override
    public String getActionTitle() {
        return ProjectAttachProcessor.canAttachToProject() ? "Remove from Project View" : "Remove Module";
    }

    public static void removeModule( final Module moduleToRemove,
                                     ModifiableRootModel modifiableRootModelToRemove,
                                     Collection<ModifiableRootModel> otherModuleRootModels,
                                     final ModifiableModuleModel moduleModel) {
        // remove all dependencies on the module that is about to be removed
        for (final ModifiableRootModel modifiableRootModel : otherModuleRootModels) {
            final OrderEntry[] orderEntries = modifiableRootModel.getOrderEntries();
            for (final OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof ModuleOrderEntry && orderEntry.isValid()) {
                    final Module orderEntryModule = ((ModuleOrderEntry)orderEntry).getModule();
                    if (orderEntryModule != null && orderEntryModule.equals(moduleToRemove)) {
                        modifiableRootModel.removeOrderEntry(orderEntry);
                    }
                }
            }
        }
        // destroyProcess editor
        if (modifiableRootModelToRemove != null) {
            modifiableRootModelToRemove.dispose();
        }
        // destroyProcess module
        moduleModel.disposeModule(moduleToRemove);
    }
}
