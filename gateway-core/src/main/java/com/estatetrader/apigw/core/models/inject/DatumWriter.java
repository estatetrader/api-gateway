package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.GenericField;

public interface DatumWriter {

    void write(Object container, Object value, String datumType);

    class ByField implements DatumWriter {

        private final GenericField field;

        public ByField(GenericField field) {
            field.setAccessible(true);
            this.field = field;
        }

        public GenericField getField() {
            return field;
        }

        @Override
        public void write(Object container, Object value, String datumType) {
            field.set(container, value);
        }
    }
}
