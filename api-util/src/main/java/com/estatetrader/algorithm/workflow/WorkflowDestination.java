package com.estatetrader.algorithm.workflow;

import java.util.List;

/**
 * a node representing the execution logic of a node in the workflow
 */
@FunctionalInterface
public interface WorkflowDestination {
    /**
     * finish the workflow
     * @param originFailed the exception thrown by origin node, null if no error occurred
     * @param nodesFailed exceptions occurred of all failed nodes (excludes origin node)
     * @param result the result accessor of the previous node of this node
     * @param context workflow context
     */
    void finish(Throwable originFailed, List<Throwable> nodesFailed, ResultAccessor result, Object context);
}
