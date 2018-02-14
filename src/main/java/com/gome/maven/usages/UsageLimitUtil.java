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

import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.ui.GuiUtils;
import com.gome.maven.usageView.UsageViewBundle;

/**
 * User: cdr
 */
public class UsageLimitUtil {
    public static final int USAGES_LIMIT = 1000;

    public static void showAndCancelIfAborted( Project project,
                                               String message,
                                               UsageViewPresentation usageViewPresentation) {
        Result retCode = showTooManyUsagesWarning(project, message, usageViewPresentation);

        if (retCode != Result.CONTINUE) {
            throw new ProcessCanceledException();
        }
    }

    public enum Result {
        CONTINUE, ABORT
    }

    
    public static Result showTooManyUsagesWarning( final Project project,
                                                   final String message,
                                                   final UsageViewPresentation usageViewPresentation) {
        int result = runOrInvokeAndWait(new Computable<Integer>() {
            @Override
            public Integer compute() {
                String title = UsageViewBundle.message("find.excessive.usages.title", StringUtil.capitalize(StringUtil.pluralize(usageViewPresentation.getUsagesWord())));
                return Messages.showOkCancelDialog(project, message,
                        title, UsageViewBundle.message("button.text.continue"), UsageViewBundle.message("button.text.abort"),
                        Messages.getWarningIcon());
            }
        });
        return result == Messages.OK ? Result.CONTINUE : Result.ABORT;
    }

    private static int runOrInvokeAndWait( final Computable<Integer> f) {
        final int[] answer = new int[1];
        try {
            GuiUtils.runOrInvokeAndWait(new Runnable() {
                @Override
                public void run() {
                    answer[0] = f.compute();
                }
            });
        }
        catch (Exception e) {
            answer[0] = 0;
        }

        return answer[0];
    }
}
