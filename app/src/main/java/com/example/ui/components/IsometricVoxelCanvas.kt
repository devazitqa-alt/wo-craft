package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveMonster
import com.example.data.model.BlockDeltaModel
import com.example.data.model.BlockType
import com.example.data.model.PlayerPresenceModel
import com.example.data.model.SrfMode
import com.example.domain.ProceduralWorldEngine
import com.example.ui.theme.VoxelDiamondCyan
import com.example.ui.theme.VoxelEmerald
import com.example.ui.theme.VoxelParasiteViolet
import com.example.ui.theme.VoxelTorchGold
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive 2.5D Isometric Voxel Sandbox Renderer.
 * Renders a 9x9 grid centered on the player's current coordinates (`playerX`, `playerZ`),
 * complete with elevation shadows, 16x16x384 chunk boundary lines, ore veins when the cave
 * scanner is active, remote multiplayer avatars, hostile monsters, and tap-to-mine/place blocks.
 */
@Composable
fun IsometricVoxelCanvas(
    seed: Long,
    playerX: Int,
    playerY: Int,
    playerZ: Int,
    selectedTargetX: Int,
    selectedTargetZ: Int,
    isFlying: Boolean,
    isNightTime: Boolean,
    undergroundCaveLayer: Boolean,
    srfMode: SrfMode,
    parasiteEvolutionPhase: Int,
    worldBlockOverrides: Map<String, BlockDeltaModel>,
    remotePlayers: List<PlayerPresenceModel>,
    activeMonsters: List<ActiveMonster>,
    onTileTapped: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridRadius = 4 // 9x9 visible isometric slice around player
    var canvasPixelSize by remember { mutableStateOf(IntSize.Zero) }

    val skyTop = when {
        srfMode == SrfMode.PARASITES && parasiteEvolutionPhase >= 2 -> Color(0xFF1E0924)
        isNightTime -> Color(0xFF070B16)
        else -> Color(0xFF15324B)
    }
    val skyBottom = when {
        srfMode == SrfMode.PARASITES && parasiteEvolutionPhase >= 2 -> Color(0xFF391242)
        isNightTime -> Color(0xFF101B2E)
        else -> Color(0xFF245475)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(skyTop, skyBottom)))
            .onSizeChanged { canvasPixelSize = it }
            .testTag("isometric_voxel_canvas")
            .pointerInput(playerX, playerZ, canvasPixelSize) {
                detectTapGestures { tapOffset ->
                    if (canvasPixelSize.width <= 0 || canvasPixelSize.height <= 0) return@detectTapGestures
                    val tileW = min(canvasPixelSize.width / 10.5f, canvasPixelSize.height / 7.5f)
                    val tileH = tileW * 0.52f
                    val centerX = canvasPixelSize.width / 2f
                    val centerY = canvasPixelSize.height * 0.50f

                    // Invert isometric projection to find closest (dx, dz) in -gridRadius..gridRadius
                    var bestX = playerX
                    var bestZ = playerZ
                    var bestDist = Float.MAX_VALUE

                    for (sum in (-gridRadius * 2)..(gridRadius * 2)) {
                        for (dx in -gridRadius..gridRadius) {
                            val dz = sum - dx
                            if (dz < -gridRadius || dz > gridRadius) continue
                            val wx = playerX + dx
                            val wz = playerZ + dz
                            val col = ProceduralWorldEngine.getNaturalColumn(wx, wz, seed, parasiteEvolutionPhase)
                            val heightDelta = (col.surfaceY - ProceduralWorldEngine.DEFAULT_SURFACE_BASE).coerceIn(-6, 10)
                            val sx = centerX + (dx - dz) * (tileW / 2f)
                            val sy = centerY + (dx + dz) * (tileH / 2f) - heightDelta * (tileH * 0.36f)
                            val dist = abs(tapOffset.x - sx) + abs(tapOffset.y - sy)
                            if (dist < bestDist) {
                                bestDist = dist
                                bestX = wx
                                bestZ = wz
                            }
                        }
                    }
                    onTileTapped(bestX, bestZ)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tileW = min(size.width / 10.5f, size.height / 7.5f)
            val tileH = tileW * 0.52f
            val blockDepth = tileH * 0.92f
            val centerX = size.width / 2f
            val centerY = size.height * 0.50f

            // Render back-to-front along isometric depth (dx + dz)
            for (sum in (-gridRadius * 2)..(gridRadius * 2)) {
                for (dx in -gridRadius..gridRadius) {
                    val dz = sum - dx
                    if (dz < -gridRadius || dz > gridRadius) continue

                    val wx = playerX + dx
                    val wz = playerZ + dz
                    val col = ProceduralWorldEngine.getNaturalColumn(wx, wz, seed, parasiteEvolutionPhase)
                    val key = "${wx}_${wz}"
                    val overrideDelta = worldBlockOverrides[key]
                    val block = overrideDelta?.let { BlockType.fromKey(it.blockType) }
                        ?: if (undergroundCaveLayer) col.undergroundOre else col.surfaceBlock

                    val heightDelta = if (undergroundCaveLayer) {
                        if (col.hasCaveVoid) -3 else 0
                    } else {
                        val base = (col.surfaceY - ProceduralWorldEngine.DEFAULT_SURFACE_BASE).coerceIn(-6, 10)
                        if (overrideDelta != null && block != BlockType.AIR) base + 1 else base
                    }

                    val sx = centerX + (dx - dz) * (tileW / 2f)
                    val sy = centerY + (dx + dz) * (tileH / 2f) - heightDelta * (tileH * 0.36f)

                    if (block != BlockType.AIR) {
                        drawIsometricCube(
                            cx = sx,
                            cy = sy,
                            tileW = tileW,
                            tileH = tileH,
                            depth = blockDepth,
                            topColor = darkenForNight(block.topColor, isNightTime),
                            leftColor = darkenForNight(block.leftSideColor, isNightTime),
                            rightColor = darkenForNight(block.rightSideColor, isNightTime),
                            isChunkBoundary = (wx % 16 == 0 || wz % 16 == 0),
                            isSelectedTarget = (wx == selectedTargetX && wz == selectedTargetZ),
                            isModifiedByPlayer = overrideDelta != null
                        )
                    }

                    // Draw remote multiplayer player if standing on this coordinate
                    val remoteHere = remotePlayers.firstOrNull { it.x.toInt() == wx && it.z.toInt() == wz }
                    if (remoteHere != null) {
                        drawPlayerMarker(
                            cx = sx,
                            cy = sy - tileH * 0.55f,
                            radius = tileW * 0.22f,
                            bodyColor = VoxelDiamondCyan,
                            ringColor = Color.White
                        )
                    }

                    // Draw hostile monster if standing on this coordinate
                    val monsterHere = activeMonsters.firstOrNull { it.x == wx && it.z == wz }
                    if (monsterHere != null) {
                        drawPlayerMarker(
                            cx = sx,
                            cy = sy - tileH * 0.55f,
                            radius = tileW * 0.23f,
                            bodyColor = Color(monsterHere.type.colorHex),
                            ringColor = Color(0xFFFF5252)
                        )
                    }

                    // Draw local active player at center (0, 0)
                    if (dx == 0 && dz == 0) {
                        val flightOffset = if (isFlying) tileH * 1.15f else tileH * 0.55f
                        if (isFlying) {
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                radius = tileW * 0.20f,
                                center = Offset(sx, sy)
                            )
                        }
                        drawPlayerMarker(
                            cx = sx,
                            cy = sy - flightOffset,
                            radius = tileW * 0.25f,
                            bodyColor = VoxelEmerald,
                            ringColor = VoxelTorchGold
                        )
                    }
                }
            }
        }

        // Top-Right Chunk & Biome Coordinate Pill
        val centerCol = remember(playerX, playerZ, seed, parasiteEvolutionPhase) {
            ProceduralWorldEngine.getNaturalColumn(playerX, playerZ, seed, parasiteEvolutionPhase)
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xCC0E141D)
        ) {
            Text(
                text = "XYZ ($playerX, $playerY, $playerZ) • Chunk (${centerCol.chunkX}, ${centerCol.chunkZ}) • ${centerCol.biome.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = if (srfMode == SrfMode.PARASITES) VoxelParasiteViolet else VoxelDiamondCyan,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun darkenForNight(color: Color, isNight: Boolean): Color {
    if (!isNight) return color
    return Color(
        red = color.red * 0.62f,
        green = color.green * 0.66f,
        blue = (color.blue * 0.78f).coerceAtMost(1f),
        alpha = color.alpha
    )
}

private fun DrawScope.drawIsometricCube(
    cx: Float,
    cy: Float,
    tileW: Float,
    tileH: Float,
    depth: Float,
    topColor: Color,
    leftColor: Color,
    rightColor: Color,
    isChunkBoundary: Boolean,
    isSelectedTarget: Boolean,
    isModifiedByPlayer: Boolean
) {
    val halfW = tileW / 2f
    val halfH = tileH / 2f

    val topFace = Path().apply {
        moveTo(cx, cy - halfH)
        lineTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx - halfW, cy)
        close()
    }

    val leftFace = Path().apply {
        moveTo(cx - halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx, cy + halfH + depth)
        lineTo(cx - halfW, cy + depth)
        close()
    }

    val rightFace = Path().apply {
        moveTo(cx + halfW, cy)
        lineTo(cx, cy + halfH)
        lineTo(cx, cy + halfH + depth)
        lineTo(cx + halfW, cy + depth)
        close()
    }

    drawPath(leftFace, color = leftColor)
    drawPath(rightFace, color = rightColor)
    drawPath(topFace, color = topColor)

    // Subtle voxel edge outline
    val strokeColor = when {
        isSelectedTarget -> VoxelTorchGold
        isModifiedByPlayer -> VoxelEmerald.copy(alpha = 0.75f)
        isChunkBoundary -> VoxelDiamondCyan.copy(alpha = 0.40f)
        else -> Color.Black.copy(alpha = 0.22f)
    }
    val strokeWidth = if (isSelectedTarget) 3.2f else if (isChunkBoundary) 1.6f else 1.0f
    drawPath(topFace, color = strokeColor, style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawPlayerMarker(
    cx: Float,
    cy: Float,
    radius: Float,
    bodyColor: Color,
    ringColor: Color
) {
    drawCircle(
        color = ringColor,
        radius = radius * 1.18f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = bodyColor,
        radius = radius * 0.88f,
        center = Offset(cx, cy)
    )
}
