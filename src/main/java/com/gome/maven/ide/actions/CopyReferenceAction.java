/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.gome.maven.ide.actions;

import com.gome.maven.codeInsight.TargetElementUtilBase;
import com.gome.maven.codeInsight.daemon.impl.IdentifierUtil;
import com.gome.maven.codeInsight.highlighting.HighlightManager;
import com.gome.maven.ide.IdeBundle;
import com.gome.maven.ide.dnd.FileCopyPasteUtil;
import com.gome.maven.openapi.actionSystem.ActionPlaces;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.colors.EditorColors;
import com.gome.maven.openapi.editor.colors.EditorColorsManager;
import com.gome.maven.openapi.editor.markup.TextAttributes;
import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.ide.CopyPasteManager;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.DumbService;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ProjectRootManager;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.wm.WindowManager;
import com.gome.maven.openapi.wm.ex.StatusBarEx;
import com.gome.maven.psi.*;
import com.gome.maven.util.*;
import com.gome.maven.util.containers.ContainerUtil;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexey
 */
public class CopyReferenceAction extends DumbAwareAction {
    public static final DataFlavor ourFlavor = FileCopyPasteUtil.createJvmDataFlavor(MyTransferable.class);

    public CopyReferenceAction() {
        super();
        setEnabledInModalContext(true);
        setInjectedContext(true);
    }

    @Override
    public void update(AnActionEvent e) {
        boolean plural = false;
        boolean enabled;

        DataContext dataContext = e.getDataContext();
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        if (editor != null && FileDocumentManager.getInstance().getFile(editor.getDocument()) != null) {
            enabled = true;
        }
        else {
            List<PsiElement> elements = getElementsToCopy(editor, dataContext);
            enabled = !elements.isEmpty();
            plural = elements.size() > 1;
        }

        e.getPresentation().setEnabled(enabled);
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setVisible(enabled);
        }
        else {
            e.getPresentation().setVisible(true);
        }
        e.getPresentation().setText(plural ? "Cop&y References" : "Cop&y Reference");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        List<PsiElement> elements = getElementsToCopy(editor, dataContext);

        if (!doCopy(elements, project, editor) && editor != null && project != null) {
            Document document = editor.getDocument();
            PsiFile file = PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
            if (file != null) {
                String toCopy = getFileFqn(file) + ":" + (editor.getCaretModel().getLogicalPosition().line + 1);
                CopyPasteManager.getInstance().setContents(new StringSelection(toCopy));
                setStatusBarText(project, toCopy + " has been copied");
            }
            return;
        }

        HighlightManager highlightManager = HighlightManager.getInstance(project);
        EditorColorsManager manager = EditorColorsManager.getInstance();
        TextAttributes attributes = manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        if (elements.size() == 1 && editor != null && project != null) {
            PsiElement element = elements.get(0);
            PsiElement nameIdentifier = IdentifierUtil.getNameIdentifier(element);
            if (nameIdentifier != null) {
                highlightManager.addOccurrenceHighlights(editor, new PsiElement[]{nameIdentifier}, attributes, true, null);
            } else {
                PsiReference reference = TargetElementUtilBase.findReference(editor, editor.getCaretModel().getOffset());
                if (reference != null) {
                    highlightManager.addOccurrenceHighlights(editor, new PsiReference[]{reference}, attributes, true, null);
                } else if (element != PsiDocumentManager.getInstance(project).getCachedPsiFile(editor.getDocument())) {
                    highlightManager.addOccurrenceHighlights(editor, new PsiElement[]{element}, attributes, true, null);
                }
            }
        }
    }

    
    private static List<PsiElement> getElementsToCopy( final Editor editor, final DataContext dataContext) {
        List<PsiElement> elements = ContainerUtil.newArrayList();
        if (editor != null) {
            PsiReference reference = TargetElementUtilBase.findReference(editor);
            if (reference != null) {
                ContainerUtil.addIfNotNull(elements, reference.getElement());
            }
        }

        if (elements.isEmpty()) {
            ContainerUtil.addIfNotNull(elements, CommonDataKeys.PSI_ELEMENT.getData(dataContext));
        }

        if (elements.isEmpty() && editor == null) {
            final Project project = CommonDataKeys.PROJECT.getData(dataContext);
            VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);
            if (project != null && files != null) {
                for (VirtualFile file : files) {
                    ContainerUtil.addIfNotNull(elements, PsiManager.getInstance(project).findFile(file));
                }
            }
        }

        return ContainerUtil.mapNotNull(elements, new Function<PsiElement, PsiElement>() {
            @Override
            public PsiElement fun(PsiElement element) {
                return element instanceof PsiFile && !((PsiFile)element).getViewProvider().isPhysical() ? null : adjustElement(element);
            }
        });
    }

    private static PsiElement adjustElement(PsiElement element) {
        for (QualifiedNameProvider provider : Extensions.getExtensions(QualifiedNameProvider.EP_NAME)) {
            PsiElement adjustedElement = provider.adjustElementToCopy(element);
            if (adjustedElement != null) return adjustedElement;
        }
        return element;
    }

    public static boolean doCopy(final PsiElement element, final Project project) {
        return doCopy(Arrays.asList(element), project, null);
    }

    private static boolean doCopy(List<PsiElement> elements,  final Project project,  Editor editor) {
        if (elements.isEmpty()) return false;

        List<String> fqns = ContainerUtil.newArrayList();
        for (PsiElement element : elements) {
            String fqn = elementToFqn(element, editor);
            if (fqn == null) return false;

            fqns.add(fqn);
        }

        String toCopy = StringUtil.join(fqns, "\n");

        CopyPasteManager.getInstance().setContents(new MyTransferable(toCopy));

        setStatusBarText(project, IdeBundle.message("message.reference.to.fqn.has.been.copied", toCopy));
        return true;
    }

    private static void setStatusBarText(Project project, String message) {
        if (project != null) {
            final StatusBarEx statusBar = (StatusBarEx)WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        }
    }

    private static class MyTransferable implements Transferable {
        private final String fqn;

        public MyTransferable(String fqn) {
            this.fqn = fqn;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ourFlavor, DataFlavor.stringFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return ArrayUtilRt.find(getTransferDataFlavors(), flavor) != -1;
        }

        @Override
        
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return fqn;
            }
            return null;
        }
    }

    
    public static String elementToFqn( final PsiElement element) {
        return elementToFqn(element, null);
    }

    
    private static String elementToFqn( final PsiElement element,  Editor editor) {
        String result = getQualifiedNameFromProviders(element);
        if (result != null) return result;

        if (editor != null) { //IDEA-70346
            PsiReference reference = TargetElementUtilBase.findReference(editor, editor.getCaretModel().getOffset());
            if (reference != null) {
                result = getQualifiedNameFromProviders(reference.resolve());
                if (result != null) return result;
            }
        }

        if (element instanceof PsiFile) {
            return FileUtil.toSystemIndependentName(getFileFqn((PsiFile)element));
        }
        if (element instanceof PsiDirectory) {
            return FileUtil.toSystemIndependentName(getVirtualFileFqn(((PsiDirectory)element).getVirtualFile(), element.getProject()));
        }

        return null;
    }

    
    private static String getQualifiedNameFromProviders( PsiElement element) {
        if (element == null) return null;
        DumbService.getInstance(element.getProject()).setAlternativeResolveEnabled(true);
        try {
            for (QualifiedNameProvider provider : Extensions.getExtensions(QualifiedNameProvider.EP_NAME)) {
                String result = provider.getQualifiedName(element);
                if (result != null) return result;
            }
        }
        finally {
            DumbService.getInstance(element.getProject()).setAlternativeResolveEnabled(false);
        }
        return null;
    }

    
    private static String getFileFqn(final PsiFile file) {
        final VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile == null ? file.getName() : getVirtualFileFqn(virtualFile, file.getProject());
    }

    private static String getVirtualFileFqn( VirtualFile virtualFile,  Project project) {
        final LogicalRoot logicalRoot = LogicalRootsManager.getLogicalRootsManager(project).findLogicalRoot(virtualFile);
        if (logicalRoot != null && logicalRoot.getVirtualFile() != null) {
            return ObjectUtils.assertNotNull(VfsUtilCore.getRelativePath(virtualFile, logicalRoot.getVirtualFile(), '/'));
        }

        final VirtualFile contentRoot = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(virtualFile);
        if (contentRoot != null) {
            return ObjectUtils.assertNotNull(VfsUtilCore.getRelativePath(virtualFile, contentRoot, '/'));
        }
        return virtualFile.getPath();
    }
}
