package com.gome.maven.openapi.util.objectTree;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.progress.ProcessCanceledException;
import com.gome.maven.util.SmartList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:26
 * @opyright(c) gome inc Gome Co.,LTD
 */
final class ObjectNode<T> {
    private static final ObjectNode[] EMPTY_ARRAY = new ObjectNode[0];

    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.util.objectTree.ObjectNode");

    private final ObjectTree<T> myTree;

    private ObjectNode<T> myParent; // guarded by myTree.treeLock
    private final T myObject;

    private List<ObjectNode<T>> myChildren; // guarded by myTree.treeLock
    private final Throwable myTrace;

    private final long myOwnModification;

    ObjectNode( ObjectTree<T> tree,
                ObjectNode<T> parentNode,
                T object,
               long modification,
                final Throwable trace) {
        myTree = tree;
        myParent = parentNode;
        myObject = object;

        myTrace = trace;
        myOwnModification = modification;
    }

    @SuppressWarnings("unchecked")
    
    private ObjectNode<T>[] getChildrenArray() {
        List<ObjectNode<T>> children = myChildren;
        if (children == null || children.isEmpty()) return EMPTY_ARRAY;
        return children.toArray(new ObjectNode[children.size()]);
    }

    void addChild( ObjectNode<T> child) {
        List<ObjectNode<T>> children = myChildren;
        if (children == null) {
            myChildren = new SmartList<ObjectNode<T>>(child);
        }
        else {
            children.add(child);
        }
        child.myParent = this;
    }

    void removeChild( ObjectNode<T> child) {
        List<ObjectNode<T>> children = myChildren;
        if (children != null) {
            // optimisation: iterate backwards
            for (int i = children.size() - 1; i >= 0; i--) {
                ObjectNode<T> node = children.get(i);
                if (node.equals(child)) {
                    children.remove(i);
                    break;
                }
            }
        }
        child.myParent = null;
    }

    ObjectNode<T> getParent() {
        return myParent;
    }

    
    Collection<ObjectNode<T>> getChildren() {
        synchronized (myTree.treeLock) {
            if (myChildren == null) return Collections.emptyList();
            return Collections.unmodifiableCollection(myChildren);
        }
    }

    void execute(final boolean disposeTree,  final ObjectTreeAction<T> action) {
        ObjectTree.executeActionWithRecursiveGuard(this, myTree.getNodesInExecution(), new ObjectTreeAction<ObjectNode<T>>() {
            @Override
            public void execute( ObjectNode<T> each) {
                try {
                    action.beforeTreeExecution(myObject);
                }
                catch (Throwable t) {
                    LOG.error(t);
                }

                ObjectNode<T>[] childrenArray;
                synchronized (myTree.treeLock) {
                    childrenArray = getChildrenArray();
                }
                //todo: [kirillk] optimize
                for (int i = childrenArray.length - 1; i >= 0; i--) {
                    childrenArray[i].execute(disposeTree, action);
                }

                if (disposeTree) {
                    synchronized (myTree.treeLock) {
                        myChildren = null;
                    }
                }

                try {
                    action.execute(myObject);
                    myTree.fireExecuted(myObject);
                }
                catch (ProcessCanceledException e) {
                    throw new ProcessCanceledException(e);
                }
                catch (Throwable e) {
                    LOG.error(e);
                }

                if (disposeTree) {
                    remove();
                }
            }

            @Override
            public void beforeTreeExecution( ObjectNode<T> parent) {

            }
        });
    }

    private void remove() {
        synchronized (myTree.treeLock) {
            myTree.putNode(myObject, null);
            if (myParent == null) {
                myTree.removeRootObject(myObject);
            }
            else {
                myParent.removeChild(this);
            }
        }
    }

    
    T getObject() {
        return myObject;
    }

    @Override
    public String toString() {
        return "Node: " + myObject.toString();
    }

    Throwable getTrace() {
        return myTrace;
    }


    void assertNoReferencesKept( T aDisposable) {
        assert getObject() != aDisposable;
        synchronized (myTree.treeLock) {
            if (myChildren != null) {
                for (ObjectNode<T> node: myChildren) {
                    node.assertNoReferencesKept(aDisposable);
                }
            }
        }
    }

    Throwable getAllocation() {
        return myTrace;
    }

    long getOwnModification() {
        return myOwnModification;
    }

    long getModification() {
        return getOwnModification();
    }

    <D extends Disposable> D findChildEqualTo(D object) {
        synchronized (myTree.treeLock) {
            List<ObjectNode<T>> children = myChildren;
            if (children != null) {
                for (ObjectNode<T> node : children) {
                    T nodeObject = node.getObject();
                    if (nodeObject.equals(object)) {
                        //noinspection unchecked
                        return (D)nodeObject;
                    }
                }
            }
            return null;
        }
    }
}