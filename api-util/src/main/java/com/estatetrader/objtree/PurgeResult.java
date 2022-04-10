package com.estatetrader.objtree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PurgeResult {

    public static final PurgeResult EMPTY = new PurgeResult(false);
    public static final PurgeResult NOT_EMPTY = new PurgeResult(true);

    private final Boolean notEmpty;
    private final Set<ObjectTreeNode> dependsOn;

    public PurgeResult(boolean notEmpty) {
        this.notEmpty = notEmpty;
        this.dependsOn = null;
    }

    public PurgeResult(ObjectTreeNode dependsOn) {
        this.notEmpty = null;
        this.dependsOn = Collections.singleton(Objects.requireNonNull(dependsOn));
    }

    private PurgeResult(Set<ObjectTreeNode> dependsOn) {
        this.notEmpty = null;
        this.dependsOn = dependsOn;
    }

    public boolean isEmpty() {
        return notEmpty != null && !notEmpty;
    }

    public PurgeResult upward(ObjectTreeNode currentNode) {
        if (dependsOn != null && dependsOn.contains(currentNode)) {
            if (dependsOn.size() == 1) {
                return EMPTY;
            } else {
                Set<ObjectTreeNode> newDependsOn = new HashSet<>(dependsOn);
                newDependsOn.remove(currentNode);
                return new PurgeResult(newDependsOn);
            }
        } else {
            return this;
        }
    }

    public PurgeResult merge(PurgeResult another) {
        /*
         * merge逻辑如下：
         * 1. 两者有一个为非空，则结果为非空
         * 2. 两者均为空，则结果为空
         * 3. 任意与空的结果为其本身
         * 4. 两者均为依赖，则结果为依赖的并集
         */
        if (notEmpty != null) {
            if (notEmpty) {
                return NOT_EMPTY;
            } else if (another.notEmpty != null) {
                if (another.notEmpty) {
                    return NOT_EMPTY;
                } else {
                    return EMPTY;
                }
            } else {
                return another;
            }
        } else if (another.notEmpty != null) {
            if (another.notEmpty) {
                return NOT_EMPTY;
            } else {
                return this;
            }
        } else if (dependsOn.equals(another.dependsOn)) {
            return this;
        } else if (dependsOn.size() >= another.dependsOn.size() && dependsOn.containsAll(another.dependsOn)) {
            return this;
        } else if (another.dependsOn.size() >= dependsOn.size() && another.dependsOn.containsAll(dependsOn)) {
            return another;
        } else {
            Set<ObjectTreeNode> newDependsOn = new HashSet<>(dependsOn);
            newDependsOn.addAll(another.dependsOn);
            return new PurgeResult(newDependsOn);
        }
    }
}
