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
package com.gome.maven.codeInspection;

import com.gome.maven.analysis.AnalysisScope;
import com.gome.maven.codeInsight.daemon.impl.Divider;
import com.gome.maven.codeInspection.ex.GlobalInspectionToolWrapper;
import com.gome.maven.codeInspection.ex.InspectionToolWrapper;
import com.gome.maven.codeInspection.ex.LocalInspectionToolWrapper;
import com.gome.maven.codeInspection.reference.RefElement;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.reference.RefManagerImpl;
import com.gome.maven.codeInspection.reference.RefVisitor;
import com.gome.maven.concurrency.JobLauncher;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.EmptyProgressIndicator;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.ProgressManager;
import com.gome.maven.openapi.project.DumbAware;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.openapi.util.ProperTextRange;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.*;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.MultiMap;
import com.gome.maven.util.containers.SmartHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InspectionEngine {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInspection.InspectionEngine");

    
    public static PsiElementVisitor createVisitorAndAcceptElements( LocalInspectionTool tool,
                                                                    ProblemsHolder holder,
                                                                   boolean isOnTheFly,
                                                                    LocalInspectionToolSession session,
                                                                    List<PsiElement> elements,
                                                                    Collection<String> languages) {
        PsiElementVisitor visitor = tool.buildVisitor(holder, isOnTheFly, session);
        //noinspection ConstantConditions
        if(visitor == null) {
            LOG.error("Tool " + tool + " must not return null from the buildVisitor() method");
        }
        assert !(visitor instanceof PsiRecursiveElementVisitor || visitor instanceof PsiRecursiveElementWalkingVisitor)
                : "The visitor returned from LocalInspectionTool.buildVisitor() must not be recursive. "+tool;

        tool.inspectionStarted(session, isOnTheFly);
        acceptElements(elements, visitor, languages);
        return visitor;
    }

    public static void acceptElements( List<PsiElement> elements,
                                       PsiElementVisitor elementVisitor,
                                       Collection<String> languages) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, elementsSize = elements.size(); i < elementsSize; i++) {
            PsiElement element = elements.get(i);
            if (languages == null || languages.contains(element.getLanguage().getID())) {
                element.accept(elementVisitor);
            }
            ProgressManager.checkCanceled();
        }
    }

    
    public static List<ProblemDescriptor> inspect( final List<LocalInspectionToolWrapper> toolWrappers,
                                                   final PsiFile file,
                                                   final InspectionManager iManager,
                                                  final boolean isOnTheFly,
                                                  boolean failFastOnAcquireReadAction,
                                                   final ProgressIndicator indicator) {
        final Map<String, List<ProblemDescriptor>> problemDescriptors = inspectEx(toolWrappers, file, iManager, isOnTheFly, failFastOnAcquireReadAction, indicator);

        final List<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>();
        for (List<ProblemDescriptor> group : problemDescriptors.values()) {
            result.addAll(group);
        }
        return result;
    }

    // public accessibility for Upsource
    // returns map (toolName -> problem descriptors)
    
    public static Map<String, List<ProblemDescriptor>> inspectEx( final List<LocalInspectionToolWrapper> toolWrappers,
                                                                  final PsiFile file,
                                                                  final InspectionManager iManager,
                                                                 final boolean isOnTheFly,
                                                                 boolean failFastOnAcquireReadAction,
                                                                  final ProgressIndicator indicator) {
        if (toolWrappers.isEmpty()) return Collections.emptyMap();
        final Map<String, List<ProblemDescriptor>> resultDescriptors = new ConcurrentHashMap<String, List<ProblemDescriptor>>();
        final List<PsiElement> elements = new ArrayList<PsiElement>();

        TextRange range = file.getTextRange();
        final LocalInspectionToolSession session = new LocalInspectionToolSession(file, range.getStartOffset(), range.getEndOffset());
        Divider.divideInsideAndOutside(file, range.getStartOffset(), range.getEndOffset(), range, elements, new ArrayList<ProperTextRange>(),
                Collections.<PsiElement>emptyList(), Collections.<ProperTextRange>emptyList(), true, Conditions.<PsiFile>alwaysTrue());

        MultiMap<LocalInspectionToolWrapper, String> toolToLanguages = getToolsForElements(toolWrappers, DumbService.isDumb(file.getProject()), elements, Collections.<PsiElement>emptyList());
        List<Map.Entry<LocalInspectionToolWrapper, Collection<String>>> entries = new ArrayList<Map.Entry<LocalInspectionToolWrapper, Collection<String>>>(toolToLanguages.entrySet());
        Processor<Map.Entry<LocalInspectionToolWrapper, Collection<String>>> processor = new Processor<Map.Entry<LocalInspectionToolWrapper, Collection<String>>>() {
            @Override
            public boolean process(final Map.Entry<LocalInspectionToolWrapper, Collection<String>> entry) {
                ProblemsHolder holder = new ProblemsHolder(iManager, file, isOnTheFly);
                final LocalInspectionTool tool = entry.getKey().getTool();
                Collection<String> languages = entry.getValue();
                createVisitorAndAcceptElements(tool, holder, isOnTheFly, session, elements, languages);

                tool.inspectionFinished(session, holder);

                if (holder.hasResults()) {
                    resultDescriptors.put(tool.getShortName(), ContainerUtil.filter(holder.getResults(), new Condition<ProblemDescriptor>() {
                        @Override
                        public boolean value(ProblemDescriptor descriptor) {
                            PsiElement element = descriptor.getPsiElement();
                            return element == null || !SuppressionUtil.inspectionResultSuppressed(element, tool);
                        }
                    }));
                }

                return true;
            }
        };
        JobLauncher.getInstance().invokeConcurrentlyUnderProgress(entries, indicator, failFastOnAcquireReadAction, processor);

        return resultDescriptors;
    }

    
    public static List<ProblemDescriptor> runInspectionOnFile( final PsiFile file,
                                                               InspectionToolWrapper toolWrapper,
                                                               final GlobalInspectionContext inspectionContext) {
        final InspectionManager inspectionManager = InspectionManager.getInstance(file.getProject());
        toolWrapper.initialize(inspectionContext);
        RefManagerImpl refManager = (RefManagerImpl)inspectionContext.getRefManager();
        refManager.inspectionReadActionStarted();
        try {
            if (toolWrapper instanceof LocalInspectionToolWrapper) {
                return inspect(Collections.singletonList((LocalInspectionToolWrapper)toolWrapper), file, inspectionManager, false, false, new EmptyProgressIndicator());
            }
            if (toolWrapper instanceof GlobalInspectionToolWrapper) {
                final GlobalInspectionTool globalTool = ((GlobalInspectionToolWrapper)toolWrapper).getTool();
                final List<ProblemDescriptor> descriptors = new ArrayList<ProblemDescriptor>();
                if (globalTool instanceof GlobalSimpleInspectionTool) {
                    GlobalSimpleInspectionTool simpleTool = (GlobalSimpleInspectionTool)globalTool;
                    ProblemsHolder problemsHolder = new ProblemsHolder(inspectionManager, file, false);
                    ProblemDescriptionsProcessor collectProcessor = new ProblemDescriptionsProcessor() {
                        
                        @Override
                        public CommonProblemDescriptor[] getDescriptions( RefEntity refEntity) {
                            return descriptors.toArray(new CommonProblemDescriptor[descriptors.size()]);
                        }

                        @Override
                        public void ignoreElement( RefEntity refEntity) {
                            throw new RuntimeException();
                        }

                        @Override
                        public void addProblemElement( RefEntity refEntity,  CommonProblemDescriptor... commonProblemDescriptors) {
                            if (!(refEntity instanceof RefElement)) return;
                            PsiElement element = ((RefElement)refEntity).getElement();
                            convertToProblemDescriptors(element, commonProblemDescriptors, descriptors);
                        }

                        @Override
                        public RefEntity getElement( CommonProblemDescriptor descriptor) {
                            throw new RuntimeException();
                        }
                    };
                    simpleTool.checkFile(file, inspectionManager, problemsHolder, inspectionContext, collectProcessor);
                    return descriptors;
                }
                RefElement fileRef = refManager.getReference(file);
                final AnalysisScope scope = new AnalysisScope(file);
                fileRef.accept(new RefVisitor(){
                    @Override
                    public void visitElement( RefEntity elem) {
                        CommonProblemDescriptor[] elemDescriptors = globalTool.checkElement(elem, scope, inspectionManager, inspectionContext);
                        if (descriptors != null) {
                            convertToProblemDescriptors(file, elemDescriptors, descriptors);
                        }

                        for (RefEntity child : elem.getChildren()) {
                            child.accept(this);
                        }
                    }
                });
                return descriptors;
            }
        }
        finally {
            refManager.inspectionReadActionFinished();
            toolWrapper.cleanup(file.getProject());
            inspectionContext.cleanup();
        }
        return Collections.emptyList();
    }

    private static void convertToProblemDescriptors(PsiElement element,
                                                    CommonProblemDescriptor[] commonProblemDescriptors,
                                                    List<ProblemDescriptor> descriptors) {
        for (CommonProblemDescriptor common : commonProblemDescriptors) {
            if (common instanceof ProblemDescriptor) {
                descriptors.add((ProblemDescriptor)common);
            }
            else {
                ProblemDescriptorBase base =
                        new ProblemDescriptorBase(element, element, common.getDescriptionTemplate(), (LocalQuickFix[])common.getFixes(),
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false, null, false, false);
                descriptors.add(base);
            }
        }
    }

    
    public static <T extends InspectionToolWrapper> MultiMap<T, String> getToolsForElements( List<T> toolWrappers,
                                                                                            boolean checkDumbAwareness,
                                                                                             List<PsiElement> inside,
                                                                                             List<PsiElement> outside) {
        Set<Language> languages = new SmartHashSet<Language>();
        Map<String, Language> langIds = new SmartHashMap<String, Language>();
        Set<String> dialects = new SmartHashSet<String>();
        calculateDialects(inside, languages, langIds, dialects);
        calculateDialects(outside, languages, langIds, dialects);
        MultiMap<T, String> toolToLanguages = new MultiMap<T, String>() {
            
            @Override
            protected Collection<String> createCollection() {
                return new SmartHashSet<String>();
            }

            
            @Override
            protected Collection<String> createEmptyCollection() {
                return Collections.emptySet();
            }
        };
        for (T wrapper : toolWrappers) {
            ProgressManager.checkCanceled();
            String language = wrapper.getLanguage();
            if (language == null) {
                InspectionProfileEntry tool = wrapper.getTool();
                if (!checkDumbAwareness || tool instanceof DumbAware) {
                    toolToLanguages.put(wrapper, null);
                }
                continue;
            }
            Language lang = langIds.get(language);
            if (lang != null) {
                InspectionProfileEntry tool = wrapper.getTool();
                if (!checkDumbAwareness || tool instanceof DumbAware) {
                    toolToLanguages.putValue(wrapper, language);
                    if (wrapper.applyToDialects()) {
                        for (Language dialect : lang.getDialects()) {
                            toolToLanguages.putValue(wrapper, dialect.getID());
                        }
                    }
                }
            }
            else if (wrapper.applyToDialects() && dialects.contains(language)) {
                InspectionProfileEntry tool = wrapper.getTool();
                if (!checkDumbAwareness || tool instanceof DumbAware) {
                    toolToLanguages.putValue(wrapper, language);
                }
            }
        }
        return toolToLanguages;
    }

    private static void calculateDialects( List<PsiElement> inside,
                                           Set<Language> languages,
                                           Map<String, Language> langIds,
                                           Set<String> dialects) {
        for (PsiElement element : inside) {
            Language language = element.getLanguage();
            if (languages.add(language)) {
                langIds.put(language.getID(), language);
                for (Language dialect : language.getDialects()) {
                    dialects.add(dialect.getID());
                }
            }
        }
    }
}
