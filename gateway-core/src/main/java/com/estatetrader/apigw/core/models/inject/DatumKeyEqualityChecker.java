package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.*;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

@FunctionalInterface
public interface DatumKeyEqualityChecker {

    boolean keyEquals(Object datumKey, Object targetKey);

    static DatumKeyEqualityChecker forType(GenericType datumKeyType, GenericType targetType) {
        // 两个类型相等，或有赋值兼容关系
        if (datumKeyType.isAssignableFrom(targetType)) {
            // 由于datum key是用于接收其他数据，因此它可以为更加抽象的类型
            return DefaultEqualityChecker.INSTANCE;
        } else if (datumKeyType instanceof ClassType && targetType instanceof ClassType) {
            Class<?> ct = ((ClassType) datumKeyType).getRawType();
            Class<?> ca = ((ClassType) targetType).getRawType();
            // 两个类型均为数字类型，则包装类型和基元类型相互兼容
            // todo 处理包装类、char、void
            if (canPromoteToLong(ct) && canPromoteToLong(ca)) {
                return ToLongEqualityChecker.INSTANCE;
            } else if (canPromoteToDouble(ct) && canPromoteToDouble(ca)) {
                return ToDoubleEqualityChecker.INSTANCE;
            } else if (canPromoteToString(ct) && canPromoteToString(ca)) {
                return ToStringEqualityChecker.INSTANCE;
            } else {
                return null;
            }
        } else if (datumKeyType instanceof CollectionLikeType && targetType instanceof CollectionLikeType) {
            GenericType datumKeyElementType = ((CollectionLikeType) datumKeyType).getElementType();
            GenericType targetElementType = ((CollectionLikeType) targetType).getElementType();
            DatumKeyEqualityChecker elementChecker = forType(datumKeyElementType, targetElementType);
            if (elementChecker == null) {
                return null;
            }
            if (datumKeyType instanceof ArrayType) {
                if (targetType instanceof ArrayType) {
                    return new Array2ArrayEqualityChecker(elementChecker, false);
                } else if (targetType instanceof CollectionType) {
                    return new Array2CollectionEqualityChecker(elementChecker, false);
                } else {
                    throw new IllegalStateException();
                }
            } else if (datumKeyType instanceof CollectionType) {
                if (targetType instanceof ArrayType) {
                    return new Array2CollectionEqualityChecker(elementChecker, true);
                } else if (targetType instanceof CollectionType) {
                    return new Collection2CollectionEqualityChecker(elementChecker, false);
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new IllegalStateException();
            }
        } else {
            return null;
        }
    }

    static boolean canPromoteToInteger(Class<?> clazz) {
        return clazz == short.class || clazz == Short.class
            || clazz == int.class || clazz == Integer.class;
    }

    static boolean canPromoteToLong(Class<?> clazz) {
        return canPromoteToInteger(clazz)
            || clazz == long.class || clazz == Long.class;
    }

    static boolean canPromoteToDouble(Class<?> clazz) {
        return canPromoteToLong(clazz)
            || clazz == float.class || clazz == Float.class
            || clazz == double.class || clazz == Double.class;
    }

    static boolean canPromoteToString(Class<?> clazz) {
        return clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) || clazz == String.class;
    }

    class DefaultEqualityChecker implements DatumKeyEqualityChecker {
        static DefaultEqualityChecker INSTANCE = new DefaultEqualityChecker();

        @Override
        public boolean keyEquals(Object datumKey, Object targetKey) {
            return Objects.equals(datumKey, targetKey);
        }
    }

    class ToLongEqualityChecker implements DatumKeyEqualityChecker {
        static ToLongEqualityChecker INSTANCE = new ToLongEqualityChecker();

        @Override
        public boolean keyEquals(Object datumKey, Object targetKey) {
            if (datumKey == null && targetKey == null) {
                return true;
            } else if (datumKey == null || targetKey == null) {
                return false;
            } else {
                return ((Number) datumKey).longValue() == ((Number) targetKey).longValue();
            }
        }
    }

    class ToDoubleEqualityChecker implements DatumKeyEqualityChecker {
        static ToDoubleEqualityChecker INSTANCE = new ToDoubleEqualityChecker();

        @Override
        public boolean keyEquals(Object datumKey, Object targetKey) {
            if (datumKey == null && targetKey == null) {
                return true;
            } else if (datumKey == null || targetKey == null) {
                return false;
            } else {
                return ((Number) datumKey).doubleValue() == ((Number) targetKey).doubleValue();
            }
        }
    }

    class ToStringEqualityChecker implements DatumKeyEqualityChecker {

        static ToStringEqualityChecker INSTANCE = new ToStringEqualityChecker();

        @Override
        public boolean keyEquals(Object datumKey, Object targetKey) {
            if (datumKey == null && targetKey == null) {
                return true;
            } else if (datumKey == null || targetKey == null) {
                return false;
            } else {
                return datumKey.toString().equals(targetKey.toString());
            }
        }
    }

    abstract class AbstractCollectionLikeEqualityChecker implements DatumKeyEqualityChecker {
        private final DatumKeyEqualityChecker elementChecker;
        private final boolean reversed;

        public AbstractCollectionLikeEqualityChecker(DatumKeyEqualityChecker elementChecker, boolean reversed) {
            this.elementChecker = elementChecker;
            this.reversed = reversed;
        }

        @Override
        public boolean keyEquals(Object datumKey, Object targetKey) {
            if (datumKey == null && targetKey == null) {
                return true;
            } else if (datumKey == null || targetKey == null) {
                return false;
            }
            if (reversed) {
                return doKeyEquals(targetKey, datumKey);
            } else {
                return doKeyEquals(datumKey, targetKey);
            }
        }

        protected boolean elementNotEquals(Object key1, Object key2) {
            if (reversed) {
                return !elementChecker.keyEquals(key2, key1);
            } else {
                return !elementChecker.keyEquals(key1, key2);
            }
        }

        protected abstract boolean doKeyEquals(Object key1, Object key2);
    }

    class Array2ArrayEqualityChecker extends AbstractCollectionLikeEqualityChecker {
        public Array2ArrayEqualityChecker(DatumKeyEqualityChecker elementChecker, boolean reversed) {
            super(elementChecker, reversed);
        }

        @Override
        protected boolean doKeyEquals(Object array1, Object array2) {
            int len1 = Array.getLength(array1);
            int len2 = Array.getLength(array2);
            if (len1 != len2) {
                return false;
            }
            for (int i = 0; i < len1; i++) {
                if (elementNotEquals(Array.get(array1, i), Array.get(array2, i))) {
                    return false;
                }
            }
            return true;
        }
    }

    class Array2CollectionEqualityChecker extends AbstractCollectionLikeEqualityChecker {
        public Array2CollectionEqualityChecker(DatumKeyEqualityChecker elementChecker, boolean reversed) {
            super(elementChecker, reversed);
        }

        @Override
        protected boolean doKeyEquals(Object array, Object collection) {
            int len = Array.getLength(array);
            Collection<?> col = (Collection<?>) collection;
            if (len != col.size()) {
                return false;
            }
            int i = 0;
            for (Object y : col) {
                Object x = Array.get(array, i++);
                if (elementNotEquals(x, y)) {
                    return false;
                }
            }
            return true;
        }
    }

    class Collection2CollectionEqualityChecker extends AbstractCollectionLikeEqualityChecker {
        public Collection2CollectionEqualityChecker(DatumKeyEqualityChecker elementChecker, boolean reversed) {
            super(elementChecker, reversed);
        }

        @Override
        protected boolean doKeyEquals(Object collection1, Object collection2) {
            Collection<?> col1 = (Collection<?>) collection1;
            Collection<?> col2 = (Collection<?>) collection2;
            if (col1.size() != col2.size()) {
                return false;
            }
            Iterator<?> iter1 = col1.iterator();
            Iterator<?> iter2 = col2.iterator();
            for (;;) {
                if (!iter1.hasNext()) {
                    return true;
                }
                if (!iter2.hasNext()) {
                    return true;
                }
                if (elementNotEquals(iter1.next(), iter2.next())) {
                    return false;
                }
            }
        }
    }
}
