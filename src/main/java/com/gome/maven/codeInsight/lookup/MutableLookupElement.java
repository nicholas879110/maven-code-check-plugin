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
package com.gome.maven.codeInsight.lookup;

import com.gome.maven.codeInsight.TailType;
import com.gome.maven.codeInsight.completion.InsertHandler;

import javax.swing.*;

/**
 * @author peter
 */
public abstract class MutableLookupElement<T> extends LookupElement{
    @Override
    
    public abstract T getObject();

    public abstract MutableLookupElement<T> setBold();

    public abstract MutableLookupElement<T> setAutoCompletionPolicy(AutoCompletionPolicy policy);

    
    public abstract MutableLookupElement<T> setIcon(Icon icon);

    
    public abstract MutableLookupElement<T> setPriority(double priority);

    
    public abstract MutableLookupElement<T> setPresentableText( String displayText);

    
    public abstract MutableLookupElement<T> setTypeText(final String text);

    
    public abstract MutableLookupElement<T> setTailText(final String text, final boolean grayed);

    
    public abstract MutableLookupElement<T> setTailType( TailType type);

    /**
     * Use {@link com.gome.maven.codeInsight.completion.CompletionResultSet#caseInsensitive()} instead
     * @param caseSensitive
     * @return
     */
    @Deprecated
    
    public abstract MutableLookupElement<T> setCaseSensitive(boolean caseSensitive);

    public abstract MutableLookupElement<T> addLookupStrings( String... additionalLookupStrings);

    public abstract MutableLookupElement<T> setInsertHandler(InsertHandler<? extends LookupElement> insertHandler);

    public abstract InsertHandler<? extends MutableLookupElement> getInsertHandler();

    public abstract boolean isBold();
}
