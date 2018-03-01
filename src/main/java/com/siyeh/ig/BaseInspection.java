/*
 * Copyright 2003-2014 Dave Griffith, Bas Leijdekkers
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
package com.siyeh.ig;

import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInspection.BaseJavaBatchLocalInspectionTool;
import com.gome.maven.codeInspection.InspectionProfileEntry;
import com.gome.maven.codeInspection.LocalInspectionToolSession;
import com.gome.maven.codeInspection.ProblemsHolder;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiElementVisitor;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.ui.DocumentAdapter;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.ui.UIUtil;
import com.siyeh.ig.telemetry.InspectionGadgetsTelemetry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

public abstract class BaseInspection extends BaseJavaBatchLocalInspectionTool {
    private static final Logger LOG = Logger.getInstance("#com.siyeh.ig.BaseInspection");

    private String m_shortName = null;
    private long timestamp = -1L;

    @Override
    
    public String getShortName() {
        if (m_shortName == null) {
            final Class<? extends BaseInspection> aClass = getClass();
            final String name = aClass.getSimpleName();
            m_shortName = InspectionProfileEntry.getShortName(name);
            if (m_shortName.equals(name)) {
                throw new AssertionError("class name must end with 'Inspection' to correctly calculate the short name: " + name);
            }
        }
        return m_shortName;
    }

    
    
    @Override
    public abstract String getDisplayName();

    @Override
    
    
    public final String getGroupDisplayName() {
        return GroupDisplayNameUtil.getGroupDisplayName(getClass());
    }

    
    protected abstract String buildErrorString(Object... infos);

    protected boolean buildQuickFixesOnlyForOnTheFlyErrors() {
        return false;
    }

    
    protected InspectionGadgetsFix buildFix(Object... infos) {
        return null;
    }

    
    protected InspectionGadgetsFix[] buildFixes(Object... infos) {
        return InspectionGadgetsFix.EMPTY_ARRAY;
    }

    public abstract BaseInspectionVisitor buildVisitor();

    @Override
    
    public final PsiElementVisitor buildVisitor( ProblemsHolder holder,
                                                boolean isOnTheFly) {
        if (!shouldInspect(holder.getFile())) {
            return new PsiElementVisitor() { };
        }
        final BaseInspectionVisitor visitor = buildVisitor();
        visitor.setProblemsHolder(holder);
        visitor.setOnTheFly(isOnTheFly);
        visitor.setInspection(this);
        return visitor;
    }

    /**
     * To check precondition(s) on the entire file, to prevent doing the check on every PsiElement visited.
     * Useful for e.g. a {@link com.gome.maven.psi.util.PsiUtil#isLanguageLevel5OrHigher(com.gome.maven.psi.PsiElement)} check
     * which will be the same for all elements in the specified file.
     * When this method returns false, {@link #buildVisitor()} will not be called.
     */
    public boolean shouldInspect(PsiFile file) {
        return true;
    }

    protected JFormattedTextField prepareNumberEditor( final String fieldName) {
        final NumberFormat formatter = NumberFormat.getIntegerInstance();
        formatter.setParseIntegerOnly(true);
        final JFormattedTextField valueField = new JFormattedTextField(formatter);
        Object value = ReflectionUtil.getField(getClass(), this, null, fieldName);
        valueField.setValue(value);
        valueField.setColumns(2);

        // hack to work around text field becoming unusably small sometimes when using GridBagLayout
        valueField.setMinimumSize(valueField.getPreferredSize());

        UIUtil.fixFormattedField(valueField);
        final Document document = valueField.getDocument();
        document.addDocumentListener(new DocumentAdapter() {
            @Override
            public void textChanged(DocumentEvent evt) {
                try {
                    valueField.commitEdit();
                    final Number number = (Number)valueField.getValue();
                    ReflectionUtil.setField(BaseInspection.this.getClass(), BaseInspection.this, int.class, fieldName, number.intValue());
                }
                catch (ParseException e) {
                    // No luck this time. Will update the field when correct value is entered.
                }
            }
        });
        return valueField;
    }

    protected static void parseString(String string, List<String>... outs) {
        final List<String> strings = StringUtil.split(string, ",");
        for (List<String> out : outs) {
            out.clear();
        }
        final int iMax = strings.size();
        for (int i = 0; i < iMax; i += outs.length) {
            for (int j = 0; j < outs.length; j++) {
                final List<String> out = outs[j];
                if (i + j >= iMax) {
                    out.add("");
                }
                else {
                    out.add(strings.get(i + j));
                }
            }
        }
    }

    protected static String formatString(List<String>... strings) {
        final StringBuilder buffer = new StringBuilder();
        final int size = strings[0].size();
        if (size > 0) {
            formatString(strings, 0, buffer);
            for (int i = 1; i < size; i++) {
                buffer.append(',');
                formatString(strings, i, buffer);
            }
        }
        return buffer.toString();
    }

    private static void formatString(List<String>[] strings, int index,
                                     StringBuilder out) {
        out.append(strings[0].get(index));
        for (int i = 1; i < strings.length; i++) {
            out.append(',');
            out.append(strings[i].get(index));
        }
    }

    @Override
    public void inspectionStarted( LocalInspectionToolSession session, boolean isOnTheFly) {
        super.inspectionStarted(session, isOnTheFly);
        if (InspectionGadgetsTelemetry.isEnabled()) {
            timestamp = System.currentTimeMillis();
        }
    }

    @Override
    public void inspectionFinished( LocalInspectionToolSession session,
                                    ProblemsHolder problemsHolder) {
        super.inspectionFinished(session, problemsHolder);
        if (InspectionGadgetsTelemetry.isEnabled()) {
            if (timestamp < 0L) {
                LOG.warn("finish reported without corresponding start");
                return;
            }
            final long end = System.currentTimeMillis();
            final String displayName = getDisplayName();
            InspectionGadgetsTelemetry.getInstance().reportRun(displayName, end - timestamp);
            timestamp = -1L;
        }
    }

    public static boolean isInspectionEnabled( String shortName, PsiElement context) {
        final InspectionProjectProfileManager profileManager = InspectionProjectProfileManager.getInstance(context.getProject());
        final InspectionProfileImpl profile = (InspectionProfileImpl)profileManager.getInspectionProfile();
        final HighlightDisplayKey key = HighlightDisplayKey.find(shortName);
        return profile.isToolEnabled(key, context);
    }
}