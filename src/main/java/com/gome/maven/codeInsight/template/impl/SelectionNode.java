/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import com.gome.maven.codeInsight.template.TextResult;

/**
 * @author mike
 */
public class SelectionNode extends Expression {
    public SelectionNode() {
    }

    @Override
    public LookupElement[] calculateLookupItems(ExpressionContext context) {
        return LookupElement.EMPTY_ARRAY;
    }

    @Override
    public Result calculateQuickResult(ExpressionContext context) {
        final String selection = context.getProperty(ExpressionContext.SELECTION);
        return new TextResult(selection == null ? "" : selection);
    }

    @Override
    public Result calculateResult(ExpressionContext context) {
        return calculateQuickResult(context);
    }

}