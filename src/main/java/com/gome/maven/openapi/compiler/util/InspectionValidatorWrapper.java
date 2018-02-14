/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.gome.maven.openapi.compiler.util;

import com.gome.maven.codeHighlighting.HighlightDisplayLevel;
import com.gome.maven.codeInsight.daemon.HighlightDisplayKey;
import com.gome.maven.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.gome.maven.codeInsight.daemon.impl.HighlightInfo;
import com.gome.maven.codeInspection.*;
import com.gome.maven.codeInspection.ex.LocalInspectionToolWrapper;
import com.gome.maven.compiler.options.ValidationConfiguration;
import com.gome.maven.lang.ExternalLanguageAnnotators;
import com.gome.maven.lang.StdLanguages;
import com.gome.maven.lang.annotation.Annotation;
import com.gome.maven.lang.annotation.AnnotationSession;
import com.gome.maven.lang.annotation.ExternalAnnotator;
import com.gome.maven.lang.annotation.HighlightSeverity;
import com.gome.maven.openapi.compiler.*;
import com.gome.maven.openapi.compiler.options.ExcludesConfiguration;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Computable;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.profile.codeInspection.InspectionProjectProfileManager;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.psi.xml.XmlFile;
import com.gome.maven.util.Processor;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.hash.LinkedHashMap;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author peter
 */
public class InspectionValidatorWrapper implements Validator {
    private final InspectionValidator myValidator;
    private final PsiManager myPsiManager;
    private final CompilerManager myCompilerManager;
    private final InspectionManager myInspectionManager;
    private final InspectionProjectProfileManager myProfileManager;
    private final PsiDocumentManager myPsiDocumentManager;
    private static final ThreadLocal<Boolean> ourCompilationThreads = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public InspectionValidatorWrapper(final CompilerManager compilerManager, final InspectionManager inspectionManager,
                                      final InspectionProjectProfileManager profileManager, final PsiDocumentManager psiDocumentManager,
                                      final PsiManager psiManager, final InspectionValidator validator) {
        myCompilerManager = compilerManager;
        myInspectionManager = inspectionManager;
        myProfileManager = profileManager;
        myPsiDocumentManager = psiDocumentManager;
        myPsiManager = psiManager;
        myValidator = validator;
    }

    public static boolean isCompilationThread() {
        return ourCompilationThreads.get().booleanValue();
    }

    private static List<ProblemDescriptor> runInspectionOnFile( PsiFile file,
                                                                LocalInspectionTool inspectionTool) {
        InspectionManager inspectionManager = InspectionManager.getInstance(file.getProject());
        GlobalInspectionContext context = inspectionManager.createNewGlobalContext(false);
        return InspectionEngine.runInspectionOnFile(file, new LocalInspectionToolWrapper(inspectionTool), context);
    }

    private class MyValidatorProcessingItem implements ProcessingItem {
        private final VirtualFile myVirtualFile;
        private final PsiFile myPsiFile;
        private PsiElementsValidityState myValidityState;

        public MyValidatorProcessingItem( final PsiFile psiFile) {
            myPsiFile = psiFile;
            myVirtualFile = psiFile.getVirtualFile();
        }

        @Override
        
        public VirtualFile getFile() {
            return myVirtualFile;
        }

        @Override

        public ValidityState getValidityState() {
            if (myValidityState == null) {
                myValidityState = computeValidityState();
            }
            return myValidityState;
        }

        private PsiElementsValidityState computeValidityState() {
            final PsiElementsValidityState state = new PsiElementsValidityState();
            for (PsiElement psiElement : myValidator.getDependencies(myPsiFile)) {
                state.addDependency(psiElement);
            }
            return state;
        }

        public PsiFile getPsiFile() {
            return myPsiFile;
        }
    }

    @Override
    
    public ProcessingItem[] getProcessingItems(final CompileContext context) {
        final Project project = context.getProject();
        if (project.isDefault() || !ValidationConfiguration.shouldValidate(this, context)) {
            return ProcessingItem.EMPTY_ARRAY;
        }
        final ExcludesConfiguration excludesConfiguration = ValidationConfiguration.getExcludedEntriesConfiguration(project);
        final List<ProcessingItem> items =
                DumbService.getInstance(project).runReadActionInSmartMode(new Computable<List<ProcessingItem>>() {
                    @Override
                    public List<ProcessingItem> compute() {
                        final CompileScope compileScope = context.getCompileScope();
                        if (!myValidator.isAvailableOnScope(compileScope)) return null;

                        final ArrayList<ProcessingItem> items = new ArrayList<ProcessingItem>();

                        final Processor<VirtualFile> processor = new Processor<VirtualFile>() {
                            @Override
                            public boolean process(VirtualFile file) {
                                if (!file.isValid()) {
                                    return true;
                                }

                                if (myCompilerManager.isExcludedFromCompilation(file) ||
                                        excludesConfiguration.isExcluded(file)) {
                                    return true;
                                }

                                final Module module = context.getModuleByFile(file);
                                if (module != null) {
                                    final PsiFile psiFile = myPsiManager.findFile(file);
                                    if (psiFile != null) {
                                        items.add(new MyValidatorProcessingItem(psiFile));
                                    }
                                }
                                return true;
                            }
                        };
                        ContainerUtil.process(myValidator.getFilesToProcess(myPsiManager.getProject(), context), processor);
                        return items;
                    }
                });
        if (items == null) return ProcessingItem.EMPTY_ARRAY;

        return items.toArray(new ProcessingItem[items.size()]);
    }

    @Override
    public ProcessingItem[] process(final CompileContext context, final ProcessingItem[] items) {
        context.getProgressIndicator().setText(myValidator.getProgressIndicatorText());

        final List<ProcessingItem> processedItems = new ArrayList<ProcessingItem>();
        final List<LocalInspectionTool> inspections = new ArrayList<LocalInspectionTool>();
        for (final Class aClass : myValidator.getInspectionToolClasses(context)) {
            try {
                inspections.add((LocalInspectionTool)aClass.newInstance());
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new Error(e);
            }
        }
        for (int i = 0; i < items.length; i++) {
            final MyValidatorProcessingItem item = (MyValidatorProcessingItem)items[i];
            context.getProgressIndicator().checkCanceled();
            context.getProgressIndicator().setFraction((double)i / items.length);

            try {
                ourCompilationThreads.set(Boolean.TRUE);

                if (checkFile(inspections, item.getPsiFile(), context)) {
                    processedItems.add(item);
                }
            }
            finally {
                ourCompilationThreads.set(Boolean.FALSE);
            }
        }

        return processedItems.toArray(new ProcessingItem[processedItems.size()]);
    }

    private boolean checkFile(List<LocalInspectionTool> inspections, final PsiFile file, final CompileContext context) {
        boolean hasErrors = false;
        if (!checkUnderReadAction(file, context, new Computable<Map<ProblemDescriptor, HighlightDisplayLevel>>() {
            @Override
            public Map<ProblemDescriptor, HighlightDisplayLevel> compute() {
                return myValidator.checkAdditionally(file);
            }
        })) {
            hasErrors = true;
        }

        if (!checkUnderReadAction(file, context, new Computable<Map<ProblemDescriptor, HighlightDisplayLevel>>() {
            @Override
            public Map<ProblemDescriptor, HighlightDisplayLevel> compute() {
                if (file instanceof XmlFile) {
                    return runXmlFileSchemaValidation((XmlFile)file);
                }
                return Collections.emptyMap();
            }
        })) {
            hasErrors = true;
        }


        final InspectionProfile inspectionProfile = myProfileManager.getInspectionProfile();
        for (final LocalInspectionTool inspectionTool : inspections) {
            if (!checkUnderReadAction(file, context, new Computable<Map<ProblemDescriptor, HighlightDisplayLevel>>() {
                @Override
                public Map<ProblemDescriptor, HighlightDisplayLevel> compute() {
                    if (getHighlightDisplayLevel(inspectionTool, inspectionProfile, file) != HighlightDisplayLevel.DO_NOT_SHOW) {
                        return runInspectionTool(file, inspectionTool, getHighlightDisplayLevel(inspectionTool, inspectionProfile, file)
                        );
                    }
                    return Collections.emptyMap();
                }
            })) {
                hasErrors = true;
            }
        }
        return !hasErrors;
    }

    private boolean checkUnderReadAction(final PsiFile file, final CompileContext context, final Computable<Map<ProblemDescriptor, HighlightDisplayLevel>> runnable) {
        return DumbService.getInstance(context.getProject()).runReadActionInSmartMode(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                if (!file.isValid()) return false;

                final Document document = myPsiDocumentManager.getCachedDocument(file);
                if (document != null && myPsiDocumentManager.isUncommited(document)) {
                    final String url = file.getViewProvider().getVirtualFile().getUrl();
                    context.addMessage(CompilerMessageCategory.WARNING, CompilerBundle.message("warning.text.file.has.been.changed"), url, -1, -1);
                    return false;
                }

                if (reportProblems(context, runnable.compute())) return false;
                return true;
            }
        });
    }

    private boolean reportProblems(CompileContext context, Map<ProblemDescriptor, HighlightDisplayLevel> problemsMap) {
        if (problemsMap.isEmpty()) {
            return false;
        }

        boolean errorsReported = false;
        for (Map.Entry<ProblemDescriptor, HighlightDisplayLevel> entry : problemsMap.entrySet()) {
            ProblemDescriptor problemDescriptor = entry.getKey();
            final PsiElement element = problemDescriptor.getPsiElement();
            final PsiFile psiFile = element.getContainingFile();
            if (psiFile == null) continue;

            final VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) continue;

            final CompilerMessageCategory category = myValidator.getCategoryByHighlightDisplayLevel(entry.getValue(), virtualFile, context);
            final Document document = myPsiDocumentManager.getDocument(psiFile);

            final int offset = problemDescriptor.getStartElement().getTextOffset();
            assert document != null;
            final int line = document.getLineNumber(offset);
            final int column = offset - document.getLineStartOffset(line);
            context.addMessage(category, problemDescriptor.getDescriptionTemplate(), virtualFile.getUrl(), line + 1, column + 1);
            if (CompilerMessageCategory.ERROR == category) {
                errorsReported = true;
            }
        }
        return errorsReported;
    }

    private static Map<ProblemDescriptor, HighlightDisplayLevel> runInspectionTool(final PsiFile file,
                                                                                   final LocalInspectionTool inspectionTool,
                                                                                   final HighlightDisplayLevel level) {
        Map<ProblemDescriptor, HighlightDisplayLevel> problemsMap = new LinkedHashMap<ProblemDescriptor, HighlightDisplayLevel>();
        for (ProblemDescriptor descriptor : runInspectionOnFile(file, inspectionTool)) {
            problemsMap.put(descriptor, level);
        }
        return problemsMap;
    }

    private static HighlightDisplayLevel getHighlightDisplayLevel(final LocalInspectionTool inspectionTool,
                                                                  final InspectionProfile inspectionProfile, PsiElement file) {
        final HighlightDisplayKey key = HighlightDisplayKey.find(inspectionTool.getShortName());
        return inspectionProfile.isToolEnabled(key, file) ? inspectionProfile.getErrorLevel(key, file) : HighlightDisplayLevel.DO_NOT_SHOW;
    }

    private Map<ProblemDescriptor, HighlightDisplayLevel> runXmlFileSchemaValidation( XmlFile xmlFile) {
        final AnnotationHolderImpl holder = new AnnotationHolderImpl(new AnnotationSession(xmlFile));

        final List<ExternalAnnotator> annotators = ExternalLanguageAnnotators.allForFile(StdLanguages.XML, xmlFile);
        for (ExternalAnnotator annotator : annotators) {
            Object initial = annotator.collectInformation(xmlFile);
            if (initial != null) {
                Object result = annotator.doAnnotate(initial);
                if (result != null) {
                    annotator.apply(xmlFile, result, holder);
                }
            }
        }

        if (!holder.hasAnnotations()) return Collections.emptyMap();

        Map<ProblemDescriptor, HighlightDisplayLevel> problemsMap = new LinkedHashMap<ProblemDescriptor, HighlightDisplayLevel>();
        for (final Annotation annotation : holder) {
            final HighlightInfo info = HighlightInfo.fromAnnotation(annotation);
            if (info.getSeverity() == HighlightSeverity.INFORMATION) continue;

            final PsiElement startElement = xmlFile.findElementAt(info.startOffset);
            final PsiElement endElement = info.startOffset == info.endOffset ? startElement : xmlFile.findElementAt(info.endOffset - 1);
            if (startElement == null || endElement == null) continue;

            final ProblemDescriptor descriptor =
                    myInspectionManager.createProblemDescriptor(startElement, endElement, info.getDescription(), ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            false);
            final HighlightDisplayLevel level = info.getSeverity() == HighlightSeverity.ERROR? HighlightDisplayLevel.ERROR: HighlightDisplayLevel.WARNING;
            problemsMap.put(descriptor, level);
        }
        return problemsMap;
    }


    @Override
    
    public String getDescription() {
        return myValidator.getDescription();
    }

    @Override
    public boolean validateConfiguration(final CompileScope scope) {
        return true;
    }

    @Override
    public ValidityState createValidityState(final DataInput in) throws IOException {
        return PsiElementsValidityState.load(in);
    }

}
