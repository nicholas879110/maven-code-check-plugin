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
 * @author max
 */
package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeInsight.daemon.DaemonBundle;
import com.gome.maven.codeInsight.daemon.impl.actions.ShowErrorDescriptionAction;
import com.gome.maven.codeInsight.hint.LineTooltipRenderer;
import com.gome.maven.codeInsight.hint.TooltipLinkHandlerEP;
import com.gome.maven.codeInsight.hint.TooltipRenderer;
import com.gome.maven.codeInspection.ui.DefaultInspectionToolPresentation;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.ex.ErrorStripTooltipRendererProvider;
import com.gome.maven.openapi.editor.impl.TrafficTooltipRenderer;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.Html;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.xml.util.XmlStringUtil;
import gnu.trove.THashSet;

import javax.swing.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class DaemonTooltipRendererProvider implements ErrorStripTooltipRendererProvider {
     private static final String END_MARKER = "<!-- end marker -->";
    private final Project myProject;

    public DaemonTooltipRendererProvider(final Project project) {
        myProject = project;
    }

    @Override
    public TooltipRenderer calcTooltipRenderer( final Collection<RangeHighlighter> highlighters) {
        LineTooltipRenderer bigRenderer = null;
        List<HighlightInfo> infos = new SmartList<HighlightInfo>();
        Collection<String> tooltips = new THashSet<String>(); //do not show same tooltip twice
        for (RangeHighlighter marker : highlighters) {
            final Object tooltipObject = marker.getErrorStripeTooltip();
            if (tooltipObject == null) continue;
            if (tooltipObject instanceof HighlightInfo) {
                HighlightInfo info = (HighlightInfo)tooltipObject;
                if (info.getToolTip() != null && tooltips.add(info.getToolTip())) {
                    infos.add(info);
                }
            }
            else {
                final String text = tooltipObject.toString();
                if (tooltips.add(text)) {
                    if (bigRenderer == null) {
                        bigRenderer = new MyRenderer(text, new Object[] {highlighters});
                    }
                    else {
                        bigRenderer.addBelow(text);
                    }
                }
            }
        }
        if (!infos.isEmpty()) {
            // show errors first
            ContainerUtil.quickSort(infos, new Comparator<HighlightInfo>() {
                @Override
                public int compare(final HighlightInfo o1, final HighlightInfo o2) {
                    int i = SeverityRegistrar.getSeverityRegistrar(myProject).compare(o2.getSeverity(), o1.getSeverity());
                    if (i != 0) return i;
                    return o1.getToolTip().compareTo(o2.getToolTip());
                }
            });
            final HighlightInfoComposite composite = new HighlightInfoComposite(infos);
            if (bigRenderer == null) {
                bigRenderer = new MyRenderer(UIUtil.convertSpace2Nbsp(composite.getToolTip()), new Object[] {highlighters});
            }
            else {
                final LineTooltipRenderer renderer = new MyRenderer(UIUtil.convertSpace2Nbsp(composite.getToolTip()), new Object[] {highlighters});
                renderer.addBelow(bigRenderer.getText());
                bigRenderer = renderer;
            }
        }
        return bigRenderer;
    }

    
    @Override
    public TooltipRenderer calcTooltipRenderer( final String text) {
        return new MyRenderer(text, new Object[] {text});
    }

    
    @Override
    public TooltipRenderer calcTooltipRenderer( final String text, final int width) {
        return new MyRenderer(text, width, new Object[] {text});
    }

    
    @Override
    public TrafficTooltipRenderer createTrafficTooltipRenderer( Runnable onHide,  Editor editor) {
        return new TrafficTooltipRendererImpl(onHide, editor);
    }

    private static class MyRenderer extends LineTooltipRenderer {
        public MyRenderer(final String text, Object[] comparable) {
            super(text, comparable);
        }

        public MyRenderer(final String text, final int width, Object[] comparable) {
            super(text, width, comparable);
        }

        @Override
        protected void onHide(final JComponent contentComponent) {
            ShowErrorDescriptionAction.rememberCurrentWidth(contentComponent.getWidth());
        }

        @Override
        protected boolean dressDescription( final Editor editor) {
            final List<String> problems = StringUtil.split(UIUtil.getHtmlBody(new Html(myText).setKeepFont(true)), UIUtil.BORDER_LINE);
            String text = "";
            for (String problem : problems) {
                final String ref = getLinkRef(problem);
                if (ref != null) {
                    String description = TooltipLinkHandlerEP.getDescription(ref, editor);
                    if (description != null) {
                        description = DefaultInspectionToolPresentation.stripUIRefsFromInspectionDescription(UIUtil.getHtmlBody(new Html(description).setKeepFont(true)));
                        text += UIUtil.getHtmlBody(new Html(problem).setKeepFont(true)).replace(DaemonBundle.message("inspection.extended.description"),
                                DaemonBundle.message("inspection.collapse.description")) +
                                END_MARKER + "<p>" + description + UIUtil.BORDER_LINE;
                    }
                }
                else {
                    text += UIUtil.getHtmlBody(new Html(problem).setKeepFont(true)) + UIUtil.BORDER_LINE;
                }
            }
            if (!text.isEmpty()) { //otherwise do not change anything
                myText = XmlStringUtil.wrapInHtml(StringUtil.trimEnd(text, UIUtil.BORDER_LINE));
                return true;
            }
            return false;
        }


        private static String getLinkRef( String text) {
            final String linkWithRef = "<a href=\"";
            final int linkStartIdx = text.indexOf(linkWithRef);
            if (linkStartIdx >= 0) {
                final String ref = text.substring(linkStartIdx + linkWithRef.length());
                final int quoteIdx = ref.indexOf('"');
                if (quoteIdx > 0) {
                    return ref.substring(0, quoteIdx);
                }
            }
            return null;
        }

        @Override
        protected void stripDescription() {
            final List<String> problems = StringUtil.split(UIUtil.getHtmlBody(new Html(myText).setKeepFont(true)), UIUtil.BORDER_LINE);
            myText = "";
            for (String problem1 : problems) {
                final String problem = StringUtil.split(problem1, END_MARKER).get(0);
                myText += UIUtil.getHtmlBody(new Html(problem).setKeepFont(true)).replace(DaemonBundle.message("inspection.collapse.description"),
                        DaemonBundle.message("inspection.extended.description")) + UIUtil.BORDER_LINE;
            }
            myText = XmlStringUtil.wrapInHtml(StringUtil.trimEnd(myText, UIUtil.BORDER_LINE));
        }

        @Override
        protected LineTooltipRenderer createRenderer(final String text, final int width) {
            return new MyRenderer(text, width, getEqualityObjects());
        }
    }
}