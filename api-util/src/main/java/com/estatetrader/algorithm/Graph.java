package com.estatetrader.algorithm;

import com.estatetrader.util.Lambda;

import java.util.*;
import java.util.function.Consumer;

public class Graph<K, V> {
    private final Map<K, Node<K, V>> nodes = new HashMap<>();

    public Graph<K, V> node(K name, V data) {
        if (nodes.containsKey(name)) {
            nodes.get(name).data = data;
        } else {
            nodes.put(name, new Node<>(name, data));
        }
        return this;
    }

    public boolean containsNode(K name) {
        return nodes.containsKey(name);
    }

    public V getAt(K name) {
        Node<K, V> node = nodes.get(name);
        return node == null ? null : node.data;
    }

    public V putAt(K name, V data) {
        if (nodes.containsKey(name)) {
            nodes.get(name).data = data;
            return data;
        } else {
            throw new IllegalArgumentException(String.valueOf(name));
        }
    }

    public Graph<K, V> arc(K from, K to) {
        Node<K, V> fromNode;
        Node<K, V> toNode;
        if (nodes.containsKey(from)) {
            fromNode = nodes.get(from);
        } else {
            fromNode = new Node<>(from, null);
            nodes.put(from, fromNode);
        }

        if (nodes.containsKey(to)) {
            toNode = nodes.get(to);
        } else {
            toNode = new Node<>(to, null);
            nodes.put(to, toNode);
        }

        if (!fromNode.next.contains(toNode)) fromNode.next.add(toNode);
        if (!toNode.prev.contains(fromNode)) toNode.prev.add(fromNode);
        return this;
    }

    public Graph<K, V> removeNode(K name) {
        Node<K, V> n = nodes.get(name);
        if (n != null) {
            for (Node<K, V> m : n.prev) {
                m.next.remove(n);
            }
            for (Node<K, V> m : n.next) {
                m.prev.remove(n);
            }
            nodes.remove(name);
        }
        return this;
    }

    public List<K> prev(K name) {
        if (!nodes.containsKey(name)) throw new IllegalArgumentException(String.valueOf(name));
        return Lambda.map(nodes.get(name).prev, n -> n.name);
    }

    public List<K> next(K name) {
        if (!nodes.containsKey(name)) throw new IllegalArgumentException(String.valueOf(name));
        return Lambda.map(nodes.get(name).next, n -> n.name);
    }

    public Graph removeArc(K from, K to) {
        Node fromNode;
        Node toNode;
        if (nodes.containsKey(from) && nodes.containsKey(to)) {
            fromNode = nodes.get(from);
            toNode = nodes.get(to);
            fromNode.next.remove(toNode);
            toNode.prev.remove(fromNode);
        }
        return this;
    }

    private Graph<K, V> fork() {
        Graph<K, V> g = new Graph<>();
        for (Node<K, V> n : nodes.values()) {
            g.node(n.name, n.data);
        }
        for (Node<K, V> n : nodes.values()) {
            for (Node<K, V> m : n.next) {
                g.arc(n.name, m.name);
            }
        }
        return g;
    }

    public List<K> topology() {
        Graph<K, V> g = fork();
        List<Node<K, V>> open = new ArrayList<>(g.nodes.values());
        List<K> result = new ArrayList<>();
        while (!open.isEmpty()) {
            Node<K, V> node = null;
            for (Node<K, V> n : open) {
                if (n.prev.isEmpty()) {
                    node = n;
                    break;
                }
            }
            if (node == null) {
                throw new IllegalStateException("circle find in graph " + Lambda.toString(Lambda.map(open, n -> n.name)));
            }
            for (Node<K, V> n : node.next) {
                n.prev.remove(node);
            }
            open.remove(node);
            result.add(node.name);
        }
        return result;
    }

    public Graph<K, V> reverse() {
        Graph<K, V> g = new Graph<>();
        for (Node<K, V> n : nodes.values()) {
            g.node(n.name, n.data);
            for (Node<K, V> m : n.next) {
                g.arc(m.name, n.name);
            }
        }

        return g;
    }

    /**
     * travel to all directly / indirectly previous nodes of those nodes which are the directly or indirectly next nodes of the starter node.
     * @param starter the starter node
     * @param visitor apply this lambda to all the visited nodes
     */
    public void travelForest(K starter, Consumer<V> visitor) {
        Set<K> close = new HashSet<>();
        travel(nodes.get(starter), false, new HashSet<>(), n -> travel(n, true, close, m -> visitor.accept(m.data)));
    }

    public void travelForward(K starter, Consumer<V> visitor) {
        travel(nodes.get(starter), false, new HashSet<>(), n -> visitor.accept(n.data));
    }

    public void travelBackward(K starter, Consumer<V> visitor) {
        travel(nodes.get(starter), true, new HashSet<>(), n -> visitor.accept(n.data));
    }

    private void travel(Node<K, V> starter, boolean backward, Set<K> close, Consumer<Node<K, V>> visitor) {
        Deque<Node<K, V>> open = new ArrayDeque<>();
        open.push(starter);
        while (!open.isEmpty()) {
            Node<K, V> node = open.pop();
            visitor.accept(node);
            close.add(node.name);
            List<Node<K, V>> list = backward ? node.prev : node.next;
            for (int i = list.size()-1; i >= 0; i--) {
                Node<K, V> n = list.get(i);
                if (!close.contains(n.name)) {
                    open.add(n);
                }
            }
        }
    }

    @Override
    public String toString() {
        return Lambda.join("\n", nodes.values());
    }

    private static class Node<K, V> {
        private final K name;
        private V data;
        private final List<Node<K, V>> prev = new ArrayList<>();
        private final List<Node<K, V>> next = new ArrayList<>();

        private Node(K name, V data) {
            this.name = name;
            this.data = data;
        }

        @Override
        public String toString() {
            return Lambda.toString(Lambda.map(prev, n -> n.name)) + " -> (" + name + ") -> " + Lambda.toString(Lambda.map(next, n->n.name));
        }
    }
}