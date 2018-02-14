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
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.GlobalInspectionContext;
import com.gome.maven.codeInspection.GlobalInspectionTool;
import com.gome.maven.codeInspection.InspectionEP;
import com.gome.maven.codeInspection.LocalInspectionTool;
import com.gome.maven.codeInspection.reference.RefGraphAnnotator;
import com.gome.maven.codeInspection.reference.RefManagerImpl;
import com.gome.maven.util.ArrayUtil;

/**
 * User: anna
 * Date: 28-Dec-2005
 */
public class GlobalInspectionToolWrapper extends InspectionToolWrapper<GlobalInspectionTool, InspectionEP> {
    public GlobalInspectionToolWrapper( GlobalInspectionTool globalInspectionTool) {
        super(globalInspectionTool);
    }

    public GlobalInspectionToolWrapper( GlobalInspectionTool tool,  InspectionEP ep) {
        super(tool, ep);
    }

    public GlobalInspectionToolWrapper( InspectionEP ep) {
        super(ep);
    }

    private GlobalInspectionToolWrapper( GlobalInspectionToolWrapper other) {
        super(other);
    }

    
    @Override
    public GlobalInspectionToolWrapper createCopy() {
        return new GlobalInspectionToolWrapper(this);
    }

    @Override
    public void initialize( GlobalInspectionContext context) {
        super.initialize(context);
        RefManagerImpl refManager = (RefManagerImpl)context.getRefManager();
        final RefGraphAnnotator annotator = getTool().getAnnotator(refManager);
        if (annotator != null) {
            refManager.registerGraphAnnotator(annotator);
        }
        getTool().initialize(context);
    }

    @Override
    
    public JobDescriptor[] getJobDescriptors( GlobalInspectionContext context) {
        final JobDescriptor[] additionalJobs = getTool().getAdditionalJobs();
        if (additionalJobs == null) {
            return getTool().isGraphNeeded() ? context.getStdJobDescriptors().BUILD_GRAPH_ONLY : JobDescriptor.EMPTY_ARRAY;
        }
        else {
            return getTool().isGraphNeeded() ? ArrayUtil.append(additionalJobs, context.getStdJobDescriptors().BUILD_GRAPH) : additionalJobs;
        }
    }

    public boolean worksInBatchModeOnly() {
        return getTool().worksInBatchModeOnly();
    }

    
    public LocalInspectionToolWrapper getSharedLocalInspectionToolWrapper() {
        final LocalInspectionTool sharedTool = getTool().getSharedLocalInspectionTool();
        if (sharedTool == null) {
            return null;
        }
        return new LocalInspectionToolWrapper(sharedTool);
    }
}
