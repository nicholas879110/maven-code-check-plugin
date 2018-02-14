
/*
 * User: anna
 * Date: 22-Jan-2007
 */
package com.gome.maven.analysis;

import com.gome.maven.openapi.progress.PerformInBackgroundOption;
import com.gome.maven.openapi.project.Project;

public class PerformAnalysisInBackgroundOption implements PerformInBackgroundOption {
    private final AnalysisUIOptions myUIOptions;

    public PerformAnalysisInBackgroundOption(Project project) {
        myUIOptions = AnalysisUIOptions.getInstance(project);
    }

    @Override
    public boolean shouldStartInBackground() {
        return myUIOptions.ANALYSIS_IN_BACKGROUND;
    }

    @Override
    public void processSentToBackground() {
        myUIOptions.ANALYSIS_IN_BACKGROUND = true;
    }

}
