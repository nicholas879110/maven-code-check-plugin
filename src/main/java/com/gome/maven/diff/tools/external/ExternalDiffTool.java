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
package com.gome.maven.diff.tools.external;

import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.DiffManagerEx;
import com.gome.maven.diff.chains.DiffRequestChain;
import com.gome.maven.diff.chains.DiffRequestProducer;
import com.gome.maven.diff.chains.DiffRequestProducerException;
import com.gome.maven.diff.chains.SimpleDiffRequestChain;
import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.requests.ContentDiffRequest;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.execution.ExecutionException;
import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationType;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.UserDataHolderBase;
import com.gome.maven.openapi.util.text.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalDiffTool {
    public static final Logger LOG = Logger.getInstance(ExternalDiffTool.class);

    public static boolean isDefault() {
        return ExternalDiffSettings.getInstance().isDiffEnabled() && ExternalDiffSettings.getInstance().isDiffDefault();
    }

    public static boolean isEnabled() {
        return ExternalDiffSettings.getInstance().isDiffEnabled();
    }

    public static void show( final Project project,
                             final DiffRequestChain chain,
                             final DiffDialogHints hints) {
        try {
            //noinspection unchecked
            final Ref<List<DiffRequest>> requestsRef = new Ref<List<DiffRequest>>();
            final Ref<Throwable> exceptionRef = new Ref<Throwable>();
            ProgressManager.getInstance().run(new Task.Modal(project, "Loading Requests", true) {
                @Override
                public void run( ProgressIndicator indicator) {
                    try {
                        requestsRef.set(collectRequests(project, chain, indicator));
                    }
                    catch (Throwable e) {
                        exceptionRef.set(e);
                    }
                }
            });

            if (!exceptionRef.isNull()) throw exceptionRef.get();

            List<DiffRequest> showInBuiltin = new ArrayList<DiffRequest>();
            for (DiffRequest request : requestsRef.get()) {
                if (canShow(request)) {
                    showRequest(project, request);
                }
                else {
                    showInBuiltin.add(request);
                }
            }

            if (!showInBuiltin.isEmpty()) {
                DiffManagerEx.getInstance().showDiffBuiltin(project, new SimpleDiffRequestChain(showInBuiltin), hints);
            }
        }
        catch (ProcessCanceledException ignore) {
        }
        catch (Throwable e) {
            LOG.error(e);
            Messages.showErrorDialog(project, e.getMessage(), "Can't Show Diff In External Tool");
        }
    }

    
    private static List<DiffRequest> collectRequests( Project project,
                                                      final DiffRequestChain chain,
                                                      ProgressIndicator indicator) {
        List<DiffRequest> requests = new ArrayList<DiffRequest>();

        UserDataHolderBase context = new UserDataHolderBase();
        List<String> errorRequests = new ArrayList<String>();

        // TODO: show all changes on explicit selection
        List<? extends DiffRequestProducer> producers = Collections.singletonList(chain.getRequests().get(chain.getIndex()));

        for (DiffRequestProducer producer : producers) {
            try {
                requests.add(producer.process(context, indicator));
            }
            catch (DiffRequestProducerException e) {
                LOG.warn(e);
                errorRequests.add(producer.getName());
            }
        }

        if (!errorRequests.isEmpty()) {
            new Notification("diff", "Can't load some changes", StringUtil.join(errorRequests, "<br>"), NotificationType.ERROR).notify(project);
        }

        return requests;
    }

    public static void showRequest( Project project,  DiffRequest request)
            throws ExecutionException, IOException {
        request.onAssigned(true);

        ExternalDiffSettings settings = ExternalDiffSettings.getInstance();

        List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
        List<String> titles = ((ContentDiffRequest)request).getContentTitles();

        ExternalDiffToolUtil.execute(settings, contents, titles, request.getTitle());

        request.onAssigned(false);
    }

    public static boolean canShow( DiffRequest request) {
        if (!(request instanceof ContentDiffRequest)) return false;
        List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
        if (contents.size() != 2 && contents.size() != 3) return false;
        for (DiffContent content : contents) {
            if (!ExternalDiffToolUtil.canCreateFile(content)) return false;
        }
        return true;
    }
}
