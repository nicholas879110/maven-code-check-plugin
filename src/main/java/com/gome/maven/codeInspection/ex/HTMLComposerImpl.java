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

/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Dec 22, 2001
 * Time: 4:54:17 PM
 * To change template for new interface use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.CommonProblemDescriptor;
import com.gome.maven.codeInspection.HTMLComposer;
import com.gome.maven.codeInspection.InspectionsBundle;
import com.gome.maven.codeInspection.export.HTMLExporter;
import com.gome.maven.codeInspection.lang.HTMLComposerExtension;
import com.gome.maven.codeInspection.lang.InspectionExtensionsFactory;
import com.gome.maven.codeInspection.reference.*;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.ProjectUtilCore;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.util.PsiUtilCore;

import java.util.HashMap;
import java.util.Map;

/**
 * @author max
 */
public abstract class HTMLComposerImpl extends HTMLComposer {
    protected HTMLExporter myExporter;
    private final int[] myListStack;
    private int myListStackTop;
    private final Map<Key, HTMLComposerExtension> myExtensions = new HashMap<Key, HTMLComposerExtension>();
    private final Map<Language, HTMLComposerExtension> myLanguageExtensions = new HashMap<Language, HTMLComposerExtension>();
     protected static final String BR = "<br>";
     protected static final String NBSP = "&nbsp;";
     protected static final String CODE_CLOSING = "</code>";
     protected static final String CODE_OPENING = "<code>";
     protected static final String B_OPENING = "<b>";
     protected static final String B_CLOSING = "</b>";

     protected static final String CLOSE_TAG = "\">";
     protected static final String A_HREF_OPENING = "<a HREF=\"";
     protected static final String A_CLOSING = "</a>";

    protected HTMLComposerImpl() {
        myListStack = new int[5];
        myListStackTop = -1;
        for (InspectionExtensionsFactory factory : Extensions.getExtensions(InspectionExtensionsFactory.EP_NAME)) {
            final HTMLComposerExtension extension = factory.createHTMLComposerExtension(this);
            if (extension != null) {
                myExtensions.put(extension.getID(), extension);
                myLanguageExtensions.put(extension.getLanguage(), extension);
            }
        }
    }

    public abstract void compose(StringBuffer buf, RefEntity refEntity);

    public void compose(StringBuffer buf, RefEntity refElement, CommonProblemDescriptor descriptor) {}

    public void composeWithExporter(StringBuffer buf, RefEntity refEntity, HTMLExporter exporter) {
        myExporter = exporter;
        compose(buf, refEntity);
        myExporter = null;
    }

    protected void genPageHeader(final StringBuffer buf, RefEntity refEntity) {
        if (refEntity instanceof RefElement) {
            RefElement refElement = (RefElement)refEntity;

            appendHeading(buf, InspectionsBundle.message("inspection.offline.view.tool.display.name.title"));
            buf.append(BR);
            appendAfterHeaderIndention(buf);

            appendShortName(buf, refElement);
            buf.append(BR).append(BR);

            appendHeading(buf, InspectionsBundle.message("inspection.export.results.capitalized.location"));
            buf.append(BR);
            appendAfterHeaderIndention(buf);
            appendLocation(buf, refElement);
            buf.append(BR).append(BR);
        }
    }

    private void appendLocation(final StringBuffer buf, final RefElement refElement) {
        final HTMLComposerExtension extension = getLanguageExtension(refElement);
        if (extension != null) {
            extension.appendLocation(refElement, buf);
        }
        if (refElement instanceof RefFile){
            buf.append(InspectionsBundle.message("inspection.export.results.file"));
            buf.append(NBSP);
            appendElementReference(buf, refElement, false);
        }
    }

    
    private HTMLComposerExtension getLanguageExtension(final RefElement refElement) {
        final PsiElement element = refElement.getElement();
        return element != null ? myLanguageExtensions.get(element.getLanguage()) : null;
    }

    private void appendShortName(final StringBuffer buf, RefElement refElement) {
        final HTMLComposerExtension extension = getLanguageExtension(refElement);
        if (extension != null) {
            extension.appendShortName(refElement, buf);
        } else {
            refElement.accept(new RefVisitor() {
                @Override public void visitFile( RefFile file) {
                    final PsiFile psiFile = file.getElement();
                    if (psiFile != null) {
                        buf.append(B_OPENING);
                        buf.append(psiFile.getName());
                        buf.append(B_CLOSING);
                    }
                }
            });
        }
    }

    protected void appendQualifiedName(StringBuffer buf, RefEntity refEntity) {
        if (refEntity == null) return;
        String qName = "";

        while (!(refEntity instanceof RefProject)) {
            if (qName.length() > 0) qName = "." + qName;

            String name = null;
            if (refEntity instanceof RefElement) {
                final HTMLComposerExtension extension = getLanguageExtension((RefElement)refEntity);
                if (extension != null) {
                    name = extension.getQualifiedName(refEntity);
                }
            }

            if (name == null) {
                name = refEntity.getName();
            }

            qName = name + qName;
            refEntity = refEntity.getOwner();
        }

        buf.append(qName);
    }

    @Override
    public void appendElementReference(final StringBuffer buf, RefElement refElement) {
        appendElementReference(buf, refElement, true);
    }

    @Override
    public void appendElementReference(final StringBuffer buf, RefElement refElement, String linkText,  String frameName) {
        if (myExporter == null) {
            final String url = ((RefElementImpl)refElement).getURL();
            if (url != null) {
                appendElementReference(buf, url, linkText, frameName);
            }
        }
        else {
            appendElementReference(buf, myExporter.getURL(refElement), linkText, frameName);
        }
    }

    @Override
    public void appendElementReference(final StringBuffer buf, String url, String linkText,  String frameName) {
        buf.append(A_HREF_OPENING);
        buf.append(url);
        if (frameName != null) {
             final String target = "\" target=\"";
            buf.append(target);
            buf.append(frameName);
        }

        buf.append("\">");
        buf.append(linkText);
        buf.append(A_CLOSING);
    }

    protected void appendQuickFix( final StringBuffer buf, String text, int index) {
        if (myExporter == null) {
            buf.append("<a HREF=\"file://bred.txt#invoke:").append(index);
            buf.append("\">");
            buf.append(text);
            buf.append("</a>");
        }
    }

    @Override
    public void appendElementReference(final StringBuffer buf, RefElement refElement, boolean isPackageIncluded) {
        final HTMLComposerExtension extension = getLanguageExtension(refElement);

        if (extension != null) {
            extension.appendReferencePresentation(refElement, buf, isPackageIncluded);
        } else if (refElement instanceof RefFile) {
            buf.append(A_HREF_OPENING);

            if (myExporter == null) {
                buf.append(((RefElementImpl)refElement).getURL());
            }
            else {
                buf.append(myExporter.getURL(refElement));
            }

            buf.append("\">");
            String refElementName = refElement.getName();
            final PsiElement element = refElement.getElement();
            if (element != null) {
                final VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
                if (virtualFile != null) {
                    refElementName = ProjectUtilCore.displayUrlRelativeToProject(virtualFile, virtualFile.getPresentableUrl(), element.getProject(),
                            true, false);
                }
            }
            buf.append(refElementName);
            buf.append(A_CLOSING);
        }
    }

    @Override
    public String composeNumereables(int n, String statement, String singleEnding, String multipleEnding) {
        final StringBuilder buf = new StringBuilder();
        buf.append(n);
        buf.append(' ');
        buf.append(statement);

        if (n % 10 == 1 && n % 100 != 11) {
            buf.append(singleEnding);
        }
        else {
            buf.append(multipleEnding);
        }
        return buf.toString();
    }

    @Override
    public void appendElementInReferences(StringBuffer buf, RefElement refElement) {
        if (refElement.getInReferences().size() > 0) {
            appendHeading(buf, InspectionsBundle.message("inspection.export.results.used.from"));
            startList(buf);
            for (RefElement refCaller : refElement.getInReferences()) {
                appendListItem(buf, refCaller);
            }
            doneList(buf);
        }
    }

    @Override
    public void appendElementOutReferences(StringBuffer buf, RefElement refElement) {
        if (refElement.getOutReferences().size() > 0) {
            buf.append(BR);
            appendHeading(buf, InspectionsBundle.message("inspection.export.results.uses"));
            startList(buf);
            for (RefElement refCallee : refElement.getOutReferences()) {
                appendListItem(buf, refCallee);
            }
            doneList(buf);
        }
    }

    @Override
    public void appendListItem(StringBuffer buf, RefElement refElement) {
        startListItem(buf);
        buf.append(CLOSE_TAG);
        appendElementReference(buf, refElement, true);
        appendAdditionalListItemInfo(buf, refElement);
        doneListItem(buf);
    }

    protected void appendAdditionalListItemInfo(StringBuffer buf, RefElement refElement) {
        // Default appends nothing.
    }

    protected void appendResolution(StringBuffer buf, RefEntity where, String[] quickFixes) {
        if (myExporter != null) return;
        if (where instanceof RefElement && !where.isValid()) return;
        if (quickFixes != null) {
            boolean listStarted = false;
            for (int i = 0; i < quickFixes.length; i++) {
                final String text = quickFixes[i];
                if (text == null) continue;
                if (!listStarted) {
                    appendHeading(buf, InspectionsBundle.message("inspection.problem.resolution"));
                    startList(buf);
                    listStarted = true;
                }
                startListItem(buf);
                appendQuickFix(buf, text, i);
                doneListItem(buf);
            }

            if (listStarted) {
                doneList(buf);
            }
        }
    }


    @Override
    public void startList( final StringBuffer buf) {
        buf.append("<ul>");
        myListStackTop++;
        myListStack[myListStackTop] = 0;
    }

    @Override
    public void doneList( StringBuffer buf) {
        buf.append("</ul>");
        if (myListStack[myListStackTop] != 0) {
            buf.append("<table cellpadding=\"0\" border=\"0\" cellspacing=\"0\"><tr><td>&nbsp;</td></tr></table>");
        }
        myListStackTop--;
    }

    @Override
    public void startListItem( StringBuffer buf) {
        myListStack[myListStackTop]++;
        buf.append("<li>");
    }

    public static void doneListItem( StringBuffer buf) {
        buf.append("</li>");
    }

    @Override
    public void appendNoProblems(StringBuffer buf) {
        buf.append(BR);
        appendAfterHeaderIndention(buf);
        buf.append(B_OPENING);
        buf.append(InspectionsBundle.message("inspection.export.results.no.problems.found"));
        buf.append(B_CLOSING).append(BR);
    }

    @Override
    public <T> T getExtension(final Key<T> key) {
        return (T)myExtensions.get(key);
    }
}