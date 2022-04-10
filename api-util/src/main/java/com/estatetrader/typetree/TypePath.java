package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class TypePath implements Iterable<TypeSpan> {
    private final TypeSpan current;
    private final TypePath parent;

    public TypePath(TypeSpan current, TypePath parent) {
        this.current = current;
        this.parent = parent;
    }

    public TypeSpan current() {
        return current;
    }

    public GenericType endType() {
        return current.getEndType();
    }

    public TypePath parent() {
        return parent;
    }

    public TypePath parent(Predicate<TypeSpan> predicate) {
        for (TypePath p = this; p != null; p = p.parent) {
            if (predicate.test(p.current)) {
                return p;
            }
        }
        return null;
    }

    public <T extends TypeSpan> T spanOf(Class<T> spanType) {
        for (TypePath p = this; p != null; p = p.parent) {
            if (spanType.isInstance(p.current)) {
                //noinspection unchecked
                return (T) p.current;
            }
        }
        return null;
    }

    public boolean inCircle() {
        for (TypePath p = parent; p != null; p = p.parent) {
            if (p.current.equals(current)) {
                return true;
            }
        }
        return false;
    }

    public <M extends TypeSpanMetadata> M metadataOf(Class<M> metadataType) {
        for (TypePath p = this; p != null; p = p.parent) {
            TypeSpanMetadata metadata = p.current.getMetadata();
            if (metadataType.isInstance(metadata)) {
                //noinspection unchecked
                return (M) metadata;
            }
        }
        return null;
    }

    public TypePath append(TypeSpan next) {
        return new TypePath(next, this);
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<TypeSpan> iterator() {
        return new Iterator<TypeSpan>() {

            TypePath p = TypePath.this;

            @Override
            public boolean hasNext() {
                return p != null;
            }

            @Override
            public TypeSpan next() {
                if (p != null) {
                    TypeSpan item = p.current;
                    p = p.parent;
                    return item;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public String toString() {
        return (parent != null ? parent + "->" : "") + current;
    }
}
