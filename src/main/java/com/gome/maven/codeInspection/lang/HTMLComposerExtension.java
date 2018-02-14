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

/*
 * User: anna
 * Date: 21-Dec-2007
 */
package com.gome.maven.codeInspection.lang;

import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.lang.Language;
import com.gome.maven.openapi.util.Key;

public interface HTMLComposerExtension<T> {
    Key<T> getID();
    Language getLanguage();

    void appendShortName(RefEntity entity, final StringBuffer buf);

    void appendLocation(RefEntity entity, final StringBuffer buf);


    String getQualifiedName(RefEntity entity);

    void appendReferencePresentation(RefEntity entity, final StringBuffer buf, final boolean isPackageIncluded);


}
