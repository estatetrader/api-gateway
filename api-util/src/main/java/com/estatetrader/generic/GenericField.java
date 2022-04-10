package com.estatetrader.generic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class GenericField {
    private final Field nativeField;
    private final GenericType declaredType;
    private final GenericType resolvedType;

    public GenericField(Field nativeField, GenericType declaredType, GenericType resolvedType) {
        this.nativeField = nativeField;
        this.declaredType = declaredType;
        this.resolvedType = resolvedType;
    }

    public Field getNativeField() {
        return nativeField;
    }

    public GenericType getDeclaredType() {
        return declaredType;
    }

    public GenericType getResolvedType() {
        return resolvedType;
    }

    public GenericField replaceResolvedType(GenericType newResolvedType) {
        if (resolvedType.equals(newResolvedType)) {
            return this;
        } else {
            return new GenericField(nativeField, declaredType, newResolvedType);
        }
    }

    public boolean isStatic() {
        return Modifier.isStatic(nativeField.getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(nativeField.getModifiers());
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return nativeField.getAnnotation(annotationClass);
    }

    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return nativeField.getAnnotationsByType(annotationClass);
    }

    public void setAccessible(boolean flag) {
        nativeField.setAccessible(flag);
    }

    public String getName() {
        return nativeField.getName();
    }

    /**
     * Returns the value of the field represented by this {@code Field}, on
     * the specified object. The value is automatically wrapped in an
     * object if it has a primitive type.
     *
     * <p>The underlying field's value is obtained as follows:
     *
     * <p>If the underlying field is a static field, the {@code obj} argument
     * is ignored; it may be null.
     *
     * <p>Otherwise, the underlying field is an instance field.  If the
     * specified {@code obj} argument is null, the method throws a
     * {@code NullPointerException}. If the specified object is not an
     * instance of the class or interface declaring the underlying
     * field, the method throws an {@code IllegalArgumentException}.
     *
     * <p>If this {@code Field} object is enforcing Java language access control, and
     * the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}.
     * If the underlying field is static, the class that declared the
     * field is initialized if it has not already been initialized.
     *
     * <p>Otherwise, the value is retrieved from the underlying instance
     * or static field.  If the field has a primitive type, the value
     * is wrapped in an object before being returned, otherwise it is
     * returned as is.
     *
     * <p>If the field is hidden in the type of {@code obj},
     * the field's value is obtained according to the preceding rules.
     *
     * @param object object from which the represented field's value is
     * to be extracted
     * @return the value of the represented field in object
     * {@code obj}; primitive values are wrapped in an appropriate
     * object before being returned
     *
     * @exception IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof).
     * @exception NullPointerException      if the specified object is null
     *              and the field is an instance field.
     * @exception ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public Object get(Object object) {
        try {
            return nativeField.get(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Sets the field represented by this {@code Field} object on the
     * specified object argument to the specified new value. The new
     * value is automatically unwrapped if the underlying field has a
     * primitive type.
     *
     * <p>The operation proceeds as follows:
     *
     * <p>If the underlying field is static, the {@code obj} argument is
     * ignored; it may be null.
     *
     * <p>Otherwise the underlying field is an instance field.  If the
     * specified object argument is null, the method throws a
     * {@code NullPointerException}.  If the specified object argument is not
     * an instance of the class or interface declaring the underlying
     * field, the method throws an {@code IllegalArgumentException}.
     *
     * <p>If this {@code Field} object is enforcing Java language access control, and
     * the underlying field is inaccessible, the method throws an
     * {@code IllegalAccessException}.
     *
     * <p>If the underlying field is final, the method throws an
     * {@code IllegalAccessException} unless {@code setAccessible(true)}
     * has succeeded for this {@code Field} object
     * and the field is non-static. Setting a final field in this way
     * is meaningful only during deserialization or reconstruction of
     * instances of classes with blank final fields, before they are
     * made available for access by other parts of a program. Use in
     * any other context may have unpredictable effects, including cases
     * in which other parts of a program continue to use the original
     * value of this field.
     *
     * <p>If the underlying field is of a primitive type, an unwrapping
     * conversion is attempted to convert the new value to a value of
     * a primitive type.  If this attempt fails, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>If, after possible unwrapping, the new value cannot be
     * converted to the type of the underlying field by an identity or
     * widening conversion, the method throws an
     * {@code IllegalArgumentException}.
     *
     * <p>If the underlying field is static, the class that declared the
     * field is initialized if it has not already been initialized.
     *
     * <p>The field is set to the possibly unwrapped and widened new value.
     *
     * <p>If the field is hidden in the type of {@code obj},
     * the field's value is set according to the preceding rules.
     *
     * @param object the object whose field should be modified
     * @param value the new value for the field of {@code obj}
     * being modified
     *
     * @exception IllegalArgumentException  if the specified object is not an
     *              instance of the class or interface declaring the underlying
     *              field (or a subclass or implementor thereof),
     *              or if an unwrapping conversion fails.
     * @exception NullPointerException      if the specified object is null
     *              and the field is an instance field.
     * @exception ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    public void set(Object object, Object value) {
        try {
            nativeField.set(object, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the {@code Class} object representing the class or interface
     * that declares the field represented by this {@code Field} object.
     */
    public Class<?> getDeclaringClass() {
        return nativeField.getDeclaringClass();
    }

    @Override
    public String toString() {
        return nativeField.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof GenericField)) return false;
        GenericField that = (GenericField) object;
        return nativeField.equals(that.nativeField) &&
            resolvedType.equals(that.resolvedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nativeField, resolvedType);
    }
}
