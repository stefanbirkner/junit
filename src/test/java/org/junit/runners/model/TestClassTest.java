package org.junit.runners.model;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.runners.Parameterized.Parameter;

public class TestClassTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    public static class TwoConstructors {
        public TwoConstructors() {
        }

        public TwoConstructors(int x) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void complainIfMultipleConstructors() {
        new TestClass(TwoConstructors.class);
    }
    
    public static class SuperclassWithField {
        @Rule
        public TestRule x;
    }

    public static class SubclassWithField extends SuperclassWithField {
        @Rule
        public TestRule x;
    }

    @Test
    public void fieldsOnSubclassesShadowSuperclasses() {
        assertThat(new TestClass(SubclassWithField.class).getAnnotatedFields(
                Rule.class).size(), is(1));
    }

    public static class OuterClass {
        public class NonStaticInnerClass {
        }
    }

    @Test
    public void identifyNonStaticInnerClass() {
        assertThat(
                new TestClass(OuterClass.NonStaticInnerClass.class)
                        .isANonStaticInnerClass(),
                is(true));
    }

    public static class OuterClass2 {
        public static class StaticInnerClass {
        }
    }

    @Test
    public void dontMarkStaticInnerClassAsNonStatic() {
        assertThat(
                new TestClass(OuterClass2.StaticInnerClass.class)
                        .isANonStaticInnerClass(),
                is(false));
    }

    public static class SimpleClass {
    }

    @Test
    public void dontMarkNonInnerClassAsInnerClass() {
        assertThat(new TestClass(SimpleClass.class).isANonStaticInnerClass(),
                is(false));
    }
        
    public static class FieldAnnotated {
    	@Rule
    	public String fieldThatShouldBeMatched = "andromeda";
    	
    	@Rule
    	public boolean fieldThatShouldNotBeMachted;
    }
    
    @Test
    public void annotatedFieldValues() {
    	TestClass tc = new TestClass(FieldAnnotated.class);
    	List<String> values = tc.getAnnotatedFieldValues(new FieldAnnotated(), Rule.class, String.class);
    	assertThat(values, hasItem("andromeda"));
    	assertThat(values.size(), is(1));
    }
    
    public static class ClassWithTwoTestsAndAnAnUnnotatedMethod {
    	@Test
    	public String methodToBeMatched() { 
    		return "jupiter";
    	}

        @Test
        public int methodOfWrongType() {
            return 0;
        }

        public int methodWithoutAnnotation() {
            return 0;
        }
    }
    
    @Test
    public void annotatedMethodValues() {
        TestClass tc = new TestClass(
                ClassWithTwoTestsAndAnAnUnnotatedMethod.class);
        List<String> values = tc.getAnnotatedMethodValues(
                new ClassWithTwoTestsAndAnAnUnnotatedMethod(), Test.class,
                String.class);
    	assertThat(values, hasItem("jupiter"));
    	assertThat(values.size(), is(1));
    }

    @Test
    public void hasListOfAllAnnotatedMethods() {
        TestClass tc = new TestClass(
                ClassWithTwoTestsAndAnAnUnnotatedMethod.class);
        List<FrameworkMethod> methods = tc.getAnnotatedMethods();
        assertThat(methods.size(), is(2));
    }

    @Test
    public void hasUnmodifiableListOfMethods() {
        TestClass tc = new TestClass(
                ClassWithTwoTestsAndAnAnUnnotatedMethod.class);
        List<FrameworkMethod> methods = tc.getAnnotatedMethods();
        assertListIsUnmodifiable(methods);
    }

    @Test
    public void hasListOfAllMethodsForSpecificAnnotation() {
        TestClass tc = new TestClass(
                ClassWithTwoTestsAndAnAnUnnotatedMethod.class);
        List<FrameworkMethod> methods = tc.getAnnotatedMethods();
        assertThat(methods.size(), is(2));
    }

    @Test
    public void hasUnmodifiableListOfMethodsForSpecificAnnotation() {
        TestClass tc = new TestClass(
                ClassWithTwoTestsAndAnAnUnnotatedMethod.class);
        List<FrameworkMethod> methods = tc.getAnnotatedMethods(Test.class);
        assertListIsUnmodifiable(methods);
    }
    
    private static class ClassWithTwoRuleAndOneParameterField {
        @Rule
        public String firstFieldWithRuleAnnotation;

        @Rule
        public boolean secondFieldWithRuleAnnotation;

        @Parameter
        public boolean fieldWithParameterAnnotation;
    }

    @Test
    public void hasListOfAllAnnotatedFields() {
        TestClass tc = new TestClass(ClassWithTwoRuleAndOneParameterField.class);
        List<FrameworkField> fields = tc.getAnnotatedFields();
        assertThat(fields.size(), is(3));
    }

    @Test
    public void hasUnmodifiableListOfFields() {
        TestClass tc = new TestClass(ClassWithTwoRuleAndOneParameterField.class);
        List<FrameworkField> fields = tc.getAnnotatedFields();
        assertListIsUnmodifiable(fields);
    }

    @Test
    public void hasListOfAllFieldsForSpecificAnnotation() {
        TestClass tc = new TestClass(ClassWithTwoRuleAndOneParameterField.class);
        List<FrameworkField> fields = tc.getAnnotatedFields(Rule.class);
        assertThat(fields.size(), is(2));
    }

    @Test
    public void hasUnmodifiableListOfFieldsForSpecificAnnotation() {
        TestClass tc = new TestClass(ClassWithTwoRuleAndOneParameterField.class);
        List<FrameworkField> fields = tc.getAnnotatedFields(Rule.class);
        assertListIsUnmodifiable(fields);
    }

    public static class MultipleFieldsAnnotated {
        @DataPoint
        public String a = "testing a";

        @Rule
        public boolean b;

        @DataPoint
        public String c = "testing c";

        @Rule
        public boolean d;
    }

    @Test
    public void annotationToFieldsReturnsFieldsInADeterministicOrder() {
        TestClass tc = new TestClass(MultipleFieldsAnnotated.class);
        List<FrameworkField> annotatedFields = tc.getAnnotatedFields();
        assertThat(annotatedFields.get(0).getName(), CoreMatchers.equalTo("a"));
        assertThat(annotatedFields.get(1).getName(), CoreMatchers.equalTo("b"));
    }

    private <T> void assertListIsUnmodifiable(List<T> list) {
        exception.expect(UnsupportedOperationException.class);
        T dummyElement = list.get(0);
        list.add(dummyElement);
    }
}
