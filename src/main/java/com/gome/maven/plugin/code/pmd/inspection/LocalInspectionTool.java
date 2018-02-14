package com.gome.maven.plugin.code.pmd.inspection;

import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevel;
import com.gome.maven.plugin.code.pmd.util.HighlightDisplayLevels;
import com.gome.maven.openapi.util.text.StringUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;

public abstract class LocalInspectionTool {
    private static Log LOG = new SystemStreamLog();

    public boolean runForWholeFile() {
        return false;
    }

    public ProblemDescriptor[] checkFile(File file, boolean isOnTheFly) {
        return null;
    }
    public String getStaticDescription() {
        return null;
    }

    interface DefaultNameProvider {
         String getDefaultShortName();
         String getDefaultDisplayName();
         String getDefaultGroupDisplayName();
    }

    protected volatile DefaultNameProvider myNameProvider = null;

    public String getDisplayName() {
        if (myNameProvider != null) {
            final String name = myNameProvider.getDefaultDisplayName();
            if (name != null) {
                return name;
            }
        }
        LOG.error(getClass() + ": display name should be overridden or configured via XML " + getClass());
        return "";
    }

    public String getShortName() {
        if (myNameProvider != null) {
            final String name = myNameProvider.getDefaultShortName();
            if (name != null) {
                return name;
            }
        }
        return getShortName(getClass().getSimpleName());
    }

    public static String getShortName( String className) {
        return StringUtil.trimEnd(StringUtil.trimEnd(className, "Inspection"), "InspectionBase");
    }

    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevels.CRITICAL;
    }

    public abstract String ruleName();
}
