package edu.vandy.simulator.managers.palantiri.concurrentMapFairSemaphore

import admin.*
import com.nhaarman.mockitokotlin2.*
import edu.vandy.simulator.utils.Student.Type.Undergraduate
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mock
import org.mockito.quality.Strictness
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.*

// Need to map to Java Object class to call Object.wait() method in many tests
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class Assignment_3B_FairSemaphoreWhiteBoxMOTest : AssignmentTests() {
    @Mock
    private lateinit var queueMock: LinkedList<FairSemaphoreMO.Waiter>

    @Mock
    private lateinit var fairSemaphoreMock: FairSemaphoreMO

    @Mock
    private lateinit var waiterMock: FairSemaphoreMO.Waiter

    /**
     * Handle mock injections manually so that Mockito doesn't get
     * confused about which locks belong where.
     */
    @Before
    fun before() {
        runAs(Undergraduate)
        ReflectionHelper.injectValueIntoFirstMatchingField(
                fairSemaphoreMock, queueMock, LinkedList::class.java)

        // This prevents warnings about unused doAnswers that
        // are defined globally by the previous setup...() calls.
        // This is necessary because we never know what methods students
        // will call so we need to stub many methods to catch all
        // possibilities.
        mockitoRule.strictness(Strictness.LENIENT)
    }

    @Test
    fun `test constructor`() {
        with (FairSemaphoreMO(9)) {
            assertThat(primitiveValueHasModifier(Modifier.VOLATILE, Int::class)).isTrue
            assertThat(primitiveValueHasModifier(Modifier.STATIC, Int::class)).isFalse
            assertThat(primitiveValue<Int>(Int::class)).isEqualTo(9)
        }
    }

    @Test
    fun testAvailablePermits() {
        val expected = 999
        permits = expected
        doCallRealMethod().whenever(fairSemaphoreMock).availablePermits()
        val result = fairSemaphoreMock.availablePermits()
        assertEquals(expected, result)
    }

    @Test
    fun testWaiterFields() {
        val waiter = FairSemaphoreMO.Waiter()

        // Field may be declared as Lock or ReentrantLock, so check for either.
        val released = ReflectionHelper.findFirstMatchingFieldValue<Boolean>(waiter, Boolean::class.javaPrimitiveType)
        assertNotNull(released, "Waiter class should have a boolean field field.")
        assertFalse(released, "Waiter class boolean field should be set to a false.")
    }

    @Test
    fun testFairSemaphoreFields() {
        val fairSemaphore = FairSemaphoreMO(PALANTIRI_COUNT)
        val queue = ReflectionHelper.findFirstMatchingFieldValue<LinkedList<*>>(fairSemaphore, LinkedList::class.java)
        assertNotNull(queue, "FairSemaphoreMO class should have a non-null LinkedList field.")
        assertEquals(PALANTIRI_COUNT, fairSemaphore.availablePermits())
    }

    @Test
    fun testAcquireUninterruptibly() {
        doCallRealMethod().whenever(fairSemaphoreMock).acquireUninterruptibly()
        try {
            fairSemaphoreMock.acquireUninterruptibly()
            assertFalse(Thread.currentThread().isInterrupted,
                    "Thread should not have interrupted flag set.")
        } catch (t: Throwable) {
            fail("Thread should not throw any exceptions.")
        }
        verify(fairSemaphoreMock, times(1)).acquire()
    }

    @Test
    fun testAcquireUninterruptiblyWithInterrupt() {
        val interrupts = 3 + Random().nextInt(7)
        var loopCount = 0
        doAnswer {
            val wasInterrupted = Thread.interrupted()
            assertFalse(wasInterrupted, "InterruptedException was not caught")
            loopCount++
            if (loopCount <= interrupts) {
                throw InterruptedException("Mock interrupt.")
            }
            null
        }.whenever(fairSemaphoreMock).acquire()

        doCallRealMethod().whenever(fairSemaphoreMock).acquireUninterruptibly()

        try {
            fairSemaphoreMock.acquireUninterruptibly()
        } catch (e: Exception) {
            fail("Exception $e should not have been thrown.")
        }

        verify(fairSemaphoreMock, times(interrupts + 1)).acquire()
        val wasInterrupted = Thread.interrupted()
        assertTrue(wasInterrupted, "Thread should have reset the Thread interrupted flag.")
    }

    @Test
    fun testAcquireWithNoBlocking() {
        whenever(fairSemaphoreMock.tryToGetPermit()).thenReturn(true)
        doCallRealMethod().whenever(fairSemaphoreMock).acquire()
        fairSemaphoreMock.acquire()
        verify(fairSemaphoreMock, times(1)).tryToGetPermit()
        verify(fairSemaphoreMock, never()).waitForPermit()
    }

    @Test
    fun testAcquireWithBlocking() {
        whenever(fairSemaphoreMock.tryToGetPermit()).thenReturn(false)
        doCallRealMethod().whenever(fairSemaphoreMock).acquire()
        fairSemaphoreMock.acquire()
        verify(fairSemaphoreMock, times(1)).tryToGetPermit()
        verify(fairSemaphoreMock, times(1)).waitForPermit()
    }

    @Test
    fun testAcquireWithInterrupt() {
        Thread.currentThread().interrupt()
        whenever(fairSemaphoreMock.tryToGetPermit()).thenReturn(false)
        doCallRealMethod().whenever(fairSemaphoreMock).acquire()
        try {
            fairSemaphoreMock.acquire()
            fail("Method should have thrown and InterruptedException.")
        } catch (e: InterruptedException) {
        }
        verify(fairSemaphoreMock, never()).tryToGetPermit()
        verify(fairSemaphoreMock, never()).waitForPermit()
    }

    @Test
    fun testTryToGetPermitWhenNotLocked() {
        whenTryToGetPermitUnlockedVerify(classLockedAndWaiterNotLocked())
        doCallRealMethod().whenever(fairSemaphoreMock).tryToGetPermit()
        val result = fairSemaphoreMock.tryToGetPermit()
        assertTrue(result, "Method should have returned true.")
    }

    private fun whenTryToGetPermitUnlockedVerify(verify: Runnable) {
        whenever(fairSemaphoreMock.tryToGetPermitUnlocked())
                .thenAnswer {
                    verify.run()
                    true
                }
    }

    @Test
    fun testTryToGetPermitUnlockedEmptyQueueAndNoAvailablePermits() {
        val availablePermits = 0
        val expectedResult = false
        val emptyQueue = 0
        testTryToGetPermitUnlocked(emptyQueue, availablePermits, availablePermits, expectedResult)
    }

    @Test
    fun testTryToGetPermitUnlockedEmptyQueueAndAvailablePermits() {
        val availablePermits = 1
        val expectedPermits = availablePermits - 1
        val expectedResult = true
        val emptyQueue = 0
        testTryToGetPermitUnlocked(emptyQueue, availablePermits, expectedPermits, expectedResult)
    }

    @Test
    fun testTryToGetPermitUnlockedFullQueueAndNoAvailablePermits() {
        val availablePermits = 0
        val expectedResult = false
        val notEmptyQueue = 1
        testTryToGetPermitUnlocked(notEmptyQueue, availablePermits, availablePermits, expectedResult)
    }

    @Test
    fun testTryToGetPermitUnlockedFullQueueAndAvailablePermits() {
        val availablePermits = 1
        val expectedResult = false
        val notEmptyQueue = 1
        testTryToGetPermitUnlocked(notEmptyQueue, availablePermits, availablePermits, expectedResult)
    }

    private fun testTryToGetPermitUnlocked(
            queueSize: Int,
            availablePermits: Int,
            expectedPermits: Int,
            expectedResult: Boolean) {
        permits = availablePermits

        // Overwrite mock queue with a real one for this test.
        buildAndInjectQueue(queueSize)
        whenever(fairSemaphoreMock.availablePermits()).thenReturn(fairSemaphoreMock.primitiveValue<Int>(Int::class))
        doCallRealMethod().whenever(fairSemaphoreMock).tryToGetPermitUnlocked()
        val result = fairSemaphoreMock.tryToGetPermitUnlocked()
        assertEquals(expectedResult, result, "Method returned wrong result.")
        assertEquals(expectedPermits, permits, "Available permits incorrect.")
    }

    @Test
    fun testWaitForPermitWithNoBlocking() {
        // Mock the Waiter.
        waiterMock.mReleased = true
        whenever(fairSemaphoreMock.createWaiter()).thenReturn(waiterMock)
        doCallRealMethod().whenever(fairSemaphoreMock).waitForPermit()
        whenTryToGetPermitUnlockedCalledVerifyAndReturn(classLockedAndWaiterLocked(), true)
        whenQueueAddCalledVerify(classLockedAndWaiterLocked())
        whenWaiterWaitCalledVerifyNotReleasedAnd(classNotLockedAndWaiterLocked())
        fairSemaphoreMock.waitForPermit()
        verify(fairSemaphoreMock, times(1)).tryToGetPermitUnlocked()
    }

    @Test
    fun testWaitForPermitWithBlocking() {
        // Set release flag to false so that wait() will be called.
        waiterMock.mReleased = false

        // Ensure that the Waiter mock is injected into the SUT method.
        whenever(fairSemaphoreMock.createWaiter()).thenReturn(waiterMock)
        whenTryToGetPermitUnlockedCalledVerifyAndReturn(classLockedAndWaiterLocked(), false)
        whenQueueAddCalledVerify(classLockedAndWaiterLocked())
        whenWaiterWaitCalledVerifyAndRelease(classNotLockedAndWaiterLocked())
        val inOrder = inOrder(fairSemaphoreMock, queueMock, waiterMock)
        doCallRealMethod().whenever(fairSemaphoreMock).waitForPermit()
        fairSemaphoreMock.waitForPermit()
        verify(fairSemaphoreMock, times(1)).createWaiter()
        verify(fairSemaphoreMock, times(1)).tryToGetPermitUnlocked()
        verifyQueueAddCalledOnce(waiterMock)
        verify(waiterMock as Object, times(1)).wait()
        verify(fairSemaphoreMock, never()).release()
        inOrder.verify(fairSemaphoreMock).createWaiter()
        inorderVerifyQueueAdd(inOrder)
        inOrder.verify(waiterMock as Object).wait()
    }

    @Test
    fun testWaitForPermitWithBlockingAndInterruptWhileQueued() {
        // Set release flag to false so that wait() will be called.
        waiterMock.mReleased = false

        // Ensure that the Waiter mock is injected into the SUT method.
        whenever(fairSemaphoreMock.createWaiter()).thenReturn(waiterMock)

        // Return true when queue remove called so that method will not call release().
        whenever(queueMock.remove(waiterMock)).thenReturn(true)
        whenTryToGetPermitUnlockedCalledVerifyAndReturn(classLockedAndWaiterLocked(), false)
        whenQueueAddCalledVerify(classLockedAndWaiterLocked())
        whenWaiterWaitCalledVerifyNotReleasedAndInterrupt(classNotLockedAndWaiterLocked())
        val inOrder = inOrder(fairSemaphoreMock, queueMock, waiterMock)
        doCallRealMethod().whenever(fairSemaphoreMock).waitForPermit()
        try {
            fairSemaphoreMock.waitForPermit()
            fail("InterruptedException should have been rethrown.")
        } catch (t: Throwable) {
            assertTrue(t is InterruptedException, "InterruptedException should have been rethrown.")
        }
        verify(fairSemaphoreMock, times(1)).createWaiter()
        verify(fairSemaphoreMock, times(1)).tryToGetPermitUnlocked()
        verifyQueueAddCalledOnce(waiterMock)
        verify(waiterMock as Object, times(1)).wait()
        verify(fairSemaphoreMock, never()).release()
        inOrder.verify(fairSemaphoreMock).createWaiter()
        inorderVerifyQueueAdd(inOrder)
        inOrder.verify(waiterMock as Object).wait()
        inOrder.verify(queueMock).remove(waiterMock)
    }

    @Test
    fun testWaitForPermitWithBlockingAndInterruptWhenNotQueued() {
        // Set release flag to false so that wait() will be called.
        waiterMock.mReleased = false

        // Ensure that the Waiter mock is injected into the SUT method.
        whenever(fairSemaphoreMock.createWaiter()).thenReturn(waiterMock)

        // Return true when queue remove called so that method will call release().
        whenever(queueMock.remove(waiterMock)).thenReturn(false)
        whenTryToGetPermitUnlockedCalledVerifyAndReturn(classLockedAndWaiterLocked(), false)
        whenQueueAddCalledVerify(classLockedAndWaiterLocked())
        whenWaiterWaitCalledVerifyNotReleasedAndInterrupt(classNotLockedAndWaiterLocked())
        val inOrder = inOrder(fairSemaphoreMock, queueMock, waiterMock)
        doCallRealMethod().whenever(fairSemaphoreMock).waitForPermit()
        try {
            fairSemaphoreMock.waitForPermit()
            fail("InterruptedException should have been rethrown.")
        } catch (t: Throwable) {
            assertTrue(t is InterruptedException, "InterruptedException should have been rethrown.")
        }
        verify(fairSemaphoreMock, times(1)).createWaiter()
        verify(fairSemaphoreMock, times(1)).tryToGetPermitUnlocked()
        verifyQueueAddCalledOnce(waiterMock)
        verify(waiterMock as Object, times(1)).wait()
        verify(fairSemaphoreMock, times(1)).release()
        inOrder.verify(fairSemaphoreMock).createWaiter()
        inorderVerifyQueueAdd(inOrder)
        inOrder.verify(waiterMock as Object).wait()
        inOrder.verify(queueMock).remove(waiterMock)
        inOrder.verify(fairSemaphoreMock).release()
    }

    @Test
    fun testReleaseWithEmptyQueue() {
        permits = 0
        val expectedPermits = permits + 1

        // Set Waiter mock field.
        waiterMock.mReleased = false
        whenQueuePollCalledVerifyAndReturn(classLockedAndWaiterNotLocked(), null)
        doCallRealMethod().whenever(fairSemaphoreMock).release()
        fairSemaphoreMock.release()
        assertEquals(expectedPermits, permits, "Available permits should be updated.")
        verifyQueuePollCalledOnce()
    }

    @Test
    fun testReleaseLockingWithNotEmptyQueue() {
        whenQueuePollCalledVerifyAndReturn(classLockedAndWaiterNotLocked(), waiterMock)
        whenWaiterNotifyCalledVerify(classLockedAndWaiterLocked(waiterMock))
        doCallRealMethod().whenever(fairSemaphoreMock).release()
        fairSemaphoreMock.release()
    }

    @Test
    fun testReleaseFIFOWithNotEmptyQueue() {
        val queueSize = 2
        val expectedQueueSize = queueSize - 1

        // Use a real queue so that all LinkedList methods will function
        // normally in called method and check that FIFO ordering is used.
        val queue = buildAndInjectQueue(queueSize)
        val expectedWaiter = queue.first
        permits = 1
        val expectedPermits = permits
        expectedWaiter.mReleased = false
        doCallRealMethod().whenever(fairSemaphoreMock).release()
        fairSemaphoreMock.release()
        assertEquals(expectedPermits, permits, "Available permits should not have changed.")
        assertEquals(expectedQueueSize, queue.size, "A Waiter entry should have been removed from the queue.")
        assertTrue(queue.first !== expectedWaiter, "The waiter was not removed using FIFO ordering.")
        assertTrue(expectedWaiter.mReleased, "Released Waiter's released flag should be set.")
    }

    private fun buildAndInjectQueue(size: Int): LinkedList<FairSemaphoreMO.Waiter> {
        val queue = LinkedList<FairSemaphoreMO.Waiter>()
        ReflectionHelper.injectValueIntoFirstMatchingField(fairSemaphoreMock, queue, LinkedList::class.java)
        for (i in 0 until size) {
            queue.add(mock())
        }
        return queue
    }

    private fun whenWaiterWaitCalledVerifyNotReleasedAnd(verify: Runnable) {
        doAnswer {
            assertFalse(waiterMock.mReleased)
            verify.run()
            null
        }.whenever(waiterMock as Object).wait()
    }

    private fun whenWaiterWaitCalledVerifyNotReleasedAndInterrupt(verify: Runnable) {
        doAnswer {
            assertFalse(waiterMock.mReleased)
            verify.run()
            throw InterruptedException("Mock interrupt.")
        }.whenever(waiterMock as Object).wait()
    }

    private fun whenQueueAddCalledVerify(verify: Runnable) {
        doAnswer {
            verify.run()
            true
        }.whenever(queueMock).add(waiterMock)
        doAnswer {
            classLockedAndWaiterLocked()
            true
        }.whenever(queueMock).addLast(waiterMock)
        doAnswer {
            classLockedAndWaiterLocked()
            true
        }.whenever(queueMock).add(queueMock.size, waiterMock)
    }

    private fun inorderVerifyQueueAdd(inOrder: InOrder) {
        try {
            inOrder.verify(queueMock).add(waiterMock)
        } catch (t1: Throwable) {
            try {
                inOrder.verify(queueMock).addLast(waiterMock)
            } catch (t2: Throwable) {
                inOrder.verify(queueMock).add(queueMock.size, waiterMock)
            }
        }
    }

    private fun whenTryToGetPermitUnlockedCalledVerifyAndReturn(
            verify: Runnable, returnValue: Boolean) {
        whenever(queueMock.add(waiterMock))
                .thenAnswer {
                    verify.run()
                    returnValue
                }
    }

    private fun whenWaiterWaitCalledVerifyAndRelease(verify: Runnable) {
        doAnswer {
            verify.run()
            // Set Waiter released flag so that caller will unblock.
            waiterMock.mReleased = true
            null
        }.whenever(waiterMock as Object).wait()
    }

    private fun whenWaiterNotifyCalledVerify(verify: Runnable) {
        doAnswer {
            verify.run()
            null
        }.whenever(waiterMock as Object).wait()
    }

    private fun whenQueuePollCalledVerifyAndReturn(verify: Runnable, o: Any?) {
        // Return Waiter mock (queue not empty) for all expected queue removal calls.
        whenever(queueMock.poll())
                .thenAnswer {
                    verify.run()
                    o
                }
        whenever(queueMock.pollFirst())
                .thenAnswer {
                    verify.run()
                    o
                }
        whenever(queueMock.remove())
                .thenAnswer {
                    verify.run()
                    o
                }
        whenever(queueMock.removeFirst())
                .thenAnswer {
                    verify.run()
                    o
                }
    }

    private fun verifyQueuePollCalledOnce() {
        try {
            verify(queueMock, times(1)).poll()
        } catch (t1: Throwable) {
            try {
                verify(queueMock, times(1)).pollFirst()
            } catch (t2: Throwable) {
                try {
                    verify(queueMock, times(1)).remove()
                } catch (t3: Throwable) {
                    verify(queueMock, times(1)).removeFirst()
                }
            }
        }
    }

    private fun inorderVerifyQueuePoll(inOrder: InOrder) {
        try {
            inOrder.verify(queueMock).poll()
        } catch (t1: Throwable) {
            try {
                inOrder.verify(queueMock).pollFirst()
            } catch (t2: Throwable) {
                try {
                    inOrder.verify(queueMock).remove()
                } catch (t3: Throwable) {
                    inOrder.verify(queueMock).removeFirst()
                }
            }
        }
    }

    private fun verifyQueueAddCalledOnce(waiter: FairSemaphoreMO.Waiter) {
        try {
            verify(queueMock, times(1)).add(waiter)
        } catch (t1: Throwable) {
            try {
                verify(queueMock, times(1)).add(queueMock.size, waiter)
            } catch (t2: Throwable) {
                verify(queueMock, times(1)).addLast(waiter)
            }
        }
    }

    private var permits: Int
        get() = ReflectionHelper.findFirstMatchingFieldValue(fairSemaphoreMock, Int::class.javaPrimitiveType)
        private set(value) {
            try {
                ReflectionHelper.injectValueIntoFirstMatchingField(
                        fairSemaphoreMock, value, Int::class.javaPrimitiveType)
            } catch (t: Throwable) {
                throw RuntimeException(t)
            }
        }

    private fun classNotLockedAndWaiterNotLocked(): Runnable {
        return Runnable {
            assertClassNotLocked()
            assertWaiterNotLocked()
        }
    }

    private fun classNotLockedAndWaiterLocked(): Runnable {
        return Runnable {
            assertClassNotLocked()
            assertWaiterLocked()
        }
    }

    private fun classLockedAndWaiterNotLocked(): Runnable {
        return Runnable {
            assertClassLocked()
            assertWaiterNotLocked()
        }
    }

    private fun classLockedAndWaiterLocked(): Runnable {
        return Runnable {
            assertClassLocked()
            assertWaiterLocked()
        }
    }

    private fun classLockedAndWaiterLocked(waiter: FairSemaphoreMO.Waiter): Runnable {
        return Runnable {
            assertClassLocked()
            assertWaiterLocked(waiter)
        }
    }

    private fun assertClassLocked() {
        assertTrue(Thread.holdsLock(fairSemaphoreMock), "Class lock should be locked.")

    }

    private fun assertClassNotLocked() {
        assertTrue(!Thread.holdsLock(fairSemaphoreMock), "Class lock should not be locked.")

    }

    private fun assertWaiterLocked() {
        assertTrue(Thread.holdsLock(waiterMock), "Waiter lock should be locked.")
    }

    private fun assertWaiterLocked(waiter: FairSemaphoreMO.Waiter) {
        assertTrue(Thread.holdsLock(waiter), "Waiter lock should be locked.")
    }

    private fun assertWaiterNotLocked() {
        assertTrue(!Thread.holdsLock(waiterMock), "Waiter lock should not be locked.")

    }

    companion object {
        private const val PALANTIRI_COUNT = 5
    }
}