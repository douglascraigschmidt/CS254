package edu.vandy.simulator.managers.beings.runnableThreads;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.InOrder;

import admin.AssignmentTests;
import edu.vandy.simulator.managers.beings.BeingManager;
import edu.vandy.simulator.managers.palantiri.Palantir;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Assignment_1A_BeingRunnableTest extends AssignmentTests {
    @Rule
    public final Timeout timeout = new Timeout(10, SECONDS);

    @Test
    public void testBeingAcquirePalantirAndGazeMethod() {
        BeingManager beingManager = mock(RunnableThreadsMgr.class);
        Palantir palantir = mock(Palantir.class);

        SimpleBeingRunnable being = new SimpleBeingRunnable(beingManager);
        assertNotNull("new SimpleBeingRunnable should not be null.", being);

        when(beingManager.acquirePalantir(same(being))).thenReturn(palantir);
        doNothing().when(beingManager).releasePalantir(same(being), same(palantir));

        InOrder inOrder = inOrder(beingManager, palantir);

        // Make the SUT call.
        being.acquirePalantirAndGaze();

        verify(beingManager, times(1)).acquirePalantir(being);
        verify(beingManager, times(1)).releasePalantir(being, palantir);
        verify(beingManager, never()).error(anyString());

        inOrder.verify(beingManager).acquirePalantir(being);
        inOrder.verify(palantir).gaze(being);
        inOrder.verify(beingManager).releasePalantir(being, palantir);
    }

    @Test
    public void testBeingRunGazingSimulationMethodErrorHandling() {
        BeingManager beingManager = mock(RunnableThreadsMgr.class);

        SimpleBeingRunnable being = new SimpleBeingRunnable(beingManager);
        assertNotNull("new SimpleBeingRunnable should not be null.", being);

        when(beingManager.acquirePalantir(same(being))).thenReturn(null);
        doNothing().when(beingManager).error(anyString());

        InOrder inOrder = inOrder(beingManager);

        // Make the SUT call.
        being.acquirePalantirAndGaze();

        verify(beingManager, times(1)).acquirePalantir(being);
        verify(beingManager, times(1)).error(anyString());

        // Ensure releasePalantir() is never called for the error case.
        verify(beingManager, never()).releasePalantir(any(), any());

        inOrder.verify(beingManager).acquirePalantir(being);
        inOrder.verify(beingManager).error(anyString());
    }
}
