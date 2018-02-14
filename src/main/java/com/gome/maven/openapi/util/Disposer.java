package com.gome.maven.openapi.util;

import com.gome.maven.openapi.Disposable;
import com.gome.maven.openapi.util.objectTree.ObjectTree;
import com.gome.maven.openapi.util.objectTree.ObjectTreeAction;
import com.gome.maven.util.ReflectionUtil;
import com.gome.maven.util.containers.ContainerUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * @author zhangliewei
 * @date 2018/1/2 10:06
 * @opyright(c) gome inc Gome Co.,LTD
 */
public class Disposer {
    private static final ObjectTree<Disposable> ourTree;

    static {
        try {
            ourTree = new ObjectTree<Disposable>();
        }
        catch (NoClassDefFoundError e) {
            throw new RuntimeException("loader=" + Disposer.class.getClassLoader(), e);
        }
    }

    private static final ObjectTreeAction<Disposable> ourDisposeAction = new ObjectTreeAction<Disposable>() {
        @Override
        public void execute( final Disposable each) {
            each.dispose();
        }

        @Override
        public void beforeTreeExecution( final Disposable parent) {
            if (parent instanceof Disposable.Parent) {
                ((Disposable.Parent)parent).beforeTreeDispose();
            }
        }
    };

    private static boolean ourDebugMode;

    private Disposer() {
    }

    
    public static Disposable newDisposable() {
        return new Disposable() {
            @Override
            public void dispose() {
            }
        };
    }

    private static final Map<String, Disposable> ourKeyDisposables = ContainerUtil.createConcurrentWeakMap();

    public static void register( Disposable parent,  Disposable child) {
        register(parent, child, null);
    }

    public static void register( Disposable parent,  Disposable child, final String key) {
        assert parent != child : " Cannot register to itself";

        ourTree.register(parent, child);

        if (key != null) {
            assert get(key) == null;
            ourKeyDisposables.put(key, child);
            register(child, new Disposable() {
                @Override
                public void dispose() {
                    ourKeyDisposables.remove(key);
                }
            });
        }
    }

    public static boolean isDisposed( Disposable disposable) {
        return !ourTree.containsKey(disposable);
    }

    public static Disposable get( String key) {
        return ourKeyDisposables.get(key);
    }

    public static void dispose( Disposable disposable) {
        dispose(disposable, true);
    }

    public static void dispose( Disposable disposable, boolean processUnregistered) {
        ourTree.executeAll(disposable, true, ourDisposeAction, processUnregistered);
    }

    public static void disposeChildAndReplace( Disposable toDispose,  Disposable toReplace) {
        ourTree.executeChildAndReplace(toDispose, toReplace, true, ourDisposeAction);
    }

    
    public static ObjectTree<Disposable> getTree() {
        return ourTree;
    }

    public static void assertIsEmpty() {
        assertIsEmpty(false);
    }
    public static void assertIsEmpty(boolean throwError) {
        if (ourDebugMode) {
            ourTree.assertIsEmpty(throwError);
        }
    }

    public static boolean isEmpty() {
        return ourDebugMode && ourTree.isEmpty();
    }

    public static void setDebugMode(final boolean b) {
        ourDebugMode = b;
    }

    public static boolean isDebugMode() {
        return ourDebugMode;
    }

    public static void clearOwnFields( Object object) {
        final Field[] all = object.getClass().getDeclaredFields();
        for (Field each : all) {
            if ((each.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) > 0) continue;
            ReflectionUtil.resetField(object, each);
        }
    }

    /**
     * @return object registered on parentDisposable which is equal to object, or null if not found
     */
    public static <T extends Disposable> T findRegisteredObject( Disposable parentDisposable,  T object) {
        return ourTree.findRegisteredObject(parentDisposable, object);
    }
}
