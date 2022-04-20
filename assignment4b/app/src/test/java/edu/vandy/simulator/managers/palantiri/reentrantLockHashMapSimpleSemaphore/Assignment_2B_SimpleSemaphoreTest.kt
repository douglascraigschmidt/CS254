package edu.vandy.simulator.managers.palantiri.reentrantLockHashMapSimpleSemaphore

import admin.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class Assignment_2B_SimpleSemaphoreTest : AssignmentTests() {
    private val palantirCount = Random.nextInt(5, 20)

    @MockK
    lateinit var lock: Lock

    @MockK
    lateinit var notZero: Condition

    @MockK(relaxed = true)
    lateinit var mock: SimpleSemaphore

    @SpyK
    var semaphore = SimpleSemaphore()

    @Before
    fun before() {
        lock.injectInto(semaphore)
        notZero.injectInto(semaphore)
        lock.injectInto(mock)
        notZero.injectInto(mock)
    }

    private fun getPermits(): Int = semaphore.getField("", Int::class.java)

    private fun setPermits(permits: Int) {
        permits.injectInto(semaphore)
    }

    @Test
    fun `Permits is declared correctly`() {
        with(SimpleSemaphore::class.java.findField(Int::class.java)) {
            assertTrue(hasModifiers("volatile"))
        }
    }

    @Test
    fun `acquire one permit`() {
        setPermits(palantirCount)

        semaphore.acquire()

        verifySequence {
            lock.lockInterruptibly()
            lock.unlock()
        }

        val expectedAvailablePermits = palantirCount - 1
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            semaphore.availablePermits()
        )
    }

    @Test
    fun `acquire one permit when none are immediately available`() {
        val expectedAwaitCalls = Random.nextInt(3, 5)
        setPermits(-(expectedAwaitCalls - 1))
        every { notZero.await() } answers {
            if (getPermits() <= 0) {
                setPermits(getPermits() + 1)
            }
        }

        semaphore.acquire()

        verifyOrder {
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }
        verify(exactly = expectedAwaitCalls) { notZero.await() }

        val expectedAvailablePermits = 0
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            semaphore.availablePermits()
        )
    }

    @Test
    fun `acquire all permits`() {
        setPermits(palantirCount)

        for (i in 0 until palantirCount) {
            semaphore.acquire()
        }

        verify(exactly = palantirCount) {
            lock.lockInterruptibly()
            lock.unlock()
        }
        verifyOrder {
            lock.lockInterruptibly()
            lock.unlock()
        }

        val expectedAvailablePermits = 0
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            semaphore.availablePermits()
        )
    }

    @Test
    fun `acquire multiple permits with await calls`() {
        (-palantirCount).injectInto(mock)

        every { notZero.await() } answers {
            val permits = mock.getField<Int>("", Int::class.java)
            (permits + 1).injectInto(mock)
        }

        every { mock.acquire() } answers { callOriginal() }
        every { mock.availablePermits() } answers { callOriginal() }

        mock.acquire()

        verifyOrder {
            mock.acquire()
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }
        verify(exactly = palantirCount + 1) { notZero.await() }
        confirmVerified(mock, lock, notZero)

        val expectedAvailablePermits = 0
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            mock.availablePermits()
        )
    }

    @Test
    fun `acquire one permit with an await call`() {
        0.injectInto(mock)

        every { notZero.await() } answers {
            1.injectInto(mock)
        }

        every { mock.acquire() } answers { callOriginal() }
        every { mock.availablePermits() } answers { callOriginal() }

        mock.acquire()

        verifySequence {
            mock.acquire()
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }

        confirmVerified(lock, notZero, mock)

        val expectedAvailablePermits = 0
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            mock.availablePermits()
        )
    }

    @Test
    fun `acquire permit with await call interrupted`() {
        0.injectInto(mock)

        every { notZero.await() } throws InterruptedException("Mock interrupt")
        every { mock.acquire() } answers { callOriginal() }
        every { mock.availablePermits() } answers { callOriginal() }

        assertFailsWith<InterruptedException> { mock.acquire() }

        verifySequence {
            mock.acquire()
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }
        val expectedAvailablePermits = 0
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            mock.availablePermits()
        )
    }


    @Test
    fun `acquire permit uninterruptibly with permits available`() {
        every { mock.acquire() } answers { Unit }
        every { mock.acquireUninterruptibly() } answers { callOriginal() }
        mock.acquireUninterruptibly()

        assertFalse(Thread.currentThread().isInterrupted)

        verifySequence {
            mock.acquireUninterruptibly()
            mock.acquire()
        }
        confirmVerified(lock, notZero, mock)
    }

    @Test
    fun `acquire uninterruptibly should not directly modify permits`() {
        every { mock.acquire() } answers { Unit }
        every { mock.acquireUninterruptibly() } answers { callOriginal() }

        val permits = Random.nextInt(10, 20)
        permits.injectInto(mock)
        assertEquals(permits, mock.primitiveValue<Int>(Int::class))

        mock.acquireUninterruptibly()

        val result = mock.primitiveValue<Int>(Int::class)
        assertEquals(permits, result)

        verifySequence {
            mock.acquireUninterruptibly()
            mock.acquire()
        }
        confirmVerified(lock, notZero, mock)
    }

    @Test
    fun `acquire permit uninterruptibly should not be interruptible`() {
        every { mock.acquireUninterruptibly() } answers { callOriginal() }
        // andThen {} is required for this test to work.
        every { mock.acquire() } throws InterruptedException("Mock exception") andThen {}

        mock.acquireUninterruptibly()

        verifySequence {
            mock.acquireUninterruptibly()
            mock.acquire()
            mock.acquire()
        }
        confirmVerified(lock, notZero, mock)
    }

    @Test
    fun `acquire uninterruptibly should reset interrupt flag when interrupted`() {

        val interrupts = 3 + Random.nextInt(7)
        var loopCount = 0

        every { mock.acquireUninterruptibly() } answers { callOriginal() }

        every { mock.acquire() } answers {
            val wasInterrupted = Thread.interrupted()
            assertFalse(
                wasInterrupted,
                "Thread should have reset the Thread interrupted flag."
            )
            loopCount++
            if (loopCount <= interrupts) {
                throw InterruptedException("Mock interrupt.")
            }
        }

        try {
            mock.acquireUninterruptibly()
        } catch (e: Exception) {
            fail("Exception $e should not have been thrown.")
        }

        verify(exactly = 1) { mock.acquireUninterruptibly() }
        verify(exactly = interrupts + 1) { mock.acquire() }
        confirmVerified(lock, notZero, mock)

        val wasInterrupted = Thread.interrupted()
        assertTrue(wasInterrupted, "Thread should have reset the Thread interrupted flag.")
    }

    @Test
    fun `acquire permit uninterruptibly should set interrupt flag before returning`() {
        every { mock.acquireUninterruptibly() } answers { callOriginal() }

        Thread.currentThread().interrupt()
        // andThen {} is required for this test to work.
        every { mock.acquire() } throws InterruptedException("Mock exception") andThen {
            Thread.interrupted()
        }

        mock.acquireUninterruptibly()

        verifySequence {
            mock.acquireUninterruptibly()
            mock.acquire()
            mock.acquire()
        }
        confirmVerified(lock, notZero, mock)

        assertTrue(Thread.currentThread().isInterrupted)
    }

    @Test
    fun `acquire permit uninterruptibly should set interrupt flag if interrupted`() {
        // andThen {} is required for this test to work.
        every { mock.acquire() } throws InterruptedException("Mock exception") andThen { }
        every { mock.acquireUninterruptibly() } answers { callOriginal() }

        assertFalse(Thread.currentThread().isInterrupted)
        mock.acquireUninterruptibly()
        assertTrue(Thread.currentThread().isInterrupted)

        verify { mock.acquireUninterruptibly() }
        verify(exactly = 2) { mock.acquire() }

        confirmVerified(lock, notZero, mock)
    }

    @Test
    fun `release permit with signal`() {
        setPermits(0)

        semaphore.release()

        verify {
            lock.lockInterruptibly()
            notZero.signal()
            lock.unlock()
        }

        val expectedAvailablePermits = 1
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            semaphore.availablePermits()
        )
    }

    @Test
    fun `release permit with no signal`() {
        setPermits(-1)

        semaphore.release()

        verifySequence {
            lock.lockInterruptibly()
            lock.unlock()
        }

        val expectedAvailablePermits = 0
        assertEquals(
            "Available permits should be $expectedAvailablePermits",
            expectedAvailablePermits,
            semaphore.availablePermits()
        )
    }

    @Test
    fun `release permit with only when positive`() {
        setPermits(1)

        semaphore.release()

        verifySequence {
            lock.lockInterruptibly()
            notZero.signal()
            lock.unlock()
        }

        assertEquals(getPermits(), 2)
    }
}