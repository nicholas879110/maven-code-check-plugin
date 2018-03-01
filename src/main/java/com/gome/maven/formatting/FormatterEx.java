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

package com.gome.maven.formatting;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.util.TextRange;
import com.gome.maven.psi.codeStyle.CodeStyleSettings;
import com.gome.maven.psi.codeStyle.CommonCodeStyleSettings;
import com.gome.maven.util.IncorrectOperationException;

public abstract class FormatterEx{

    private static FormatterEx myTestInstance;

    public static FormatterEx getInstance() {
        final Application application = ApplicationManager.getApplication();
        if (application != null) {
            return application.getComponent(FormatterEx.class);
        }
        else {
            return getTestInstance();
        }
    }


    private static FormatterEx getTestInstance() {
        if (myTestInstance == null) {
            try {
                myTestInstance = (FormatterEx)Class.forName("com.gome.maven.formatting.FormatterImpl").newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return myTestInstance;
    }

    public abstract void format(FormattingModel model,
                                CodeStyleSettings settings,
                                CommonCodeStyleSettings.IndentOptions indentOptions,
                                FormatTextRanges affectedRanges) throws IncorrectOperationException;

    public abstract void format(FormattingModel model,
                                CodeStyleSettings settings,
                                CommonCodeStyleSettings.IndentOptions indentOptions,
                                CommonCodeStyleSettings.IndentOptions javaIndentOptions,
                                FormatTextRanges affectedRanges) throws IncorrectOperationException;


    public abstract IndentInfo getWhiteSpaceBefore(final FormattingDocumentModel psiBasedFormattingModel,
                                                   final Block block,
                                                   final CodeStyleSettings settings,
                                                   final CommonCodeStyleSettings.IndentOptions indentOptions,
                                                   final TextRange textRange, final boolean mayChangeLineFeeds);

    public abstract int adjustLineIndent(final FormattingModel psiBasedFormattingModel,
                                         final CodeStyleSettings settings,
                                         final CommonCodeStyleSettings.IndentOptions indentOptions,
                                         final int offset,
                                         final TextRange affectedRange) throws IncorrectOperationException;

    
    public abstract String getLineIndent(final FormattingModel psiBasedFormattingModel,
                                         final CodeStyleSettings settings,
                                         final CommonCodeStyleSettings.IndentOptions indentOptions,
                                         final int offset,
                                         final TextRange affectedRange);

    public abstract void adjustTextRange(final FormattingModel documentModel,
                                         final CodeStyleSettings settings,
                                         final CommonCodeStyleSettings.IndentOptions indentOptions,
                                         final TextRange textRange,
                                         final boolean keepBlankLines,
                                         final boolean keepLineBreaks,
                                         final boolean changeWSBeforeFirstElement,
                                         final boolean changeLineFeedsBeforeFirstElement, final IndentInfoStorage indentInfoStorage);

    public abstract void saveIndents(final FormattingModel model, final TextRange affectedRange,
                                     IndentInfoStorage storage,
                                     final CodeStyleSettings settings,
                                     final CommonCodeStyleSettings.IndentOptions indentOptions);

    public abstract boolean isDisabled();



    public abstract void adjustLineIndentsForRange(final FormattingModel model,
                                                   final CodeStyleSettings settings,
                                                   final CommonCodeStyleSettings.IndentOptions indentOptions,
                                                   final TextRange rangeToAdjust);

    public abstract void formatAroundRange(final FormattingModel model, final CodeStyleSettings settings,
                                           final TextRange textRange, final FileType fileType);

    public abstract void adjustTextRange(FormattingModel model,
                                         CodeStyleSettings settings,
                                         CommonCodeStyleSettings.IndentOptions indentOptions,
                                         TextRange affectedRange);

    public abstract void setProgressTask( FormattingProgressTask progressIndicator);

    /**
     * Calculates minimum spacing, allowed by formatting model (in columns) for a block starting at given offset,
     * relative to its previous sibling block.
     * Returns zero, if required block cannot be found at provided offset, or spacing cannot be calculated due to some other reason.
     */
    public abstract int getSpacingForBlockAtOffset(FormattingModel model, int offset);

    public interface IndentInfoStorage {
        void saveIndentInfo( IndentInfo info, int startOffset);

        IndentInfo getIndentInfo(int startOffset);
    }

    public static FormatterEx getInstanceEx() {
        return getInstance();
    }

}
