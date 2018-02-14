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

package com.gome.maven.codeInsight.completion;

import com.gome.maven.codeInsight.CodeInsightSettings;
import com.gome.maven.codeInsight.TailType;
import com.gome.maven.codeInsight.lookup.Lookup;
import com.gome.maven.codeInsight.lookup.LookupElement;
import com.gome.maven.codeInsight.lookup.LookupItem;
import com.gome.maven.codeInsight.lookup.LookupValueWithPsiElement;
import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.NotNullLazyValue;
import com.gome.maven.patterns.ElementPattern;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.filters.TrueFilter;
import com.gome.maven.util.containers.HashMap;

import java.util.*;

import static com.gome.maven.patterns.PlatformPatterns.character;

public class CompletionUtil {
    public static final Key<TailType> TAIL_TYPE_ATTR = LookupItem.TAIL_TYPE_ATTR;

    private static final CompletionData ourGenericCompletionData = new CompletionData() {
        {
            final CompletionVariant variant = new CompletionVariant(PsiElement.class, TrueFilter.INSTANCE);
            variant.addCompletionFilter(TrueFilter.INSTANCE, TailType.NONE);
            registerVariant(variant);
        }
    };
    private static final HashMap<FileType, NotNullLazyValue<CompletionData>> ourCustomCompletionDatas = new HashMap<FileType, NotNullLazyValue<CompletionData>>();

    public static final  String DUMMY_IDENTIFIER = CompletionInitializationContext.DUMMY_IDENTIFIER;
    public static final  String DUMMY_IDENTIFIER_TRIMMED = DUMMY_IDENTIFIER.trim();

    public static boolean startsWith(String text, String prefix) {
        //if (text.length() <= prefix.length()) return false;
        return toLowerCase(text).startsWith(toLowerCase(prefix));
    }

    private static String toLowerCase(String text) {
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        switch (settings.COMPLETION_CASE_SENSITIVE) {
            case CodeInsightSettings.NONE:
                return text.toLowerCase();

            case CodeInsightSettings.FIRST_LETTER: {
                StringBuffer buffer = new StringBuffer();
                buffer.append(text.toLowerCase());
                if (buffer.length() > 0) {
                    buffer.setCharAt(0, text.charAt(0));
                }
                return buffer.toString();
            }

            default:
                return text;
        }
    }

    
    public static CompletionData getCompletionDataByElement( final PsiElement position,  PsiFile originalFile) {
        if (position == null) return null;

        PsiElement parent = position.getParent();
        Language language = parent == null ? position.getLanguage() : parent.getLanguage();
        final FileType fileType = language.getAssociatedFileType();
        if (fileType != null) {
            final CompletionData mainData = getCompletionDataByFileType(fileType);
            if (mainData != null) {
                return mainData;
            }
        }

        final CompletionData mainData = getCompletionDataByFileType(originalFile.getFileType());
        return mainData != null ? mainData : ourGenericCompletionData;
    }

    /** @see CompletionDataEP */
    @Deprecated
    public static void registerCompletionData(FileType fileType, NotNullLazyValue<CompletionData> completionData) {
        ourCustomCompletionDatas.put(fileType, completionData);
    }

    /** @see CompletionDataEP */
    @Deprecated
    public static void registerCompletionData(FileType fileType, final CompletionData completionData) {
        registerCompletionData(fileType, new NotNullLazyValue<CompletionData>() {
            @Override
            
            protected CompletionData compute() {
                return completionData;
            }
        });
    }

    
    public static CompletionData getCompletionDataByFileType(FileType fileType) {
        for(CompletionDataEP ep: Extensions.getExtensions(CompletionDataEP.EP_NAME)) {
            if (ep.fileType.equals(fileType.getName())) {
                return ep.getHandler();
            }
        }
        final NotNullLazyValue<CompletionData> lazyValue = ourCustomCompletionDatas.get(fileType);
        return lazyValue == null ? null : lazyValue.getValue();
    }


    public static boolean shouldShowFeature(final CompletionParameters parameters,  final String id) {
        if (FeatureUsageTracker.getInstance().isToBeAdvertisedInLookup(id, parameters.getPosition().getProject())) {
            FeatureUsageTracker.getInstance().triggerFeatureShown(id);
            return true;
        }
        return false;
    }

    public static String findJavaIdentifierPrefix(CompletionParameters parameters) {
        return findJavaIdentifierPrefix(parameters.getPosition(), parameters.getOffset());
    }

    public static String findJavaIdentifierPrefix(final PsiElement insertedElement, final int offset) {
        return findIdentifierPrefix(insertedElement, offset, character().javaIdentifierPart(), character().javaIdentifierStart());
    }

    public static String findReferenceOrAlphanumericPrefix(CompletionParameters parameters) {
        String prefix = findReferencePrefix(parameters);
        return prefix == null ? findAlphanumericPrefix(parameters) : prefix;
    }

    public static String findAlphanumericPrefix(CompletionParameters parameters) {
        return findIdentifierPrefix(parameters.getPosition().getContainingFile(), parameters.getOffset(), character().letterOrDigit(), character().letterOrDigit());
    }

    public static String findIdentifierPrefix(PsiElement insertedElement, int offset, ElementPattern<Character> idPart,
                                              ElementPattern<Character> idStart) {
        if(insertedElement == null) return "";
        final String text = insertedElement.getText();

        final int offsetInElement = offset - insertedElement.getTextRange().getStartOffset();
        int start = offsetInElement - 1;
        while (start >=0 ) {
            if (!idPart.accepts(text.charAt(start))) break;
            --start;
        }
        while (start + 1 < offsetInElement && !idStart.accepts(text.charAt(start + 1))) {
            start++;
        }

        return text.substring(start + 1, offsetInElement).trim();
    }

    
    public static String findReferencePrefix(CompletionParameters parameters) {
        return CompletionData.getReferencePrefix(parameters.getPosition(), parameters.getOffset());
    }


    static InsertionContext emulateInsertion(InsertionContext oldContext, int newStart, final LookupElement item) {
        final InsertionContext newContext = newContext(oldContext, item);
        emulateInsertion(item, newStart, newContext);
        return newContext;
    }

    private static InsertionContext newContext(InsertionContext oldContext, LookupElement forElement) {
        final Editor editor = oldContext.getEditor();
        return new InsertionContext(new OffsetMap(editor.getDocument()), Lookup.AUTO_INSERT_SELECT_CHAR, new LookupElement[]{forElement}, oldContext.getFile(), editor,
                oldContext.shouldAddCompletionChar());
    }

    public static InsertionContext newContext(InsertionContext oldContext, LookupElement forElement, int startOffset, int tailOffset) {
        final InsertionContext context = newContext(oldContext, forElement);
        setOffsets(context, startOffset, tailOffset);
        return context;
    }

    public static void emulateInsertion(LookupElement item, int offset, InsertionContext context) {
        setOffsets(context, offset, offset);

        final Editor editor = context.getEditor();
        final Document document = editor.getDocument();
        final String lookupString = item.getLookupString();

        document.insertString(offset, lookupString);
        editor.getCaretModel().moveToOffset(context.getTailOffset());
        PsiDocumentManager.getInstance(context.getProject()).commitDocument(document);
        item.handleInsert(context);
        PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document);
    }

    private static void setOffsets(InsertionContext context, int offset, final int tailOffset) {
        final OffsetMap offsetMap = context.getOffsetMap();
        offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, offset);
        offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, tailOffset);
        offsetMap.addOffset(CompletionInitializationContext.SELECTION_END_OFFSET, tailOffset);
        context.setTailOffset(tailOffset);
    }

    
    public static PsiElement getTargetElement(LookupElement lookupElement) {
        PsiElement psiElement = lookupElement.getPsiElement();
        if (psiElement != null) {
            return getOriginalElement(psiElement);
        }

        Object object = lookupElement.getObject();
        if (object instanceof LookupValueWithPsiElement) {
            final PsiElement element = ((LookupValueWithPsiElement)object).getElement();
            if (element != null) return getOriginalElement(element);
        }

        return null;
    }

    
    public static <T extends PsiElement> T getOriginalElement( T psi) {
        return CompletionUtilCoreImpl.getOriginalElement(psi);
    }

    
    public static <T extends PsiElement> T getOriginalOrSelf( T psi) {
        final T element = getOriginalElement(psi);
        return element == null ? psi : element;
    }

    public static LinkedHashSet<String> sortMatching(final PrefixMatcher matcher, Collection<String> _names) {
        ProgressManager.checkCanceled();

        List<String> sorted = new ArrayList<String>();
        for (String name : _names) {
            if (matcher.prefixMatches(name)) {
                sorted.add(name);
            }
        }

        ProgressManager.checkCanceled();
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        ProgressManager.checkCanceled();

        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String name : sorted) {
            if (matcher.isStartMatch(name)) {
                result.add(name);
            }
        }

        ProgressManager.checkCanceled();

        result.addAll(sorted);
        return result;
    }
}
