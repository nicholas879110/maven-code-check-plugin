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
package com.gome.maven.openapi.vcs.history;

import com.gome.maven.diff.DiffContentFactory;
import com.gome.maven.diff.DiffDialogHints;
import com.gome.maven.diff.DiffManager;
import com.gome.maven.diff.DiffRequestFactoryImpl;
import com.gome.maven.diff.contents.DiffContent;
import com.gome.maven.diff.requests.DiffRequest;
import com.gome.maven.diff.requests.SimpleDiffRequest;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.openapi.progress.ProgressIndicator;
import com.gome.maven.openapi.progress.Task;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.Messages;
import com.gome.maven.openapi.util.Couple;
import com.gome.maven.openapi.util.Pair;
import com.gome.maven.openapi.vcs.FilePath;
import com.gome.maven.openapi.vcs.VcsBundle;
import com.gome.maven.openapi.vcs.VcsException;
import com.gome.maven.openapi.vcs.changes.actions.diff.FileAwareDocumentContent;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.encoding.EncodingManager;
import com.gome.maven.openapi.vfs.encoding.EncodingProjectManager;
import com.gome.maven.util.WaitForProgressToShow;

import java.io.IOException;

public class VcsHistoryUtil {

    private static final Logger LOG = Logger.getInstance(VcsHistoryUtil.class);

    private VcsHistoryUtil() {
    }

    public static int compare(VcsFileRevision first, VcsFileRevision second) {
        if (first instanceof CurrentRevision && second instanceof CurrentRevision) {
            return compareNumbers(first, second);
        }
        if (second instanceof CurrentRevision) return -1 * compare(second, first);

        if (first instanceof CurrentRevision) {
            int result = compareNumbers(first, second);
            if (result == 0) {
                return 1;
            }
            else {
                return result;
            }
        }
        else {
            return compareNumbers(first, second);
        }
    }

    public static int compareNumbers(VcsFileRevision first, VcsFileRevision second) {
        return first.getRevisionNumber().compareTo(second.getRevisionNumber());
    }

    /**
     * Invokes {@link com.gome.maven.openapi.diff.DiffManager#getDiffTool()} to show difference between the given revisions of the given file.
     * @param project   project under vcs control.
     * @param path  file which revisions are compared.
     * @param revision1 first revision - 'before', to the left.
     * @param revision2 second revision - 'after', to the right.
     * @throws VcsException
     * @throws IOException
     */
    public static void showDiff( final Project project,  FilePath path,
                                 VcsFileRevision revision1,  VcsFileRevision revision2,
                                 String title1,  String title2) throws VcsException, IOException {
        final byte[] content1 = loadRevisionContent(revision1);
        final byte[] content2 = loadRevisionContent(revision2);

        String title = DiffRequestFactoryImpl.getContentTitle(path);

        DiffContent diffContent1 = createContent(project, content1, revision1, path);
        DiffContent diffContent2 = createContent(project, content2, revision2, path);

        final DiffRequest request = new SimpleDiffRequest(title, diffContent1, diffContent2, title1, title2);

        WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
            public void run() {
                DiffManager.getInstance().showDiff(project, request);
            }
        }, null, project);
    }

    
    public static byte[] loadRevisionContent( VcsFileRevision revision) throws VcsException, IOException {
        byte[] content = revision.getContent();
        if (content == null) {
            revision.loadContent();
            content = revision.getContent();
        }
        if (content == null) throw new VcsException("Failed to load content for revision " + revision.getRevisionNumber().asString());
        return content;
    }

    public static String loadRevisionContentGuessEncoding( final VcsFileRevision revision,  final VirtualFile file,
                                                           final Project project) throws VcsException, IOException {
        final byte[] bytes = loadRevisionContent(revision);
        if (file != null) {
            return new String(bytes, file.getCharset());
        }
        EncodingManager e = project != null ? EncodingProjectManager.getInstance(project) : null;
        if (e == null) {
            e = EncodingManager.getInstance();
        }

        return CharsetToolkit.bytesToString(bytes, e.getDefaultCharset());
    }

    
    private static DiffContent createContent( Project project,  byte[] content,  VcsFileRevision revision,
                                              FilePath filePath) throws IOException {
        if (isCurrent(revision)) {
            VirtualFile file = filePath.getVirtualFile();
            if (file != null) return DiffContentFactory.getInstance().create(project, file);
        }
        if (isEmpty(revision)) {
            return DiffContentFactory.getInstance().createEmpty();
        }
        if (filePath.getFileType().isBinary()) {
            return DiffContentFactory.getInstance().createBinary(project, filePath.getName(), filePath.getFileType(), content);
        }
        String text = CharsetToolkit.bytesToString(content, filePath.getCharset());
        return FileAwareDocumentContent.create(project, text, filePath);
    }

    private static boolean isCurrent(VcsFileRevision revision) {
        return revision instanceof CurrentRevision;
    }

    private static boolean isEmpty(VcsFileRevision revision) {
        return revision == null || VcsFileRevision.NULL.equals(revision);
    }

    /**
     * Shows difference between two revisions of a file in a diff tool.
     * The content of revisions is queried in a background thread.
     * If {@code findOlderNewer} is set to {@code true}, revisions may be specified in any order:
     * this method will sort them so that the older revision is at the left, and the newer one is at the right.
     * @param findOlderNewer specify {@code true} to let method compare revisions, and put the older revision at the left, and newer revision
     *                       at the right.<br/>
     *                       Specify {@code false} to put {@code revision1} at the left, and {@code revision2} at the right.
     * @see #showDiff(Project, FilePath, VcsFileRevision, VcsFileRevision, String, String)
     */
    public static void showDifferencesInBackground( final Project project,  final FilePath filePath,
                                                    final VcsFileRevision revision1,  final VcsFileRevision revision2,
                                                   final boolean findOlderNewer) {
        new Task.Backgroundable(project, "Loading revisions to compare") {
            @Override
            public void run( ProgressIndicator indicator) {
                VcsFileRevision left = revision1;
                VcsFileRevision right = revision2;
                if (findOlderNewer) {
                    Pair<VcsFileRevision, VcsFileRevision> pair = sortRevisions(revision1, revision2);
                    left = pair.first;
                    right = pair.second;
                }

                try {
                    final String leftTitle = left.getRevisionNumber().asString() +
                            (left instanceof CurrentRevision ? " (" + VcsBundle.message("diff.title.local") + ")" : "");
                    final String rightTitle = right.getRevisionNumber().asString() +
                            (right instanceof CurrentRevision ? " (" + VcsBundle.message("diff.title.local") + ")" : "");
                    showDiff(project, filePath, left, right, leftTitle, rightTitle);
                }
                catch (final VcsException e) {
                    LOG.info(e);
                    WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
                        public void run() {
                            Messages.showErrorDialog(VcsBundle.message("message.text.cannot.show.differences", e.getLocalizedMessage()),
                                    VcsBundle.message("message.title.show.differences"));
                        }
                    }, null, project);
                }
                catch (IOException e) {
                    LOG.info(e);
                }
                catch (ProcessCanceledException ex) {
                    LOG.info(ex);
                }
            }
        }.queue();
    }

    /**
     * Compares the given revisions and returns a pair of them, where the first one is older, and second is newer.
     */
    
    public static Couple<VcsFileRevision> sortRevisions( VcsFileRevision revision1,
                                                         VcsFileRevision revision2) {
        VcsFileRevision left = revision1;
        VcsFileRevision right = revision2;
        if (compare(revision1, revision2) > 0) {
            left = revision2;
            right = revision1;
        }
        return Couple.of(left, right);
    }

}
