package com.estatetrader.algorithm.workflow;

import java.util.Collections;
import java.util.NoSuchElementException;

/**
 * the proxy you can use to access the workflow
 */
public interface WorkflowPipeline {

    /**
     * the context defined in the workflow
     * @return context value
     */
    Object getContext();

    /**
     * the param passed to this stage while execution
     * @return the param passed to the execution
     */
    Object getParam();

    /**
     * check if the specified node exists
     * @param name name of the node to check
     * @return true if the node exists
     */
    boolean containsNode(String name);

    /**
     * set data to current stage
     * @param data data to save
     */
    void setData(Object data);

    /**
     * add more node
     * these nodes will be called after all the dependencies completed (succeeded or failed)
     * @param name the name of the node you want to pipe
     * @param dependencies iterate all dependencies of the node you are creating (must be unique)
     * @param param param passed to the execution
     * @param batch a batch of executions (must not empty)
     */
    void node(String name, Iterable<String> dependencies, Object param, WorkflowExecution... batch);

    /**
     * add more node
     * these nodes will be called after all the dependencies completed (succeeded or failed)
     * @param name the name of the node you want to pipe
     * @param dependency the only dependency of this node
     * @param param param passed to the execution
     * @param batch a batch of executions (must not empty)
     */
    default void node(String name, String dependency, Object param, WorkflowExecution... batch) {
        node(name, Collections.singletonList(dependency), param, batch);
    }

    /**
     * add more node
     * these nodes will be called after all the dependencies completed (succeeded or failed)
     * @param name the name of the node you want to pipe
     * @param param param passed to the execution
     * @param batch a batch of executions (must not empty)
     */
    default void node(String name, Object param, WorkflowExecution... batch) {
        node(name, Collections.emptyList(), param, batch);
    }

    /**
     * add a stage to current node
     * @param param param used to execute the execution logic
     * @param batch a batch of executions (must not empty)
     */
    void stage(Object param, WorkflowExecution... batch);

    /**
     * add a stage to current node
     * @param param param used to execute the execution logic
     * @param batch a batch of executions (must not empty)
     * @param more more executions to append
     */
    default void stage(Object param, WorkflowExecution[] batch, WorkflowExecution more) {
        WorkflowExecution[] newBatch = new WorkflowExecution[batch.length + 1];
        System.arraycopy(batch, 0, newBatch, 0, batch.length);
        newBatch[batch.length] = more;
        stage(param, newBatch);
    }

    /**
     * add a stage to current node
     * @param param param used to execute the execution logic
     * @param batch a batch of executions (must not empty)
     * @param more more executions to append
     */
    default void stage(Object param, WorkflowExecution[] batch, WorkflowExecution... more) {
        WorkflowExecution[] newBatch = new WorkflowExecution[batch.length + more.length];
        System.arraycopy(batch, 0, newBatch, 0, batch.length);
        System.arraycopy(more, 0, newBatch, batch.length, more.length);
        stage(param, newBatch);
    }

    /**
     * add a stage to current node
     * @param batch a batch of executions (must not empty)
     */
    default void stage(WorkflowExecution... batch) {
        stage(null, batch);
    }

    /**
     * get the result of the previous stage in the same batch (if exists)
     * @return result of the previous stage
     * @throws Throwable exception thrown by the previous stage if error occurred
     */
    Object previousValue() throws Throwable;

    /**
     * check if the previous stage in the same batch (if exists) has failed
     * @return the exception thrown by the previous stage
     * @throws NoSuchElementException this exception will be thrown if no such previous stage found in the same batch
     */
    Throwable hasPreviousFailed() throws NoSuchElementException;

    /**
     * get the data saved to the previous stage
     * @return data saved before
     */
    Object getPreviousData();
}
