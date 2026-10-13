package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BlockType
import com.example.data.model.DifficultyLevel
import com.example.data.model.GameMode
import com.example.data.model.SrfMode
import com.example.data.model.WorldModel
import com.example.ui.auth.rememberAsyncDrawableBitmap
import com.example.ui.components.CraftingGridBottomSheet
import com.example.ui.components.HowToPlayVideoGuideSheet
import com.example.ui.components.IsometricVoxelCanvas
import com.example.ui.components.LevelProgressionBottomSheet
import com.example.ui.components.TechnicalArchitectureSheet
import com.example.ui.theme.VoxelCrimsonHeart
import com.example.ui.theme.VoxelDiamondCyan
import com.example.ui.theme.VoxelEmerald
import com.example.ui.theme.VoxelObsidian
import com.example.ui.theme.VoxelParasiteViolet
import com.example.ui.theme.VoxelSlate
import com.example.ui.theme.VoxelSurfaceElevated
import com.example.ui.theme.VoxelTorchGold
import com.example.ui.viewmodel.CrosshairActionMode
import com.example.ui.viewmodel.GameSessionUiState
import com.example.ui.viewmodel.VoxelGameViewModel

@Composable
fun MainVoxelCraftScreen(
    viewModel: VoxelGameViewModel,
    currentUserId: String,
    onSignOut: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val worlds by viewModel.worldsList.collectAsStateWithLifecycle()

    var showCreateWorldDialog by remember { mutableStateOf(false) }
    var showCraftingSheet by remember { mutableStateOf(false) }
    var showTechnicalSheet by remember { mutableStateOf(false) }
    var showLevelSheet by remember { mutableStateOf(false) }
    var showVideoGuideSheet by remember { mutableStateOf(false) }

    if (session.isInWorld && session.activeWorld != null) {
        BackHandler {
            viewModel.exitWorldToLobby()
        }
        ActiveSandboxWorldScreen(
            session = session,
            onMove = viewModel::movePlayer,
            onTileTapped = viewModel::selectGridTile,
            onActionModeChange = viewModel::setActionMode,
            onSelectHotbarBlock = viewModel::selectHotbarBlock,
            onToggleFlight = viewModel::toggleFlight,
            onToggleCaveScanner = viewModel::toggleUndergroundCaveLayer,
            onToggleOfflineFlow = viewModel::toggleSimulatedOfflineMode,
            onManualSync = viewModel::triggerAutoSyncIfOnline,
            onOpenCrafting = { showCraftingSheet = true },
            onOpenTechnicalLab = { showTechnicalSheet = true },
            onOpenLevelSheet = { showLevelSheet = true },
            onOpenVideoGuide = { showVideoGuideSheet = true },
            onInstantLevelUp = viewModel::levelUpNow,
            onExitToLobby = viewModel::exitWorldToLobby
        )
    } else {
        WorldLobbyScreen(
            worlds = worlds,
            session = session,
            currentUserId = currentUserId,
            onEnterWorld = viewModel::enterWorld,
            onDeleteWorld = viewModel::deleteWorld,
            onOpenCreateDialog = { showCreateWorldDialog = true },
            onOpenTechnicalLab = { showTechnicalSheet = true },
            onOpenLevelSheet = { showLevelSheet = true },
            onOpenVideoGuide = { showVideoGuideSheet = true },
            onToggleOfflineFlow = viewModel::toggleSimulatedOfflineMode,
            onManualSync = viewModel::triggerAutoSyncIfOnline,
            onSignOut = onSignOut
        )
    }

    if (showCreateWorldDialog) {
        CreateWorldDialog(
            onDismiss = { showCreateWorldDialog = false },
            onCreate = { name, seed, gameMode, srfMode, isPublic ->
                showCreateWorldDialog = false
                viewModel.createAndEnterWorld(name, seed, gameMode, srfMode, isPublic)
            }
        )
    }

    if (showCraftingSheet) {
        CraftingGridBottomSheet(
            session = session,
            onCraftItem = viewModel::craftItem,
            onSelectHotbarBlock = viewModel::selectHotbarBlock,
            onDismiss = { showCraftingSheet = false }
        )
    }

    if (showTechnicalSheet) {
        TechnicalArchitectureSheet(
            session = session,
            onSelectGameMode = viewModel::setGameMode,
            onSelectSrfMode = viewModel::setSrfMode,
            onChangeChunkRadius = viewModel::setChunkRadius,
            onDismiss = { showTechnicalSheet = false }
        )
    }

    if (showLevelSheet) {
        LevelProgressionBottomSheet(
            session = session,
            onSelectLevel = { level ->
                viewModel.updateGameplayLevel(level)
            },
            onInstantLevelUp = viewModel::levelUpNow,
            onDismiss = { showLevelSheet = false }
        )
    }

    if (showVideoGuideSheet) {
        HowToPlayVideoGuideSheet(
            onDismiss = { showVideoGuideSheet = false },
            onOpenLevelProgression = {
                showVideoGuideSheet = false
                showLevelSheet = true
            }
        )
    }
}

@Composable
private fun WorldLobbyScreen(
    worlds: List<WorldModel>,
    session: GameSessionUiState,
    currentUserId: String,
    onEnterWorld: (WorldModel) -> Unit,
    onDeleteWorld: (String) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onOpenTechnicalLab: () -> Unit,
    onOpenLevelSheet: () -> Unit,
    onOpenVideoGuide: () -> Unit,
    onToggleOfflineFlow: () -> Unit,
    onManualSync: () -> Unit,
    onSignOut: () -> Unit
) {
    val heroBitmap = rememberAsyncDrawableBitmap(R.drawable.img_voxel_hero_banner)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = VoxelObsidian,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreateDialog,
                containerColor = VoxelEmerald,
                contentColor = Color(0xFF042413),
                modifier = Modifier.testTag("create_world_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create World")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Seed World", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Hero Banner Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = VoxelSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF123528), Color(0xFF122C3D))
                                )
                            )
                    ) {
                        if (heroBitmap != null) {
                            Image(
                                bitmap = heroBitmap,
                                contentDescription = "VoxelCraft Multiplayer Lobby",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(165.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.25f), VoxelSlate)
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = VoxelObsidian.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = if (currentUserId == "guest_player") "VOXELCRAFT • DIRECT PLAY READY" else "VOXELCRAFT MULTIPLAYER & OFFLINE ENGINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VoxelEmerald,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = onOpenVideoGuide,
                                        modifier = Modifier.testTag("open_video_guide_lobby_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = "How to Play Video Guide",
                                            tint = VoxelTorchGold
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenLevelSheet,
                                        modifier = Modifier.testTag("open_level_sheet_lobby_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = "Level Progression",
                                            tint = VoxelTorchGold
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenTechnicalLab,
                                        modifier = Modifier.testTag("open_tech_guide_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = "Technical Guide & SRF Lab",
                                            tint = VoxelDiamondCyan
                                        )
                                    }
                                    if (currentUserId != "guest_player") {
                                        IconButton(
                                            onClick = onSignOut,
                                            modifier = Modifier.testTag("sign_out_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = "Sign Out",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                            Text(
                                text = "Infinite Procedural Worlds",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Explore biomes, harvest & craft tools, and build with real-time Firebase sync or offline Room persistence.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Easy Gameplay & How to Play Video Guide Action Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF132030)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenVideoGuide)
                        .testTag("how_to_play_lobby_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VoxelTorchGold.copy(alpha = 0.20f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Play Tutorial Video",
                                    tint = VoxelTorchGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "How to Play (Animated Video)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = VoxelEmerald.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "EASY GUIDE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VoxelEmerald,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Watch simple 1-tap movements, mining, building, and updating level (1..10) while playing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenVideoGuide,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VoxelTorchGold,
                                contentColor = Color(0xFF231600)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Watch", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Offline Connectivity & Room Queue Control Bar
            item {
                OfflineConnectivityBanner(
                    session = session,
                    onToggleOfflineFlow = onToggleOfflineFlow,
                    onManualSync = onManualSync
                )
            }

            // Quick Preset Seed Worlds Banner if no worlds exist yet
            if (worlds.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VoxelSlate),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Multiplayer Worlds Yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Generate your first procedural world from a custom Seed or launch a preset technical realm below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onOpenCreateDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VoxelEmerald,
                                    contentColor = Color(0xFF042413)
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Create World")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Seed World", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "SHARED MULTIPLAYER & OFFLINE-CACHED WORLDS (${worlds.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = VoxelDiamondCyan
                    )
                }
                items(worlds, key = { it.worldId }) { world ->
                    WorldLobbyCard(
                        world = world,
                        isOwner = world.ownerId == currentUserId,
                        onJoin = { onEnterWorld(world) },
                        onDelete = { onDeleteWorld(world.worldId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun OfflineConnectivityBanner(
    session: GameSessionUiState,
    onToggleOfflineFlow: () -> Unit,
    onManualSync: () -> Unit
) {
    val isOffline = session.isEffectiveOffline
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOffline) Color(0xFF33240F) else VoxelSlate
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                contentDescription = if (isOffline) "Offline Flow" else "Online Sync",
                tint = if (isOffline) VoxelTorchGold else VoxelEmerald,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOffline) {
                        "Offline Flow Active (${session.pendingOfflineSyncCount} edits queued in Room)"
                    } else {
                        "Firebase Real-Time Multiplayer Connected (${session.pendingOfflineSyncCount} pending)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOffline) VoxelTorchGold else VoxelEmerald
                )
                Text(
                    text = "Toggle 'Simulate Offline' to test playing & building without internet connectivity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isOffline && session.pendingOfflineSyncCount > 0) {
                IconButton(onClick = onManualSync) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Offline Edits",
                        tint = VoxelDiamondCyan
                    )
                }
            }
            Switch(
                checked = session.simulatedOfflineToggle,
                onCheckedChange = { onToggleOfflineFlow() },
                modifier = Modifier.testTag("offline_mode_switch")
            )
        }
    }
}

@Composable
private fun WorldLobbyCard(
    world: WorldModel,
    isOwner: Boolean,
    onJoin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = VoxelSlate),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onJoin)
            .testTag("world_card_${world.worldId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VoxelSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = world.name,
                    tint = VoxelEmerald,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = world.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (world.gameMode == GameMode.CREATIVE.name) {
                            VoxelDiamondCyan.copy(alpha = 0.18f)
                        } else {
                            VoxelEmerald.copy(alpha = 0.18f)
                        }
                    ) {
                        Text(
                            text = world.gameMode,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (world.gameMode == GameMode.CREATIVE.name) VoxelDiamondCyan else VoxelEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Seed: ${world.seed} • SRF: ${world.srfMode} • Edits: ${world.blockEditCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Host: ${world.ownerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isOwner) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete World",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Button(
                onClick = onJoin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoxelEmerald,
                    contentColor = Color(0xFF042413)
                )
            ) {
                Text("Play", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveSandboxWorldScreen(
    session: GameSessionUiState,
    onMove: (Int, Int) -> Unit,
    onTileTapped: (Int, Int) -> Unit,
    onActionModeChange: (CrosshairActionMode) -> Unit,
    onSelectHotbarBlock: (BlockType) -> Unit,
    onToggleFlight: () -> Unit,
    onToggleCaveScanner: () -> Unit,
    onToggleOfflineFlow: () -> Unit,
    onManualSync: () -> Unit,
    onOpenCrafting: () -> Unit,
    onOpenTechnicalLab: () -> Unit,
    onOpenLevelSheet: () -> Unit,
    onOpenVideoGuide: () -> Unit,
    onInstantLevelUp: () -> Unit,
    onExitToLobby: () -> Unit
) {
    val world = session.activeWorld ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxelObsidian)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // 1. Top Telemetry, Level Progression & Multiplayer/Offline Status HUD
        Surface(
            color = VoxelSlate,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onExitToLobby,
                            modifier = Modifier.size(36.dp).testTag("exit_world_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to World Lobby"
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = world.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${session.currentTps} TPS • ${if (session.isNightTime) "Night (Hostile Mobs)" else "Daylight"} • Seed ${world.seed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoxelDiamondCyan
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // How to Play Video Guide Quick Button
                        IconButton(
                            onClick = onOpenVideoGuide,
                            modifier = Modifier.size(34.dp).testTag("hud_video_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "How to Play Video Guide",
                                tint = VoxelTorchGold
                            )
                        }

                        // Interactive Level Badge Pill (Opens Level Selector Sheet)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = VoxelTorchGold.copy(alpha = 0.22f),
                            modifier = Modifier
                                .clickable { onOpenLevelSheet() }
                                .testTag("hud_level_badge_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Gameplay Level",
                                    tint = VoxelTorchGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LV.${session.currentLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoxelTorchGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Online / Offline pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (session.isEffectiveOffline) {
                                VoxelTorchGold.copy(alpha = 0.2f)
                            } else {
                                VoxelEmerald.copy(alpha = 0.2f)
                            },
                            modifier = Modifier
                                .clickable { onToggleOfflineFlow() }
                                .testTag("hud_offline_toggle_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (session.isEffectiveOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                    contentDescription = "Connectivity Status",
                                    tint = if (session.isEffectiveOffline) VoxelTorchGold else VoxelEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (session.isEffectiveOffline) {
                                        "OFFLINE (${session.pendingOfflineSyncCount})"
                                    } else {
                                        "ONLINE (${session.remotePlayers.size + 1}P)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (session.isEffectiveOffline) VoxelTorchGold else VoxelEmerald
                                )
                            }
                        }

                        if (!session.isEffectiveOffline && session.pendingOfflineSyncCount > 0) {
                            IconButton(
                                onClick = onManualSync,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Pending Offline Edits",
                                    tint = VoxelDiamondCyan
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenTechnicalLab,
                            modifier = Modifier.size(34.dp).testTag("hud_tech_lab_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "SRF & Technical Architecture Lab",
                                tint = VoxelParasiteViolet
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Level-Wise Progression Strip inside Gameplay HUD
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VoxelObsidian.copy(alpha = 0.65f))
                        .clickable { onOpenLevelSheet() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lv.${session.currentLevel}: ${session.levelConfig.title} (${session.levelConfig.miningSpeedBonus}x Speed)",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoxelEmerald
                            )
                            Text(
                                text = "XP ${session.currentXp}/${session.nextLevelConfig?.xpThreshold ?: "MAX"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoxelTorchGold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { session.levelProgressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = VoxelEmerald
                        )
                    }
                    if (session.currentLevel < 10) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = VoxelTorchGold,
                            modifier = Modifier
                                .clickable { onInstantLevelUp() }
                                .testTag("hud_instant_level_up_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Update Level",
                                    tint = Color(0xFF2B1B00),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Lv Up",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2B1B00),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Health, Hunger, Mode & Tool Status Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Health",
                            tint = VoxelCrimsonHeart,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (session.gameMode == GameMode.CREATIVE) "∞ HP" else "${session.health}/20",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "Hunger",
                            tint = VoxelTorchGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (session.gameMode == GameMode.CREATIVE) "∞ Food" else "${session.hunger}/20",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Active Players",
                            tint = VoxelDiamondCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tool: ${session.equippedTool?.displayName ?: "Bare Hands"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoxelEmerald
                        )
                    }
                }

                if (session.miningProgress > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { session.miningProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = VoxelTorchGold
                    )
                }
            }
        }

        // 2. Interactive 3D Isometric Voxel Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            IsometricVoxelCanvas(
                seed = world.seed,
                playerX = session.playerX,
                playerY = session.playerY,
                playerZ = session.playerZ,
                selectedTargetX = session.selectedTargetX,
                selectedTargetZ = session.selectedTargetZ,
                isFlying = session.isFlying,
                isNightTime = session.isNightTime,
                undergroundCaveLayer = session.undergroundCaveLayer,
                srfMode = session.srfMode,
                parasiteEvolutionPhase = session.parasiteEvolutionPhase,
                worldBlockOverrides = session.worldBlockOverrides,
                remotePlayers = session.remotePlayers,
                activeMonsters = session.activeMonsters,
                onTileTapped = onTileTapped
            )

            // Bottom live event ticker inside canvas
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xCC0E141D)
            ) {
                Text(
                    text = session.statusBannerMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Bottom Control Deck: D-Pad Movement, Action Modes, Hotbar & Crafting Button
        Surface(
            color = VoxelSlate,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Action Mode Selector & Special Toggles (Mine, Build, Inspect, Cave Layer, Flight, Crafting)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrosshairActionMode.entries.forEach { mode ->
                        val active = session.actionMode == mode
                        FilterChip(
                            selected = active,
                            onClick = { onActionModeChange(mode) },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (mode) {
                                        CrosshairActionMode.MINE -> Icons.Default.Hardware
                                        CrosshairActionMode.BUILD -> Icons.Default.Build
                                        CrosshairActionMode.INSPECT -> Icons.Default.Search
                                    },
                                    contentDescription = mode.label,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onToggleCaveScanner,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (session.undergroundCaveLayer) VoxelDiamondCyan.copy(alpha = 0.25f) else VoxelSurfaceElevated)
                            .testTag("toggle_cave_layer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle Underground Cave Scanner",
                            tint = if (session.undergroundCaveLayer) VoxelDiamondCyan else Color.White
                        )
                    }

                    IconButton(
                        onClick = onToggleFlight,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (session.isFlying) VoxelTorchGold.copy(alpha = 0.25f) else VoxelSurfaceElevated)
                            .testTag("toggle_flight_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = "Toggle Creative Free Flight",
                            tint = if (session.isFlying) VoxelTorchGold else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // D-Pad + Hotbar + Crafting Bench Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Isometric Movement Pad
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DpadButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            description = "Move West",
                            tag = "move_west_btn",
                            onClick = { onMove(-1, 0) }
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DpadButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                description = "Move North",
                                tag = "move_north_btn",
                                onClick = { onMove(0, -1) }
                            )
                            DpadButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                description = "Move South",
                                tag = "move_south_btn",
                                onClick = { onMove(0, 1) }
                            )
                        }
                        DpadButton(
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            description = "Move East",
                            tag = "move_east_btn",
                            onClick = { onMove(1, 0) }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 5-Slot Quick Block Hotbar
                    val hotbarBlocks = listOf(
                        BlockType.GRASS,
                        BlockType.WOOD_PLANKS,
                        BlockType.STONE,
                        BlockType.TORCH,
                        BlockType.RAID_BEACON
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hotbarBlocks.forEach { block ->
                            val selected = session.selectedHotbarBlock == block
                            val count = if (session.gameMode == GameMode.CREATIVE) 99 else (session.inventory[block] ?: 0)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) VoxelEmerald.copy(alpha = 0.24f) else VoxelObsidian)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) VoxelEmerald else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSelectHotbarBlock(block) }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(block.topColor)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (count >= 99) "∞" else "$count",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Open 3x3 Crafting Grid Button
                    Button(
                        onClick = onOpenCrafting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoxelTorchGold,
                            contentColor = Color(0xFF2B1B00)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("open_crafting_grid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Open Crafting Grid",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Craft", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DpadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = VoxelSurfaceElevated,
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White
            )
        }
    }
}

@Composable
private fun CreateWorldDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, GameMode, SrfMode, Boolean) -> Unit
) {
    var worldName by remember { mutableStateOf("Emerald Highlands") }
    var seedText by remember { mutableStateOf("20261006") }
    var selectedGameMode by remember { mutableStateOf(GameMode.SURVIVAL) }
    var selectedSrfMode by remember { mutableStateOf(SrfMode.NONE) }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoxelSlate,
        title = {
            Text(
                text = "Create Procedural Voxel World",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = worldName,
                    onValueChange = { worldName = it },
                    label = { Text("World Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("world_name_input")
                )
                OutlinedTextField(
                    value = seedText,
                    onValueChange = { seedText = it },
                    label = { Text("Procedural Seed (Number or String)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("world_seed_input")
                )

                Text(
                    text = "Primary Game Mode (Section 3):",
                    style = MaterialTheme.typography.labelMedium,
                    color = VoxelEmerald
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedGameMode == mode,
                            onClick = { selectedGameMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text(
                    text = "SRF Community System (Section 5):",
                    style = MaterialTheme.typography.labelMedium,
                    color = VoxelDiamondCyan
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SrfMode.entries.forEach { srf ->
                        FilterChip(
                            selected = selectedSrfMode == srf,
                            onClick = { selectedSrfMode = srf },
                            label = { Text(srf.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(worldName, seedText, selectedGameMode, selectedSrfMode, isPublic)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoxelEmerald,
                    contentColor = Color(0xFF042413)
                ),
                modifier = Modifier.testTag("confirm_create_world_button")
            ) {
                Text("Generate & Launch", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
