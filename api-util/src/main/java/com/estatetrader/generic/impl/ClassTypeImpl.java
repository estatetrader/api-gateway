package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;
import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ClassTypeImpl extends StaticTypeImpl implements ClassType {
    private static final Set<Class<?>> BASIC_SIMPLE_TYPES = Stream.of(
        boolean.class,
        Boolean.class,
        byte.class,
        Byte.class,
        char.class,
        Character.class,
        CharSequence.class,
        Class.class,
        Date.class,
        double.class,
        Double.class,
        Enum.class,
        float.class,
        Float.class,
        int.class,
        Integer.class,
        long.class,
        Long.class,
        Number.class,
        short.class,
        Short.class,
        String.class,
        StringBuffer.class,
        StringBuilder.class,
        void.class,
        Void.class
    ).collect(Collectors.toSet());

    public ClassTypeImpl(Class<?> clazz) {
        super(clazz);
    }

    /**
     * Returns the {@code Type} object representing the class or interface
     * that declared this type.
     *
     * @return the {@code Type} object representing the class or interface
     * that declared this type
     * @since 1.5
     */
    @Override
    public Class<?> getRawType() {
        return (Class<?>) element;
    }

    /**
     * Returns a string describing this type, including information
     * about any type parameters.
     *
     * @return a string describing this type
     * @since 1.8
     */
    @Override
    public String getTypeName() {
        return getRawType().getTypeName();
    }

    /**
     * Determines if the specified {@code Class} object represents an
     * interface type.
     *
     * @return {@code true} if this object represents an interface;
     * {@code false} otherwise.
     */
    @Override
    public boolean isInterface() {
        return getRawType().isInterface();
    }

    /**
     * Determines if the specified {@code Class} object represents a
     * primitive type.
     *
     * <p> There are nine predefined {@code Class} objects to represent
     * the eight primitive types and void.  These are created by the Java
     * Virtual Machine, and have the same names as the primitive types that
     * they represent, namely {@code boolean}, {@code byte},
     * {@code char}, {@code short}, {@code int},
     * {@code long}, {@code float}, and {@code double}.
     *
     * <p> These objects may only be accessed via the following public static
     * final variables, and are the only {@code Class} objects for which
     * this method returns {@code true}.
     *
     * @return true if and only if this class represents a primitive type
     * @see Boolean#TYPE
     * @see Character#TYPE
     * @see Byte#TYPE
     * @see Short#TYPE
     * @see Integer#TYPE
     * @see Long#TYPE
     * @see Float#TYPE
     * @see Double#TYPE
     * @see Void#TYPE
     * @since JDK1.1
     */
    @Override
    public boolean isPrimitive() {
        return getRawType().isPrimitive();
    }

    /**
     * 判断给定的类型是否简单类型，所谓简单类型，指的是能在网络上直接传输的类型（提供了原生的序列化方式）。<br/>
     * 包括如下情况：
     * <ul>
     *     <li>Java基元类型（由硬件直接支持的类型），例如byte,char,int,long,float等</li>
     *     <li>基元类型对应的包装类型，例如Integer,Long</li>
     *     <li>数字类型，Number抽象类的所有实现类</li>
     *     <li>字符串类型：String</li>
     *     <li>日期类型：Date</li>
     *     <li>类：Class</li>
     *     <li>枚举类型</li>
     * </ul>
     *
     * @return 返回true表示当前类型是简单类型
     */
    @Override
    public boolean isSimpleType() {
        Class<?> clazz = getRawType();
        return BASIC_SIMPLE_TYPES.contains(clazz)
            || clazz.isEnum()
            || Date.class.isAssignableFrom(clazz)
            || Number.class.isAssignableFrom(clazz);
    }

    /**
     * Returns true if this {@code Class} object represents an annotation
     * type.  Note that if this method returns true, {@link #isInterface()}
     * would also return true, as all annotation types are also interfaces.
     *
     * @return {@code true} if this class object represents an annotation
     * type; {@code false} otherwise
     * @since 1.5
     */
    @Override
    public boolean isAnnotation() {
        return getRawType().isAnnotation();
    }

    /**
     * Returns true if and only if this class was declared as an enum in the
     * source code.
     *
     * @return true if and only if this class was declared as an enum in the
     * source code
     * @since 1.5
     */
    @Override
    public boolean isEnum() {
        return getRawType().isEnum();
    }

    /**
     * Returns the Java language modifiers for this class or interface, encoded
     * in an integer. The modifiers consist of the Java Virtual Machine's
     * constants for {@code public}, {@code protected},
     * {@code private}, {@code final}, {@code static},
     * {@code abstract} and {@code interface}; they should be decoded
     * using the methods of class {@code Modifier}.
     *
     * <p> If the underlying class is an array class, then its
     * {@code public}, {@code private} and {@code protected}
     * modifiers are the same as those of its component type.  If this
     * {@code Class} represents a primitive type or void, its
     * {@code public} modifier is always {@code true}, and its
     * {@code protected} and {@code private} modifiers are always
     * {@code false}. If this object represents an array class, a
     * primitive type or void, then its {@code final} modifier is always
     * {@code true} and its interface modifier is always
     * {@code false}. The values of its other modifiers are not determined
     * by this specification.
     *
     * <p> The modifier encodings are defined in <em>The Java Virtual Machine
     * Specification</em>, table 4.1.
     *
     * @return the {@code int} representing the modifiers for this class
     * @see Modifier
     * @since JDK1.1
     */
    @Override
    public int getModifiers() {
        return getRawType().getModifiers();
    }

    /**
     * Returns the  name of the entity (class, interface, array class,
     * primitive type, or void) represented by this {@code Class} object,
     * as a {@code String}.
     *
     * <p> If this class object represents a reference type that is not an
     * array type then the binary name of the class is returned, as specified
     * by
     * <cite>The Java&trade; Language Specification</cite>.
     *
     * <p> If this class object represents a primitive type or void, then the
     * name returned is a {@code String} equal to the Java language
     * keyword corresponding to the primitive type or void.
     *
     * <p> If this class object represents a class of arrays, then the internal
     * form of the name consists of the name of the element type preceded by
     * one or more '{@code [}' characters representing the depth of the array
     * nesting.  The encoding of element type names is as follows:
     *
     * <blockquote><table summary="Element types and encodings">
     * <tr><th> Element Type <th> &nbsp;&nbsp;&nbsp; <th> Encoding
     * <tr><td> boolean      <td> &nbsp;&nbsp;&nbsp; <td align=center> Z
     * <tr><td> byte         <td> &nbsp;&nbsp;&nbsp; <td align=center> B
     * <tr><td> char         <td> &nbsp;&nbsp;&nbsp; <td align=center> C
     * <tr><td> class or interface
     * <td> &nbsp;&nbsp;&nbsp; <td align=center> L<i>classname</i>;
     * <tr><td> double       <td> &nbsp;&nbsp;&nbsp; <td align=center> D
     * <tr><td> float        <td> &nbsp;&nbsp;&nbsp; <td align=center> F
     * <tr><td> int          <td> &nbsp;&nbsp;&nbsp; <td align=center> I
     * <tr><td> long         <td> &nbsp;&nbsp;&nbsp; <td align=center> J
     * <tr><td> short        <td> &nbsp;&nbsp;&nbsp; <td align=center> S
     * </table></blockquote>
     *
     * <p> The class or interface name <i>classname</i> is the binary name of
     * the class specified above.
     *
     * <p> Examples:
     * <blockquote><pre>
     * String.class.getName()
     *     returns "java.lang.String"
     * byte.class.getName()
     *     returns "byte"
     * (new Object[3]).getClass().getName()
     *     returns "[Ljava.lang.Object;"
     * (new int[3][4][5][6][7][8][9]).getClass().getName()
     *     returns "[[[[[[[I"
     * </pre></blockquote>
     *
     * @return the name of the class or interface
     * represented by this object.
     */
    @Override
    public String getName() {
        return getRawType().getName();
    }

    /**
     * Returns the simple name of the underlying class as given in the
     * source code. Returns an empty string if the underlying class is
     * anonymous.
     *
     * <p>The simple name of an array is the simple name of the
     * component type with "[]" appended.  In particular the simple
     * name of an array whose component type is anonymous is "[]".
     *
     * @return the simple name of the underlying class
     * @since 1.5
     */
    @Override
    public String getSimpleName() {
        return getRawType().getSimpleName();
    }

    /**
     * Returns the canonical name of the underlying class as
     * defined by the Java Language Specification.  Returns null if
     * the underlying class does not have a canonical name (i.e., if
     * it is a local or anonymous class or an array whose component
     * type does not have a canonical name).
     *
     * @return the canonical name of the underlying class if it exists, and
     * {@code null} otherwise.
     * @since 1.5
     */
    @Override
    public String getCanonicalName() {
        return getRawType().getCanonicalName();
    }

    /**
     * Returns a {@code Constructor} object that reflects the specified
     * constructor of the class or interface represented by this
     * {@code Class} object.  The {@code parameterTypes} parameter is
     * an array of {@code Class} objects that identify the constructor's
     * formal parameter types, in declared order.
     * <p>
     * If this {@code Class} object represents an inner class
     * declared in a non-static context, the formal parameter types
     * include the explicit enclosing instance as the first parameter.
     *
     * @param parameterTypes the parameter array
     * @return The {@code Constructor} object for the constructor with the
     * specified parameter list
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws SecurityException     If a security manager, <i>s</i>, is present and any of the
     *                               following conditions is met:
     *
     *                               <ul>
     *
     *                               <li> the caller's class loader is not the same as the
     *                               class loader of this class and invocation of
     *                               {@link SecurityManager#checkPermission
     *                               s.checkPermission} method with
     *                               {@code RuntimePermission("accessDeclaredMembers")}
     *                               denies access to the declared constructor
     *
     *                               <li> the caller's class loader is not the same as or an
     *                               ancestor of the class loader for the current class and
     *                               invocation of {@link SecurityManager#checkPackageAccess
     *                               s.checkPackageAccess()} denies access to the package
     *                               of this class
     *
     *                               </ul>
     * @since JDK1.1
     */
    @Override
    public Constructor<?> getDeclaredConstructor(Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        return getRawType().getDeclaredConstructor(parameterTypes);
    }

    /**
     * the origin type when it is declared, the parameterized-type with its type variables, for instance
     *
     * @return the origin declared type of this type
     */
    @Override
    public StaticType getDeclaredType() {
        return this;
    }

    /**
     * Determines if the class or interface represented by this
     * {@code Class} object is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the specified
     * {@code Class} parameter. It returns {@code true} if so;
     * otherwise it returns {@code false}. If this {@code Class}
     * object represents a primitive type, this method returns
     * {@code true} if the specified {@code Class} parameter is
     * exactly this {@code Class} object; otherwise it returns
     * {@code false}.
     *
     * <p> Specifically, this method tests whether the type represented by the
     * specified {@code Class} parameter can be converted to the type
     * represented by this {@code Class} object via an identity conversion
     * or via a widening reference conversion. See <em>The Java Language
     * Specification</em>, sections 5.1.1 and 5.1.4 , for details.
     *
     * @param another the {@code Class} object to be checked
     * @return the {@code boolean} value indicating whether objects of the
     * type {@code cls} can be assigned to objects of this class
     * @throws NullPointerException if the specified Class parameter is
     *                              null.
     * @since JDK1.1
     */
    @Override
    public boolean isAssignableFrom(ClassType another) {
        return getRawType().isAssignableFrom(another.getRawType());
    }

    /**
     * check if the instances of this type can be assigned to the given class type
     *
     * @param clazz the given class type to check
     * @return true if this type's instance is assignable to the given class
     */
    @Override
    public boolean isAssignableTo(Class<?> clazz) {
        return clazz.isAssignableFrom(getRawType());
    }

    /**
     * replaces all the variables in this generic type consulting the specified variable resolver.
     *
     * @param variableResolver the variable resolver used to resolve all the variables included in this type
     * @return resolved type
     */
    @Override
    public ClassType asResolvedType(TypeVariableResolver variableResolver) {
        return this;
    }

    /**
     * replaces all the variables in this generic type consulting the specified declaringType.
     * the declaringType is one of the usage of a class type, in which this generic type is used to declare
     * a field type, method return type and parameter types, super class type and interface types.
     *
     * @param declaringType the declaring type in which this generic type is used
     * @return resolved type
     */
    @Override
    public ClassType asResolvedType(ParameterizedType declaringType) {
        return (ClassType) super.asResolvedType(declaringType);
    }

    /**
     * combines this type and the given actual-type,
     * if these two types do not match, than an IllegalArgument exception will be thrown.
     * calculates the variables included in this type and its nested types while combine.
     *
     * @param actualType the actual-type used to resolve variables
     * @return the binding of all the variables included in this type and their resolved types,
     * or null if these two types do not match
     */
    @Override
    public VariableBinding combineType(GenericType actualType) {
        if (this.equals(actualType)
            || actualType instanceof StaticType
            && getRawType().isAssignableFrom(((StaticType) actualType).getRawType())) {
            return VariableBinding.EMPTY;
        } else {
            return null;
        }
    }

    /**
     * calculate the intersected type of this type and the given type
     *
     * @param another another type
     * @return the intersected type, or null if there is no intersect type between the two types
     */
    @Override
    public GenericType intersect(GenericType another) {
        if (this.equals(another)) {
            return this;
        } else if (another instanceof StaticType) {
            StaticType anotherType = (StaticType) another;
            if (anotherType.asSuperType(this.getRawType()) != null) {
                return another;
            } else if (this.asSuperType(anotherType.getRawType()) != null) {
                return this;
            } else {
                return null;
            }
        } else {
            return another.intersect(this);
        }
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link Type}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public Class<?> toReflectType() {
        return getRawType();
    }

    /**
     * convert the type represented by this generic-type to the format used by Java native type {@link AnnotatedType}
     *
     * @return the type represented by Java native type system
     */
    @Override
    public AnnotatedType toAnnotatedType() {
        return new AnnotatedReflectType(this);
    }

    /**
     * compares this type with the given native generic type and return true if these two types are the same
     *
     * @param type the native generic type to compare to
     * @return true if these two types are the same
     */
    @Override
    public boolean equals(GenericType type) {
        if (!(type instanceof ClassType)) return false;
        ClassType classType = (ClassType) type;
        return getRawType().equals(classType.getRawType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawType());
    }

    @Override
    protected GenericType replaceComponents(GenericTypeReplacer replacer) {
        return this;
    }

    /**
     * visit this type and all this component types using the given visitor
     *
     * @param visitor the visitor instance used to visit this type
     * @return the final visit report
     */
    @Override
    public <R> R visit(GenericTypeVisitor<R> visitor) {
        return visitor.visitType(this, Collections.emptyList());
    }

    static class OfCollection extends ClassTypeImpl implements CollectionType {

        private final GenericType elementType;

        OfCollection(Class<?> clazz) {
            super(clazz);
            ParameterizedType pType = (ParameterizedType) asSuperType(Collection.class);
            GenericType[] typeArgs = pType.getTypeArguments();
            if (typeArgs.length != 1) {
                throw new IllegalStateException("the type " + pType + " should contains exactly one type argument");
            }
            this.elementType = typeArgs[0];
        }

        /**
         * the element type of the collection
         *
         * @return element type
         */
        @Override
        public GenericType getElementType() {
            return elementType;
        }
    }

    static class OfMap extends ClassTypeImpl implements MapType {

        private final GenericType keyType;
        private final GenericType valueType;

        public OfMap(Class<?> clazz) {
            super(clazz);
            ParameterizedType pType = (ParameterizedType) asSuperType(Map.class);
            GenericType[] typeArgs = pType.getTypeArguments();
            if (typeArgs.length != 2) {
                throw new IllegalStateException("the type " + pType + " should contains exactly two type arguments");
            }
            this.keyType = typeArgs[0];
            this.valueType = typeArgs[1];
        }

        /**
         * get the key type of the map
         *
         * @return the generic presentation of the key type
         */
        @Override
        public GenericType getKeyType() {
            return keyType;
        }

        /**
         * get the value type of the map
         *
         * @return the generic presentation of the value type
         */
        @Override
        public GenericType getValueType() {
            return valueType;
        }
    }
}
