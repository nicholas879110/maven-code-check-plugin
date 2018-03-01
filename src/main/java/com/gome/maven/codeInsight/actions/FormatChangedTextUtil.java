/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.gome.maven.codeInsight.actions;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ReadAction;
import com.gome.maven.openapi.application.Result;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.impl.EditorFactoryImpl;
import com.gome.maven.openapi.editor.markup.RangeHighlighter;
import com.gome.maven.openapi.module.ModifiableModuleModel;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.roots.ModuleRootManager;
import com.gome.maven.openapi.util.Key;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.Change;
import com.gome.maven.openapi.vcs.changes.ChangeListManager;
import com.gome.maven.openapi.vcs.changes.ContentRevision;
import com.gome.maven.openapi.vcs.ex.LineStatusTracker;
import com.gome.maven.openapi.vcs.ex.Range;
import com.gome.maven.openapi.vcs.ex.RangesBuilder;
import com.gome.maven.openapi.vcs.impl.LineStatusTrackerManager;
import com.gome.maven.openapi.vcs.impl.LineStatusTrackerManagerI;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.psi.PsiDirectory;
import com.gome.maven.psi.PsiDocumentManager;
import com.gome.maven.psi.PsiFile;
import com.gome.maven.psi.PsiManager;
import com.gome.maven.util.Function;
import com.gome.maven.util.containers.ContainerUtil;
import com.gome.maven.util.containers.ContainerUtilRt;
import com.gome.maven.util.diff.FilesTooBigForDiffException;

import java.util.*;

public class FormatChangedTextUtil {
    public static final Key<CharSequence> TEST_REVISION_CONTENT = Key.create("test.revision.content");
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.codeInsight.actions.FormatChangedTextUtil");


    private FormatChangedTextUtil() {
    }

    /**
     * Allows to answer if given file has changes in comparison with VCS.
     *
     * @param file  target file
     * @return      <code>true</code> if given file has changes; <code>false</code> otherwise
     */
    public static boolean hasChanges( PsiFile file) {
        final Project project = file.getProject();
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            final Change change = ChangeListManager.getInstance(project).getChange(virtualFile);
            return change != null;
        }
        return false;
    }

    /**
     * Allows to answer if any file below the given directory (any level of nesting) has changes in comparison with VCS.
     *
     * @param directory  target directory to check
     * @return           <code>true</code> if any file below the given directory has changes in comparison with VCS;
     *                   <code>false</code> otherwise
     */
    public static boolean hasChanges( PsiDirectory directory) {
        return hasChanges(directory.getVirtualFile(), directory.getProject());
    }

    /**
     * Allows to answer if given file or any file below the given directory (any level of nesting) has changes in comparison with VCS.
     *
     * @param file     target directory to check
     * @param project  target project
     * @return         <code>true</code> if given file or any file below the given directory has changes in comparison with VCS;
     *                 <code>false</code> otherwise
     */
    public static boolean hasChanges( VirtualFile file,  Project project) {
        final Collection<Change> changes = ChangeListManager.getInstance(project).getChangesIn(file);
        for (Change change : changes) {
            if (change.getType() == Change.Type.NEW || change.getType() == Change.Type.MODIFICATION) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasChanges( VirtualFile[] files,  Project project) {
        for (VirtualFile file : files) {
            if (hasChanges(file, project))
                return true;
        }
        return false;
    }

    /**
     * Allows to answer if any file that belongs to the given module has changes in comparison with VCS.
     *
     * @param module  target module to check
     * @return        <code>true</code> if any file that belongs to the given module has changes in comparison with VCS
     *                <code>false</code> otherwise
     */
    public static boolean hasChanges( Module module) {
        final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        for (VirtualFile root : rootManager.getSourceRoots()) {
            if (hasChanges(root, module.getProject())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Allows to answer if any file that belongs to the given project has changes in comparison with VCS.
     *
     * @param project  target project to check
     * @return         <code>true</code> if any file that belongs to the given project has changes in comparison with VCS
     *                 <code>false</code> otherwise
     */
    public static boolean hasChanges( final Project project) {
        final ModifiableModuleModel moduleModel = new ReadAction<ModifiableModuleModel>() {
            @Override
            protected void run(Result<ModifiableModuleModel> result) throws Throwable {
                result.setResult(ModuleManager.getInstance(project).getModifiableModel());
            }
        }.execute().getResultObject();
        try {
            for (Module module : moduleModel.getModules()) {
                if (hasChanges(module)) {
                    return true;
                }
            }
            return false;
        }
        finally {
            moduleModel.dispose();
        }
    }

    /**
     * Allows to ask for the changed text of the given file (in comparison with VCS).
     *
     * @param file  target file
     * @return      collection of changed regions for the given file
     */
    
    public static Collection<TextRange> getChanges( PsiFile file) {
        final Set<TextRange> defaultResult = Collections.singleton(file.getTextRange());
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            final Change change = ChangeListManager.getInstance(file.getProject()).getChange(virtualFile);
            if (change != null && change.getType() == Change.Type.NEW) {
                return defaultResult;
            }
        }

        final LineStatusTrackerManagerI manager = LineStatusTrackerManager.getInstance(file.getProject());
        if (manager == null) {
            return defaultResult;
        }
        final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return defaultResult;
        }
        final LineStatusTracker lineStatusTracker = manager.getLineStatusTracker(document);
        if (lineStatusTracker == null) {
            return defaultResult;
        }
        final List<Range> ranges = lineStatusTracker.getRanges();
        if (ranges == null || ranges.isEmpty()) {
            return defaultResult;
        }

        List<TextRange> result = new ArrayList<TextRange>();
        for (Range range : ranges) {
            if (range.getType() != Range.DELETED) {
                final RangeHighlighter highlighter = range.getHighlighter();
                if (highlighter != null) {
                    result.add(new TextRange(highlighter.getStartOffset(), highlighter.getEndOffset()));
                }
            }
        }
        return result;
    }

    
    public static List<PsiFile> getChangedFilesFromDirs( Project project,  List<PsiDirectory> dirs)  {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        Collection<Change> changes = ContainerUtil.newArrayList();

        for (PsiDirectory dir : dirs) {
            changes.addAll(changeListManager.getChangesIn(dir.getVirtualFile()));
        }

        return getChangedFiles(project, changes);
    }

    
    public static List<PsiFile> getChangedFiles( final Project project,  Collection<Change> changes) {
        Function<Change, PsiFile> changeToPsiFileMapper = new Function<Change, PsiFile>() {
            private PsiManager myPsiManager = PsiManager.getInstance(project);

            @Override
            public PsiFile fun(Change change) {
                VirtualFile vFile = change.getVirtualFile();
                return vFile != null ? myPsiManager.findFile(vFile) : null;
            }
        };

        return ContainerUtil.mapNotNull(changes, changeToPsiFileMapper);
    }

    
    public static List<TextRange> getChangedTextRanges( Project project,  PsiFile file) throws FilesTooBigForDiffException {
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) return ContainerUtil.emptyList();

        List<TextRange> cachedChangedLines = getCachedChangedLines(project, document);
        if (cachedChangedLines != null) {
            return cachedChangedLines;
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            CharSequence testContent = file.getUserData(TEST_REVISION_CONTENT);
            if (testContent != null) {
                return calculateChangedTextRanges(document, testContent);
            }
        }

        Change change = ChangeListManager.getInstance(project).getChange(file.getVirtualFile());
        if (change == null) {
            return ContainerUtilRt.emptyList();
        }
        if (change.getType() == Change.Type.NEW) {
            return ContainerUtil.newArrayList(file.getTextRange());
        }

        String contentFromVcs = getRevisionedContentFrom(change);
        return contentFromVcs != null ? calculateChangedTextRanges(document, contentFromVcs)
                : ContainerUtil.<TextRange>emptyList();
    }

    
    private static List<TextRange> getCachedChangedLines( Project project,  Document document) {
        LineStatusTracker tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(document);
        if (tracker != null) {
            List<Range> ranges = tracker.getRanges();
            return getChangedTextRanges(document, ranges);
        }

        return null;
    }

    
    private static String getRevisionedContentFrom( Change change) {
        ContentRevision revision = change.getBeforeRevision();
        if (revision == null) {
            return null;
        }

        try {
            return revision.getContent();
        }
        catch (VcsException e) {
            LOG.error("Can't get content for: " + change.getVirtualFile(), e);
            return null;
        }
    }

    
    protected static List<TextRange> calculateChangedTextRanges( Document document,
                                                                 CharSequence contentFromVcs) throws FilesTooBigForDiffException
    {
        return getChangedTextRanges(document, getRanges(document, contentFromVcs));
    }

    
    private static List<Range> getRanges( Document document,
                                          CharSequence contentFromVcs) throws FilesTooBigForDiffException
    {
        Document documentFromVcs = ((EditorFactoryImpl)EditorFactory.getInstance()).createDocument(contentFromVcs, true, false);
        return new RangesBuilder(document, documentFromVcs).getRanges();
    }

    protected static int calculateChangedLinesNumber( Document document,  CharSequence contentFromVcs) {
        try {
            List<Range> changedRanges = getRanges(document, contentFromVcs);
            int linesChanges = 0;
            for (Range range : changedRanges) {
                linesChanges += countLines(range);
            }
            return linesChanges;
        } catch (FilesTooBigForDiffException e) {
            LOG.info("File too big, can not calculate changed lines number");
            return -1;
        }
    }

    private static int countLines(Range range) {
        byte rangeType = range.getType();
        if (rangeType == Range.MODIFIED) {
            int currentChangedLines = range.getLine2() - range.getLine1();
            int revisionLinesChanged = range.getVcsLine2() - range.getVcsLine1();
            return Math.max(currentChangedLines, revisionLinesChanged);
        }
        else if (rangeType == Range.DELETED) {
            return range.getVcsLine2() - range.getVcsLine1();
        }
        else if (rangeType == Range.INSERTED) {
            return range.getLine2() - range.getLine1();
        }

        return 0;
    }

    
    private static List<TextRange> getChangedTextRanges( Document document,  List<Range> changedRanges) {
        List<TextRange> ranges = ContainerUtil.newArrayList();
        for (Range range : changedRanges) {
            if (range.getType() != Range.DELETED) {
                int changeStartLine = range.getLine1();
                int changeEndLine = range.getLine2();

                int lineStartOffset = document.getLineStartOffset(changeStartLine);
                int lineEndOffset = document.getLineEndOffset(changeEndLine - 1);

                ranges.add(new TextRange(lineStartOffset, lineEndOffset));
            }
        }
        return ranges;
    }
}
