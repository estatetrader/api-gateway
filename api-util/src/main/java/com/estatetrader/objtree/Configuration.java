package com.estatetrader.objtree;

import com.estatetrader.typetree.DefaultRecordTypeResolver;
import com.estatetrader.typetree.DefaultTypePathPioneer;
import com.estatetrader.typetree.RecordTypeResolver;
import com.estatetrader.typetree.TypePathPioneer;

import java.util.Objects;

class Configuration {
    private TypePathPioneer pathPioneer = new DefaultTypePathPioneer(new DefaultRecordTypeResolver());
    private NodeHandlerProvider<?, ?> handlerProvider = (p, r) -> null;
    private RouteRecorderFactory recorderFactory = RouteRecorder.VisitOnlyOnce::new;

    public RecordTypeResolver getTypeResolver() {
        return pathPioneer.typeResolver();
    }

    public TypePathPioneer getPathPioneer() {
        return pathPioneer;
    }

    public void setPathPioneer(TypePathPioneer pathPioneer) {
        this.pathPioneer = pathPioneer;
    }

    public NodeHandlerProvider<?, ?> getHandlerProvider() {
        return handlerProvider;
    }

    public void setHandlerProvider(NodeHandlerProvider<?, ?> handlerProvider) {
        this.handlerProvider = handlerProvider;
    }

    public RouteRecorderFactory getRecorderFactory() {
        return recorderFactory;
    }

    public void setRecorderFactory(RouteRecorderFactory recorderFactory) {
        this.recorderFactory = Objects.requireNonNull(recorderFactory);
    }
}
