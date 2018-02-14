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

package com.gome.maven.history.integration;

import com.gome.maven.history.*;
import com.gome.maven.history.core.*;
import com.gome.maven.history.core.tree.RootEntry;
import com.gome.maven.history.utils.LocalHistoryLog;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.PathManager;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Clock;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.ShutDownTracker;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalHistoryImpl extends LocalHistory implements ApplicationComponent {
    private ChangeList myChangeList;
    private LocalHistoryFacade myVcs;
    private IdeaGateway myGateway;

    private LocalHistoryEventDispatcher myEventDispatcher;

    private final AtomicBoolean isInitialized = new AtomicBoolean();
    private Runnable myShutdownTask;

    public static LocalHistoryImpl getInstanceImpl() {
        return (LocalHistoryImpl)getInstance();
    }

    @Override
    public void initComponent() {
        if (!ApplicationManager.getApplication().isUnitTestMode() && ApplicationManager.getApplication().isHeadlessEnvironment()) return;

        myShutdownTask = new Runnable() {
            @Override
            public void run() {
                disposeComponent();
            }
        };
        ShutDownTracker.getInstance().registerShutdownTask(myShutdownTask);

        initHistory();
        isInitialized.set(true);
    }

    protected void initHistory() {
        ChangeListStorage storage;
        try {
            storage = new ChangeListStorageImpl(getStorageDir());
        }
        catch (Throwable e) {
            LocalHistoryLog.LOG.warn("cannot create storage, in-memory  implementation will be used", e);
            storage = new InMemoryChangeListStorage();
        }
        myChangeList = new ChangeList(storage);
        myVcs = new LocalHistoryFacade(myChangeList);

        myGateway = new IdeaGateway();

        myEventDispatcher = new LocalHistoryEventDispatcher(myVcs, myGateway);

        CommandProcessor.getInstance().addCommandListener(myEventDispatcher);

        VirtualFileManager fm = VirtualFileManager.getInstance();
        fm.addVirtualFileListener(myEventDispatcher);
        fm.addVirtualFileManagerListener(myEventDispatcher);

        if (ApplicationManager.getApplication().isInternal() && !ApplicationManager.getApplication().isUnitTestMode()) {
            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                @Override
                public void run() {
                    validateStorage();
                }
            });
        }
    }

    private void validateStorage() {
        if (ApplicationManager.getApplication().isInternal() && !ApplicationManager.getApplication().isUnitTestMode()) {
            LocalHistoryLog.LOG.info("Checking local history storage...");
            try {
                long before = Clock.getTime();
                myVcs.getChangeListInTests().getChangesInTests();
                LocalHistoryLog.LOG.info("Local history storage seems to be ok (took " + ((Clock.getTime() - before) / 1000) + " sec)");
            }
            catch (Exception e) {
                LocalHistoryLog.LOG.error(e);
            }
        }
    }

    public File getStorageDir() {
        return new File(getSystemPath(), "LocalHistory");
    }

    protected String getSystemPath() {
        return PathManager.getSystemPath();
    }

    @Override
    public void disposeComponent() {
        if (!isInitialized.getAndSet(false)) return;

        long period = Registry.intValue("localHistory.daysToKeep") * 1000L * 60L * 60L * 24L;

        VirtualFileManager fm = VirtualFileManager.getInstance();
        fm.removeVirtualFileListener(myEventDispatcher);
        fm.removeVirtualFileManagerListener(myEventDispatcher);
        CommandProcessor.getInstance().removeCommandListener(myEventDispatcher);


        validateStorage();
        LocalHistoryLog.LOG.debug("Purging local history...");
        myChangeList.purgeObsolete(period);
        validateStorage();

        myChangeList.close();
        LocalHistoryLog.LOG.debug("Local history storage successfully closed.");

        ShutDownTracker.getInstance().unregisterShutdownTask(myShutdownTask);
    }

    
    public void cleanupForNextTest() {
        disposeComponent();
        FileUtil.delete(getStorageDir());
        initComponent();
    }

    @Override
    public LocalHistoryAction startAction(String name) {
        if (!isInitialized()) return LocalHistoryAction.NULL;

        LocalHistoryActionImpl a = new LocalHistoryActionImpl(myEventDispatcher, name);
        a.start();
        return a;
    }

    @Override
    public Label putUserLabel(Project p,  String name) {
        if (!isInitialized()) return Label.NULL_INSTANCE;
        myGateway.registerUnsavedDocuments(myVcs);
        return label(myVcs.putUserLabel(name, getProjectId(p)));
    }

    private String getProjectId(Project p) {
        return p.getLocationHash();
    }

    @Override
    public Label putSystemLabel(Project p,  String name, int color) {
        if (!isInitialized()) return Label.NULL_INSTANCE;
        myGateway.registerUnsavedDocuments(myVcs);
        return label(myVcs.putSystemLabel(name, getProjectId(p), color));
    }

    private Label label(final LabelImpl impl) {
        return new Label() {
            @Override
            public ByteContent getByteContent(final String path) {
                return ApplicationManager.getApplication().runReadAction(new Computable<ByteContent>() {
                    @Override
                    public ByteContent compute() {
                        RootEntry root = myGateway.createTransientRootEntryForPathOnly(path);
                        return impl.getByteContent(root, path);
                    }
                });
            }
        };
    }

    
    @Override
    public byte[] getByteContent(final VirtualFile f, final FileRevisionTimestampComparator c) {
        if (!isInitialized()) return null;
        if (!myGateway.areContentChangesVersioned(f)) return null;
        return ApplicationManager.getApplication().runReadAction(new Computable<byte[]>() {
            @Override
            public byte[] compute() {
                return new ByteContentRetriever(myGateway, myVcs, f, c).getResult();
            }
        });
    }

    @Override
    public boolean isUnderControl(VirtualFile f) {
        return isInitialized() && myGateway.isVersioned(f);
    }

    private boolean isInitialized() {
        return isInitialized.get();
    }

    @Override
    
    
    public String getComponentName() {
        return "Local History";
    }

    
    public LocalHistoryFacade getFacade() {
        return myVcs;
    }

    
    public IdeaGateway getGateway() {
        return myGateway;
    }
}
