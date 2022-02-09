package edu.vandy.simulator.managers.palantiri.spinLockHashMap

import admin.AssignmentTests
import admin.injectInto
import admin.value
import edu.vandy.simulator.managers.palantiri.Palantir
import edu.vandy.simulator.utils.Assignment.isGraduate
import edu.vandy.simulator.utils.Assignment.isUndergraduate
import edu.vandy.simulator.utils.Student
import edu.vandy.simulator.utils.Student.Type.Graduate
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.Semaphore
import java.util.function.Consumer
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
class Assignment_1B_SpinLockHashMapMgrTest : AssignmentTests() {
    @MockK
    internal lateinit var cancellableLockMock: CancellableLock

    @MockK
    lateinit var semaphoreMock: Semaphore

    private val manager = spyk<SpinLockHashMapMgr>()

    private val spinLock: CancellableLock
        get() = manager.value(CancellableLock::class.java)

    private val availablePalantiri: Semaphore
        get() = manager.value(Semaphore::class.java)

    // In order to put mock entries in this map, it can't be a mock.
    @SpyK
    private var palantiriMap = HashMap<Palantir, Boolean>(PALANTIRI_COUNT)

    // In order to put mock entries in this list, it can't be a mock.
    private var palantiri = mutableListOf<Palantir>()

    @Before
    fun before() {
        repeat(PALANTIRI_COUNT) {
            mockk<Palantir>().let {
                palantiri.add(it)
                palantiriMap[it] = true
            }
        }

        // mPalantiriMap and mPalantiri can't be mocked themselves,
        // only their contents can be mocked.
        palantiriMap.injectInto<Map<Palantir, Boolean>>(manager)
        palantiri.injectInto(manager)

        cancellableLockMock.injectInto(manager)
        semaphoreMock.injectInto(manager)
    }

    @Test
    fun buildModelUndergraduate() {
        runAs(Student.Type.Undergraduate)
        buildModel()
    }

    @Test
    fun buildModelGraduate() {
        runAs(Graduate)
        buildModel()
    }

    @Test
    fun `buildModel creates proper palantiri hashmap`() {
        // Note that the buildModel method does not use the
        // SpinLockHashMapMgr created in the @Before setup
        // method because it needs to test the real Semaphore,
        // SpinLock, and Map fields for proper initialization.
        val mockPalantiri = (1..PALANTIRI_COUNT).map { mockk<Palantir>() }

        every { manager.palantiri } returns mockPalantiri

        // Call SUT method.
        manager.buildModel()

        if (isUndergraduate()) {
            assertTrue(spinLock is SpinLock)
        } else if (isGraduate()) {
            assertTrue(spinLock is ReentrantSpinLock)
        }

        assertEquals(
                "getPalantiriMap() should contain $PALANTIRI_COUNT entries.",
                PALANTIRI_COUNT.toLong(),
                manager.value<Map<Palantir, Boolean>>().size.toLong())
    }

    private fun buildModel() {
        val list = mockk<List<Palantir>>(relaxed = true)
        list.injectInto(manager)

        val palantiriMap = mockk<HashMap<Palantir, Boolean>>().also {
            it.injectInto<Map<Palantir, Boolean>>(manager)
        }

        every { manager.palantiri } returns list
        every { list.forEach(any<Consumer<Palantir>>()) } returns Unit
        every { list.size } returns PALANTIRI_COUNT

        // Call SUT method.
        manager.buildModel()

        verify {
            list.size
            manager.palantiri
        }

        if (isUndergraduate()) {
            try {
                verify { list.iterator() }
            } catch (t: Throwable) {
                verify { list.forEach(any<Consumer<Palantir>>()) }
            }

            assertTrue(spinLock is SpinLock)
        } else if (isGraduate()) {
            verify { list.forEach(any<Consumer<Palantir>>()) }
            assertTrue(spinLock is ReentrantSpinLock)
        }

        assertNotNull(availablePalantiri)
        assertEquals(
                "The available palantiri semaphore should 0 permits.",
                0,
                availablePalantiri.queueLength.toLong())
        assertNotNull(
                "getSpinLock() accessor should not return null.",
                spinLock)
        assertNotNull(
                "getPalantiriMap() should not return null.",
                palantiriMap)
    }

    @Test
    fun `acquire a palantir when all palantiri are available`() {
        // SUT
        val palantir = manager.acquire()

        assertNotNull("Acquire should return a non-null Palantir", palantir)
        val locked = palantiriMap.values.count { !it }
        assertEquals("Only 1 palantir should be locked", 1, locked)
        verify {
            semaphoreMock.acquire()
            cancellableLockMock.lock(any())
            cancellableLockMock.unlock()
        }

        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    /**
     * Uses mManager instance created in the @Before setup method.
     */
    @Test
    fun `acquire a palantir when only one palantir is available`() {
        lockAllPalantiri()
        val unlockedPalantir = palantiri[PALANTIRI_COUNT - 1]
        unlockPalantir(unlockedPalantir)

        // SUT
        val palantir = manager.acquire()

        assertNotNull("Acquire should return a non-null Palantir", palantir)
        val locked = palantiriMap.values.count { !it }
        assertEquals("All $PALANTIRI_COUNT palantiri should be locked", PALANTIRI_COUNT, locked)
        assertSame("The only available Palantir should be returned", unlockedPalantir, palantir)

        verifyOrder {
            semaphoreMock.acquire()
            cancellableLockMock.lock(any())
            cancellableLockMock.unlock()
        }

        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    /**
     * Uses mManager instance created in the @Before setup method.
     */
    @Test
    fun `acquire all available palantiri`() {
        for (i in 1..PALANTIRI_COUNT) {
            // SUT
            val palantir = manager.acquire()

            assertNotNull("Acquire should return a non-null Palantir", palantir)
            val locked = palantiriMap.values.count { !it }
            assertEquals("$i palantiri should be acquired (locked).", i, locked)
        }

        verify(exactly = PALANTIRI_COUNT) {
            semaphoreMock.acquire()
            cancellableLockMock.lock(any())
            cancellableLockMock.unlock()
        }

        verifyOrder {
            semaphoreMock.acquire()
            cancellableLockMock.lock(any())
            cancellableLockMock.unlock()
        }

        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `acquire does not call unlock if semaphore acquire fails`() {
        every { semaphoreMock.acquire() } throws SimulatedException()

        // SUT
        assertFailsWith<SimulatedException> { manager.acquire() }

        verify { semaphoreMock.acquire() }

        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `acquire does not call unlock if lock fails`() {
        every { cancellableLockMock.lock(any()) } throws SimulatedException()

        // SUT
        assertFailsWith<SimulatedException> { manager.acquire() }

        verify {
            semaphoreMock.acquire()
            cancellableLockMock.lock(any())
        }
        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `release a null palantir`() {
        assertFailsWith<IllegalArgumentException> { manager.release(null) }

        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `release a locked palantir`() {
        val palantiriMap = mockk<HashMap<Palantir, Boolean>>().also {
            it.injectInto<Map<Palantir, Boolean>>(manager)
        }
        val palantir = mockk<Palantir>()

        every { palantiriMap.replace(palantir, true) } returns false

        // SUT
        manager.release(palantir)

        verifyOrder {
            cancellableLockMock.lock(any())
            palantiriMap.replace(palantir, true)
            cancellableLockMock.unlock()
            semaphoreMock.release()
        }

        confirmVerified(palantiriMap, palantir, cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `release an unlocked palantir`() {
        val palantiriMap = mockk<HashMap<Palantir, Boolean>>().also {
            it.injectInto<Map<Palantir, Boolean>>(manager)
        }
        val palantir = mockk<Palantir>()

        every { palantiriMap.replace(palantir, true) } returns true

        // SUT
        assertFailsWith<IllegalArgumentException> { manager.release(palantir) }

        verifyOrder {
            cancellableLockMock.lock(any())
            palantiriMap.replace(palantir, true)
            cancellableLockMock.unlock()
        }

        verify(exactly = 0) { semaphoreMock.release() }

        confirmVerified(palantiriMap, palantir, cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `release all acquired palantiri`() {
        lockAllPalantiri()

        // SUT
        palantiri.forEach { manager.release(it) }

        verify(exactly = PALANTIRI_COUNT) {
            semaphoreMock.release()
            cancellableLockMock.unlock()
            cancellableLockMock.lock(any())
        }

        val unlocked = palantiriMap.values.count { it }

        assertEquals("All $PALANTIRI_COUNT Palantiri should be unlocked.",
                PALANTIRI_COUNT, unlocked)

        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `release must call cancellable lock and semaphore methods in the correct order`() {
        lockAllPalantiri()
        manager.release(palantiri[0])

        verifySequence {
            cancellableLockMock.lock(any())
            cancellableLockMock.unlock()
            semaphoreMock.release()
        }
    }

    @Test
    fun `release does not unlock the lock if the lock call is interrupted`() {
        every { cancellableLockMock.lock(any()) } throws SimulatedException()

        val palantir = Palantir(manager)

        // SUT
        assertFailsWith<SimulatedException> { manager.release(palantir) }

        verify { cancellableLockMock.lock(any()) }
        confirmVerified(cancellableLockMock, semaphoreMock)
    }

    @Test
    fun `release never throws a NullPointerException`() {
        val palantir = Palantir(manager)

        // SUT
        assertFailsWith<IllegalArgumentException> { manager.release(palantir) }
    }

    private fun lockAllPalantiri() {
        // Lock all but the last Palantir in the Map.
        for (i in 0 until PALANTIRI_COUNT) {
            val palantir = palantiri[i]
            palantiriMap[palantir] = false
        }
    }

    private fun unlockPalantir(palantir: Palantir) {
        palantiriMap[palantir] = true
    }

    private fun lockPalantir(palantir: Palantir) {
        palantiriMap[palantir] = false
    }

    companion object {
        private const val PALANTIRI_COUNT = 5
    }
}