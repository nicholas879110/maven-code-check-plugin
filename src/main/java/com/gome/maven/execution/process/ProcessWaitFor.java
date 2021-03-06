package com.gome.maven.execution.process;

import com.gome.maven.execution.TaskExecutor;
import com.gome.maven.openapi.diagnostic.Logger;
import com.gome.maven.util.Consumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

public class ProcessWaitFor {
    private static final Logger LOG = Logger.getInstance("#com.gome.maven.execution.process.ProcessWaitFor");

    private final Future<?> myWaitForThreadFuture;
    private final BlockingQueue<Consumer<Integer>> myTerminationCallback = new ArrayBlockingQueue<Consumer<Integer>>(1);

    public void detach() {
        myWaitForThreadFuture.cancel(true);
    }


    public ProcessWaitFor(final Process process, final TaskExecutor executor) {
        myWaitForThreadFuture = executor.executeTask(new Runnable() {
            @Override
            public void run() {
                int exitCode = 0;
                try {
                    while (true) {
                        try {
                            exitCode = process.waitFor();
                            break;
                        }
                        catch (InterruptedException e) {
                            LOG.debug(e);
                        }
                    }
                }
                finally {
                    try {
                        myTerminationCallback.take().consume(exitCode);
                    }
                    catch (InterruptedException e) {
                        LOG.info(e);
                    }
                }
            }
        });
    }

    public void setTerminationCallback(Consumer<Integer> r) {
        myTerminationCallback.offer(r);
    }
}
