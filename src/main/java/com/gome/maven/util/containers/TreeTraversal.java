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
package com.gome.maven.util.containers;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.util.Consumer;
import com.gome.maven.util.Function;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A redesigned version of com.google.common.collect.TreeTraversal.
 * <p/>
 * The original JavaDoc:
 * <p/>
 * Views elements of a type {@code T} as nodes in a tree, and provides methods to traverse the trees
 * induced by this traverser.
 *
 * <p>For example, the tree
 *
 * <pre>          {@code
 *          h
 *        / | \
 *       /  e  \
 *      d       g
 *     /|\      |
 *    / | \     f
 *   a  b  c       }</pre>
 *
 * <p>can be iterated over in pre-order (hdabcegf), post-order (abcdefgh), or breadth-first order
 * (hdegabcf).
 *
 * <p>Null nodes are strictly forbidden.
 *
 * @author Louis Wasserman
 * <p/>
 *
 * @author gregsh
 */
public abstract class TreeTraversal {

    private final String debugName;

    protected TreeTraversal( String debugName) {
        this.debugName = debugName;
    }

    
    public <T> JBIterable<T> traversal( final Iterable<? extends T> roots,  final Function<T, ? extends Iterable<? extends T>> tree) {
        return new JBIterable<T>() {
            
            @Override
            public Iterator<T> iterator() {
                return createIterator(roots, tree);
            }
        };
    }

    
    public <T> JBIterable<T> traversal( final T root,  final Function<T, ? extends Iterable<? extends T>> tree) {
        return traversal(ContainerUtil.createMaybeSingletonList(root), tree);
    }

    
    public <T> Function<T, JBIterable<T>> traversal( final Function<T, ? extends Iterable<? extends T>> tree) {
        return new Function<T, JBIterable<T>>() {
            @Override
            public JBIterable<T> fun(T t) {
                return traversal(t, tree);
            }
        };
    }

    /**
     * Creates a new iterator for this type of traversal.
     * @param roots tree roots
     * @param tree tree structure the children for parent function.
     *             May return null (useful for map representation).
     */
    
    public abstract <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree);

    @Override
    public String toString() {
        return debugName;
    }

    public static abstract class It<T> extends JBIterator<T> {

    }

    public static abstract class TracingIt<T> extends It<T> {

        
        public abstract T parent();

        
        public abstract JBIterable<T> backtrace();
    }

    public static abstract class GuidedIt<T> extends It<T> {
        
        public T curChild, curParent;
        
        public Iterable<? extends T> curChildren;
        public boolean curNoChildren;

        public abstract GuidedIt<T> setGuide(Consumer<GuidedIt<T>> guide);

        public abstract GuidedIt<T> queueNext(T child);
        public abstract GuidedIt<T> queueLast(T child);
        public abstract GuidedIt<T> result(T node);

    }

    
    public static final TreeTraversal GUIDED_TRAVERSAL = new TreeTraversal("GUIDED_TRAVERSAL") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new GuidedItImpl<T>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the nodes in a tree structure, using pre-order
     * traversal. That is, each node's subtrees are traversed after the node itself is returned.
     *
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    
    public static final TreeTraversal PRE_ORDER_DFS = new TreeTraversal("PRE_ORDER_DFS") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new PreOrderIt<T>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the nodes in a tree structure, using post-order
     * traversal. That is, each node's subtrees are traversed before the node itself is returned.
     * <p/>
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    
    public static final TreeTraversal POST_ORDER_DFS = new TreeTraversal("POST_ORDER_DFS") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new PostOrderIt<T>(roots, tree);
        }
    };


    
    public static final TreeTraversal LEAVES_DFS = new TreeTraversal("LEAVES_DFS") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new LeavesDfsIt<T>(roots, tree);
        }
    };

    /**
     * Returns an iterator over the nodes in a tree structure, using breadth-first
     * traversal. That is, all the nodes of depth 0 are returned, then depth 1, then 2, and so on.
     * <p/>
     * <p>No guarantees are made about the behavior of the traversal when nodes change while
     * iteration is in progress or when the iterators generated by {@code tree} are advanced.
     */
    
    public static final TreeTraversal PLAIN_BFS = new TreeTraversal("PLAIN_BFS") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new BfsIterator<T>(roots, tree);
        }
    };

    
    public static final TreeTraversal TRACING_BFS = new TreeTraversal("TRACING_BFS") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new TracingBfsIt<T>(roots, tree);
        }
    };

    
    public static final TreeTraversal LEAVES_BFS = new TreeTraversal("LEAVES_BFS") {
        
        @Override
        public <T> It<T> createIterator( Iterable<? extends T> roots,  Function<T, ? extends Iterable<? extends T>> tree) {
            return new LeavesBfsIt<T>(roots, tree);
        }
    };


    // -----------------------------------------------------------------------------
    // Iterators: DFS
    // -----------------------------------------------------------------------------

    private abstract static class DfsIt<T> extends TracingIt<T> {
        final ArrayDeque<P<T>> stack = new ArrayDeque<P<T>>();

        
        public T parent() {
            if (stack.isEmpty()) throw new NoSuchElementException();
            Iterator<P<T>> it = stack.descendingIterator();
            it.next();
            return it.hasNext() ? it.next().node : null;
        }

        
        public JBIterable<T> backtrace() {
            if (stack.isEmpty()) throw new NoSuchElementException();
            return new JBIterable<P<T>>() {
                @Override
                public Iterator<P<T>> iterator() {
                    return stack.descendingIterator();
                }
            }.transform(P.<T>toNode()).filter(Condition.NOT_NULL);
        }
    }

    private final static class PreOrderIt<T> extends DfsIt<T> {

        final Function<T, ? extends Iterable<? extends T>> tree;

        PreOrderIt( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            stack.addLast(P.create(roots));
        }

        @Override
        public T nextImpl() {
            while (!stack.isEmpty()) {
                Iterator<? extends T> it = stack.getLast().iterator(tree);
                if (it.hasNext()) {
                    T result = it.next();
                    stack.addLast(P.create(result));
                    return result;
                }
                else {
                    stack.removeLast();
                }
            }
            return stop();
        }
    }

    private static final class PostOrderIt<T> extends DfsIt<T> {

        final Function<T, ? extends Iterable<? extends T>> tree;

        PostOrderIt( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            for (T root : roots) {
                stack.addLast(P.create(root));
            }
        }

        @Override
        public T nextImpl() {
            while (!stack.isEmpty()) {
                Iterator<? extends T> it = stack.getLast().iterator(tree);
                if (it.hasNext()) {
                    T result = it.next();
                    stack.addLast(P.create(result));
                }
                else {
                    return stack.removeLast().node;
                }
            }
            return stop();
        }
    }

    private final static class LeavesDfsIt<T> extends DfsIt<T> {

        final Function<T, ? extends Iterable<? extends T>> tree;

        LeavesDfsIt( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            stack.addLast(P.create(roots));
        }

        @Override
        public T nextImpl() {
            while (!stack.isEmpty()) {
                P<T> top = stack.getLast();
                if (top.iterator(tree).hasNext() && !top.empty) {
                    T child = top.iterator(tree).next();
                    stack.addLast(P.create(child));
                }
                else {
                    stack.removeLast();
                    if (top.empty) return stack.isEmpty() ? stop() : top.node;
                }
            }
            return stop();
        }
    }

    // -----------------------------------------------------------------------------
    // Iterators: BFS
    // -----------------------------------------------------------------------------

    private static final class BfsIterator<T> extends It<T> {

        final Function<T, ? extends Iterable<? extends T>> tree;
        final ArrayDeque<T> queue = new ArrayDeque<T>();

        BfsIterator( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            JBIterable.from(roots).addAllTo(queue);
        }

        @Override
        public T nextImpl() {
            if (queue.isEmpty()) return stop();
            T result = queue.remove();
            JBIterable.from(tree.fun(result)).addAllTo(queue);
            return result;
        }
    }

    private static final class LeavesBfsIt<T> extends It<T> {

        final Function<T, ? extends Iterable<? extends T>> tree;
        final ArrayDeque<T> queue = new ArrayDeque<T>();

        LeavesBfsIt( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            JBIterable.from(roots).addAllTo(queue);
        }

        @Override
        public T nextImpl() {
            while (!queue.isEmpty()) {
                T result = queue.remove();
                Iterable<? extends T> children = tree.fun(result);
                Iterator<? extends T> it = children == null ? null: children.iterator();
                if (it == null || !it.hasNext()) return result;
                while (it.hasNext()) queue.add(it.next());
            }
            return stop();
        }
    }

    private final static class TracingBfsIt<T> extends TracingIt<T> {

        final Function<T, ? extends Iterable<? extends T>> tree;
        final ArrayDeque<T> queue = new ArrayDeque<T>();
        final Map<T, T> paths = ContainerUtil.newTroveMap(ContainerUtil.<T>identityStrategy());
        T cur;

        TracingBfsIt( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            JBIterable.from(roots).addAllTo(queue);
        }

        @Override
        public T nextImpl() {
            if (queue.isEmpty()) return stop();
            T result = queue.remove();
            for (T t : JBIterable.from(tree.fun(result))) {
                if (paths.containsKey(t)) continue;
                queue.add(t);
                paths.put(t, result);
            }
            return cur = result;
        }

        @Override
        public T parent() {
            return paths.get(cur);
        }

        
        @Override
        public JBIterable<T> backtrace() {
            final T first = cur;
            return new JBIterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new JBIterator<T>() {
                        T cur = first;

                        @Override
                        public T nextImpl() {
                            if (cur == null) return stop();
                            T result = cur;
                            cur = paths.get(cur);
                            return result;
                        }
                    };
                }
            };
        }
    }

    // -----------------------------------------------------------------------------
    // Misc
    // -----------------------------------------------------------------------------
    private static final class GuidedItImpl<T> extends GuidedIt<T> {
        final ArrayDeque<P<T>> stack = new ArrayDeque<P<T>>();
        final Function<T, ? extends Iterable<? extends T>> tree;

        Consumer<GuidedIt<T>> guide;
        T curResult;

        GuidedItImpl( Iterable<? extends T> roots, Function<T, ? extends Iterable<? extends T>> tree) {
            this.tree = tree;
            stack.addLast(P.create(roots));
        }

        public GuidedIt<T> setGuide(Consumer<GuidedIt<T>> guide) {
            this.guide = guide;
            return this;
        }

        public GuidedIt<T> queueNext(T child) {
            if (child != null) stack.addLast(P.create(child));
            return this;
        }

        public GuidedIt<T> queueLast(T child) {
            if (child != null) stack.addFirst(P.create(child));
            return this;
        }

        public GuidedIt<T> result(T node) {
            curResult = node;
            return this;
        }

        @Override
        public T nextImpl() {
            if (guide == null) return stop();
            while (!stack.isEmpty()) {
                P<T> top = stack.getLast();
                Iterator<? extends T> it = top.iterator(tree);
                boolean hasNext = it.hasNext();
                curResult = null;
                if (top.node != null || hasNext) {
                    curChild = hasNext ? it.next() : null;
                    curParent = top.node;
                    curChildren = top.itle;
                    curNoChildren = top.empty;
                    guide.consume(this);
                }
                if (!hasNext) {
                    stack.removeLast();
                }
                if (curResult != null) {
                    return curResult;
                }
            }
            return stop();
        }
    }

    private static class P<T> {
        T node;
        Iterable<? extends T> itle;
        Iterator<? extends T> it;
        boolean empty;

        Iterator<? extends T> iterator( Function<T, ? extends Iterable<? extends T>> tree) {
            if (it != null) return it;
            it = (itle != null ? itle : JBIterable.from(itle = tree.fun(node))).iterator();
            empty = itle == null || !it.hasNext();
            return it;
        }

        static <T> P<T> create(T node) {
            P<T> p = new P<T>();
            p.node = node;
            return p;
        }

        static <T> P<T> create(Iterable<? extends T> it) {
            P<T> p = new P<T>();
            p.itle = it;
            return p;
        }

        static <T> Function<P<T>, T> toNode() {
            //noinspection unchecked
            return TO_NODE;
        }

        static final Function TO_NODE = new Function<P<?>, Object>() {
            @Override
            public Object fun(P<?> tp) {
                return tp.node;
            }
        };
    }
}