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


import com.alibaba.p3c.pmd.I18nResources;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.gome.maven.plugin.code.pmd.config.P3cConfig;
//import com.gome.maven.plugin.code.pmd.inspection.standalone.AliMissingOverrideAnnotationInspection;
import com.gome.maven.plugin.code.pmd.util.Intrinsics;
import javassist.*;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import javax.annotation.Generated;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

;

/**
 * @author caikang
 * @date 2016/12/16
 */
public class AliLocalInspectionToolProvider {

    private static Log LOGGER = new SystemStreamLog();

    private static List<Class<?>> CLASS_LIST = Lists.newArrayList();
    public static List<String> ruleNames = Lists.newArrayList();
    private static Map<String, RuleInfo> ruleInfoMap = Maps.newHashMap();
    private static List<Class<?>> nativeInspectionToolClass =
            new ArrayList<Class<?>>() {{
//                add(AliMissingOverrideAnnotationInspection.class);
//                add(AliAccessStaticViaInstanceInspection.class);
//                add(AliDeprecationInspection.class);
//                add(MapOrSetKeyShouldOverrideHashCodeEqualsInspection.class);
//                add(AliAccessToNonThreadSafeStaticFieldFromInstanceInspection.class);
//                add(AliArrayNamingShouldHaveBracketInspection.class);
//                add(AliControlFlowStatementWithoutBracesInspection.class);
//                add(AliEqualsAvoidNullInspection.class);
//                add(AliLongLiteralsEndingWithLowercaseLInspection.class);
//                add(AliWrapperTypeEqualityInspection.class);
            }};

    static {
        I18nResources.changeLanguage(P3cConfig.getInstance().locale);
        Thread.currentThread().setContextClassLoader(AliLocalInspectionToolProvider.class.getClassLoader());
        initPmdInspection();
//        initNativeInspection();
    }

    public static List<Class<?>> getInspectionClasses() {
        List<Class<?>> classes = new ArrayList<Class<?>>(CLASS_LIST);
        return classes;
    }


    public static Map<String, RuleInfo> getRuleInfoMap() {
        return ruleInfoMap;
    }


    public static ShouldInspectChecker javaShouldInspectChecker = new ShouldInspectChecker() {
        @Override
        public Boolean shouldInspect(File file) {
            return true;
        }
    };


    public static void initNativeInspection() {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(DelegateLocalInspectionTool.class));
        try {
            for (Class<?> it : nativeInspectionToolClass) {
                pool.insertClassPath(new ClassClassPath(it));
                CtClass cc = pool.get(DelegateLocalInspectionTool.class.getName());
                cc.setName("Delegate" + it.getSimpleName());
                CtField ctField = cc.getField("forJavassist");
                cc.removeField(ctField);
                CtClass itClass = pool.get(it.getName());
                CtClass toolClass = pool.get(LocalInspectionTool.class.getName());
                CtField newField = new CtField(toolClass, "forJavassist", cc);
                cc.addField(newField, CtField.Initializer.byNew(itClass));
                CLASS_LIST.add(cc.toClass());
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
    }

    public static void initPmdInspection() {
        for (RuleInfo ri : newRuleInfos()) {
            ruleNames.add(ri.getRule().getName());
            ruleInfoMap.put(ri.getRule().getName(), ri);
        }
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(DelegatePmdInspection.class));
        try {
            for (RuleInfo ruleInfo : ruleInfoMap.values()) {
                String ruleName = ruleInfo.getRule().getName();
                CtClass cc = pool.get(DelegatePmdInspection.class.getName());
                cc.setName(ruleInfo.getRule().getName() + "Inspection");
                CtField ctField = cc.getField("ruleName");
                cc.removeField(ctField);
                String value = "\"" + ruleInfo.getRule().getName() + "\"";
                CtField newField = CtField.make("private String ruleName =" + value + ";", cc);
                cc.addField(newField, value);

                byte[] byteArr = cc.toBytecode();
                FileOutputStream fos = new FileOutputStream(new File("D://" + cc.getName() + ".class"));
                fos.write(byteArr);
                fos.close();
                CLASS_LIST.add(cc.toClass());
            }

        } catch (NotFoundException e) {
            LOGGER.error(e);
        } catch (CannotCompileException e) {
            LOGGER.error(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<RuleInfo> newRuleInfos() {
        List<RuleInfo> result = Lists.newArrayList();
        result.addAll(processForRuleSet("java/ali-pmd", new ShouldInspectChecker() {
            @Override
            public Boolean shouldInspect(File file) {
                return true;
            }
        }));
        result.addAll(processForRuleSet("vm/ali-other", new ShouldInspectChecker() {

            @Override
            public Boolean shouldInspect(File file) {
                return true;
            }
        }));
        return result;
    }

    private static List<RuleInfo> processForRuleSet(String ruleSetName, ShouldInspectChecker shouldInspectChecker) {
        if (shouldInspectChecker ==null){
            LOGGER.error("shouldInspectChecker is null 11");
        }
        RuleSetFactory factory = new RuleSetFactory();
        List<RuleInfo> result = Lists.newArrayList();
        try {
            RuleSet ruleSet = factory.createRuleSet(ruleSetName.replace("/", "-"));
            for (Rule it : ruleSet.getRules()) {
                result.add(new RuleInfo(it, shouldInspectChecker));
            }
        } catch (RuleSetNotFoundException e) {
            LOGGER.error(String.format("rule set %s not found for", ruleSetName));
        }

        return result;
    }
}
