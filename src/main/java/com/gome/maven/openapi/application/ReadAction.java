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
package com.gome.maven.openapi.application;

import com.gome.maven.openapi.util.Computable;

public abstract class ReadAction<T> extends BaseActionRunnable<T> {
    @Override
    public RunResult<T> execute() {
        final RunResult<T> result = new RunResult<T>(this);
        return ApplicationManager.getApplication().runReadAction(new Computable<RunResult<T>>() {
            @Override
            public RunResult<T> compute() {
                return result.run();
            }
        });
    }

    public static AccessToken start() {
        return ApplicationManager.getApplication().acquireReadActionLock();
    }
}
