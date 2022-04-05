package edu.vandy.simulator.managers.beings.forkJoinPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ForkJoinPool;

import edu.vandy.simulator.managers.beings.Being;
import edu.vandy.simulator.managers.beings.BeingManager;
import edu.vandy.simulator.managers.palantiri.Palantir;

/**
 * This class implements the gazing logic of a Being using the Java
 * ManagedBlocker class that's associated with the Java common
 * fork-join pool.
 */
public class BeingBlocker
       extends Being
       implements ForkJoinPool.ManagedBlocker {
    /**
     * Used for Android debugging.
     */
    private final static String TAG =
        BeingBlocker.class.getName();

    /**
     * A CyclicBarrier entry barrier that ensures all threads in the
     * thread pool start running at the same time.
     */
    // TODO -- you fill in here.
    

    /**
     * A CountDownLatch exit barrier that ensures the waiter thread
     * doesn't finish until all the Beings finish gazing.
     */
    

    /**
     * Constructor initializes the field.
     *
     * @param manager The controlling BeingManager instance.
     */
    BeingBlocker(BeingManager<BeingBlocker> manager) {
        // Call super constructor passing the manager.
        super(manager);
    }

    /**
     * Initialize the barrier fields.
     *
     * @param entryBarrier A {@link CyclicBarrier} that ensures
     *                     all Beings start gazing at the same time 
     * @param exitBarrier  A {@link CountDownLatch} that ensures
     *                     the waiter thread doesn't exit until
     *                     all Beings are done gazing
     */
    void setBarriers(CyclicBarrier entryBarrier,
                     CountDownLatch exitBarrier) {
        // Initialize the barrier fields.
        // TODO -- you fill in here.
        
    }

    /**
     * Run the loop that performs the Being gazing logic.
     */
    @Override
    public boolean block() {
        try {
            // Don't start gazing until all Beings are ready to run.
            // TODO -- You fill in here.
            

            // Gaze at a palantir the designated number of times.
            runGazingSimulation(getGazingIterations());

            // Inform waiter thread that this Being is done gazing.
            // TODO -- You fill in here.
            

            // TODO -- you fill in here replacing this statement with your
            // solution.
            throw new UnsupportedOperationException("Replace this");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Always return false.
     */
    @Override
    public boolean isReleasable() {
        return false;
    }

    /**
     * Perform a single gazing operation.
     */
    @Override
    protected void acquirePalantirAndGaze() {
        // Get a palantir from the BeingManager by calling the
        // appropriate base class helper method - this call will block
        // if there are no available palantiri (if a concurrency error
        // occurs in the assignment implementation, null is returned
        // and this being should immediately call Being.error(), which
        // throws an IllegalStateException).  Then gaze at the
        // palantir for this being (which blocks for a random period
        // of time).  Finally, release the palantir for this being via
        // a call to the appropriate base class helper method.

        // TODO -- you fill in here.
        
    }
}
