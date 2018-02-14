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

/**
 * @author Vladimir Kondratyev
 */
package com.gome.maven.openapi.wm.impl.commands;

import com.gome.maven.openapi.wm.impl.InternalDecorator;
import com.gome.maven.openapi.wm.impl.StripeButton;
import com.gome.maven.openapi.wm.impl.WindowInfoImpl;

/**
 * Apply <code>info</code> to the corresponded tool button and decarator.
 * Command uses freezed copy of passed <code>info</code> object.
 */
public final class ApplyWindowInfoCmd extends FinalizableCommand {
    private final WindowInfoImpl myInfo;
    private final StripeButton myButton;
    private final InternalDecorator myDecorator;

    public ApplyWindowInfoCmd( final WindowInfoImpl info,
                               final StripeButton button,
                               final InternalDecorator decorator,
                               Runnable finishCallBack) {
        super(finishCallBack);
        myInfo = info.copy();
        myButton = button;
        myDecorator = decorator;
    }

    @Override
    public final void run() {
        try {
            myButton.apply(myInfo);
            myDecorator.apply(myInfo);
        }
        finally {
            finish();
        }
    }
}
