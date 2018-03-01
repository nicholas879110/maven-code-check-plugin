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

package com.gome.maven.util.xml;

import com.gome.maven.ide.structureView.StructureViewBuilder;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.search.GlobalSearchScope;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.util.Function;

import java.util.Collection;
import java.util.List;

/**
 * @author Gregory.Shrago
 */
public abstract class DomService {
    private static DomService ourCachedInstance = null;

    public static DomService getInstance() {
        if (ourCachedInstance == null) {
            ourCachedInstance = ServiceManager.getService(DomService.class);
        }
        return ourCachedInstance;
    }

    /**
     * @deprecated use {@link #getDomFileCandidates(Class, com.gome.maven.openapi.project.Project, com.gome.maven.psi.search.GlobalSearchScope)} (to remove in IDEA 15)
     */
    public abstract Collection<VirtualFile> getDomFileCandidates(Class<? extends DomElement> description, Project project);

    /**
     * @param rootElementClass class of root (file-level) element in DOM model
     * @param project          current project
     * @param scope            search scope
     * @return files containing given root element
     * @see #getFileElements(Class, com.gome.maven.openapi.project.Project, com.gome.maven.psi.search.GlobalSearchScope)
     */
    public abstract Collection<VirtualFile> getDomFileCandidates(Class<? extends DomElement> rootElementClass,
                                                                 Project project,
                                                                 GlobalSearchScope scope);

    /**
     * @param rootElementClass class of root (file-level) element in DOM model
     * @param project          current project
     * @param scope            search scope
     * @return DOM file elements containing given root element
     */
    public abstract <T extends DomElement> List<DomFileElement<T>> getFileElements(Class<T> rootElementClass,
                                                                                   final Project project,
                                                                                    GlobalSearchScope scope);

    public abstract ModelMerger createModelMerger();

    public abstract <T extends DomElement> DomAnchor<T> createAnchor(T domElement);

    
    public abstract XmlFile getContainingFile( DomElement domElement);

    
    public abstract EvaluatedXmlName getEvaluatedXmlName( DomElement element);

    
    public abstract XmlFileHeader getXmlFileHeader(XmlFile file);

    public enum StructureViewMode {
        SHOW, SHOW_CHILDREN, SKIP
    }

    public abstract StructureViewBuilder createSimpleStructureViewBuilder(final XmlFile file,
                                                                          final Function<DomElement, StructureViewMode> modeProvider);
}
