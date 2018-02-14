//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.gome.maven.openapi.keymap.impl.ui;

import com.gome.maven.openapi.actionSystem.ActionGroup;
import com.gome.maven.openapi.actionSystem.ActionManager;
import com.gome.maven.openapi.actionSystem.AnAction;
import com.gome.maven.openapi.actionSystem.DefaultActionGroup;
import com.gome.maven.openapi.actionSystem.Separator;
import com.gome.maven.openapi.actionSystem.ex.QuickList;
import com.gome.maven.openapi.keymap.KeymapGroup;
import com.gome.maven.openapi.util.text.StringUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.Icon;

public class Group implements KeymapGroup {
    private Group myParent;
    private final String myName;
    private String myId;
    private final Icon myIcon;
    private final ArrayList<Object> myChildren;
    private final Set<String> myIds = new HashSet();

    public Group(String name, String id, Icon icon) {
        this.myName = name;
        this.myId = id;
        this.myIcon = icon;
        this.myChildren = new ArrayList();
    }

    public Group(String name, Icon icon) {
        this.myChildren = new ArrayList();
        this.myIcon = icon;
        this.myName = name;
    }

    public String getName() {
        return this.myName;
    }

    public Icon getIcon() {
        return this.myIcon;
    }

    public String getId() {
        return this.myId;
    }

    public void addActionId(String id) {
        this.myChildren.add(id);
    }

    public void addQuickList(QuickList list) {
        this.myChildren.add(list);
    }

    public void addGroup(KeymapGroup keymapGroup) {
        Group group = (Group)keymapGroup;
        this.myChildren.add(group);
        group.myParent = this;
    }

    public void addSeparator() {
        this.myChildren.add(Separator.getInstance());
    }

    public boolean containsId(String id) {
        return this.myIds.contains(id);
    }

    public Set<String> initIds() {
        Iterator i$ = this.myChildren.iterator();

        while(i$.hasNext()) {
            Object child = i$.next();
            if (child instanceof String) {
                this.myIds.add((String)child);
            } else if (child instanceof QuickList) {
                this.myIds.add(((QuickList)child).getActionId());
            } else if (child instanceof Group) {
                this.myIds.addAll(((Group)child).initIds());
            }
        }

        return this.myIds;
    }

    public ArrayList<Object> getChildren() {
        return this.myChildren;
    }

    public int getSize() {
        return this.myChildren.size();
    }

    public void normalizeSeparators() {
        while(this.myChildren.size() > 0 && this.myChildren.get(0) instanceof Separator) {
            this.myChildren.remove(0);
        }

        while(this.myChildren.size() > 0 && this.myChildren.get(this.myChildren.size() - 1) instanceof Separator) {
            this.myChildren.remove(this.myChildren.size() - 1);
        }

        for(int i = 1; i < this.myChildren.size() - 1; ++i) {
            if (this.myChildren.get(i) instanceof Separator && this.myChildren.get(i + 1) instanceof Separator) {
                this.myChildren.remove(i);
                --i;
            }
        }

    }

    public String getActionQualifiedPath(String id) {
        Group cur = this.myParent;

        StringBuilder answer;
        for(answer = new StringBuilder(); cur != null && !cur.isRoot(); cur = cur.myParent) {
            answer.insert(0, cur.getName() + " | ");
        }

        String suffix = this.calcActionQualifiedPath(id);
        if (StringUtil.isEmpty(suffix)) {
            return null;
        } else {
            answer.append(suffix);
            return answer.toString();
        }
    }

    private String calcActionQualifiedPath(String id) {
        Iterator i$ = this.myChildren.iterator();

        while(i$.hasNext()) {
            Object child = i$.next();
            if (child instanceof QuickList) {
                child = ((QuickList)child).getActionId();
            }

            if (child instanceof String) {
                if (id.equals(child)) {
                    AnAction action = ActionManager.getInstance().getActionOrStub(id);
                    String path;
                    if (action != null) {
                        path = action.getTemplatePresentation().getText();
                    } else {
                        path = id;
                    }

                    return !this.isRoot() ? this.getName() + " | " + path : path;
                }
            } else if (child instanceof Group) {
                String path = ((Group)child).calcActionQualifiedPath(id);
                if (path != null) {
                    return !this.isRoot() ? this.getName() + " | " + path : path;
                }
            }
        }

        return null;
    }

    public boolean isRoot() {
        return this.myParent == null;
    }

    public String getQualifiedPath() {
        StringBuilder path = new StringBuilder(64);

        for(Group group = this; group != null && !group.isRoot(); group = group.myParent) {
            if (path.length() > 0) {
                path.insert(0, " | ");
            }

            path.insert(0, group.getName());
        }

        return path.toString();
    }

    public void addAll(Group group) {
        Iterator i$ = group.getChildren().iterator();

        while(i$.hasNext()) {
            Object o = i$.next();
            if (o instanceof String) {
                this.addActionId((String)o);
            } else if (o instanceof QuickList) {
                this.addQuickList((QuickList)o);
            } else if (o instanceof Group) {
                this.addGroup((Group)o);
            } else if (o instanceof Separator) {
                this.addSeparator();
            }
        }

    }

    public ActionGroup constructActionGroup(boolean popup) {
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup group = new DefaultActionGroup(this.getName(), popup);
        AnAction groupToRestorePresentation = null;
        if (this.getName() != null) {
            groupToRestorePresentation = actionManager.getAction(this.getName());
        } else if (this.getId() != null) {
            groupToRestorePresentation = actionManager.getAction(this.getId());
        }

        if (groupToRestorePresentation != null) {
            group.copyFrom(groupToRestorePresentation);
        }

        Iterator i$ = this.myChildren.iterator();

        while(i$.hasNext()) {
            Object o = i$.next();
            if (o instanceof String) {
                group.add(actionManager.getAction((String)o));
            } else if (o instanceof Separator) {
                group.addSeparator();
            } else if (o instanceof Group) {
                group.add(((Group)o).constructActionGroup(popup));
            }
        }

        return group;
    }

    public boolean equals(Object object) {
        if (!(object instanceof Group)) {
            return false;
        } else {
            Group group = (Group)object;
            if (group.getName() != null && this.getName() != null) {
                return group.getName().equals(this.getName());
            } else if (this.getChildren() != null && group.getChildren() != null) {
                if (this.getChildren().size() != group.getChildren().size()) {
                    return false;
                } else {
                    for(int i = 0; i < this.getChildren().size(); ++i) {
                        if (!this.getChildren().get(i).equals(group.getChildren().get(i))) {
                            return false;
                        }
                    }

                    return true;
                }
            } else {
                return false;
            }
        }
    }

    public int hashCode() {
        return this.getName() != null ? this.getName().hashCode() : 0;
    }

    public String toString() {
        return this.getName();
    }
}
