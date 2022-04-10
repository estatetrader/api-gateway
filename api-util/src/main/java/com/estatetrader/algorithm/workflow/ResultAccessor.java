package com.estatetrader.algorithm.workflow;

/**
 * execution result accessor of the workflow graph node
 */
public interface ResultAccessor {

    /**
     * check if the node has completed
     * @param nodeName the name of the node you want to check
     * @return return true if the node already completed
     */
    boolean hasCompleted(String nodeName);

    /**
     * access the result of the specified node
     *
     * @param nodeName the name of the node you want to access the result
     * @return value of the result
     * @throws Throwable if the node has something wrong (failed to start or finished), a throwable will be thrown
     */
    Object getResult(String nodeName) throws Throwable;

    /**
     * check if the the execution of the specified node has failed
     * since throw a new instance of exception is an expensive task, you are suggested to call this method to check if
     * the node has failed, before calling get() method to retrieve the result, to avoid throwing an unneeded exception.
     *
     * @param nodeName the name of the node you want to access the result
     * @return the reason (throwable) why the previous node failed, null if it did not fail
     */
    Throwable hasFailed(String nodeName);
}
