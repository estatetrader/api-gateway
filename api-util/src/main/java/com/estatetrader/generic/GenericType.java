package com.estatetrader.generic;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

/**
 * Type is the common superinterface for all types in the Java
 * programming language. These include raw types, parameterized types,
 * array types, type variables and primitive types.
 *
 * @since 1.5
 */
public interface GenericType {
    /**
     * Returns a string describing this type, including information
     * about any type parameters.
     *
     * @return a string describing this type
     * @since 1.8
     */
    String getTypeName();

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    GenericType asResolvedType(TypeVariableResolver variableResolver);

    /**
     * replaces all the variables in this generic type consulting the specified declaringType.
     * the declaringType is one of the usage of a class type, in which this generic type is used to declare
     * a field type, method return type and parameter types, super class type and interface types.
     * @param declaringType the declaring type in which this generic type is used
     * @return resolved type
     */
    GenericType asResolvedType(ParameterizedType declaringType);

    /**
     * combines this type and the given actual-type,
     * if these two types do not match, than an IllegalArgument exception will be thrown.
     * calculates the variables included in this type and its nested types while combine.
     * @param actualType the actual-type used to resolve variables
     * @return the binding of all the variables included in this type and their resolved types,
     * or null if these two types do not match
     */
    VariableBinding combineType(GenericType actualType);

    /**
     * calculate the intersected type of this type and the given type
     * @param another another type
     * @return the intersected type, or null if there is no intersect type between the two types
     */
    GenericType intersect(GenericType another);

    /**
     * check if this type can be assigned by the given type another.
     * @param another the another type
     * @return true if the assigment can be archived
     */
    boolean isAssignableFrom(GenericType another);

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
     *        annotation type
     * @return true if an annotation for the specified annotation
     *     type is present on this element, else false
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>present</em>, else null.
     *
     * @param <T> the type of the annotation to query for and return if present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns annotations that are <em>present</em> on this element.
     *
     * If there are no annotations <em>present</em> on this element, the return
     * value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations present on this element
     * @since 1.5
     */
    Annotation[] getAnnotations();

    /**
     * Returns annotations that are <em>associated</em> with this element.
     *
     * If there are no annotations <em>associated</em> with this element, the return
     * value is an array of length 0.
     *
     * The difference between this method and {@link #getAnnotation(Class)}
     * is that this method detects if its argument is a <em>repeatable
     * annotation type</em> (JLS 9.6), and if so, attempts to find one or
     * more annotations of that type by "looking through" a container
     * annotation.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
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
     *
     * @param <T> the type of the annotation to query for and return if present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     associated with this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass);

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>directly present</em>, else null.
     *
     * This method ignores inherited annotations. (Returns null if no
     * annotations are directly present on this element.)
     *
     * @implSpec The default implementation first performs a null check
     * and then loops over the results of {@link
     * #getDeclaredAnnotations} returning the first annotation whose
     * annotation type matches the argument type.
     *
     * @param <T> the type of the annotation to query for and return if directly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     directly present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass);

    /**
     * Returns this element's annotation(s) for the specified type if
     * such annotations are either <em>directly present</em> or
     * <em>indirectly present</em>. This method ignores inherited
     * annotations.
     *
     * If there are no specified annotations directly or indirectly
     * present on this element, the return value is an array of length
     * 0.
     *
     * The difference between this method and {@link
     * #getDeclaredAnnotation(Class)} is that this method detects if its
     * argument is a <em>repeatable annotation type</em> (JLS 9.6), and if so,
     * attempts to find one or more annotations of that type by "looking
     * through" a container annotation if one is present.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
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
     *
     * @param <T> the type of the annotation to query for and return
     * if directly or indirectly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     directly or indirectly present on this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass);

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     *
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    Annotation[] getDeclaredAnnotations();

    /**
     * the internal annotated element of this generic type
     * @return annotated element of this generic type
     */
    AnnotatedElement annotatedElement();

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     * @return the type represented by Java native type system
     */
    Type toReflectType();

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     * @return the type represented by Java native type system
     */
    AnnotatedType toAnnotatedType();

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    boolean equals(GenericType type);

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    boolean equals(Type type);

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    boolean equals(AnnotatedType type);

    /**
     * replace the component types of this type with the given replacer
     * @param replacer the replacer which specifies the type replacement logic
     * @return the replaced type
     */
    GenericType replace(GenericTypeReplacer replacer);

    /**
     * visit this type and all this component types using the given visitor
     * @param visitor the visitor instance used to visit this type
     * @param <R> the visit report type
     * @return the final visit report
     */
    <R> R visit(GenericTypeVisitor<R> visitor);
}
