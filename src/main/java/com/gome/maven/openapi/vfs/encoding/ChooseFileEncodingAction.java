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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jul 19, 2007
 * Time: 5:53:46 PM
 */
package com.gome.maven.openapi.vfs.encoding;

import com.gome.maven.icons.AllIcons;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.AnActionEvent;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.actionSystem.ex.ComboBoxAction;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.openapi.vfs.VirtualFile;
import com.gome.maven.util.Function;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ChooseFileEncodingAction extends ComboBoxAction {
    private final VirtualFile myVirtualFile;

    public ChooseFileEncodingAction(VirtualFile virtualFile) {
        myVirtualFile = virtualFile;
    }

    @Override
    public abstract void update(final AnActionEvent e);

    private void fillCharsetActions( DefaultActionGroup group,
                                     VirtualFile virtualFile,
                                     List<Charset> charsets,
                                     final Function<Charset, String> charsetFilter) {
        for (final Charset slave : charsets) {
            ChangeFileEncodingTo action = new ChangeFileEncodingTo(virtualFile, slave) {
                {
                    String description = charsetFilter.fun(slave);
                    if (description == null) {
                        getTemplatePresentation().setIcon(AllIcons.General.Warning);
                    }
                    else {
                        getTemplatePresentation().setDescription(description);
                    }
                }

                @Override
                public void update( AnActionEvent e) {
                }

                @Override
                protected void chosen(final VirtualFile file,  final Charset charset) {
                    ChooseFileEncodingAction.this.chosen(file, charset);
                }
            };
            group.add(action);
        }
    }

    private class ClearThisFileEncodingAction extends AnAction {
        private final VirtualFile myFile;

        private ClearThisFileEncodingAction( VirtualFile file,  String clearItemText) {
            super(clearItemText, "Clear " + (file == null ? "default" : "file '"+file.getName()+"'") + " encoding.", null);
            myFile = file;
        }

        @Override
        public void actionPerformed( final AnActionEvent e) {
            chosen(myFile, NO_ENCODING);
        }
    }

    public static final Charset NO_ENCODING = new Charset("NO_ENCODING", null) {
        @Override
        public boolean contains(final Charset cs) {
            return false;
        }

        @Override
        public CharsetDecoder newDecoder() {
            return null;
        }

        @Override
        public CharsetEncoder newEncoder() {
            return null;
        }
    };
    protected abstract void chosen( VirtualFile virtualFile,  Charset charset);

    
    protected DefaultActionGroup createCharsetsActionGroup(String clearItemText,
                                                           Charset alreadySelected,
                                                            Function<Charset, String> charsetFilter) {
        DefaultActionGroup group = new DefaultActionGroup();
        List<Charset> favorites = new ArrayList<Charset>(EncodingManager.getInstance().getFavorites());
        Collections.sort(favorites);
        Charset current = myVirtualFile == null ? null : myVirtualFile.getCharset();
        favorites.remove(current);
        favorites.remove(alreadySelected);

        if (clearItemText != null) {
            group.add(new ClearThisFileEncodingAction(myVirtualFile, clearItemText));
        }
        if (favorites.isEmpty() && clearItemText == null) {
            fillCharsetActions(group, myVirtualFile, Arrays.asList(CharsetToolkit.getAvailableCharsets()), charsetFilter);
        }
        else {
            fillCharsetActions(group, myVirtualFile, favorites, charsetFilter);

            DefaultActionGroup more = new DefaultActionGroup("more", true);
            group.add(more);
            fillCharsetActions(more, myVirtualFile, Arrays.asList(CharsetToolkit.getAvailableCharsets()), charsetFilter);
        }
        return group;
    }
}
