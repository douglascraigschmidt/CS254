package edu.vandy.simulator.managers.palantiri.reentrantLockHashMapSimpleSemaphore

import admin.AssignmentTests
import admin.injectInto
import admin.value
import edu.vandy.simulator.managers.palantiri.Palantir
import edu.vandy.simulator.utils.Student.Type.Graduate
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collector
import java.util.stream.Stream
import kotlin.collections.HashMap
import kotlin.test.assertFailsWith

class Assignment_2B_ReentrantLockHashMapSimpleSemaphoreMgrTest : AssignmentTests() {
    companion object {
        private const val PALANTIR_COUNT = 5
    }

    @MockK
    lateinit var streamPalantir: Stream<Palantir>

    @MockK
    lateinit var optionalPalantir: Optional<Palantir>

    @MockK
    lateinit var palantir: Palantir

    @MockK
    lateinit var semaphore: SimpleSemaphore

    @MockK
    lateinit var entrySet: MutableSet<MutableMap.MutableEntry<Palantir, Boolean>>

    @MockK
    lateinit var streamEntrySet: Stream<MutableMap.MutableEntry<Palantir, Boolean>>

    @MockK
    lateinit var streamPalantiriList: Stream<Palantir>

    @MockK
    lateinit var lock: ReentrantLock

    @SpyK
    var manager = ReentrantLockHashMapSimpleSemaphoreMgr()

    @SpyK
    var palantirMap = mutableMapOf<Palantir, Boolean>()

    @SpyK
    private var palantirList = mutableListOf<Palantir>()

    @Before
    fun before() {
        repeat(PALANTIR_COUNT) {
            mockk<Palantir>().let {
                palantirList.add(it)
                palantirMap[it] = true
            }
        }

        // Erase all traces of add and put calls just
        // made to the list and map spys so that tests
        // will be working with clean (uncalled) spys.
        clearMocks(palantirList, palantirMap)

        // Allow field to be either Map or HashMap.
        try {
            // Preferred
            palantirMap.injectInto(manager, Map::class.java)
        } catch (t: Throwable) {
            // Acceptable but not preferred.
            palantirMap.injectInto(manager, HashMap::class.java)
        }

        palantirList.injectInto(manager)
        semaphore.injectInto(manager)
        (lock as Lock).injectInto(manager)
    }

    @Test
    fun `build model undergraduate and graduate test`() {
        // Note that the buildModel method does not use the
        // manager created in the @Before setup
        // method because it needs to test the real Semaphore,
        // ReentrantLock, and HashMap fields for proper initialization.
        val manager: ReentrantLockHashMapSimpleSemaphoreMgr = spyk()

        every { manager.palantiri } returns palantirList
        every { manager.palantiriCount } returns palantirList.size

        // Call SUT method.
        manager.buildModel()

        val semaphore = manager.value<SimpleSemaphore>()
        assertNotNull("Semaphore field should exist and not be null.", semaphore)
        assertEquals(PALANTIR_COUNT, semaphore.availablePermits())

        val lock = try {
            manager.value<ReentrantLock>()
        } catch (t: Throwable) {
            manager.value<Lock>()
        }
        assertNotNull("Lock field should exist and not be null.", lock)

        val palantiriMap = manager.value<Map<Palantir, Boolean>>()
        assertNotNull("Palantiri map should not be null.", palantiriMap)
        assertEquals("getPalantiriMap() should contain $PALANTIR_COUNT entries.",
                PALANTIR_COUNT, palantiriMap.size)
    }

    /**
     * Tests GRADUATE students use of Java 8 streams.
     */
    @Test
    fun `build model graduate only test`() {
        runAs(Graduate)

        every { manager.palantiri } returns palantirList
        every { palantirList.stream() } returns streamPalantiriList
        every { streamPalantiriList.collect(any<Collector<in Palantir, Any, Any>>()) } returns palantirMap
        every { palantirMap.size } returns PALANTIR_COUNT

        // Call SUT method.
        manager.buildModel()

        verifyOrder {
            manager.palantiri
            palantirList.stream()
            streamPalantiriList.collect(any<Collector<in Palantir, Any, Any>>())
            palantirMap.size
        }
        confirmVerified(semaphore, lock)
    }

    /**
     * Uses mManager instance created in the @Before setup method.
     */
    @Test
    fun `acquire locks and returns the first available palantir`() {
        val palantir = manager.acquire()
        assertNotNull("Acquire should return a non-null Palantir", palantir)

        val locked = palantirMap.values.count { !it }
        assertEquals("Only 1 palantir should be locked", 1, locked)

        verifyOrder {
            semaphore.acquire()
            lock.lockInterruptibly()
            lock.unlock()
        }

        confirmVerified(semaphore, lock)
    }

    /**
     * Uses mManager instance created in the @Before setup method.
     */
    @Test
    fun `acquire locks the only available palantir`() {
        lockAllPalantiri()
        val unlockedPalantir = palantirList[PALANTIR_COUNT - 1]
        unlockPalantir(unlockedPalantir)

        // Call SUT.
        val palantir = manager.acquire()

        assertNotNull("Acquire should return a non-null Palantir", palantir)

        val locked = palantirMap.values.count { !it }
        assertEquals("All $PALANTIR_COUNT palantiri should be locked", PALANTIR_COUNT, locked)
        assertSame("The only available Palantir should be returned", unlockedPalantir, palantir)

        verifyOrder {
            semaphore.acquire()
            lock.lockInterruptibly()
            lock.unlock()
        }
    }

    /**
     * Uses mManager instance created in the @Before setup method.
     */
    @Test
    fun `acquire all available palantiri`() {
        for (i in 1..PALANTIR_COUNT) {
            // Call SUT.
            val palantir = manager.acquire()

            assertNotNull("Acquire should return a non-null Palantir", palantir)
            val locked = palantirMap.values.count { !it }
            assertEquals("$i palantiri should be acquired (locked).", i, locked)
        }

        verify(exactly = PALANTIR_COUNT) {
            semaphore.acquire()
            lock.lockInterruptibly()
            lock.unlock()
        }

        verify(exactly = 0) { palantirMap.keys }
        verify(exactly = 0) { lock.lock() }

        verifyOrder {
            semaphore.acquire()
            lock.lockInterruptibly()
            lock.unlock()
        }
    }

    /**
     * Tests GRADUATE students use of Java 8 streams.
     */
    @Test
    fun `acquire all available palantiri graduate version`() {
        runAs(Graduate)

        every { palantirMap.entries } returns entrySet
        every { entrySet.stream() } returns streamEntrySet

        every {
            streamEntrySet.filter(any<Predicate<Map.Entry<Palantir, Boolean>>>())
        } returns streamEntrySet

        every {
            streamEntrySet.map(any<Function<Map.Entry<Palantir, Boolean>, Palantir>>())
        } returns streamPalantir

        every { streamPalantir.findFirst() } returns optionalPalantir
        every { streamPalantir.findAny() } returns optionalPalantir
        every { optionalPalantir.orElse(null) } returns palantir

        palantirMap.injectInto(manager)

        // Call SUT.
        val palantir = manager.acquire()

        assertEquals(this.palantir, palantir)
        verify {
            palantirMap.entries
            entrySet.stream()
            streamEntrySet.filter(any())
            streamEntrySet.map(any<Function<Map.Entry<Palantir, Boolean>, Palantir>>())
        }

        verify(exactly = 0) { palantirMap.keys }

        try {
            verify { streamPalantir.findFirst() }
        } catch (e: Exception) {
            verify { streamPalantir.findAny() }
        }

        verifyOrder {
            palantirMap.entries
            entrySet.stream()
            streamEntrySet.filter(any())
            streamEntrySet.map(any<Function<Map.Entry<Palantir, Boolean>, Palantir>>())
        }

        verify(exactly = 0) { palantirMap.keys }

        try {
            verify { streamPalantir.findFirst() }
        } catch (e: Exception) {
            verify { streamPalantir.findAny() }
        }
    }

    @Test
    fun `acquire does not call unlock if semaphore acquire fails`() {
        every { semaphore.acquire() } throws SimulatedException()

        // SUT
        assertFailsWith<SimulatedException> { manager.acquire() }

        verify { semaphore.acquire() }
        confirmVerified(lock)
    }

    @Test
    fun `acquire does not call unlock if lock fails`() {
        every { lock.lock() } throws SimulatedException()
        every { lock.lockInterruptibly() } throws SimulatedException()

        // SUT
        assertFailsWith<SimulatedException> { manager.acquire() }

        verify { semaphore.acquire() }

        try {
            verify { lock.lock() }
        } catch (t: Throwable) {
            verify { lock.lockInterruptibly() }
        }

        confirmVerified(lock)
    }

    @Test
    fun `release all acquired palantiri`() {
        lockAllPalantiri()

        // SUT
        palantirList.forEach { manager.release(it) }

        verify(exactly = PALANTIR_COUNT) {
            semaphore.release()
            lock.unlock()
            lock.lockInterruptibly()
            palantirMap.replace(any(), true)
        }

        val unlocked = palantirMap.values.count { it }

        assertEquals("All $PALANTIR_COUNT Palantiri should be unlocked.",
                PALANTIR_COUNT, unlocked)

        confirmVerified(lock, semaphore)
    }

    @Test
    fun `release must call cancellable lock and semaphore methods in the correct order`() {
        lockAllPalantiri()
        val palantir = palantirList[0]
        clearMocks(palantirList)

        every { palantirMap.replace(palantir, true) } returns false

        manager.release(palantir)

        verifyOrder {
            lock.lockInterruptibly()
            palantirMap.replace(palantir, true)
            lock.unlock()
            semaphore.release()
        }
        confirmVerified(lock, semaphore, palantirMap)
    }

    @Test
    fun `release does not unlock the lock if the lock call is interrupted`() {
        every { lock.lockInterruptibly() } throws SimulatedException()

        val palantir = Palantir(manager)

        // SUT
        assertFailsWith<SimulatedException> { manager.release(palantir) }

        verify { lock.lockInterruptibly() }
        confirmVerified(lock, semaphore, palantirMap)
    }

    @Test
    fun `release handles an invalid palantir`() {
        val palantir = Palantir(manager)

        every { palantirMap.replace(palantir, true) } returns null

        // SUT
        assertFailsWith<IllegalArgumentException> { manager.release(palantir) }

        verifyOrder {
            lock.lockInterruptibly()
            palantirMap.replace(palantir, true)
            lock.unlock()
        }
        confirmVerified(lock, semaphore, palantirMap)
    }

    @Test
    fun `release handles a null palantir`() {
        assertFailsWith<IllegalArgumentException> {
            manager.release(null)
        }
        confirmVerified(lock, semaphore, palantirMap)
    }

    private fun lockAllPalantiri() {
        repeat(PALANTIR_COUNT) {
            val palantir = palantirList[it]
            palantirMap[palantir] = false
        }
        clearMocks(palantirList, palantirMap)
    }

    private fun unlockPalantir(palantir: Palantir) {
        palantirMap[palantir] = true
        clearMocks(palantirMap)
    }

    private fun lockPalantir(palantir: Palantir) {
        palantirMap[palantir] = false
        clearMocks(palantirMap)
    }
}