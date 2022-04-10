package com.estatetrader.objtree;

import com.estatetrader.generic.GenericType;
import com.estatetrader.generic.StaticType;
import com.estatetrader.typetree.*;
import com.estatetrader.typetree.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ObjectTreeFactory {

    public static <P> ObjectTree<P> create(GenericType rootType, Configuration cfg) {
        GenericType actualRootType = cfg.getTypeResolver().concreteType(rootType);
        ObjectTreeNode rootNode = new NodeProducer(cfg).produce(actualRootType);
        if (rootNode == null) {
            return null;
        }
        PurgeResult rootPurgeResult = rootNode.purge(new LinkedList<>());
        if (rootPurgeResult.isEmpty()) {
            return null;
        }
        return new ObjectTreeImpl<>(rootNode, cfg.getRecorderFactory());
    }

    private static class NodeProducer {
        private final Configuration cfg;
        private final Map<NodeKey, ObjectTreeNode> parsedNodes = new HashMap<>();

        public NodeProducer(Configuration cfg) {
            this.cfg = cfg;
        }

        public ObjectTreeNode produce(GenericType rootType) {
            return parseType(cfg.getPathPioneer().start(rootType));
        }

        private ObjectTreeNode parseType(TypePath path) {
            NodeKey key = nodeKey(path);
            ObjectTreeNode node = parsedNodes.get(key);
            if (node == null) {
                node = doParseType(key, path);
                node.setType(key.span.getEndType());
                node.setHandler(key.handler);
                parsedNodes.put(key, node);
            }
            return node;
        }

        private NodeKey nodeKey(TypePath path) {
            NodeHandler<Object, Object> handler = determineHandler(path);
            return new NodeKey(path.current(), handler);
        }

        private NodeHandler<Object, Object> determineHandler(TypePath path) {
            //noinspection unchecked
            return (NodeHandler<Object, Object>) cfg.getHandlerProvider().handlerFor(path, cfg.getTypeResolver());
        }

        private <N extends ObjectTreeNode> N createNode(NodeKey key, Class<N> clazz, Consumer<N> init) {
            N node;
            try {
                node = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("bug");
            }

            node.setType(key.span.getEndType());
            node.setHandler(key.handler);
            parsedNodes.put(key, node);

            init.accept(node);

            return node;
        }

        private ObjectTreeNode doParseType(NodeKey key, TypePath path) {
            TypePathMove move = cfg.getPathPioneer().next(path);

            if (move instanceof ArrayTypePathMove) {
                return createNode(key, CollectionNode.class, node -> {
                    ArraySpan eleSpan = ((ArrayTypePathMove) move).next();
                    node.setElement(parseType(path.append(eleSpan)));
                });
            } else if (move instanceof CollectionTypePathMove) {
                return createNode(key, CollectionNode.class, node -> {
                    CollectionSpan eleSpan = ((CollectionTypePathMove) move).next();
                    node.setElement(parseType(path.append(eleSpan)));
                });
            } else if (move instanceof MapTypeMove) {
                return createNode(key, MapNode.class, node -> {
                    MapSpan valueSpan = ((MapTypeMove) move).next();
                    node.setValueNode(parseType(path.append(valueSpan)));
                });
            } else if (move instanceof UnionTypeMove) {
                return createNode(key, UnionTypeNode.class, node -> {
                    for (PossibleSpan possibleSpan : ((UnionTypeMove) move).possibleSpans()) {
                        ObjectTreeNode concreteNode = parseType(path.append(possibleSpan));
                        node.addPossibleNode(concreteNode);
                    }
                });
            } else if (move instanceof RecordTypeMove) {
                return createNode(key, RecordNode.class, node -> {
                    for (FieldSpan fieldSpan : ((RecordTypeMove) move).fieldSpans()) {
                        node.addField(fieldSpan.getField(), parseType(path.append(fieldSpan)));
                    }
                });
            } else if (path.endType() instanceof StaticType && move instanceof TypePathEndMove) {
                return createNode(key, LeafNode.class, node -> {});
            } else if (key.handler == null) {
                return createNode(key, EmptyNode.class, node -> {});
            } else {
                throw new IllegalArgumentException("unsupported type path " + path);
            }
        }
    }

    private static class NodeKey {
        final TypeSpan span;
        final NodeHandler<Object, Object> handler;

        public NodeKey(TypeSpan span, NodeHandler<Object, Object> handler) {
            this.span = span;
            this.handler = handler;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof NodeKey)) return false;
            NodeKey that = (NodeKey) object;
            return span.equals(that.span)
                && Objects.equals(handler, that.handler);
        }

        @Override
        public int hashCode() {
            return Objects.hash(span, handler);
        }
    }
}
