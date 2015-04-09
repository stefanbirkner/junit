package org.junit.runners.parameterized;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.junit.validator.SinglePublicConstructorValidator;
import org.junit.validator.TestClassValidator;

/**
 * A {@link BlockJUnit4ClassRunner} with parameters support. Parameters can be
 * injected via constructor or into annotated fields.
 */
public class BlockJUnit4ClassRunnerWithParameters extends
        BlockJUnit4ClassRunner {
    private final Object[] parameters;

    private final String name;

    public BlockJUnit4ClassRunnerWithParameters(TestWithParameters test)
            throws InitializationError {
        super(test.getTestClass().getJavaClass(),
                createAdditionalValidators(test));
        parameters = test.getParameters().toArray(
                new Object[test.getParameters().size()]);
        name = test.getName();
    }

    private static List<TestClassValidator> createAdditionalValidators(
            TestWithParameters test) {
        return asList(new FieldsValidator(test.getParameters()),
                new SinglePublicConstructorValidator());
    }

    @Override
    public Object createTest() throws Exception {
        if (fieldsAreAnnotated()) {
            return createTestUsingFieldInjection();
        } else {
            return createTestUsingConstructorInjection();
        }
    }

    private Object createTestUsingConstructorInjection() throws Exception {
        return getTestClass().getOnlyConstructor().newInstance(parameters);
    }

    private Object createTestUsingFieldInjection() throws Exception {
        Object testClassInstance = getTestClass().getJavaClass().newInstance();
        for (FrameworkField each : getAnnotatedFieldsByParameter()) {
            Field field = each.getField();
            Parameter annotation = field.getAnnotation(Parameter.class);
            int index = annotation.value();
            try {
                field.set(testClassInstance, parameters[index]);
            } catch (IllegalArgumentException iare) {
                throw new Exception(getTestClass().getName()
                        + ": Trying to set " + field.getName()
                        + " with the value " + parameters[index]
                        + " that is not the right type ("
                        + parameters[index].getClass().getSimpleName()
                        + " instead of " + field.getType().getSimpleName()
                        + ").", iare);
            }
        }
        return testClassInstance;
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected String testName(FrameworkMethod method) {
        return method.getName() + getName();
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        if (fieldsAreAnnotated()) {
            validateZeroArgConstructor(errors);
        }
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        return childrenInvoker(notifier);
    }

    @Override
    protected Annotation[] getRunnerAnnotations() {
        return new Annotation[0];
    }

    private List<FrameworkField> getAnnotatedFieldsByParameter() {
        return getTestClass().getAnnotatedFields(Parameter.class);
    }

    private boolean fieldsAreAnnotated() {
        return !getAnnotatedFieldsByParameter().isEmpty();
    }

    private static class FieldsValidator implements TestClassValidator {
        private static final List<Exception> NO_VALIDATION_ERRORS = emptyList();

        private final List<Object> parameters;

        FieldsValidator(List<Object> parameters) {
            this.parameters = parameters;
        }

        public List<Exception> validateTestClass(TestClass testClass) {
            List<FrameworkField> annotatedFields = testClass
                    .getAnnotatedFields(Parameter.class);
            if (annotatedFields.isEmpty()) {
                return NO_VALIDATION_ERRORS;
            } else {
                return validateAnnotatedFields(annotatedFields);
            }
        }

        private List<Exception> validateAnnotatedFields(
                List<FrameworkField> fields) {
            List<Exception> errors = new ArrayList<Exception>();
            if (fields.size() != parameters.size()) {
                errors.add(new Exception(
                        "Wrong number of parameters and @Parameter fields."
                                + " @Parameter fields counted: "
                                + fields.size() + ", available parameters: "
                                + parameters.size() + "."));
            } else {
                int[] usedIndices = new int[fields.size()];
                for (FrameworkField each : fields) {
                    int index = each.getField().getAnnotation(Parameter.class)
                            .value();
                    if (index < 0 || index > fields.size() - 1) {
                        errors.add(new Exception("Invalid @Parameter value: "
                                + index + ". @Parameter fields counted: "
                                + fields.size()
                                + ". Please use an index between 0 and "
                                + (fields.size() - 1) + "."));
                    } else {
                        usedIndices[index]++;
                    }
                }
                for (int index = 0; index < usedIndices.length; index++) {
                    int numberOfUse = usedIndices[index];
                    if (numberOfUse == 0) {
                        errors.add(new Exception("@Parameter(" + index
                                + ") is never used."));
                    } else if (numberOfUse > 1) {
                        errors.add(new Exception("@Parameter(" + index
                                + ") is used more than once (" + numberOfUse
                                + ")."));
                    }
                }
            }
            return errors;
        }
    }
}
