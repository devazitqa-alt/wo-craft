package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockType
import com.example.data.model.CraftedItem
import com.example.data.model.GameMode
import com.example.data.model.SrfMode
import com.example.domain.ProceduralWorldEngine
import com.example.ui.theme.VoxelDiamondCyan
import com.example.ui.theme.VoxelEmerald
import com.example.ui.theme.VoxelObsidian
import com.example.ui.theme.VoxelParasiteViolet
import com.example.ui.theme.VoxelSlate
import com.example.ui.theme.VoxelTorchGold
import com.example.ui.viewmodel.GameSessionUiState

/**
 * Grid-Based Crafting & Inventory Sheet derived from Section 2 ("Gathering & Crafting")
 * and Section 3 ("Primary Game Modes") of the PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CraftingGridBottomSheet(
    session: GameSessionUiState,
    onCraftItem: (CraftedItem) -> Unit,
    onSelectHotbarBlock: (BlockType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoxelSlate
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Crafting Grid",
                        tint = VoxelTorchGold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "3x3 Crafting & Resource Inventory",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (session.gameMode == GameMode.CREATIVE) {
                                "Creative Mode: Infinite resources unlocked"
                            } else {
                                "Survival Mode: Harvest raw materials to craft tools & SRF blocks"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Crafting Sheet")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inventory Block Selector Strip
            Text(
                text = "BLOCK INVENTORY (TAP TO EQUIP IN HOTBAR)",
                style = MaterialTheme.typography.labelMedium,
                color = VoxelEmerald
            )
            Spacer(modifier = Modifier.height(8.dp))

            val placeableBlocks = BlockType.entries.filter { it.isPlaceable }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                placeableBlocks.take(6).forEach { block ->
                    val count = if (session.gameMode == GameMode.CREATIVE) 999 else (session.inventory[block] ?: 0)
                    InventoryBlockBadge(
                        block = block,
                        count = count,
                        isSelected = session.selectedHotbarBlock == block,
                        onClick = { onSelectHotbarBlock(block) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                placeableBlocks.drop(6).take(6).forEach { block ->
                    val count = if (session.gameMode == GameMode.CREATIVE) 999 else (session.inventory[block] ?: 0)
                    InventoryBlockBadge(
                        block = block,
                        count = count,
                        isSelected = session.selectedHotbarBlock == block,
                        onClick = { onSelectHotbarBlock(block) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "GRID CRAFTING RECIPES",
                style = MaterialTheme.typography.labelMedium,
                color = VoxelDiamondCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(CraftedItem.entries) { recipe ->
                    val canCraft = session.gameMode == GameMode.CREATIVE ||
                        recipe.recipeIngredients.all { (block, req) ->
                            (session.inventory[block] ?: 0) >= req
                        }
                    val isUnlockedTool = session.unlockedTools.contains(recipe)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = VoxelObsidian.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = recipe.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isUnlockedTool) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Equipped Tool",
                                            tint = VoxelEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = recipe.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val reqText = recipe.recipeIngredients.entries.joinToString(" + ") { (b, c) ->
                                    val have = if (session.gameMode == GameMode.CREATIVE) "∞" else "${session.inventory[b] ?: 0}"
                                    "${c}x ${b.displayName} ($have)"
                                }
                                Text(
                                    text = "Requires: $reqText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canCraft) VoxelEmerald else VoxelTorchGold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { onCraftItem(recipe) },
                                enabled = canCraft,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VoxelEmerald,
                                    contentColor = Color(0xFF042413)
                                ),
                                modifier = Modifier.testTag("craft_${recipe.name.lowercase()}")
                            ) {
                                Text("Craft", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InventoryBlockBadge(
    block: BlockType,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) VoxelEmerald.copy(alpha = 0.22f) else VoxelObsidian)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) VoxelEmerald else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(block.topColor)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (count >= 999) "∞" else "x$count",
            style = MaterialTheme.typography.labelSmall,
            color = if (count > 0) Color.White else Color.White.copy(alpha = 0.45f)
        )
    }
}

/**
 * Technical Guide & SRF Architecture Sheet derived directly from Sections 3, 4 & 5 of the
 * uploaded Minecraft Complete Overview & Technical Guide PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalArchitectureSheet(
    session: GameSessionUiState,
    onSelectGameMode: (GameMode) -> Unit,
    onSelectSrfMode: (SrfMode) -> Unit,
    onChangeChunkRadius: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val compressionStats = ProceduralWorldEngine.computeSlimeRegionCompression(
        loadedChunksCount = session.loadedChunksCount,
        editedBlocksCount = session.worldBlockOverrides.size
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoxelSlate
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Technical Engine",
                            tint = VoxelDiamondCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Internal Architecture & SRF Lab",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Live controls for Game Ticks (20 TPS), Chunks (16x16x384), Seeds & SRF",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close Technical Sheet")
                    }
                }
            }

            // Section 3: Primary Game Modes Comparison & Switcher
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VoxelObsidian),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SECTION 3 • PRIMARY GAME MODES",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoxelEmerald
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GameMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = session.gameMode == mode,
                                    onClick = { onSelectGameMode(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Survival Mode: Limited health & hunger, manual mining, walking/jumping, active night monster threats.\n" +
                                "• Creative Mode: Infinite health & resources, free flight anywhere, passive monsters ignore player.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 4: Internal Architecture & Mechanics (20 TPS & 16x16x384 Chunks)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VoxelObsidian),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SECTION 4 • 20 TPS CLOCK & 16x16x384 CHUNK SEGMENTATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoxelDiamondCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Clock Loop: ${session.currentTps} TPS (0.05s / tick)",
                                style = MaterialTheme.typography.labelLarge,
                                color = VoxelEmerald
                            )
                            Text(
                                text = "Total Ticks: ${session.totalGameTicks}",
                                style = MaterialTheme.typography.labelLarge,
                                color = VoxelTorchGold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Active Chunk Radius: ${session.activeChunkRadius} (${session.loadedChunksCount} chunks loaded • ${session.frozenDistantChunksCount} distant chunks frozen for CPU efficiency)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = session.activeChunkRadius.toFloat(),
                            onValueChange = { onChangeChunkRadius(it.toInt()) },
                            valueRange = 2f..6f,
                            steps = 3
                        )
                        Text(
                            text = "World Seed: ${session.activeWorld?.seed ?: 0L} (Mathematical pseudorandom layout for biomes, caves & ore veins)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 5: Deciphering the SRF Acronym (Interactive 3 Systems)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VoxelObsidian),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SECTION 5 • DECIPHERING THE SRF ACRONYM",
                            style = MaterialTheme.typography.labelMedium,
                            color = VoxelTorchGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        SrfMode.entries.forEach { mode ->
                            val selected = session.srfMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) VoxelEmerald.copy(alpha = 0.16f) else VoxelSlate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectSrfMode(mode) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            SrfMode.NONE -> Icons.Default.GridOn
                                            SrfMode.RAID_FARM -> Icons.Default.Bolt
                                            SrfMode.SLIME_REGION -> Icons.Default.Compress
                                            SrfMode.PARASITES -> Icons.Default.Coronavirus
                                        },
                                        contentDescription = mode.label,
                                        tint = if (selected) VoxelEmerald else VoxelDiamondCyan
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.acronymFull,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live SRF Telemetry Panel
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VoxelSlate,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "LIVE SRF TELEMETRY",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = VoxelParasiteViolet
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Stacking Raid Farms: ${session.raidStackedWaves} stacked sub-chunk waves | ${session.raidEmeraldsCollected} item drops channeled",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "• Slime Region Format: ${compressionStats.rawRegionBytes / 1024} KB raw -> ${compressionStats.slimeCompressedBytes / 1024} KB compressed (${compressionStats.compressionRatioPercent}% saved)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "• Scape & Run: Parasites: Evolution Phase ${session.parasiteEvolutionPhase}/5 (${session.parasitePoints} mutation pts)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (session.parasiteEvolutionPhase / 5f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = VoxelParasiteViolet
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
