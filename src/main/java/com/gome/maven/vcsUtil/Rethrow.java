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
package com.gome.maven.vcsUtil;

/**
 * @author irengrig
 */
public class Rethrow {
    private Rethrow() {
    }

    public static void reThrowRuntime(final Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }
}
