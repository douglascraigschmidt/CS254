package edu.vandy.simulator.managers.palantiri.stampedLockFairSemaphore

import admin.AssignmentTests
import admin.ReflectionHelper.assertAnonymousFieldNotNull
import admin.injectInto
import admin.value
import edu.vandy.simulator.managers.palantiri.Palantir
import edu.vandy.simulator.managers.palantiri.concurrentMapFairSemaphore.FairSemaphore
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.locks.StampedLock
import kotlin.test.*

class Assignment_4_StampedLockFairSemaphoreMgrTest : AssignmentTests() {
    @MockK
    private lateinit var semaphore: FairSemaphore

    @MockK
    private lateinit var lock: StampedLock

    @SpyK
    private var palantirMap = mutableMapOf<Palantir, Boolean>()

    @SpyK
    private var manager = StampedLockFairSemaphoreMgr()

    @SpyK
    private var palantiri = mutableListOf<Palantir>()

    @Before
    fun setup() {
        repeat(PALANTIR_COUNT) {
            mockk<Palantir>().let { palantir ->
                palantirMap[palantir] = true
                palantiri.add(palantir)
            }
        }

        clearAllMocks()

        palantirMap.injectInto(manager)
        palantiri.injectInto(manager)
        lock.injectInto(manager)
        semaphore.injectInto(manager)
    }

    @Test
    fun buildModel() {
        val manager = spyk<StampedLockFairSemaphoreMgr>()
        assertNull(manager.value<Map<Palantir, Boolean>?>())
        assertNull(manager.value<FairSemaphore?>())
        every { manager.palantiri } returns palantiri

        manager.buildModel()

        // Call SUT method.
        manager.buildModel()
        val map = manager.value<Map<Palantir, Boolean>>()
        val semaphore = manager.value<FairSemaphore>()
        assertNotNull(semaphore)
        assertAnonymousFieldNotNull(manager, StampedLock::class.java)
        assertNotNull(map)
        assertEquals(PALANTIR_COUNT, map.size)
        assertThat(manager.value<Map<Palantir, Boolean>?>())
            .isInstanceOf(HashMap::class.java)

        confirmVerified(lock)
    }

    @Test
    fun `acquire when read stamp is automatically upgraded to a write stamp`() {
        val stamp = System.currentTimeMillis()
        every { lock.readLockInterruptibly() } returns stamp
        every { lock.tryConvertToWriteLock(any()) } answers
                {
                    assertEquals(
                        stamp, it.invocation.args[0],
                        "tryConvertToWriteLock called with wrong stamp value"
                    )
                    stamp
                }
        every { lock.writeLockInterruptibly() } returns stamp
        every { palantirMap[any()] } throws SimulatedException("Inefficient call")
        every { palantirMap.replace(any(), any()) } throws SimulatedException("Expensive call")

        // SUT
        val acquire = manager.acquire()
        val palantir = acquire

        assertNotNull(palantir, "Acquire should return a non-null Palantir")
        val locked = palantirMap.values.count { !it }
        assertEquals(1, locked, "Only 1 palantir should be locked")

        verifySequence {
            semaphore.acquire()
            lock.readLockInterruptibly()
            lock.tryConvertToWriteLock(stamp)
            lock.unlock(stamp)
        }

        verify { palantirMap.entries }

        verify(exactly = 0) {
            palantirMap[any()]
            palantirMap.replace(any(), any())
        }
    }

    @Test
    fun `acquire when only one palantir is available`() {
        // Lock all but the last Palantir.
        lockAllPalantiri()
        val unlockedPalantir = palantiri[PALANTIR_COUNT - 1]
        unlockPalantir(unlockedPalantir)

        val stamp = System.currentTimeMillis()
        every { lock.readLockInterruptibly() } returns stamp
        every { lock.tryConvertToWriteLock(any()) } answers
                {
                    assertEquals(
                        stamp, it.invocation.args[0],
                        "tryConvertToWriteLock called with wrong stamp value"
                    )
                    stamp
                }
        every { palantirMap[any()] } throws SimulatedException("Inefficient call")
        every { palantirMap.replace(any(), any()) } throws SimulatedException("Expensive call")

        // SUT
        val palantir = manager.acquire()

        assertNotNull(palantir, "Acquire should return a non-null Palantir")
        assertSame(
            unlockedPalantir, palantir,
            "The only available Palantir should be returned"
        )
        val locked = palantirMap.values.count { !it }
        assertEquals(
            PALANTIR_COUNT, locked,
            "All $PALANTIR_COUNT palantiri should be locked"
        )

        verifySequence {
            semaphore.acquire()
            lock.readLockInterruptibly()
            lock.tryConvertToWriteLock(stamp)
            lock.unlock(stamp)
        }
    }

    @Test
    fun `acquire all available palantiri`() {
        val stamp = System.currentTimeMillis()
        every { lock.readLockInterruptibly() } returns stamp
        every { lock.tryConvertToWriteLock(stamp) } returns stamp

        // SUT
        repeat(PALANTIR_COUNT) { i ->
            val palantir = manager.acquire()
            assertNotNull(palantir)
            val lockedCount = palantirMap.values.count { !it }
            assertTrue(palantirMap[palantir] == false)
            assertEquals(i + 1, lockedCount, "${i + 1} palantiri should have been locked.")
        }

        verify(exactly = PALANTIR_COUNT) {
            lock.readLockInterruptibly()
            lock.tryConvertToWriteLock(stamp)
            lock.unlock(stamp)
            semaphore.acquire()
        }
        verifyOrder {
            semaphore.acquire()
            lock.readLockInterruptibly()
            lock.tryConvertToWriteLock(stamp)
            lock.unlock(stamp)
        }

        confirmVerified(lock, semaphore)
    }

    @Test
    fun `acquire when an upgrade to write lock is necessary`() {
        val stamp = System.currentTimeMillis()
        every { lock.readLockInterruptibly() } returns stamp
        every { lock.writeLockInterruptibly() } returns stamp
        every { lock.tryConvertToWriteLock(stamp) } returnsMany (listOf(0L, stamp))
        every { palantirMap[any()] } throws SimulatedException("Inefficient call")
        every { palantirMap.replace(any(), any()) } throws SimulatedException("Expensive call")

        // There are many possible solutions involving different permutations
        // on calls to unlock, unlockRead, and unlockWrite. To handle all possible
        // permutations, a special UnlockCounter class instance is used to track
        // the count of calls to these 3 methods.
        val unlockCounter = UnlockCounter()
        every {
            lock.unlock(any())
        } answers { unlockCounter.unlock++ }

        every {
            lock.unlockRead(any())
        } answers { unlockCounter.unlockRead++ }

        every {
            lock.unlockWrite(any())
        } answers { unlockCounter.unlockWrite++ }

        // SUT
        val palantir = manager.acquire()

        assertNotNull(palantir, "Acquire should return a non-null Palantir")
        val locked = palantirMap.values.count { !it }
        assertEquals(1, locked, "Only 1 palantir should be locked")

        // An optimal solution should include a single call to unlockRead and a
        // single call to unlock, but another viable solution is to 2 calls to unlock.
        // Note that a 3rd possible solution is to call unlockRead followed by unlockWrite
        // but this is considered as being very unlikely and would also require an extra
        // boolean to keep track of whether unlockRead or unlockWrite should be called and
        // this is a sub-optimal solution and is therefore considered an error.
        if (unlockCounter.unlock + unlockCounter.unlockRead != 2) {
            fail("Expected 1 call to StampedLock unlockRead() and 1 call to unlock().")
        } else if (unlockCounter.unlockRead == 2) {
            fail("Expected only a single call to either either StampedLock unlockRead() or unlock().")
        }

        if (unlockCounter.unlock > 0) {
            verifySequence {
                semaphore.acquire()
                lock.readLockInterruptibly()
                lock.tryConvertToWriteLock(stamp)
                lock.unlockRead(stamp)
                lock.writeLockInterruptibly()
                lock.tryConvertToWriteLock(stamp)
                lock.unlock(stamp)
            }
        } else {
            verifySequence {
                semaphore.acquire()
                lock.readLockInterruptibly()
                lock.tryConvertToWriteLock(stamp)
                lock.writeLockInterruptibly()
                lock.unlockRead(stamp)
                lock.tryConvertToWriteLock(stamp)
                lock.unlock(stamp)
            }
        }
    }

    @Test
    fun `acquire uses correct map accessor methods`() {

    }

    @Test
    fun `release an in use palantir`() {
        val palantir = mockk<Palantir>()
        val stamp = System.currentTimeMillis()
        every { lock.writeLockInterruptibly() } returns stamp
        every { palantirMap.replace(any(), any()) } returns false
        palantirMap.injectInto(manager)

        // SUT
        manager.release(palantir)

        verifyOrder {
            lock.writeLockInterruptibly()
            palantirMap.replace(palantir, true)
            lock.unlockWrite(stamp)
            semaphore.release()
        }

        confirmVerified(lock, semaphore)
    }

    @Test
    fun `release handles a null palantir`() {
        assertFailsWith<IllegalArgumentException> {
            manager.release(null)
        }
        confirmVerified(lock, semaphore, palantirMap)
    }

    @Test
    fun `release handles an invalid palantir`() {
        val palantir = Palantir(manager)
        val stamp = System.currentTimeMillis()

        every { lock.writeLockInterruptibly() } returns stamp
        every { palantirMap.replace(palantir, true) } returns null

        // SUT
        assertFailsWith<IllegalArgumentException> { manager.release(palantir) }

        verifyOrder {
            lock.writeLockInterruptibly()
            palantirMap.replace(palantir, true)
            lock.unlockWrite(stamp)
        }

        confirmVerified(lock, semaphore)
    }

    @Test
    fun `release all acquired palantiri`() {
        val stamp = System.currentTimeMillis()

        lockAllPalantiri()
        clearAllMocks()

        every { lock.writeLockInterruptibly() } returns stamp

        // SUT
        palantiri.forEach { manager.release(it) }

        verify(exactly = PALANTIR_COUNT) {
            lock.writeLockInterruptibly()
            lock.unlockWrite(stamp)
            palantirMap.replace(any(), true)
            semaphore.release()
        }

        val unlocked = palantirMap.values.count { it }

        assertEquals(PALANTIR_COUNT, unlocked)
        confirmVerified(lock, semaphore)
    }

    @Test
    fun `release must call cancellable lock and semaphore methods in the correct order`() {
        val stamp = System.currentTimeMillis()
        lockAllPalantiri()

        clearAllMocks()
        val palantir = palantiri[0]

        every { lock.writeLockInterruptibly() } returns stamp
        every { palantirMap.replace(any(), true) } returns false

        manager.release(palantir)

        verifyOrder {
            lock.writeLockInterruptibly()
            palantirMap.replace(any(), true)
            lock.unlockWrite(stamp)
            semaphore.release()
        }

        confirmVerified(lock, semaphore)
    }

    @Test
    fun `release does not unlock the lock if the lock call is interrupted`() {
        every { lock.writeLockInterruptibly() } throws SimulatedException()

        val palantir = Palantir(manager)

        // SUT
        assertFailsWith<SimulatedException> { manager.release(palantir) }

        verify { lock.writeLockInterruptibly() }
        confirmVerified(lock, semaphore, palantirMap)
    }

    private fun lockAllPalantiri() {
        for (i in 0 until PALANTIR_COUNT) {
            val palantir = palantiri[i]
            palantirMap[palantir] = false
        }
    }

    private fun unlockPalantir(palantir: Palantir) {
        palantirMap[palantir] = true
    }

    private fun lockPalantir(palantir: Palantir) {
        palantirMap[palantir] = false
    }

    private inner class UnlockCounter {
        var unlock = 0
        var unlockRead = 0
        var unlockWrite = 0
    }

    companion object {
        private const val PALANTIR_COUNT = 5
    }
}
