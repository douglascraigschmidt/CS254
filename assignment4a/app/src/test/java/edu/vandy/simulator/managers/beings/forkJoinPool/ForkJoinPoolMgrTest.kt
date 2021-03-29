package edu.vandy.simulator.managers.beings.forkJoinPool

import admin.AssignmentTests
import admin.injectInto
import admin.value
import io.mockk.*
import io.mockk.impl.annotations.SpyK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ForkJoinPool

class ForkJoinPoolMgrTest : AssignmentTests() {
    @SpyK
    var manager = ForkJoinPoolMgr()

    @Test
    fun `new Being is created`() {
        assertThat(manager.newBeing()).isNotNull
    }

    @Test
    fun `simulation runs`() {
        every { manager.initializeBarriers() } answers {}
        every { manager.processAllBeings() } answers {}

        manager.runSimulation()

        verifyOrder {
            manager.initializeBarriers()
            manager.processAllBeings()
        }
    }

    @Test
    fun `barriers are initialized`() {
        val being = mockk<BeingBlocker>()
        val beings: List<BeingBlocker> = listOf(being, being, being)

        every { manager.beings } returns beings
        every { being.setBarriers(any(), any()) } answers {}
        every { manager.beingCount } returns beings.size

        manager.initializeBarriers()

        verify {
            manager.beings
            being.setBarriers(any(), any())
            manager.beingCount
        }

        val cyclicBarrier: CyclicBarrier = manager.value(CyclicBarrier::class.java)
        val countDownLatch: CountDownLatch = manager.value(CountDownLatch::class.java)

        assertThat(cyclicBarrier).isNotNull
        assertThat(cyclicBarrier.parties).isEqualTo(beings.size + 1L)
        assertThat(countDownLatch).isNotNull
        assertThat(countDownLatch.count).isEqualTo(beings.size.toLong())

        verify(exactly = beings.size) { being.setBarriers(any(), any()) }
    }

    @Test
    fun `all beings are processed`() {
        val being = mockk<BeingBlocker>()
        val beings: List<BeingBlocker> = listOf(being, being, being)
        val cyclicBarrier = mockk<CyclicBarrier>()
        val countDownLatch = mockk<CountDownLatch>()
        val commonPool = spyk<ForkJoinPool>()

        mockkStatic(ForkJoinPool::class)

        cyclicBarrier.injectInto(manager)
        countDownLatch.injectInto(manager)

        every { manager.beings } returns beings
        every { manager.beingCount } returns beings.size
        every { ForkJoinPool.commonPool() } returns commonPool
        every { being.isReleasable } returns true
        every { cyclicBarrier.await() } returns 0
        every { countDownLatch.await() } answers {}

        manager.processAllBeings()

        verify { manager.beings }
        verify(exactly = beings.size) {
            ForkJoinPool.commonPool()
            commonPool.execute(any<Runnable>())
            ForkJoinPool.managedBlock(being)
        }
        verifyOrder {
            cyclicBarrier.await()
            countDownLatch.await()
        }
    }
}
