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
package com.gome.maven.psi.search.scope.packageSet;

import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.vfs.VirtualFile;

public class UnionPackageSet extends PackageSetBase {
    private final PackageSet myFirstSet;
    private final PackageSet mySecondSet;

    private String myText;

    public UnionPackageSet( PackageSet set1,  PackageSet set2) {
        myFirstSet = set1;
        mySecondSet = set2;
    }

    @Override
    public boolean contains(VirtualFile file,  NamedScopesHolder holder) {
        return contains(file, holder.getProject(), holder);
    }

    @Override
    public boolean contains(VirtualFile file,  Project project,  NamedScopesHolder holder) {
        return (myFirstSet instanceof PackageSetBase ? ((PackageSetBase)myFirstSet).contains(file, project, holder) : myFirstSet.contains(getPsiFile(file, project), holder)) ||
                (mySecondSet instanceof PackageSetBase ? ((PackageSetBase)mySecondSet).contains(file, project, holder) : mySecondSet.contains(getPsiFile(file, project), holder));
    }

    @Override
    
    public PackageSet createCopy() {
        return new UnionPackageSet(myFirstSet.createCopy(), mySecondSet.createCopy());
    }

    @Override
    public int getNodePriority() {
        return 3;
    }

    @Override
    
    public String getText() {
        if (myText == null) {
            myText = myFirstSet.getText() + "||" + mySecondSet.getText();
        }
        return myText;
    }

    public PackageSet getFirstSet() {
        return myFirstSet;
    }

    public PackageSet getSecondSet() {
        return mySecondSet;
    }
}