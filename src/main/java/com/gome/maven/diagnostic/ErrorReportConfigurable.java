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
package com.gome.maven.diagnostic;

import com.gome.maven.openapi.components.NamedComponent;
import com.gome.maven.openapi.components.ServiceManager;
import com.gome.maven.openapi.util.DefaultJDOMExternalizer;
import com.gome.maven.openapi.util.InvalidDataException;
import com.gome.maven.openapi.util.JDOMExternalizable;
import com.gome.maven.openapi.util.WriteExternalException;
import com.gome.maven.openapi.vfs.CharsetToolkit;
import com.gome.maven.util.Base64;
import org.jdom.Element;

public class ErrorReportConfigurable implements JDOMExternalizable, NamedComponent {
    public String ITN_LOGIN = "";
    public String ITN_PASSWORD_CRYPT = "";
    public boolean KEEP_ITN_PASSWORD = false;

    public String EMAIL = "";

    public static ErrorReportConfigurable getInstance() {
        return ServiceManager.getService(ErrorReportConfigurable.class);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
        if (!KEEP_ITN_PASSWORD) {
            ITN_PASSWORD_CRYPT = "";
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        String itnPassword = ITN_PASSWORD_CRYPT;
        if (!KEEP_ITN_PASSWORD) {
            ITN_PASSWORD_CRYPT = "";
        }
        DefaultJDOMExternalizer.writeExternal(this, element);

        ITN_PASSWORD_CRYPT = itnPassword;
    }

    @Override
    public String getComponentName() {
        return "ErrorReportConfigurable";
    }

    public String getPlainItnPassword() {
        return new String(Base64.decode(getInstance().ITN_PASSWORD_CRYPT), CharsetToolkit.UTF8_CHARSET);
    }

    public void setPlainItnPassword(String password) {
        ITN_PASSWORD_CRYPT = Base64.encode(password.getBytes(CharsetToolkit.UTF8_CHARSET));
    }
}
