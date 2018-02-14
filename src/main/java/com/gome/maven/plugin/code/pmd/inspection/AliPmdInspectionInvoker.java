/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gome.maven.plugin.code.pmd.inspection;


import com.beust.jcommander.internal.Lists;
import com.gome.maven.plugin.code.pmd.config.P3cConfig;
import com.gome.maven.plugin.code.pmd.pmd.AliPmdProcessor;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

;

/**
 * @author caikang
 * @date 2016/12/13
 */
public class AliPmdInspectionInvoker {


    private static final Log logger = new SystemStreamLog();

    private File psiFile;
    private Rule rule;

    public AliPmdInspectionInvoker(File psiFile, Rule rule) {
        this.psiFile = psiFile;
        this.rule = rule;
    }

    private List<RuleViolation> violations = Collections.emptyList();

    public void doInvoke() throws IOException {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        AliPmdProcessor processor = new AliPmdProcessor(rule, Charset.defaultCharset().name());
        long start = System.currentTimeMillis();
        violations = processor.processFile(psiFile);
        logger.info("elapsed "+(System.currentTimeMillis() - start)+"ms to" +
                " to apply rule "+rule.getName()+" for file "+psiFile.getCanonicalPath());
    }

    public ProblemDescriptor[] getRuleProblems(Boolean isOnTheFly) {
        if (violations.isEmpty()) {
            return null;
        }
        List<ProblemDescriptor> problemDescriptors = Lists.newArrayList(violations.size());
        for (RuleViolation rv : violations) {
            ProblemDescriptor problemDescriptor = new ProblemDescriptor(rv);
            if (problemDescriptor == null) continue;
            problemDescriptors.add(problemDescriptor);
        }
        ProblemDescriptor[] problemDescriptorArray = new ProblemDescriptor[problemDescriptors.size()];
        problemDescriptorArray = problemDescriptors.toArray(problemDescriptorArray);
        return problemDescriptorArray;
    }


    public static Cache<FileRule, AliPmdInspectionInvoker> invokers;


    public static P3cConfig smartFoxConfig = P3cConfig.getInstance();

    static {
        reInitInvokers(smartFoxConfig.getRuleCacheTime());
    }

    public static ProblemDescriptor[] invokeInspection(File psiFile, Rule rule, Boolean isOnTheFly) {
        if (psiFile == null) {
            return null;
        }
        AliPmdInspectionInvoker invoker = null;
        try {
            if (!smartFoxConfig.isRuleCacheEnable()) {
                invoker = new AliPmdInspectionInvoker(psiFile, rule);
                invoker.doInvoke();
                return invoker.getRuleProblems(isOnTheFly);
            }
            invoker = invokers.getIfPresent(new FileRule(psiFile.getCanonicalPath(), rule.getName()));
            if (invoker == null) {
                synchronized (psiFile) {
                    invoker = invokers.getIfPresent(psiFile.getCanonicalPath());
                    if (invoker == null) {
                        invoker = new AliPmdInspectionInvoker(psiFile, rule);
                        invoker.doInvoke();
                        invokers.put(new FileRule(psiFile.getCanonicalPath(), rule.getName()), invoker);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return invoker.getRuleProblems(isOnTheFly);
    }

    private static void doInvokeIfPresent(String filePath, String rule) throws IOException {
        invokers.getIfPresent(new FileRule(filePath, rule)).doInvoke();
    }

    public static void refreshFileViolationsCache(File file) throws IOException {
        for (String it : AliLocalInspectionToolProvider.ruleNames) {
            doInvokeIfPresent(file.getCanonicalPath(), it);

        }
    }

    public static void reInitInvokers(Long expireTime) {
        invokers = CacheBuilder.newBuilder().maximumSize(500).expireAfterWrite(expireTime, TimeUnit.MILLISECONDS).build();
    }


    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
}


class FileRule {
    private String filePath;
    private String ruleName;

    public FileRule() {
    }

    public FileRule(String filePath, String ruleName) {
        this.filePath = filePath;
        this.ruleName = ruleName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
}
