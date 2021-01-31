package edu.vandy.simulator.managers.beings.runnableThreads

import admin.AssignmentTests
import edu.vandy.simulator.managers.palantiri.Palantir
import io.mockk.*
import org.junit.Assert.assertThrows
import org.junit.Test

class Assignment_1A_BeingRunnableTest : AssignmentTests(timeoutSeconds = 10) {

    private class GazeException : RuntimeException()
    private class ErrorException : RuntimeException()

    @Test
    fun `BeingAcquirePalantirAndGazeMethod makes expected calls`() {
        val beingManager = mockk<RunnableThreadsMgr>()
        val palantir = mockk<Palantir>()
        val being = spyk(SimpleBeingRunnable(beingManager))

        every { being.acquirePalantir() } returns palantir
        every { being.releasePalantir(palantir) } returns Unit
        every { palantir.gaze(being) } returns Unit

        // Make the SUT call.
        being.acquirePalantirAndGaze()

        verifyOrder {
            being.acquirePalantir()
            palantir.gaze(being)
            being.releasePalantir(palantir)
        }

        confirmVerified(being, palantir, beingManager)
    }

    @Test
    fun `BeingRunGazingSimulationMethod handles null palantir`() {
        val beingManager = mockk<RunnableThreadsMgr>()
        val being = spyk(SimpleBeingRunnable(beingManager))

        every { being.acquirePalantir() } returns null
        every { being.error(any<String>()) } throws ErrorException()
        every { being.releasePalantir(any()) } returns Unit

        // Make the SUT call.
        assertThrows(ErrorException::class.java) {
            being.acquirePalantirAndGaze()
        }

        verifyOrder {
            being.acquirePalantir()
            being.error(any<String>())
        }

        confirmVerified(being, beingManager)
    }

    @Test
    fun `BeingRunGazingSimulationMethod handles exceptions`() {
        val beingManager = mockk<RunnableThreadsMgr>()
        val palantir = mockk<Palantir>()
        val being = spyk(SimpleBeingRunnable(beingManager))

        every { being.acquirePalantir() } returns palantir
        every { being.error(any<String>()) } throws ErrorException()
        every { palantir.gaze(being) } throws GazeException()
        every { being.releasePalantir(any()) } returns Unit

        // Make the SUT call.
        assertThrows(GazeException::class.java) {
            being.acquirePalantirAndGaze()
        }

        verifyOrder {
            being.acquirePalantir()
            palantir.gaze(being)
            being.releasePalantir(any())
        }

        confirmVerified(being, palantir, beingManager)
    }
}