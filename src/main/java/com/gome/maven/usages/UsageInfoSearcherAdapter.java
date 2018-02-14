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
package com.gome.maven.usages;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.IndexNotReadyException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.usageView.UsageInfo;
import com.gome.maven.util.Processor;

public abstract class UsageInfoSearcherAdapter implements UsageSearcher {
    protected void processUsages(final  Processor<Usage> processor,  Project project) {
        final Ref<UsageInfo[]> refUsages = new Ref<UsageInfo[]>();
        final Ref<Boolean> dumbModeOccurred = new Ref<Boolean>();
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                try {
                    refUsages.set(findUsages());
                }
                catch (IndexNotReadyException e) {
                    dumbModeOccurred.set(true);
                }
            }
        });
        if (!dumbModeOccurred.isNull()) {
            DumbService.getInstance(project).showDumbModeNotification("Usage search is not available until indices are ready");
            return;
        }
        final Usage[] usages = ApplicationManager.getApplication().runReadAction(new Computable<Usage[]>() {
            @Override
            public Usage[] compute() {
                return UsageInfo2UsageAdapter.convert(refUsages.get());
            }
        });

        for (final Usage usage : usages) {
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    processor.process(usage);
                }
            });
        }
    }

    protected abstract UsageInfo[] findUsages();
}
