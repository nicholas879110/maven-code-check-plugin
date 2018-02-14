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
package com.gome.maven.execution;

import com.gome.maven.AbstractBundle;


/**
 * @author lesya
 */
public class ExecutionBundle extends AbstractBundle {

    public static String message( String key,  Object... params) {
        return ourInstance.getMessage(key, params);
    }

    public static final String PATH_TO_BUNDLE = "messages.ExecutionBundle";
    private static final AbstractBundle ourInstance = new ExecutionBundle();

    private ExecutionBundle() {
        super(PATH_TO_BUNDLE);
    }
}