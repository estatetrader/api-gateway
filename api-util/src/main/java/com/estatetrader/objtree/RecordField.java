package com.estatetrader.objtree;

import com.estatetrader.generic.GenericField;

public class RecordField {
    private final GenericField field;
    private final ObjectTreeNode node;

    RecordField(GenericField field, ObjectTreeNode node) {
        this.field = field;
        this.node = node;
    }

    GenericField getField() {
        return field;
    }

    ObjectTreeNode getNode() {
        return node;
    }

    public Object readValue(Object object) {
        return field.get(object);
    }

    public void writeValue(Object object, Object value) {
        field.set(object, value);
    }
}
