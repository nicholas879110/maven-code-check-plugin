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
package com.gome.maven.compiler.server;

/**
 * @author Eugene Zhuravlev
 *         Date: 20-Oct-14
 */
class PreloadedProcessMessageHandler extends DelegatingMessageHandler {
    private volatile BuilderMessageHandler myDelegateHandler;

    public PreloadedProcessMessageHandler() {
    }

    @Override
    protected BuilderMessageHandler getDelegateHandler() {
        final BuilderMessageHandler delegate = myDelegateHandler;
        return delegate != null? delegate : BuilderMessageHandler.DEAF;
    }

    public void setDelegateHandler(BuilderMessageHandler delegateHandler) {
        myDelegateHandler = delegateHandler;
    }
}
