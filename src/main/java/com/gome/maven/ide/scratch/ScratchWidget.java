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

import com.gome.maven.icons.AllIcons;
import com.gome.maven.lang.Language;
import com.gome.maven.lang.PerFileMappings;
import com.gome.maven.openapi.editor.Editor;
import com.gome.maven.openapi.fileEditor.FileEditorManager;
import com.gome.maven.openapi.fileEditor.FileEditorManagerEvent;
import com.gome.maven.openapi.fileTypes.LanguageFileType;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.ui.popup.ListPopup;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.openapi.vfs.VirtualFileManager;
import com.gome.maven.openapi.vfs.newvfs.BulkFileListener;
import com.gome.maven.openapi.vfs.newvfs.events.VFileEvent;
import com.gome.maven.openapi.wm.CustomStatusBarWidget;
import com.gome.maven.openapi.wm.StatusBarWidget;
import com.gome.maven.openapi.wm.impl.status.EditorBasedWidget;
import com.gome.maven.openapi.wm.impl.status.TextPanel;
import com.gome.maven.psi.LanguageSubstitutors;
import com.gome.maven.ui.ClickListener;
import com.gome.maven.ui.awt.RelativePoint;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.FileContentUtilCore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

class ScratchWidget extends EditorBasedWidget implements CustomStatusBarWidget.Multiframe, CustomStatusBarWidget {
    static final String WIDGET_ID = "Scratch";

    private final MyTextPanel myPanel = new MyTextPanel();

    public ScratchWidget(Project project) {
        super(project);
        new ClickListener() {
            @Override
            public boolean onClick( MouseEvent e, int clickCount) {
                Project project = getProject();
                Editor editor = getEditor();
                final VirtualFile file = getSelectedFile();
                if (project == null || editor == null || file == null) return false;
                final PerFileMappings<Language> fileService = ScratchFileService.getInstance().getScratchesMapping();

                ListPopup popup = NewScratchFileAction.buildLanguageSelectionPopup(project, "Change Language", fileService.getMapping(file), new Consumer<Language>() {
                    @Override
                    public void consume(Language language) {
                        fileService.setMapping(file, language);
                        update();
                    }
                });
                Dimension dimension = popup.getContent().getPreferredSize();
                Point at = new Point(0, -dimension.height);
                popup.show(new RelativePoint(myPanel, at));

                return true;
            }
        }.installOn(myPanel);
        myConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after( List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event.getRequestor() == FileContentUtilCore.FORCE_RELOAD_REQUESTOR) {
                        update();
                        break;
                    }
                }
            }
        });
    }

    
    @Override
    public String ID() {
        return WIDGET_ID;
    }

    
    @Override
    public WidgetPresentation getPresentation( PlatformType type) {
        return null;
    }

    private void update() {
        Project project = getProject();
        if (project == null) return;
        VirtualFile file = getSelectedFile();
        if (file == null) return;
        ScratchFileService fileService = ScratchFileService.getInstance();
        if (fileService.getRootType(file) instanceof ScratchRootType) {
            Language lang = fileService.getScratchesMapping().getMapping(file);
            if (lang == null) {
                lang = LanguageSubstitutors.INSTANCE.substituteLanguage(((LanguageFileType)file.getFileType()).getLanguage(), file, project);
            }
            myPanel.setText(lang.getDisplayName());
            myPanel.setBorder(WidgetBorder.INSTANCE);
            myPanel.setIcon(getDefaultIcon(lang));
            myPanel.setVisible(true);
        }
        else {
            myPanel.setBorder(null);
            myPanel.setVisible(false);
        }
        if (myStatusBar != null) {
            myStatusBar.updateWidget(WIDGET_ID);
        }
    }

    @Override
    public StatusBarWidget copy() {
        return new ScratchWidget(myProject);
    }

    @Override
    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    public void fileOpened( FileEditorManager source,  VirtualFile file) {
        update();
        super.fileOpened(source, file);
    }

    @Override
    public void fileClosed( FileEditorManager source,  VirtualFile file) {
        update();
        super.fileClosed(source, file);
    }

    @Override
    public void selectionChanged( FileEditorManagerEvent event) {
        update();
        super.selectionChanged(event);
    }

    private static Icon getDefaultIcon( Language language) {
        LanguageFileType associatedLanguage = language.getAssociatedFileType();
        return associatedLanguage != null ? associatedLanguage.getIcon() : null;
    }

    private static class MyTextPanel extends TextPanel {
        private int myIconTextGap = 2;
        private Icon myIcon;

        @Override
        protected void paintComponent( final Graphics g) {
            super.paintComponent(g);
            if (getText() != null) {
                Rectangle r = getBounds();
                Insets insets = getInsets();
                AllIcons.Ide.Statusbar_arrows.paintIcon(this, g, r.width - insets.right - AllIcons.Ide.Statusbar_arrows.getIconWidth() - 2,
                        r.height / 2 - AllIcons.Ide.Statusbar_arrows.getIconHeight() / 2);
                if (myIcon != null) {
                    myIcon.paintIcon(this, g, insets.left - myIconTextGap - myIcon.getIconWidth(), r.height / 2 - myIcon.getIconHeight() / 2);
                }
            }
        }

        
        @Override
        public Insets getInsets() {
            Insets insets = super.getInsets();
            if (myIcon != null) {
                insets.left += myIcon.getIconWidth() + myIconTextGap * 2;
            }
            return insets;
        }

        @Override
        public Dimension getPreferredSize() {
            final Dimension preferredSize = super.getPreferredSize();
            int deltaWidth = AllIcons.Ide.Statusbar_arrows.getIconWidth() + myIconTextGap * 2;
            if (myIcon != null) {
                deltaWidth += myIcon.getIconWidth() + myIconTextGap * 2;
            }
            return new Dimension(preferredSize.width + deltaWidth, preferredSize.height);
        }

        public void setIcon(Icon icon) {
            myIcon = icon;
        }
    }

}
