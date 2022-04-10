package com.estatetrader.typetree;

import com.estatetrader.generic.GenericType;

import java.util.ArrayList;
import java.util.List;

public class TypeTree {
    private final GenericType rootType;
    private final TypePathPioneer pathPioneer;

    public TypeTree(GenericType rootType, TypePathPioneer pathPioneer) {
        this.rootType = rootType;
        this.pathPioneer = pathPioneer;
    }

    public <T> T visit(TypeTreeVisitor<T> visitor) {
        return doVisit(pathPioneer.start(rootType), visitor);
    }

    private <T> T doVisit(TypePath path, TypeTreeVisitor<T> visitor) {
        VisitorContext<T> context = new VisitorContextImpl<>(path, visitor);
        return visitor.visit(path, context);
    }

    private <T> T doVisitChildren(TypePath path, TypeTreeVisitor<T> visitor) {
        TypePathMove move = pathPioneer.next(path);
        if (move instanceof TypePathOneWayMove) {
            TypePath next = path.append(((TypePathOneWayMove) move).next());
            return doVisit(next, visitor);
        } else if (move instanceof TypePathMultiWayMove) {
            List<? extends TypeSpan> spans = ((TypePathMultiWayMove) move).next();
            List<T> reports = new ArrayList<>(spans.size());
            for (TypeSpan span : spans) {
                TypePath next = path.append(span);
                T report = doVisit(next, visitor);
                reports.add(report);
            }
            return visitor.report(path, (TypePathMultiWayMove) move, reports);
        } else {
            throw new IllegalStateException("unsupported move " + move);
        }
    }

    private class VisitorContextImpl<T> implements VisitorContext<T> {

        private final TypePath path;
        private final TypeTreeVisitor<T> visitor;

        public VisitorContextImpl(TypePath path, TypeTreeVisitor<T> visitor) {
            this.path = path;
            this.visitor = visitor;
        }

        @Override
        public T visitChildren() {
            return doVisitChildren(path, visitor);
        }

        @Override
        public T visit(GenericType newType) {
            return doVisit(path.append(pathPioneer.jump(path, newType)), visitor);
        }

        @Override
        public RecordTypeResolver typeResolver() {
            return pathPioneer.typeResolver();
        }
    }
}
