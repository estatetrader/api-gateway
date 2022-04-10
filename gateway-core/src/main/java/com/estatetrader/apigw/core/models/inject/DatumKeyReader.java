package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.GenericField;

public interface DatumKeyReader {

    Object read(Object datum);

    class ByField implements DatumKeyReader {

        private final GenericField field;

        public ByField(GenericField field) {
            this.field = field;
            field.setAccessible(true);
        }

        @Override
        public Object read(Object datum) {
            return field.get(datum);
        }
    }
}
