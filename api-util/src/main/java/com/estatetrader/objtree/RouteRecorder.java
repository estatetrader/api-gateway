package com.estatetrader.objtree;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public interface RouteRecorder {
    boolean enter(Object object);

    void exit();

    class VisitOnlyOnce implements RouteRecorder {
        private static final Object PRESENT = new Object();
        private final Map<Object, Object> history = new IdentityHashMap<>();

        @Override
        public boolean enter(Object object) {
            return history.putIfAbsent(object, PRESENT) == null;
        }

        @Override
        public void exit() {
        }
    }

    class VisitNonRecursively implements RouteRecorder {
        private final List<Object> path = new ArrayList<>();

        @Override
        public boolean enter(Object object) {
            for (Object x : path) {
                if (x == object) {
                    return false;
                }
            }
            path.add(object);
            return true;
        }

        @Override
        public void exit() {
            path.remove(path.size() - 1);
        }
    }
}
