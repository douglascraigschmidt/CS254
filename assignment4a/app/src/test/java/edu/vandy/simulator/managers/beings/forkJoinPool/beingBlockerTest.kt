package edu.vandy.simulator.managers.beings.forkJoinPool

import admin.AssignmentTests
import admin.injectInto
import edu.vandy.simulator.managers.palantiri.Palantir
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

class BeingBlockerTest : AssignmentTests() {

    @MockK
    lateinit var manager: ForkJoinPoolMgr

    @MockK
    lateinit var entryBarrier: CyclicBarrier

    @MockK
    lateinit var exitBarrier: CountDownLatch

    @Test
    fun testBlock() {
        val beingBlocker = spyk(BeingBlocker(manager))
        val palantir = mockk<Palantir>()
        entryBarrier.injectInto(beingBlocker)
        exitBarrier.injectInto(beingBlocker)

        every { beingBlocker.gazingIterations } returns 4
        every { beingBlocker.runGazingSimulation(4) } answers { }
        every { entryBarrier.await() } returns 1
        every { manager.acquirePalantir(any()) } returns palantir

        assertThat(beingBlocker.block()).isTrue

        verifyOrder {
            entryBarrier.await()
            beingBlocker.gazingIterations
            beingBlocker.runGazingSimulation(4)
            exitBarrier.countDown()
        }

        confirmVerified(exitBarrier, entryBarrier)
    }

    @Test
    fun `acquirePalantirAndGaze succeeds when a palantir is available`() {
        val beingBlocker = spyk(BeingBlocker(manager))
        val palantir = mockk<Palantir>()

        every { beingBlocker.acquirePalantir() } returns palantir
        every { beingBlocker.releasePalantir(palantir) } answers {}
        every { palantir.gaze(beingBlocker) } answers {}

        beingBlocker.acquirePalantirAndGaze()

        verifyOrder {
            beingBlocker.acquirePalantir()
            palantir.gaze(beingBlocker)
            beingBlocker.releasePalantir(palantir)
        }

        confirmVerified(palantir)
    }

    @Test
    fun `acquirePalantirAndGaze fails when a palantir is not available`() {
        val beingBlocker = spyk(BeingBlocker(manager))

        every { beingBlocker.acquirePalantir() } returns null
        every { beingBlocker.error(any<Throwable>()) } throws SimulatedException()
        every { beingBlocker.error(any<String>()) } throws SimulatedException()

        assertThrows(SimulatedException::class.java) {
            beingBlocker.acquirePalantirAndGaze()
        }

        verifyOrder {
            beingBlocker.acquirePalantir()
        }
    }
}
