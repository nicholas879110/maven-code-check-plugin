/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeInsight.daemon.GutterMark;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInsight.intention.IntentionAction;
import com.gome.maven.codeInsight.intention.IntentionManager;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.ex.GlobalInspectionToolWrapper;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.ex.LocalInspectionToolWrapper;
import com.gome.maven.lang.ASTNode;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.lang.annotation.ProblemGroup;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.editor.colors.*;
import com.gome.maven.openapi.editor.ex.RangeHighlighterEx;
import com.gome.maven.openapi.editor.markup.GutterIconRenderer;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.util.*;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.ArrayUtilRt;
import com.gome.maven.util.BitUtil;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.xml.util.XmlStringUtil;
//import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class HighlightInfo implements Segment {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.HighlightInfo");

    public static final HighlightInfo[] EMPTY_ARRAY = new HighlightInfo[0];
    // optimisation: if tooltip contains this marker object, then it replaced with description field in getTooltip()
    private static final String DESCRIPTION_PLACEHOLDER = "\u0000";
    JComponent fileLevelComponent;
    public final TextAttributes forcedTextAttributes;
    public final TextAttributesKey forcedTextAttributesKey;

    
    public final HighlightInfoType type;
    private int group;
    public final int startOffset;
    public final int endOffset;

    private int fixStartOffset;
    private int fixEndOffset;
    RangeMarker fixMarker; // null means it the same as highlighter

    private final String description;
    private final String toolTip;
    
    private final HighlightSeverity severity;

    final int navigationShift;

    volatile RangeHighlighterEx highlighter; // modified in EDT only

    public List<Pair<IntentionActionDescriptor, TextRange>> quickFixActionRanges;
    public List<Pair<IntentionActionDescriptor, RangeMarker>> quickFixActionMarkers;

    private final GutterMark gutterIconRenderer;
    private final ProblemGroup myProblemGroup;

    private volatile byte myFlags; // bit packed flags below:
    private static final byte BIJECTIVE_MASK = 1;
    private static final byte HAS_HINT_MASK = 2;
    private static final byte FROM_INJECTION_MASK = 4;
    private static final byte AFTER_END_OF_LINE_MASK = 8;
    private static final byte FILE_LEVEL_ANNOTATION_MASK = 16;
    private static final byte NEEDS_UPDATE_ON_TYPING_MASK = 32;
    PsiElement psiElement;

    
    ProperTextRange getFixTextRange() {
        return new ProperTextRange(fixStartOffset, fixEndOffset);
    }

    void setFromInjection(boolean fromInjection) {
        setFlag(FROM_INJECTION_MASK, fromInjection);
    }

    public String getToolTip() {
        String toolTip = this.toolTip;
        String description = this.description;
        if (toolTip == null || description == null || !toolTip.contains(DESCRIPTION_PLACEHOLDER)) return toolTip;
        String decoded = StringUtil.replace(toolTip, DESCRIPTION_PLACEHOLDER, XmlStringUtil.escapeString(description));
        return XmlStringUtil.wrapInHtml(decoded);
    }

    private static String encodeTooltip(String toolTip, String description) {
        if (toolTip == null || description == null) return toolTip;
        String unescaped = StringUtil.unescapeXml(XmlStringUtil.stripHtml(toolTip));

        String encoded = description.isEmpty() ? unescaped : StringUtil.replace(unescaped, description, DESCRIPTION_PLACEHOLDER);
        //noinspection StringEquality
        if (encoded == unescaped) {
            return toolTip;
        }
        if (encoded.equals(DESCRIPTION_PLACEHOLDER)) encoded = DESCRIPTION_PLACEHOLDER;
        return encoded;
    }

    public String getDescription() {
        return description;
    }

//    @MagicConstant(intValues = {BIJECTIVE_MASK, HAS_HINT_MASK, FROM_INJECTION_MASK, AFTER_END_OF_LINE_MASK, FILE_LEVEL_ANNOTATION_MASK,
//            NEEDS_UPDATE_ON_TYPING_MASK})
    private @interface FlagConstant {}

    private boolean isFlagSet(@FlagConstant byte mask) {
        return BitUtil.isSet(myFlags, mask);
    }

    private void setFlag(@FlagConstant byte mask, boolean value) {
        myFlags = BitUtil.set(myFlags, mask, value);
    }

    boolean isFileLevelAnnotation() {
        return isFlagSet(FILE_LEVEL_ANNOTATION_MASK);
    }

    boolean isBijective() {
        return isFlagSet(BIJECTIVE_MASK);
    }

    void setBijective(boolean bijective) {
        setFlag(BIJECTIVE_MASK, bijective);
    }

    
    public HighlightSeverity getSeverity() {
        return severity;
    }

    public boolean isAfterEndOfLine() {
        return isFlagSet(AFTER_END_OF_LINE_MASK);
    }

    
    public TextAttributes getTextAttributes( final PsiElement element,  final EditorColorsScheme editorColorsScheme) {
        if (forcedTextAttributes != null) {
            return forcedTextAttributes;
        }

        EditorColorsScheme colorsScheme = getColorsScheme(editorColorsScheme);

        if (forcedTextAttributesKey != null) {
            return colorsScheme.getAttributes(forcedTextAttributesKey);
        }

        return getAttributesByType(element, type, colorsScheme);
    }

    public static TextAttributes getAttributesByType( final PsiElement element,
                                                      HighlightInfoType type,
                                                      TextAttributesScheme colorsScheme) {
        final SeverityRegistrar severityRegistrar = SeverityRegistrar
                .getSeverityRegistrar(element != null ? element.getProject() : null);
        final TextAttributes textAttributes = severityRegistrar.getTextAttributesBySeverity(type.getSeverity(element));
        if (textAttributes != null) {
            return textAttributes;
        }
        TextAttributesKey key = type.getAttributesKey();
        return colorsScheme.getAttributes(key);
    }

    
    public Color getErrorStripeMarkColor( PsiElement element,
                                          final EditorColorsScheme colorsScheme) { // if null global scheme will be used
        if (forcedTextAttributes != null) {
            return forcedTextAttributes.getErrorStripeColor();
        }
        EditorColorsScheme scheme = getColorsScheme(colorsScheme);
        if (forcedTextAttributesKey != null) {
            TextAttributes forcedTextAttributes = scheme.getAttributes(forcedTextAttributesKey);
            if (forcedTextAttributes != null) {
                final Color errorStripeColor = forcedTextAttributes.getErrorStripeColor();
                // let's copy above behaviour of forcedTextAttributes stripe color, but I'm not sure that the behaviour is correct in general
                if (errorStripeColor != null) {
                    return errorStripeColor;
                }
            }
        }

        if (getSeverity() == HighlightSeverity.ERROR) {
            return scheme.getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.WARNING) {
            return scheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.INFO){
            return scheme.getAttributes(CodeInsightColors.INFO_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.WEAK_WARNING){
            return scheme.getAttributes(CodeInsightColors.WEAK_WARNING_ATTRIBUTES).getErrorStripeColor();
        }
        if (getSeverity() == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) {
            return scheme.getAttributes(CodeInsightColors.GENERIC_SERVER_ERROR_OR_WARNING).getErrorStripeColor();
        }

        TextAttributes attributes = getAttributesByType(element, type, scheme);
        return attributes == null ? null : attributes.getErrorStripeColor();

    }

    
    private static EditorColorsScheme getColorsScheme( final EditorColorsScheme customScheme) {
        if (customScheme != null) {
            return customScheme;
        }
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    
    
    private static String htmlEscapeToolTip( String unescapedTooltip) {
        return unescapedTooltip == null ? null : XmlStringUtil.wrapInHtml(XmlStringUtil.escapeString(unescapedTooltip));
    }

    
    private static final HighlightInfoFilter[] FILTERS = HighlightInfoFilter.EXTENSION_POINT_NAME.getExtensions();

    public boolean needUpdateOnTyping() {
        return isFlagSet(NEEDS_UPDATE_ON_TYPING_MASK);
    }

    HighlightInfo( TextAttributes forcedTextAttributes,
                   TextAttributesKey forcedTextAttributesKey,
                   HighlightInfoType type,
                  int startOffset,
                  int endOffset,
                   String escapedDescription,
                   String escapedToolTip,
                   HighlightSeverity severity,
                  boolean afterEndOfLine,
                   Boolean needsUpdateOnTyping,
                  boolean isFileLevelAnnotation,
                  int navigationShift,
                  ProblemGroup problemGroup,
                  GutterMark gutterIconRenderer) {
        if (startOffset < 0 || startOffset > endOffset) {
            LOG.error("Incorrect highlightInfo bounds. description="+escapedDescription+"; startOffset="+startOffset+"; endOffset="+endOffset+";type="+type);
        }
        this.forcedTextAttributes = forcedTextAttributes;
        this.forcedTextAttributesKey = forcedTextAttributesKey;
        this.type = type;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        fixStartOffset = startOffset;
        fixEndOffset = endOffset;
        description = escapedDescription;
        // optimisation: do not retain extra memory if can recompute
        toolTip = encodeTooltip(escapedToolTip, escapedDescription);
        this.severity = severity;
        setFlag(AFTER_END_OF_LINE_MASK, afterEndOfLine);
        setFlag(NEEDS_UPDATE_ON_TYPING_MASK, calcNeedUpdateOnTyping(needsUpdateOnTyping, type));
        setFlag(FILE_LEVEL_ANNOTATION_MASK, isFileLevelAnnotation);
        this.navigationShift = navigationShift;
        myProblemGroup = problemGroup;
        this.gutterIconRenderer = gutterIconRenderer;
    }

    private static boolean calcNeedUpdateOnTyping( Boolean needsUpdateOnTyping, HighlightInfoType type) {
        if (needsUpdateOnTyping != null) return needsUpdateOnTyping.booleanValue();

        if (type == HighlightInfoType.TODO) return false;
        if (type == HighlightInfoType.LOCAL_VARIABLE) return false;
        if (type == HighlightInfoType.INSTANCE_FIELD) return false;
        if (type == HighlightInfoType.STATIC_FIELD) return false;
        if (type == HighlightInfoType.STATIC_FINAL_FIELD) return false;
        if (type == HighlightInfoType.PARAMETER) return false;
        if (type == HighlightInfoType.METHOD_CALL) return false;
        if (type == HighlightInfoType.METHOD_DECLARATION) return false;
        if (type == HighlightInfoType.STATIC_METHOD) return false;
        if (type == HighlightInfoType.ABSTRACT_METHOD) return false;
        if (type == HighlightInfoType.INHERITED_METHOD) return false;
        if (type == HighlightInfoType.CONSTRUCTOR_CALL) return false;
        if (type == HighlightInfoType.CONSTRUCTOR_DECLARATION) return false;
        if (type == HighlightInfoType.INTERFACE_NAME) return false;
        if (type == HighlightInfoType.ABSTRACT_CLASS_NAME) return false;
        if (type == HighlightInfoType.ENUM_NAME) return false;
        if (type == HighlightInfoType.CLASS_NAME) return false;
        if (type == HighlightInfoType.ANONYMOUS_CLASS_NAME) return false;
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof HighlightInfo)) return false;
        HighlightInfo info = (HighlightInfo)obj;

        return info.getSeverity() == getSeverity() &&
                info.startOffset == startOffset &&
                info.endOffset == endOffset &&
                Comparing.equal(info.type, type) &&
                Comparing.equal(info.gutterIconRenderer, gutterIconRenderer) &&
                Comparing.equal(info.forcedTextAttributes, forcedTextAttributes) &&
                Comparing.equal(info.forcedTextAttributesKey, forcedTextAttributesKey) &&
                Comparing.strEqual(info.getDescription(), getDescription());
    }

    public boolean equalsByActualOffset( HighlightInfo info) {
        if (info == this) return true;

        return info.getSeverity() == getSeverity() &&
                info.getActualStartOffset() == getActualStartOffset() &&
                info.getActualEndOffset() == getActualEndOffset() &&
                Comparing.equal(info.type, type) &&
                Comparing.equal(info.gutterIconRenderer, gutterIconRenderer) &&
                Comparing.equal(info.forcedTextAttributes, forcedTextAttributes) &&
                Comparing.equal(info.forcedTextAttributesKey, forcedTextAttributesKey) &&
                Comparing.strEqual(info.getDescription(), getDescription());
    }

    @Override
    public int hashCode() {
        return startOffset;
    }

    @Override
    
    public String toString() {
        return getDescription() != null ? getDescription() : "";
    }

    public String paramString() {
         String s = "HighlightInfo(" + startOffset + "," + endOffset+")";
        if (getActualStartOffset() != startOffset || getActualEndOffset() != endOffset) {
            s += "; actual: (" + getActualStartOffset() + "," + getActualEndOffset() + ")";
        }
        if (highlighter != null) s += " text='" + getText() + "'";
        if (getDescription() != null) s+= ", description='" + getDescription() + "'";
        s += " severity=" + getSeverity();
        s += " group=" + getGroup();

        if (quickFixActionRanges != null) {
            s+= "; quickFixes: "+quickFixActionRanges;
        }
        if (gutterIconRenderer != null) {
            s += "; gutter: " + gutterIconRenderer;
        }
        return s;
    }

    
    public static Builder newHighlightInfo( HighlightInfoType type) {
        return new B(type);
    }

    void setGroup(int group) {
        this.group = group;
    }

    public interface Builder {
        // only one 'range' call allowed
         Builder range( TextRange textRange);
         Builder range( ASTNode node);
         Builder range( PsiElement element);
         Builder range( PsiElement element, int start, int end);
         Builder range(int start, int end);

         Builder gutterIconRenderer( GutterIconRenderer gutterIconRenderer);
         Builder problemGroup( ProblemGroup problemGroup);

        // only one allowed
         Builder description( String description);
         Builder descriptionAndTooltip( String description);

        // only one allowed
         Builder textAttributes( TextAttributes attributes);
         Builder textAttributes( TextAttributesKey attributesKey);

        // only one allowed
         Builder unescapedToolTip( String unescapedToolTip);
         Builder escapedToolTip( String escapedToolTip);

         Builder endOfLine();
         Builder needsUpdateOnTyping(boolean update);
         Builder severity( HighlightSeverity severity);
         Builder fileLevelAnnotation();
         Builder navigationShift(int navigationShift);

        HighlightInfo create();

        
        HighlightInfo createUnconditionally();
    }

    private static boolean isAcceptedByFilters( HighlightInfo info,  PsiElement psiElement) {
        PsiFile file = psiElement == null ? null : psiElement.getContainingFile();
        for (HighlightInfoFilter filter : FILTERS) {
            if (!filter.accept(info, file)) {
                return false;
            }
        }
        info.psiElement = psiElement;
        return true;
    }

    private static class B implements Builder {
        private Boolean myNeedsUpdateOnTyping;
        private TextAttributes forcedTextAttributes;
        private TextAttributesKey forcedTextAttributesKey;

        private final HighlightInfoType type;
        private int startOffset = -1;
        private int endOffset = -1;

        private String escapedDescription;
        private String escapedToolTip;
        private HighlightSeverity severity;

        private boolean isAfterEndOfLine;
        private boolean isFileLevelAnnotation;
        private int navigationShift = 0;

        private GutterIconRenderer gutterIconRenderer;
        private ProblemGroup problemGroup;
        private PsiElement psiElement;

        public B( HighlightInfoType type) {
            this.type = type;
        }

        
        @Override
        public Builder gutterIconRenderer( GutterIconRenderer gutterIconRenderer) {
            assert this.gutterIconRenderer == null : "gutterIconRenderer already set";
            this.gutterIconRenderer = gutterIconRenderer;
            return this;
        }

        
        @Override
        public Builder problemGroup( ProblemGroup problemGroup) {
            assert this.problemGroup == null : "problemGroup already set";
            this.problemGroup = problemGroup;
            return this;
        }

        
        @Override
        public Builder description( String description) {
            assert escapedDescription == null : "description already set";
            escapedDescription = description;
            return this;
        }

        
        @Override
        public Builder descriptionAndTooltip( String description) {
            return description(description).unescapedToolTip(description);
        }

        
        @Override
        public Builder textAttributes( TextAttributes attributes) {
            assert forcedTextAttributes == null : "textattributes already set";
            forcedTextAttributes = attributes;
            return this;
        }

        
        @Override
        public Builder textAttributes( TextAttributesKey attributesKey) {
            assert forcedTextAttributesKey == null : "textattributesKey already set";
            forcedTextAttributesKey = attributesKey;
            return this;
        }

        
        @Override
        public Builder unescapedToolTip( String unescapedToolTip) {
            assert escapedToolTip == null : "Tooltip was already set";
            escapedToolTip = htmlEscapeToolTip(unescapedToolTip);
            return this;
        }

        
        @Override
        public Builder escapedToolTip( String escapedToolTip) {
            assert this.escapedToolTip == null : "Tooltip was already set";
            this.escapedToolTip = escapedToolTip;
            return this;
        }

        
        @Override
        public Builder range(int start, int end) {
            assert startOffset == -1 && endOffset == -1 : "Offsets already set";

            startOffset = start;
            endOffset = end;
            return this;
        }

        
        @Override
        public Builder range( TextRange textRange) {
            assert startOffset == -1 && endOffset == -1 : "Offsets already set";
            startOffset = textRange.getStartOffset();
            endOffset = textRange.getEndOffset();
            return this;
        }

        
        @Override
        public Builder range( ASTNode node) {
            return range(node.getPsi());
        }

        
        @Override
        public Builder range( PsiElement element) {
            assert psiElement == null : " psiElement already set";
            psiElement = element;
            return range(element.getTextRange());
        }

        
        @Override
        public Builder range( PsiElement element, int start, int end) {
            assert psiElement == null : " psiElement already set";
            psiElement = element;
            return range(start, end);
        }

        
        @Override
        public Builder endOfLine() {
            isAfterEndOfLine = true;
            return this;
        }

        
        @Override
        public Builder needsUpdateOnTyping(boolean update) {
            assert myNeedsUpdateOnTyping == null : " needsUpdateOnTyping already set";
            myNeedsUpdateOnTyping = update;
            return this;
        }

        
        @Override
        public Builder severity( HighlightSeverity severity) {
            assert this.severity == null : " severity already set";
            this.severity = severity;
            return this;
        }

        
        @Override
        public Builder fileLevelAnnotation() {
            isFileLevelAnnotation = true;
            return this;
        }

        
        @Override
        public Builder navigationShift(int navigationShift) {
            this.navigationShift = navigationShift;
            return this;
        }

        
        @Override
        public HighlightInfo create() {
            HighlightInfo info = createUnconditionally();
            LOG.assertTrue(psiElement != null || severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY || severity == HighlightInfoType.INJECTED_FRAGMENT_SEVERITY || ArrayUtilRt.find(HighlightSeverity.DEFAULT_SEVERITIES, severity) != -1,
                    "Custom type requires not-null element to detect its text attributes");

            if (!isAcceptedByFilters(info, psiElement)) return null;

            return info;
        }

        
        @Override
        public HighlightInfo createUnconditionally() {
            if (severity == null) {
                severity = type.getSeverity(psiElement);
            }

            return new HighlightInfo(forcedTextAttributes, forcedTextAttributesKey, type, startOffset, endOffset, escapedDescription,
                    escapedToolTip, severity, isAfterEndOfLine, myNeedsUpdateOnTyping, isFileLevelAnnotation, navigationShift,
                    problemGroup, gutterIconRenderer);
        }
    }

    public GutterMark getGutterIconRenderer() {
        return gutterIconRenderer;
    }

    
    public ProblemGroup getProblemGroup() {
        return myProblemGroup;
    }

    
    public static HighlightInfo fromAnnotation( Annotation annotation) {
        return fromAnnotation(annotation, null, false);
    }

    
    public static HighlightInfo fromAnnotation( Annotation annotation,  TextRange fixedRange, boolean batchMode) {
        final TextAttributes forcedAttributes = annotation.getEnforcedTextAttributes();
        final TextAttributesKey forcedAttributesKey = forcedAttributes == null ? annotation.getTextAttributes() : null;

        HighlightInfo info = new HighlightInfo(forcedAttributes, forcedAttributesKey, convertType(annotation),
                fixedRange != null? fixedRange.getStartOffset() : annotation.getStartOffset(),
                fixedRange != null? fixedRange.getEndOffset() : annotation.getEndOffset(),
                annotation.getMessage(), annotation.getTooltip(),
                annotation.getSeverity(), annotation.isAfterEndOfLine(), annotation.needsUpdateOnTyping(), annotation.isFileLevelAnnotation(),
                0, annotation.getProblemGroup(), annotation.getGutterIconRenderer());
        appendFixes(fixedRange, info, batchMode ? annotation.getBatchFixes() : annotation.getQuickFixes());
        return info;
    }

    public static final String ANNOTATOR_INSPECTION_SHORT_NAME = "Annotator";

    private static void appendFixes( TextRange fixedRange,  HighlightInfo info,  List<Annotation.QuickFixInfo> fixes) {
        if (fixes != null) {
            for (final Annotation.QuickFixInfo quickFixInfo : fixes) {
                TextRange range = fixedRange != null ? fixedRange : quickFixInfo.textRange;
                HighlightDisplayKey key = quickFixInfo.key != null
                        ? quickFixInfo.key
                        : HighlightDisplayKey.find(ANNOTATOR_INSPECTION_SHORT_NAME);
                info.registerFix(quickFixInfo.quickFix, null, HighlightDisplayKey.getDisplayNameByKey(key), range, key);
            }
        }
    }

    
    public static HighlightInfoType convertType( Annotation annotation) {
        ProblemHighlightType type = annotation.getHighlightType();
        if (type == ProblemHighlightType.LIKE_UNUSED_SYMBOL) return HighlightInfoType.UNUSED_SYMBOL;
        if (type == ProblemHighlightType.LIKE_UNKNOWN_SYMBOL) return HighlightInfoType.WRONG_REF;
        if (type == ProblemHighlightType.LIKE_DEPRECATED) return HighlightInfoType.DEPRECATED;
        return convertSeverity(annotation.getSeverity());
    }

    
    public static HighlightInfoType convertSeverity( HighlightSeverity severity) {
        return severity == HighlightSeverity.ERROR? HighlightInfoType.ERROR :
                severity == HighlightSeverity.WARNING ? HighlightInfoType.WARNING :
                        severity == HighlightSeverity.INFO ? HighlightInfoType.INFO :
                                severity == HighlightSeverity.WEAK_WARNING ? HighlightInfoType.WEAK_WARNING :
                                        severity ==HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING ? HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER :
                                                HighlightInfoType.INFORMATION;
    }

    
    public static ProblemHighlightType convertType(HighlightInfoType infoType) {
        if (infoType == HighlightInfoType.ERROR || infoType == HighlightInfoType.WRONG_REF) return ProblemHighlightType.ERROR;
        if (infoType == HighlightInfoType.WARNING) return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
        if (infoType == HighlightInfoType.INFORMATION) return ProblemHighlightType.INFORMATION;
        return ProblemHighlightType.WEAK_WARNING;
    }

    
    public static ProblemHighlightType convertSeverityToProblemHighlight(HighlightSeverity severity) {
        return severity == HighlightSeverity.ERROR ? ProblemHighlightType.ERROR :
                severity == HighlightSeverity.WARNING ? ProblemHighlightType.GENERIC_ERROR_OR_WARNING :
                        severity == HighlightSeverity.INFO ? ProblemHighlightType.INFO :
                                severity == HighlightSeverity.WEAK_WARNING ? ProblemHighlightType.WEAK_WARNING : ProblemHighlightType.INFORMATION;
    }


    public boolean hasHint() {
        return isFlagSet(HAS_HINT_MASK);
    }

    void setHint(final boolean hasHint) {
        setFlag(HAS_HINT_MASK, hasHint);
    }

    public int getActualStartOffset() {
        RangeHighlighterEx h = highlighter;
        return h == null || !h.isValid() ? startOffset : h.getStartOffset();
    }
    public int getActualEndOffset() {
        RangeHighlighterEx h = highlighter;
        return h == null || !h.isValid() ? endOffset : h.getEndOffset();
    }

    public static class IntentionActionDescriptor {
        private final IntentionAction myAction;
        private volatile List<IntentionAction> myOptions;
        private volatile HighlightDisplayKey myKey;
        private final ProblemGroup myProblemGroup;
        private final HighlightSeverity mySeverity;
        private final String myDisplayName;
        private final Icon myIcon;
        private Boolean myCanCleanup;

        public IntentionActionDescriptor( IntentionAction action, final List<IntentionAction> options, final String displayName) {
            this(action, options, displayName, null);
        }

        public IntentionActionDescriptor( IntentionAction action, final Icon icon) {
            this(action, null, null, icon);
        }

        public IntentionActionDescriptor( IntentionAction action,
                                          final List<IntentionAction> options,
                                          final String displayName,
                                          Icon icon) {
            this(action, options, displayName, icon, null, null, null);
        }

        public IntentionActionDescriptor( IntentionAction action,
                                          final List<IntentionAction> options,
                                          final String displayName,
                                          Icon icon,
                                          HighlightDisplayKey key,
                                          ProblemGroup problemGroup,
                                          HighlightSeverity severity) {
            myAction = action;
            myOptions = options;
            myDisplayName = displayName;
            myIcon = icon;
            myKey = key;
            myProblemGroup = problemGroup;
            mySeverity = severity;
        }

        
        public IntentionAction getAction() {
            return myAction;
        }

        public boolean notError() {
            return mySeverity != null && mySeverity.compareTo(HighlightSeverity.ERROR) < 0;
        }

        public boolean canCleanup( PsiElement element) {
            if (myCanCleanup == null) {
                InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getInspectionProfile();
                final HighlightDisplayKey key = myKey;
                if (key == null) {
                    myCanCleanup = false;
                } else {
                    InspectionToolWrapper toolWrapper = profile.getInspectionTool(key.toString(), element);
                    myCanCleanup = toolWrapper != null && toolWrapper.isCleanupTool();
                }
            }
            return myCanCleanup;
        }

        
        public List<IntentionAction> getOptions( PsiElement element,  Editor editor) {
            if (editor != null && Boolean.FALSE.equals(editor.getUserData(IntentionManager.SHOW_INTENTION_OPTIONS_KEY))) {
                return null;
            }
            List<IntentionAction> options = myOptions;
            HighlightDisplayKey key = myKey;
            if (myProblemGroup != null) {
                String problemName = myProblemGroup.getProblemName();
                HighlightDisplayKey problemGroupKey = problemName != null ? HighlightDisplayKey.findById(problemName) : null;
                if (problemGroupKey != null) {
                    key = problemGroupKey;
                }
            }
            if (options != null || key == null) {
                return options;
            }
            IntentionManager intentionManager = IntentionManager.getInstance();
            List<IntentionAction> newOptions = intentionManager.getStandardIntentionOptions(key, element);
            InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getInspectionProfile();
            InspectionToolWrapper toolWrapper = profile.getInspectionTool(key.toString(), element);
            if (!(toolWrapper instanceof LocalInspectionToolWrapper)) {
                HighlightDisplayKey idkey = HighlightDisplayKey.findById(key.toString());
                if (idkey != null) {
                    toolWrapper = profile.getInspectionTool(idkey.toString(), element);
                }
            }
            if (toolWrapper != null) {

                myCanCleanup = toolWrapper.isCleanupTool();

                final IntentionAction fixAllIntention = intentionManager.createFixAllIntention(toolWrapper, myAction);
                InspectionProfileEntry wrappedTool = toolWrapper instanceof LocalInspectionToolWrapper ? ((LocalInspectionToolWrapper)toolWrapper).getTool()
                        : ((GlobalInspectionToolWrapper)toolWrapper).getTool();
                if (wrappedTool instanceof DefaultHighlightVisitorBasedInspection.AnnotatorBasedInspection) {
                    List<IntentionAction> actions = Collections.<IntentionAction>emptyList();
                    if (myProblemGroup instanceof SuppressableProblemGroup) {
                        actions = Arrays.<IntentionAction>asList(((SuppressableProblemGroup)myProblemGroup).getSuppressActions(element));
                    }
                    if (fixAllIntention != null) {
                        if (actions.isEmpty()) {
                            return Collections.<IntentionAction>singletonList(fixAllIntention);
                        }
                        else {
                            actions = new ArrayList<IntentionAction>(actions);
                            actions.add(fixAllIntention);
                        }
                    }
                    return actions;
                }
                ContainerUtil.addIfNotNull(newOptions, fixAllIntention);
                if (wrappedTool instanceof CustomSuppressableInspectionTool) {
                    final IntentionAction[] suppressActions = ((CustomSuppressableInspectionTool)wrappedTool).getSuppressActions(element);
                    if (suppressActions != null) {
                        ContainerUtil.addAll(newOptions, suppressActions);
                    }
                } else {
                    SuppressQuickFix[] suppressFixes = wrappedTool.getBatchSuppressActions(element);
                    if (suppressFixes.length > 0) {
                        ContainerUtil.addAll(newOptions, ContainerUtil.map(suppressFixes, new Function<SuppressQuickFix, IntentionAction>() {
                            @Override
                            public IntentionAction fun(SuppressQuickFix fix) {
                                return SuppressIntentionActionFromFix.convertBatchToSuppressIntentionAction(fix);
                            }
                        }));
                    }
                }

            }
            if (myProblemGroup instanceof SuppressableProblemGroup) {
                final IntentionAction[] suppressActions = ((SuppressableProblemGroup)myProblemGroup).getSuppressActions(element);
                ContainerUtil.addAll(newOptions, suppressActions);
            }

            synchronized (this) {
                options = myOptions;
                if (options == null) {
                    myOptions = options = newOptions;
                }
                myKey = null;
            }
            return options;
        }

        
        public String getDisplayName() {
            return myDisplayName;
        }

        @Override
        
        public String toString() {
            String text = getAction().getText();
            return "descriptor: " + (text.isEmpty() ? getAction().getClass() : text);
        }

        
        public Icon getIcon() {
            return myIcon;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof IntentionActionDescriptor && myAction.equals(((IntentionActionDescriptor)obj).myAction);
        }
    }

    @Override
    public int getStartOffset() {
        return getActualStartOffset();
    }

    @Override
    public int getEndOffset() {
        return getActualEndOffset();
    }

    int getGroup() {
        return group;
    }

    boolean isFromInjection() {
        return isFlagSet(FROM_INJECTION_MASK);
    }

    
    public String getText() {
        RangeHighlighterEx highlighter = this.highlighter;
        if (highlighter == null) throw new RuntimeException("info not applied yet");
        if (!highlighter.isValid()) return "";
        return highlighter.getDocument().getText(TextRange.create(highlighter));
    }

    public void registerFix( IntentionAction action,
                             List<IntentionAction> options,
                             String displayName,
                             TextRange fixRange,
                             HighlightDisplayKey key) {
        if (action == null) return;
        if (fixRange == null) fixRange = new TextRange(startOffset, endOffset);
        if (quickFixActionRanges == null) {
            quickFixActionRanges = ContainerUtil.createLockFreeCopyOnWriteList();
        }
        IntentionActionDescriptor desc = new IntentionActionDescriptor(action, options, displayName, null, key, getProblemGroup(), getSeverity());
        quickFixActionRanges.add(Pair.create(desc, fixRange));
        fixStartOffset = Math.min (fixStartOffset, fixRange.getStartOffset());
        fixEndOffset = Math.max (fixEndOffset, fixRange.getEndOffset());
        if (action instanceof HintAction) {
            setHint(true);
        }
    }

    public void unregisterQuickFix( Condition<IntentionAction> condition) {
        for (Iterator<Pair<IntentionActionDescriptor, TextRange>> it = quickFixActionRanges.iterator(); it.hasNext();) {
            Pair<IntentionActionDescriptor, TextRange> pair = it.next();
            if (condition.value(pair.first.getAction())) {
                it.remove();
            }
        }
    }
}
