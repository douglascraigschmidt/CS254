package edu.vandy.simulator.managers.palantiri.spinLockHashMap;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * This class emulates a "compare and swap"-style spin lock with
 * non-recursive semantics.
 */
class SpinLock
        implements CancellableLock {
    /**
     * Define an AtomicBoolean that's used as the basis for an atomic
     * compare-and-swap.  The default state of the spinlock should be
     * "unlocked".
     */
    // TODO -- you fill in here.
    public AtomicBoolean mOwner = new AtomicBoolean();

    /**
     * @return The AtomicBoolean used for compare-and-swap.
     */
    public AtomicBoolean getOwner() {
        // TODO -- you fill in here, replacing null with the proper
        // code.
        return mOwner;
    }

    /**
     * Acquire the lock only if it is free at the time of invocation.
     * Acquire the lock if it is available and returns immediately
     * with the value true.  If the lock is not available then this
     * method will return immediately with the value false.
     */
    @Override
    public boolean tryLock() {
        // Try to set mOwner's value to true, which succeeds iff its
        // current value is false.
        // TODO -- you fill in here, replacing false with the proper
        // code.
        return mOwner.compareAndSet(false, true);
    }

    /**
     * Acquire the lock. If the lock is not available then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until the lock has been acquired.
     *
     * @param isCancelled Supplier that is called to see if the attempt
     *                    to lock should be abandoned due to a pending
     *                    shutdown operation.
     * @throws CancellationException Thrown only if a pending shutdown
     *                               operation is has been detected by calling the isCancelled supplier.
     */
    @Override
    public void lock(Supplier<Boolean> isCancelled)
            throws CancellationException {
        // Loop trying to set mOwner's value to true, which succeeds
        // iff its current value is false.  Each iteration should also
        // check if a shutdown has been requested and if so throw a
        // cancellation exception.
        // TODO -- you fill in here.

        for (; ; ) {
            // Only try to get the lock if its null, which improves
            // cache performance.
            if (!mOwner.get() && tryLock()) {
                // Break out of the loop if we got the lock.
                break;
            } else {
                // Check if a shutdown has been requested and if so throw
                // a cancellation exception.
                if (isCancelled.get()) {
                    throw new CancellationException("SpinLock cancelled.");
                }

                // Yield the thread to allow another thread to run.
                Thread.yield();
            }
        }
    }

    /**
     * Release the lock.  Throws IllegalMonitorStateException if
     * the calling thread doesn't own the lock.
     */
    @Override
    public void unlock() {
        // Atomically release the lock that's currently held by
        // mOwner. If the lock is not held by mOwner, then throw
        // an IllegalMonitorStateException.
        // TODO -- you fill in here.

        // Set mOwner's value to false, which atomically releases the
        // lock that's currently held.
        if (!mOwner.getAndSet(false)) {
            throw new IllegalMonitorStateException("Unlock called when not locked");
        }
    }
}
