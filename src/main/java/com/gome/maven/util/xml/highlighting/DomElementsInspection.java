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

package com.gome.maven.util.xml.highlighting;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.codeInspection.XmlSuppressableInspectionTool;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.xml.XmlElement;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.psi.xml.XmlTag;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.xml.*;
import com.gome.maven.util.xml.reflect.AbstractDomChildrenDescription;
import gnu.trove.THashSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 * @see com.gome.maven.util.xml.highlighting.BasicDomElementsInspection
 */
public abstract class DomElementsInspection<T extends DomElement> extends XmlSuppressableInspectionTool {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.util.xml.highlighting.DomElementsInspection");

    private final Set<Class<? extends T>> myDomClasses;

    public DomElementsInspection(Class<? extends T> domClass,  Class<? extends T>... additionalClasses) {
        myDomClasses = new THashSet<Class<? extends T>>(Arrays.asList(additionalClasses));
        myDomClasses.add(domClass);
    }

    /**
     * This method is called internally in {@link DomElementAnnotationsManager#checkFileElement(com.gome.maven.util.xml.DomFileElement, DomElementsInspection, boolean)}
     * it should add some problems to the annotation holder. The default implementation performs recursive tree traversal, and calls
     * {@link #checkDomElement(com.gome.maven.util.xml.DomElement, DomElementAnnotationHolder, DomHighlightingHelper)} for each element. 
     * @param domFileElement file element to check
     * @param holder the place to store problems
     */
    public void checkFileElement(DomFileElement<T> domFileElement, final DomElementAnnotationHolder holder) {
        final DomHighlightingHelper helper =
                DomElementAnnotationsManager.getInstance(domFileElement.getManager().getProject()).getHighlightingHelper();
        final Consumer<DomElement> consumer = new Consumer<DomElement>() {
            @Override
            public void consume(final DomElement element) {
                checkChildren(element, this);
                checkDomElement(element, holder, helper);
            }
        };
        consumer.consume(domFileElement.getRootElement());
    }

    @SuppressWarnings({"MethodMayBeStatic"})
    protected void checkChildren(final DomElement element, Consumer<DomElement> visitor) {
        final XmlElement xmlElement = element.getXmlElement();
        if (xmlElement instanceof XmlTag) {
            for (final DomElement child : DomUtil.getDefinedChildren(element, true, true)) {
                final XmlElement element1 = child.getXmlElement();
                if (element1 == null) {
                    LOG.error("child=" + child + " of class " + child.getClass() + "; parent=" + element);
                }
                if (element1.isPhysical()) {
                    visitor.consume(child);
                }
            }

            for (final AbstractDomChildrenDescription description : element.getGenericInfo().getChildrenDescriptions()) {
                if (description.getAnnotation(Required.class) != null) {
                    for (final DomElement child : description.getValues(element)) {
                        if (!DomUtil.hasXml(child)) {
                            visitor.consume(child);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the classes passed earlier to the constructor
     */
    public final Set<Class<? extends T>> getDomClasses() {
        return myDomClasses;
    }

    /**
     * Not intended to be overridden or called by implementors.
     * Override {@link #checkFileElement(com.gome.maven.util.xml.DomFileElement, DomElementAnnotationHolder)} (which is preferred) or
     * {@link #checkDomElement(com.gome.maven.util.xml.DomElement, DomElementAnnotationHolder, DomHighlightingHelper)} instead.
     */
    @Override
    
    public ProblemDescriptor[] checkFile( PsiFile file,  InspectionManager manager, boolean isOnTheFly) {
        if (file instanceof XmlFile && (file.isPhysical() || ApplicationManager.getApplication().isUnitTestMode())) {
            for (Class<? extends T> domClass: myDomClasses) {
                final DomFileElement<? extends T> fileElement = DomManager.getDomManager(file.getProject()).getFileElement((XmlFile)file, domClass);
                if (fileElement != null) {
                    //noinspection unchecked
                    return checkDomFile((DomFileElement<T>)fileElement, manager, isOnTheFly);
                }
            }
        }
        return null;
    }

    @Override
    
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    /**
     * not intended to be overridden or called by implementors
     */
    
    protected ProblemDescriptor[] checkDomFile( final DomFileElement<T> domFileElement,
                                                final InspectionManager manager,
                                               @SuppressWarnings("UnusedParameters") final boolean isOnTheFly) {
        final DomElementAnnotationsManager annotationsManager = DomElementAnnotationsManager.getInstance(manager.getProject());

        final List<DomElementProblemDescriptor> list = annotationsManager.checkFileElement(domFileElement, this, isOnTheFly);
        if (list.isEmpty()) return ProblemDescriptor.EMPTY_ARRAY;

        List<ProblemDescriptor> problems =
                ContainerUtil.concat(list, new Function<DomElementProblemDescriptor, Collection<? extends ProblemDescriptor>>() {
                    @Override
                    public Collection<ProblemDescriptor> fun(final DomElementProblemDescriptor s) {
                        return annotationsManager.createProblemDescriptors(manager, s);
                    }
                });
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    /**
     * Check particular DOM element for problems. The inspection implementor should focus on this method.
     * The default implementation throws {@link UnsupportedOperationException}.
     * See {@link com.gome.maven.util.xml.highlighting.BasicDomElementsInspection}
     * @param element element to check
     * @param holder a place to add problems to
     * @param helper helper object
     */
    protected void checkDomElement(DomElement element, DomElementAnnotationHolder holder, DomHighlightingHelper helper) {
        throw new UnsupportedOperationException("checkDomElement() is not implemented in " + getClass().getName());
    }
}
