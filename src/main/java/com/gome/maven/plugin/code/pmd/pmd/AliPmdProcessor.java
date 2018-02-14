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
package com.gome.maven.plugin.code.pmd.pmd;


import net.sourceforge.pmd.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author caikang
 * @date 2016/12/11
 */
public class AliPmdProcessor {
    private static Log LOG = new SystemStreamLog();

    private RuleSetFactory ruleSetFactory;
    private PMDConfiguration configuration = new PMDConfiguration();
    private Rule rule;
    private String encoding;

    public AliPmdProcessor(Rule rule, String encoding) {
        this.rule = rule;
        this.encoding = encoding;
        ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(configuration);
    }


    public List<RuleViolation> processFile(File psiFile) {
        if (StringUtils.isBlank(encoding)) {
            encoding = Charset.defaultCharset().name();
        }
        configuration.setSourceEncoding(encoding);
        try {
            configuration.setInputPaths(psiFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
//        Document document = FileDocumentManager.getInstance().getDocument(psiFile.getVirtualFile());
//        if (document == null) return Collections.emptyList();
        final RuleContext ctx = new RuleContext();
        SourceCodeProcessor processor = new SourceCodeProcessor(configuration);
        String niceFileName = null;
        try {
            niceFileName = psiFile.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if (niceFileName==null) throw new Exception("niceFileName is null ");

        Report report = Report.createReport(ctx, niceFileName);
        RuleSets ruleSets = new RuleSets();
        RuleSet ruleSet = new RuleSet();
        ruleSet.addRule(rule);
        ruleSets.addRuleSet(ruleSet);
        LOG.debug("Processing " + ctx.getSourceCodeFilename());
        ruleSets.start(ctx);
        try {
            ctx.setLanguageVersion(null);
            processor.processSourceCode(new StringReader(FileUtils.fileRead(psiFile, encoding)), ruleSets, ctx);
        } catch (PMDException pmde) {
            LOG.debug("Error while processing file: " + niceFileName, pmde.getCause());
            report.addError(new Report.ProcessingError(pmde.getMessage(), niceFileName));
        } catch (IOException ioe) {
            LOG.error("Unable to read source file: " + niceFileName, ioe);
        } catch (RuntimeException re) {
            LOG.error("RuntimeException while processing file: " + niceFileName, re);
        }
        ruleSets.end(ctx);
        Report ctxReport = ctx.getReport();
        Iterable<RuleViolation> iterable = (Iterable<RuleViolation>) ctxReport;
        Iterator<RuleViolation> iterator = iterable.iterator();
        List<RuleViolation> list = new ArrayList<RuleViolation>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }


}
