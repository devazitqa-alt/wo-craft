package com.example.ui.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.LocalBlockEntity
import com.example.data.local.LocalWorldEntity
import com.example.data.local.VoxelLocalDatabase
import com.example.data.local.VoxelWorldDao
import com.example.data.model.BlockType
import com.example.data.model.GameMode
import com.example.data.model.SrfMode
import com.example.data.model.WorldModel
import com.example.data.repository.VoxelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoxelGameLoopTest : FirestoreEmulatorTestBase() {

    private lateinit var database: VoxelLocalDatabase
    private lateinit var dao: VoxelWorldDao
    private lateinit var repository: VoxelRepository
    private lateinit var viewModel: VoxelGameViewModel

    @Before
    fun setUpTest() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, VoxelLocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.voxelWorldDao()
        repository = VoxelRepository(firestore, dao)

        viewModel = VoxelGameViewModel(
            repository = repository,
            localDao = dao,
            currentUserId = "test_player",
            appContext = context
        )
    }

    @After
    fun tearDownTest() {
        database.close()
    }

    @Test
    fun testProcessSingleGameTick_advancesDayNightCycleAndTickCounter() {
        val testWorld = WorldModel(
            worldId = "world_1",
            name = "Tick Test Realm",
            seed = 42L,
            gameMode = GameMode.SURVIVAL.name,
            srfMode = SrfMode.NONE.name,
            ownerId = "test_player",
            ownerName = "Tester"
        )
        viewModel.enterWorld(testWorld)

        val initialDayTicks = viewModel.session.value.dayTimeTicks
        val initialTotalTicks = viewModel.session.value.totalGameTicks

        // Execute discrete game tick step
        viewModel.processSingleGameTick(initialTotalTicks + 1)

        val updated = viewModel.session.value
        assertEquals(initialTotalTicks + 1, updated.totalGameTicks)
        assertEquals((initialDayTicks + 1) % 2400, updated.dayTimeTicks)
        assertEquals(20, updated.currentTps)
    }

    @Test
    fun testProcessSingleGameTick_creativeModeKeepsMaxVitals() {
        val creativeWorld = WorldModel(
            worldId = "world_creative",
            name = "Creative World",
            seed = 999L,
            gameMode = GameMode.CREATIVE.name,
            srfMode = SrfMode.NONE.name,
            ownerId = "test_player",
            ownerName = "Tester"
        )
        viewModel.enterWorld(creativeWorld)

        // Run ticks in game loop
        for (i in 1..200) {
            viewModel.processSingleGameTick(i.toLong())
        }

        val state = viewModel.session.value
        assertEquals(20, state.health)
        assertEquals(20, state.hunger)
    }

    @Test
    fun testProcessSingleGameTick_srfRaidFarmStacksWaves() {
        val raidWorld = WorldModel(
            worldId = "world_raid",
            name = "Raid World",
            seed = 12345L,
            gameMode = GameMode.SURVIVAL.name,
            srfMode = SrfMode.RAID_FARM.name,
            ownerId = "test_player",
            ownerName = "Tester"
        )
        viewModel.enterWorld(raidWorld)

        val initialEmeralds = viewModel.session.value.raidEmeraldsCollected

        // Run tick at modulo 60 (Raid Wave tick interval)
        viewModel.processSingleGameTick(60L)

        val state = viewModel.session.value
        assertTrue(state.raidStackedWaves >= 1)
        assertTrue(state.raidEmeraldsCollected > initialEmeralds)
    }

    @Test
    fun testProcessSingleGameTick_survivalHungerDecay() {
        val survivalWorld = WorldModel(
            worldId = "world_survival",
            name = "Survival Realm",
            seed = 54321L,
            gameMode = GameMode.SURVIVAL.name,
            srfMode = SrfMode.NONE.name,
            ownerId = "test_player",
            ownerName = "Tester"
        )
        viewModel.enterWorld(survivalWorld)

        // Hunger decay triggers at tick modulo 180
        viewModel.processSingleGameTick(180L)

        val state = viewModel.session.value
        assertEquals(19, state.hunger)
    }
}
