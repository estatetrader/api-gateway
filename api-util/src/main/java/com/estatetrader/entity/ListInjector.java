package com.estatetrader.entity;

import com.estatetrader.util.Lambda;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * support array and list merge (with the same primitive/reference element type)
 */
public class ListInjector extends SimpleInjector {

    @Override
    public Class<? extends Data> getDataType() {
        return Data.class;
    }

    public static class Data extends SimpleInjector.Data {

        public Data() {}

        public Data(String name, Collection<?> value) {
            super(name, value);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void batchMerge(InjectionData injection) {
            Object v1 = getValue();
            Object v2 = injection.getValue();

            if (v1 == null) {
                setValue(v2);
            } else if (v2 == null) {
                // nothing to do
            } else if (v1 instanceof Collection) {

                if (v2 instanceof Collection) {
                    setValue(Lambda.concat((Collection) v1, (Collection) v2));
                } else if (v2.getClass().isArray()) {
                    setValue(Lambda.concat((Collection) v1, Lambda.array2list(v2)));
                } else {
                    setValue(Lambda.append((Collection) v1, v2));
                }

            } else if (v1.getClass().isArray()) {

                if (v2 instanceof Collection) {
                    setValue(Lambda.concat(Lambda.array2list(v1), (Collection) v2));
                } else if (v2.getClass().isArray()) {
                    setValue(Lambda.concatArray(v1, v2));
                } else {
                    setValue(Lambda.appendArray(v1, v2));
                }

            } else if (v2 instanceof Collection) {

                setValue(Lambda.prepend(v1, (Collection) v2));

            } else if (v2.getClass().isArray()) {

                setValue(Lambda.prependArray(v1, v2));

            } else {
                setValue(Arrays.asList(v1, v2));
            }
        }

        @Override
        public Object getValue() {
            Object v = super.getValue();
            if (v == null) {
                return null;
            } else if (v instanceof Collection || v.getClass().isArray()) {
                return v;
            } else {
                return Collections.singletonList(v);
            }
        }
    }
}
