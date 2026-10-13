package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp

enum class GameMode(val label: String, val subtitle: String) {
    SURVIVAL("Survival Mode", "Limited health & hunger, manual harvesting, active night monsters"),
    CREATIVE("Creative Mode", "Infinite health & resources, free flight, passive monsters")
}

/**
 * Difficulty Level Modes: Easy, Medium, Hard, Extra Hard.
 * Users can choose their desired difficulty before playing on the home page.
 */
enum class DifficultyLevel(
    val id: String,
    val displayName: String,
    val badgeLabel: String,
    val description: String,
    val colorHex: Long,
    val monsterDamageMultiplier: Float,
    val hungerDecayTickInterval: Long, // 20 TPS clock ticks between hunger decrements
    val healthRegenTickInterval: Long, // 20 TPS clock ticks between health regens
    val miningSpeedMultiplier: Float,  // mining progress rate multiplier
    val maxMonstersPerWave: Int,       // maximum concurrent hostile mobs
    val monsterHpBonus: Int            // bonus HP for hostile mobs
) {
    EASY(
        id = "easy",
        displayName = "Easy",
        badgeLabel = "RELAXED",
        description = "Gentle pace: slow hunger, rapid health regen, fewer monsters with 50% dmg, +50% mining speed.",
        colorHex = 0xFF4CAF50,
        monsterDamageMultiplier = 0.5f,
        hungerDecayTickInterval = 360L,
        healthRegenTickInterval = 40L,
        miningSpeedMultiplier = 1.5f,
        maxMonstersPerWave = 2,
        monsterHpBonus = 0
    ),
    MEDIUM(
        id = "medium",
        displayName = "Medium",
        badgeLabel = "STANDARD",
        description = "Balanced classic survival: standard monster damage, regular hunger pacing, normal mining.",
        colorHex = 0xFF29B6F6,
        monsterDamageMultiplier = 1.0f,
        hungerDecayTickInterval = 180L,
        healthRegenTickInterval = 80L,
        miningSpeedMultiplier = 1.0f,
        maxMonstersPerWave = 4,
        monsterHpBonus = 2
    ),
    HARD(
        id = "hard",
        displayName = "Hard",
        badgeLabel = "CHALLENGE",
        description = "Tough wilderness: +60% monster damage, faster hunger drain, slower regen, challenging mining.",
        colorHex = 0xFFFF9800,
        monsterDamageMultiplier = 1.6f,
        hungerDecayTickInterval = 100L,
        healthRegenTickInterval = 140L,
        miningSpeedMultiplier = 0.9f,
        maxMonstersPerWave = 6,
        monsterHpBonus = 6
    ),
    EXTRA_HARD(
        id = "extra_hard",
        displayName = "Extra Hard",
        badgeLabel = "NIGHTMARE",
        description = "Extreme nightmare: 2.4x monster damage, aggressive spawns, rapid starvation, grueling mining.",
        colorHex = 0xFFF44336,
        monsterDamageMultiplier = 2.4f,
        hungerDecayTickInterval = 60L,
        healthRegenTickInterval = 220L,
        miningSpeedMultiplier = 0.75f,
        maxMonstersPerWave = 8,
        monsterHpBonus = 12
    );

    companion object {
        fun fromKey(nameOrId: String?): DifficultyLevel {
            if (nameOrId.isNullOrBlank()) return MEDIUM
            return entries.firstOrNull {
                it.name.equals(nameOrId, ignoreCase = true) || it.id.equals(nameOrId, ignoreCase = true)
            } ?: MEDIUM
        }
    }
}

enum class SrfMode(val label: String, val acronymFull: String, val description: String) {
    NONE(
        "Vanilla Core",
        "Standard Clock & Chunk Engine",
        "Pure 20 TPS procedural world with standard biome generation and chunk streaming."
    ),
    RAID_FARM(
        "Stacking Raid Farm",
        "SRF: Sub-Chunk Raid Stacking",
        "Exploits sub-chunk geometry and village boundaries to trigger and stack simultaneous raid waves into instant item collectors."
    ),
    SLIME_REGION(
        "Slime Region Format",
        "SRF: Ultra-Compressed Regions",
        "Compresses 16x16x384 region chunks with zero-air stripping and palette bit-packing for instant multiplayer world streaming."
    ),
    PARASITES(
        "Scape & Run: Parasites",
        "SRP/SRF: Mutating Ecosystem",
        "Survival horror ecosystem where alien parasites gradually mutate across evolution phases (0-5) and infect terrain."
    )
}

/**
 * Level-Wise Progression System (Levels 1..10) so players can progress automatically via XP
 * or manually switch/advance levels while playing.
 */
enum class GamePlayLevel(
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val xpThreshold: Int,
    val miningSpeedBonus: Float,
    val recommendedChunkRadius: Int,
    val defaultSrfMode: SrfMode,
    val missionGoalText: String,
    val rewardBlocks: Map<BlockType, Int>,
    val unlocksTool: CraftedItem? = null
) {
    LEVEL_1(
        levelNumber = 1,
        title = "Overworld Novice",
        subtitle = "Surface Biomes & Basic Harvesting",
        xpThreshold = 0,
        miningSpeedBonus = 1.0f,
        recommendedChunkRadius = 2,
        defaultSrfMode = SrfMode.NONE,
        missionGoalText = "Mine 4 Oak Logs or Grass Blocks & craft Oak Planks",
        rewardBlocks = mapOf(BlockType.OAK_LOG to 4, BlockType.WOOD_PLANKS to 8)
    ),
    LEVEL_2(
        levelNumber = 2,
        title = "Timber & Workbench",
        subtitle = "3x3 Grid Crafting & Shelter Building",
        xpThreshold = 100,
        miningSpeedBonus = 1.25f,
        recommendedChunkRadius = 3,
        defaultSrfMode = SrfMode.NONE,
        missionGoalText = "Craft a Wooden Pickaxe and place 5 shelter blocks",
        rewardBlocks = mapOf(BlockType.CRAFTING_TABLE to 1, BlockType.WOOD_PLANKS to 12, BlockType.TORCH to 4),
        unlocksTool = CraftedItem.WOODEN_PICKAXE
    ),
    LEVEL_3(
        levelNumber = 3,
        title = "Stone Age Delver",
        subtitle = "Cobblestone & Sub-Surface Coal Veins",
        xpThreshold = 250,
        miningSpeedBonus = 1.55f,
        recommendedChunkRadius = 3,
        defaultSrfMode = SrfMode.NONE,
        missionGoalText = "Toggle Cave Scanner and mine 5 Cobblestone or Coal Ore",
        rewardBlocks = mapOf(BlockType.STONE to 12, BlockType.COAL_ORE to 6, BlockType.TORCH to 8),
        unlocksTool = CraftedItem.STONE_PICKAXE
    ),
    LEVEL_4(
        levelNumber = 4,
        title = "Iron Smelter",
        subtitle = "Deep Chunk Excavation & Night Defense",
        xpThreshold = 450,
        miningSpeedBonus = 1.9f,
        recommendedChunkRadius = 4,
        defaultSrfMode = SrfMode.NONE,
        missionGoalText = "Harvest Iron Ore in deep caves and defeat 1 Night Monster",
        rewardBlocks = mapOf(BlockType.IRON_ORE to 8, BlockType.STONE to 16),
        unlocksTool = CraftedItem.IRON_PICKAXE
    ),
    LEVEL_5(
        levelNumber = 5,
        title = "Diamond Prospector",
        subtitle = "384-Height Sub-Chunk Diamond Veins",
        xpThreshold = 750,
        miningSpeedBonus = 2.4f,
        recommendedChunkRadius = 4,
        defaultSrfMode = SrfMode.NONE,
        missionGoalText = "Extract rare Diamond Ore and equip the Diamond Pickaxe",
        rewardBlocks = mapOf(BlockType.DIAMOND_ORE to 6, BlockType.EMERALD_BLOCK to 2),
        unlocksTool = CraftedItem.DIAMOND_PICKAXE
    ),
    LEVEL_6(
        levelNumber = 6,
        title = "Slime Region Architect",
        subtitle = "SRF Ultra-Compressed 16x16x384 Streaming",
        xpThreshold = 1150,
        miningSpeedBonus = 3.0f,
        recommendedChunkRadius = 5,
        defaultSrfMode = SrfMode.SLIME_REGION,
        missionGoalText = "Deploy Slime Region Cores to compress multi-chunk landscapes",
        rewardBlocks = mapOf(BlockType.SLIME_CORE to 4, BlockType.DIAMOND_ORE to 4)
    ),
    LEVEL_7(
        levelNumber = 7,
        title = "Stacking Raid Engineer",
        subtitle = "SRF Sub-Chunk Geometry Raid Automation",
        xpThreshold = 1650,
        miningSpeedBonus = 3.6f,
        recommendedChunkRadius = 5,
        defaultSrfMode = SrfMode.RAID_FARM,
        missionGoalText = "Place SRF Raid Beacons to stack simultaneous raid waves",
        rewardBlocks = mapOf(BlockType.RAID_BEACON to 4, BlockType.EMERALD_BLOCK to 8)
    ),
    LEVEL_8(
        levelNumber = 8,
        title = "Parasite Containment",
        subtitle = "SRP/SRF Phase 2 Bio-Horror Survival",
        xpThreshold = 2300,
        miningSpeedBonus = 4.2f,
        recommendedChunkRadius = 5,
        defaultSrfMode = SrfMode.PARASITES,
        missionGoalText = "Survive Rupter Parasites and harvest Parasite Biomass",
        rewardBlocks = mapOf(BlockType.PARASITE_BIOMASS to 6, BlockType.TORCH to 12)
    ),
    LEVEL_9(
        levelNumber = 9,
        title = "Apex Mutated Ecosystem",
        subtitle = "SRP/SRF Phase 4 Adapted Longarms Siege",
        xpThreshold = 3100,
        miningSpeedBonus = 5.0f,
        recommendedChunkRadius = 6,
        defaultSrfMode = SrfMode.PARASITES,
        missionGoalText = "Conquer Infected Wastes and purify mutated crags",
        rewardBlocks = mapOf(BlockType.EMERALD_BLOCK to 12, BlockType.RAID_BEACON to 6)
    ),
    LEVEL_10(
        levelNumber = 10,
        title = "Master Voxel Architect",
        subtitle = "End-Game 20 TPS Omniscient Sandbox Mastery",
        xpThreshold = 4000,
        miningSpeedBonus = 6.5f,
        recommendedChunkRadius = 6,
        defaultSrfMode = SrfMode.RAID_FARM,
        missionGoalText = "Full mastery unlocked: Free flight, instant mining & max SRF automation",
        rewardBlocks = mapOf(
            BlockType.DIAMOND_ORE to 16,
            BlockType.EMERALD_BLOCK to 20,
            BlockType.RAID_BEACON to 10,
            BlockType.SLIME_CORE to 10
        ),
        unlocksTool = CraftedItem.DIAMOND_PICKAXE
    );

    companion object {
        fun fromLevelNumber(level: Int): GamePlayLevel {
            return entries.firstOrNull { it.levelNumber == level.coerceIn(1, 10) } ?: LEVEL_1
        }
    }
}

enum class BiomeType(val displayName: String, val primaryColorHex: Long, val surfaceBlock: BlockType, val subBlock: BlockType) {
    PLAINS("Plains", 0xFF567D46, BlockType.GRASS, BlockType.DIRT),
    FOREST("Oak Forest", 0xFF2D5A27, BlockType.GRASS, BlockType.DIRT),
    DESERT("Arid Desert", 0xFFD9C27E, BlockType.SAND, BlockType.SANDSTONE),
    SNOWY_PEAKS("Snowy Peaks", 0xFFEAF4F8, BlockType.SNOW, BlockType.STONE),
    PARASITE_BIOME("Infected Wastes", 0xFF5C164E, BlockType.PARASITE_BIOMASS, BlockType.INFECTED_STONE)
}

enum class BlockType(
    val displayName: String,
    val topColor: Color,
    val leftSideColor: Color,
    val rightSideColor: Color,
    val hardnessTicks: Int,
    val isGatherable: Boolean = true,
    val isPlaceable: Boolean = true,
    val requiredToolTier: Int = 0,
    val xpReward: Int = 15
) {
    AIR("Air", Color.Transparent, Color.Transparent, Color.Transparent, 0, false, false, xpReward = 0),
    GRASS("Grass Block", Color(0xFF669E38), Color(0xFF7B5836), Color(0xFF63462B), 10, xpReward = 12),
    DIRT("Dirt", Color(0xFF8B5A2B), Color(0xFF734A22), Color(0xFF5C3B1B), 8, xpReward = 10),
    STONE("Cobblestone", Color(0xFF8A8D8F), Color(0xFF707375), Color(0xFF585A5C), 24, requiredToolTier = 1, xpReward = 20),
    SAND("Sand", Color(0xFFE3CFA1), Color(0xFFC9B68B), Color(0xFFB09E76), 8, xpReward = 10),
    SANDSTONE("Sandstone", Color(0xFFD6C08D), Color(0xFFBDA878), Color(0xFFA39065), 20, requiredToolTier = 1, xpReward = 18),
    SNOW("Snow Block", Color(0xFFF2FAFC), Color(0xFFDCEBF0), Color(0xFFC4D7DE), 6, xpReward = 10),
    WATER("Water", Color(0xCC2874A6), Color(0xCC1F618D), Color(0xCC154360), 0, false, false, xpReward = 0),
    OAK_LOG("Oak Log", Color(0xFFA07848), Color(0xFF6B4C2B), Color(0xFF543B20), 18, xpReward = 18),
    OAK_LEAVES("Oak Leaves", Color(0xFF3E7A2E), Color(0xFF316324), Color(0xFF254D1B), 4, xpReward = 8),
    WOOD_PLANKS("Oak Planks", Color(0xFFB88B4A), Color(0xFF9E763E), Color(0xFF856232), 16, xpReward = 14),
    CRAFTING_TABLE("Crafting Table", Color(0xFFC47B35), Color(0xFF8F5726), Color(0xFF70421B), 18, xpReward = 25),
    COAL_ORE("Coal Ore", Color(0xFF505457), Color(0xFF3D4042), Color(0xFF2B2D2F), 30, requiredToolTier = 1, xpReward = 30),
    IRON_ORE("Iron Ore", Color(0xFFA88F7B), Color(0xFF8C7564), Color(0xFF735E4F), 36, requiredToolTier = 2, xpReward = 45),
    DIAMOND_ORE("Diamond Ore", Color(0xFF4EE2EC), Color(0xFF38B8C2), Color(0xFF28929B), 46, requiredToolTier = 3, xpReward = 80),
    EMERALD_BLOCK("Emerald Collector", Color(0xFF2ECC71), Color(0xFF27AE60), Color(0xFF1E8449), 30, xpReward = 65),
    RAID_BEACON("SRF Raid Beacon", Color(0xFFF39C12), Color(0xFFD68910), Color(0xFFB9770E), 30, xpReward = 70),
    SLIME_CORE("SRF Slime Core", Color(0xFF76D7C4), Color(0xFF48C9B0), Color(0xFF1ABC9C), 14, xpReward = 55),
    PARASITE_BIOMASS("SRP Parasite Biomass", Color(0xFF8E44AD), Color(0xFF71368A), Color(0xFF5B2C6F), 28, xpReward = 60),
    INFECTED_STONE("Infected Crag", Color(0xFF4A235A), Color(0xFF381A45), Color(0xFF271230), 34, xpReward = 40),
    TORCH("Torch Block", Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFF8F00), 2, xpReward = 10);

    companion object {
        fun fromKey(key: String): BlockType {
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: STONE
        }
    }
}

enum class CraftedItem(
    val displayName: String,
    val description: String,
    val recipeIngredients: Map<BlockType, Int>,
    val toolSpeedMultiplier: Float = 1f,
    val toolTier: Int = 0,
    val yieldsBlock: BlockType? = null,
    val yieldsCount: Int = 1,
    val restoresHunger: Int = 0,
    val craftXpReward: Int = 35
) {
    OAK_PLANKS_PACK(
        "Oak Planks (x4)",
        "Basic structural building blocks processed from raw Oak Logs.",
        mapOf(BlockType.OAK_LOG to 1),
        yieldsBlock = BlockType.WOOD_PLANKS,
        yieldsCount = 4,
        craftXpReward = 20
    ),
    CRAFTING_BENCH(
        "Crafting Table",
        "3x3 grid workstation for complex tools and SRF machinery.",
        mapOf(BlockType.WOOD_PLANKS to 4),
        yieldsBlock = BlockType.CRAFTING_TABLE,
        yieldsCount = 1,
        craftXpReward = 35
    ),
    WOODEN_PICKAXE(
        "Wooden Pickaxe",
        "Tier 1 harvesting tool. Unlocks Cobblestone & Coal mining (1.6x speed).",
        mapOf(BlockType.WOOD_PLANKS to 3, BlockType.OAK_LOG to 1),
        toolSpeedMultiplier = 1.6f,
        toolTier = 1,
        craftXpReward = 50
    ),
    STONE_PICKAXE(
        "Stone Pickaxe",
        "Tier 2 harvesting tool. Unlocks Iron Ore extraction (2.4x speed).",
        mapOf(BlockType.STONE to 3, BlockType.WOOD_PLANKS to 1),
        toolSpeedMultiplier = 2.4f,
        toolTier = 2,
        craftXpReward = 70
    ),
    IRON_PICKAXE(
        "Iron Pickaxe",
        "Tier 3 harvesting tool. Unlocks Diamond Ore & deep-chunk excavation (3.5x speed).",
        mapOf(BlockType.IRON_ORE to 3, BlockType.COAL_ORE to 1, BlockType.WOOD_PLANKS to 1),
        toolSpeedMultiplier = 3.5f,
        toolTier = 3,
        craftXpReward = 100
    ),
    DIAMOND_PICKAXE(
        "Diamond Pickaxe",
        "Tier 4 master harvesting tool. Instantaneous block breaking (6.0x speed).",
        mapOf(BlockType.DIAMOND_ORE to 3, BlockType.OAK_LOG to 1),
        toolSpeedMultiplier = 6.0f,
        toolTier = 4,
        craftXpReward = 160
    ),
    TORCH_BUNDLE(
        "Torch Block (x4)",
        "Illuminates dark chunks and suppresses hostile night monster spawns.",
        mapOf(BlockType.COAL_ORE to 1, BlockType.WOOD_PLANKS to 1),
        yieldsBlock = BlockType.TORCH,
        yieldsCount = 4,
        craftXpReward = 30
    ),
    SRF_RAID_TRAP(
        "SRF Raid Beacon",
        "Sub-chunk geometry trap that stacks simultaneous raid events & funnels drops.",
        mapOf(BlockType.IRON_ORE to 2, BlockType.STONE to 4, BlockType.WOOD_PLANKS to 2),
        yieldsBlock = BlockType.RAID_BEACON,
        yieldsCount = 1,
        craftXpReward = 120
    ),
    SRF_SLIME_COMPRESSOR(
        "SRF Slime Region Core",
        "Compresses 16x16x384 chunk regions into Slime Region Format for zero-lag loading.",
        mapOf(BlockType.DIAMOND_ORE to 1, BlockType.COAL_ORE to 2, BlockType.STONE to 2),
        yieldsBlock = BlockType.SLIME_CORE,
        yieldsCount = 1,
        craftXpReward = 140
    ),
    RATION_BREAD(
        "Field Ration",
        "Restores +6 Hunger and triggers natural health regeneration on Game Ticks.",
        mapOf(BlockType.GRASS to 2, BlockType.OAK_LEAVES to 1),
        restoresHunger = 6,
        craftXpReward = 25
    )
}

data class WorldModel(
    val worldId: String = "",
    val name: String = "New World",
    val seed: Long = 1048576L,
    val gameMode: String = GameMode.SURVIVAL.name,
    val srfMode: String = SrfMode.NONE.name,
    val difficulty: String = DifficultyLevel.MEDIUM.name,
    val ownerId: String = "",
    val ownerName: String = "Player",
    val isPublic: Boolean = true,
    val blockEditCount: Long = 0L,
    val parasiteEvolutionPhase: Long = 0L,
    val raidStackedWaves: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class BlockDeltaModel(
    val blockKey: String = "",
    val worldId: String = "",
    val x: Long = 0L,
    val y: Long = 64L,
    val z: Long = 0L,
    val blockType: String = BlockType.STONE.name,
    val updatedBy: String = "",
    val updatedByName: String = "Player",
    val updatedAt: Timestamp? = null
)

data class PlayerPresenceModel(
    val userId: String = "",
    val worldId: String = "",
    val displayName: String = "Explorer",
    val x: Long = 0L,
    val y: Long = 65L,
    val z: Long = 0L,
    val gameMode: String = GameMode.SURVIVAL.name,
    val health: Long = 20L,
    val hunger: Long = 20L,
    val isFlying: Boolean = false,
    val heldItem: String = BlockType.GRASS.name,
    val updatedAt: Timestamp? = null
)

enum class MonsterType(val displayName: String, val colorHex: Long, val maxHp: Int, val attackDamage: Int, val xpReward: Int, val isParasite: Boolean = false) {
    ZOMBIE("Night Zombie", 0xFF3D6B48, 12, 2, 45),
    SKELETON("Bone Archer", 0xFFD0D3D4, 10, 3, 50),
    RAID_PILLAGER("SRF Raid Pillager", 0xFF5D6D7E, 14, 3, 65),
    RUPTER_PARASITE("SRP Rupter Parasite", 0xFF9B59B6, 16, 4, 85, isParasite = true),
    ADAPTED_LONGLARMS("SRP Adapted Longarms", 0xFF6C3483, 28, 6, 140, isParasite = true)
}

data class ActiveMonster(
    val id: String,
    val type: MonsterType,
    val x: Int,
    val z: Int,
    val hp: Int
)
