package com.example.ui.components

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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.data.model.GamePlayLevel
import com.example.ui.theme.VoxelDiamondCyan
import com.example.ui.theme.VoxelEmerald
import com.example.ui.theme.VoxelObsidian
import com.example.ui.theme.VoxelSlate
import com.example.ui.theme.VoxelTorchGold
import com.example.ui.viewmodel.GameSessionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelProgressionBottomSheet(
    session: GameSessionUiState,
    onSelectLevel: (Int) -> Unit,
    onInstantLevelUp: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentSpec = session.levelConfig
    val nextSpec = session.nextLevelConfig

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
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Level Progression",
                        tint = VoxelTorchGold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Level-Wise Gameplay Progression",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Earn XP by mining, building & crafting — or switch levels anytime",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close Level Sheet")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = VoxelObsidian),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CURRENT LEVEL ${session.currentLevel} / 10",
                                style = MaterialTheme.typography.labelMedium,
                                color = VoxelEmerald
                            )
                            Text(
                                text = currentSpec.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = currentSpec.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VoxelDiamondCyan
                            )
                        }
                        if (session.currentLevel < 10) {
                            Button(
                                onClick = onInstantLevelUp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VoxelTorchGold,
                                    contentColor = Color(0xFF2B1B00)
                                ),
                                modifier = Modifier.testTag("instant_level_up_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Level Up Now",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Level Up", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { session.levelProgressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = VoxelEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "XP: ${session.currentXp} / ${nextSpec?.xpThreshold ?: "MAX"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoxelTorchGold
                        )
                        Text(
                            text = "Mining Speed: ${currentSpec.miningSpeedBonus}x • Chunk Radius: ${session.activeChunkRadius}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoxelDiamondCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mission Goal: ${currentSpec.missionGoalText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Level Stats: ${session.blocksMinedInLevel} mined • ${session.blocksPlacedInLevel} built • ${session.itemsCraftedInLevel} crafted • ${session.monstersDefeatedInLevel} mobs defeated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "SELECT OR UPDATE GAMEPLAY LEVEL (LEVELS 1–10)",
                style = MaterialTheme.typography.labelMedium,
                color = VoxelDiamondCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GamePlayLevel.entries) { lvl ->
                    val isCurrent = session.currentLevel == lvl.levelNumber
                    val isUnlocked = session.currentXp >= lvl.xpThreshold || lvl.levelNumber <= session.currentLevel

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                VoxelEmerald.copy(alpha = 0.16f)
                            } else {
                                VoxelObsidian.copy(alpha = 0.85f)
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) VoxelEmerald else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLevel(lvl.levelNumber) }
                            .testTag("select_level_${lvl.levelNumber}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) VoxelEmerald else VoxelSlate,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "L${lvl.levelNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isCurrent) Color(0xFF042413) else VoxelTorchGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lvl.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isUnlocked) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Unlocked",
                                            tint = VoxelTorchGold,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = lvl.missionGoalText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val rewardSummary = lvl.rewardBlocks.entries.joinToString(", ") {
                                    "+${it.value} ${it.key.displayName}"
                                }
                                Text(
                                    text = "Speed ${lvl.miningSpeedBonus}x • SRF: ${lvl.defaultSrfMode.label} • Rewards: $rewardSummary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VoxelEmerald
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onSelectLevel(lvl.levelNumber) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrent) VoxelDiamondCyan else VoxelEmerald,
                                    contentColor = Color(0xFF042413)
                                )
                            ) {
                                Text(
                                    text = if (isCurrent) "Active" else "Select",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
