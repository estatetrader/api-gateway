package com.estatetrader.algorithm.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 工作流图
 */
public class WorkflowGraph {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowGraph.class);

    private static final ScheduledExecutorService cleanupWorker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "workflow-cleanup-worker");
        t.setDaemon(true);
        return t;
    });

    public static void shutdown() {
        cleanupWorker.shutdown();
    }

    private static final ConcurrentHashMap<WorkflowGraph, WorkflowGraph> GRAPHS = new ConcurrentHashMap<>();

    public static final String ORIGIN_NODE_NAME = "@";

    private final Map<String, Node> nodes = new HashMap<>();
    private final WorkflowDestination destination;
    private final Object context;
    /**
     * at which time, this graph should be finished by force and removed
     */
    private long expire;
    private CompletableFuture<Void> completeFuture;

    /**
     * if set to true, the workflow has completed all its tasks
     */
    private AtomicBoolean completed = new AtomicBoolean();

    public WorkflowGraph(WorkflowExecution origin, WorkflowDestination destination) {
        this(origin, destination, null);
    }

    public WorkflowGraph(WorkflowExecution origin, WorkflowDestination destination, Object context) {
        this(origin, destination, context, null);
    }

    public WorkflowGraph(WorkflowExecution origin, WorkflowDestination destination, Object context, Object originParam) {
        nodes.put(ORIGIN_NODE_NAME, new Node(ORIGIN_NODE_NAME, originParam, origin));
        this.destination = destination;
        this.context = context;
    }

    static {
        cleanupWorker.scheduleAtFixedRate(WorkflowGraph::cleanup, 30, 30, TimeUnit.MINUTES);
    }

    private static void cleanup() {
        try {
            if (GRAPHS.isEmpty()) {
                return;
            }

            logger.info("graphs.size = {}", GRAPHS.size());
            long now = System.currentTimeMillis();
            Iterator<WorkflowGraph> graphs = GRAPHS.keySet().iterator();
            int count = 0;
            while (graphs.hasNext()) {
                WorkflowGraph graph;
                try {
                    graph = graphs.next();
                } catch (NoSuchElementException e) {
                    continue;
                }

                if (now <= graph.expire) {
                    continue;
                }

                graph.performTimeoutCheck(now);
                if (!graph.isCompleted()) {
                    // next time we will retry it
                    continue;
                }

                count++;
                try {
                    graphs.remove();
                } catch (NoSuchElementException e) {
                    // ignore
                }
            }
            if (count > 0) {
                logger.info("cleaned up {} timed out workflow graph[s]", count);
            }
        } catch (Throwable t) {
            logger.warn("error occurred in workflow clean up loop", t);
        }
    }

    public CompletableFuture<Void> start(long timeout) {
        if (expire != 0 || completeFuture != null) {
            throw new IllegalStateException("the graph has already started");
        }
        expire = System.currentTimeMillis() + timeout;
        completeFuture = new CompletableFuture<>();
        GRAPHS.put(this, this);

        Node node;
        synchronized (nodes) {
            node = nodes.get(ORIGIN_NODE_NAME);
        }
        node.start();

        return completeFuture;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    private void performTimeoutCheck(long checkTime) {
        if (isCompleted()) {
            return;
        }

        // try to force fail all uncompleted nodes (but started)
        // these timeout may be caused by too late callback
        List<Node> nodesToFail = new ArrayList<>(1);
        synchronized (nodes) {
            for (Node node : nodes.values()) {
                if (node.isStarted() && !node.isCompleted()) {
                    nodesToFail.add(node);
                }
            }
        }

        Throwable throwable = new TimeoutException("graph has timed out " + (checkTime - expire) + "ms");
        if (!nodesToFail.isEmpty()) {
            for (Node node : nodesToFail) {
                node.forceFail(throwable);
            }
            return;
        }

        logger.error("there are some nodes which has not started but all started nodes have completed. bugs of workflow graph algorithm may be found");
    }

    private Node getNode(String name) {
        Node node;
        synchronized (nodes) {
            node = nodes.get(name);
        }

        if (node == null) {
            throw new IllegalArgumentException("invalid node name " + name);
        }
        return node;
    }

    /**
     * check if the specified node exists
     *
     * @param name name of the node to check
     * @return true if the node exists
     */
    private boolean containsNode(String name) {
        synchronized (nodes) {
            return nodes.containsKey(name);
        }
    }

    /**
     * pipe more node
     * these nodes will be called after all the dependencies completed (succeeded or failed)
     *
     * @param name         the name of the node you want to pipe
     * @param fromNode from which node this new node created
     * @param dependencies iterate all dependencies of the node you are creating (must be unique)
     * @param param param passed to execution
     * @param executions    the node itself (the execution logic)
     */
    private void pipeNode(String name, Node fromNode, Iterable<String> dependencies, Object param, WorkflowExecution[] executions) {
        Node node = new Node(name, param, executions);

        synchronized (nodes) {
            if (nodes.containsKey(name)) {
                throw new IllegalArgumentException("node " + name + " is already defined");
            }
            nodes.put(name, node);

            node.prev.addLast(fromNode);
            fromNode.next.addLast(node);

            for (String dep : dependencies) {
                Node n = nodes.get(dep);
                if (n == null) {
                    throw new IllegalArgumentException("invalid dependency name " + dep);
                }
                n.next.addLast(node);
                node.prev.addLast(n);
            }
        }

        if (performDependencyCheck(node)) {
            node.start();
        }
    }

    private void onNodeComplete(Node node) {
        List<Node> nodesNeedStart = null;
        boolean shouldExecuteDestination;
        synchronized (nodes) {
            if (node.throwable == null) {
                // collect all nodes that can start
                for (Node n : node.next) {
                    if (!n.isStarted() && !n.isCompleted() && performDependencyCheck(n)) {
                        if (nodesNeedStart == null) {
                            nodesNeedStart = new LinkedList<>();
                        }
                        nodesNeedStart.add(n);
                    }
                }
            } else {
                // let all its next fail
                for (Node n : node.next) {
                    n.dependencyFail(node.name, node.throwable);
                }
            }

            shouldExecuteDestination = node.next.isEmpty() && shouldExecuteDestination();
        }

        if (shouldExecuteDestination) {
            // only the first thread which set the completed to true should execute destination
            if (!completed.getAndSet(true)) {
                executeDestination();
            }
        }

        if (nodesNeedStart != null) {
            for (Node n : nodesNeedStart) {
                n.start();
            }
        }
    }

    /**
     * check if we should execute destination
     * must be protected by nodes
     * @return return true if we need
     */
    private boolean shouldExecuteDestination() {
        if (completed.get()) {
            return false;
        }
        for (Node n : nodes.values()) {
            if (!n.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    /**
     * check if all dependencies have completed and set dependency failure if at least one dependency failed
     * @param n node n
     * @return return true if all dependencies have completed
     */
    private boolean performDependencyCheck(Node n) {
        for (Node m : n.prev) {
            if (!m.isCompleted() || m.throwable != null) {
                if (m.throwable != null) {
                    n.dependencyFail(m.name, m.throwable);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * must only be executed once, ensured by caller
     */
    private void executeDestination() {
        List<Throwable> exceptions = null;
        Node origin = nodes.get(ORIGIN_NODE_NAME);
        for (Node n : nodes.values()) {
            // since all have completed, there will be no change any more.
            if (origin != n && n.throwable != null) {
                if (n.throwable instanceof DependentFailureException) {
                    continue; // skip this kind of exception
                }
                if (exceptions == null) {
                    exceptions = new ArrayList<>(1);
                }
                exceptions.add(n.throwable);
            }
        }

        List<Throwable> finalExceptions = exceptions != null ? exceptions : Collections.emptyList();

        try {
            destination.finish(origin.throwable, finalExceptions, resultAccessor, context);
        } catch (Throwable throwable) {
            logger.error("failed to finish workflow: " + throwable.getMessage(), throwable);
        }

        onFinished();
    }

    private void onFinished() {
        completeFuture.complete(null);
        GRAPHS.remove(this);
    }

    private final ResultAccessor resultAccessor = new ResultAccessor() {
        /**
         * check if the node has completed
         *
         * @param nodeName the name of the node you want to check
         * @return return true if the node already completed
         */
        @Override
        public boolean hasCompleted(String nodeName) {
            return getNode(nodeName).isCompleted();
        }

        /**
         * access the result of the specified node
         *
         * @param nodeName the name of the node you want to access the result
         * @return value of the result
         * @throws Throwable if the node has something wrong (failed to start or finished), a throwable will be thrown
         */
        @Override
        public Object getResult(String nodeName) throws Throwable {
            Node node = getNode(nodeName);
            if (!node.isCompleted()) {
                throw new IllegalStateException("node " + nodeName + " has not completed");
            }
            if (node.throwable != null) {
                throw node.throwable;
            }
            return node.result;
        }

        /**
         * check if the the execution of the specified node has failed
         * since throw a new instance of exception is an expensive task, you are suggested to call this method to check if
         * the node has failed, before calling get() method to retrieve the result, to avoid throwing an unneeded exception.
         *
         * @param nodeName the name of the node you want to access the result
         * @return the reason (throwable) why the previous node failed, null if it did not fail
         */
        @Override
        public Throwable hasFailed(String nodeName) {
            Node node = getNode(nodeName);
            if (!node.isCompleted()) {
                throw new IllegalStateException("node " + nodeName + " has not completed");
            }
            return node.throwable;
        }
    };

    private class Node {

        private final String name;
        /**
         * all dependencies of this node
         * this field does not need any lock to protect,
         * since there will be no modification after created.
         */
        private final LinkedList<Node> prev = new LinkedList<>();
        /**
         * all nodes which depend on this node
         * this field needs lock any time
         */
        private final LinkedList<Node> next = new LinkedList<>();

        /**
         * the head stage in the stage link list
         */
        private final Stage head;

        private final AtomicBoolean started = new AtomicBoolean();
        private final AtomicBoolean completed = new AtomicBoolean();
        /**
         * this field is protected by field completed
         */
        private Object result;
        /**
         * this field is protected by field completed
         */
        private Throwable throwable;

        Node(String name, Object param, WorkflowExecution... batch) {
            this.name = name;

            if (batch.length == 0) {
                throw new IllegalArgumentException("executions must have at least one");
            }

            head = new Stage(batch[0], param, null, null);
            Stage p = head;
            for (int i = 1; i < batch.length; i++) {
                p = new Stage(batch[i], param, null, p);
                p.prev.next = p;
            }
        }

        boolean isStarted() {
            return started.get();
        }

        /**
         * completed maybe true if started is not
         * @return whether it is completed
         */
        boolean isCompleted() {
            return completed.get();
        }

        public void start() {
            if (started.getAndSet(true)) {
                // another thread already starts this node
                return;
            }
            if (head == null) {
                throw new IllegalStateException("there are no executions to execute");
            }

            head.start();
        }

        void forceFail(Throwable throwable) {
            completeNode(null, throwable);
        }

        /**
         * one of its dependencies has failed
         * @param throwable exception thrown by one of its dependency
         */
        void dependencyFail(String dependencyName, Throwable throwable) {
            forceFail(throwable instanceof DependentFailureException ? throwable : new DependentFailureException(dependencyName, throwable));
        }

        /**
         * the execution of this node completed
         * you can call success or fail to instruct the status of your execution,
         * or just call complete to direct show your status
         *
         * @param result    the result of this execution
         *                  (can be null if you do not have such a result or something bad occurred)
         * @param throwable the throwable thrown while execution, it mut be not null if you are failed.
         */
        void completeNode(Object result, Throwable throwable) {
            // ensure after we finished this node, all stages must have completed, or cannot execute anymore
            synchronized (this) {
                if (completed.getAndSet(true)) {
                    return; // ignore the other complete method call, only the first complete will perform
                }
                started.set(true);
                this.result = result;
                this.throwable = throwable;
                onNodeComplete(this);
            }
        }

        /**
         * there will only be one thread at the same time to access a stage
         * since all stages are executed one by one from head to tail
         */
        private class Stage implements WorkflowPipeline, ExecutionCallback {
            private final WorkflowExecution execution;
            private final Object param;
            /**
             * whether this stage has started
             */
            private final AtomicBoolean started = new AtomicBoolean();
            /**
             * whether this stage has completed
             */
            private boolean completed;
            private boolean childrenCompleted;
            /**
             * in a bath of executions, the prev is the previous stage while the next is the next stage
             * these two fields will not be modified after created.
             * parent will be not null if we are included in a sub batch of a batch, and it refers to that batch
             */
            private final Stage parent;
            private final Stage prev;
            /**
             * next should be also a final field, but we cannot set it in the constructor
             */
            private Stage next;
            /**
             * sub batches of this batch, every element in the list starts one own batch
             */
            private Stage subBatch;
            private Stage nextBatch;

            private Object result;
            private Throwable throwable;
            /**
             * field used to pass other values
             */
            private Object data;

            Stage(WorkflowExecution execution, Object param, Stage parent, Stage prev) {
                this.execution = execution;
                this.param = param;
                this.parent = parent;
                this.prev = prev;
            }

            /**
             * start will only be called by the start() method in node class or complete() method in stage class
             * fortunately these two methods can ensure thread safe.
             * so this method will only be called once and before complete() method
             */
            void start() {
                if (started.getAndSet(true)) {
                    return;
                }

                synchronized (Node.this) {
                    if (Node.this.completed.get()) {
                        // we must ensure that after node completed, all stages in that node must not start
                        // this can occurred when the node completed by a timeout event
                        return;
                    }
                }

                // failure occurred before this stage in current batch but we does not accept it
                if (prev != null && prev.throwable != null && !execution.acceptPreviousFailure()) {

                    Throwable throwable;
                    if (prev.throwable instanceof DependentFailureException || execution.inheritPreviousFailure()) {
                        throwable = prev.throwable;
                    } else {
                        throwable = new DependentFailureException(prev.throwable);
                    }

                    onCompleted(null, throwable);
                    return;
                }

                ExecutionResult result;
                try {
                    result = execution.start(this);
                } catch (Throwable throwable) {
                    onCompleted(null, throwable);
                    return;
                }

                if (result == null) {
                    onCompleted(null, null);
                } else {
                    result.setCallback(this);
                }
            }

            /**
             * the context defined in the workflow
             *
             * @return context value
             */
            @Override
            public Object getContext() {
                return context;
            }

            /**
             * the param passed to this stage while execution
             *
             * @return the param passed to the execution
             */
            @Override
            public Object getParam() {
                return param;
            }

            /**
             * check if the specified node exists
             *
             * @param name name of the node to check
             * @return true if the node exists
             */
            @Override
            public boolean containsNode(String name) {
                return WorkflowGraph.this.containsNode(name);
            }

            /**
             * set data to current stage
             *
             * @param data data to save
             */
            @Override
            public void setData(Object data) {
                this.data = data;
            }

            /**
             * add more node
             * these nodes will be called after all the dependencies completed (succeeded or failed)
             *
             * @param name         the name of the node you want to pipe
             * @param dependencies iterate all dependencies of the node you are creating (must be unique)
             * @param param        param passed to the execution
             * @param batch        a batch of executions (must not empty)
             */
            @Override
            public void node(String name, Iterable<String> dependencies, Object param, WorkflowExecution... batch) {
                pipeNode(name, Node.this, dependencies, param, batch);
            }

            /**
             * add a stage to current node
             *
             * @param param param used to execute the execution logic
             * @param batch a batch of executions (must not empty)
             */
            @Override
            public void stage(Object param, WorkflowExecution... batch) {
                // h will be the head of the creating linked list of stages of this batch
                Stage h = new Stage(batch[0], param, this, null), p = h;
                for (int i = 1; i < batch.length; i++) {
                    p = new Stage(batch[i], param, this, p);
                    p.prev.next = p;
                }
                synchronized (head) {
                    if (completed && childrenCompleted) {
                        // a completed stage (without uncompleted sub batches) does not allow to modify its nextBatch field
                        throw new IllegalStateException("current stage has completed");
                    }
                    h.nextBatch = subBatch;
                    subBatch = h;
                }
                h.start(); // start the batch as soon as possible
            }

            /**
             * get the result of the previous stage in the same batch (if exists)
             *
             * @return result of the previous stage
             * @throws Throwable exception thrown by the previous stage if error occurred
             */
            @Override
            public Object previousValue() throws Throwable {
                if (prev == null) {
                    throw new NoSuchElementException("prev is not found in this batch");
                }
                if (prev.throwable != null) {
                    throw prev.throwable;
                }
                return prev.result;
            }

            /**
             * check if the previous stage in the same batch (if exists) has failed
             *
             * @return the exception thrown by the previous stage
             * @throws NoSuchElementException this exception will be thrown if no such previous stage found in the same batch
             */
            @Override
            public Throwable hasPreviousFailed() {
                if (prev == null) {
                    throw new NoSuchElementException("prev is not found in this batch");
                }
                return prev.throwable instanceof DependentFailureException ? prev.throwable.getCause() : prev.throwable;
            }

            /**
             * get the data saved to the previous stage
             *
             * @return data saved before
             */
            @Override
            public Object getPreviousData() {
                if (prev == null) {
                    throw new NoSuchElementException("prev is not found in this batch");
                }
                return prev.data;
            }

            /**
             * update childrenCompleted field
             * must be protected by caller
             */
            void updateChildrenCompleted() {
                boolean allCompleted = true;
                for (Stage p = subBatch; p != null; p = p.nextBatch) {
                    Throwable batchError = null;
                    for (Stage q = p; q != null; q = q.next) {
                        if (q.completed) {
                            if (!q.childrenCompleted) {
                                q.updateChildrenCompleted();
                                if (!q.childrenCompleted) {
                                    allCompleted = false;
                                    break;
                                }
                            }
                            if (q.next == null && q.throwable != null) { // populate the error from children, last stage of the batch
                                batchError = q.throwable;
                            }
                        } else {
                            allCompleted = false;
                            break;
                        }
                    }
                    if (throwable == null && batchError != null) {
                        // only get the last batch, since the last batch always be the first in the subBatch link
                        // since then, we have put the error of the last stage in the last batch who have failed
                        throwable = batchError;
                    }
                }
                if (allCompleted) {
                    childrenCompleted = true;
                }
            }

            /**
             * the execution has completed
             *
             * @param result    result of the execution if not failed
             * @param throwable the exception thrown when the execution failed
             */
            @Override
            public void onCompleted(Object result, Throwable throwable) {
                if (throwable != null) {
                    logger.debug("execute stage failed", throwable);
                }

                Stage stageToExecute = null, topNext = null;

                // what we should do:
                // 1. check if all my children have completed to determine whether to start next stage in current batch
                // 2. check if current batch completed, so we can tell parent to move to its next batch
                // 3. no parent or no next batch, tell current node to complete the node
                synchronized (head) {
                    if (completed) {
                        throw new IllegalStateException("the stage has already completed");
                    }
                    completed = true;

                    this.result = result;
                    this.throwable = throwable;

                    for (Stage p = this; p != null; p = p.parent) {
                        if (p.completed && !p.childrenCompleted) {
                            p.updateChildrenCompleted();
                        }
                        if (!p.childrenCompleted) {
                            // there are sub batches of p which do not have completed.
                            // it is the sub batch's responsibility to start p.next stage
                            return;
                        }
                        if (p.next == null) {
                            if (p.parent == null) {
                                topNext = p;
                            }
                        } else {
                            stageToExecute = p.next;
                            break;
                        }
                    }
                }

                if (stageToExecute == null) {
                    // adding new stages only can be allowed when there are some stages which have not completed
                    // so there will be no chance a new stage created
                    assert topNext != null;
                    completeNode(topNext.result, topNext.throwable);
                } else {
                    // next stage to execute will never be changed since current stage has completed
                    stageToExecute.start();
                }
            }
        }
    }
}
