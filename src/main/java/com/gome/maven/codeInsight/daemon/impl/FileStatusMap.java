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

package com.gome.maven.codeInsight.daemon.impl;

import com.gome.maven.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory;
import com.gome.maven.codeHighlighting.Pass;
import com.gome.maven.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.gome.maven.codeInsight.daemon.ProblemHighlightFilter;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.RangeMarker;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.util.ConcurrencyUtil;
import com.gome.maven.util.containers.WeakHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectFunction;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FileStatusMap implements Disposable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.daemon.impl.FileStatusMap");
    private final Project myProject;
    private final Map<Document,FileStatus> myDocumentToStatusMap = new WeakHashMap<Document, FileStatus>(); // all dirty if absent
    private volatile boolean myAllowDirt = true;

    public FileStatusMap( Project project) {
        myProject = project;
    }

    @Override
    public void dispose() {
        // clear dangling references to PsiFiles/Documents. SCR#10358
        markAllFilesDirty();
    }

    public static TextRange getDirtyTextRange( Editor editor, int passId) {
        Document document = editor.getDocument();

        FileStatusMap me = DaemonCodeAnalyzerEx.getInstanceEx(editor.getProject()).getFileStatusMap();
        TextRange dirtyScope = me.getFileDirtyScope(document, passId);
        if (dirtyScope == null) return null;
        TextRange documentRange = TextRange.from(0, document.getTextLength());
        return documentRange.intersection(dirtyScope);
    }

    public void setErrorFoundFlag( Project project,  Document document, boolean errorFound) {
        //GHP has found error. Flag is used by ExternalToolPass to decide whether to run or not
        synchronized(myDocumentToStatusMap) {
            FileStatus status = myDocumentToStatusMap.get(document);
            if (status == null){
                if (!errorFound) return;
                status = new FileStatus(project);
                myDocumentToStatusMap.put(document, status);
            }
            status.errorFound = errorFound;
        }
    }

    public boolean wasErrorFound( Document document) {
        synchronized(myDocumentToStatusMap) {
            FileStatus status = myDocumentToStatusMap.get(document);
            return status != null && status.errorFound;
        }
    }

    private static class FileStatus {
        private boolean defensivelyMarked; // file marked dirty without knowledge of specific dirty region. Subsequent markScopeDirty can refine dirty scope, not extend it
        private boolean wolfPassFinished;
        // if contains the special value "WHOLE_FILE_MARKER" then the corresponding range is (0, document length)
        private final TIntObjectHashMap<RangeMarker> dirtyScopes = new TIntObjectHashMap<RangeMarker>();
        private boolean errorFound;

        private FileStatus( Project project) {
            markWholeFileDirty(project);
        }

        private void markWholeFileDirty( Project project) {
            setDirtyScope(Pass.UPDATE_ALL, WHOLE_FILE_DIRTY_MARKER);
            setDirtyScope(Pass.EXTERNAL_TOOLS, WHOLE_FILE_DIRTY_MARKER);
            setDirtyScope(Pass.LOCAL_INSPECTIONS, WHOLE_FILE_DIRTY_MARKER);
            TextEditorHighlightingPassRegistrarEx registrar = (TextEditorHighlightingPassRegistrarEx) TextEditorHighlightingPassRegistrar.getInstance(project);
            for(DirtyScopeTrackingHighlightingPassFactory factory: registrar.getDirtyScopeTrackingFactories()) {
                setDirtyScope(factory.getPassId(), WHOLE_FILE_DIRTY_MARKER);
            }
        }

        private boolean allDirtyScopesAreNull() {
            for (Object o : dirtyScopes.getValues()) {
                if (o != null) return false;
            }
            return true;
        }

        private void combineScopesWith( final TextRange scope, final int fileLength,  final Document document) {
            dirtyScopes.transformValues(new TObjectFunction<RangeMarker, RangeMarker>() {
                @Override
                public RangeMarker execute(RangeMarker oldScope) {
                    RangeMarker newScope = combineScopes(oldScope, scope, fileLength, document);
                    if (newScope != oldScope && oldScope != null) {
                        oldScope.dispose();
                    }
                    return newScope;
                }
            });
        }

        @Override
        public String toString() {
             final StringBuilder s = new StringBuilder();
            s.append("defensivelyMarked = ").append(defensivelyMarked);
            s.append("; wolfPassFinfished = ").append(wolfPassFinished);
            s.append("; errorFound = ").append(errorFound);
            s.append("; dirtyScopes: (");
            dirtyScopes.forEachEntry(new TIntObjectProcedure<RangeMarker>() {
                @Override
                public boolean execute(int passId, RangeMarker rangeMarker) {
                    s.append(" pass: ").append(passId).append(" -> ").append(rangeMarker == WHOLE_FILE_DIRTY_MARKER ? "Whole file" : rangeMarker).append(";");
                    return true;
                }
            });
            s.append(")");
            return s.toString();
        }

        private void setDirtyScope(int passId, RangeMarker scope) {
            RangeMarker marker = dirtyScopes.get(passId);
            if (marker != scope) {
                if (marker != null) {
                    marker.dispose();
                }
                dirtyScopes.put(passId, scope);
            }
        }
    }

    public void markAllFilesDirty() {
        assertAllowModifications();
        LOG.debug("********************************* Mark all dirty");
        synchronized (myDocumentToStatusMap) {
            myDocumentToStatusMap.clear();
        }
    }

    private void assertAllowModifications() {
        try {
            assert myAllowDirt;
        }
        finally {
            myAllowDirt = true; //give next test a chance
        }
    }

    public void markFileUpToDate( Document document, int passId) {
        synchronized(myDocumentToStatusMap){
            FileStatus status = myDocumentToStatusMap.get(document);
            if (status == null){
                status = new FileStatus(myProject);
                myDocumentToStatusMap.put(document, status);
            }
            status.defensivelyMarked=false;
            if (passId == Pass.WOLF) {
                status.wolfPassFinished = true;
            }
            else if (status.dirtyScopes.containsKey(passId)) {
                status.setDirtyScope(passId, null);
            }
        }
    }

    /**
     * @return null for processed file, whole file for untouched or entirely dirty file, range(usually code block) for dirty region (optimization)
     */
    
    public TextRange getFileDirtyScope( Document document, int passId) {
        synchronized(myDocumentToStatusMap){
            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
            if (!ProblemHighlightFilter.shouldHighlightFile(file)) return null;
            FileStatus status = myDocumentToStatusMap.get(document);
            if (status == null){
                return file == null ? null : file.getTextRange();
            }
            if (status.defensivelyMarked) {
                status.markWholeFileDirty(myProject);
                status.defensivelyMarked = false;
            }
            LOG.assertTrue(status.dirtyScopes.containsKey(passId), "Unknown pass " + passId);
            RangeMarker marker = status.dirtyScopes.get(passId);
            return marker == null ? null : marker.isValid() ? TextRange.create(marker) : new TextRange(0, document.getTextLength());
        }
    }

    public void markFileScopeDirtyDefensively( PsiFile file) {
        assertAllowModifications();
        if (LOG.isDebugEnabled()) {
            LOG.debug("********************************* Mark dirty file defensively: "+file.getName());
        }
        // mark whole file dirty in case no subsequent PSI events will come, but file requires rehighlighting nevertheless
        // e.g. in the case of quick typing/backspacing char
        synchronized(myDocumentToStatusMap){
            Document document = PsiDocumentManager.getInstance(myProject).getCachedDocument(file);
            if (document == null) return;
            FileStatus status = myDocumentToStatusMap.get(document);
            if (status == null) return; // all dirty already
            status.defensivelyMarked = true;
        }
    }

    public void markFileScopeDirty( Document document,  TextRange scope, int fileLength) {
        assertAllowModifications();
        if (LOG.isDebugEnabled()) {
            LOG.debug("********************************* Mark dirty: "+scope);
        }
        synchronized(myDocumentToStatusMap) {
            FileStatus status = myDocumentToStatusMap.get(document);
            if (status == null) return; // all dirty already
            if (status.defensivelyMarked) {
                status.defensivelyMarked = false;
            }
            status.combineScopesWith(scope, fileLength, document);
        }
    }

    
    private static RangeMarker combineScopes(RangeMarker old,  TextRange scope, int textLength,  Document document) {
        if (old == null) {
            if (scope.equalsToRange(0, textLength)) return WHOLE_FILE_DIRTY_MARKER;
            return document.createRangeMarker(scope);
        }
        if (old == WHOLE_FILE_DIRTY_MARKER) return old;
        TextRange oldRange = TextRange.create(old);
        TextRange union = scope.union(oldRange);
        if (old.isValid() && union.equals(oldRange)) {
            return old;
        }
        if (union.getEndOffset() > textLength) {
            union = union.intersection(new TextRange(0, textLength));
        }
        assert union != null;
        return document.createRangeMarker(union);
    }

    public boolean allDirtyScopesAreNull( Document document) {
        synchronized (myDocumentToStatusMap) {
            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
            if (!ProblemHighlightFilter.shouldHighlightFile(file)) return true;

            FileStatus status = myDocumentToStatusMap.get(document);
            return status != null && !status.defensivelyMarked && status.wolfPassFinished && status.allDirtyScopesAreNull();
        }
    }

    
    public void assertAllDirtyScopesAreNull( Document document) {
        synchronized (myDocumentToStatusMap) {
            FileStatus status = myDocumentToStatusMap.get(document);
            assert status != null && !status.defensivelyMarked && status.wolfPassFinished && status.allDirtyScopesAreNull() : status;
        }
    }

    
    void allowDirt(boolean allow) {
        myAllowDirt = allow;
    }

    private static final RangeMarker WHOLE_FILE_DIRTY_MARKER = new RangeMarker(){
        
        @Override
        public Document getDocument() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getStartOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getEndOffset() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public void setGreedyToLeft(boolean greedy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setGreedyToRight(boolean greedy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGreedyToRight() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGreedyToLeft() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispose() {
            // ignore
        }

        @Override
        public <T> T getUserData( Key<T> key) {
            throw null;
        }

        @Override
        public <T> void putUserData( Key<T> key,  T value) {
            throw new UnsupportedOperationException();
        }
    };

    // logging
    private static final ConcurrentMap<Thread, Integer> threads = new ConcurrentHashMap<Thread, Integer>();
    private static int getThreadNum() {
        return ConcurrencyUtil.cacheOrGet(threads, Thread.currentThread(), threads.size());
    }
    private static final StringBuffer log = new StringBuffer();
    private static final boolean IN_TESTS = ApplicationManager.getApplication().isUnitTestMode();
    public static void log(  Object... info) {
        if (IN_TESTS) {
            if (log.length() > 10000) {
                log.replace(0, log.length()-5000, "");
            }
            String s = StringUtil.repeatSymbol(' ', getThreadNum() * 4) + Arrays.asList(info) + "\n";
            log.append(s);
        }
    }
    
    static String getAndClearLog() {
        String l = log.toString();
        log.setLength(0);
        return l;
    }
}
