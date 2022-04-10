package com.estatetrader.generic.impl;

import com.estatetrader.generic.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class StaticTypeImpl extends AbstractGenericType implements StaticType {
    public StaticTypeImpl(AnnotatedElement element) {
        super(element);
    }

    /**
     * Returns the {@code Class} representing the superclass of the entity
     * (class, interface, primitive type or void) represented by this
     * {@code Class}.  If this {@code Class} represents either the
     * {@code Object} class, an interface, a primitive type, or void, then
     * null is returned.  If this object represents an array class then the
     * {@code Class} object representing the {@code Object} class is
     * returned.
     *
     * @return the superclass of the class represented by this object.
     */
    @Override
    public StaticType getSuperclass() {
        AnnotatedType st = getRawType().getAnnotatedSuperclass();
        if (st == null) {
            return null;
        } else {
            GenericType gt = GenericTypeFactory.toGenericType(st);
            if (this instanceof ParameterizedType) {
                return (StaticType) gt.asResolvedType((ParameterizedType) this);
            } else {
                return (StaticType) gt;
            }
        }
    }

    /**
     * Determines the interfaces implemented by the class or interface
     * represented by this object.
     *
     * <p> If this object represents a class, the return value is an array
     * containing objects representing all interfaces implemented by the
     * class. The order of the interface objects in the array corresponds to
     * the order of the interface names in the {@code implements} clause
     * of the declaration of the class represented by this object. For
     * example, given the declaration:
     * <blockquote>
     * {@code class Shimmer implements FloorWax, DessertTopping { ... }}
     * </blockquote>
     * suppose the value of {@code s} is an instance of
     * {@code Shimmer}; the value of the expression:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[0]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code FloorWax}; and the value of:
     * <blockquote>
     * {@code s.getClass().getInterfaces()[1]}
     * </blockquote>
     * is the {@code Class} object that represents interface
     * {@code DessertTopping}.
     *
     * <p> If this object represents an interface, the array contains objects
     * representing all interfaces extended by the interface. The order of the
     * interface objects in the array corresponds to the order of the interface
     * names in the {@code extends} clause of the declaration of the
     * interface represented by this object.
     *
     * <p> If this object represents a class or interface that implements no
     * interfaces, the method returns an array of length 0.
     *
     * <p> If this object represents a primitive type or void, the method
     * returns an array of length 0.
     *
     * <p> If this {@code Class} object represents an array type, the
     * interfaces {@code Cloneable} and {@code java.io.Serializable} are
     * returned in that order.
     *
     * @return an array of interfaces implemented by this class.
     */
    @Override
    public StaticType[] getInterfaces() {
        AnnotatedType[] its = getRawType().getAnnotatedInterfaces();
        StaticType[] gts = new StaticType[its.length];
        for (int i = 0; i < its.length; i++) {
            StaticType grt = (StaticType) GenericTypeFactory.toGenericType(its[i]);
            if (this instanceof ParameterizedType) {
                gts[i] = grt.asResolvedType((ParameterizedType) this);
            } else {
                gts[i] = grt;
            }
        }
        return gts;
    }

    /**
     * calculates and returns all the fields and their corresponding generic resolved typ declared in this type
     *
     * @return all declared fields
     */
    @Override
    public GenericField[] getDeclaredFields() {
        Field[] nativeFields = getRawType().getDeclaredFields();
        GenericField[] genericFields = new GenericField[nativeFields.length];
        for (int i = 0; i < nativeFields.length; i++) {
            Field nativeField = nativeFields[i];
            GenericType fieldType = GenericTypes.fieldType(nativeField);
            GenericType resolvedType;
            if (this instanceof ParameterizedType) {
                resolvedType = fieldType.asResolvedType((ParameterizedType) this);
            } else {
                resolvedType = fieldType;
            }
            genericFields[i] = new GenericField(nativeField, fieldType, resolvedType);
        }
        return genericFields;
    }

    /**
     * Returns a {@code Field} object that reflects the specified declared
     * field of the class or interface represented by this {@code Class}
     * object. The {@code name} parameter is a {@code String} that specifies
     * the simple name of the desired field.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code length} field of the array type.
     *
     * @param name the name of the field
     * @return the {@code Field} object for the specified field in this
     * class
     * @throws NoSuchFieldException if a field with the specified name is
     *                              not found.
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws SecurityException    If a security manager, <i>s</i>, is present and any of the
     *                              following conditions is met:
     *
     *                              <ul>
     *
     *                              <li> the caller's class loader is not the same as the
     *                              class loader of this class and invocation of
     *                              {@link SecurityManager#checkPermission
     *                              s.checkPermission} method with
     *                              {@code RuntimePermission("accessDeclaredMembers")}
     *                              denies access to the declared field
     *
     *                              <li> the caller's class loader is not the same as or an
     *                              ancestor of the class loader for the current class and
     *                              invocation of {@link SecurityManager#checkPackageAccess
     *                              s.checkPackageAccess()} denies access to the package
     *                              of this class
     *
     *                              </ul>
     * @since JDK1.1
     */
    @Override
    public GenericField getDeclaredField(String name) throws NoSuchFieldException, SecurityException {
        Field field = getRawType().getField(name);
        GenericType fieldType = GenericTypes.fieldType(field);
        GenericType resolvedType;
        if (this instanceof ParameterizedType) {
            resolvedType = fieldType.asResolvedType((ParameterizedType) this);
        } else {
            resolvedType = fieldType;
        }
        return new GenericField(field, fieldType, resolvedType);
    }

    /**
     * Returns an array containing {@code Method} objects reflecting all the
     * declared methods of the class or interface represented by this {@code
     * Class} object, including public, protected, default (package)
     * access, and private methods, but excluding inherited methods.
     *
     * <p> If this {@code Class} object represents a type that has multiple
     * declared methods with the same name and parameter types, but different
     * return types, then the returned array has a {@code Method} object for
     * each such method.
     *
     * <p> If this {@code Class} object represents a type that has a class
     * initialization method {@code <clinit>}, then the returned array does
     * <em>not</em> have a corresponding {@code Method} object.
     *
     * <p> If this {@code Class} object represents a class or interface with no
     * declared methods, then the returned array has length 0.
     *
     * <p> If this {@code Class} object represents an array type, a primitive
     * type, or void, then the returned array has length 0.
     *
     * <p> The elements in the returned array are not sorted and are not in any
     * particular order.
     *
     * @return the array of {@code Method} objects representing all the
     * declared methods of this class
     * @throws SecurityException If a security manager, <i>s</i>, is present and any of the
     *                           following conditions is met:
     *
     *                           <ul>
     *
     *                           <li> the caller's class loader is not the same as the
     *                           class loader of this class and invocation of
     *                           {@link SecurityManager#checkPermission
     *                           s.checkPermission} method with
     *                           {@code RuntimePermission("accessDeclaredMembers")}
     *                           denies access to the declared methods within this class
     *
     *                           <li> the caller's class loader is not the same as or an
     *                           ancestor of the class loader for the current class and
     *                           invocation of {@link SecurityManager#checkPackageAccess
     *                           s.checkPackageAccess()} denies access to the package
     *                           of this class
     *
     *                           </ul>
     * @since JDK1.1
     */
    @Override
    public Method[] getDeclaredMethods() throws SecurityException {
        return getRawType().getDeclaredMethods();
    }

    /**
     * Returns a {@code Method} object that reflects the specified
     * declared method of the class or interface represented by this
     * {@code Class} object. The {@code name} parameter is a
     * {@code String} that specifies the simple name of the desired
     * method, and the {@code parameterTypes} parameter is an array of
     * {@code Class} objects that identify the method's formal parameter
     * types, in declared order.  If more than one method with the same
     * parameter types is declared in a class, and one of these methods has a
     * return type that is more specific than any of the others, that method is
     * returned; otherwise one of the methods is chosen arbitrarily.  If the
     * name is "&lt;init&gt;"or "&lt;clinit&gt;" a {@code NoSuchMethodException}
     * is raised.
     *
     * <p> If this {@code Class} object represents an array type, then this
     * method does not find the {@code clone()} method.
     *
     * @param name           the name of the method
     * @param parameterTypes the parameter array
     * @return the {@code Method} object for the method of this class
     * matching the specified name and parameters
     * @throws NoSuchMethodException if a matching method is not found.
     * @throws NullPointerException  if {@code name} is {@code null}
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
     *                               denies access to the declared method
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
    public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException {
        return getRawType().getDeclaredMethod(name, parameterTypes);
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
    public StaticType asResolvedType(ParameterizedType declaringType) {
        return (StaticType) super.asResolvedType(declaringType);
    }

    /**
     * convert this generic type to one of its super types with the specified class
     *
     * @param superType the super-type converting to
     * @return the converted type, or null if the specified super type is not this type's super
     */
    @Override
    public StaticType asSuperType(Class<?> superType) {
        if (!superType.isAssignableFrom(getRawType())) {
            return null;
        }
        if (getRawType().equals(superType)) {
            return this;
        }
        StaticType sc = getSuperclass();
        if (sc != null) {
            StaticType st = sc.asSuperType(superType);
            if (st != null) {
                return st;
            }
        }
        for (StaticType i : getInterfaces()) {
            StaticType si = i.asSuperType(superType);
            if (si != null) {
                return si;
            }
        }
        return null;
    }

    /**
     * convert this generic type to one of its sub types with the specified class
     *
     * @param subType the sub-type converting to
     * @return the converted type, or null if the specified super type is not this type's sub class
     */
    @Override
    public StaticType asSubType(Class<?> subType) {
        if (!getRawType().isAssignableFrom(subType)) {
            return null;
        }
        // the sub-type cannot be array type
        StaticType formalSubType = GenericTypeFactory.fromClass(subType, true);
        // use the formal subtype to calculate its super type to this type,
        StaticType thisTypeWithVars = formalSubType.asSuperType(getRawType());
        // so, the calculated super type is the representation of this type
        // using the variables defined in the formal subtype
        if (thisTypeWithVars == null) {
            throw new IllegalStateException("could not calculate the super class " + getRawType()
                + " for " + formalSubType);
        }
        // then, combine the variable-included this type with this type (which includes actual type arguments)
        VariableBinding binding = thisTypeWithVars.combineType(this);
        if (binding == null) {
            return null;
        }
        TypeVariableResolver resolver = new TypeVariableResolver() {
            VariableBinding currentBinding = binding;
            @Override
            public GenericType resolve(TypeVariable var) {
                GenericType t = currentBinding.get(var);
                if (t != null) {
                    return t;
                } else {
                    // if the variable is not defined in this type, then an wildcard is returned
                    GenericType n = new WildcardTypeImpl(var.annotatedElement(), var.getBounds(), null);
                    currentBinding = currentBinding.add(var, n);
                    return n;
                }
            }
        };
        // replaces all variables in the formal-sub-type, and return it
        return formalSubType.asResolvedType(resolver);
    }

    /**
     * check if this type can be assigned by the given type another.
     *
     * @param another the another type
     * @return true if the assigment can be archived
     */
    @Override
    public boolean isAssignableFrom(GenericType another) {
        if (this.equals(another)) {
            return true;
        } else if (another instanceof StaticType) {
            StaticType anotherType = (StaticType) another;
            StaticType anotherSuperType = anotherType.asSuperType(getRawType());
            if (anotherSuperType == null) {
                return false;
            }
            return combineType(anotherSuperType) != null;
        } else if (another instanceof DynamicType) {
            DynamicType anotherType = (DynamicType) another;
            for (GenericType upperBound : anotherType.getUpperBounds()) {
                if (!isAssignableFrom(upperBound)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
