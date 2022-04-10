package com.estatetrader.generic;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class GenericArraySpliterator implements Spliterator<Object> {
    /**
     * The array, explicitly typed as Object[]. Unlike in some other
     * classes (see for example CR 6260652), we do not need to
     * screen arguments to ensure they are exactly of type Object[]
     * so long as no methods write into the array or serialize it,
     * which we ensure here by defining this class as final.
     */
    private final Object array;
    private int index;        // current index, modified on advance/split
    private final int fence;  // one past last index
    private final int characteristics;

    /**
     * Creates a spliterator covering all of the given array.
     * @param array the array, assumed to be unmodified during use
     * @param additionalCharacteristics Additional spliterator characteristics
     * of this spliterator's source or elements beyond {@code SIZED} and
     * {@code SUBSIZED} which are are always reported
     */
    public GenericArraySpliterator(Object array, int additionalCharacteristics) {
        this(array, 0, Array.getLength(array), additionalCharacteristics);
    }

    /**
     * Creates a spliterator covering the given array and range
     * @param array the array, assumed to be unmodified during use
     * @param origin the least index (inclusive) to cover
     * @param fence one past the greatest index to cover
     * @param additionalCharacteristics Additional spliterator characteristics
     * of this spliterator's source or elements beyond {@code SIZED} and
     * {@code SUBSIZED} which are are always reported
     */
    public GenericArraySpliterator(Object array, int origin, int fence, int additionalCharacteristics) {
        this.array = array;
        this.index = origin;
        this.fence = fence;
        this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
    }

    @Override
    public Spliterator<Object> trySplit() {
        int lo = index, mid = (lo + fence) >>> 1;
        return (lo >= mid)
            ? null
            : new GenericArraySpliterator(array, lo, index = mid, characteristics);
    }

    /**
     * Performs the given action for each remaining element, sequentially in
     * the current thread, until all elements have been processed or the action
     * throws an exception.  If this Spliterator is {@link #ORDERED}, actions
     * are performed in encounter order.  Exceptions thrown by the action
     * are relayed to the caller.
     *
     * @param action The action
     * @throws NullPointerException if the specified action is null
     * @implSpec The default implementation repeatedly invokes {@link #tryAdvance} until
     * it returns {@code false}.  It should be overridden whenever possible.
     */
    @Override
    public void forEachRemaining(Consumer<? super Object> action) {
        Object a; int i, hi; // hoist accesses and checks from loop
        if (action == null)
            throw new NullPointerException();
        if (Array.getLength(a = array) >= (hi = fence) &&
            (i = index) >= 0 && i < (index = hi)) {
            do { action.accept(Array.get(a, i)); } while (++i < hi);
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super Object> action) {
        if (action == null)
            throw new NullPointerException();
        if (index >= 0 && index < fence) {
            Object e = Array.get(array, index++);
            action.accept(e);
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() { return fence - index; }

    @Override
    public int characteristics() {
        return characteristics;
    }

    @Override
    public Comparator<? super Object> getComparator() {
        if (hasCharacteristics(Spliterator.SORTED))
            return null;
        throw new IllegalStateException();
    }
}
