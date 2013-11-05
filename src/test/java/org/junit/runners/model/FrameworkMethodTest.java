package org.junit.runners.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.junit.rules.ExpectedException.none;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FrameworkMethodTest {
    @Rule
    public final ExpectedException thrown = none();

    @Test
    public void cannotBeCreatedWithoutUnderlyingField() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("FrameworkMethod cannot be created without an underlying method.");
        new FrameworkMethod(null);
    }

    @Test
    public void hasToStringWhichPrintsMethodName() throws Exception {
        Method method = ClassWithDummyMethod.class.getMethod("dummyMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        assertTrue(frameworkMethod.toString().contains("dummyMethod"));
    }

    @Test
    public void presentAnnotationIsAvailable() throws Exception {
        Method method = ClassWithDummyMethod.class.getMethod("annotatedDummyMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Annotation annotation = frameworkMethod.getAnnotation(Rule.class);
        assertTrue(Rule.class.isAssignableFrom(annotation.getClass()));
    }

    @Test
    public void missingAnnotationIsNotAvailable() throws Exception {
        Method method = ClassWithDummyMethod.class.getMethod("annotatedDummyMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Annotation annotation = frameworkMethod.getAnnotation(ClassRule.class);
        assertThat(annotation, is(nullValue()));
    }

    private static class ClassWithDummyMethod {
        @SuppressWarnings("unused")
        public void dummyMethod() {
        }

        @Rule
        public void annotatedDummyMethod() {
        }
    }

    private static class ClassWithTest {
        @Test
        public void theTestMethod() {
        }
    }

    @Test
    public void providesAnnotation() throws Exception {
        Method method = ClassWithTest.class.getMethod("theTestMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Annotation[] annotations = frameworkMethod.getAnnotations();
        assertEquals("Wrong number of annotations.", 1, annotations.length);
        assertEquals("Wrong annotation type.", Test.class,
                annotations[0].annotationType());
    }

    @Test
    public void providesDistinctArrayOfAnnotations() throws Exception {
        Method method = ClassWithTest.class.getMethod("theTestMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Annotation[] annotationsOfFirstCall = frameworkMethod.getAnnotations();
        annotationsOfFirstCall[0] = null;
        Annotation[] annotationsOfSecondCall = frameworkMethod.getAnnotations();
        assertNotNull("Annotation has been changed.",
                annotationsOfSecondCall[0]);
    }

    private static class ChildClassWithOveriddenAndIgnoredTest extends
            ClassWithTest {
        @Ignore
        @Override
        public void theTestMethod() {
        }
    }

    @Test
    public void providesAnnotationFromSuperClass() throws Exception {
        Method method = ChildClassWithOveriddenAndIgnoredTest.class
                .getMethod("theTestMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Annotation[] annotations = frameworkMethod.getAnnotations();
        // @Test and @Ignore, because @Override is not available at runtime.
        assertEquals("Wrong number of annotations.", 2, annotations.length);
    }

    private static class ChildClassWithDifferentTestAnnotation extends
            ClassWithTest {
        @Override
        @Test(expected = NullPointerException.class)
        public void theTestMethod() {
        }
    }

    @Test
    public void masksAnnotationFromSuperClassWithAnnotationInChildClass()
            throws Exception {
        Method method = ChildClassWithDifferentTestAnnotation.class
                .getMethod("theTestMethod");
        FrameworkMethod frameworkMethod = new FrameworkMethod(method);
        Annotation[] annotations = frameworkMethod.getAnnotations();
        // @Test and @Ignore, because @Override is not available at runtime.
        assertEquals("Wrong number of annotations.", 1, annotations.length);
        assertEquals("Wrong number of annotations.",
                ((Test) annotations[0]).expected(), NullPointerException.class);
    }
}
