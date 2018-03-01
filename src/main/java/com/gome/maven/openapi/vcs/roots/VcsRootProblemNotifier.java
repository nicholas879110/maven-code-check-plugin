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
package com.gome.maven.openapi.vcs.roots;

import com.gome.maven.idea.ActionsBundle;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.options.ShowSettingsUtil;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.*;
import com.gome.maven.openapi.vcs.changes.ChangeListManager;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.HashSet;
import com.gome.maven.vcsUtil.VcsUtil;

import javax.swing.event.HyperlinkEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.gome.maven.openapi.util.text.StringUtil.pluralize;

/**
 * Searches for Vcs roots problems via {@link VcsRootErrorsFinder} and notifies about them.
 *
 * @author Nadya Zabrodina
 */
public class VcsRootProblemNotifier {

    public static final Function<VcsRootError, String> PATH_FROM_ROOT_ERROR = new Function<VcsRootError, String>() {
        @Override
        public String fun( VcsRootError error) {
            return error.getMapping();
        }
    };

     private final Project myProject;
     private final VcsConfiguration mySettings;
     private final ProjectLevelVcsManager myVcsManager;
     private final ChangeListManager myChangeListManager;

     private final Set<String> myReportedUnregisteredRoots;

     private Notification myNotification;
     private final Object NOTIFICATION_LOCK = new Object();

    public static VcsRootProblemNotifier getInstance( Project project) {
        return new VcsRootProblemNotifier(project);
    }

    private VcsRootProblemNotifier( Project project) {
        myProject = project;
        mySettings = VcsConfiguration.getInstance(myProject);
        myChangeListManager = ChangeListManager.getInstance(project);
        myVcsManager = ProjectLevelVcsManager.getInstance(project);
        myReportedUnregisteredRoots = new HashSet<String>(mySettings.IGNORED_UNREGISTERED_ROOTS);
    }

    public void rescanAndNotifyIfNeeded() {
        if (!mySettings.SHOW_VCS_ERROR_NOTIFICATIONS) {
            return;
        }

        Collection<VcsRootError> errors = scan();
        if (errors.isEmpty()) {
            synchronized (NOTIFICATION_LOCK) {
                expireNotification();
            }
            return;
        }

        Collection<VcsRootError> importantUnregisteredRoots = getImportantUnregisteredMappings(errors);
        Collection<VcsRootError> invalidRoots = getInvalidRoots(errors);

        List<String> unregRootPaths = ContainerUtil.map(importantUnregisteredRoots, PATH_FROM_ROOT_ERROR);
        if (invalidRoots.isEmpty() && (importantUnregisteredRoots.isEmpty() || myReportedUnregisteredRoots.containsAll(unregRootPaths))) {
            return;
        }
        myReportedUnregisteredRoots.addAll(unregRootPaths);

        String title = makeTitle(importantUnregisteredRoots, invalidRoots);
        String description = makeDescription(importantUnregisteredRoots, invalidRoots);

        synchronized (NOTIFICATION_LOCK) {
            expireNotification();
            NotificationListener listener = new MyNotificationListener(myProject, mySettings, myVcsManager, importantUnregisteredRoots);
            VcsNotifier notifier = VcsNotifier.getInstance(myProject);
            myNotification = invalidRoots.isEmpty()
                    ? notifier.notifyMinorInfo(title, description, listener)
                    : notifier.notifyError(title, description, listener);
        }
    }

    private boolean isUnderOrAboveProjectDir( String mapping) {
        String projectDir = myProject.getBasePath();
        return mapping.equals(VcsDirectoryMapping.PROJECT_CONSTANT) ||
                FileUtil.isAncestor(projectDir, mapping, false) ||
                FileUtil.isAncestor(mapping, projectDir, false);
    }

    private boolean isIgnored( String mapping) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(mapping);
        return file != null && myChangeListManager.isIgnoredFile(file);
    }

    private void expireNotification() {
        if (myNotification != null) {
            final Notification notification = myNotification;
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    notification.expire();
                }
            });

            myNotification = null;
        }
    }

    
    private Collection<VcsRootError> scan() {
        return new VcsRootErrorsFinder(myProject).find();
    }

    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    
    private static String makeDescription( Collection<VcsRootError> unregisteredRoots,
                                           Collection<VcsRootError> invalidRoots) {
        Function<VcsRootError, String> rootToDisplayableString = new Function<VcsRootError, String>() {
            @Override
            public String fun(VcsRootError rootError) {
                if (rootError.getMapping().equals(VcsDirectoryMapping.PROJECT_CONSTANT)) {
                    return StringUtil.escapeXml(rootError.getMapping());
                }
                return FileUtil.toSystemDependentName(rootError.getMapping());
            }
        };

        StringBuilder description = new StringBuilder();
        if (!invalidRoots.isEmpty()) {
            if (invalidRoots.size() == 1) {
                VcsRootError rootError = invalidRoots.iterator().next();
                String vcsName = rootError.getVcsKey().getName();
                description.append(String.format("The directory %s is registered as a %s root, but no %s repositories were found there.",
                        rootToDisplayableString.fun(rootError), vcsName, vcsName));
            }
            else {
                description.append("The following directories are registered as VCS roots, but they are not: <br/>" +
                        StringUtil.join(invalidRoots, rootToDisplayableString, "<br/>"));
            }
            description.append("<br/>");
        }

        if (!unregisteredRoots.isEmpty()) {
            if (unregisteredRoots.size() == 1) {
                VcsRootError unregisteredRoot = unregisteredRoots.iterator().next();
                description.append(String.format("The directory %s is under %s, but is not registered in the Settings.",
                        rootToDisplayableString.fun(unregisteredRoot), unregisteredRoot.getVcsKey().getName()));
            }
            else {
                description.append("The following directories are roots of VCS repositories, but they are not registered in the Settings: <br/>" +
                        StringUtil.join(unregisteredRoots, rootToDisplayableString, "<br/>"));
            }
            description.append("<br/>");
        }

        String add = invalidRoots.isEmpty() ? "<a href='add'>Add " + pluralize("root", unregisteredRoots.size()) + "</a>&nbsp;&nbsp;" : "";
        String configure = "<a href='configure'>Configure</a>";
        String ignore = invalidRoots.isEmpty() ? "&nbsp;&nbsp;<a href='ignore'>Ignore</a>" : "";
        description.append(add + configure + ignore);

        return description.toString();
    }

    
    private static String makeTitle( Collection<VcsRootError> unregisteredRoots,  Collection<VcsRootError> invalidRoots) {
        String title;
        if (unregisteredRoots.isEmpty()) {
            title = "Invalid VCS root " + pluralize("mapping", invalidRoots.size());
        }
        else if (invalidRoots.isEmpty()) {
            title = "Unregistered VCS " + pluralize("root", unregisteredRoots.size()) + " detected";
        }
        else {
            title = "VCS root configuration problems";
        }
        return title;
    }

    
    private List<VcsRootError> getImportantUnregisteredMappings( Collection<VcsRootError> errors) {
        return ContainerUtil.filter(errors, new Condition<VcsRootError>() {
            @Override
            public boolean value(VcsRootError error) {
                String mapping = error.getMapping();
                return error.getType() == VcsRootError.Type.UNREGISTERED_ROOT && isUnderOrAboveProjectDir(mapping) && !isIgnored(mapping);
            }
        });
    }

    
    private static Collection<VcsRootError> getInvalidRoots( Collection<VcsRootError> errors) {
        return ContainerUtil.filter(errors, new Condition<VcsRootError>() {
            @Override
            public boolean value(VcsRootError error) {
                return error.getType() == VcsRootError.Type.EXTRA_MAPPING;
            }
        });
    }

    private static class MyNotificationListener extends NotificationListener.Adapter {

         private final Project myProject;
         private final VcsConfiguration mySettings;
         private final ProjectLevelVcsManager myVcsManager;
         private final Collection<VcsRootError> myImportantUnregisteredRoots;

        private MyNotificationListener( Project project,
                                        VcsConfiguration settings,
                                        ProjectLevelVcsManager vcsManager,
                                        Collection<VcsRootError> importantUnregisteredRoots) {
            myProject = project;
            mySettings = settings;
            myVcsManager = vcsManager;
            myImportantUnregisteredRoots = importantUnregisteredRoots;
        }

        @Override
        protected void hyperlinkActivated( Notification notification,  HyperlinkEvent event) {
            if (event.getDescription().equals("configure") && !myProject.isDisposed()) {
                ShowSettingsUtil.getInstance().showSettingsDialog(myProject, ActionsBundle.message("group.VcsGroup.text"));
                Collection<VcsRootError> errorsAfterPossibleFix = getInstance(myProject).scan();
                if (errorsAfterPossibleFix.isEmpty() && !notification.isExpired()) {
                    notification.expire();
                }
            }
            else if (event.getDescription().equals("ignore")) {
                mySettings.addIgnoredUnregisteredRoots(ContainerUtil.map(myImportantUnregisteredRoots, PATH_FROM_ROOT_ERROR));
                notification.expire();
            }
            else if (event.getDescription().equals("add")) {
                List<VcsDirectoryMapping> mappings = myVcsManager.getDirectoryMappings();
                for (VcsRootError root : myImportantUnregisteredRoots) {
                    mappings = VcsUtil.addMapping(mappings, root.getMapping(), root.getVcsKey().getName());
                }
                myVcsManager.setDirectoryMappings(mappings);
            }
        }
    }
}
