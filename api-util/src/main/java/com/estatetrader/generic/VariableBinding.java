package com.estatetrader.generic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VariableBinding {

    public static final VariableBinding EMPTY = new VariableBinding(Collections.emptyMap());

    private final Map<TypeVariable, GenericType> map;

    public VariableBinding(Map<TypeVariable, GenericType> map) {
        this.map = Collections.unmodifiableMap(map);
    }

    public VariableBinding(TypeVariable var, GenericType type) {
        this.map = Collections.singletonMap(var, type);
    }

    public GenericType get(TypeVariable var) {
        return map.get(var);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public VariableBinding add(TypeVariable var, GenericType type) {
        if (this.map.isEmpty()) {
            return new VariableBinding(var, type);
        } else {
            return doMerge(Collections.singletonMap(var, type));
        }
    }

    public VariableBinding merge(VariableBinding another) {
        if (this.map.isEmpty()) {
            return another;
        } else if (another.isEmpty()) {
            return this;
        } else if (another.map.size() <= this.map.size()) {
            return doMerge(another.map);
        } else {
            // performance tuning
            return another.doMerge(this.map);
        }
    }

    private VariableBinding doMerge(Map<TypeVariable, GenericType> anotherMap) {
        Map<TypeVariable, GenericType> binding = null;
        for (Map.Entry<TypeVariable, GenericType> entry : anotherMap.entrySet()) {
            TypeVariable var = entry.getKey();
            GenericType t1 = this.map.get(var);
            GenericType t2 = entry.getValue();
            GenericType t;
            if (t1 == null) {
                t = t2;
            } else {
                GenericType intersect = t1.intersect(t2);
                if (intersect == null) {
                    return null;
                } else if (intersect.equals(t1)) {
                    continue;
                } else {
                    t = intersect;
                }
            }
            if (binding == null) {
                binding = new HashMap<>(this.map);
            }
            binding.put(var, t);
        }
        if (binding == null) {
            return this;
        } else {
            return new VariableBinding(binding);
        }
    }
}
