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
package com.gome.maven.ide.scratch;

import com.gome.maven.concurrency.JobScheduler;
import com.gome.maven.featureStatistics.FeatureUsageTracker;
import com.gome.maven.ide.util.PropertiesComponent;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.LanguageUtil;
import com.gome.maven.lang.StdLanguages;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CommonDataKeys;
import com.gome.maven.openapi.application.AccessToken;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.editor.Caret;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.JBPopupFactory;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.ui.popup.ListPopupStep;
import com.gome.maven.openapi.ui.popup.PopupStep;
import com.gome.maven.openapi.ui.popup.util.BaseListPopupStep;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VfsUtilCore;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiElement;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.ui.popup.list.ListPopupImpl;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.ui.EmptyIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ignatov
 */
public class NewScratchFileAction extends DumbAwareAction {
    public static final int MAX_VISIBLE_SIZE = 20;

    @Override
    public void update( AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(isEnabled(e));
    }

    public static boolean isEnabled( AnActionEvent e) {
        return e.getProject() != null && Registry.is("ide.scratch.enabled");
    }

    @Override
    public void actionPerformed( AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) return;

        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        Language language = getLanguageFromCaret(project, editor, file);
        String text = getSelectionText(editor);
        if (language != null && text != null) {
            openNewFile(project, language, text, false);
            return;
        }

        final Ref<Boolean> tenMinuteScratch = Ref.create(false);
        ListPopup popup = buildLanguageSelectionPopup(project, "New Scratch", language, new Consumer<Language>() {
            @Override
            public void consume( Language language) {
                openNewFile(project, language, "", tenMinuteScratch.get());
            }
        });
        setupShiftActions((ListPopupImpl)popup, tenMinuteScratch);
        popup.showCenteredInCurrentWindow(project);
    }

    
    public String getSelectionText( Editor editor) {
        if (editor == null) return null;
        return editor.getSelectionModel().getSelectedText();
    }

    
    public Language getLanguageFromCaret( Project project,
                                          Editor editor,
                                          PsiFile psiFile) {
        if (editor == null || psiFile == null) return null;
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int offset = caret.getOffset();
        PsiElement element = InjectedLanguageManager.getInstance(project).findInjectedElementAt(psiFile, offset);
        PsiFile file = element != null ? element.getContainingFile() : psiFile;
        return file.getLanguage();
    }

    public static VirtualFile openNewFile( Project project,  Language language,  String text, boolean altMode) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("scratch");
        storeLRULanguages(project, language);
        VirtualFile file = ScratchRootType.getInstance().createScratchFile(project, "scratch", language, text);
        if (file != null) {
            FileEditorManager.getInstance(project).openFile(file, true);
            if (altMode) scheduleSelfDestruct(file);
        }
        return file;
    }

    private static void storeLRULanguages( Project project,  Language language) {
        String[] values = PropertiesComponent.getInstance(project).getValues(getLRUStoreKey());
        List<String> lastUsed = ContainerUtil.newArrayListWithCapacity(5);
        lastUsed.add(language.getID());
        if (values != null) {
            for (String value : values) {
                if (!lastUsed.contains(value)) {
                    lastUsed.add(value);
                }
                if (lastUsed.size() == 5) break;
            }
        }
        PropertiesComponent.getInstance(project).setValues(getLRUStoreKey(), ArrayUtil.toStringArray(lastUsed));
    }

    
    private static List<String> restoreLRULanguages( Project project) {
        String[] values = PropertiesComponent.getInstance(project).getValues(getLRUStoreKey());
        return values == null ? ContainerUtil.<String>emptyList() : ContainerUtil.list(values);
    }

    private static String getLRUStoreKey() {
        return NewScratchFileAction.class.getName();
    }

    
    public static ListPopup buildLanguageSelectionPopup( Project project,  String title,
                                                         Language context,  final Consumer<Language> onChosen) {
        List<Language> languages = LanguageUtil.getFileLanguages();
        final List<String> ids = ContainerUtil.newArrayList(restoreLRULanguages(project));
        if (context != null) {
            ids.add(context.getID());
        }
        if (ids.isEmpty()) {
            ids.add(StdLanguages.TEXT.getID());
        }

        ContainerUtil.sort(languages, new Comparator<Language>() {
            @Override
            public int compare( Language o1,  Language o2) {
                int ind1 = ids.indexOf(o1.getID());
                int ind2 = ids.indexOf(o2.getID());
                if (ind1 == -1) ind1 = 666;
                if (ind2 == -1) ind2 = 666;
                return ind1 - ind2;
            }
        });
        BaseListPopupStep<Language> step =
                new BaseListPopupStep<Language>(title, languages) {
                    
                    @Override
                    public String getTextFor( Language value) {
                        return value.getDisplayName();
                    }

                    @Override
                    public boolean isSpeedSearchEnabled() {
                        return true;
                    }

                    @Override
                    public PopupStep onChosen(Language selectedValue, boolean finalChoice) {
                        onChosen.consume(selectedValue);
                        return null;
                    }

                    @Override
                    public Icon getIconFor( Language language) {
                        LanguageFileType associatedLanguage = language.getAssociatedFileType();
                        return associatedLanguage != null ? associatedLanguage.getIcon() : null;
                    }
                };
        step.setDefaultOptionIndex(0);

        return tweakSizeToPreferred(JBPopupFactory.getInstance().createListPopup(step));
    }

    
    private static ListPopup tweakSizeToPreferred( ListPopup popup) {
        int nameLen = 0;
        ListPopupStep step = popup.getListStep();
        List values = step.getValues();
        for (Object v : values) {
            //noinspection unchecked
            nameLen = Math.max(nameLen, step.getTextFor(v).length());
        }
        if (values.size() > MAX_VISIBLE_SIZE) {
            Dimension size = new JLabel(StringUtil.repeatSymbol('a', nameLen), EmptyIcon.ICON_16, SwingConstants.LEFT).getPreferredSize();
            size.width += 20;
            size.height *= MAX_VISIBLE_SIZE;
            popup.setSize(size);
        }
        return popup;
    }

    private static void setupShiftActions(final ListPopupImpl popup, final Ref<Boolean> tenMinuteScratch) {
        popup.registerAction("alternateMode", KeyStroke.getKeyStroke("shift pressed SHIFT"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tenMinuteScratch.set(true);
                popup.setCaption("New 10 Minute Scratch");
            }
        });

        popup.registerAction("regularMode", KeyStroke.getKeyStroke("released SHIFT"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tenMinuteScratch.set(false);
                popup.setCaption("New Scratch");
            }
        });

        popup.registerAction("invokeAlternate", KeyStroke.getKeyStroke("shift ENTER"), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.handleSelect(true);
            }
        });
    }

    private static void scheduleSelfDestruct(final VirtualFile virtualFile) {
        VfsUtilCore.virtualToIoFile(virtualFile).deleteOnExit();
        JobScheduler.getScheduler().schedule(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(NewScratchFileAction.class);
                        try {
                            virtualFile.delete(NewScratchFileAction.class);
                        }
                        catch (Exception ignored) {
                        }
                        finally {
                            token.finish();
                        }
                    }
                });
            }
        }, 10, TimeUnit.MINUTES);
    }

}