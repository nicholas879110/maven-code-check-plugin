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
package com.gome.maven.openapi.ui.playback.commands;

import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.ui.playback.PlaybackCommand;
import com.gome.maven.openapi.ui.playback.PlaybackContext;
import com.gome.maven.openapi.util.ActionCallback;

import javax.swing.*;
import java.io.File;

public abstract class AbstractCommand implements PlaybackCommand {

    public static final String CMD_PREFIX = "%";

    private final String myText;
    private final int myLine;
    private final boolean myExecuteInAwt;

    private File myScriptDir;

    public AbstractCommand(String text, int line) {
        this(text, line, false);
    }

    public AbstractCommand(String text, int line, boolean executeInAwt) {
        myExecuteInAwt = executeInAwt;
        myText = text != null ? text : null;
        myLine = line;
    }

    public String getText() {
        return myText;
    }

    public int getLine() {
        return myLine;
    }

    public boolean canGoFurther() {
        return true;
    }

    public final ActionCallback execute(final PlaybackContext context) {
        try {
            if (isToDumpCommand()) {
                dumpCommand(context);
            }
            final ActionCallback result = new ActionCallback();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        _execute(context).notify(result);
                    }
                    catch (Throwable e) {
                        context.error(e.getMessage(), getLine());
                        result.setRejected();
                    }
                }
            };

            if (isAwtThread()) {
                // prevent previous action context affecting next action.
                // E.g. previous action may have called callback.setDone from inside write action, while
                // next action may not expect that

                //noinspection SSBasedInspection
                SwingUtilities.invokeLater(runnable);
            }
            else {
                ApplicationManager.getApplication().executeOnPooledThread(runnable);
            }

            return result;
        }
        catch (Throwable e) {
            context.error(e.getMessage(), getLine());
            return new ActionCallback.Rejected();
        }
    }

    protected boolean isToDumpCommand() {
        return true;
    }

    protected boolean isAwtThread() {
        return myExecuteInAwt;
    }

    protected abstract ActionCallback _execute(PlaybackContext context);

    public void dumpCommand(PlaybackContext context) {
        context.code(getText(), getLine());
    }

    public void dumpError(PlaybackContext context, final String text) {
        context.error(text, getLine());
    }

    @Override
    public File getScriptDir() {
        return myScriptDir;
    }


    public PlaybackCommand setScriptDir(File scriptDir) {
        myScriptDir = scriptDir;
        return this;
    }
}
