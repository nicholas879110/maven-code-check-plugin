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

package com.gome.maven.codeInsight.lookup;

import com.gome.maven.codeInsight.completion.InsertHandler;
import com.gome.maven.codeInsight.completion.InsertionContext;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiNamedElement;
import com.gome.maven.psi.SmartPointerManager;
import com.gome.maven.psi.util.PsiUtilCore;
import gnu.trove.THashSet;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Set;

/**
 * @author peter
 */
public final class LookupElementBuilder extends LookupElement {
     private final String myLookupString;
     private final Object myObject;
    private final boolean myCaseSensitive;
     private final InsertHandler<LookupElement> myInsertHandler;
     private final LookupElementRenderer<LookupElement> myRenderer;
     private final LookupElementPresentation myHardcodedPresentation;
     private final Set<String> myAllLookupStrings;

    private LookupElementBuilder( String lookupString,  Object object,  InsertHandler<LookupElement> insertHandler,
                                  LookupElementRenderer<LookupElement> renderer,
                                  LookupElementPresentation hardcodedPresentation,
                                  Set<String> allLookupStrings,
                                 boolean caseSensitive) {
        myLookupString = lookupString;
        myObject = object;
        myInsertHandler = insertHandler;
        myRenderer = renderer;
        myHardcodedPresentation = hardcodedPresentation;
        myAllLookupStrings = allLookupStrings;
        myCaseSensitive = caseSensitive;
    }

    private LookupElementBuilder( String lookupString,  Object object) {
        this(lookupString, object, null, null, null, Collections.singleton(lookupString), true);
    }

    public static LookupElementBuilder create( String lookupString) {
        return new LookupElementBuilder(lookupString, lookupString);
    }

    public static LookupElementBuilder createWithSmartPointer( String lookupString,  PsiElement element) {
        PsiUtilCore.ensureValid(element);
        return new LookupElementBuilder(lookupString,
                SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element));
    }

    public static LookupElementBuilder create( PsiNamedElement element) {
        PsiUtilCore.ensureValid(element);
        return new LookupElementBuilder(StringUtil.notNullize(element.getName()), element);
    }

    public static LookupElementBuilder createWithIcon( PsiNamedElement element) {
        PsiUtilCore.ensureValid(element);
        return create(element).withIcon(element.getIcon(0));
    }

    public static LookupElementBuilder create( Object lookupObject,  String lookupString) {
        if (lookupObject instanceof PsiElement) {
            PsiUtilCore.ensureValid((PsiElement)lookupObject);
        }
        return new LookupElementBuilder(lookupString, lookupObject);
    }

    /**
     * @deprecated use {@link #withInsertHandler(com.gome.maven.codeInsight.completion.InsertHandler)}
     */
    public LookupElementBuilder setInsertHandler( InsertHandler<LookupElement> insertHandler) {
        return withInsertHandler(insertHandler);
    }

    public LookupElementBuilder withInsertHandler( InsertHandler<LookupElement> insertHandler) {
        return new LookupElementBuilder(myLookupString, myObject, insertHandler, myRenderer, myHardcodedPresentation,
                myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #withRenderer(LookupElementRenderer)}
     */
    
    public LookupElementBuilder setRenderer( LookupElementRenderer<LookupElement> renderer) {
        return withRenderer(renderer);
    }
    
    public LookupElementBuilder withRenderer( LookupElementRenderer<LookupElement> renderer) {
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, renderer, myHardcodedPresentation,
                myAllLookupStrings, myCaseSensitive);
    }

    @Override
    
    public Set<String> getAllLookupStrings() {
        return myAllLookupStrings;
    }

    /**
     * @deprecated use {@link #withIcon(javax.swing.Icon)}
     */
    
    public LookupElementBuilder setIcon( Icon icon) {
        return withIcon(icon);
    }

    
    public LookupElementBuilder withIcon( Icon icon) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setIcon(icon);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation,
                myAllLookupStrings, myCaseSensitive);
    }

    
    private LookupElementPresentation copyPresentation() {
        final LookupElementPresentation presentation = new LookupElementPresentation();
        if (myHardcodedPresentation != null) {
            presentation.copyFrom(myHardcodedPresentation);
        } else {
            presentation.setItemText(myLookupString);
        }
        return presentation;
    }

    /**
     * @deprecated use {@link #withLookupString(String)}
     */
    
    public LookupElementBuilder addLookupString( String another) {
        return withLookupString(another);
    }
    
    public LookupElementBuilder withLookupString( String another) {
        final THashSet<String> set = new THashSet<String>(myAllLookupStrings);
        set.add(another);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, myRenderer, myHardcodedPresentation,
                Collections.unmodifiableSet(set), myCaseSensitive);
    }

    @Override
    public boolean isCaseSensitive() {
        return myCaseSensitive;
    }

    /**
     * @deprecated use {@link #withCaseSensitivity(boolean)}
     */
    
    public LookupElementBuilder setCaseSensitive(boolean caseSensitive) {
        return withCaseSensitivity(caseSensitive);
    }
    /**
     * @param caseSensitive if this lookup item should be completed in the same letter case as prefix
     * @return modified builder
     * @see com.gome.maven.codeInsight.completion.CompletionResultSet#caseInsensitive()
     */
    
    public LookupElementBuilder withCaseSensitivity(boolean caseSensitive) {
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, myRenderer, myHardcodedPresentation,
                myAllLookupStrings, caseSensitive);
    }

    /**
     * @deprecated use {@link #withItemTextForeground(java.awt.Color)}
     */
    
    public LookupElementBuilder setItemTextForeground( Color itemTextForeground) {
        return withItemTextForeground(itemTextForeground);
    }
    
    public LookupElementBuilder withItemTextForeground( Color itemTextForeground) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setItemTextForeground(itemTextForeground);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation, myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #withItemTextUnderlined(boolean)}
     */
    
    public LookupElementBuilder setItemTextUnderlined(boolean underlined) {
        return withItemTextUnderlined(underlined);
    }
    
    public LookupElementBuilder withItemTextUnderlined(boolean underlined) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setItemTextUnderlined(underlined);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation, myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #withTypeText(String)}
     */
    
    public LookupElementBuilder setTypeText( String typeText) {
        return withTypeText(typeText);
    }
    
    public LookupElementBuilder withTypeText( String typeText) {
        return withTypeText(typeText, false);
    }

    /**
     * @deprecated use {@link #withTypeText(String, boolean)}
     */
    
    public LookupElementBuilder setTypeText( String typeText, boolean grayed) {
        return withTypeText(typeText, grayed);
    }

    
    public LookupElementBuilder withTypeText( String typeText, boolean grayed) {
        return withTypeText(typeText, null, grayed);
    }

    
    public LookupElementBuilder withTypeText( String typeText,  Icon typeIcon, boolean grayed) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setTypeText(typeText, typeIcon);
        presentation.setTypeGrayed(grayed);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation,
                myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #withPresentableText(String)}
     */
    
    public LookupElementBuilder setPresentableText( String presentableText) {
        return withPresentableText(presentableText);
    }
    
    public LookupElementBuilder withPresentableText( String presentableText) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setItemText(presentableText);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation,
                myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #bold()}
     */
    
    public LookupElementBuilder setBold() {
        return bold();
    }
    
    public LookupElementBuilder bold() {
        return withBoldness(true);
    }

    /**
     * @deprecated use {@link #withBoldness(boolean)}
     */
    
    public LookupElementBuilder setBold(boolean bold) {
        return withBoldness(bold);
    }
    
    public LookupElementBuilder withBoldness(boolean bold) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setItemTextBold(bold);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation,
                myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #strikeout()}
     */
    
    public LookupElementBuilder setStrikeout() {
        return strikeout();
    }
    
    public LookupElementBuilder strikeout() {
        return withStrikeoutness(true);
    }

    /**
     * @deprecated use {@link #withStrikeoutness(boolean)}
     */
    
    public LookupElementBuilder setStrikeout(boolean strikeout) {
        return withStrikeoutness(strikeout);
    }
    
    public LookupElementBuilder withStrikeoutness(boolean strikeout) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setStrikeout(strikeout);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation,
                myAllLookupStrings, myCaseSensitive);
    }

    /**
     * @deprecated use {@link #withTailText(String)}
     */
    
    public LookupElementBuilder setTailText( String tailText) {
        return withTailText(tailText);
    }
    
    public LookupElementBuilder withTailText( String tailText) {
        return withTailText(tailText, false);
    }

    /**
     * @deprecated use {@link #withTailText(String, boolean)}
     */
    
    public LookupElementBuilder setTailText( String tailText, boolean grayed) {
        return withTailText(tailText, grayed);
    }
    
    public LookupElementBuilder withTailText( String tailText, boolean grayed) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.setTailText(tailText, grayed);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation,
                myAllLookupStrings, myCaseSensitive);
    }

    
    public LookupElementBuilder appendTailText( String tailText, boolean grayed) {
        final LookupElementPresentation presentation = copyPresentation();
        presentation.appendTailText(tailText, grayed);
        return new LookupElementBuilder(myLookupString, myObject, myInsertHandler, null, presentation, myAllLookupStrings, myCaseSensitive);
    }

    
    public LookupElement withAutoCompletionPolicy(AutoCompletionPolicy policy) {
        return policy.applyPolicy(this);
    }

    
    @Override
    public String getLookupString() {
        return myLookupString;
    }

    
    @Override
    public Object getObject() {
        return myObject;
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if (myInsertHandler != null) {
            myInsertHandler.handleInsert(context, this);
        }
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        if (myRenderer != null) {
            myRenderer.renderElement(this, presentation);
        }
        else if (myHardcodedPresentation != null) {
            presentation.copyFrom(myHardcodedPresentation);
        } else {
            presentation.setItemText(myLookupString);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LookupElementBuilder that = (LookupElementBuilder)o;

        final InsertHandler<LookupElement> insertHandler = that.myInsertHandler;
        if (myInsertHandler != null && insertHandler != null ? !myInsertHandler.getClass().equals(insertHandler.getClass())
                : myInsertHandler != insertHandler) return false;
        if (!myLookupString.equals(that.myLookupString)) return false;
        if (!myObject.equals(that.myObject)) return false;

        final LookupElementRenderer<LookupElement> renderer = that.myRenderer;
        if (myRenderer != null && renderer != null ? !myRenderer.getClass().equals(renderer.getClass()) : myRenderer != renderer) return false;

        return true;
    }

    @Override
    public String toString() {
        return "LookupElementBuilder: string=" + getLookupString() + "; handler=" + myInsertHandler;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + (myInsertHandler != null ? myInsertHandler.getClass().hashCode() : 0);
        result = 31 * result + (myLookupString.hashCode());
        result = 31 * result + (myObject.hashCode());
        result = 31 * result + (myRenderer != null ? myRenderer.getClass().hashCode() : 0);
        return result;
    }

}
