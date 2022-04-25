package edu.vandy.simulator.managers.palantiri.stampedLockFairSemaphore;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

import edu.vandy.simulator.managers.palantiri.Palantir;
import edu.vandy.simulator.managers.palantiri.PalantiriManager;
import edu.vandy.simulator.managers.palantiri.concurrentMapFairSemaphore.FairSemaphore;
import edu.vandy.simulator.managers.palantiri.concurrentMapFairSemaphore.FairSemaphoreCO;
import edu.vandy.simulator.managers.palantiri.concurrentMapFairSemaphore.FairSemaphoreMO;
import edu.vandy.simulator.utils.Assignment;

import static java.util.stream.Collectors.toMap;

/**
 * This class uses a FairSemaphore, a HashMap, and a StampedLock to
 * mediate concurrent access to to a fixed number of available
 * Palantiri.  This class implements a variant of the "Pooling"
 * pattern (kircher-schwanninger.de/michael/publications/Pooling.pdf).
 */
public class StampedLockFairSemaphoreMgr
        extends PalantiriManager {
    /**
     * Debugging tag used by the Android logger.
     */
    private final static String TAG =
            StampedLockFairSemaphoreMgr.class.getSimpleName();

    /**
     * A counting semaphore that limits concurrent access to the fixed
     * number of available palantiri managed by the
     * StampedLockFairSemaphoreMgr.
     */
    private FairSemaphore mAvailablePalantiri;

    /**
     * A map that associates the {@link Palantir} key to the
     * {@link Boolean} values that keep track of whether the
     * key is available or not.
     */
    private Map<Palantir, Boolean> mPalantiriMap;

    /**
     * A StampedLock synchronizer that protects the Palantiri state.
     */
    // TODO -- you fill in here.
    

    /**
     * Zero parameter constructor required for Factory creation.
     */
    public StampedLockFairSemaphoreMgr() {
    }

    /**
     * Called by super class to build the Palantiri model.
     */
    @Override
    public void buildModel() {
        // Create a new HashMap, iterate through the List of Palantiri
        // and initialize each key in the HashMap with "true" to
        // indicate it's available, and initialize the Semaphore to
        // use a "fair" implementation that mediates concurrent access
        // to the given Palantiri.

        // Use the getPalantiri() to get a list of Palantiri and
        // initialize each key in the mPalantiriMap with "true" to
        // indicate it's available.  Grad students should use a Java
        // Stream to initialize mPalantiriMap, whereas ugrad students
        // can implement without using a Java Stream.
        // TODO -- you fill in here.
        

        // Initialize the Semaphore to use a "fair" implementation
        // that mediates concurrent access to the given Palantiri.
        // Grad students must use a FairSemaphoreCO, whereas ugrad
        // students must use a FairSemaphoreMO.
        if (Assignment.isUndergraduate()) {
            // TODO -- you fill in here.
            
        } else if (Assignment.isGraduate()) {
            // TODO -- you fill in here.
            
        }

        // Initialize the StampedLock.
        // TODO -- you fill in here.
        
    }

    /**
     * Get a palantir, blocking until one is available.
     *
     * This method should never return a null {@link Palantir}. It may,
     * however, throw a {@link InterruptedException} if a shutdown is
     * being processed while a thread is waiting for a {@link Palantir}.
     *
     * @return The first available Palantir
     * @throws {@link InterruptedException} if interrupted
     */
    @Override
    @NotNull
    public Palantir acquire() throws InterruptedException {
        // This code is tricky, so please carefully read the
        // StampedLock "upgrade" example that's described at
        // docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/StampedLock.html.

        // Grad students must use Java streams.
        if (Assignment.isGraduate()) {
            // Acquire the FairSemaphore interruptibly and then use
            // Java streams to find the first key in the HashMap whose
            // value is "true" (which indicates it's available for
            // use).  Replace the value of this key with "false" to
            // indicate the Palantir isn't available and then return
            // that palantir to the client.
            // 
            // This implementation should demonstrate StampedLock's
            // support for upgrading a readLock to a writeLock, as
            // well as for releasing a readLock and acquiring a
            // writeLock.  All locking of StampedLocks should be
            // interruptible.  You'll need to use a for-each loop in
            // conjunction with Java streams so you can restart your
            // search at the beginning of the HashMap if you're unable
            // to atomically upgrade the readlock to a writelock.

            // TODO -- you fill in here.
            

        // Undergrad students must use a Java iterator.
        } else if (Assignment.isUndergraduate()) {
            // Acquire the FairSemaphore interruptibly and then use an
            // Iterator to iterate through the HashMap to find the
            // first key in the HashMap whose value is "true" (which
            // indicates it's available for use).  Replace the value
            // of this key with "false" to indicate the Palantir isn't
            // available and then return that palantir to the client.
            // 
            // This implementation should demonstrate StampedLock's
            // support for upgrading a readLock to a writeLock, as
            // well as for releasing a readLock and acquiring a
            // writeLock.  All locking of StampedLocks should be
            // interruptible.  You'll need to use an Iterator instead
            // of a for-each loop so that you can restart your search
            // at the beginning of the HashMap if you're unable to
            // atomically upgrade the readlock to a writelock.

            // TODO -- you fill in here.
            
        }

        // This method either succeeds by returning a Palantir, or
        // fails if interrupted by a shutdown.  In ether case,
        // reaching this line should not be possible.
        throw new IllegalStateException("This is not possible");
    }

    /**
     * Releases the {@code palantir} so that it's available for other
     * Beings to use.
     *
     * @param palantir The palantir to release to the Palantiri pool
     * @throws {@link IllegalArgumentException} if the {@code
     *         palantir} is invalid
     */
    @Override
    public void release(final Palantir palantir) throws InterruptedException {
        // Update the status of the designated palantir in the
        // StampedLockFairSemaphoreMgr so it's available for other
        // Beings to use. This method should efficiently and robustly
        // 1. Handle invalid palantir values, such as null values or
        //    values that are not valid keys in the HashMap.
        // 2. Lock and unlock the StampedLock field correctly to ensure
        //    mutually exclusive access to mutable shared state in
        //    a manner that's interruptible.
        // 3. Only release the semaphore if the palantir parameter
        //    is correct.
        // TODO -- you fill in here.
        
    }

    /**
     * Called when the simulation is being shutdown to allow model
     * components the opportunity to and release resources and to
     * reset field values.
     */
    @Override
    public void shutdownNow() {
        Log.d(TAG, "shutdownNow: called.");
    }
}
