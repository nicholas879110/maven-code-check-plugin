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
package com.gome.maven.openapi.components.impl.stores;

import com.gome.maven.notification.Notification;
import com.gome.maven.notification.NotificationListener;
import com.gome.maven.notification.NotificationType;


import java.util.Collection;

/**
 * @author spleaner
 */
public class UnknownMacroNotification extends Notification {
    private final Collection<String> myMacros;

    public UnknownMacroNotification( String groupId,
                                     String title,
                                     String content,
                                     NotificationType type,
                                     NotificationListener listener,
                                     Collection<String> macros) {
        super(groupId, title, content, type, listener);

        myMacros = macros;
    }

    public Collection<String> getMacros() {
        return myMacros;
    }
}
