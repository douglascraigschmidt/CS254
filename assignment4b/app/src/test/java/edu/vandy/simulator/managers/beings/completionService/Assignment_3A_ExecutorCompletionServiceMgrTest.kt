package edu.vandy.simulator.managers.beings.completionService

import admin.AssignmentTests
import admin.injectInto
import admin.value
import edu.vandy.simulator.utils.Student
import edu.vandy.simulator.utils.Student.Type.Undergraduate
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.*
import java.util.function.IntFunction
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.IntStream
import java.util.stream.Stream

class Assignment_3A_ExecutorCompletionServiceMgrTest : AssignmentTests() {
    @SpyK
    private var manager = ExecutorCompletionServiceMgr()

    @Test
    fun `newBeing creates a new callable`() {
        assertThat(manager.newBeing()).isNotNull
    }

    @Test
    fun `runSimulation calls expected helpers`() {
        every { manager.beginBeingThreadPool() } returns Unit
        every { manager.awaitCompletionOfFutures() } returns Unit
        every { manager.shutdownNow() } returns Unit

        manager.runSimulation()

        verifySequence {
            manager.runSimulation()
            manager.beginBeingThreadPool()
            manager.awaitCompletionOfFutures()
            manager.shutdownNow()
        }
    }

    @Test
    fun `Expected member fields must exist`() {
        val instance = ExecutorCompletionServiceMgr()
        try {
            (null as ExecutorService?).injectInto(instance)
            (null as CompletionService<*>?).injectInto(instance)
        } catch (t: Throwable) {
            fail("Missing class members.")
        }
    }

    @Test
    fun `createExecutorService creates the appropriate thread pool`() {
        mockkStatic(Executors::class)
        val pool = mockk<ExecutorService>()
        every { Executors.newCachedThreadPool() } returns pool
        assertThat(manager.createExecutorService()).isSameAs(pool)
    }

    @Test
    fun `createExecutorCompletionService returns the expected value`() {
        val mock = mockk<ExecutorService>()
        mockkConstructor(ExecutorCompletionService::class)
        assertThat(manager.createExecutorCompletionService(mock)).isNotNull
        verify { constructedWith<ExecutorCompletionService<*>>(EqMatcher(mock)) }
    }

    @Test
    fun `beginBeingThreadPool initialized required members`() {
        val mock1 = mockk<ExecutorService>()
        val mock2 = mockk<CompletionService<BeingCallable>>()
        val mock3 = mockk<BeingCallable>()
        val mock4 = mockk<Future<BeingCallable>>()
        every { manager.createExecutorService() } returns mock1
        every { manager.createExecutorCompletionService(mock1) } returns mock2
        every { manager.beings } returns listOf(mock3)
        every { mock2.submit(mock3) } answers { mock4 }
        manager.beginBeingThreadPool()
        assertThat(manager.value<ExecutorService>()).isSameAs(mock1)
        assertThat(manager.value<CompletionService<BeingCallable>>()).isSameAs(mock2)
        verify(exactly = 1) {
            manager.createExecutorService()
            manager.createExecutorCompletionService(mock1)
            manager.beings
            mock2.submit(mock3)
        }
    }

    //    /**
//     * Test common to both UNDERGRADUATE And GRADUATE Assignments.
//     */
//    @Test
//    fun testAwaitCompletionOfFutures() {
//        val beingCallableMock = mock<BeingCallable>()
//        val mockBeings = createMockBeingList(BEING_COUNT)
//        val size = mockBeings.size
//        whenever(manager.beings).thenReturn(mockBeings)
//        whenever(manager.beingCount).thenReturn(size)
//        whenever(mCompletionServiceMock.take()).thenReturn(mFutureMock)
//        whenever(mFutureMock.get()).thenReturn(beingCallableMock)
//        ReflectionHelper.injectValueIntoFirstMatchingField(
//                manager, mExecutorMock, ExecutorService::class.java)
//        ReflectionHelper.injectValueIntoFirstMatchingField(
//                manager, mCompletionServiceMock, CompletionService::class.java)
//        doCallRealMethod().whenever(manager).awaitCompletionOfFutures()
//
//        // Call SUT
//        manager.awaitCompletionOfFutures()
//
//        try {
//            verify(manager, atLeast(2)).beingCount
//        } catch (t: Throwable) {
//            verify(mockBeings, atLeastOnce()).size
//        }
//
//        verify(mCompletionServiceMock, times(BEING_COUNT)).take()
//        verify(mFutureMock, times(BEING_COUNT)).get()
//    }
//
//    @Test
//    fun testAwaitCompletionOfFuturesWithExceptionUndergraduates() {
//        runAs(Undergraduate)
//
//        val mockBeings = createMockBeingList(BEING_COUNT)
//        val size = mockBeings.size
//        whenever(manager.beings).thenReturn(mockBeings)
//        whenever(manager.beingCount).thenReturn(size)
//        whenever(mCompletionServiceMock.take()).thenReturn(mFutureMock)
//        whenever(mFutureMock.get()).thenThrow(SimulatedException())
//        ReflectionHelper.injectValueIntoFirstMatchingField(
//                manager, mExecutorMock, ExecutorService::class.java)
//        ReflectionHelper.injectValueIntoFirstMatchingField(
//                manager, mCompletionServiceMock, CompletionService::class.java)
//        doCallRealMethod().whenever(manager).awaitCompletionOfFutures()
//
//        // Call SUT
//        assertFailsWith<RuntimeException> { manager.awaitCompletionOfFutures() }
//
//        try {
//            verify(manager, atLeast(2)).beingCount
//        } catch (t: Throwable) {
//            verify(mockBeings, atLeastOnce()).size
//        }
//
//        verify(mCompletionServiceMock, times(1)).take()
//        verify(mFutureMock, times(1)).get()
//    }
//
//    @Test
//    fun testAwaitCompletionOfFuturesWithExceptionGraduates() {
//        runAs(Graduate)
//
//        val mockBeings = createMockBeingList(BEING_COUNT)
//        val size = mockBeings.size
//        whenever(manager.beings).thenReturn(mockBeings)
//        whenever(manager.beingCount).thenReturn(size)
//        whenever(mCompletionServiceMock.take()).thenReturn(mFutureMock)
//        whenever(mFutureMock.get()).thenThrow(SimulatedException())
//        ReflectionHelper.injectValueIntoFirstMatchingField(
//                manager, mExecutorMock, ExecutorService::class.java)
//        ReflectionHelper.injectValueIntoFirstMatchingField(
//                manager, mCompletionServiceMock, CompletionService::class.java)
//        doCallRealMethod().whenever(manager).awaitCompletionOfFutures()
//
//        // Call SUT
//        assertFailsWith<RuntimeException> { manager.awaitCompletionOfFutures() }
//
//        try {
//            verify(manager, atLeast(2)).beingCount
//        } catch (t: Throwable) {
//            verify(mockBeings, atLeastOnce()).size
//        }
//
//        verify(mCompletionServiceMock, times(1)).take()
//        verify(mFutureMock, times(1)).get()
//    }
//

    @Test
    fun `awaitCompletionOfFutures has expected undergraduate solution`() {
        runAs(Undergraduate)

        val mock2 = mockk<CompletionService<BeingCallable>>()
        val mock3 = mockk<Future<BeingCallable>>()
        val callables = (1..5).map { mockk<BeingCallable>() }
        callables.injectInto(manager)
        mock2.injectInto(manager)

        every { mock3.get() } returns callables[0]
        every { mock2.take() } returns mock3
        every { manager.beings } returns callables

        manager.awaitCompletionOfFutures()

        verify {
            mock2.take()
            mock3.get()
            manager.beings
        }
    }

    @Test
    fun `awaitCompletionOfFutures undergraduate solution handles errors`() {
        runAs(Undergraduate)

        val mock2 = mockk<CompletionService<BeingCallable>>()
        val mock3 = mockk<Future<BeingCallable>>()
        val callables = (1..5).map {
            if (it <= 4) {
                mockk<BeingCallable>()
            } else {
                null
            }
        }
        callables.injectInto(manager)
        mock2.injectInto(manager)

        every { mock3.get() } returnsMany callables
        every { mock2.take() } returns mock3
        every { manager.beings } returns callables

        val exception = assertThrows(RuntimeException::class.java) {
            manager.awaitCompletionOfFutures()
        }

        assertThat(exception.message).isEqualTo(
            "expected ${callables.size} but got ${callables.size - 1}"
        )
    }

    @Test
    fun `awaitCompletionOfFutures has expected graduate solution`() {
        runAs(Student.Type.Graduate)

        mockkStatic(IntStream::class)
        val mock3 = mockk<CompletionService<BeingCallable>>()
        val mock8 = mockk<BeingCallable>()
        val mock2 = mockk<Stream<Future<BeingCallable>>>()
        val mock0 = mockk<Future<BeingCallable>>()
        val mock1 = mockk<IntStream>()
        val mock4 = (1..5).map { mockk<BeingCallable>() }
        val mock9 = mockk<Supplier<Future<BeingCallable>>>()
        mock3.injectInto(manager)
        mock4.injectInto(manager)

        every { IntStream.rangeClosed(any(), any()) } returns mock1
        every { mock1.mapToObj(any<IntFunction<Future<BeingCallable>>>()) } answers {
            arg<IntFunction<Future<BeingCallable>>>(0).apply(0)
            mock2
        }
        every { mock3.take() } answers {
            mock0
        }
        every { mock0.get() } answers {
            mock8
        }
        every { mock9.get() } answers {
            mock0
        }

        every { mock2.filter(any()) } answers {
            assertThat(arg<Predicate<Future<BeingCallable>>>(0).test(mock0)).isTrue
            mock2
        }
        every { mock2.count() } returns 5

        manager.awaitCompletionOfFutures()

        verify {
            manager.beingCount
            IntStream.rangeClosed(any(), any())
            mock1.mapToObj(any<IntFunction<Future<BeingCallable>>>())
            mock0.get()
            mock2.filter(any())
            mock3.take()
        }
    }

    @Test
    fun `awaitCompletionOfFutures graduate solution handles errors`() {
        runAs(Student.Type.Graduate)

        mockkStatic(IntStream::class)
        val mock3 = mockk<CompletionService<BeingCallable>>()
        val mock8 = mockk<BeingCallable>()
        val mock2 = mockk<Stream<Future<BeingCallable>>>()
        val mock0 = mockk<Future<BeingCallable>>()
        val mock1 = mockk<IntStream>()
        val mock4 = (1..5).map { mockk<BeingCallable>() }
        val mock9 = mockk<Supplier<Future<BeingCallable>>>()
        mock3.injectInto(manager)
        mock4.injectInto(manager)

        every { IntStream.rangeClosed(any(), any()) } returns mock1
        every { mock1.mapToObj(any<IntFunction<Future<BeingCallable>>>()) } answers {
            arg<IntFunction<Future<BeingCallable>>>(0).apply(0)
            mock2
        }
        every { mock3.take() } returns mock0
        every { mock0.get() } returns null
        every { mock9.get() } returns mock0

        val m3 = mockk<Stream<Future<BeingCallable>>>()
        every { mock2.filter(any()) } answers {
            assertThat(arg<Predicate<Future<BeingCallable>>>(0).test(mock0)).isFalse
            m3
        }

        every { m3.count() } answers {
            99
        }

        val exception = assertThrows(RuntimeException::class.java) {
            manager.awaitCompletionOfFutures()
        }

        assertThat(exception.message).isEqualTo(
            "expected ${mock4.size} but got 99"
        )

        verify {
            manager.beingCount
            IntStream.rangeClosed(any(), any())
            mock1.mapToObj(any<IntFunction<Future<BeingCallable>>>())
            mock0.get()
            mock2.filter(any())
            mock3.take()
            m3.count()
        }
    }

    @Test
    fun testShutdownNow() {

        val mock1 = mockk<ExecutorService>()
        val mock2 = mockk<List<Runnable>>()
        mock1.injectInto(manager)
        every { mock1.awaitTermination(any(), any()) } returns true
        every { mock1.shutdownNow() } returns mock2

        manager.shutdownNow()
        verify {
            mock1.shutdownNow()
            mock1.awaitTermination(any(), any())
        }
    }
}