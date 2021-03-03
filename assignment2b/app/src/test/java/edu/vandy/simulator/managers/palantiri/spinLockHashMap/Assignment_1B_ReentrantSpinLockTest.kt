package edu.vandy.simulator.managers.palantiri.spinLockHashMap

import admin.AssignmentTests
import admin.injectInto
import org.junit.Assert.*
import admin.value
import edu.vandy.simulator.utils.Student.Type.Graduate
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.random.Random
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Run with power mock and prepare the Thread
 * class for mocking it's static methods.
 */
class Assignment_1B_ReentrantSpinLockTest : AssignmentTests() {
    @MockK
    lateinit var isCancelled: Supplier<Boolean>

    @MockK
    lateinit var owner: AtomicReference<Thread>

    @SpyK
    internal var spinLock = ReentrantSpinLock()

    @Before
    fun before() {
        runAs(Graduate)
        owner.injectInto(spinLock)
    }

    @Test
    fun `tryLock locks the Atomic member`() {
        val localSpinLock = ReentrantSpinLock()
        val owner: AtomicReference<Thread> = localSpinLock.value(AtomicReference::class.java)
        assertNotNull(owner)
        assertTrue(localSpinLock.tryLock())
        assertEquals(Thread.currentThread(), owner.get())
    }

    @Test
    fun `tryLock only uses expected call`() {
        every { owner.compareAndSet(any(), any()) } returns false
        spinLock.tryLock()
        verify { owner.compareAndSet(any(), any()) }
    }

    @Test
    fun `lock takes ownership of lock when lock is not owned`() {
        // Trap call to AtomicReference compareAndSet and return
        // true so that spin lock (should) only call it once.
        every { owner.compareAndSet(null, Thread.currentThread()) } returns true

        // SUT
        val locked = spinLock.tryLock()

        assertEquals(true, locked)
        assertEquals(0, spinLock.recursionCount.toLong())

        verify(exactly = 1) { owner.compareAndSet(null, Thread.currentThread()) }
    }

    @Test
    fun `lock increments lock count when already held by calling thread`() {
        every { owner.get() } returns Thread.currentThread()

        // SUT
        spinLock.lock(isCancelled)

        verify { owner.get() }
        verify(exactly = 0) {
            owner.set(any())
            owner.compareAndSet(any(), any())
        }
        verify { spinLock.lock(isCancelled) }

        confirmVerified(owner, spinLock)
        assertEquals(1, spinLock.recursionCount.toLong())
    }

    @Test
    fun `lock is reentrant when called multiple times by owning thread`() {
        every { owner.get() } returns Thread.currentThread()

        // SUT
        repeat(3) {
            spinLock.lock(isCancelled)
        }

        verify(exactly = 3) { owner.get() }
        verify(exactly = 0) {
            isCancelled.get()
            owner.compareAndSet(any(), any())
        }
        verify { spinLock.lock(isCancelled) }

        confirmVerified(owner, spinLock)
        assertEquals(3, spinLock.recursionCount.toLong())
    }

    @Test
    fun `lock immediately takes ownership when lock not already held`() {
        every { owner.get() } returns null
        every { owner.compareAndSet(null, Thread.currentThread()) } returns true

        // SUT
        spinLock.lock(isCancelled)

        verify(atLeast = 1) { owner.get() }
        verify { owner.compareAndSet(null, Thread.currentThread()) }
        verify { spinLock.lock(isCancelled) }
        verify { spinLock.tryLock() }
        confirmVerified(owner, spinLock)
        assertEquals(0, spinLock.recursionCount.toLong())
    }

    @Test
    fun `lock spins when lock already held and then holds like once lock has been released`() {
        // Handles case where get() == null is used to
        // avoid calling tryLock's compareAndSet.
        every { owner.get() } returns null
        every { isCancelled.get() } returns false
        every { owner.compareAndSet(null, Thread.currentThread()) } returnsMany (listOf(false, true))

        // SUT
        spinLock.lock(isCancelled)

        verify(atLeast = 1) { owner.get() }
        verify { isCancelled.get() }
        verify(exactly = 2) { owner.compareAndSet(null, Thread.currentThread()) }
        verify { spinLock.lock(isCancelled) }
        verify(exactly = 2) { spinLock.tryLock() }
        confirmVerified(owner, spinLock)
        assertEquals(0, spinLock.recursionCount.toLong())
    }

    @Test
    fun `waiting for a held lock can be cancelled`() {
        // Handles case where get() == null is used to
        // avoid calling tryLock's compareAndSet.
        every { owner.get() } returns null
        every { isCancelled.get() } returns true
        every { owner.compareAndSet(null, Thread.currentThread()) } returns false
        // SUT

        assertFailsWith<CancellationException> { spinLock.lock(isCancelled) }

        verify { isCancelled.get() }
        verify(atLeast = 1) { owner.get() }
        verify { owner.compareAndSet(null, Thread.currentThread()) }
        verify { spinLock.lock(isCancelled) }
        verify { spinLock.tryLock() }
        verify { spinLock.lock(isCancelled) }
        confirmVerified(owner, spinLock)
        assertEquals(0, spinLock.recursionCount.toLong())
    }

    @Test
    fun `unlock releases a held lock`() {
        every { owner.get() } returns Thread.currentThread()

        // SUT
        spinLock.unlock()

        verify { owner.get() }
        verify { owner.set(null) }
        verify(exactly = 0) { owner.compareAndSet(any(), any()) }
        verify { spinLock.unlock() }
//        confirmVerified(owner, spinLock)
        assertEquals(0, spinLock.recursionCount.toLong())
    }

    @Test
    fun `unlock only release lock after recursion count reaches 0`() {
        every { owner.get() } returns Thread.currentThread()
        val count = Random.nextInt(from = 10, until = 20)
        (count - 1).injectInto(spinLock)

        // SUT
        repeat(count) {
            spinLock.unlock()
        }

        verify(exactly = count) { owner.get() }
        verify { owner.set(null) }
        verify { spinLock.unlock() }
        assertEquals(0, spinLock.recursionCount.toLong())

    }

    @Test
    fun `unlock should throw an exception when lock is not held`() {
        every { owner.get() } returns null

        // SUT
        assertFailsWith<IllegalMonitorStateException> { spinLock.unlock() }

        verify { owner.get() }
        verify { spinLock.unlock() }
    }
}