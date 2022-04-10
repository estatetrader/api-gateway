package com.estatetrader.objtree;

import com.estatetrader.gateway.StructTypePathPioneer;
import com.estatetrader.typetree.FieldSpan;
import com.estatetrader.typetree.TypePathPioneer;
import com.estatetrader.typetree.TypeSpan;
import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.GenericTypes;

import java.lang.annotation.Annotation;

public class ObjectTreeBuilder<P> {

    private final Configuration cfg = new Configuration();
    private final GenericType rootType;

    public ObjectTreeBuilder(Class<?> rootType) {
        this.rootType = GenericTypes.of(rootType);
    }

    public ObjectTreeBuilder(GenericType rootType) {
        this.rootType = rootType;
    }

    public <V> ObjectTreeBuilder<P> adviceAnnotatedField(Class<? extends Annotation> annotationClass,
                                                         NodeHandler<V, P> handler) {
        NodePointcut pointcut = path -> {
            TypeSpan span = path.current();
            if (span instanceof FieldSpan) {
                return ((FieldSpan) span).getField().getAnnotation(annotationClass) != null;
            } else {
                return false;
            }
        };
        return advice(pointcut, handler);
    }

    public <V> ObjectTreeBuilder<P> adviceRecord(Class<V> recordType, NodeHandler<V, P> handler) {
        NodePointcut pointcut = path -> GenericTypes.of(recordType).isAssignableFrom(path.endType());
        return advice(pointcut, handler);
    }

    public <V> ObjectTreeBuilder<P> advice(NodePointcut pointcut, NodeHandler<V, P> handler) {
        return handlerProvider(new PointcutNodeHandlerProvider<>(pointcut, handler));
    }

    public <K, V> ObjectTreeBuilder<P> handlerFactory(NodeHandlerFactory<K, V, P> handlerFactory) {
        return handlerProvider(new CachedHandlerProvider<>(handlerFactory));
    }

    public <V> ObjectTreeBuilder<P> handlerProvider(NodeHandlerProvider<V, P> handlerProvider) {
        cfg.setHandlerProvider(new CombinedNodeHandlerProvider(handlerProvider, cfg.getHandlerProvider()));
        return this;
    }

    public ObjectTreeBuilder<P> pathPioneer(TypePathPioneer pathPioneer) {
        cfg.setPathPioneer(pathPioneer);
        return this;
    }

    public ObjectTreeBuilder<P> structPathPioneer() {
        cfg.setPathPioneer(new StructTypePathPioneer());
        return this;
    }

    public ObjectTreeBuilder<P> routeRecorder(RouteRecorderFactory recorderCreator) {
        cfg.setRecorderFactory(recorderCreator);
        return this;
    }

    public ObjectTreeBuilder<P> visitOnlyOnce() {
        cfg.setRecorderFactory(RouteRecorder.VisitOnlyOnce::new);
        return this;
    }

    public ObjectTreeBuilder<P> visitNonRecursively() {
        cfg.setRecorderFactory(RouteRecorder.VisitNonRecursively::new);
        return this;
    }

    public ObjectTree<P> build() {
        return ObjectTreeFactory.create(rootType, cfg);
    }
}
