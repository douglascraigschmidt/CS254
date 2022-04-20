package edu.vandy.simulator.managers.beings.forkJoinPool

import admin.AssignmentTests
import admin.injectInto
import edu.vandy.simulator.managers.palantiri.Palantir
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

class BeingBlockerTest : AssignmentTests() {

    @MockK
    var manager: ForkJoinPoolMgr = mockk()

    @MockK
    lateinit var entryBarrier: CyclicBarrier

    @MockK
    lateinit var exitBarrier: CountDownLatch

    @SpyK
    var being = BeingBlocker(manager)

    @MockK
    lateinit var palantir: Palantir

    @Test
    fun testBlock() {
        entryBarrier.injectInto(being)
        exitBarrier.injectInto(being)

        every { being.gazingIterations } returns 4
        every { being.runGazingSimulation(4) } answers { }
        every { entryBarrier.await() } returns 1
        every { manager.acquirePalantir(any()) } returns palantir

        assertThat(being.block()).isTrue

        verifyOrder {
            entryBarrier.await()
            being.gazingIterations
            being.runGazingSimulation(4)
            exitBarrier.countDown()
        }

        confirmVerified(exitBarrier, entryBarrier)
    }

    @Test
    fun `acquirePalantirAndGaze succeeds when a palantir is available`() {
        every { being.acquirePalantir() } returns palantir
        every { being.releasePalantir(palantir) } answers {}
        every { palantir.gaze(being) } answers {}

        being.acquirePalantirAndGaze()

        verifyOrder {
            being.acquirePalantir()
            palantir.gaze(being)
            being.releasePalantir(palantir)
        }

        confirmVerified(palantir)
    }

    @Test
    fun `acquirePalantirAndGaze fails when a palantir is not available`() {
        every { being.acquirePalantir() } returns null
        every { being.error(any<Throwable>()) } throws SimulatedException()
        every { being.error(any<String>()) } throws SimulatedException()

        assertThrows(SimulatedException::class.java) {
            being.acquirePalantirAndGaze()
        }

        verifyOrder {
            being.acquirePalantir()
        }
    }

    @Test
    fun `acquirePalantirAndGaze handles a gazing exception`() {
        every { being.acquirePalantir() } returns palantir
        every { palantir.gaze(any()) } throws SimulatedException()
        every { being.releasePalantir(palantir) } returns Unit

        assertThrows(SimulatedException::class.java) {
            being.acquirePalantirAndGaze()
        }

        verifySequence {
            being.acquirePalantirAndGaze()
            being.acquirePalantir()
            being.releasePalantir(any())
        }
    }
}
