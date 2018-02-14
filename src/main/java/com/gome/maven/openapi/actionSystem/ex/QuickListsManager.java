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
package com.gome.maven.openapi.actionSystem.ex;

import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.actions.QuickSwitchSchemeAction;
import com.gome.maven.openapi.actionSystem.*;
import com.gome.maven.openapi.actionSystem.impl.BundledQuickListsProvider;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.components.ExportableApplicationComponent;
import com.gome.maven.openapi.components.RoamingType;
import com.gome.maven.openapi.components.StoragePathMacros;
import com.gome.maven.openapi.options.BaseSchemeProcessor;
import com.gome.maven.openapi.options.SchemesManager;
import com.gome.maven.openapi.options.SchemesManagerFactory;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.JDOMUtil;
import com.gome.maven.util.PathUtilRt;
import com.gome.maven.util.ThrowableConvertor;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public class QuickListsManager implements ExportableApplicationComponent {
    static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + "/quicklists";

    private static final String LIST_TAG = "list";

    private final ActionManager myActionManager;
    private final SchemesManager<QuickList, QuickList> mySchemesManager;

    public QuickListsManager( ActionManager actionManager,  SchemesManagerFactory schemesManagerFactory) {
        myActionManager = actionManager;
        mySchemesManager = schemesManagerFactory.createSchemesManager(FILE_SPEC,
                new BaseSchemeProcessor<QuickList>() {
                    
                    @Override
                    public QuickList readScheme( Element element) {
                        return createItem(element);
                    }

                    @Override
                    public Element writeScheme( QuickList scheme) {
                        Element element = new Element(LIST_TAG);
                        scheme.writeExternal(element);
                        return element;
                    }
                },
                RoamingType.PER_USER);
    }

    public static QuickListsManager getInstance() {
        return ApplicationManager.getApplication().getComponent(QuickListsManager.class);
    }

    @Override
    
    public File[] getExportFiles() {
        return new File[]{mySchemesManager.getRootDirectory()};
    }

    
    @Override
    public String getPresentableName() {
        return IdeBundle.message("quick.lists.presentable.name");
    }

    
    private static QuickList createItem( Element element) {
        QuickList item = new QuickList();
        item.readExternal(element);
        return item;
    }

    @Override
    
    public String getComponentName() {
        return "QuickListsManager";
    }

    @Override
    public void initComponent() {
        for (BundledQuickListsProvider provider : BundledQuickListsProvider.EP_NAME.getExtensions()) {
            for (final String path : provider.getBundledListsRelativePaths()) {
                mySchemesManager.loadBundledScheme(path, provider, new ThrowableConvertor<Element, QuickList, Throwable>() {
                    @Override
                    public QuickList convert(Element element) throws Throwable {
                        QuickList item = createItem(element);
                        item.getExternalInfo().setHash(JDOMUtil.getTreeHash(element, true));
                        item.getExternalInfo().setPreviouslySavedName(item.getName());
                        item.getExternalInfo().setCurrentFileName(PathUtilRt.getFileName(path));
                        return item;
                    }
                });
            }
        }
        mySchemesManager.loadSchemes();
        registerActions();
    }

    @Override
    public void disposeComponent() {
    }

    
    public QuickList[] getAllQuickLists() {
        Collection<QuickList> lists = mySchemesManager.getAllSchemes();
        return lists.toArray(new QuickList[lists.size()]);
    }

    private void registerActions() {
        // to prevent exception if 2 or more targets have the same name
        Set<String> registeredIds = new THashSet<String>();
        for (QuickList list : mySchemesManager.getAllSchemes()) {
            String actionId = list.getActionId();
            if (registeredIds.add(actionId)) {
                myActionManager.registerAction(actionId, new InvokeQuickListAction(list));
            }
        }
    }

    private void unregisterActions() {
        for (String oldId : myActionManager.getActionIds(QuickList.QUICK_LIST_PREFIX)) {
            myActionManager.unregisterAction(oldId);
        }
    }

    public void setQuickLists( QuickList[] quickLists) {
        mySchemesManager.clearAllSchemes();
        unregisterActions();
        for (QuickList quickList : quickLists) {
            mySchemesManager.addNewScheme(quickList, true);
        }
        registerActions();
    }

    private static class InvokeQuickListAction extends QuickSwitchSchemeAction {
        private final QuickList myQuickList;

        public InvokeQuickListAction( QuickList quickList) {
            myQuickList = quickList;
            myActionPlace = ActionPlaces.ACTION_PLACE_QUICK_LIST_POPUP_ACTION;
            getTemplatePresentation().setDescription(myQuickList.getDescription());
            getTemplatePresentation().setText(myQuickList.getName(), false);
        }

        @Override
        protected void fillActions(Project project,  DefaultActionGroup group,  DataContext dataContext) {
            ActionManager actionManager = ActionManager.getInstance();
            for (String actionId : myQuickList.getActionIds()) {
                if (QuickList.SEPARATOR_ID.equals(actionId)) {
                    group.addSeparator();
                }
                else {
                    AnAction action = actionManager.getAction(actionId);
                    if (action != null) {
                        group.add(action);
                    }
                }
            }
        }

        @Override
        protected boolean isEnabled() {
            return true;
        }
    }
}
