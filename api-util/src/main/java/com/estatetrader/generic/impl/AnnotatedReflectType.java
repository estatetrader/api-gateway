package com.estatetrader.generic.impl;

import com.estatetrader.generic.GenericType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

class AnnotatedReflectType extends ReflectType implements AnnotatedType {
    public AnnotatedReflectType(GenericType genericType) {
        super(genericType);
    }

    /**
     * Returns the underlying type that this annotated type represents.
     *
     * @return the type this annotated type represents
     */
    @Override
    public Type getType() {
        return getGenericType().toReflectType();
    }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>present</em>, else null.
     *
     * @param annotationClass the Class object corresponding to the
     *                        annotation type
     * @return this element's annotation for the specified annotation type if
     * present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return getGenericType().getAnnotation(annotationClass);
    }

    /**
     * Returns annotations that are <em>present</em> on this element.
     * <p>
     * If there are no annotations <em>present</em> on this element, the return
     * value is an array of length 0.
     * <p>
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations present on this element
     * @since 1.5
     */
    @Override
    public Annotation[] getAnnotations() {
        return getGenericType().getAnnotations();
    }

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     * <p>
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     * <p>
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    @Override
    public Annotation[] getDeclaredAnnotations() {
        return getGenericType().getDeclaredAnnotations();
    }

    /**
     * Returns true if an annotation for the specified type
     * is <em>present</em> on this element, else false.  This method
     * is designed primarily for convenient access to marker annotations.
     *
     * <p>The truth value returned by this method is equivalent to:
     * {@code getAnnotation(annotationClass) != null}
     *
     * <p>The body of the default method is specified to be the code
     * above.
     *
     * @param annotationClass the Class object corresponding to the
     *                        annotation type
     * @return true if an annotation for the specified annotation
     * type is present on this element, else false
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getGenericType().isAnnotationPresent(annotationClass);
    }

    /**
     * Returns annotations that are <em>associated</em> with this element.
     * <p>
     * If there are no annotations <em>associated</em> with this element, the return
     * value is an array of length 0.
     * <p>
     * The difference between this method and {@link #getAnnotation(Class)}
     * is that this method detects if its argument is a <em>repeatable
     * annotation type</em> (JLS 9.6), and if so, attempts to find one or
     * more annotations of that type by "looking through" a container
     * annotation.
     * <p>
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @param annotationClass the Class object corresponding to the
     *                        annotation type
     * @return all this element's annotations for the specified annotation type if
     * associated with this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @implSpec The default implementation first calls {@link
     * #getDeclaredAnnotationsByType(Class)} passing {@code
     * annotationClass} as the argument. If the returned array has
     * length greater than zero, the array is returned. If the returned
     * array is zero-length and this {@code AnnotatedElement} is a
     * class and the argument type is an inheritable annotation type,
     * and the superclass of this {@code AnnotatedElement} is non-null,
     * then the returned result is the result of calling {@link
     * #getAnnotationsByType(Class)} on the superclass with {@code
     * annotationClass} as the argument. Otherwise, a zero-length
     * array is returned.
     * @since 1.8
     */
    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return getGenericType().getAnnotationsByType(annotationClass);
    }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>directly present</em>, else null.
     * <p>
     * This method ignores inherited annotations. (Returns null if no
     * annotations are directly present on this element.)
     *
     * @param annotationClass the Class object corresponding to the
     *                        annotation type
     * @return this element's annotation for the specified annotation type if
     * directly present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @implSpec The default implementation first performs a null check
     * and then loops over the results of {@link
     * #getDeclaredAnnotations} returning the first annotation whose
     * annotation type matches the argument type.
     * @since 1.8
     */
    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return getGenericType().getDeclaredAnnotation(annotationClass);
    }

    /**
     * Returns this element's annotation(s) for the specified type if
     * such annotations are either <em>directly present</em> or
     * <em>indirectly present</em>. This method ignores inherited
     * annotations.
     * <p>
     * If there are no specified annotations directly or indirectly
     * present on this element, the return value is an array of length
     * 0.
     * <p>
     * The difference between this method and {@link
     * #getDeclaredAnnotation(Class)} is that this method detects if its
     * argument is a <em>repeatable annotation type</em> (JLS 9.6), and if so,
     * attempts to find one or more annotations of that type by "looking
     * through" a container annotation if one is present.
     * <p>
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @param annotationClass the Class object corresponding to the
     *                        annotation type
     * @return all this element's annotations for the specified annotation type if
     * directly or indirectly present on this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @implSpec The default implementation may call {@link
     * #getDeclaredAnnotation(Class)} one or more times to find a
     * directly present annotation and, if the annotation type is
     * repeatable, to find a container annotation. If annotations of
     * the annotation type {@code annotationClass} are found to be both
     * directly and indirectly present, then {@link
     * #getDeclaredAnnotations()} will get called to determine the
     * order of the elements in the returned array.
     *
     * <p>Alternatively, the default implementation may call {@link
     * #getDeclaredAnnotations()} a single time and the returned array
     * examined for both directly and indirectly present
     * annotations. The results of calling {@link
     * #getDeclaredAnnotations()} are assumed to be consistent with the
     * results of calling {@link #getDeclaredAnnotation(Class)}.
     * @since 1.8
     */
    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return getGenericType().getDeclaredAnnotationsByType(annotationClass);
    }

    protected static AnnotatedType[] toAnnotatedTypes(GenericType[] types) {
        AnnotatedType[] rs = new AnnotatedType[types.length];
        for (int i = 0; i < types.length; i++) {
            rs[i] = types[i].toAnnotatedType();
        }
        return rs;
    }

    protected static Type[] toReflectTypes(GenericType[] types) {
        Type[] rs = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            rs[i] = types[i].toReflectType();
        }
        return rs;
    }
}
