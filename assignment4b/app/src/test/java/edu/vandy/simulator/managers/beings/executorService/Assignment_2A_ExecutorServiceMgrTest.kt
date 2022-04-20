package edu.vandy.simulator.managers.beings.executorService

import admin.AssignmentTests
import admin.injectInto
import admin.value
import edu.vandy.simulator.utils.Student.Type.Graduate
import edu.vandy.simulator.utils.Student.Type.Undergraduate
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collector
import java.util.stream.Stream

@Suppress("SimplifyBooleanWithConstants")
class Assignment_2A_ExecutorServiceMgrTest : AssignmentTests() {
    companion object {
        private const val BEING_COUNT = 5
    }

    @MockK
    private lateinit var future: Future<BeingCallable>

    @MockK
    private lateinit var beings: List<BeingCallable>

    @MockK
    private lateinit var futures: List<Future<BeingCallable>>

    @MockK
    private lateinit var executor: ExecutorService

    @MockK
    private lateinit var beingCallableStream: Stream<BeingCallable>

    @MockK
    private lateinit var futureStream: Stream<Future<BeingCallable>>

    @SpyK
    private var manager = ExecutorServiceMgr()

    // Used to count lambda errors caught by try/catch block
    private var mErrorCount = 0

    @Before
    fun before() {
        futures.injectInto(manager)
        executor.injectInto(manager)
    }

    @Test
    fun testNewBeing() {
        val beingCallable = manager.newBeing()
        assertNotNull("newBeing should not return null.", beingCallable)
    }

    @Test
    fun testRunSimulation() {
        every { manager.beginBeingThreadPool() } returns Unit
        every { manager.awaitCompletionOfFutures() } returns Unit
        every { manager.shutdownNow() } returns Unit

        manager.runSimulation()

        verifyOrder {
            manager.beginBeingThreadPool()
            manager.awaitCompletionOfFutures()
            manager.shutdownNow()
        }
    }

    @Test(expected = SimulatedException::class)
    fun `beginBeingThreadPool must initialize futures list`() {
        runAs(Undergraduate)
        val mock = spyk<ExecutorServiceMgr>()
        every { mock.threadCount } returns BEING_COUNT
        every { mock.createExecutorService(BEING_COUNT) } returns executor
        every { mock.beings } answers {
            assertThat(mock.value<List<Future<BeingCallable>>>()).isNotNull
            throw SimulatedException()
        }

        mock.beginBeingThreadPool()
    }

    /**
     * Test common to both UNDERGRADUATE And GRADUATE Assignments.
     */
    @Test
    fun testBeginBeingThreadPool() {
        val mockBeings = createMockBeingList(BEING_COUNT)

        every { manager.beings } returns mockBeings
        every { manager.beingCount } returns mockBeings.size
        every { manager.createExecutorService(mockBeings.size) } returns executor
        every { manager.threadCount } returns BEING_COUNT
        every { executor.submit(any<BeingCallable>()) } returns future

        manager.beginBeingThreadPool()

        verify(exactly = mockBeings.size) {
            executor.submit(any<BeingCallable>())
        }

        val futureList = manager.value<MutableList<Future<BeingCallable>>>()

        assertNotNull(
            "Unable to access List<Future<BeingCallable>> field in " +
                    "ExecutorServiceMgr class.",
            futureList
        )
        assertEquals(
            "Futures list should contain $BEING_COUNT threads.",
            BEING_COUNT.toLong(),
            futureList.size.toLong()
        )
        for (future in futureList) {
            assertEquals(
                "Unexpected future value in Futures List",
                this.future,
                future
            )
        }

        verify {
            manager.createExecutorService(mockBeings.size)
        }
    }

    /**
     * Test for GRADUATE use of Java 8 Streams.
     */
    @Test
    fun testBeginBeingThreadPoolGraduate() {
        runAs(Graduate)

        every { beings.size } returns BEING_COUNT
        every { beings.stream() } returns beingCallableStream
        every { manager.beings } returns beings
        every { manager.createExecutorService(BEING_COUNT) } returns executor
        every { manager.beingCount } returns BEING_COUNT
        every { manager.threadCount } returns BEING_COUNT
        every {
            beingCallableStream.map(any<Function<BeingCallable, Future<BeingCallable>>>())
        } returns futureStream

        val futureListMock: List<Future<BeingCallable>> = ArrayList()
        every {
            futureStream.collect(any<Collector<in Future<BeingCallable>, Any, Any>>())
        } returns futureListMock

        manager.beginBeingThreadPool()

        verify {
            manager.createExecutorService(BEING_COUNT)
            beings.stream()
            beingCallableStream.map(any<Function<BeingCallable, Future<BeingCallable>>>())
            futureStream.collect(any<Collector<in Future<BeingCallable>, Any, Any>>())
        }

        val futureList = manager.value<List<Future<BeingCallable>>>()

        assertNotNull(
            "Unable to access List<Future<BeingCallable>> field in " +
                    "ExecutorServiceMgr class.",
            futureList
        )

        assertSame(
            "Futures list should contain $BEING_COUNT threads.",
            futureListMock,
            futureList
        )
    }

    /**
     * Test common to both UNDERGRADUATE And GRADUATE Assignments.
     */
    @Test
    fun testAwaitCompletionOfFutures() {
        val futureList = createMockFutureList(BEING_COUNT, true)
        futureList.injectInto(manager)

        // Call SUT.
        manager.awaitCompletionOfFutures()

        futureList.forEach {
            try {
                verify { it.get() }
            } catch (e: Exception) {
                mErrorCount++
            }
        }

        check(mErrorCount <= 0) {
            "Call to Future.get() failed " + mErrorCount +
                    if (mErrorCount == 1) " time" else "times"
        }
    }

    /**
     * Test for GRADUATE use of Java 8 Streams.
     */
    @Test
    fun testAwaitCompletionOfFuturesGraduate() {
        runAs(Graduate)

        futures.injectInto(manager)
        // Call SUT.
        manager.awaitCompletionOfFutures()

        // Support new awaitCompletionOfFutures method.
        verify { futures.forEach(any<Consumer<in Future<BeingCallable>>>()) }
    }

    @Test
    fun `shutdownNow only cancels the correct futures`() {
        val futureList = createMockFutureList(1010, false)
        val random = Random()
        var count = 0
        futureList.forEach {
            val b1 = random.nextBoolean()
            val b2 = random.nextBoolean()
            every { it.isCancelled } returns b2
            every { it.isDone } returns b1
            every { it.cancel(any()) } answers {
                if (b1 or b2) count++
                true
            }
        }
        futureList.injectInto(manager)

        every { executor.shutdownNow() } returns emptyList()

        manager.shutdownNow()

        verify { manager.shutdownNow() }

        if (count != 0) {
            println("$count future${if (count != 1) "s" else ""} should not have been cancelled.")
            fail("$count future${if (count != 1) "s" else ""} should not have been cancelled.")
        }
    }

    @Test
    fun `shutdownNow uses expected chained methods`() {
        runAs(Graduate)

        every { futures.stream() } returns futureStream
        every { futureStream.filter(any()) } returns futureStream
        every { futureStream.forEach(any()) } returns Unit
        every { executor.shutdownNow() } returns emptyList()

        manager.shutdownNow()

        verifySequence {
            futures.stream()
            futureStream.filter(any())
            futureStream.forEach(any())
            executor.shutdownNow()
        }
    }

    private fun createMockFutureList(count: Int, mockGet: Boolean): List<Future<BeingCallable>> =
        (1..count).map {
            mockk<Future<BeingCallable>>().apply {
                try {
                    if (mockGet) {
                        every { get() } returns mockk<BeingCallable>()
                    }
                } catch (e: Exception) {
                }
            }
        }

    private fun createMockBeingList(count: Int): List<BeingCallable> =
        (1..count).map {
            @Suppress("RemoveExplicitTypeArguments")
            mockk<BeingCallable>()
        }
}