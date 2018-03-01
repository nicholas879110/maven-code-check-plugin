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
package com.gome.maven.codeInsight.editorActions;

import com.gome.maven.application.options.editor.WebEditorOptions;
import com.gome.maven.codeInsight.completion.XmlTagInsertHandler;
import com.gome.maven.codeInsight.lookup.LookupManager;
import com.gome.maven.codeInsight.lookup.impl.LookupImpl;
import com.gome.maven.codeInspection.htmlInspections.RenameTagBeginOrEndIntentionAction;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.html.HTMLLanguage;
import com.gome.maven.lang.injection.InjectedLanguageManager;
import com.gome.maven.lang.xhtml.XHTMLLanguage;
import com.gome.maven.lang.xml.XMLLanguage;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.command.CommandAdapter;
import com.gome.maven.openapi.command.CommandEvent;
import com.gome.maven.openapi.command.CommandProcessor;
import com.gome.maven.openapi.command.undo.UndoManager;
import com.gome.maven.openapi.components.ApplicationComponent;
import com.gome.maven.openapi.diagnostic.Attachment;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.*;
import com.gome.maven.openapi.editor.event.DocumentAdapter;
import com.gome.maven.openapi.editor.event.DocumentEvent;
import com.gome.maven.openapi.editor.event.EditorFactoryAdapter;
import com.gome.maven.openapi.editor.event.EditorFactoryEvent;
import com.gome.maven.openapi.editor.ex.DocumentEx;
import com.gome.maven.openapi.editor.impl.EditorImpl;
import com.gome.maven.openapi.fileEditor.FileDocumentManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.*;
import com.gome.maven.psi.impl.PsiDocumentManagerBase;
import com.gome.maven.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.gome.maven.util.SmartList;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.xml.util.XmlUtil;

import java.util.List;
import java.util.Set;

/**
 * @author Dennis.Ushakov
 */
public class XmlTagNameSynchronizer extends CommandAdapter implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(XmlTagNameSynchronizer.class);
    private static final Set<String> SUPPORTED_LANGUAGES = ContainerUtil.set(HTMLLanguage.INSTANCE.getID(),
            XMLLanguage.INSTANCE.getID(),
            XHTMLLanguage.INSTANCE.getID(),
            "JavaScript",
            "ECMA Script Level 4");

    private static final Key<TagNameSynchronizer> SYNCHRONIZER_KEY = Key.create("tag_name_synchronizer");
    private final FileDocumentManager myFileDocumentManager;

    public XmlTagNameSynchronizer(EditorFactory editorFactory, FileDocumentManager manager, CommandProcessor processor) {
        myFileDocumentManager = manager;
        editorFactory.addEditorFactoryListener(new EditorFactoryAdapter() {
            @Override
            public void editorCreated( EditorFactoryEvent event) {
                installSynchronizer(event.getEditor());
            }

            @Override
            public void editorReleased( EditorFactoryEvent event) {
                uninstallSynchronizer(event.getEditor());
            }
        }, ApplicationManager.getApplication());
        processor.addCommandListener(this);
    }

    public void uninstallSynchronizer(final Editor editor) {
        final Document document = editor.getDocument();
        final TagNameSynchronizer synchronizer = findSynchronizer(document);
        if (synchronizer != null) {
            synchronizer.clearMarkers();
        }
        document.putUserData(SYNCHRONIZER_KEY, null);
    }

    private void installSynchronizer(final Editor editor) {
        final Project project = editor.getProject();
        if (project == null) return;

        final Document document = editor.getDocument();
        final VirtualFile file = myFileDocumentManager.getFile(document);
        final Language language = findXmlLikeLanguage(project, file);
        if (language != null) new TagNameSynchronizer(editor, project, language);
    }

    private static Language findXmlLikeLanguage(Project project, VirtualFile file) {
        final PsiFile psiFile = file != null && file.isValid() ? PsiManager.getInstance(project).findFile(file) : null;
        if (psiFile != null) {
            for (Language language : psiFile.getViewProvider().getLanguages()) {
                if (SUPPORTED_LANGUAGES.contains(language.getID())) return language;
            }
        }
        return null;
    }

    
    @Override
    public String getComponentName() {
        return "XmlTagNameSynchronizer";
    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    
    public TagNameSynchronizer findSynchronizer(final Document document) {
        if (!WebEditorOptions.getInstance().isSyncTagEditing() || document == null) return null;
        return document.getUserData(SYNCHRONIZER_KEY);
    }

    @Override
    public void beforeCommandFinished(CommandEvent event) {
        final TagNameSynchronizer synchronizer = findSynchronizer(event.getDocument());
        if (synchronizer != null) {
            synchronizer.beforeCommandFinished();
        }
    }

    private static class TagNameSynchronizer extends DocumentAdapter {
        private final PsiDocumentManagerBase myDocumentManager;
        private final Language myLanguage;

        private enum State {INITIAL, TRACKING, APPLYING}

        private final Editor myEditor;
        private State myState = State.INITIAL;
        private final List<Couple<RangeMarker>> myMarkers = new SmartList<Couple<RangeMarker>>();

        public TagNameSynchronizer(Editor editor, Project project, Language language) {
            myEditor = editor;
            myLanguage = language;
            final Disposable disposable = ((EditorImpl)editor).getDisposable();
            final Document document = editor.getDocument();
            document.addDocumentListener(this, disposable);
            document.putUserData(SYNCHRONIZER_KEY, this);
            myDocumentManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(project);
        }

        @Override
        public void beforeDocumentChange(DocumentEvent event) {
            if (!WebEditorOptions.getInstance().isSyncTagEditing()) return;

            final Document document = event.getDocument();
            if (myState == State.APPLYING || UndoManager.getInstance(myEditor.getProject()).isUndoInProgress() ||
                    ((DocumentEx)document).isInBulkUpdate()) return;

            final int offset = event.getOffset();
            final int oldLength = event.getOldLength();
            final CharSequence fragment = event.getNewFragment();
            final int newLength = event.getNewLength();

            if (document.getUserData(XmlTagInsertHandler.ENFORCING_TAG) == Boolean.TRUE) {
                // xml completion inserts extra space after tag name to ensure correct parsing
                // we need to ignore it
                return;
            }

            for (int i = 0; i < newLength; i++) {
                if (!XmlUtil.isValidTagNameChar(fragment.charAt(i))) {
                    clearMarkers();
                    return;
                }
            }

            if (myState == State.INITIAL) {
                final PsiFile file = myDocumentManager.getPsiFile(document);
                if (file == null || myDocumentManager.getSynchronizer().isInSynchronization(document)) return;

                final SmartList<RangeMarker> leaders = new SmartList<RangeMarker>();
                for (Caret caret : myEditor.getCaretModel().getAllCarets()) {
                    final RangeMarker leader = createTagNameMarker(caret);
                    if (leader == null) {
                        for (RangeMarker marker : leaders) {
                            marker.dispose();
                        }
                        return;
                    }
                    leader.setGreedyToLeft(true);
                    leader.setGreedyToRight(true);
                    leaders.add(leader);
                }
                if (leaders.isEmpty()) return;

                if (myDocumentManager.isUncommited(document)) {
                    myDocumentManager.commitDocument(document);
                }

                for (RangeMarker leader : leaders) {
                    final RangeMarker support = findSupport(leader, file, document);
                    if (support == null) {
                        clearMarkers();
                        return;
                    }
                    support.setGreedyToLeft(true);
                    support.setGreedyToRight(true);
                    myMarkers.add(Couple.of(leader, support));
                }

                if (!fitsInMarker(offset, oldLength)) {
                    clearMarkers();
                    return;
                }

                myState = State.TRACKING;
            }
            if (myMarkers.isEmpty()) return;

            boolean fitsInMarker = fitsInMarker(offset, oldLength);
            if (!fitsInMarker || myMarkers.size() != myEditor.getCaretModel().getCaretCount()) {
                clearMarkers();
                beforeDocumentChange(event);
            }
        }

        public boolean fitsInMarker(int offset, int oldLength) {
            boolean fitsInMarker = false;
            for (Couple<RangeMarker> leaderAndSupport : myMarkers) {
                final RangeMarker leader = leaderAndSupport.first;
                if (!leader.isValid()) {
                    fitsInMarker = false;
                    break;
                }
                fitsInMarker |= offset >= leader.getStartOffset() && offset + oldLength <= leader.getEndOffset();
            }
            return fitsInMarker;
        }

        public void clearMarkers() {
            for (Couple<RangeMarker> leaderAndSupport : myMarkers) {
                leaderAndSupport.first.dispose();
                leaderAndSupport.second.dispose();
            }
            myMarkers.clear();
            myState = State.INITIAL;
        }

        private RangeMarker createTagNameMarker(Caret caret) {
            final int offset = caret.getOffset();
            final Document document = myEditor.getDocument();
            final CharSequence sequence = document.getCharsSequence();
            int start = -1;
            int end = -1;
            for (int i = offset - 1; i >= Math.max(0, offset - 50); i--) {
                try {
                    final char c = sequence.charAt(i);
                    if (c == '<' || (c == '/' && i > 0 && sequence.charAt(i - 1) == '<')) {
                        start = i + 1;
                        break;
                    }
                    if (!XmlUtil.isValidTagNameChar(c)) break;
                } catch (IndexOutOfBoundsException e) {
                    LOG.error("incorrect offset:" + i + ", initial: " + offset, new Attachment("document.txt", sequence.toString()));
                    return null;
                }
            }
            if (start < 0) return null;
            for (int i = offset; i < Math.min(document.getTextLength(), offset + 50); i++) {
                final char c = sequence.charAt(i);
                if (!XmlUtil.isValidTagNameChar(c)) {
                    end = i;
                    break;
                }
            }
            if (end < 0 || start >= end) return null;
            return document.createRangeMarker(start, end, true);
        }

        public void beforeCommandFinished() {
            if (myMarkers.isEmpty()) return;

            myState = State.APPLYING;

            final Document document = myEditor.getDocument();
            final Runnable apply = new Runnable() {
                public void run() {
                    for (Couple<RangeMarker> couple : myMarkers) {
                        final RangeMarker leader = couple.first;
                        final RangeMarker support = couple.second;
                        final String name = document.getText(new TextRange(leader.getStartOffset(), leader.getEndOffset()));
                        document.replaceString(support.getStartOffset(), support.getEndOffset(), name);
                    }
                }
            };
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    final LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(myEditor);
                    if (lookup != null) {
                        lookup.performGuardedChange(apply);
                    } else {
                        apply.run();
                    }
                }
            });

            myState = State.TRACKING;
        }

        private RangeMarker findSupport(RangeMarker leader, PsiFile file, Document document) {
            final int offset = leader.getStartOffset();
            PsiElement element = InjectedLanguageUtil.findElementAtNoCommit(file, offset);
            PsiElement support = findSupportElement(element);
            if (support == null && file.getViewProvider() instanceof MultiplePsiFilesPerDocumentFileViewProvider) {
                element = file.getViewProvider().findElementAt(offset, myLanguage);
                support = findSupportElement(element);
            }

            if (support == null) return null;

            final TextRange range = support.getTextRange();
            TextRange realRange = InjectedLanguageManager.getInstance(file.getProject()).injectedToHost(element.getContainingFile(), range);
            return document.createRangeMarker(realRange.getStartOffset(), realRange.getEndOffset(), true);
        }

        private static PsiElement findSupportElement(PsiElement element) {
            if (element == null) return null;
            PsiElement support = RenameTagBeginOrEndIntentionAction.findOtherSide(element, false);
            support = support == null || element == support ? RenameTagBeginOrEndIntentionAction.findOtherSide(element, true) : support;
            return support != null && StringUtil.equals(element.getText(), support.getText()) ? support : null;
        }
    }
}
