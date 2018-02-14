package com.gome.maven.plugin.code.pmd.inspection;

import com.gome.maven.plugin.code.pmd.util.Intrinsics;
import net.sourceforge.pmd.Rule;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * @author zhangliewei
 * @date 2017/12/20 16:43
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class RuleInfo {
    private static Log LOGGER = new SystemStreamLog();
    private Rule rule;
    private ShouldInspectChecker shouldInspectChecker;

    public final Rule getRule() {
        return this.rule;
    }

    public final void setRule(Rule rule) {
        Intrinsics.checkParameterIsNotNull(rule, "<set-?>");
        this.rule = rule;
    }

    public final ShouldInspectChecker getShouldInspectChecker() {
        return this.shouldInspectChecker;
    }

    public final void setShouldInspectChecker(ShouldInspectChecker shouldInspectChecker) {
        Intrinsics.checkParameterIsNotNull(shouldInspectChecker, "<set-?>");
        this.shouldInspectChecker = shouldInspectChecker;
    }

    public RuleInfo(Rule rule, ShouldInspectChecker shouldInspectChecker) {
        if (shouldInspectChecker == null) {
            LOGGER.error("shouldInspectChecker is null!");
        }
        this.rule = rule;
        this.shouldInspectChecker = shouldInspectChecker;
    }
}
