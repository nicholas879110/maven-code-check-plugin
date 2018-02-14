/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.template.impl;

import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.template.Expression;
import com.gome.maven.codeInsight.template.ExpressionContext;
import com.gome.maven.codeInsight.template.Result;

public class EmptyNode extends Expression {
    public EmptyNode() {
    }

    @Override
    public Result calculateResult(ExpressionContext context) {
        return null;
    }

    @Override
    public Result calculateQuickResult(ExpressionContext context) {
        return null;
    }

    @Override
    public LookupElement[] calculateLookupItems(ExpressionContext context) {
        return null;
    }

}
