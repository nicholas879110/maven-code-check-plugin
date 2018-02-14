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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Sep 26, 2007
 * Time: 1:56:28 PM
 */
package com.gome.maven.openapi.progress.util;

import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.StandardProgressIndicator;
import com.gome.maven.openapi.progress.WrappedProgressIndicator;

public class ProgressWrapper extends AbstractProgressIndicatorBase implements WrappedProgressIndicator, StandardProgressIndicator {
    private final ProgressIndicator myOriginal;
    private final boolean myCheckCanceledForMe;

    protected ProgressWrapper( ProgressIndicator original) {
        this(original, false);
    }

    protected ProgressWrapper( ProgressIndicator original, boolean checkCanceledForMe) {
        myOriginal = original;
        myCheckCanceledForMe = checkCanceledForMe;
    }


    @Override
    public final void cancel() {
        super.cancel();
    }


    @Override
    public final boolean isCanceled() {
        return myOriginal.isCanceled() || myCheckCanceledForMe && super.isCanceled();
    }

    @Override
    public final void checkCanceled() {
        myOriginal.checkCanceled();
        super.checkCanceled();
    }

    @Override
    
    public ProgressIndicator getOriginalProgressIndicator() {
        return myOriginal;
    }

    public static ProgressWrapper wrap( ProgressIndicator indicator) {
        return indicator == null || indicator instanceof ProgressWrapper ? (ProgressWrapper)indicator : new ProgressWrapper(indicator);
    }

    public static ProgressIndicator unwrap(ProgressIndicator indicator) {
        return indicator instanceof ProgressWrapper ?
                ((ProgressWrapper)indicator).getOriginalProgressIndicator() : indicator;
    }
}
