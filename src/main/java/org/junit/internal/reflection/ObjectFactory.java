package org.junit.internal.reflection;


/**
 * Creates objects via reflection.
 * 
 * @since 4.12
 */
public class ObjectFactory {
    /**
     * Creates an object of the specified class. The class must have a zero-arg
     * constructor.
     * 
     * @param klass
     *            the object's klass.
     * @return a new object of the specified class.
     */
    public <T> T createObjectWithClass(Class<T> klass) throws Exception {
        return klass.getConstructor().newInstance();
    }
}
