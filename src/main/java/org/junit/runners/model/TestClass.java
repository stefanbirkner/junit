package org.junit.runners.model;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.internal.MethodSorter;

/**
 * Wraps a class to be run, providing method validation and annotation searching
 * 
 * @since 4.5
 */
public class TestClass {
    private final Class<?> fClass;
    private final Members<FrameworkField> fFields = new Members<FrameworkField>();
    private final Members<FrameworkMethod> fMethods = new Members<FrameworkMethod>();

    /**
     * Creates a {@code TestClass} wrapping {@code klass}. Each time this
     * constructor executes, the class is scanned for annotations, which can be
     * an expensive process (we hope in future JDK's it will not be.) Therefore,
     * try to share instances of {@code TestClass} where possible.
     */
    public TestClass(Class<?> klass) {
        fClass = klass;
        if (klass != null && klass.getConstructors().length > 1) {
            throw new IllegalArgumentException(
                    "Test class can only have one constructor");
        }

        for (Class<?> eachClass : getSuperClasses(fClass)) {
            for (Method eachMethod : MethodSorter.getDeclaredMethods(eachClass)) {
                fMethods.addMemberIfNotShadowed(new FrameworkMethod(eachMethod));
            }
            // ensuring fields are sorted to make sure that entries are inserted
            // and read from fieldForAnnotations in a deterministic order
            for (Field eachField : getSortedDeclaredFields(eachClass)) {
                fFields.addMemberIfNotShadowed(new FrameworkField(eachField));
            }
        }
    }

    private static Field[] getSortedDeclaredFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Arrays.sort(declaredFields, new Comparator<Field>() {
            public int compare(Field field1, Field field2) {
                return field1.getName().compareTo(field2.getName());
            }
        });
        return declaredFields;
    }

    /**
     * Gets all methods that have an annotation in this class or its
     * superclasses.
     * 
     * @since 4.12
     */
    public List<FrameworkMethod> getAnnotatedMethods() {
        return fMethods.getAnnotatedMembers();
    }

    /**
     * Returns, efficiently, all the non-overridden methods in this class and
     * its superclasses that are annotated with {@code annotationClass}.
     */
    public List<FrameworkMethod> getAnnotatedMethods(
            Class<? extends Annotation> annotationClass) {
        return fMethods.getMembersWithAnnotation(annotationClass);
    }

    /**
     * Gets all fields that have an annotation in this class or its
     * superclasses.
     * 
     * @since 4.12
     */
    public List<FrameworkField> getAnnotatedFields() {
        return fFields.getAnnotatedMembers();
    }

    /**
     * Returns, efficiently, all the non-overridden fields in this class and its
     * superclasses that are annotated with {@code annotationClass}.
     */
    public List<FrameworkField> getAnnotatedFields(
            Class<? extends Annotation> annotationClass) {
        return fFields.getMembersWithAnnotation(annotationClass);
    }

    private static List<Class<?>> getSuperClasses(Class<?> testClass) {
        ArrayList<Class<?>> results = new ArrayList<Class<?>>();
        Class<?> current = testClass;
        while (current != null) {
            results.add(current);
            current = current.getSuperclass();
        }
        return results;
    }

    /**
     * Returns the underlying Java class.
     */
    public Class<?> getJavaClass() {
        return fClass;
    }

    /**
     * Returns the class's name.
     */
    public String getName() {
        if (fClass == null) {
            return "null";
        }
        return fClass.getName();
    }

    /**
     * Returns the only public constructor in the class, or throws an
     * {@code AssertionError} if there are more or less than one.
     */

    public Constructor<?> getOnlyConstructor() {
        Constructor<?>[] constructors = fClass.getConstructors();
        Assert.assertEquals(1, constructors.length);
        return constructors[0];
    }

    /**
     * Returns the annotations on this class
     */
    public Annotation[] getAnnotations() {
        if (fClass == null) {
            return new Annotation[0];
        }
        return fClass.getAnnotations();
    }

    public <T> List<T> getAnnotatedFieldValues(Object test,
            Class<? extends Annotation> annotationClass, Class<T> valueClass) {
        List<T> results = new ArrayList<T>();
        for (FrameworkField each : getAnnotatedFields(annotationClass)) {
            try {
                Object fieldValue = each.get(test);
                if (valueClass.isInstance(fieldValue)) {
                    results.add(valueClass.cast(fieldValue));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                        "How did getFields return a field we couldn't access?",
                        e);
            }
        }
        return results;
    }

    public <T> List<T> getAnnotatedMethodValues(Object test,
            Class<? extends Annotation> annotationClass, Class<T> valueClass) {
        List<T> results = new ArrayList<T>();
        for (FrameworkMethod each : getAnnotatedMethods(annotationClass)) {
            try {
                Object fieldValue = each.invokeExplosively(test);
                if (valueClass.isInstance(fieldValue)) {
                    results.add(valueClass.cast(fieldValue));
                }
            } catch (Throwable e) {
                throw new RuntimeException("Exception in " + each.getName(), e);
            }
        }
        return results;
    }

    public boolean isANonStaticInnerClass() {
        return fClass.isMemberClass() && !isStatic(fClass.getModifiers());
    }

    /**
     * Controls access to fields or methods.
     */
    private static class Members<T extends FrameworkMember<T>> {
        @SuppressWarnings("unchecked")
        private static final List<Class<? extends Annotation>> ANNOTATION_TYPES_WITH_REVERSE_ORDER = asList(
                Before.class, BeforeClass.class);
        private final List<T> fAnnotatedMembers = new ArrayList<T>();
        private final Map<Class<?>, List<T>> fMembersWithAnnotation = new HashMap<Class<?>, List<T>>();

        public void addMemberIfNotShadowed(T member) {
            if (member.getAnnotations().length != 0) {
                addMember(member);
            }
        }

        private void addMember(T member) {
            if (!member.isShadowedBy(fAnnotatedMembers)) {
                fAnnotatedMembers.add(member);
            }
            for (Annotation annotation : member.getAnnotations()) {
                addMemberToListOfAnnotationType(member,
                        annotation.annotationType());
            }
        }

        private void addMemberToListOfAnnotationType(T member,
                Class<?> annotationType) {
            ensureListOfMembersForAnnotationTypeExists(annotationType);
            List<T> members = fMembersWithAnnotation.get(annotationType);
            if (!member.isShadowedBy(members)) {
                if (ANNOTATION_TYPES_WITH_REVERSE_ORDER
                        .contains(annotationType)) {
                    members.add(0, member);
                } else {
                    members.add(member);
                }
            }
        }

        private void ensureListOfMembersForAnnotationTypeExists(Class<?> type) {
            if (!fMembersWithAnnotation.containsKey(type)) {
                fMembersWithAnnotation.put(type, new ArrayList<T>());
            }
        }

        public List<T> getAnnotatedMembers() {
            return unmodifiableList(fAnnotatedMembers);
        }

        public List<T> getMembersWithAnnotation(Class<?> annotationType) {
            List<T> members = fMembersWithAnnotation.get(annotationType);
            if (members == null) {
                return emptyList();
            } else {
                return unmodifiableList(members);
            }
        }
    }
}
