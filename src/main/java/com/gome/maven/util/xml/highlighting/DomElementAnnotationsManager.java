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

import com.gome.maven.codeInspection.InspectionManager;
import com.gome.maven.codeInspection.ProblemDescriptor;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.SimpleModificationTracker;
import com.gome.maven.util.xml.DomElement;
import com.gome.maven.util.xml.DomFileElement;

import java.util.EventListener;
import java.util.List;

public abstract class DomElementAnnotationsManager extends SimpleModificationTracker {

    public static DomElementAnnotationsManager getInstance(Project project) {
        return ServiceManager.getService(project, DomElementAnnotationsManager.class);
    }

    
    public abstract DomElementsProblemsHolder getProblemHolder(DomElement element);

    
    public abstract DomElementsProblemsHolder getCachedProblemHolder(DomElement element);

    public abstract List<ProblemDescriptor> createProblemDescriptors(final InspectionManager manager, DomElementProblemDescriptor problemDescriptor);

    public abstract boolean isHighlightingFinished(final DomElement[] domElements);

    public abstract void addHighlightingListener(DomHighlightingListener listener, Disposable parentDisposable);

    public abstract DomHighlightingHelper getHighlightingHelper();

    /**
     * Calls {@link com.gome.maven.util.xml.highlighting.DomElementsInspection#checkFileElement(com.gome.maven.util.xml.DomFileElement, DomElementAnnotationHolder)}
     * with appropriate parameters if needed, saves the collected problems to {@link com.gome.maven.util.xml.highlighting.DomElementsProblemsHolder}, which
     * can then be obtained from {@link #getProblemHolder(com.gome.maven.util.xml.DomElement)} method, and returns them.
     *
     * @param element file element being checked
     * @param inspection inspection to run on the given file element
     * @param onTheFly
     * @return collected DOM problem descriptors
     */
    
    public abstract <T extends DomElement> List<DomElementProblemDescriptor> checkFileElement( DomFileElement<T> element,
                                                                                               DomElementsInspection<T> inspection,
                                                                                              boolean onTheFly);

    public abstract void dropAnnotationsCache();

    public interface DomHighlightingListener extends EventListener {

        /**
         * Called each time when an annotator or inspection has finished error-highlighting of a particular
         * {@link com.gome.maven.util.xml.DomFileElement}
         * @param element file element whose highlighting has been finished
         */
        void highlightingFinished( DomFileElement element);
    }
}
