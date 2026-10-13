package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.auth.rememberAsyncDrawableBitmap
import com.example.ui.theme.VoxelCrimsonHeart
import com.example.ui.theme.VoxelDiamondCyan
import com.example.ui.theme.VoxelEmerald
import com.example.ui.theme.VoxelObsidian
import com.example.ui.theme.VoxelParasiteViolet
import com.example.ui.theme.VoxelSlate
import com.example.ui.theme.VoxelSurfaceElevated
import com.example.ui.theme.VoxelTorchGold
import kotlinx.coroutines.delay

enum class TutorialChapter(
    val title: String,
    val icon: ImageVector,
    val durationSeconds: Int,
    val keyAction: String,
    val narrative: String
) {
    MOVEMENT(
        title = "1. Easy Movement",
        icon = Icons.Default.Navigation,
        durationSeconds = 8,
        keyAction = "D-Pad Arrows or Direct Screen Tap",
        narrative = "Navigate effortlessly: Use the 4 directional arrows on the bottom-left D-Pad, or simply TAP ANY TILE directly on the 3D isometric terrain to walk there immediately!"
    ),
    MINING(
        title = "2. Quick Mining",
        icon = Icons.Default.Hardware,
        durationSeconds = 9,
        keyAction = "Mine Mode -> Tap Block",
        narrative = "Gather essential resources easily: Toggle 'Mine / Harvest' mode at the bottom, then tap any grass, oak log, stone, or ore block to harvest it into your bag and gain XP!"
    ),
    BUILDING(
        title = "3. Simple Building",
        icon = Icons.Default.Build,
        durationSeconds = 9,
        keyAction = "Pick Hotbar Slot -> Tap Tile",
        narrative = "Construct bridges and fortresses with ease: Select a block from your 5-slot bottom hotbar (wood, stone, torch), then tap where you want to place it."
    ),
    LEVEL_UP(
        title = "4. Level-Wise Update",
        icon = Icons.Default.EmojiEvents,
        durationSeconds = 10,
        keyAction = "Tap LV Pill in HUD -> Choose 1..10",
        narrative = "Update your gameplay level anytime: Tap the golden 'LV.' badge or progress bar at the top of your screen to jump to any of the 10 levels, boosting mining speed and unlocking rare gear!"
    ),
    CRAFTING_TECH(
        title = "5. Crafting & Modes",
        icon = Icons.Default.Layers,
        durationSeconds = 9,
        keyAction = "Craft Button, Cave Scan & Flight",
        narrative = "Master advanced tools: Tap 'Craft' to make pickaxes, swords, and beacons. Tap the Layers button to scan underground cave ores, or toggle Flight to soar in Creative mode!"
    )
}

/**
 * Interactive "How to Play" Video & Step-by-Step Gameplay Guide Bottom Sheet.
 * Features an interactive visual demo player (simulated animated gameplay video screen with play/pause,
 * scrubber, chapter jump, and animated avatar demonstrating moves) plus a comprehensive quick-reference manual.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayVideoGuideSheet(
    onDismiss: () -> Unit,
    onOpenLevelProgression: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Video Player, 1 = Quick Reference Card
    val bannerBitmap = rememberAsyncDrawableBitmap(R.drawable.img_gameplay_tutorial_banner)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoxelObsidian,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header Bar
            Surface(
                color = VoxelSlate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = VoxelTorchGold.copy(alpha = 0.20f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "How to Play Video Guide",
                                    tint = VoxelTorchGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "How to Play VoxelCraft",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Animated Gameplay Video & Easy Controls",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoxelEmerald
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_how_to_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Guide",
                            tint = Color.White
                        )
                    }
                }
            }

            // Tabs: Interactive Video Player vs Step-by-Step Guide
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = VoxelSlate,
                contentColor = VoxelDiamondCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VoxelDiamondCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video Guide", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Rules", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                InteractiveVideoDemoPlayer(
                    onOpenLevelProgression = onOpenLevelProgression,
                    onDismiss = onDismiss
                )
            } else {
                QuickReferenceRulesList(
                    bannerBitmap = bannerBitmap,
                    onOpenLevelProgression = onOpenLevelProgression,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * Animated video simulation demonstrating each core gameplay mechanic in real time:
 * - Movement (D-pad & touch tap)
 * - Mining (crosshair & progress bar)
 * - Building (hotbar placing)
 * - Level-wise updates (XP thresholds, level bar & instant upgrade)
 * - Crafting & Flight
 */
@Composable
private fun InteractiveVideoDemoPlayer(
    onOpenLevelProgression: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var activeChapter by remember { mutableStateOf(TutorialChapter.MOVEMENT) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPlaybackSec by remember { mutableFloatStateOf(0f) }

    // Auto-advance playback loop when playing
    LaunchedEffect(isPlaying, activeChapter) {
        while (isPlaying) {
            delay(100L)
            currentPlaybackSec += 0.1f
            if (currentPlaybackSec >= activeChapter.durationSeconds) {
                currentPlaybackSec = 0f
                // Advance to next chapter
                val nextIndex = (activeChapter.ordinal + 1) % TutorialChapter.entries.size
                activeChapter = TutorialChapter.entries[nextIndex]
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Video Screen Container
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B12)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VoxelDiamondCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            ) {
                Column {
                    // Video Canvas Screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0F1E2E), Color(0xFF081018))
                                )
                            )
                    ) {
                        AnimatedTutorialCanvas(
                            chapter = activeChapter,
                            playbackProgress = (currentPlaybackSec / activeChapter.durationSeconds).coerceIn(0f, 1f)
                        )

                        // Top Video Overlay: LIVE VIDEO TUTORIAL tag & Chapter Name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xCC000000)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Color(0xFFFF4136) else VoxelTorchGold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlaying) "DEMO VIDEO PLAYING" else "PAUSED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VoxelEmerald.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = activeChapter.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoxelEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Bottom Video Scrubber Progress
                        LinearProgressIndicator(
                            progress = { (currentPlaybackSec / activeChapter.durationSeconds).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter),
                            color = VoxelTorchGold,
                            trackColor = Color(0x44FFFFFF)
                        )
                    }

                    // Video Playback Controls Bar
                    Surface(
                        color = VoxelSlate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val prevIndex = if (activeChapter.ordinal == 0) TutorialChapter.entries.size - 1 else activeChapter.ordinal - 1
                                        activeChapter = TutorialChapter.entries[prevIndex]
                                        currentPlaybackSec = 0f
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.FastRewind, contentDescription = "Previous Chapter", tint = Color.White)
                                }

                                IconButton(
                                    onClick = { isPlaying = !isPlaying },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(VoxelTorchGold)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color(0xFF231600)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val nextIndex = (activeChapter.ordinal + 1) % TutorialChapter.entries.size
                                        activeChapter = TutorialChapter.entries[nextIndex]
                                        currentPlaybackSec = 0f
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.FastForward, contentDescription = "Next Chapter", tint = Color.White)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "${currentPlaybackSec.toInt()}s / ${activeChapter.durationSeconds}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { currentPlaybackSec = 0f },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = "Replay Chapter", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Current Chapter Explainer Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VoxelSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = VoxelEmerald.copy(alpha = 0.20f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = activeChapter.icon,
                                    contentDescription = activeChapter.title,
                                    tint = VoxelEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeChapter.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Action: ${activeChapter.keyAction}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VoxelDiamondCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = activeChapter.narrative,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Chapter Selection Chips
        item {
            Text(
                text = "TUTORIAL CHAPTERS",
                style = MaterialTheme.typography.labelMedium,
                color = VoxelDiamondCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TutorialChapter.entries.forEach { chapter ->
                    val selected = activeChapter == chapter
                    FilterChip(
                        selected = selected,
                        onClick = {
                            activeChapter = chapter
                            currentPlaybackSec = 0f
                            isPlaying = true
                        },
                        label = {
                            Text(
                                text = chapter.title.substringBefore("."),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoxelEmerald.copy(alpha = 0.22f),
                            selectedLabelColor = VoxelEmerald
                        )
                    )
                }
            }
        }

        // Action Prompts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOpenLevelProgression != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenLevelProgression()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoxelTorchGold,
                            contentColor = Color(0xFF231600)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_level_sheet_from_guide_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Level", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoxelEmerald,
                        contentColor = Color(0xFF042413)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_playing_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Playing", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Custom canvas animation demonstrating mechanics inside the simulated video screen.
 */
@Composable
private fun AnimatedTutorialCanvas(
    chapter: TutorialChapter,
    playbackProgress: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Draw isometric mini grid floor
        val tileSize = 36f
        for (dx in -3..3) {
            for (dz in -2..2) {
                val sx = cx + (dx - dz) * (tileSize * 0.9f)
                val sy = cy + (dx + dz) * (tileSize * 0.45f) + 20f

                val topFace = Path().apply {
                    moveTo(sx, sy - tileSize * 0.45f)
                    lineTo(sx + tileSize * 0.9f, sy)
                    lineTo(sx, sy + tileSize * 0.45f)
                    lineTo(sx - tileSize * 0.9f, sy)
                    close()
                }
                drawPath(topFace, color = Color(0xFF1E3A2C).copy(alpha = 0.75f))
                drawPath(topFace, color = Color(0xFF2F5E46), style = Stroke(width = 1f))
            }
        }

        // Draw animated mechanic according to Chapter
        when (chapter) {
            TutorialChapter.MOVEMENT -> {
                // Moving player avatar across the tiles
                val moveOffset = (playbackProgress * 120f) - 60f
                val playerPos = Offset(cx + moveOffset, cy + moveOffset * 0.5f)

                // Simulated finger tap ripple indicator
                drawCircle(
                    color = VoxelDiamondCyan.copy(alpha = pulseAlpha),
                    radius = 24f + (pulseAlpha * 12f),
                    center = Offset(cx + 40f, cy + 20f),
                    style = Stroke(width = 2.5f)
                )

                // Player avatar circle
                drawCircle(color = VoxelTorchGold, radius = 14f, center = playerPos)
                drawCircle(color = VoxelEmerald, radius = 10f, center = playerPos)

                // Arrow pointing to destination
                drawLine(
                    color = VoxelDiamondCyan,
                    start = Offset(cx - 30f, cy - 15f),
                    end = Offset(cx + 40f, cy + 20f),
                    strokeWidth = 3f
                )
            }

            TutorialChapter.MINING -> {
                // Target block being mined with progress ring
                val targetBlockCenter = Offset(cx + 35f, cy + 10f)

                // Highlight border around block
                drawCircle(
                    color = VoxelTorchGold.copy(alpha = pulseAlpha),
                    radius = 26f,
                    center = targetBlockCenter,
                    style = Stroke(width = 3f)
                )

                // Mining pickaxe swing angle simulation
                val miningFraction = playbackProgress.coerceIn(0f, 1f)
                drawArc(
                    color = VoxelTorchGold,
                    startAngle = -90f,
                    sweepAngle = 360f * miningFraction,
                    useCenter = false,
                    topLeft = Offset(targetBlockCenter.x - 28f, targetBlockCenter.y - 28f),
                    size = Size(56f, 56f),
                    style = Stroke(width = 5f)
                )

                // Player standing nearby
                drawCircle(color = VoxelTorchGold, radius = 12f, center = Offset(cx - 35f, cy - 10f))
                drawCircle(color = VoxelEmerald, radius = 9f, center = Offset(cx - 35f, cy - 10f))
            }

            TutorialChapter.BUILDING -> {
                // Hotbar representation on bottom
                val hotbarY = h - 26f
                for (i in 0..4) {
                    val bx = cx - 70f + (i * 35f)
                    val isSelected = i == 1
                    drawRoundRect(
                        color = if (isSelected) VoxelEmerald else Color(0x88182230),
                        topLeft = Offset(bx - 14f, hotbarY - 14f),
                        size = Size(28f, 28f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = if (isSelected) Stroke(width = 2.5f) else Stroke(width = 1f)
                    )
                }

                // Newly placed block rising up
                val rise = (playbackProgress * 15f).coerceIn(0f, 15f)
                val placedCenter = Offset(cx + 30f, cy - rise)
                drawRoundRect(
                    color = Color(0xFFC49A45),
                    topLeft = Offset(placedCenter.x - 14f, placedCenter.y - 14f),
                    size = Size(28f, 28f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            TutorialChapter.LEVEL_UP -> {
                // HUD Level Progression bar at top center
                val barW = 180f
                val barX = cx - (barW / 2f)
                val barY = 40f

                drawRoundRect(
                    color = Color(0x66000000),
                    topLeft = Offset(barX, barY),
                    size = Size(barW, 16f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                drawRoundRect(
                    color = VoxelEmerald,
                    topLeft = Offset(barX, barY),
                    size = Size(barW * playbackProgress, 16f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Gold level badge pulsing
                drawCircle(
                    color = VoxelTorchGold,
                    radius = 20f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF231600),
                    radius = 16f,
                    center = Offset(cx, cy)
                )
            }

            TutorialChapter.CRAFTING_TECH -> {
                // 3x3 crafting grid sketch
                val gridOrigin = Offset(cx - 36f, cy - 36f)
                val slotSize = 22f

                for (r in 0..2) {
                    for (c in 0..2) {
                        drawRoundRect(
                            color = Color(0xFF33465C),
                            topLeft = Offset(gridOrigin.x + (c * 26f), gridOrigin.y + (r * 26f)),
                            size = Size(slotSize, slotSize),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                // Output sword or tool
                drawCircle(
                    color = VoxelDiamondCyan.copy(alpha = pulseAlpha),
                    radius = 20f,
                    center = Offset(cx + 60f, cy),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

/**
 * Step-by-step easy rules card & infographic layout.
 */
@Composable
private fun QuickReferenceRulesList(
    bannerBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onOpenLevelProgression: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (bannerBitmap != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VoxelSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        bitmap = bannerBitmap,
                        contentDescription = "VoxelCraft Tutorial Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        item {
            Text(
                text = "EASY 3-STEP GAMEPLAY LOOP",
                style = MaterialTheme.typography.labelMedium,
                color = VoxelDiamondCyan
            )
        }

        item {
            RuleCard(
                stepNumber = "1",
                title = "Move & Explore",
                description = "Tap any block directly on the 3D isometric terrain or press the bottom-left D-Pad arrow keys. Fly freely in Creative mode!",
                icon = Icons.Default.TouchApp,
                accentColor = VoxelEmerald
            )
        }

        item {
            RuleCard(
                stepNumber = "2",
                title = "Mine & Build in One Tap",
                description = "Choose 'Mine / Harvest' to harvest wood, stone, and ores into your inventory. Choose 'Build / Place' and pick a hotbar slot to place blocks on the ground.",
                icon = Icons.Default.Build,
                accentColor = VoxelTorchGold
            )
        }

        item {
            RuleCard(
                stepNumber = "3",
                title = "Update Level While Playing",
                description = "Tap the golden 'LV.' pill at the top of the screen at any moment! Jump to Level 1 through Level 10 to instantly update your character's speed, tool tier, and bonus inventory.",
                icon = Icons.Default.EmojiEvents,
                accentColor = VoxelDiamondCyan
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = VoxelSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Real-Time Multiplayer & Offline Flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• When ONLINE: Live block modifications and remote player avatars synchronize via Firebase Firestore in real time.\n" +
                                "• When OFFLINE: The game smoothly continues without interruption! All block edits, levels, and worlds are automatically saved into local Room Database and synced when you reconnect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOpenLevelProgression != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            onOpenLevelProgression()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoxelTorchGold,
                            contentColor = Color(0xFF231600)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_level_sheet_from_quick_rules_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Level", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VoxelEmerald,
                        contentColor = Color(0xFF042413)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("got_it_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It, Let's Play!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VoxelSlate),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.20f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
