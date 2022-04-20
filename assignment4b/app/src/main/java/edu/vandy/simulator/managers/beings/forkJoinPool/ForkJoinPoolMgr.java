package edu.vandy.simulator.managers.beings.forkJoinPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ForkJoinPool;

import edu.vandy.simulator.managers.beings.BeingManager;

import static edu.vandy.simulator.utils.ExceptionUtils.rethrowRunnable;

/**
 * This BeingManager implementation uses the Java common ForkJoinPool
 * to create a pool of Java threads that run Being simulations.
 */
public class ForkJoinPoolMgr
        extends BeingManager<BeingBlocker> {
    /**
     * Used for Android debugging.
     */
    private final static String TAG =
            ForkJoinPoolMgr.class.getName();

    /**
     * A CyclicBarrier entry barrier that ensures all threads in the
     * thread pool start running at the same time.
     */
    // TODO -- you fill in here.
    

    /**
     * A CountDownLatch exit barrier that ensures the waiter thread
     * doesn't finish until all the Beings finish gazing.
     */
    // TODO -- you fill in here.
    

    /**
     * Default constructor.
     */
    public ForkJoinPoolMgr() {
    }

    /**
     * Resets the fields to their initial values and tells all beings
     * to reset themselves.
     */
    @Override
    public void reset() {
        super.reset();
    }

    /**
     * Abstract method that BeingManagers implement to return a new
     * BeingBlocker instance.
     *
     * @return A new typed Being instance.
     */
    @Override
    public BeingBlocker newBeing() {
        // Return a new BeingBlocker instance.
        // TODO -- you fill in here replacing this statement with
        // your solution.
        throw new UnsupportedOperationException("Replace this");
    }

    /**
     * This entry point method is called by the Simulator framework to
     * start the being gazing simulation.
     **/
    @Override
    public void runSimulation() throws Exception {
        // Call a method that initializes the barrier
        // synchronizers and assigns them to the Beings.
        // TODO -- you fill in here.
        

        // Call a method that uses the common fork-join pool to run a
        // pool of threads that represent the Beings in this
        // simulation.
        // TODO -- you fill in here.
        
    }

    /**
     * Initialize the barrier synchronizers and assign them to the
     * Beings.
     */
    public void initializeBarriers() {
        // Initialize an entry barrier that ensures all threads in the
        // thread pool start running at the same time.
        // TODO -- you fill in here.
        

        // Initialize an exit barrier to ensure the waiter thread
        // doesn't finish until all the Beings finish gazing.
        // TODO -- you fill in here.
        

        // Iterate through all the Beings and set their barriers
        // accordingly.
        // TODO -- you fill in here.
        
    }

    /**
     * Use the common fork-join pool to run the beings in this
     * simulation.
     */
    public void processAllBeings() throws Exception {
        // Call the BeingManager.getBeings() method to iterate through
        // the BeingBlockers and execute each BeingBlocker to run as
        // a ManagedBlocker in the common fork-join pool via the
        // managedBlock() method.

        // TODO -- you fill in here.
        

        // Don't continue with any processing until all Beings are ready
        // to run.
        // TODO -- You fill in here.
        

        // Don't continue until all Beings have finished their gazing.
        // TODO -- You fill in here.
        
    }

    /**
     * This is a no-op for this implementation since the Java common
     * fork-join pool is a singleton that doesn't get shutdown.
     */
    @Override
    public void shutdownNow() {
    }

}
