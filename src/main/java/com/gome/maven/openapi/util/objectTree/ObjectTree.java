package com.gome.maven.openapi.util.objectTree;

import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.Disposer;
import com.gome.maven.util.ArrayUtil;
import com.gome.maven.util.containers.ContainerUtil;
import gnu.trove.Equality;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:13
 * @opyright(c) gome inc Gome Co.,LTD
 */
public final class ObjectTree<T> {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.openapi.util.objectTree.ObjectTree");

    private final List<ObjectTreeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    // identity used here to prevent problems with hashCode/equals overridden by not very bright minds
    private final Set<T> myRootObjects = ContainerUtil.newIdentityTroveSet(); // guarded by treeLock
    private final Map<T, ObjectNode<T>> myObject2NodeMap = ContainerUtil.newIdentityTroveMap(); // guarded by treeLock

    private final List<ObjectNode<T>> myExecutedNodes = new ArrayList<ObjectNode<T>>(); // guarded by myExecutedNodes
    private final List<T> myExecutedUnregisteredNodes = new ArrayList<T>(); // guarded by myExecutedUnregisteredNodes

    final Object treeLock = new Object();

    private final AtomicLong myModification = new AtomicLong(0);

    ObjectNode<T> getNode( T object) {
        return myObject2NodeMap.get(object);
    }

    ObjectNode<T> putNode( T object, ObjectNode<T> node) {
        return node == null ? myObject2NodeMap.remove(object) : myObject2NodeMap.put(object, node);
    }

    
    final List<ObjectNode<T>> getNodesInExecution() {
        return myExecutedNodes;
    }

    public final void register( T parent,  T child) {
        synchronized (treeLock) {
            ObjectNode<T> parentNode = getOrCreateNodeFor(parent, null);

            ObjectNode<T> childNode = getNode(child);
            if (childNode == null) {
                childNode = createNodeFor(child, parentNode, Disposer.isDebugMode() ? new Throwable() : null);
            }
            else {
                ObjectNode<T> oldParent = childNode.getParent();
                if (oldParent != null) {
                    oldParent.removeChild(childNode);
                }
            }
            myRootObjects.remove(child);
            checkWasNotAddedAlready(childNode, child);
            parentNode.addChild(childNode);

            fireRegistered(childNode.getObject());
        }
    }

    private void checkWasNotAddedAlready( ObjectNode<T> childNode,  T child) {
        ObjectNode parent = childNode.getParent();
        boolean childIsInTree = parent != null;
        if (!childIsInTree) return;

        while (parent != null) {
            if (parent.getObject() == child) {
                LOG.error(child + " was already added as a child of: " + parent);
            }
            parent = parent.getParent();
        }
    }

    
    private ObjectNode<T> getOrCreateNodeFor( T object, ObjectNode<T> defaultParent) {
        final ObjectNode<T> node = getNode(object);

        if (node != null) return node;

        return createNodeFor(object, defaultParent, Disposer.isDebugMode() ? new Throwable() : null);
    }

    
    private ObjectNode<T> createNodeFor( T object, ObjectNode<T> parentNode, final Throwable trace) {
        final ObjectNode<T> newNode = new ObjectNode<T>(this, parentNode, object, getNextModification(), trace);
        if (parentNode == null) {
            myRootObjects.add(object);
        }
        putNode(object, newNode);
        return newNode;
    }

    private long getNextModification() {
        return myModification.incrementAndGet();
    }

    public final boolean executeAll( T object, boolean disposeTree,  ObjectTreeAction<T> action, boolean processUnregistered) {
        ObjectNode<T> node;
        synchronized (treeLock) {
            node = getNode(object);
        }
        if (node == null) {
            if (processUnregistered) {
                executeUnregistered(object, action);
                return true;
            }
            else {
                return false;
            }
        }
        node.execute(disposeTree, action);
        return true;
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static <T> void executeActionWithRecursiveGuard( T object,
                                                     List<T> recursiveGuard,
                                                     final ObjectTreeAction<T> action) {
        synchronized (recursiveGuard) {
            if (ArrayUtil.indexOf(recursiveGuard, object, Equality.IDENTITY) != -1) return;
            recursiveGuard.add(object);
        }

        try {
            action.execute(object);
        }
        finally {
            synchronized (recursiveGuard) {
                int i = ArrayUtil.lastIndexOf(recursiveGuard, object, Equality.IDENTITY);
                assert i != -1;
                recursiveGuard.remove(i);
            }
        }
    }

    private void executeUnregistered( final T object,  final ObjectTreeAction<T> action) {
        executeActionWithRecursiveGuard(object, myExecutedUnregisteredNodes, action);
    }

    public final void executeChildAndReplace( T toExecute,  T toReplace, boolean disposeTree,  ObjectTreeAction<T> action) {
        final ObjectNode<T> toExecuteNode;
        T parentObject;
        synchronized (treeLock) {
            toExecuteNode = getNode(toExecute);
            assert toExecuteNode != null : "Object " + toExecute + " wasn't registered or already disposed";

            final ObjectNode<T> parent = toExecuteNode.getParent();
            assert parent != null : "Object " + toExecute + " is not connected to the tree - doesn't have parent";
            parentObject = parent.getObject();
        }

        toExecuteNode.execute(disposeTree, action);
        register(parentObject, toReplace);
    }

    public boolean containsKey( T object) {
        synchronized (treeLock) {
            return getNode(object) != null;
        }
    }


    // public for Upsource
    public void assertNoReferenceKeptInTree( T disposable) {
        synchronized (treeLock) {
            Collection<ObjectNode<T>> nodes = myObject2NodeMap.values();
            for (ObjectNode<T> node : nodes) {
                node.assertNoReferencesKept(disposable);
            }
        }
    }

    void removeRootObject( T object) {
        myRootObjects.remove(object);
    }

    @SuppressWarnings({"UseOfSystemOutOrSystemErr", "HardCodedStringLiteral"})
    public void assertIsEmpty(boolean throwError) {
        synchronized (treeLock) {
            for (T object : myRootObjects) {
                if (object == null) continue;
                ObjectNode<T> objectNode = getNode(object);
                if (objectNode == null) continue;
                while (objectNode.getParent() != null) {
                    objectNode = objectNode.getParent();
                }
                final Throwable trace = objectNode.getTrace();
                RuntimeException exception = new RuntimeException("Memory leak detected: " + object + " of class " + object.getClass()
                        + "\nSee the cause for the corresponding Disposer.register() stacktrace:\n",
                        trace);
                if (throwError) {
                    throw exception;
                }
                LOG.error(exception);
            }
        }
    }


    public boolean isEmpty() {
        synchronized (treeLock) {
            return myRootObjects.isEmpty();
        }
    }

    
    Set<T> getRootObjects() {
        synchronized (treeLock) {
            return myRootObjects;
        }
    }

    void addListener( ObjectTreeListener listener) {
        myListeners.add(listener);
    }

    void removeListener( ObjectTreeListener listener) {
        myListeners.remove(listener);
    }

    private void fireRegistered( Object object) {
        for (ObjectTreeListener each : myListeners) {
            each.objectRegistered(object);
        }
    }

    void fireExecuted( Object object) {
        for (ObjectTreeListener each : myListeners) {
            each.objectExecuted(object);
        }
    }

    int size() {
        synchronized (treeLock) {
            return myObject2NodeMap.size();
        }
    }


    public <D extends Disposable> D findRegisteredObject(T parentDisposable, D object) {
        synchronized (treeLock) {
            ObjectNode<T> parentNode = getNode(parentDisposable);
            if (parentNode == null) return null;
            return parentNode.findChildEqualTo(object);
        }
    }

    long getModification() {
        return myModification.get();
    }
}
