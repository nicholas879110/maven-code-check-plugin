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
package com.gome.maven.openapi.ui;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.CustomShortcutSet;
import com.gome.maven.openapi.actionSystem.ShortcutSet;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.fileChooser.FileChooser;
import com.gome.maven.openapi.fileChooser.FileChooserDescriptor;
import com.gome.maven.openapi.keymap.KeymapUtil;
import com.gome.maven.openapi.project.DumbAwareAction;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.openapi.util.SystemInfo;
import com.gome.maven.openapi.util.io.FileUtil;
import com.gome.maven.openapi.util.text.StringUtil;
import com.gome.maven.openapi.vfs.LocalFileSystem;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.ui.GuiUtils;
import com.gome.maven.ui.UIBundle;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.ui.UIUtil;
import com.gome.maven.util.ui.update.LazyUiDisposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ComponentWithBrowseButton<Comp extends JComponent> extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(ComponentWithBrowseButton.class);

    private final Comp myComponent;
    private final FixedSizeButton myBrowseButton;
    private boolean myButtonEnabled = true;

    public ComponentWithBrowseButton(Comp component,  ActionListener browseActionListener) {
        super(new BorderLayout(SystemInfo.isMac ? 0 : 2, 0));

        myComponent = component;
        // required! otherwise JPanel will occasionally gain focus instead of the component
        setFocusable(false);
        add(myComponent, BorderLayout.CENTER);

        myBrowseButton = new FixedSizeButton(myComponent);
        if (browseActionListener != null) {
            myBrowseButton.addActionListener(browseActionListener);
        }
        // don't force FixedSizeButton to occupy the whole height
        add(wrapWithoutResize(myBrowseButton), BorderLayout.EAST);

        myBrowseButton.setToolTipText(UIBundle.message("component.with.browse.button.browse.button.tooltip.text"));

        // FixedSizeButton isn't focusable but it should be selectable via keyboard.
        if (ApplicationManager.getApplication() != null) {  // avoid crash at design time
            new MyDoClickAction(myBrowseButton).registerShortcut(myComponent);
        }
    }

    private static JPanel wrapWithoutResize(JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.add(component);
        return panel;
    }

    public final Comp getChildComponent() {
        return myComponent;
    }

    public void setTextFieldPreferredWidth(final int charCount) {
        final Comp comp = getChildComponent();
        Dimension size = GuiUtils.getSizeByChars(charCount, comp);
        comp.setPreferredSize(size);
        final Dimension preferredSize = myBrowseButton.getPreferredSize();
        setPreferredSize(new Dimension(size.width + preferredSize.width + 2, UIUtil.isUnderAquaLookAndFeel() ? preferredSize.height : preferredSize.height + 2));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        myBrowseButton.setEnabled(enabled && myButtonEnabled);
        myComponent.setEnabled(enabled);
    }

    public void setButtonEnabled(boolean buttonEnabled) {
        myButtonEnabled = buttonEnabled;
        setEnabled(isEnabled());
    }

    public void setButtonIcon(Icon icon) {
        myBrowseButton.setIcon(icon);
    }

    /**
     * Adds specified <code>listener</code> to the browse button.
     */
    public void addActionListener(ActionListener listener){
        myBrowseButton.addActionListener(listener);
    }

    public void removeActionListener(ActionListener listener) {
        myBrowseButton.removeActionListener(listener);
    }

    public void addBrowseFolderListener( String title,  String description,  Project project,
                                        FileChooserDescriptor fileChooserDescriptor,
                                        TextComponentAccessor<Comp> accessor) {
        addBrowseFolderListener(title, description, project, fileChooserDescriptor, accessor, true);
    }

    public void addBrowseFolderListener( String title,  String description,  Project project,
                                        FileChooserDescriptor fileChooserDescriptor,
                                        TextComponentAccessor<Comp> accessor, boolean autoRemoveOnHide) {
        addBrowseFolderListener(project, new BrowseFolderActionListener<Comp>(title, description, this, project, fileChooserDescriptor, accessor), autoRemoveOnHide);
    }

    public void addBrowseFolderListener( Project project, final BrowseFolderActionListener<Comp> actionListener) {
        addBrowseFolderListener(project, actionListener, true);
    }

    public void addBrowseFolderListener( Project project, final BrowseFolderActionListener<Comp> actionListener, boolean autoRemoveOnHide) {
        if (autoRemoveOnHide) {
            new LazyUiDisposable<ComponentWithBrowseButton<Comp>>(null, this, this) {
                @Override
                protected void initialize( Disposable parent,  ComponentWithBrowseButton<Comp> child,  Project project) {
                    addActionListener(actionListener);
                    Disposer.register(child, new Disposable() {
                        @Override
                        public void dispose() {
                            removeActionListener(actionListener);
                        }
                    });
                }
            };
        } else {
            addActionListener(actionListener);
        }
    }

    @Override
    public void dispose() { }

    public FixedSizeButton getButton() {
        return myBrowseButton;
    }

    /**
     * Do not use this class directly it is public just to hack other implementation of controls similar to TextFieldWithBrowseButton.
     */
    public static final class MyDoClickAction extends DumbAwareAction {
        private final FixedSizeButton myBrowseButton;
        public MyDoClickAction(FixedSizeButton browseButton) {
            myBrowseButton = browseButton;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myBrowseButton.isVisible() && myBrowseButton.isEnabled());
        }

        @Override
        public void actionPerformed(AnActionEvent e){
            myBrowseButton.doClick();
        }

        public void registerShortcut(JComponent textField) {
            ShortcutSet shiftEnter = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK));
            registerCustomShortcutSet(shiftEnter, textField);
            myBrowseButton.setToolTipText(KeymapUtil.getShortcutsText(shiftEnter.getShortcuts()));
        }

        public static void addTo(FixedSizeButton browseButton, JComponent aComponent) {
            new MyDoClickAction(browseButton).registerShortcut(aComponent);
        }
    }

    public static class BrowseFolderActionListener<T extends JComponent> implements ActionListener {
        private final String myTitle;
        private final String myDescription;
        protected ComponentWithBrowseButton<T> myTextComponent;
        private final TextComponentAccessor<T> myAccessor;
        private Project myProject;
        protected final FileChooserDescriptor myFileChooserDescriptor;

        public BrowseFolderActionListener( String title,
                                           String description,
                                          ComponentWithBrowseButton<T> textField,
                                           Project project,
                                          FileChooserDescriptor fileChooserDescriptor,
                                          TextComponentAccessor<T> accessor) {
            if (fileChooserDescriptor != null && fileChooserDescriptor.isChooseMultiple()) {
                LOG.error("multiple selection not supported");
                fileChooserDescriptor = new FileChooserDescriptor(fileChooserDescriptor) {
                    @Override
                    public boolean isChooseMultiple() {
                        return false;
                    }
                };
            }

            myTitle = title;
            myDescription = description;
            myTextComponent = textField;
            myProject = project;
            myFileChooserDescriptor = fileChooserDescriptor;
            myAccessor = accessor;
        }

        
        protected Project getProject() {
            return myProject;
        }

        protected void setProject( Project project) {
            myProject = project;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileChooserDescriptor fileChooserDescriptor = myFileChooserDescriptor;
            if (myTitle != null || myDescription != null) {
                fileChooserDescriptor = (FileChooserDescriptor)myFileChooserDescriptor.clone();
                if (myTitle != null) {
                    fileChooserDescriptor.setTitle(myTitle);
                }
                if (myDescription != null) {
                    fileChooserDescriptor.setDescription(myDescription);
                }
            }

            FileChooser.chooseFile(fileChooserDescriptor, getProject(), getInitialFile(), new Consumer<VirtualFile>() {
                @Override
                public void consume(VirtualFile file) {
                    onFileChosen(file);
                }
            });
        }

        
        protected VirtualFile getInitialFile() {
            String directoryName = getComponentText();
            if (StringUtil.isEmptyOrSpaces(directoryName)) {
                return null;
            }

            directoryName = FileUtil.toSystemIndependentName(directoryName);
            VirtualFile path = LocalFileSystem.getInstance().findFileByPath(expandPath(directoryName));
            while (path == null && directoryName.length() > 0) {
                int pos = directoryName.lastIndexOf('/');
                if (pos <= 0) break;
                directoryName = directoryName.substring(0, pos);
                path = LocalFileSystem.getInstance().findFileByPath(directoryName);
            }
            return path;
        }

        
        protected String expandPath( String path) {
            return path;
        }

        protected String getComponentText() {
            return myAccessor.getText(myTextComponent.getChildComponent()).trim();
        }

        
        protected String chosenFileToResultingText( VirtualFile chosenFile) {
            return chosenFile.getPresentableUrl();
        }

        /** @deprecated use/override {@link #onFileChosen(VirtualFile)} (to be removed in IDEA 15) */
        @SuppressWarnings("SpellCheckingInspection")
        protected void onFileChoosen( VirtualFile chosenFile) {
            myAccessor.setText(myTextComponent.getChildComponent(), chosenFileToResultingText(chosenFile));
        }

        @SuppressWarnings("deprecation")
        protected void onFileChosen( VirtualFile chosenFile) {
            onFileChoosen(chosenFile);
        }
    }

    @Override
    public final void requestFocus() {
        myComponent.requestFocus();
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void setNextFocusableComponent(Component aComponent) {
        super.setNextFocusableComponent(aComponent);
        myComponent.setNextFocusableComponent(aComponent);
    }

    private KeyEvent myCurrentEvent = null;

    @Override
    protected final boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (condition == WHEN_FOCUSED && myCurrentEvent != e) {
            try {
                myCurrentEvent = e;
                myComponent.dispatchEvent(e);
            }
            finally {
                myCurrentEvent = null;
            }
        }
        if (e.isConsumed()) return true;
        return super.processKeyBinding(ks, e, condition, pressed);
    }
}
