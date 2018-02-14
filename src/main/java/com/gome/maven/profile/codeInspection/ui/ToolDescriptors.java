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
package com.gome.maven.profile.codeInspection.ui;

import com.gome.maven.codeInspection.ex.Descriptor;
import com.gome.maven.codeInspection.ex.InspectionProfileImpl;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.ex.ScopeToolState;
import com.gome.maven.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class ToolDescriptors {

    
    private final Descriptor myDefaultDescriptor;
    
    private final List<Descriptor> myNonDefaultDescriptors;

    private ToolDescriptors(final  Descriptor defaultDescriptor,
                            final  List<Descriptor> nonDefaultDescriptors) {
        myDefaultDescriptor = defaultDescriptor;
        myNonDefaultDescriptors = nonDefaultDescriptors;
    }

    public static ToolDescriptors fromScopeToolState(final ScopeToolState state,
                                                     final InspectionProfileImpl profile,
                                                     final Project project) {
        final InspectionToolWrapper toolWrapper = state.getTool();
        final List<ScopeToolState> nonDefaultTools = profile.getNonDefaultTools(toolWrapper.getShortName(), project);
        final ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>(nonDefaultTools.size());
        for (final ScopeToolState nonDefaultToolState : nonDefaultTools) {
            descriptors.add(new Descriptor(nonDefaultToolState, profile, project));
        }
        return new ToolDescriptors(new Descriptor(state, profile, project), descriptors);
    }

    
    public Descriptor getDefaultDescriptor() {
        return myDefaultDescriptor;
    }

    
    public List<Descriptor> getNonDefaultDescriptors() {
        return myNonDefaultDescriptors;
    }

    
    public ScopeToolState getDefaultScopeToolState() {
        return myDefaultDescriptor.getState();
    }
}
