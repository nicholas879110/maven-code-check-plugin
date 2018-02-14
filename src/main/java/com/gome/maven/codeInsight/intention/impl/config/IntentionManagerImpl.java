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

package com.gome.maven.codeInsight.intention.impl.config;

import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInsight.intention.IntentionActionBean;
import com.gome.maven.codeInsight.intention.IntentionManager;
import com.gome.maven.codeInspection.GlobalInspectionTool;
import com.gome.maven.codeInspection.GlobalSimpleInspectionTool;
import com.gome.maven.codeInspection.LocalQuickFix;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.actions.CleanupInspectionIntention;
import com.gome.maven.codeInspection.actions.RunInspectionIntention;
import com.gome.maven.codeInspection.ex.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.extensions.ExtensionPoint;
import com.gome.maven.openapi.extensions.ExtensionPointListener;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.extensions.PluginDescriptor;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.IncorrectOperationException;
import com.gome.maven.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dsl
 */
public class IntentionManagerImpl extends IntentionManager {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.intention.impl.config.IntentionManagerImpl");

    private final List<IntentionAction> myActions = ContainerUtil.createLockFreeCopyOnWriteList();
    private final IntentionManagerSettings mySettings;

    private final Alarm myInitActionsAlarm = new Alarm(Alarm.ThreadToUse.SHARED_THREAD);

    public IntentionManagerImpl(IntentionManagerSettings intentionManagerSettings) {
        mySettings = intentionManagerSettings;

        addAction(new EditInspectionToolsSettingsInSuppressedPlaceIntention());

        final ExtensionPoint<IntentionActionBean> point = Extensions.getArea(null).getExtensionPoint(EP_INTENTION_ACTIONS);

        point.addExtensionPointListener(new ExtensionPointListener<IntentionActionBean>() {
            @Override
            public void extensionAdded( final IntentionActionBean extension,  final PluginDescriptor pluginDescriptor) {
                registerIntentionFromBean(extension);
            }

            @Override
            public void extensionRemoved( final IntentionActionBean extension,  final PluginDescriptor pluginDescriptor) {
            }
        });
    }

    private void registerIntentionFromBean( final IntentionActionBean extension) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final String descriptionDirectoryName = extension.getDescriptionDirectoryName();
                final String[] categories = extension.getCategories();
                final IntentionAction instance = createIntentionActionWrapper(extension, categories);
                if (categories == null) {
                    addAction(instance);
                }
                else {
                    if (descriptionDirectoryName != null) {
                        addAction(instance);
                        mySettings.registerIntentionMetaData(instance, categories, descriptionDirectoryName, extension.getMetadataClassLoader());
                    }
                    else {
                        registerIntentionAndMetaData(instance, categories);
                    }
                }
            }
        };
        //todo temporary hack, need smarter logic:
        // * on the first request, wait until all the initialization is finished
        // * ensure this request doesn't come on EDT
        // * while waiting, check for ProcessCanceledException
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            runnable.run();
        }
        else {
            myInitActionsAlarm.addRequest(runnable, 300);
        }
    }

    private static IntentionAction createIntentionActionWrapper( IntentionActionBean intentionActionBean, String[] categories) {
        return new IntentionActionWrapper(intentionActionBean, categories);
    }

    @Override
    public void registerIntentionAndMetaData( IntentionAction action,  String... category) {
        registerIntentionAndMetaData(action, category, getDescriptionDirectoryName(action));
    }

    
    private static String getDescriptionDirectoryName(final IntentionAction action) {
        if (action instanceof IntentionActionWrapper) {
            final IntentionActionWrapper wrapper = (IntentionActionWrapper)action;
            return getDescriptionDirectoryName(wrapper.getImplementationClassName());
        }
        else {
            return getDescriptionDirectoryName(action.getClass().getName());
        }
    }

    private static String getDescriptionDirectoryName(final String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1).replaceAll("\\$", "");
    }

    @Override
    public void registerIntentionAndMetaData( IntentionAction action,
                                              String[] category,
                                               String descriptionDirectoryName) {
        addAction(action);
        mySettings.registerIntentionMetaData(action, category, descriptionDirectoryName);
    }

    @Override
    public void registerIntentionAndMetaData( final IntentionAction action,
                                              final String[] category,
                                              final String description,
                                              final String exampleFileExtension,
                                              final String[] exampleTextBefore,
                                              final String[] exampleTextAfter) {
        addAction(action);

        IntentionActionMetaData metaData = new IntentionActionMetaData(action, category,
                new PlainTextDescriptor(description, "description.html"),
                mapToDescriptors(exampleTextBefore, "before." + exampleFileExtension),
                mapToDescriptors(exampleTextAfter, "after." + exampleFileExtension));
        mySettings.registerMetaData(metaData);
    }

    @Override
    public void unregisterIntention( IntentionAction intentionAction) {
        myActions.remove(intentionAction);
        mySettings.unregisterMetaData(intentionAction);
    }

    private static TextDescriptor[] mapToDescriptors(String[] texts,  String fileName) {
        TextDescriptor[] result = new TextDescriptor[texts.length];
        for (int i = 0; i < texts.length; i++) {
            result[i] = new PlainTextDescriptor(texts[i], fileName);
        }
        return result;
    }

    @Override
    
    public List<IntentionAction> getStandardIntentionOptions( final HighlightDisplayKey displayKey,
                                                              final PsiElement context) {
        List<IntentionAction> options = new ArrayList<IntentionAction>(9);
        options.add(new EditInspectionToolsSettingsAction(displayKey));
        options.add(new RunInspectionIntention(displayKey));
        options.add(new DisableInspectionToolAction(displayKey));
        return options;
    }

    
    @Override
    public IntentionAction createFixAllIntention(InspectionToolWrapper toolWrapper, IntentionAction action) {
        if (toolWrapper instanceof LocalInspectionToolWrapper) {
            Class aClass = action.getClass();
            if (action instanceof QuickFixWrapper) {
                aClass = ((QuickFixWrapper)action).getFix().getClass();
            }
            return new CleanupInspectionIntention(toolWrapper, aClass, action.getText());
        }
        else if (toolWrapper instanceof GlobalInspectionToolWrapper) {
            GlobalInspectionTool wrappedTool = ((GlobalInspectionToolWrapper)toolWrapper).getTool();
            if (wrappedTool instanceof GlobalSimpleInspectionTool && (action instanceof LocalQuickFix || action instanceof QuickFixWrapper)) {
                Class aClass = action.getClass();
                if (action instanceof QuickFixWrapper) {
                    aClass = ((QuickFixWrapper)action).getFix().getClass();
                }
                return new CleanupInspectionIntention(toolWrapper, aClass, action.getText());
            }
        }
        else {
            throw new AssertionError("unknown tool: " + toolWrapper);
        }
        return null;
    }

    @Override
    
    public LocalQuickFix convertToFix( final IntentionAction action) {
        if (action instanceof LocalQuickFix) {
            return (LocalQuickFix)action;
        }
        return new LocalQuickFix() {
            @Override
            
            public String getName() {
                return action.getText();
            }

            @Override
            
            public String getFamilyName() {
                return action.getFamilyName();
            }

            @Override
            public void applyFix( final Project project,  final ProblemDescriptor descriptor) {
                final PsiFile psiFile = descriptor.getPsiElement().getContainingFile();
                try {
                    action.invoke(project, new LazyEditor(psiFile), psiFile);
                }
                catch (IncorrectOperationException e) {
                    LOG.error(e);
                }
            }
        };
    }

    @Override
    public void addAction( IntentionAction action) {
        myActions.add(action);
    }

    @Override
    
    public IntentionAction[] getIntentionActions() {
        return ArrayUtil.stripTrailingNulls(myActions.toArray(new IntentionAction[myActions.size()]));
    }

    
    @Override
    public IntentionAction[] getAvailableIntentionActions() {
        List<IntentionAction> list = new ArrayList<IntentionAction>(myActions.size());
        for (IntentionAction action : myActions) {
            if (mySettings.isEnabled(action)) {
                list.add(action);
            }
        }
        return list.toArray(new IntentionAction[list.size()]);
    }

    public boolean hasActiveRequests() {
        return !myInitActionsAlarm.isEmpty();
    }
}
