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
 * User: anna
 * Date: 05-Jan-2007
 */
package com.gome.maven.codeInspection.offlineViewer;

import com.gome.maven.codeInspection.offline.OfflineProblemDescriptor;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.ui.InspectionToolPresentation;
import com.gome.maven.codeInspection.ui.RefElementNode;

public class OfflineRefElementNode extends RefElementNode {
    public OfflineRefElementNode( OfflineProblemDescriptor descriptor,  InspectionToolPresentation presentation) {
        super(descriptor, presentation);
    }

    @Override
    
    public RefEntity getElement() {
        if (userObject instanceof RefEntity) {
            return (RefEntity)userObject;
        }
        if (userObject == null) return null;
        final RefEntity refElement = ((OfflineProblemDescriptor)userObject).getRefElement(myToolPresentation.getContext().getRefManager());
        setUserObject(refElement);
        return refElement;
    }

    
    public OfflineProblemDescriptor getDescriptor() {
        if (userObject instanceof OfflineProblemDescriptor) {
            return (OfflineProblemDescriptor)userObject;
        }
        return null;
    }
}