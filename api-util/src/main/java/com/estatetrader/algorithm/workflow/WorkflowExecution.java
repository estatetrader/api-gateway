package com.estatetrader.algorithm.workflow;

import com.estatetrader.annotation.NotThreadSafe;

/**
 * a node representing the execution logic of a node in the workflow
 * NOTE: only one thread should execute this execution, please do not create new threads inside this execution body
 * which accesses those parameters passed by the start() method
 */
@NotThreadSafe
@FunctionalInterface
public interface WorkflowExecution {
    /**
     * start the execution of the node
     * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
     * @throws Throwable exception occurred while starting this node
     */
    ExecutionResult start(WorkflowPipeline pipeline) throws Throwable;

    /**
     * whether this execution accepts the failure of its previous stage in the same batch
     *
     */
    default boolean acceptPreviousFailure() {
        return false;
    }

    /**
     * whether this execution inherits the failure of its previous stage in the same batch
     * @return return true if you do not want to wrap the failure of previous stage
     */
    default boolean inheritPreviousFailure() {
        return false;
    }

    /**
     * sync mode of this execution
     */
    @FunctionalInterface
    interface Sync extends WorkflowExecution {
        /**
         * start the execution of the node
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         * @throws Throwable exception occurred while starting this node
         */
        default ExecutionResult start(WorkflowPipeline pipeline) throws Throwable {
            run(pipeline);
            return null;
        }

        /**
         * execute the execution of the node
         * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
         * @throws Throwable exception occurred while starting this node
         */
        void run(WorkflowPipeline pipeline) throws Throwable;

        interface Resource extends Sync {
            /**
             * before the body of the execution of the node
             *
             * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
             * @throws Throwable exception occurred while starting this node
             */
            void setup(WorkflowPipeline pipeline) throws Throwable;

            /**
             * execute the execution of the node
             *
             * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
             * @throws Throwable exception occurred while starting this node
             */
            @Override
            default void run(WorkflowPipeline pipeline) throws Throwable {
                setup(pipeline);
                try {
                    body(pipeline);
                } finally {
                    cleanup(pipeline);
                }
            }

            /**
             * the body the execution of the node
             * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
             * @throws Throwable exception occurred while starting this node
             */
            void body(WorkflowPipeline pipeline) throws Throwable;

            /**
             * after the body the execution of the node, will be executed whether the run success or fail
             *
             * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
             * @throws Throwable exception occurred while starting this node
             */
            void cleanup(WorkflowPipeline pipeline) throws Throwable;
        }

        interface Simple extends Sync {
            /**
             * execute the execution of the node
             * @param pipeline a pipeline instance which allows you to add successors (nodes which depend on this node) of this node
             * @throws Throwable exception occurred while starting this node
             */
            default void run(WorkflowPipeline pipeline) throws Throwable {
                run();
            }

            /**
             * execute the execution of the node
             * @throws Throwable exception occurred while running this node
             */
            void run() throws Throwable;
        }
    }
}
