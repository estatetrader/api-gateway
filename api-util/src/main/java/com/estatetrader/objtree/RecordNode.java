package com.estatetrader.objtree;

import com.estatetrader.generic.GenericField;
import com.estatetrader.generic.StaticType;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class RecordNode extends ObjectTreeNode {

    private final List<RecordField> fields = new ArrayList<>();

    void addField(GenericField field, ObjectTreeNode node) {
        fields.add(new RecordField(field, node));
    }

    @Override
    public StaticType getType() {
        return (StaticType) super.getType();
    }

    @Override
    protected PurgeResult purgeChildren(Deque<ObjectTreeNode> path) {
        PurgeResult purgeResult = PurgeResult.EMPTY;
        for (Iterator<RecordField> iter = fields.iterator(); iter.hasNext();) {
            PurgeResult pr = iter.next().getNode().purge(path);
            if (pr.isEmpty()) {
                iter.remove();
            }
            purgeResult = purgeResult.merge(pr);
        }
        return purgeResult;
    }

    @Override
    protected <R> R inspectChildren(ObjectTreeInspector<R> inspector, RouteRecorder recorder) {
        R report = null;
        for (RecordField field : fields) {
            report = field.getNode().inspect(inspector, report, recorder);
        }
        return report;
    }

    @Override
    protected void childrenAccept(Object object, Object param, RouteRecorder recorder) {
        for (RecordField field : fields) {
            Object value = field.readValue(object);
            NodeContext context = new NodeContext(null, null, field.getField().getName());
            field.getNode().accept(value, param, context, recorder);
        }
    }
}
