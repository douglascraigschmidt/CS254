package edu.vandy.simulator.managers.palantiri.reentrantLockHashMapSimpleSemaphore

import admin.*
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
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

    @SpyK
    var semaphore = SimpleSemaphore()

    @Before
    fun before() {
        lock.injectInto(semaphore)
        notZero.injectInto(semaphore)
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
                semaphore.availablePermits())
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
                semaphore.availablePermits())
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
                semaphore.availablePermits())
    }

    @Test
    fun `acquire multiple permits with await calls`() {
        setPermits(-palantirCount)

        every { notZero.await() } answers {
            setPermits(getPermits() + 1)
        }

        semaphore.acquire()

        verifyOrder {
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }
        verify(exactly = palantirCount + 1) { notZero.await() }

        val expectedAvailablePermits = 0
        assertEquals(
                "Available permits should be $expectedAvailablePermits",
                expectedAvailablePermits,
                semaphore.availablePermits())
    }

    @Test
    fun `acquire one permit with an await call`() {
        setPermits(0)
        every { notZero.await() } answers {
            setPermits(1)
        }

        semaphore.acquire()

        verifyOrder {
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }

        val expectedAvailablePermits = 0
        assertEquals(
                "Available permits should be $expectedAvailablePermits",
                expectedAvailablePermits,
                semaphore.availablePermits())
    }

    @Test
    fun `acquire permit with await call interrupted`() {
        setPermits(0)
        every { notZero.await() } throws InterruptedException("Mock interrupt")
        assertFailsWith<InterruptedException> { semaphore.acquire() }

        verifyOrder {
            lock.lockInterruptibly()
            notZero.await()
            lock.unlock()
        }
        val expectedAvailablePermits = 0
        assertEquals(
                "Available permits should be $expectedAvailablePermits",
                expectedAvailablePermits,
                semaphore.availablePermits())
    }


    @Test
    fun `acquire permit uninterruptibly with permits available`() {
        every { semaphore.acquire() } returns Unit

        semaphore.acquireUninterruptibly()

        assertFalse(Thread.currentThread().isInterrupted)

        verifySequence {
            semaphore.acquireUninterruptibly()
            semaphore.acquire()
        }
    }

    @Test
    fun `acquire uninterruptibly should not directly modify permits`() {
        val permits = Random.nextInt(10, 20)
        permits.injectInto(semaphore)
        assertEquals(permits, semaphore.primitiveValue<Int>(Int::class))

        every { semaphore.acquire() } returns Unit

        semaphore.acquireUninterruptibly()

        val result = semaphore.primitiveValue<Int>(Int::class)
        assertEquals(permits, result)

        verifySequence {
            semaphore.acquireUninterruptibly()
            semaphore.acquire()
        }
    }

    @Test
    fun `acquire permit uninterruptibly should not be interruptible`() {
        every { semaphore.acquire() } throws InterruptedException("Mock exception") andThen { }

        semaphore.acquireUninterruptibly()

        verifySequence {
            semaphore.acquireUninterruptibly()
            semaphore.acquire()
            semaphore.acquire()
        }
    }

    @Test
    fun testAcquireUninterruptiblyWithInterrupt() {
        val interrupts = 3 + Random.nextInt(7)
        var loopCount = 0

        every { semaphore.acquire() } answers {
            val wasInterrupted = Thread.interrupted()
            assertFalse(wasInterrupted,
                    "Thread should have reset the Thread interrupted flag.")
            loopCount++
            if (loopCount <= interrupts) {
                throw InterruptedException("Mock interrupt.")
            }
        }

        try {
            semaphore.acquireUninterruptibly()
        } catch (e: Exception) {
            fail("Exception $e should not have been thrown.")
        }

        verify(exactly = interrupts + 1) { semaphore.acquire() }
        val wasInterrupted = Thread.interrupted()
        assertTrue(wasInterrupted, "Thread should have reset the Thread interrupted flag.")
    }

    @Test
    fun `acquire permit uninterruptibly should set interrupt flag before returning`() {
        Thread.currentThread().interrupt()
        every { semaphore.acquire() } throws InterruptedException("Mock exception") andThen {
            Thread.interrupted()
        }

        semaphore.acquireUninterruptibly()

        verify(exactly = 2) { semaphore.acquire() }

        assertTrue(Thread.currentThread().isInterrupted)
    }

    @Test
    fun `acquire permit uninterruptibly should set interrupt flag if interrupted`() {
        every { semaphore.acquire() } throws InterruptedException("Mock exception") andThen { }

        semaphore.acquireUninterruptibly()

        assertTrue(Thread.currentThread().isInterrupted)
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
                semaphore.availablePermits())
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
                semaphore.availablePermits())
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