package org.junit.runners.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.internal.runners.model.ReflectiveCallable;

/**
 * Represents a method on a test class to be invoked at the appropriate point in
 * test execution. These methods are usually marked with an annotation (such as
 * {@code @Test}, {@code @Before}, {@code @After}, {@code @BeforeClass},
 * {@code @AfterClass}, etc.)
 *
 * @since 4.5
 */
public class FrameworkMethod extends FrameworkMember<FrameworkMethod> {
    private static final List<Annotation> NO_ANNOTATIONS = emptyList();
    private final Method method;
    private final List<Annotation> annotations;

    /**
     * Returns a new {@code FrameworkMethod} for {@code method}
     */
    public FrameworkMethod(Method method) {
        if (method == null) {
            throw new NullPointerException(
                    "FrameworkMethod cannot be created without an underlying method.");
        }
        this.method = method;
        this.annotations = collectAnnotationsForMethod(method);
    }

    /**
     * Returns the underlying Java method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns the result of invoking this method on {@code target} with
     * parameters {@code params}. {@link InvocationTargetException}s thrown are
     * unwrapped, and their causes rethrown.
     */
    public Object invokeExplosively(final Object target, final Object... params)
            throws Throwable {
        return new ReflectiveCallable() {
            @Override
            protected Object runReflectiveCall() throws Throwable {
                return method.invoke(target, params);
            }
        }.run();
    }

    /**
     * Returns the method's name
     */
    @Override
    public String getName() {
        return method.getName();
    }

    /**
     * Adds to {@code errors} if this method:
     * <ul>
     * <li>is not public, or
     * <li>takes parameters, or
     * <li>returns something other than void, or
     * <li>is static (given {@code isStatic is false}), or
     * <li>is not static (given {@code isStatic is true}).
     * </ul>
     */
    public void validatePublicVoidNoArg(boolean isStatic, List<Throwable> errors) {
        validatePublicVoid(isStatic, errors);
        if (method.getParameterTypes().length != 0) {
            errors.add(new Exception("Method " + method.getName() + " should have no parameters"));
        }
    }


    /**
     * Adds to {@code errors} if this method:
     * <ul>
     * <li>is not public, or
     * <li>returns something other than void, or
     * <li>is static (given {@code isStatic is false}), or
     * <li>is not static (given {@code isStatic is true}).
     * </ul>
     */
    public void validatePublicVoid(boolean isStatic, List<Throwable> errors) {
        if (isStatic() != isStatic) {
            String state = isStatic ? "should" : "should not";
            errors.add(new Exception("Method " + method.getName() + "() " + state + " be static"));
        }
        if (!isPublic()) {
            errors.add(new Exception("Method " + method.getName() + "() should be public"));
        }
        if (method.getReturnType() != Void.TYPE) {
            errors.add(new Exception("Method " + method.getName() + "() should be void"));
        }
    }

    @Override
    protected int getModifiers() {
        return method.getModifiers();
    }

    /**
     * Returns the return type of the method
     */
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    /**
     * Returns the return type of the method
     */
    @Override
    public Class<?> getType() {
        return getReturnType();
    }

    /**
     * Returns the class where the method is actually declared
     */
    @Override
    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    public void validateNoTypeParametersOnArgs(List<Throwable> errors) {
        new NoGenericTypeParametersValidator(method).validate(errors);
    }

    @Override
    public boolean isShadowedBy(FrameworkMethod other) {
        if (!other.getName().equals(getName())) {
            return false;
        }
        if (other.getParameterTypes().length != getParameterTypes().length) {
            return false;
        }
        for (int i = 0; i < other.getParameterTypes().length; i++) {
            if (!other.getParameterTypes()[i].equals(getParameterTypes()[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (!FrameworkMethod.class.isInstance(obj)) {
            return false;
        }
        return ((FrameworkMethod) obj).method.equals(method);
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }

    /**
     * Returns true if this is a no-arg method that returns a value assignable
     * to {@code type}
     *
     * @deprecated This is used only by the Theories runner, and does not
     *             use all the generic type info that it ought to. It will be replaced
     *             with a forthcoming ParameterSignature#canAcceptResultOf(FrameworkMethod)
     *             once Theories moves to junit-contrib.
     */
    @Deprecated
    public boolean producesType(Type type) {
        return getParameterTypes().length == 0 && type instanceof Class<?>
                && ((Class<?>) type).isAssignableFrom(method.getReturnType());
    }

    private Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    /**
     * Returns the annotations on this method
     */
    public Annotation[] getAnnotations() {
        return annotations.toArray(new Annotation[annotations.size()]);
    }

    /**
     * Returns the annotation of type {@code annotationType} on this method, if
     * one exists.
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return method.getAnnotation(annotationType);
    }

    @Override
    public String toString() {
        return method.toString();
    }

    private List<Annotation> collectAnnotationsForMethod(Method method) {
        List<Annotation> annotations = new ArrayList<Annotation>();
        for (Class<?> klass = method.getDeclaringClass(); klass != null; klass = klass
                .getSuperclass()) {
            addAnnotations(annotations,
                    getAnnotationForMethodTemplateInClass(method, klass));
        }
        return annotations;
    }

    private List<Annotation> getAnnotationForMethodTemplateInClass(
            Method methodTemplate, Class<?> klass) {
        try {
            Method method = klass.getDeclaredMethod(methodTemplate.getName(),
                    methodTemplate.getParameterTypes());
            if (!Modifier.isPrivate(method.getModifiers())) {
                return asList(method.getAnnotations());
            } else {
                return NO_ANNOTATIONS;
            }
        } catch (NoSuchMethodException e) {
            return NO_ANNOTATIONS;
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void addAnnotations(List<Annotation> currentAnnotations,
            List<Annotation> additionalAnnotations) {
        for (Annotation additionalAnnotation : additionalAnnotations) {
            if (!containsAnnotationWithType(currentAnnotations,
                    additionalAnnotation.annotationType())) {
                currentAnnotations.add(additionalAnnotation);
            }
        }
    }

    private boolean containsAnnotationWithType(List<Annotation> annotations,
            Class<? extends Annotation> type) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
