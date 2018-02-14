package com.gome.maven.codeInspection;

import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.FunctionUtil;
import com.gome.maven.util.containers.ContainerUtil;

/**
 * User: anna
 * Date: 04-Jan-2006
 */
public class CommonProblemDescriptorImpl implements CommonProblemDescriptor {
    private final QuickFix[] myFixes;
    private final String myDescriptionTemplate;

    public CommonProblemDescriptorImpl(final QuickFix[] fixes,  final String descriptionTemplate) {
        if (fixes == null) {
            myFixes = null;
        }
        else if (fixes.length == 0) {
            myFixes = QuickFix.EMPTY_ARRAY;
        }
        else {
            // no copy in most cases
            myFixes = ArrayUtil.contains(null, fixes) ? ContainerUtil.mapNotNull(fixes, FunctionUtil.<QuickFix>id(), QuickFix.EMPTY_ARRAY) : fixes;
        }
        myDescriptionTemplate = descriptionTemplate;
    }

    @Override
    
    public String getDescriptionTemplate() {
        return myDescriptionTemplate;
    }

    @Override

    public QuickFix[] getFixes() {
        return myFixes;
    }

    public String toString() {
        return myDescriptionTemplate;
    }
}
