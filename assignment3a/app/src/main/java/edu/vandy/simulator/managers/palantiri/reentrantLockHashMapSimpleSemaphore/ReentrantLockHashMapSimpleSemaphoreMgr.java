package edu.vandy.simulator.managers.palantiri.reentrantLockHashMapSimpleSemaphore;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import edu.vandy.simulator.managers.palantiri.Palantir;
import edu.vandy.simulator.managers.palantiri.PalantiriManager;
import edu.vandy.simulator.utils.Assignment;

import static java.util.stream.Collectors.toMap;

/**
 * Defines a mechanism that mediates concurrent access to a fixed
 * number of available Palantiri.  This class uses a HashMap, a
 * SimpleSemaphore, and a ReentrantLock to mediate concurrent access
 * to the Palantiri.  This class implements a variant of the "Pooling"
 * pattern (kircher-schwanninger.de/michael/publications/Pooling.pdf).
 */
public class ReentrantLockHashMapSimpleSemaphoreMgr extends PalantiriManager {
    /**
     * Debugging tag used by the Android logger.
     */
    protected final static String TAG =
            ReentrantLockHashMapSimpleSemaphoreMgr.class.getSimpleName();

    /**
     * A map that associates the Palantir key to the Boolean values to
     * keep track of whether the key is available.
     */
    // TODO -- you fill in here.
    

    /**
     * A counting SimpleSemaphore that limits concurrent access to the
     * fixed number of available palantiri managed by the
     * PalantiriManager.
     */
    // TODO -- you fill in here.
    

    /**
     * A Lock used to protect critical sections involving the HashMap.
     */
    // TODO -- you fill in here.
    

    /**
     * Resets the fields to their initial values
     * and tells all beings to reset themselves.
     * <p>
     * Override this class if the being manager
     * implementation has it's own fields or
     * state to reset.
     */
    @Override
    public void reset() {
        super.reset();
    }

    /**
     * Implementations should setup and initialize class field
     * required for the Palantiri management strategy. All Palantiri
     * will have already been created by this super class. The list of
     * Palantiri can be accessed via the getPalantiri() method and the
     * count via getPalantirCount().
     */
    @Override
    protected void buildModel() {
        // ALL STUDENTS:
        // Create a new HashMap, iterate through the List of Palantiri
        // and initialize each key in the HashMap with "true" to
        // indicate it's available, initialize the Semaphore to use a
        // "fair" implementation that mediates concurrent access to
        // the given Palantiri, and initialize the ReentrantLock to
        // use "unfair" semantics.

        // GRADUATE STUDENTS:
        // Use a Java sequential stream to convert a list of Palantiri
        // into a stream and then collect the results in a manner that
        // initializes each key in the mPalantiriMap with "true" to
        // indicate it's available.

        // Undergraduate students are free to use a Java sequential
        // stream, but it's not required.

        if (Assignment.isUndergraduate()) {
            // TODO -- you fill in here.
            
        } else if (Assignment.isGraduate()) {
            // TODO -- you fill in here.
            
        } else {
            throw new IllegalStateException("Invalid assignment type");
        }

        // Initialize the SimpleSemaphore (which has a "fair"
        // implementation) and is used to mediate concurrent access to
        // the given Palantiri map.
        // TODO -- you fill in here.
        

        // Initialize a new ReentrantLock (which has a "non-fair" implementation).
        // TODO -- you fill in here.
        
    }

    /**
     * Get a Palantir from the PalantiriManager, blocking until one is
     * available.
     *
     * This method should never return a null Palantir. It may,
     * however, throw a InterruptedException if a shutdown is being
     * processed while a thread is waiting for a Palantir.
     *
     * @return The first available Palantir
     * @throws InterruptedException if interrupted
     */
    @Override
    @NotNull
    protected Palantir acquire() throws InterruptedException {
        // ALL STUDENTS:
        // Acquire the SimpleSemaphore interruptibly and then find the
        // first key in the HashMap whose value is "true" (which
        // indicates it's available for use) in a thread-safe manner
        // (i.e., using the ReentrantLock properly).  Replace the
        // value of this key with "false" to indicate the Palantir
        // isn't available and return that palantir to the client.

        // GRADUATE STUDENTS:
        // Use a Java Stream to find the key in the HashMap whose
        // value is "true" and replace the key with "false".

        // Undergraduate students are free to use a Java Stream, but
        // it's not required.

        // Acquire the Semaphore interruptibly ensure that you serialize
        // the following actions in an interruptible manner to ensure
        // thread-safety.
        // TODO -- you fill in here.
        

        try {
            if (Assignment.isUndergraduate()) {
                // TODO -- you fill in here.
                
            } else if (Assignment.isGraduate()) {
                // TODO -- you fill in here.
                
            } else {
                throw new IllegalStateException("Invalid assignment type");
            }
        } finally {
            // Always release the lock.
            // TODO -- you fill in here.
            
        }

        // This invariant should always hold for all acquire()
        // implementations if implemented correctly.  That is the
        // purpose of enforcing the @NotNull along with the
        // CancellationException, i.e., it is clear that all
        // implementations should either be successful (if implemented
        // correctly) and return a Palantir, or fail because of
        // cancellation.
        throw new IllegalStateException("This method should either return a valid "
                                        + "Palantir or throw a InterruptedException. "
                                        + "In either case, this statement should not be reached.");
    }

    /**
     * Releases the {@code palantir} so that it's available for other
     * Beings to use.
     *
     * @param palantir The palantir to release to the Palantir pool
     * @throws {@link IllegalArgumentException} if the {@code
     *         palantir} is invalid
     */
    @Override
    protected void release(Palantir palantir) throws InterruptedException {
        // Update the status of the designated palantir in the
        // ReentrantLockHashMapSimpleSemaphoreMgr so it's available
        // for other Beings to use. This method should efficiently and
        // robustly
        // 1. Handle invalid palantir values, such as null values or
        //    values that are not valid keys in the HashMap.
        // 2. Lock and unlock the ReentrantLock field correctly to ensure
        //    mutually exclusive access to mutable shared state in a
        //    manner that is interruptible.
        // 3. Only release the semaphore if the palantir parameter
        //    is correct.
        // TODO -- you fill in here.
        
    }

    /**
     * Called when the simulation is being shutdown to allow model
     * components the opportunity to and release resources and to
     * reset field values.  The Beings will have already have been
     * shutdown by the base class before calling this method.
     */
    @Override
    public void shutdownNow() {
        Log.d(TAG, "shutdownNow: called.");
    }
}
