package edu.vandy.simulator.managers.beings.completionService

import admin.AssignmentTests
import edu.vandy.simulator.managers.beings.BeingManager
import edu.vandy.simulator.managers.palantiri.Palantir
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test

class Assignment_3A_BeingCallableTest : AssignmentTests() {

    var beingManager: BeingManager<BeingCallable> = mockk()

    @MockK
    lateinit var palantir: Palantir

    @SpyK
    var being = BeingCallable(beingManager)

    @Test
    fun `call test`() {
        every { being.gazingIterations } returns 7
        every { being.runGazingSimulation(7) } returns Unit

        // Make the SUT call.
        val callResult = being.call()

        verify {
            being.gazingIterations
            being.runGazingSimulation(7)
        }

        // Check Results.
        assertThat(callResult).isSameAs(being)
    }

    @Test
    fun `acquirePalantirAndGaze test`() {
        every { being.acquirePalantir() } returns palantir
        every { palantir.gaze(any()) } returns Unit
        every { being.releasePalantir(palantir) } returns Unit

        being.acquirePalantirAndGaze()

        verifySequence {
            being.acquirePalantirAndGaze()
            being.acquirePalantir()
            palantir.gaze(being)
            being.releasePalantir(palantir)
        }

        confirmVerified(beingManager, palantir)
    }

    @Test
    fun `acquirePalantirAndGaze handles a null palantir`() {
        every { being.acquirePalantir() } returns null
        every { being.error(any<String>()) } returns Unit

        being.acquirePalantirAndGaze()

        verifySequence {
            being.acquirePalantirAndGaze()
            being.acquirePalantir()
            being.error(any<String>())
        }

        confirmVerified(beingManager, palantir)
    }

    @Test
    fun `acquirePalantirAndGaze handles a gazing exception`() {
        every { being.acquirePalantir() } returns palantir
        every { palantir.gaze(any()) } throws SimulatedException()
        every { being.releasePalantir(palantir) } returns Unit

        Assert.assertThrows(SimulatedException::class.java) {
            being.acquirePalantirAndGaze()
        }

        verifySequence {
            being.acquirePalantirAndGaze()
            being.acquirePalantir()
            being.releasePalantir(any())
        }
    }
}