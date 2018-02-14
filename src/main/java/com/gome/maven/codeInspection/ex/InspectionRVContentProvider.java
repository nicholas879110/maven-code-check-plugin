/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
 * Date: 10-Jan-2007
 */
package com.gome.maven.codeInspection.ex;

import com.gome.maven.codeInspection.CommonProblemDescriptor;
import com.gome.maven.codeInspection.QuickFix;
import com.gome.maven.codeInspection.reference.RefEntity;
import com.gome.maven.codeInspection.ui.*;
import com.gome.maven.openapi.application.ApplicationManager;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.module.Module;
import com.gome.maven.openapi.module.ModuleManager;
import com.gome.maven.openapi.project.Project;
import com.gome.maven.openapi.util.Ref;
import com.gome.maven.util.Function;
import com.gome.maven.util.ui.tree.TreeUtil;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public abstract class InspectionRVContentProvider {
    private static final Logger LOG = Logger.getInstance("#" + InspectionRVContentProvider.class.getName());
    private final Project myProject;

    public InspectionRVContentProvider( Project project) {
        myProject = project;
    }

    protected interface UserObjectContainer<T> {
        
        UserObjectContainer<T> getOwner();

        
        RefElementNode createNode( InspectionToolPresentation presentation);

        
        T getUserObject();

        
        String getModule();

        boolean areEqual(final T o1, final T o2);

        boolean supportStructure();
    }

    public abstract boolean checkReportedProblems( GlobalInspectionContextImpl context,  InspectionToolWrapper toolWrapper);

    public boolean hasQuickFixes(InspectionTree tree) {
        final TreePath[] treePaths = tree.getSelectionPaths();
        if (treePaths == null) return false;
        for (TreePath selectionPath : treePaths) {
            if (!TreeUtil.traverseDepth((TreeNode)selectionPath.getLastPathComponent(), new TreeUtil.Traverse() {
                @Override
                public boolean accept(final Object node) {
                    if (!((InspectionTreeNode)node).isValid()) return true;
                    if (node instanceof ProblemDescriptionNode) {
                        final CommonProblemDescriptor descriptor = ((ProblemDescriptionNode)node).getDescriptor();
                        final QuickFix[] fixes = descriptor != null ? descriptor.getFixes() : null;
                        return fixes == null || fixes.length == 0;
                    }
                    return true;
                }
            })) {
                return true;
            }
        }
        return false;
    }

    
    public abstract QuickFixAction[] getQuickFixes( InspectionToolWrapper toolWrapper,  InspectionTree tree);


    public void appendToolNodeContent( GlobalInspectionContextImpl context,
                                       InspectionNode toolNode,
                                       InspectionTreeNode parentNode,
                                      final boolean showStructure) {
        InspectionToolWrapper wrapper = toolNode.getToolWrapper();
        InspectionToolPresentation presentation = context.getPresentation(wrapper);
        Map<String, Set<RefEntity>> content = presentation.getContent();
        Map<RefEntity, CommonProblemDescriptor[]> problems = presentation.getProblemElements();
        appendToolNodeContent(context, toolNode, parentNode, showStructure, content, problems, null);
    }

    public abstract void appendToolNodeContent( GlobalInspectionContextImpl context,
                                                InspectionNode toolNode,
                                                InspectionTreeNode parentNode,
                                               final boolean showStructure,
                                                Map<String, Set<RefEntity>> contents,
                                                Map<RefEntity, CommonProblemDescriptor[]> problems,
                                                final DefaultTreeModel model);

    protected abstract void appendDescriptor( GlobalInspectionContextImpl context,
                                              InspectionToolWrapper toolWrapper,
                                              UserObjectContainer container,
                                              InspectionPackageNode pNode,
                                             final boolean canPackageRepeat);

    public boolean isContentLoaded() {
        return true;
    }

    protected <T> List<InspectionTreeNode> buildTree( GlobalInspectionContextImpl context,
                                                      Map<String, Set<T>> packageContents,
                                                     final boolean canPackageRepeat,
                                                      InspectionToolWrapper toolWrapper,
                                                      Function<T, UserObjectContainer<T>> computeContainer,
                                                     final boolean showStructure) {
        final List<InspectionTreeNode> content = new ArrayList<InspectionTreeNode>();
        final Map<String, Map<String, InspectionPackageNode>> module2PackageMap = new HashMap<String, Map<String, InspectionPackageNode>>();
        boolean supportStructure = showStructure;
        for (String packageName : packageContents.keySet()) {
            final Set<T> elements = packageContents.get(packageName);
            for (T userObject : elements) {
                final UserObjectContainer<T> container = computeContainer.fun(userObject);
                supportStructure &= container.supportStructure();
                final String moduleName = showStructure ? container.getModule() : null;
                Map<String, InspectionPackageNode> packageNodes = module2PackageMap.get(moduleName);
                if (packageNodes == null) {
                    packageNodes = new HashMap<String, InspectionPackageNode>();
                    module2PackageMap.put(moduleName, packageNodes);
                }
                InspectionPackageNode pNode = packageNodes.get(packageName);
                if (pNode == null) {
                    pNode = new InspectionPackageNode(packageName);
                    packageNodes.put(packageName, pNode);
                }
                appendDescriptor(context, toolWrapper, container, pNode, canPackageRepeat);
            }
        }
        if (supportStructure) {
            final HashMap<String, InspectionModuleNode> moduleNodes = new HashMap<String, InspectionModuleNode>();
            for (final String moduleName : module2PackageMap.keySet()) {
                final Map<String, InspectionPackageNode> packageNodes = module2PackageMap.get(moduleName);
                for (InspectionPackageNode packageNode : packageNodes.values()) {
                    if (packageNode.getChildCount() > 0) {
                        InspectionModuleNode moduleNode = moduleNodes.get(moduleName);
                        if (moduleNode == null) {
                            if (moduleName != null) {
                                final Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleName);
                                if (module != null) {
                                    moduleNode = new InspectionModuleNode(module);
                                    moduleNodes.put(moduleName, moduleNode);
                                }
                                else { //module content was removed ?
                                    continue;
                                }
                            } else {
                                content.addAll(packageNodes.values());
                                break;
                            }
                        }
                        if (packageNode.getPackageName() != null) {
                            moduleNode.add(packageNode);
                        } else {
                            for(int i = packageNode.getChildCount() - 1; i >= 0; i--) {
                                moduleNode.add((MutableTreeNode)packageNode.getChildAt(i));
                            }
                        }
                    }
                }
            }
            content.addAll(moduleNodes.values());
        }
        else {
            for (Map<String, InspectionPackageNode> packageNodes : module2PackageMap.values()) {
                for (InspectionPackageNode pNode : packageNodes.values()) {
                    for (int i = 0; i < pNode.getChildCount(); i++) {
                        final TreeNode childNode = pNode.getChildAt(i);
                        if (childNode instanceof ProblemDescriptionNode) {
                            content.add(pNode);
                            break;
                        }
                        LOG.assertTrue(childNode instanceof RefElementNode, childNode.getClass().getName());
                        final RefElementNode elementNode = (RefElementNode)childNode;
                        final Set<RefElementNode> parentNodes = new LinkedHashSet<RefElementNode>();
                        if (pNode.getPackageName() != null) {
                            parentNodes.add(elementNode);
                        } else {
                            boolean hasElementNodeUnder = true;
                            for(int e = 0; e < elementNode.getChildCount(); e++) {
                                final TreeNode grandChildNode = elementNode.getChildAt(e);
                                if (grandChildNode instanceof ProblemDescriptionNode) {
                                    hasElementNodeUnder = false;
                                    break;
                                }
                                LOG.assertTrue(grandChildNode instanceof RefElementNode);
                                parentNodes.add((RefElementNode)grandChildNode);
                            }
                            if (!hasElementNodeUnder) {
                                content.add(elementNode);
                                continue;
                            }
                        }
                        for (RefElementNode parentNode : parentNodes) {
                            final List<ProblemDescriptionNode> nodes = new ArrayList<ProblemDescriptionNode>();
                            TreeUtil.traverse(parentNode, new TreeUtil.Traverse() {
                                @Override
                                public boolean accept(final Object node) {
                                    if (node instanceof ProblemDescriptionNode) {
                                        nodes.add((ProblemDescriptionNode)node);
                                    }
                                    return true;
                                }
                            });
                            if (nodes.isEmpty()) continue;  //FilteringInspectionTool == DeadCode
                            parentNode.removeAllChildren();
                            for (ProblemDescriptionNode node : nodes) {
                                parentNode.add(node);
                            }
                        }
                        content.addAll(parentNodes);
                    }
                }
            }
        }
        return content;
    }

    
    protected static RefElementNode addNodeToParent( UserObjectContainer container,
                                                     InspectionToolPresentation presentation,
                                                    final InspectionTreeNode parentNode) {
        final RefElementNode nodeToBeAdded = container.createNode(presentation);
        final Ref<Boolean> firstLevel = new Ref<Boolean>(true);
        RefElementNode prevNode = null;
        final Ref<RefElementNode> result = new Ref<RefElementNode>();
        while (true) {
            final RefElementNode currentNode = firstLevel.get() ? nodeToBeAdded : container.createNode(presentation);
            final UserObjectContainer finalContainer = container;
            final RefElementNode finalPrevNode = prevNode;
            TreeUtil.traverseDepth(parentNode, new TreeUtil.Traverse() {
                @Override
                public boolean accept(Object node) {
                    if (node instanceof RefElementNode) {
                        final RefElementNode refElementNode = (RefElementNode)node;
                        final Object userObject = finalContainer.getUserObject();
                        final Object object = refElementNode.getUserObject();
                        if ((object == null || userObject.getClass().equals(object.getClass())) && finalContainer.areEqual(object, userObject)) {
                            if (firstLevel.get()) {
                                result.set(refElementNode);
                                return false;
                            }
                            else {
                                insertByIndex(finalPrevNode, refElementNode);
                                result.set(nodeToBeAdded);
                                return false;
                            }
                        }
                    }
                    return true;
                }
            });
            if(!result.isNull()) return result.get();

            if (!firstLevel.get()) {
                insertByIndex(prevNode, currentNode);
            }
            final UserObjectContainer owner = container.getOwner();
            if (owner == null) {
                insertByIndex(currentNode, parentNode);
                return nodeToBeAdded;
            }
            container = owner;
            prevNode = currentNode;
            firstLevel.set(false);
        }
    }

    @SuppressWarnings({"ConstantConditions"}) //class cast suppression
    protected static void merge( DefaultTreeModel model, InspectionTreeNode child, InspectionTreeNode parent, boolean merge) {
        if (merge) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                InspectionTreeNode current = (InspectionTreeNode)parent.getChildAt(i);
                if (child.getClass() != current.getClass()) {
                    continue;
                }
                if (current instanceof InspectionPackageNode) {
                    if (((InspectionPackageNode)current).getPackageName().compareTo(((InspectionPackageNode)child).getPackageName()) == 0) {
                        processDepth(model, child, current);
                        return;
                    }
                }
                else if (current instanceof RefElementNode) {
                    if (((RefElementNode)current).getElement().getName().compareTo(((RefElementNode)child).getElement().getName()) == 0) {
                        processDepth(model, child, current);
                        return;
                    }
                }
                else if (current instanceof InspectionNode) {
                    if (((InspectionNode)current).getToolWrapper().getShortName().compareTo(((InspectionNode)child).getToolWrapper().getShortName()) == 0) {
                        processDepth(model, child, current);
                        return;
                    }
                }
                else if (current instanceof InspectionModuleNode) {
                    if (((InspectionModuleNode)current).getName().compareTo(((InspectionModuleNode)child).getName()) == 0) {
                        processDepth(model, child, current);
                        return;
                    }
                }
            }
        }
        add(model, child, parent);
    }

    protected static void add( final DefaultTreeModel model, final InspectionTreeNode child, final InspectionTreeNode parent) {
        if (model == null) {
            insertByIndex(child, parent);
        }
        else {
            if (parent.getIndex(child) < 0) {
                model.insertNodeInto(child, parent, child.getParent() == parent ? parent.getChildCount() - 1 : parent.getChildCount());
            }
        }
    }

    private static void insertByIndex(InspectionTreeNode child, InspectionTreeNode parent) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            parent.add(child);
            return;
        }
        final int i = TreeUtil.indexedBinarySearch(parent, child, InspectionResultsViewComparator.getInstance());
        if (i >= 0){
            parent.add(child);
            return;
        }
        parent.insert(child, -i -1);
    }

    private static void processDepth( DefaultTreeModel model, final InspectionTreeNode child, final InspectionTreeNode current) {
        InspectionTreeNode[] children = new InspectionTreeNode[child.getChildCount()];
        for (int i = 0; i < children.length; i++) {
            children[i] = (InspectionTreeNode)child.getChildAt(i);
        }
        for (InspectionTreeNode node : children) {
            merge(model, node, current, true);
        }
    }
}
