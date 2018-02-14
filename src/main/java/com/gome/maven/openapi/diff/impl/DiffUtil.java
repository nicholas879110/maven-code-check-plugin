/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.gome.maven.openapi.diff.impl;

import com.gome.maven.codeStyle.CodeStyleFacade;
import com.gome.maven.openapi.actionSystem.DataContext;
import com.gome.maven.openapi.diff.DiffContent;
import com.gome.maven.openapi.diff.DiffContentUtil;
import com.gome.maven.openapi.diff.DiffViewer;
import com.gome.maven.openapi.diff.impl.external.DiffManagerImpl;
import com.gome.maven.openapi.diff.impl.fragments.Fragment;
import com.gome.maven.openapi.diff.impl.fragments.LineFragment;
import com.gome.maven.openapi.diff.impl.string.DiffString;
import com.gome.maven.openapi.diff.impl.util.FocusDiffSide;
import com.gome.maven.openapi.diff.impl.util.TextDiffType;
import com.gome.maven.openapi.editor.Document;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.editor.EditorFactory;
import com.gome.maven.openapi.editor.ex.EditorEx;
import com.gome.maven.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces;
import com.gome.maven.openapi.fileTypes.FileType;
import com.gome.maven.openapi.fileTypes.FileTypes;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.FrameWrapper;
import com.gome.maven.util.ImageLoader;
import com.gome.maven.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class DiffUtil {
    private DiffUtil() {
    }

    public static void initDiffFrame(Project project,  FrameWrapper frameWrapper,  final DiffViewer diffPanel, final JComponent mainComponent) {
        frameWrapper.setComponent(mainComponent);
        frameWrapper.setProject(project);
        frameWrapper.setImage(ImageLoader.loadFromResource("/diff/Diff.png"));
        frameWrapper.setPreferredFocusedComponent(diffPanel.getPreferredFocusedComponent());
        frameWrapper.closeOnEsc();
    }

    
    public static FocusDiffSide getFocusDiffSide( DataContext dataContext) {
        return FocusDiffSide.DATA_KEY.getData(dataContext);
    }

    
    public static DiffString[] convertToLines( String text) {
        return DiffString.create(text).tokenize();
    }

    
    public static FileType[] chooseContentTypes( DiffContent[] contents) {
        FileType commonType = FileTypes.PLAIN_TEXT;
        for (DiffContent content : contents) {
            FileType contentType = content.getContentType();
            if (DiffContentUtil.isTextType(contentType)) commonType = contentType;
        }
        FileType[] result = new FileType[contents.length];
        for (int i = 0; i < contents.length; i++) {
            FileType contentType = contents[i].getContentType();
            result[i] = DiffContentUtil.isTextType(contentType) ? contentType : commonType;
        }
        return result;
    }

    public static boolean isWritable( DiffContent content) {
        Document document = content.getDocument();
        return document != null && document.isWritable();
    }

    public static EditorEx createEditor(Document document, Project project, boolean isViewer) {
        return createEditor(document, project, isViewer, null);
    }

    public static EditorEx createEditor(Document document, Project project, boolean isViewer,  FileType fileType) {
        EditorFactory factory = EditorFactory.getInstance();
        EditorEx editor = (EditorEx)(isViewer ? factory.createViewer(document, project) : factory.createEditor(document, project));
        editor.putUserData(DiffManagerImpl.EDITOR_IS_DIFF_KEY, Boolean.TRUE);
        editor.setSoftWrapAppliancePlace(SoftWrapAppliancePlaces.VCS_DIFF);
        editor.getGutterComponentEx().revalidateMarkup();

        if (fileType != null && project != null && !project.isDisposed()) {
            CodeStyleFacade codeStyleFacade = CodeStyleFacade.getInstance(project);
            editor.getSettings().setTabSize(codeStyleFacade.getTabSize(fileType));
            editor.getSettings().setUseTabCharacter(codeStyleFacade.useTabCharacter(fileType));
        }

        return editor;
    }

    public static void drawBoldDottedFramingLines( Graphics2D g, int startX, int endX, int startY, int bottomY,  Color color) {
        UIUtil.drawBoldDottedLine(g, startX, endX, startY, null, color, false);
        UIUtil.drawBoldDottedLine(g, startX, endX, bottomY, null, color, false);
    }

    public static void drawDoubleShadowedLine( Graphics2D g, int startX, int endX, int y,  Color color) {
        UIUtil.drawLine(g, startX, y, endX, y, null, getFramingColor(color));
        UIUtil.drawLine(g, startX, y + 1, endX, y + 1, null, color);
    }

    
    public static Color getFramingColor( Color backgroundColor) {
        return backgroundColor != null ? backgroundColor.darker() : null;
    }

    
    public static TextDiffType makeTextDiffType( LineFragment fragment) {
        TextDiffType type = TextDiffType.create(fragment.getType());
        if (isInlineWrapper(fragment)) {
            return TextDiffType.deriveInstanceForInlineWrapperFragment(type);
        }
        return type;
    }

    public static boolean isInlineWrapper( Fragment fragment) {
        return fragment instanceof LineFragment && ((LineFragment)fragment).getChildrenIterator() != null;
    }

    private static boolean isUnknownFileType( DiffContent diffContent) {
        return FileTypes.UNKNOWN.equals(diffContent.getContentType());
    }

    private static boolean isEmptyFileType( DiffContent diffContent) {
        return diffContent.getContentType() == null;
    }

    public static boolean oneIsUnknown( DiffContent content1,  DiffContent content2) {
        if (content1 == null && content2 == null) return true;
        if (content1 != null && content2 != null) {
            return isUnknownFileType(content1) || isUnknownFileType(content2) || (isEmptyFileType(content1) && isEmptyFileType(content2));
        }
        if (content1 != null) {
            return isUnknownFileType(content1) || isEmptyFileType(content1);
        }
        else {
            return isUnknownFileType(content2) || isEmptyFileType(content2);
        }
    }

    public static boolean isDiffEditor( Editor editor) {
        return editor.getUserData(DiffManagerImpl.EDITOR_IS_DIFF_KEY) != null;
    }
}
