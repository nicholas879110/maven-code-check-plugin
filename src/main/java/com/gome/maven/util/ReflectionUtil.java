package com.gome.maven.util;

import com.gome.maven.openapi.util.Condition;
import com.gome.maven.openapi.util.DifferenceFilter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import sun.reflect.ConstructorAccessor;

import java.lang.reflect.*;
import java.util.*;

public class ReflectionUtil {
    private static Log LOG = new SystemStreamLog();

    private ReflectionUtil() {
    }


    public static Type resolveVariable( TypeVariable variable,  Class classType) {
        return resolveVariable(variable, classType, true);
    }


    public static Type resolveVariable( TypeVariable variable,  Class classType, boolean resolveInInterfacesOnly) {
        final Class aClass = getRawType(classType);
        int index = ArrayUtilRt.find(aClass.getTypeParameters(), variable);
        if (index >= 0) {
            return variable;
        }

        final Class[] classes = aClass.getInterfaces();
        final Type[] genericInterfaces = aClass.getGenericInterfaces();
        for (int i = 0; i <= classes.length; i++) {
            Class anInterface;
            if (i < classes.length) {
                anInterface = classes[i];
            }
            else {
                anInterface = aClass.getSuperclass();
                if (resolveInInterfacesOnly || anInterface == null) {
                    continue;
                }
            }
            final Type resolved = resolveVariable(variable, anInterface);
            if (resolved instanceof Class || resolved instanceof ParameterizedType) {
                return resolved;
            }
            if (resolved instanceof TypeVariable) {
                final TypeVariable typeVariable = (TypeVariable)resolved;
                index = ArrayUtilRt.find(anInterface.getTypeParameters(), typeVariable);
                if (index < 0) {
                    LOG.error("Cannot resolve type variable:\n" + "typeVariable = " + typeVariable + "\n" + "genericDeclaration = " +
                            declarationToString(typeVariable.getGenericDeclaration()) + "\n" + "searching in " + declarationToString(anInterface));
                }
                final Type type = i < genericInterfaces.length ? genericInterfaces[i] : aClass.getGenericSuperclass();
                if (type instanceof Class) {
                    return Object.class;
                }
                if (type instanceof ParameterizedType) {
                    return getActualTypeArguments((ParameterizedType)type)[index];
                }
                throw new AssertionError("Invalid type: " + type);
            }
        }
        return null;
    }

    @SuppressWarnings("HardCodedStringLiteral")

    public static String declarationToString( GenericDeclaration anInterface) {
        return anInterface.toString()
                + Arrays.asList(anInterface.getTypeParameters())
                + " loaded by " + ((Class)anInterface).getClassLoader();
    }


    public static Class<?> getRawType( Type type) {
        if (type instanceof Class) {
            return (Class)type;
        }
        if (type instanceof ParameterizedType) {
            return getRawType(((ParameterizedType)type).getRawType());
        }
        if (type instanceof GenericArrayType) {
            //todo[peter] don't create new instance each time
            return Array.newInstance(getRawType(((GenericArrayType)type).getGenericComponentType()), 0).getClass();
        }
        assert false : type;
        return null;
    }


    public static Type[] getActualTypeArguments( ParameterizedType parameterizedType) {
        return parameterizedType.getActualTypeArguments();
    }


    public static Class<?> substituteGenericType( Type genericType,  Type classType) {
        if (genericType instanceof TypeVariable) {
            final Class<?> aClass = getRawType(classType);
            final Type type = resolveVariable((TypeVariable)genericType, aClass);
            if (type instanceof Class) {
                return (Class)type;
            }
            if (type instanceof ParameterizedType) {
                return (Class<?>)((ParameterizedType)type).getRawType();
            }
            if (type instanceof TypeVariable && classType instanceof ParameterizedType) {
                final int index = ArrayUtilRt.find(aClass.getTypeParameters(), type);
                if (index >= 0) {
                    return getRawType(getActualTypeArguments((ParameterizedType)classType)[index]);
                }
            }
        }
        else {
            return getRawType(genericType);
        }
        return null;
    }


    public static List<Field> collectFields( Class clazz) {
        List<Field> result = new ArrayList<Field>();
        collectFields(clazz, result);
        return result;
    }


    public static Field findField( Class clazz,  final Class type,  final String name) throws NoSuchFieldException {
        Field result = processFields(clazz, new Condition<Field>() {
            @Override
            public boolean value(Field field) {
                return name.equals(field.getName()) && (type == null || field.getType().equals(type));
            }
        });
        if (result != null) return result;

        throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
    }


    public static Field findAssignableField( Class<?> clazz, final Class<?> fieldType,  final String fieldName) throws NoSuchFieldException {
        Field result = processFields(clazz, new Condition<Field>() {
            @Override
            public boolean value(Field field) {
                return fieldName.equals(field.getName()) && (fieldType == null || fieldType.isAssignableFrom(field.getType()));
            }
        });
        if (result != null) return result;
        throw new NoSuchFieldException("Class: " + clazz + " fieldName: " + fieldName + " fieldType: " + fieldType);
    }

    private static void collectFields( Class clazz,  List<Field> result) {
        final Field[] fields = clazz.getDeclaredFields();
        result.addAll(Arrays.asList(fields));
        final Class superClass = clazz.getSuperclass();
        if (superClass != null) {
            collectFields(superClass, result);
        }
        final Class[] interfaces = clazz.getInterfaces();
        for (Class each : interfaces) {
            collectFields(each, result);
        }
    }

    private static Field processFields( Class clazz,  Condition<Field> checker) {
        for (Field field : clazz.getDeclaredFields()) {
            if (checker.value(field)) {
                field.setAccessible(true);
                return field;
            }
        }
        final Class superClass = clazz.getSuperclass();
        if (superClass != null) {
            Field result = processFields(superClass, checker);
            if (result != null) return result;
        }
        final Class[] interfaces = clazz.getInterfaces();
        for (Class each : interfaces) {
            Field result = processFields(each, checker);
            if (result != null) return result;
        }
        return null;
    }

    public static void resetField( Class clazz,  Class type,  String name)  {
        try {
            resetField(null, findField(clazz, type, name));
        }
        catch (NoSuchFieldException e) {
            LOG.info(e);
        }
    }
    public static void resetField( Object object,  Class type,  String name)  {
        try {
            resetField(object, findField(object.getClass(), type, name));
        }
        catch (NoSuchFieldException e) {
            LOG.info(e);
        }
    }

    public static void resetField( Object object,  String name) {
        try {
            resetField(object, findField(object.getClass(), null, name));
        }
        catch (NoSuchFieldException e) {
            LOG.info(e);
        }
    }

    public static void resetField( final Object object,  Field field) {
        field.setAccessible(true);
        Class<?> type = field.getType();
        try {
            if (type.isPrimitive()) {
                if (boolean.class.equals(type)) {
                    field.set(object, Boolean.FALSE);
                }
                else if (int.class.equals(type)) {
                    field.set(object, Integer.valueOf(0));
                }
                else if (double.class.equals(type)) {
                    field.set(object, Double.valueOf(0));
                }
                else if (float.class.equals(type)) {
                    field.set(object, Float.valueOf(0));
                }
            }
            else {
                field.set(object, null);
            }
        }
        catch (IllegalAccessException e) {
            LOG.info(e);
        }
    }


    public static Method findMethod( Collection<Method> methods,   String name,  Class... parameters) {
        for (final Method method : methods) {
            if (name.equals(method.getName()) && Arrays.equals(parameters, method.getParameterTypes())) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }


    public static Method getMethod( Class aClass,   String name,  Class... parameters) {
        return findMethod(getClassPublicMethods(aClass, false), name, parameters);
    }


    public static Method getDeclaredMethod( Class aClass,   String name,  Class... parameters) {
        return findMethod(getClassDeclaredMethods(aClass, false), name, parameters);
    }


    public static Field getDeclaredField( Class aClass,   final String name) {
        return processFields(aClass, new Condition<Field>() {
            @Override
            public boolean value(Field field) {
                return name.equals(field.getName());
            }
        });
    }


    public static List<Method> getClassPublicMethods( Class aClass) {
        return getClassPublicMethods(aClass, false);
    }


    public static List<Method> getClassPublicMethods( Class aClass, boolean includeSynthetic) {
        Method[] methods = aClass.getMethods();
        return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
    }


    public static List<Method> getClassDeclaredMethods( Class aClass) {
        return getClassDeclaredMethods(aClass, false);
    }


    public static List<Method> getClassDeclaredMethods( Class aClass, boolean includeSynthetic) {
        Method[] methods = aClass.getDeclaredMethods();
        return includeSynthetic ? Arrays.asList(methods) : filterRealMethods(methods);
    }


    public static List<Field> getClassDeclaredFields( Class aClass) {
        Field[] fields = aClass.getDeclaredFields();
        return Arrays.asList(fields);
    }


    private static List<Method> filterRealMethods( Method[] methods) {
        List<Method> result = new ArrayList<Method>();
        for (Method method : methods) {
            if (!method.isSynthetic()) {
                result.add(method);
            }
        }
        return result;
    }


    public static Class getMethodDeclaringClass( Class<?> instanceClass,   String methodName,  Class... parameters) {
        Method method = getMethod(instanceClass, methodName, parameters);
        return method == null ? null : method.getDeclaringClass();
    }

    public static <T> T getField( Class objectClass, Object object, Class<T> fieldType,   String fieldName) {
        try {
            final Field field = findAssignableField(objectClass, fieldType, fieldName);
            return (T)field.get(object);
        }
        catch (NoSuchFieldException e) {
            LOG.debug(e);
            return null;
        }
        catch (IllegalAccessException e) {
            LOG.debug(e);
            return null;
        }
    }

    // returns true if value was set
    public static <T> boolean setField( Class objectClass, Object object, Class<T> fieldType,   String fieldName, T value) {
        try {
            final Field field = findAssignableField(objectClass, fieldType, fieldName);
            field.set(object, value);
            return true;
        }
        catch (NoSuchFieldException e) {
            LOG.debug(e);
            // this 'return' was moved into 'catch' block because otherwise reference to common super-class of these exceptions (ReflectiveOperationException)
            // which doesn't exist in JDK 1.6 will be added to class-file during instrumentation
            return false;
        }
        catch (IllegalAccessException e) {
            LOG.debug(e);
            return false;
        }
    }

    public static Type resolveVariableInHierarchy( TypeVariable variable,  Class aClass) {
        Type type;
        Class current = aClass;
        while ((type = resolveVariable(variable, current, false)) == null) {
            current = current.getSuperclass();
            if (current == null) {
                return null;
            }
        }
        if (type instanceof TypeVariable) {
            return resolveVariableInHierarchy((TypeVariable)type, aClass);
        }
        return type;
    }


    public static <T> Constructor<T> getDefaultConstructor( Class<T> aClass) {
        try {
            final Constructor<T> constructor = aClass.getConstructor();
            constructor.setAccessible(true);
            return constructor;
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("No default constructor in " + aClass, e);
        }
    }


    private static final Method acquireConstructorAccessorMethod = getDeclaredMethod(Constructor.class, "acquireConstructorAccessor");
    private static final Method getConstructorAccessorMethod = getDeclaredMethod(Constructor.class, "getConstructorAccessor");


    public static ConstructorAccessor getConstructorAccessor( Constructor constructor) {
        constructor.setAccessible(true);
        // it is faster to invoke constructor via sun.reflect.ConstructorAccessor; it avoids AccessibleObject.checkAccess()
        try {
            acquireConstructorAccessorMethod.invoke(constructor);
            return (ConstructorAccessor)getConstructorAccessorMethod.invoke(constructor);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static <T> T createInstanceViaConstructorAccessor( ConstructorAccessor constructorAccessor,
                                                              Object... arguments) {
        try {
            return (T)constructorAccessor.newInstance(arguments);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T createInstanceViaConstructorAccessor( ConstructorAccessor constructorAccessor) {
        try {
            return (T)constructorAccessor.newInstance(new Object[]{});
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@link Class#newInstance()} cannot instantiate private classes
     */

    public static <T> T newInstance( Class<T> aClass,  Class... parameterTypes) {
        try {
            Constructor<T> constructor = aClass.getDeclaredConstructor(parameterTypes);
            try {
                constructor.setAccessible(true);
            }
            catch (SecurityException e) {
                return aClass.newInstance();
            }
            return constructor.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static <T> T createInstance( Constructor<T> constructor,  Object... args) {
        try {
            return constructor.newInstance(args);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void resetThreadLocals() {
        resetField(Thread.currentThread(), null, "threadLocals");
    }


    public static Class getGrandCallerClass() {
        int stackFrameCount = 3;
        Class callerClass = findCallerClass(stackFrameCount);
        while (callerClass != null && callerClass.getClassLoader() == null) { // looks like a system class
            callerClass = findCallerClass(++stackFrameCount);
        }
        if (callerClass == null) {
            callerClass = findCallerClass(2);
        }
        return callerClass;
    }

    public static void copyFields( Field[] fields,  Object from,  Object to) {
        copyFields(fields, from, to, null);
    }

    public static boolean copyFields( Field[] fields,  Object from,  Object to,  DifferenceFilter diffFilter) {
        Set<Field> sourceFields = new HashSet<Field>(Arrays.asList(from.getClass().getFields()));
        boolean valuesChanged = false;
        for (Field field : fields) {
            if (sourceFields.contains(field)) {
                if (isPublic(field) && !isFinal(field)) {
                    try {
                        if (diffFilter == null || diffFilter.isAccept(field)) {
                            copyFieldValue(from, to, field);
                            valuesChanged = true;
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return valuesChanged;
    }

    public static void copyFieldValue( Object from,  Object to,  Field field)
            throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        if (fieldType.isPrimitive() || fieldType.equals(String.class)) {
            field.set(to, field.get(from));
        }
        else {
            throw new RuntimeException("Field '" + field.getName()+"' not copied: unsupported type: "+field.getType());
        }
    }

    private static boolean isPublic(final Field field) {
        return (field.getModifiers() & Modifier.PUBLIC) != 0;
    }

    private static boolean isFinal(final Field field) {
        return (field.getModifiers() & Modifier.FINAL) != 0;
    }


    private static class MySecurityManager extends SecurityManager {
        private static final MySecurityManager INSTANCE = new MySecurityManager();
        public Class[] getStack() {
            return getClassContext();
        }
    }

    /**
     * Returns the class this method was called 'framesToSkip' frames up the caller hierarchy.
     *
     * NOTE:
     * <b>Extremely expensive!
     * Please consider not using it.
     * These aren't the droids you're looking for!</b>
     */

    public static Class findCallerClass(int framesToSkip) {
        try {
            Class[] stack = MySecurityManager.INSTANCE.getStack();
            int indexFromTop = 1 + framesToSkip;
            return stack.length > indexFromTop ? stack[indexFromTop] : null;
        }
        catch (Exception e) {
            LOG.warn(e);
            return null;
        }
    }

    public static boolean isAssignable( Class<?> ancestor,  Class<?> descendant) {
        return ancestor == descendant || ancestor.isAssignableFrom(descendant);
    }
}
