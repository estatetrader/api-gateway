package com.estatetrader.objtree;

import com.estatetrader.generic.StaticType;
import com.estatetrader.generic.UnionType;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public class UnionTypeNode extends ObjectTreeNode {

    private final Map<Class<?>, ObjectTreeNode> classNodeMap = new LinkedHashMap<>();

    void addPossibleNode(ObjectTreeNode node) {
        StaticType type = (StaticType) node.getType();
        Class<?> clazz = type.getRawType();
        if (classNodeMap.put(clazz, node) != null) {
            throw new IllegalArgumentException("the raw class of concrete type " + type
                + " is duplicated for " + getType());
        }
    }

    @Override
    public UnionType getType() {
        return (UnionType) super.getType();
    }

    @Override
    protected PurgeResult purgeChildren(Deque<ObjectTreeNode> path) {
        PurgeResult purgeResult = PurgeResult.EMPTY;
        for (Map.Entry<Class<?>, ObjectTreeNode> entry : classNodeMap.entrySet()) {
            ObjectTreeNode child = entry.getValue();
            if (child instanceof EmptyNode) continue;
            PurgeResult pr = child.purge(path);
            if (pr.isEmpty()) {
                EmptyNode en = new EmptyNode();
                en.setType(child.getType());
                entry.setValue(en);
            }
            purgeResult = purgeResult.merge(pr);
        }
        return purgeResult;
    }

    @Override
    protected <R> R inspectChildren(ObjectTreeInspector<R> inspector, RouteRecorder recorder) {
        R report = null;
        for (Map.Entry<Class<?>, ObjectTreeNode> entry : classNodeMap.entrySet()) {
            report = entry.getValue().inspect(inspector, report, recorder);
        }
        return report;
    }

    @Override
    protected void childrenAccept(Object object, Object param, RouteRecorder recorder) {
        Class<?> clazz = object.getClass();
        ObjectTreeNode node = nodeOfClass(clazz);
        if (node != null) {
            // 本结点已经登记了object对象，无需在具体结点中再次登记
            node.doAccept(object, param, null, recorder);
        } else {
            // 可能短暂出现（例如在服务发版和网关reload之间），如果长期存在，则为bug
            LOGGER.warn("response type {} is not supported by union type {}", clazz, getType());
        }
    }

    private ObjectTreeNode nodeOfClass(Class<?> clazz) {
        // fast-path，在一般场景下，传入的类型与静态代码分析时得到的类型一致，在这种情况下我们需要快速查找
        ObjectTreeNode node = classNodeMap.get(clazz);
        if (node != null) {
            return node;
        }
        // fall-back，某些框架层逻辑可能会返回实际类型的一个子类型，我们需要兼容这种情况
        // 由于动态类型仅支持在静态代码分析阶段的最终类型（无子类），因此不会有多于一个类型同时兼容传入的clazz
        for (Map.Entry<Class<?>, ObjectTreeNode> entry : classNodeMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(clazz)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
