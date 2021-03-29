package edu.vandy.app.ui.screens.main

import edu.vandy.simulator.managers.beings.BeingManager.Factory.Type.COMMON_FORK_JOIN_POOL
import edu.vandy.simulator.managers.palantiri.PalantiriManager.Factory.Type.CONCURRENT_MAP_FAIR_SEMAPHORE

class Assignment_4A_InstrumentedTests : InstrumentedTests() {
    override val beingManager = COMMON_FORK_JOIN_POOL
    override val palantirManager = CONCURRENT_MAP_FAIR_SEMAPHORE
}