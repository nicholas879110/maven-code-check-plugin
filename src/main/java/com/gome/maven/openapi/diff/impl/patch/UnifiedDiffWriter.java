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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 24.11.2006
 * Time: 15:29:45
 */
package com.gome.maven.openapi.diff.impl.patch;

import com.gome.maven.openapi.extensions.Extensions;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vcs.changes.CommitContext;
import com.gome.maven.util.containers.HashMap;

import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class UnifiedDiffWriter {
     private static final String INDEX_SIGNATURE = "Index: {0}{1}";
     public static final String ADDITIONAL_PREFIX = "IDEA additional info:";
     public static final String ADD_INFO_HEADER = "Subsystem: ";
     public static final String ADD_INFO_LINE_START = "<+>";
    private static final String HEADER_SEPARATOR = "===================================================================";
     public static final String NO_NEWLINE_SIGNATURE = "\\ No newline at end of file";

    private UnifiedDiffWriter() {
    }

    public static void write(Project project, Collection<FilePatch> patches, Writer writer, final String lineSeparator,
                              final CommitContext commitContext) throws IOException {
        final PatchEP[] extensions = project == null ? new PatchEP[0] : Extensions.getExtensions(PatchEP.EP_NAME, project);
        write(project, patches, writer, lineSeparator, extensions, commitContext);
    }

    public static void write(Project project, Collection<FilePatch> patches, Writer writer, final String lineSeparator,
                             final PatchEP[] extensions, final CommitContext commitContext) throws IOException {
        for(FilePatch filePatch: patches) {
            if (!(filePatch instanceof TextFilePatch)) continue;
            TextFilePatch patch = (TextFilePatch) filePatch;
            final String path = patch.getBeforeName() == null ? patch.getAfterName() : patch.getBeforeName();
            final Map<String , CharSequence> additionalMap = new HashMap<String, CharSequence>();
            for (PatchEP extension : extensions) {
                final CharSequence charSequence = extension.provideContent(path, commitContext);
                if (! StringUtil.isEmpty(charSequence)) {
                    additionalMap.put(extension.getName(), charSequence);
                }
            }
            writeFileHeading(patch, writer, lineSeparator, additionalMap);
            for(PatchHunk hunk: patch.getHunks()) {
                writeHunkStart(writer, hunk.getStartLineBefore(), hunk.getEndLineBefore(), hunk.getStartLineAfter(), hunk.getEndLineAfter(),
                        lineSeparator);
                for(PatchLine line: hunk.getLines()) {
                    char prefixChar = ' ';
                    switch(line.getType()) {
                        case ADD: prefixChar = '+'; break;
                        case REMOVE: prefixChar = '-'; break;
                        case CONTEXT: prefixChar = ' '; break;
                    }
                    String text = line.getText();
                    if (text.endsWith("\n")) {
                        text = text.substring(0, text.length()-1);
                    }
                    writeLine(writer, text, prefixChar);
                    if (line.isSuppressNewLine()) {
                        writer.write(lineSeparator + NO_NEWLINE_SIGNATURE + lineSeparator);
                    }
                    else {
                        writer.write(lineSeparator);
                    }
                }
            }
        }
    }

    private static void writeFileHeading(final FilePatch patch,
                                         final Writer writer,
                                         final String lineSeparator,
                                         Map<String, CharSequence> additionalMap) throws IOException {
        writer.write(MessageFormat.format(INDEX_SIGNATURE, patch.getBeforeName(), lineSeparator));
        if (additionalMap != null && ! additionalMap.isEmpty()) {
            writer.write(ADDITIONAL_PREFIX);
            writer.write(lineSeparator);
            for (Map.Entry<String, CharSequence> entry : additionalMap.entrySet()) {
                writer.write(ADD_INFO_HEADER + entry.getKey());
                writer.write(lineSeparator);
                final String value = StringUtil.escapeStringCharacters(entry.getValue().toString());
                final List<String> lines = StringUtil.split(value, "\n");
                for (String line : lines) {
                    writer.write(ADD_INFO_LINE_START);
                    writer.write(line);
                    writer.write(lineSeparator);
                }
            }
        }
        writer.write(HEADER_SEPARATOR + lineSeparator);
        writeRevisionHeading(writer, "---", patch.getBeforeName(), patch.getBeforeVersionId(), lineSeparator);
        writeRevisionHeading(writer, "+++", patch.getAfterName(), patch.getAfterVersionId(), lineSeparator);
    }

    private static void writeRevisionHeading(final Writer writer, final String prefix,
                                             final String revisionPath, final String revisionName,
                                             final String lineSeparator)
            throws IOException {
        writer.write(prefix + " ");
        writer.write(revisionPath);
        writer.write("\t");
        writer.write(revisionName);
        writer.write(lineSeparator);
    }

    private static void writeHunkStart(Writer writer, int startLine1, int endLine1, int startLine2, int endLine2,
                                       final String lineSeparator)
            throws IOException {
        StringBuilder builder = new StringBuilder("@@ -");
        builder.append(startLine1+1).append(",").append(endLine1-startLine1);
        builder.append(" +").append(startLine2+1).append(",").append(endLine2-startLine2).append(" @@").append(lineSeparator);
        writer.append(builder.toString());
    }

    private static void writeLine(final Writer writer, final String line, final char prefix) throws IOException {
        writer.write(prefix);
        writer.write(line);
    }
}
