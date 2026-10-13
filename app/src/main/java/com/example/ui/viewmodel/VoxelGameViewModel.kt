package com.example.ui.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalBlockEntity
import com.example.data.local.LocalWorldEntity
import com.example.data.local.VoxelWorldDao
import com.example.data.model.ActiveMonster
import com.example.data.model.BlockDeltaModel
import com.example.data.model.BlockType
import com.example.data.model.CraftedItem
import com.example.data.model.DifficultyLevel
import com.example.data.model.GameMode
import com.example.data.model.GamePlayLevel
import com.example.data.model.MonsterType
import com.example.data.model.PlayerPresenceModel
import com.example.data.model.SrfMode
import com.example.data.model.WorldModel
import com.example.data.repository.VoxelRepository
import com.example.domain.ProceduralWorldEngine
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CrosshairActionMode(val label: String) {
    BUILD("Build / Place"),
    MINE("Mine / Harvest"),
    INSPECT("Chunk Scanner")
}

data class GameSessionUiState(
    val activeWorld: WorldModel? = null,
    val isInWorld: Boolean = false,
    val playerX: Int = 0,
    val playerY: Int = 65,
    val playerZ: Int = 0,
    val selectedTargetX: Int = 0,
    val selectedTargetZ: Int = 0,
    val gameMode: GameMode = GameMode.SURVIVAL,
    val srfMode: SrfMode = SrfMode.NONE,
    val selectedDifficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    // Level-Wise Gameplay Progression State (Levels 1..10)
    val currentLevel: Int = 1,
    val currentXp: Int = 0,
    val blocksMinedInLevel: Int = 0,
    val blocksPlacedInLevel: Int = 0,
    val monstersDefeatedInLevel: Int = 0,
    val itemsCraftedInLevel: Int = 0,
    // Player Vital Stats
    val health: Int = 20,
    val hunger: Int = 20,
    val isFlying: Boolean = false,
    val undergroundCaveLayer: Boolean = false,
    val actionMode: CrosshairActionMode = CrosshairActionMode.MINE,
    val selectedHotbarBlock: BlockType = BlockType.GRASS,
    val equippedTool: CraftedItem? = null,
    val miningProgress: Float = 0f,
    val miningTargetX: Int? = null,
    val miningTargetZ: Int? = null,
    val inventory: Map<BlockType, Int> = mapOf(
        BlockType.OAK_LOG to 4,
        BlockType.WOOD_PLANKS to 8,
        BlockType.DIRT to 12,
        BlockType.STONE to 6,
        BlockType.TORCH to 4
    ),
    val unlockedTools: Set<CraftedItem> = emptySet(),
    // 20 TPS Clock Engine Telemetry (Section 4 of PDF)
    val totalGameTicks: Long = 0L,
    val currentTps: Int = 20,
    val dayTimeTicks: Int = 600, // 0..2400 cycle (0..1400 Day, 1400..2400 Night)
    val activeChunkRadius: Int = 3,
    val loadedChunksCount: Int = 49,
    val frozenDistantChunksCount: Int = 196,
    // SRF Community Systems Telemetry (Section 5 of PDF)
    val raidStackedWaves: Int = 0,
    val raidEmeraldsCollected: Int = 0,
    val parasiteEvolutionPhase: Int = 0,
    val parasitePoints: Int = 0,
    val activeMonsters: List<ActiveMonster> = emptyList(),
    // Real-time Multiplayer & Offline Flow State
    val isNetworkOnline: Boolean = true,
    val simulatedOfflineToggle: Boolean = false,
    val pendingOfflineSyncCount: Int = 0,
    val lastSyncedToast: String? = null,
    val statusBannerMessage: String = "Level 1: Overworld Novice • 20 TPS Clock Active",
    val remotePlayers: List<PlayerPresenceModel> = emptyList(),
    val worldBlockOverrides: Map<String, BlockDeltaModel> = emptyMap()
) {
    val isEffectiveOffline: Boolean
        get() = !isNetworkOnline || simulatedOfflineToggle

    val isNightTime: Boolean
        get() = dayTimeTicks >= 1450

    val levelConfig: GamePlayLevel
        get() = GamePlayLevel.fromLevelNumber(currentLevel)

    val nextLevelConfig: GamePlayLevel?
        get() = if (currentLevel < 10) GamePlayLevel.fromLevelNumber(currentLevel + 1) else null

    val levelProgressFraction: Float
        get() {
            val next = nextLevelConfig ?: return 1f
            val curBase = levelConfig.xpThreshold
            val span = (next.xpThreshold - curBase).coerceAtLeast(1)
            return ((currentXp - curBase).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }
}

class VoxelGameViewModel(
    private val repository: VoxelRepository,
    private val localDao: VoxelWorldDao,
    private val currentUserId: String,
    appContext: Context
) : ViewModel() {

    private val _session = MutableStateFlow(GameSessionUiState())
    val session: StateFlow<GameSessionUiState> = _session.asStateFlow()

    private var worldBlocksJob: Job? = null
    private var localBlocksJob: Job? = null
    private var worldPlayersJob: Job? = null
    private var tickEngineJob: Job? = null

    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _session.update { it.copy(isNetworkOnline = true) }
            triggerAutoSyncIfOnline()
        }

        override fun onLost(network: Network) {
            _session.update {
                it.copy(
                    isNetworkOnline = false,
                    statusBannerMessage = "Offline Flow Active — Level & Block edits queued locally in Room DB"
                )
            }
        }
    }

    val worldsList: StateFlow<List<WorldModel>> = combine(
        repository.observePublicWorlds()
            .catch { e ->
                Log.w(TAG, "Public worlds stream fallback to offline Room cache", e)
                emit(emptyList())
            },
        repository.observeMyWorlds(currentUserId)
            .catch { e ->
                Log.w(TAG, "My worlds stream fallback to offline Room cache", e)
                emit(emptyList())
            },
        localDao.observeAllWorlds()
    ) { publicCloud, myCloud, localRoom ->
        val merged = linkedMapOf<String, WorldModel>()
        for (w in myCloud + publicCloud) {
            if (w.worldId.isNotBlank()) {
                merged[w.worldId] = w
            }
        }
        if (merged.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                val entities = merged.values.map { w ->
                    LocalWorldEntity(
                        worldId = w.worldId,
                        name = w.name,
                        seed = w.seed,
                        gameMode = w.gameMode,
                        srfMode = w.srfMode,
                        difficulty = w.difficulty,
                        ownerId = w.ownerId,
                        ownerName = w.ownerName,
                        isPublic = w.isPublic,
                        blockEditCount = w.blockEditCount.toInt(),
                        parasiteEvolutionPhase = w.parasiteEvolutionPhase.toInt(),
                        raidStackedWaves = w.raidStackedWaves.toInt(),
                        pendingSync = false
                    )
                }
                localDao.upsertWorlds(entities)
            }
        }
        for (lw in localRoom) {
            if (!merged.containsKey(lw.worldId)) {
                merged[lw.worldId] = WorldModel(
                    worldId = lw.worldId,
                    name = lw.name,
                    seed = lw.seed,
                    gameMode = lw.gameMode,
                    srfMode = lw.srfMode,
                    difficulty = lw.difficulty,
                    ownerId = lw.ownerId,
                    ownerName = lw.ownerName,
                    isPublic = lw.isPublic,
                    blockEditCount = lw.blockEditCount.toLong(),
                    parasiteEvolutionPhase = lw.parasiteEvolutionPhase.toLong(),
                    raidStackedWaves = lw.raidStackedWaves.toLong()
                )
            }
        }
        merged.values.toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        checkInitialConnectivity()
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback", e)
        }

        viewModelScope.launch {
            localDao.observePendingSyncCount().collect { count ->
                _session.update { it.copy(pendingOfflineSyncCount = count) }
            }
        }

        // Pre-seed a default Starter World in Room DB so user can play immediately with zero waiting
        viewModelScope.launch(Dispatchers.IO) {
            val starterId = "starter_world_realm"
            if (localDao.getWorldById(starterId) == null) {
                localDao.upsertWorld(
                    LocalWorldEntity(
                        worldId = starterId,
                        name = "Starter World (Greenfield)",
                        seed = 104729L,
                        gameMode = GameMode.SURVIVAL.name,
                        srfMode = SrfMode.NONE.name,
                        difficulty = DifficultyLevel.MEDIUM.name,
                        ownerId = currentUserId,
                        ownerName = "Crafter",
                        isPublic = false,
                        blockEditCount = 0,
                        parasiteEvolutionPhase = 0,
                        raidStackedWaves = 0,
                        pendingSync = false
                    )
                )
            }
        }

        startGameTickClockEngine()
    }

    private fun checkInitialConnectivity() {
        val activeNet = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNet)
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _session.update { it.copy(isNetworkOnline = connected) }
    }

    /**
     * Awards XP and checks whether the player automatically levels up.
     */
    private fun awardExperience(xpGain: Int, reason: String) {
        if (xpGain <= 0) return
        val state = _session.value
        val updatedXp = state.currentXp + xpGain

        // Determine highest unlocked level from updatedXp
        val targetLevel = GamePlayLevel.entries
            .lastOrNull { updatedXp >= it.xpThreshold }
            ?.levelNumber ?: state.currentLevel

        if (targetLevel > state.currentLevel) {
            applyLevelUpgrade(
                targetLevel = targetLevel,
                newXp = updatedXp,
                triggerMessage = "LEVEL UP! Reached Level $targetLevel: ${GamePlayLevel.fromLevelNumber(targetLevel).title} (+Rewards Unlocked!)"
            )
        } else {
            _session.update {
                it.copy(
                    currentXp = updatedXp,
                    statusBannerMessage = "$reason (+$xpGain XP • Lv.${state.currentLevel} ${state.currentXp + xpGain}/${state.nextLevelConfig?.xpThreshold ?: state.currentXp})"
                )
            }
        }
    }

    /**
     * Allows the player to directly update/select any gameplay level (1..10) while playing
     * or advance to the next level immediately.
     */
    fun updateGameplayLevel(targetLevelNumber: Int) {
        val clamped = targetLevelNumber.coerceIn(1, 10)
        val levelSpec = GamePlayLevel.fromLevelNumber(clamped)
        val targetXp = maxOf(_session.value.currentXp, levelSpec.xpThreshold)
        applyLevelUpgrade(
            targetLevel = clamped,
            newXp = targetXp,
            triggerMessage = "Switched to Level $clamped: ${levelSpec.title} (${levelSpec.subtitle})"
        )
    }

    fun levelUpNow() {
        val next = (_session.value.currentLevel + 1).coerceAtMost(10)
        val levelSpec = GamePlayLevel.fromLevelNumber(next)
        applyLevelUpgrade(
            targetLevel = next,
            newXp = levelSpec.xpThreshold,
            triggerMessage = "Promoted to Level $next: ${levelSpec.title}! New tools & rewards added to inventory."
        )
    }

    private fun applyLevelUpgrade(
        targetLevel: Int,
        newXp: Int,
        triggerMessage: String
    ) {
        val levelSpec = GamePlayLevel.fromLevelNumber(targetLevel)
        val state = _session.value

        val updatedInv = state.inventory.toMutableMap()
        for ((block, qty) in levelSpec.rewardBlocks) {
            updatedInv[block] = (updatedInv[block] ?: 0) + qty
        }

        val updatedUnlockedTools = state.unlockedTools.toMutableSet()
        var nextEquippedTool = state.equippedTool
        if (levelSpec.unlocksTool != null) {
            updatedUnlockedTools.add(levelSpec.unlocksTool)
            if (nextEquippedTool == null || levelSpec.unlocksTool.toolTier >= nextEquippedTool.toolTier) {
                nextEquippedTool = levelSpec.unlocksTool
            }
        }

        val diameter = levelSpec.recommendedChunkRadius * 2 + 1
        val loadedChunks = diameter * diameter
        val frozenChunks = (15 * 15) - loadedChunks

        val newSrfMode = if (levelSpec.defaultSrfMode != SrfMode.NONE) {
            levelSpec.defaultSrfMode
        } else {
            state.srfMode
        }

        val newParasitePhase = when {
            targetLevel >= 9 -> maxOf(state.parasiteEvolutionPhase, 4)
            targetLevel == 8 -> maxOf(state.parasiteEvolutionPhase, 2)
            else -> state.parasiteEvolutionPhase
        }

        val newRaidWaves = when {
            targetLevel >= 7 -> maxOf(state.raidStackedWaves, targetLevel * 3)
            else -> state.raidStackedWaves
        }

        _session.update {
            it.copy(
                currentLevel = targetLevel,
                currentXp = newXp,
                blocksMinedInLevel = 0,
                blocksPlacedInLevel = 0,
                monstersDefeatedInLevel = 0,
                itemsCraftedInLevel = 0,
                health = 20,
                hunger = 20,
                isFlying = if (targetLevel == 10) true else it.isFlying,
                inventory = updatedInv,
                unlockedTools = updatedUnlockedTools,
                equippedTool = nextEquippedTool,
                activeChunkRadius = levelSpec.recommendedChunkRadius,
                loadedChunksCount = loadedChunks,
                frozenDistantChunksCount = frozenChunks,
                srfMode = newSrfMode,
                parasiteEvolutionPhase = newParasitePhase,
                raidStackedWaves = newRaidWaves,
                statusBannerMessage = triggerMessage
            )
        }

        // Persist updated world telemetry to Room & Firestore
        val activeWorld = _session.value.activeWorld
        if (activeWorld != null) {
            viewModelScope.launch {
                repository.updateWorldTelemetry(
                    worldId = activeWorld.worldId,
                    blockEditCount = _session.value.worldBlockOverrides.size,
                    parasiteEvolutionPhase = _session.value.parasiteEvolutionPhase,
                    raidStackedWaves = _session.value.raidStackedWaves,
                    isOfflineMode = _session.value.isEffectiveOffline
                )
            }
            broadcastPresence()
        }
    }

    /**
     * Coroutine-based Game Loop (The Clock Engine)
     * Executes at a steady 20 TPS (Ticks Per Second, 50ms per tick) via viewModelScope.
     * Handles:
     * - Day / Night diurnal cycle tick (2400-tick day)
     * - Survival mode hunger decay and health regeneration
     * - In-progress mining progression per tick
     * - Hostile monster AI tracking & movement toward the player
     * - Community SRF systems (Raid Farm automated wave stacking & Parasite Phase advancement)
     * - Dynamic TPS calculation & performance monitoring
     */
    fun startGameTickClockEngine() {
        tickEngineJob?.cancel()
        tickEngineJob = viewModelScope.launch(Dispatchers.Default) {
            var tickCounter = _session.value.totalGameTicks
            while (isActive) {
                val tickStartNanos = System.nanoTime()
                tickCounter++

                processSingleGameTick(tickCounter)

                val elapsedMs = (System.nanoTime() - tickStartNanos) / 1_000_000L
                val remainingDelay = (TICK_INTERVAL_MS - elapsedMs).coerceAtLeast(0L)
                delay(remainingDelay)
            }
        }
    }

    /**
     * Executes one discrete 20 TPS simulation step on game state.
     * Can also be invoked synchronously in unit tests to verify deterministic behavior.
     */
    fun processSingleGameTick(tickCounter: Long = _session.value.totalGameTicks + 1L) {
        val state = _session.value
        val world = state.activeWorld ?: return
        if (!state.isInWorld) return

        val newDayTicks = ((state.dayTimeTicks + 1) % 2400)
        var newHealth = state.health
        var newHunger = state.hunger
        var newRaidWaves = state.raidStackedWaves
        var newEmeralds = state.raidEmeraldsCollected
        var newParasitePoints = state.parasitePoints
        var newParasitePhase = state.parasiteEvolutionPhase
        var updatedMonsters = state.activeMonsters
        var updatedInventory = state.inventory
        var newBanner = state.statusBannerMessage

        // 1. Player Vitals Simulation (Survival vs Creative)
        val difficulty = state.selectedDifficulty
        if (state.gameMode == GameMode.CREATIVE) {
            newHealth = 20
            newHunger = 20
        } else {
            // Hunger decay based on selected difficulty tick interval
            if (tickCounter % difficulty.hungerDecayTickInterval == 0L && newHunger > 0) {
                newHunger = (newHunger - 1).coerceAtLeast(0)
            }
            // Health regeneration when saturated (hunger >= 16) based on difficulty interval
            if (tickCounter % difficulty.healthRegenTickInterval == 0L && newHunger >= 16 && newHealth < 20) {
                newHealth = (newHealth + 1).coerceAtMost(20)
            }
            // Starvation damage when hunger is completely depleted
            val starvationInterval = if (difficulty == DifficultyLevel.EXTRA_HARD) 50L else 100L
            if (tickCounter % starvationInterval == 0L && newHunger == 0 && newHealth > 1) {
                newHealth = (newHealth - 1).coerceAtLeast(1)
                newBanner = "Starving in [${difficulty.displayName}] Mode! Craft & eat Field Rations."
            }
        }

        // 2. Active SRF Systems Simulation
        when (state.srfMode) {
            SrfMode.RAID_FARM -> {
                if (tickCounter % 60L == 0L) { // Every 3 seconds (60 ticks)
                    val beaconCount = state.worldBlockOverrides.values.count {
                        it.blockType == BlockType.RAID_BEACON.name
                    }.coerceAtLeast(1)
                    newRaidWaves = (newRaidWaves + beaconCount).coerceAtMost(99)
                    val dropCount = beaconCount * 2
                    newEmeralds += dropCount
                    updatedInventory = updatedInventory.toMutableMap().apply {
                        put(
                            BlockType.EMERALD_BLOCK,
                            (get(BlockType.EMERALD_BLOCK) ?: 0) + 1
                        )
                    }
                    newBanner = "SRF Raid Farm Tick: Stacked $newRaidWaves sub-chunk waves (+$dropCount drops)"
                }
            }

            SrfMode.PARASITES -> {
                if (tickCounter % 50L == 0L) { // Every 2.5 seconds (50 ticks)
                    newParasitePoints += 3
                    newParasitePhase = (newParasitePoints / 30).coerceIn(0, 5)
                }
            }

            else -> Unit
        }

        // 3. Hostile Monster Spawning and Pathfinding
        if (tickCounter % 20L == 0L) { // Evaluated every 1 second (20 ticks)
            val isNightOrParasite = newDayTicks >= 1450 || state.srfMode == SrfMode.PARASITES || state.currentLevel >= 4
            val baseMobs = (2 + state.currentLevel / 2)
            val maxMonstersForLevel = ((baseMobs + difficulty.maxMonstersPerWave) / 2).coerceIn(2, difficulty.maxMonstersPerWave)
            if (isNightOrParasite && updatedMonsters.size < maxMonstersForLevel) {
                val spawnType = when {
                    state.srfMode == SrfMode.PARASITES && (newParasitePhase >= 3 || state.currentLevel >= 9) ->
                        MonsterType.ADAPTED_LONGLARMS
                    state.srfMode == SrfMode.PARASITES || state.currentLevel >= 8 ->
                        MonsterType.RUPTER_PARASITE
                    state.srfMode == SrfMode.RAID_FARM || state.currentLevel >= 7 ->
                        MonsterType.RAID_PILLAGER
                    Random.nextBoolean() -> MonsterType.ZOMBIE
                    else -> MonsterType.SKELETON
                }
                val offsetX = if (Random.nextBoolean()) Random.nextInt(3, 6) else -Random.nextInt(3, 6)
                val offsetZ = if (Random.nextBoolean()) Random.nextInt(3, 6) else -Random.nextInt(3, 6)
                updatedMonsters = updatedMonsters + ActiveMonster(
                    id = "mob_${tickCounter}_${Random.nextInt(100, 999)}",
                    type = spawnType,
                    x = state.playerX + offsetX,
                    z = state.playerZ + offsetZ,
                    hp = spawnType.maxHp + (state.currentLevel * 2) + difficulty.monsterHpBonus
                )
            } else if (!isNightOrParasite && updatedMonsters.isNotEmpty()) {
                updatedMonsters = updatedMonsters.filter { it.type.isParasite }
            }

            if (state.gameMode == GameMode.SURVIVAL && updatedMonsters.isNotEmpty()) {
                updatedMonsters = updatedMonsters.map { mob ->
                    val dx = (state.playerX - mob.x).coerceIn(-1, 1)
                    val dz = (state.playerZ - mob.z).coerceIn(-1, 1)
                    val nextX = mob.x + dx
                    val nextZ = mob.z + dz
                    if (abs(nextX - state.playerX) <= 1 && abs(nextZ - state.playerZ) <= 1 && !state.isFlying) {
                        val scaledDamage = (mob.type.attackDamage * difficulty.monsterDamageMultiplier).roundToInt().coerceAtLeast(1)
                        newHealth = (newHealth - scaledDamage).coerceAtLeast(0)
                        newBanner = "${mob.type.displayName} attacked you (-$scaledDamage HP) [${difficulty.displayName}]!"
                    }
                    mob.copy(x = nextX, z = nextZ)
                }
                if (newHealth == 0) {
                    newHealth = 20
                    newHunger = 20
                    newBanner = "Defeated in ${difficulty.displayName} Mode! Respawned at chunk origin (0, 0)."
                }
            }
        }

        // 4. Per-Tick Mining Progress (Ticks Per Block Harvesting)
        var currentMiningProgress = state.miningProgress
        var currentMiningTargetX = state.miningTargetX
        var currentMiningTargetZ = state.miningTargetZ
        var minedBlockReward: BlockType? = null

        if (state.actionMode == CrosshairActionMode.MINE && currentMiningTargetX != null && currentMiningTargetZ != null) {
            val col = ProceduralWorldEngine.getNaturalColumn(currentMiningTargetX, currentMiningTargetZ, world.seed, state.parasiteEvolutionPhase)
            val key = "${currentMiningTargetX}_${currentMiningTargetZ}"
            val existingOverride = state.worldBlockOverrides[key]
            val currentBlock = existingOverride?.let { BlockType.fromKey(it.blockType) }
                ?: if (state.undergroundCaveLayer) col.undergroundOre else col.surfaceBlock

            if (currentBlock != BlockType.AIR && currentBlock != BlockType.WATER) {
                val effectiveToolTier = maxOf(
                    state.equippedTool?.toolTier ?: 0,
                    (state.currentLevel - 1) / 2
                )
                if (currentBlock.requiredToolTier <= effectiveToolTier || state.gameMode == GameMode.CREATIVE) {
                    val toolMult = state.equippedTool?.toolSpeedMultiplier ?: 1f
                    val levelMult = state.levelConfig.miningSpeedBonus
                    val diffMult = difficulty.miningSpeedMultiplier
                    val step = (1f * toolMult * levelMult * diffMult) / currentBlock.hardnessTicks.coerceAtLeast(4)
                    currentMiningProgress += step
                    if (currentMiningProgress >= 1f) {
                        // Block harvested successfully in game tick loop
                        minedBlockReward = currentBlock
                        currentMiningProgress = 0f
                        currentMiningTargetX = null
                        currentMiningTargetZ = null
                        val replacementBlock = if (state.undergroundCaveLayer) BlockType.AIR else col.biome.subBlock
                        commitBlockModification(world, currentMiningTargetX ?: state.selectedTargetX, col.surfaceY, currentMiningTargetZ ?: state.selectedTargetZ, replacementBlock)
                        awardExperience(currentBlock.xpReward, "Harvested +1 ${currentBlock.displayName}")
                    }
                }
            } else {
                currentMiningProgress = 0f
                currentMiningTargetX = null
                currentMiningTargetZ = null
            }
        }

        if (minedBlockReward != null) {
            updatedInventory = updatedInventory.toMutableMap().apply {
                put(minedBlockReward, (get(minedBlockReward) ?: 0) + 1)
            }
        }

        // 5. Commit Tick State Update
        _session.update {
            it.copy(
                totalGameTicks = tickCounter,
                currentTps = 20,
                dayTimeTicks = newDayTicks,
                health = newHealth,
                hunger = newHunger,
                miningProgress = currentMiningProgress,
                miningTargetX = currentMiningTargetX,
                miningTargetZ = currentMiningTargetZ,
                raidStackedWaves = newRaidWaves,
                raidEmeraldsCollected = newEmeralds,
                parasitePoints = newParasitePoints,
                parasiteEvolutionPhase = newParasitePhase,
                activeMonsters = updatedMonsters,
                inventory = updatedInventory,
                statusBannerMessage = newBanner,
                blocksMinedInLevel = if (minedBlockReward != null) it.blocksMinedInLevel + 1 else it.blocksMinedInLevel
            )
        }
    }

    fun createAndEnterWorld(
        name: String,
        seedInput: String,
        gameMode: GameMode,
        srfMode: SrfMode,
        isPublic: Boolean,
        difficulty: DifficultyLevel = _session.value.selectedDifficulty
    ) {
        val parsedSeed = seedInput.trim().toLongOrNull()
            ?: if (seedInput.isBlank()) Random.nextLong(100_000L, 999_999_999L)
            else seedInput.hashCode().toLong()

        viewModelScope.launch {
            val offline = _session.value.isEffectiveOffline
            val result = repository.createWorld(
                name = name,
                seed = parsedSeed,
                gameMode = gameMode,
                srfMode = srfMode,
                isPublic = isPublic,
                difficulty = difficulty,
                isOfflineMode = offline
            )
            result.onSuccess { worldId ->
                val created = WorldModel(
                    worldId = worldId,
                    name = name.trim().ifEmpty { "Overworld" },
                    seed = parsedSeed,
                    gameMode = gameMode.name,
                    srfMode = srfMode.name,
                    difficulty = difficulty.name,
                    ownerId = currentUserId,
                    ownerName = "You",
                    isPublic = isPublic
                )
                enterWorld(created)
            }.onFailure { err ->
                _session.update {
                    it.copy(statusBannerMessage = "Created in local offline cache: ${err.localizedMessage ?: "Offline"}")
                }
            }
        }
    }

    fun selectDifficulty(difficulty: DifficultyLevel) {
        _session.update {
            it.copy(
                selectedDifficulty = difficulty,
                statusBannerMessage = "Difficulty mode: ${difficulty.displayName} (${difficulty.badgeLabel})"
            )
        }
    }

    fun launchDirectPlayWithDifficulty(difficulty: DifficultyLevel) {
        selectDifficulty(difficulty)
        val targetWorld = worldsList.value.firstOrNull() ?: WorldModel(
            worldId = "starter_world_realm",
            name = "Starter World (Greenfield)",
            seed = 104729L,
            gameMode = GameMode.SURVIVAL.name,
            srfMode = SrfMode.NONE.name,
            difficulty = difficulty.name,
            ownerId = currentUserId,
            ownerName = "Crafter"
        )
        enterWorld(targetWorld.copy(difficulty = difficulty.name))
    }

    fun enterWorld(world: WorldModel) {
        val mode = runCatching { GameMode.valueOf(world.gameMode) }.getOrDefault(GameMode.SURVIVAL)
        val srf = runCatching { SrfMode.valueOf(world.srfMode) }.getOrDefault(SrfMode.NONE)
        val diff = DifficultyLevel.fromKey(world.difficulty.ifEmpty { _session.value.selectedDifficulty.name })

        _session.update {
            it.copy(
                activeWorld = world,
                isInWorld = true,
                playerX = 0,
                playerY = 65,
                playerZ = 0,
                selectedTargetX = 0,
                selectedTargetZ = 0,
                gameMode = mode,
                srfMode = srf,
                selectedDifficulty = diff,
                health = 20,
                hunger = 20,
                isFlying = mode == GameMode.CREATIVE,
                raidStackedWaves = world.raidStackedWaves.toInt(),
                parasiteEvolutionPhase = world.parasiteEvolutionPhase.toInt(),
                activeMonsters = emptyList(),
                statusBannerMessage = "Entered '${world.name}' • [${diff.displayName}] • Level ${it.currentLevel}: ${it.levelConfig.title}"
            )
        }

        subscribeToWorldStreams(world.worldId)
        broadcastPresence()
    }

    fun exitWorldToLobby() {
        worldBlocksJob?.cancel()
        localBlocksJob?.cancel()
        worldPlayersJob?.cancel()
        _session.update {
            it.copy(
                isInWorld = false,
                activeWorld = null,
                remotePlayers = emptyList(),
                worldBlockOverrides = emptyMap()
            )
        }
    }

    private fun subscribeToWorldStreams(worldId: String) {
        worldBlocksJob?.cancel()
        localBlocksJob?.cancel()
        worldPlayersJob?.cancel()

        localBlocksJob = viewModelScope.launch {
            localDao.observeBlocksForWorld(worldId).collect { localBlocks ->
                val map = localBlocks.associate { b ->
                    "${b.x}_${b.z}" to BlockDeltaModel(
                        blockKey = b.blockKey,
                        worldId = b.worldId,
                        x = b.x.toLong(),
                        y = b.y.toLong(),
                        z = b.z.toLong(),
                        blockType = b.blockType,
                        updatedBy = b.updatedBy,
                        updatedByName = b.updatedByName
                    )
                }
                _session.update { state ->
                    state.copy(worldBlockOverrides = state.worldBlockOverrides + map)
                }
            }
        }

        worldBlocksJob = viewModelScope.launch {
            repository.observeWorldBlocks(worldId)
                .catch { e ->
                    Log.w(TAG, "Realtime block listener paused (offline)", e)
                }
                .collect { cloudBlocks ->
                    val cloudMap = cloudBlocks.associateBy { "${it.x}_${it.z}" }
                    _session.update { state ->
                        state.copy(worldBlockOverrides = state.worldBlockOverrides + cloudMap)
                    }
                    val roomEntities = cloudBlocks.map { cb ->
                        LocalBlockEntity(
                            worldId = worldId,
                            blockKey = cb.blockKey.ifEmpty { "b_${cb.x}_${cb.y}_${cb.z}" },
                            x = cb.x.toInt(),
                            y = cb.y.toInt(),
                            z = cb.z.toInt(),
                            blockType = cb.blockType,
                            updatedBy = cb.updatedBy,
                            updatedByName = cb.updatedByName,
                            pendingSync = false
                        )
                    }
                    if (roomEntities.isNotEmpty()) {
                        localDao.upsertBlocks(roomEntities)
                    }
                }
        }

        worldPlayersJob = viewModelScope.launch {
            repository.observeWorldPlayers(worldId)
                .catch { e ->
                    Log.w(TAG, "Realtime player presence paused (offline)", e)
                }
                .collect { players ->
                    _session.update { state ->
                        state.copy(
                            remotePlayers = players.filter { it.userId != currentUserId }
                        )
                    }
                }
        }
    }

    fun movePlayer(dx: Int, dz: Int) {
        val state = _session.value
        val world = state.activeWorld ?: return
        val newX = (state.playerX + dx).coerceIn(-30000, 30000)
        val newZ = (state.playerZ + dz).coerceIn(-30000, 30000)
        val col = ProceduralWorldEngine.getNaturalColumn(newX, newZ, world.seed, state.parasiteEvolutionPhase)

        val overrideBlock = state.worldBlockOverrides["${newX}_${newZ}"]?.let {
            BlockType.fromKey(it.blockType)
        }
        val effectiveSurface = overrideBlock ?: col.surfaceBlock
        if (state.gameMode == GameMode.SURVIVAL && !state.isFlying && effectiveSurface == BlockType.WATER) {
            _session.update {
                it.copy(
                    selectedTargetX = newX,
                    selectedTargetZ = newZ,
                    statusBannerMessage = "Deep water ahead! Place blocks to build a bridge or switch to Creative Flight."
                )
            }
            return
        }

        val newY = if (state.isFlying) 78 else col.surfaceY + 1
        _session.update {
            it.copy(
                playerX = newX,
                playerY = newY,
                playerZ = newZ,
                selectedTargetX = newX,
                selectedTargetZ = newZ,
                miningProgress = 0f
            )
        }
        broadcastPresence()
    }

    fun selectGridTile(targetX: Int, targetZ: Int) {
        _session.update {
            it.copy(
                selectedTargetX = targetX,
                selectedTargetZ = targetZ,
                miningProgress = 0f
            )
        }
        performTileAction(targetX, targetZ)
    }

    fun performTileAction(targetX: Int = _session.value.selectedTargetX, targetZ: Int = _session.value.selectedTargetZ) {
        val state = _session.value
        val world = state.activeWorld ?: return

        val monsterOnTile = state.activeMonsters.firstOrNull { it.x == targetX && it.z == targetZ }
        if (monsterOnTile != null) {
            attackMonster(monsterOnTile)
            return
        }

        val col = ProceduralWorldEngine.getNaturalColumn(targetX, targetZ, world.seed, state.parasiteEvolutionPhase)
        val key = "${targetX}_${targetZ}"
        val existingOverride = state.worldBlockOverrides[key]
        val currentBlock = existingOverride?.let { BlockType.fromKey(it.blockType) }
            ?: if (state.undergroundCaveLayer) col.undergroundOre else col.surfaceBlock

        when (state.actionMode) {
            CrosshairActionMode.INSPECT -> {
                val chunkX = ProceduralWorldEngine.chunkCoord(targetX)
                val chunkZ = ProceduralWorldEngine.chunkCoord(targetZ)
                _session.update {
                    it.copy(
                        statusBannerMessage = "Chunk ($chunkX, $chunkZ) • Y:${col.surfaceY}/384 • ${col.biome.displayName} • ${currentBlock.displayName}"
                    )
                }
            }

            CrosshairActionMode.MINE -> {
                if (currentBlock == BlockType.AIR || currentBlock == BlockType.WATER) {
                    _session.update {
                        it.copy(statusBannerMessage = "Cannot harvest ${currentBlock.displayName}. Switch to Build Mode to place blocks.")
                    }
                    return
                }

                if (state.gameMode == GameMode.SURVIVAL) {
                    val effectiveToolTier = maxOf(
                        state.equippedTool?.toolTier ?: 0,
                        (state.currentLevel - 1) / 2
                    )
                    if (currentBlock.requiredToolTier > effectiveToolTier) {
                        val neededTool = CraftedItem.entries.firstOrNull { it.toolTier == currentBlock.requiredToolTier }?.displayName ?: "Pickaxe"
                        _session.update {
                            it.copy(statusBannerMessage = "${currentBlock.displayName} requires $neededTool or higher Level! Open Level/Crafting menu.")
                        }
                        return
                    }
                    val toolMult = state.equippedTool?.toolSpeedMultiplier ?: 1f
                    val levelMult = state.levelConfig.miningSpeedBonus
                    val diffMult = state.selectedDifficulty.miningSpeedMultiplier
                    val step = (10f * toolMult * levelMult * diffMult) / currentBlock.hardnessTicks.coerceAtLeast(4)
                    val nextProgress = state.miningProgress + step
                    if (nextProgress < 1f) {
                        _session.update {
                            it.copy(
                                miningProgress = nextProgress,
                                miningTargetX = targetX,
                                miningTargetZ = targetZ,
                                statusBannerMessage = "Mining ${currentBlock.displayName} (Lv.${state.currentLevel} ${levelMult}x)... ${(nextProgress * 100).toInt()}%"
                            )
                        }
                        return
                    }
                }

                val replacementBlock = if (state.undergroundCaveLayer) BlockType.AIR else col.biome.subBlock
                val updatedInv = state.inventory.toMutableMap().apply {
                    put(currentBlock, (get(currentBlock) ?: 0) + 1)
                }
                _session.update {
                    it.copy(
                        miningProgress = 0f,
                        inventory = updatedInv,
                        blocksMinedInLevel = it.blocksMinedInLevel + 1
                    )
                }
                commitBlockModification(world, targetX, col.surfaceY, targetZ, replacementBlock)
                awardExperience(currentBlock.xpReward, "Harvested +1 ${currentBlock.displayName}")
            }

            CrosshairActionMode.BUILD -> {
                val blockToPlace = state.selectedHotbarBlock
                if (state.gameMode == GameMode.SURVIVAL) {
                    val available = state.inventory[blockToPlace] ?: 0
                    if (available <= 0) {
                        _session.update {
                            it.copy(statusBannerMessage = "Out of ${blockToPlace.displayName}! Mine resources, level up, or switch to Creative Mode.")
                        }
                        return
                    }
                    val updatedInv = state.inventory.toMutableMap().apply {
                        put(blockToPlace, (available - 1).coerceAtLeast(0))
                    }
                    _session.update { it.copy(inventory = updatedInv) }
                }

                val placeY = (col.surfaceY + 1).coerceIn(0, 384)
                _session.update {
                    it.copy(blocksPlacedInLevel = it.blocksPlacedInLevel + 1)
                }
                commitBlockModification(world, targetX, placeY, targetZ, blockToPlace)
                awardExperience(12, "Placed ${blockToPlace.displayName} at ($targetX, $placeY, $targetZ)")
            }
        }
    }

    private fun attackMonster(monster: ActiveMonster) {
        val state = _session.value
        val levelBonusDmg = state.currentLevel * 2
        val damage = if (state.gameMode == GameMode.CREATIVE) 100 else (4 + (state.equippedTool?.toolTier ?: 0) * 3 + levelBonusDmg)
        val remainingHp = monster.hp - damage
        if (remainingHp <= 0) {
            val rewardBlock = if (monster.type.isParasite) BlockType.PARASITE_BIOMASS else BlockType.EMERALD_BLOCK
            val updatedInv = state.inventory.toMutableMap().apply {
                put(rewardBlock, (get(rewardBlock) ?: 0) + 1)
            }
            _session.update {
                it.copy(
                    activeMonsters = it.activeMonsters.filterNot { m -> m.id == monster.id },
                    inventory = updatedInv,
                    monstersDefeatedInLevel = it.monstersDefeatedInLevel + 1
                )
            }
            awardExperience(monster.type.xpReward, "Defeated ${monster.type.displayName} (+1 ${rewardBlock.displayName})")
        } else {
            _session.update {
                it.copy(
                    activeMonsters = it.activeMonsters.map { m ->
                        if (m.id == monster.id) m.copy(hp = remainingHp) else m
                    },
                    statusBannerMessage = "Hit ${monster.type.displayName} (-$damage HP, $remainingHp HP left)"
                )
            }
        }
    }

    private fun commitBlockModification(
        world: WorldModel,
        x: Int,
        y: Int,
        z: Int,
        blockType: BlockType
    ) {
        val offline = _session.value.isEffectiveOffline
        val optimisticDelta = BlockDeltaModel(
            blockKey = "b_${x}_${y}_${z}".replace("-", "m"),
            worldId = world.worldId,
            x = x.toLong(),
            y = y.toLong(),
            z = z.toLong(),
            blockType = blockType.name,
            updatedBy = currentUserId,
            updatedByName = "You"
        )
        _session.update { state ->
            state.copy(
                worldBlockOverrides = state.worldBlockOverrides + ("${x}_${z}" to optimisticDelta)
            )
        }

        viewModelScope.launch {
            repository.writeBlockEdit(
                worldId = world.worldId,
                x = x,
                y = y,
                z = z,
                blockType = blockType,
                isOfflineMode = offline
            )
            repository.updateWorldTelemetry(
                worldId = world.worldId,
                blockEditCount = _session.value.worldBlockOverrides.size,
                parasiteEvolutionPhase = _session.value.parasiteEvolutionPhase,
                raidStackedWaves = _session.value.raidStackedWaves,
                isOfflineMode = offline
            )
        }
    }

    fun craftItem(item: CraftedItem) {
        val state = _session.value
        if (state.gameMode == GameMode.SURVIVAL) {
            for ((ingredient, needed) in item.recipeIngredients) {
                val owned = state.inventory[ingredient] ?: 0
                if (owned < needed) {
                    _session.update {
                        it.copy(statusBannerMessage = "Missing ${needed - owned}x ${ingredient.displayName} to craft ${item.displayName}.")
                    }
                    return
                }
            }
        }

        val updatedInv = state.inventory.toMutableMap()
        if (state.gameMode == GameMode.SURVIVAL) {
            for ((ingredient, needed) in item.recipeIngredients) {
                updatedInv[ingredient] = ((updatedInv[ingredient] ?: 0) - needed).coerceAtLeast(0)
            }
        }

        var newHunger = state.hunger
        var newHealth = state.health
        var newEquipped = state.equippedTool
        val newUnlocked = state.unlockedTools.toMutableSet()

        if (item.yieldsBlock != null) {
            updatedInv[item.yieldsBlock] = (updatedInv[item.yieldsBlock] ?: 0) + item.yieldsCount
        }
        if (item.toolTier > 0) {
            newUnlocked.add(item)
            if (newEquipped == null || item.toolTier >= newEquipped.toolTier) {
                newEquipped = item
            }
        }
        if (item.restoresHunger > 0) {
            newHunger = (newHunger + item.restoresHunger).coerceAtMost(20)
            newHealth = (newHealth + 2).coerceAtMost(20)
        }

        _session.update {
            it.copy(
                inventory = updatedInv,
                unlockedTools = newUnlocked,
                equippedTool = newEquipped,
                hunger = newHunger,
                health = newHealth,
                itemsCraftedInLevel = it.itemsCraftedInLevel + 1,
                selectedHotbarBlock = item.yieldsBlock ?: it.selectedHotbarBlock
            )
        }
        awardExperience(item.craftXpReward, "Crafted ${item.displayName}")
    }

    fun setGameMode(mode: GameMode) {
        _session.update {
            it.copy(
                gameMode = mode,
                health = if (mode == GameMode.CREATIVE) 20 else it.health,
                hunger = if (mode == GameMode.CREATIVE) 20 else it.hunger,
                isFlying = if (mode == GameMode.CREATIVE) it.isFlying else false,
                statusBannerMessage = "Switched to ${mode.label}: ${mode.subtitle}"
            )
        }
        broadcastPresence()
    }

    fun toggleFlight() {
        val state = _session.value
        if (state.gameMode == GameMode.SURVIVAL && state.currentLevel < 10) {
            _session.update {
                it.copy(statusBannerMessage = "Free flight requires Creative Mode or Level 10 Master Architect!")
            }
            return
        }
        _session.update {
            val next = !it.isFlying
            it.copy(
                isFlying = next,
                playerY = if (next) 80 else 65,
                statusBannerMessage = if (next) "Free Flight Enabled (Y: 80)" else "Landed on terrain surface"
            )
        }
        broadcastPresence()
    }

    fun toggleUndergroundCaveLayer() {
        _session.update {
            val next = !it.undergroundCaveLayer
            it.copy(
                undergroundCaveLayer = next,
                statusBannerMessage = if (next) "Deep Sub-Chunk Cave Scanner Active (Ore Veins Exposed)" else "Overworld Surface View Active"
            )
        }
    }

    fun setActionMode(mode: CrosshairActionMode) {
        _session.update { it.copy(actionMode = mode, miningProgress = 0f) }
    }

    fun selectHotbarBlock(block: BlockType) {
        _session.update {
            it.copy(
                selectedHotbarBlock = block,
                actionMode = CrosshairActionMode.BUILD
            )
        }
        broadcastPresence()
    }

    fun setSrfMode(mode: SrfMode) {
        _session.update {
            it.copy(
                srfMode = mode,
                statusBannerMessage = "Activated ${mode.acronymFull}"
            )
        }
    }

    fun setChunkRadius(radius: Int) {
        val clamped = radius.coerceIn(2, 6)
        val diameter = clamped * 2 + 1
        val loaded = diameter * diameter
        val frozen = (15 * 15) - loaded
        _session.update {
            it.copy(
                activeChunkRadius = clamped,
                loadedChunksCount = loaded,
                frozenDistantChunksCount = frozen,
                statusBannerMessage = "Chunk Radius set to $clamped ($loaded active 16x16x384 chunks, $frozen distant chunks frozen)"
            )
        }
    }

    fun toggleSimulatedOfflineMode() {
        val nextOffline = !_session.value.simulatedOfflineToggle
        _session.update {
            it.copy(
                simulatedOfflineToggle = nextOffline,
                statusBannerMessage = if (nextOffline) {
                    "Offline Flow Enabled: Changes saved to local Room queue while disconnected"
                } else {
                    "Reconnected to Firebase! Syncing offline Room queue..."
                }
            )
        }
        if (!nextOffline) {
            triggerAutoSyncIfOnline()
        }
    }

    fun triggerAutoSyncIfOnline() {
        if (_session.value.isEffectiveOffline) return
        viewModelScope.launch {
            val count = repository.syncPendingOfflineEdits()
            if (count > 0) {
                _session.update {
                    it.copy(
                        lastSyncedToast = "Synced $count offline block edits to Firebase!",
                        statusBannerMessage = "Cloud Sync Complete: $count offline block edits merged to Firestore."
                    )
                }
            }
        }
    }

    fun deleteWorld(worldId: String) {
        viewModelScope.launch {
            repository.deleteWorld(worldId)
        }
    }

    private fun broadcastPresence() {
        val state = _session.value
        val world = state.activeWorld ?: return
        if (state.isEffectiveOffline) return
        viewModelScope.launch {
            repository.updatePlayerPresence(
                worldId = world.worldId,
                x = state.playerX,
                y = state.playerY,
                z = state.playerZ,
                gameMode = state.gameMode,
                health = state.health,
                hunger = state.hunger,
                isFlying = state.isFlying,
                heldItem = "Lv.${state.currentLevel}_${state.equippedTool?.name ?: state.selectedHotbarBlock.name}".take(32)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
    }

    companion object {
        const val TAG = "VoxelGameVM"
        const val TICK_RATE_TPS = 20
        const val TICK_INTERVAL_MS = 50L
    }
}
