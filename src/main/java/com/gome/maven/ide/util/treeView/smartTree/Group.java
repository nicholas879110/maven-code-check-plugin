//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.ide.util.treeView.smartTree;

import com.gome.maven.navigation.ItemPresentation;
import java.util.Collection;

public interface Group {
    
    ItemPresentation getPresentation();

    
    Collection<TreeElement> getChildren();
}
