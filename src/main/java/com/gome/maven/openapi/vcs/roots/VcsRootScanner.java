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

import com.gome.maven.ProjectTopics;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootEvent;
import com.gome.maven.openapi.roots.ModuleRootListener;
import com.gome.maven.openapi.vcs.ProjectLevelVcsManager;
import com.gome.maven.openapi.vcs.VcsListener;
import com.gome.maven.openapi.vcs.VcsRootChecker;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.util.Alarm;
import com.gome.maven.util.messages.MessageBus;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class VcsRootScanner implements BulkFileListener, ModuleRootListener, VcsListener {

     private final VcsRootProblemNotifier myRootProblemNotifier;
     private final VcsRootChecker[] myCheckers;

     private final Alarm myAlarm;
    private static final long WAIT_BEFORE_SCAN = TimeUnit.SECONDS.toMillis(1);

    public static void start( Project project,  VcsRootChecker[] checkers) {
        new VcsRootScanner(project, checkers).scheduleScan();
    }

    private VcsRootScanner( Project project,  VcsRootChecker[] checkers) {
        myRootProblemNotifier = VcsRootProblemNotifier.getInstance(project);
        myCheckers = checkers;

        final MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, this);
        messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, this);
        messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, this);

        myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, project);
    }

    @Override
    public void before( List<? extends VFileEvent> events) {
    }

    @Override
    public void after( List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            String filePath = event.getPath();
            for (VcsRootChecker checker : myCheckers) {
                if (checker.isVcsDir(filePath)) {
                    scheduleScan();
                    break;
                }
            }
        }
    }

    @Override
    public void beforeRootsChange(ModuleRootEvent event) {
    }

    @Override
    public void rootsChanged(ModuleRootEvent event) {
        scheduleScan();
    }

    @Override
    public void directoryMappingChanged() {
        scheduleScan();
    }

    private void scheduleScan() {
        if (myAlarm.isDisposed()) {
            return;
        }

        myAlarm.cancelAllRequests(); // one scan is enough, no need to queue, they all do the same
        myAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                myRootProblemNotifier.rescanAndNotifyIfNeeded();
            }
        }, WAIT_BEFORE_SCAN);
    }
}
