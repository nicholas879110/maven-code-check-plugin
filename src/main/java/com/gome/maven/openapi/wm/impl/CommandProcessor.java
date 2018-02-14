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
package com.gome.maven.openapi.wm.impl;

import com.gome.maven.openapi.application.Application;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.application.ModalityState;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.Conditions;
import com.gome.maven.openapi.util.registry.Registry;
import com.gome.maven.openapi.wm.impl.commands.FinalizableCommand;

import java.util.ArrayList;
import java.util.List;

public final class CommandProcessor implements Runnable {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.wm.impl.CommandProcessor");
    private final Object myLock = new Object();

    private final List<CommandGroup> myCommandGroupList = new ArrayList<CommandGroup>();
    private int myCommandCount;

    public final int getCommandCount() {
        synchronized (myLock) {
            return myCommandCount;
        }
    }

    /**
     * Executes passed batch of commands. Note, that the processor surround the
     * commands with BlockFocusEventsCmd - UnbockFocusEventsCmd. It's required to
     * prevent focus handling of events which is caused by the commands to be executed.
     */
    public final void execute( List<FinalizableCommand> commandList,  Condition expired) {
        synchronized (myLock) {
            final boolean isBusy = myCommandCount > 0;

            final CommandGroup commandGroup = new CommandGroup(commandList, expired);
            myCommandGroupList.add(commandGroup);
            myCommandCount += commandList.size();

            if (!isBusy) {
                run();
            }
        }
    }

    public final void run() {
        synchronized (myLock) {
            final CommandGroup commandGroup = getNextCommandGroup();
            if (commandGroup == null || commandGroup.isEmpty()) return;

            final Condition conditionForGroup = commandGroup.getExpireCondition();

            final FinalizableCommand command = commandGroup.takeNextCommand();
            myCommandCount--;

            final Condition expire = command.getExpireCondition() != null ? command.getExpireCondition() : conditionForGroup;

            if (LOG.isDebugEnabled()) {
                LOG.debug("CommandProcessor.run " + command);
            }
            // max. I'm not actually quite sure this should have NON_MODAL modality but it should
            // definitely have some since runnables in command list may (and do) request some PSI activity
            final boolean queueNext = myCommandCount > 0;
            Application application = ApplicationManager.getApplication();
            ModalityState modalityState = Registry.is("ide.perProjectModality") ? ModalityState.defaultModalityState() : ModalityState.NON_MODAL;
            application.getInvokator().invokeLater(command, modalityState, expire == null ? application.getDisposed() : expire).doWhenDone(new Runnable() {
                public void run() {
                    if (queueNext) {
                        CommandProcessor.this.run();
                    }
                }
            });
        }
    }

    private CommandGroup getNextCommandGroup() {
        while (!myCommandGroupList.isEmpty()) {
            final CommandGroup candidate = myCommandGroupList.get(0);
            if (!candidate.isEmpty()) {
                return candidate;
            }
            myCommandGroupList.remove(candidate);
        }

        return null;
    }

    private static class CommandGroup {
        private final List<FinalizableCommand> myList;
        private Condition myExpireCondition;

        private CommandGroup( List<FinalizableCommand> list,  Condition expireCondition) {
            myList = list;
            myExpireCondition = expireCondition;
        }

        public Condition getExpireCondition() {
            return myExpireCondition;
        }

        public boolean isEmpty() {
            return myList.isEmpty();
        }

        public FinalizableCommand takeNextCommand() {
            FinalizableCommand command = myList.remove(0);
            if (isEmpty()) {
                // memory leak otherwise
                myExpireCondition = Conditions.alwaysTrue();
            }
            return command;
        }
    }
}
