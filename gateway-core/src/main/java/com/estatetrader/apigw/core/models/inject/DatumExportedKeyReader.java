package com.estatetrader.apigw.core.models.inject;

import com.estatetrader.generic.GenericField;

public interface DatumExportedKeyReader {

    Object read(Object object, String datumType);

    class ByField implements DatumExportedKeyReader {
        private final GenericField field;

        public ByField(GenericField field) {
            this.field = field;
            field.setAccessible(true);
        }

        @Override
        public Object read(Object object, String datumType) {
            return field.get(object);
        }
    }
}
