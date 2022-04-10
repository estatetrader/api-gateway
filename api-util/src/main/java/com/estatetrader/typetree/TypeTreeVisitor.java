package com.estatetrader.typetree;

import java.util.List;

public interface TypeTreeVisitor<T> {
    T visit(TypePath path, VisitorContext<T> context);
    T report(TypePath path, TypePathMultiWayMove move, List<T> reports);
}
