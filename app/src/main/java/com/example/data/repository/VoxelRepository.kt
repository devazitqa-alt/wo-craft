package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.local.LocalBlockEntity
import com.example.data.local.LocalWorldEntity
import com.example.data.local.VoxelWorldDao
import com.example.data.model.BlockDeltaModel
import com.example.data.model.BlockType
import com.example.data.model.GameMode
import com.example.data.model.PlayerPresenceModel
import com.example.data.model.SrfMode
import com.example.data.model.WorldModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class VoxelRepository(
    private val db: FirebaseFirestore,
    private val localDao: VoxelWorldDao? = null
) {
    constructor(context: Context, localDao: VoxelWorldDao? = null) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localDao
    )

    private val auth
        get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentDisplayName(): String {
        val raw = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Crafter"
        return raw.take(50)
    }

    fun observePublicWorlds(): Flow<List<WorldModel>> {
        val path = "worlds"
        return db.collection(path)
            .whereEqualTo("isPublic", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WorldModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyWorlds(userId: String): Flow<List<WorldModel>> {
        val path = "worlds"
        return db.collection(path)
            .whereEqualTo("ownerId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WorldModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getWorldById(worldId: String): Result<WorldModel?> = runCatching {
        val path = "worlds/$worldId"
        try {
            val snap = db.collection("worlds").document(worldId).get().await()
            snap.toObject(WorldModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            throw e
        }
    }

    suspend fun createWorld(
        name: String,
        seed: Long,
        gameMode: GameMode,
        srfMode: SrfMode,
        isPublic: Boolean,
        isOfflineMode: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val displayName = currentDisplayName()
        val cleanName = name.trim().ifEmpty { "Overworld" }.take(60)
        val worldId = "w_" + UUID.randomUUID().toString().replace("-", "").take(20)

        // Always persist immediately to local Room DB for offline resilience
        localDao?.upsertWorld(
            LocalWorldEntity(
                worldId = worldId,
                name = cleanName,
                seed = seed,
                gameMode = gameMode.name,
                srfMode = srfMode.name,
                ownerId = uid,
                ownerName = displayName,
                isPublic = isPublic,
                blockEditCount = 0,
                parasiteEvolutionPhase = 0,
                raidStackedWaves = 0,
                pendingSync = isOfflineMode
            )
        )

        if (!isOfflineMode) {
            val path = "worlds/$worldId"
            val payload = mapOf(
                "worldId" to worldId,
                "name" to cleanName,
                "seed" to seed,
                "gameMode" to gameMode.name,
                "srfMode" to srfMode.name,
                "ownerId" to uid,
                "ownerName" to displayName,
                "isPublic" to isPublic,
                "blockEditCount" to 0L,
                "parasiteEvolutionPhase" to 0L,
                "raidStackedWaves" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                db.collection("worlds").document(worldId).set(payload).await()
                localDao?.markWorldSynced(worldId)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, path)
                throw e
            }
        }
        worldId
    }

    suspend fun updateWorldTelemetry(
        worldId: String,
        blockEditCount: Int,
        parasiteEvolutionPhase: Int,
        raidStackedWaves: Int,
        isOfflineMode: Boolean = false
    ): Result<Unit> = runCatching {
        requireUserId()
        localDao?.getWorldById(worldId)?.let { existing ->
            localDao.upsertWorld(
                existing.copy(
                    blockEditCount = blockEditCount,
                    parasiteEvolutionPhase = parasiteEvolutionPhase.coerceIn(0, 5),
                    raidStackedWaves = raidStackedWaves.coerceIn(0, 100),
                    pendingSync = isOfflineMode,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
        if (!isOfflineMode) {
            val path = "worlds/$worldId"
            val updates = mapOf(
                "blockEditCount" to blockEditCount.coerceAtLeast(0).toLong(),
                "parasiteEvolutionPhase" to parasiteEvolutionPhase.coerceIn(0, 5).toLong(),
                "raidStackedWaves" to raidStackedWaves.coerceIn(0, 100).toLong(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                db.collection("worlds").document(worldId).update(updates).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, path)
                throw e
            }
        }
    }

    suspend fun deleteWorld(worldId: String): Result<Unit> = runCatching {
        requireUserId()
        localDao?.deleteWorld(worldId)
        val path = "worlds/$worldId"
        try {
            db.collection("worlds").document(worldId).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    fun observeWorldBlocks(worldId: String): Flow<List<BlockDeltaModel>> {
        val path = "worlds/$worldId/blocks"
        return db.collection("worlds")
            .document(worldId)
            .collection("blocks")
            .whereEqualTo("worldId", worldId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BlockDeltaModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun writeBlockEdit(
        worldId: String,
        x: Int,
        y: Int,
        z: Int,
        blockType: BlockType,
        isOfflineMode: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val displayName = currentDisplayName()
        val safeX = x.coerceIn(-30000, 30000)
        val safeY = y.coerceIn(0, 384)
        val safeZ = z.coerceIn(-30000, 30000)
        val blockKey = "b_${safeX}_${safeY}_${safeZ}".replace("-", "m")

        // Write immediately to local Room database so offline gameplay works with zero latency
        localDao?.upsertBlock(
            LocalBlockEntity(
                worldId = worldId,
                blockKey = blockKey,
                x = safeX,
                y = safeY,
                z = safeZ,
                blockType = blockType.name,
                updatedBy = uid,
                updatedByName = displayName,
                pendingSync = true
            )
        )

        if (!isOfflineMode) {
            val path = "worlds/$worldId/blocks/$blockKey"
            val payload = mapOf(
                "blockKey" to blockKey,
                "worldId" to worldId,
                "x" to safeX.toLong(),
                "y" to safeY.toLong(),
                "z" to safeZ.toLong(),
                "blockType" to blockType.name,
                "updatedBy" to uid,
                "updatedByName" to displayName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                db.collection("worlds")
                    .document(worldId)
                    .collection("blocks")
                    .document(blockKey)
                    .set(payload)
                    .await()
                localDao?.markBlockSynced(worldId, blockKey)
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, path)
                throw e
            }
        }
        blockKey
    }

    fun observeWorldPlayers(worldId: String): Flow<List<PlayerPresenceModel>> {
        val path = "worlds/$worldId/players"
        return db.collection("worlds")
            .document(worldId)
            .collection("players")
            .whereEqualTo("worldId", worldId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PlayerPresenceModel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun updatePlayerPresence(
        worldId: String,
        x: Int,
        y: Int,
        z: Int,
        gameMode: GameMode,
        health: Int,
        hunger: Int,
        isFlying: Boolean,
        heldItem: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val displayName = currentDisplayName()
        val path = "worlds/$worldId/players/$uid"
        val payload = mapOf(
            "userId" to uid,
            "worldId" to worldId,
            "displayName" to displayName,
            "x" to x.coerceIn(-30000, 30000).toLong(),
            "y" to y.coerceIn(0, 384).toLong(),
            "z" to z.coerceIn(-30000, 30000).toLong(),
            "gameMode" to gameMode.name,
            "health" to health.coerceIn(0, 20).toLong(),
            "hunger" to hunger.coerceIn(0, 20).toLong(),
            "isFlying" to isFlying,
            "heldItem" to heldItem.take(32).ifEmpty { BlockType.GRASS.name },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("worlds")
                .document(worldId)
                .collection("players")
                .document(uid)
                .set(payload, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    /**
     * Flushes all offline-queued worlds and block edits from Room to Firestore
     * when internet connectivity is restored.
     */
    suspend fun syncPendingOfflineEdits(): Int {
        val dao = localDao ?: return 0
        val uid = auth.currentUser?.uid ?: return 0
        var syncedCount = 0

        val pendingWorlds = dao.getPendingSyncWorlds()
        for (world in pendingWorlds) {
            if (world.ownerId == uid) {
                val payload = mapOf(
                    "worldId" to world.worldId,
                    "name" to world.name.take(60),
                    "seed" to world.seed,
                    "gameMode" to world.gameMode,
                    "srfMode" to world.srfMode,
                    "ownerId" to uid,
                    "ownerName" to world.ownerName.take(60),
                    "isPublic" to world.isPublic,
                    "blockEditCount" to world.blockEditCount.toLong(),
                    "parasiteEvolutionPhase" to world.parasiteEvolutionPhase.coerceIn(0, 5).toLong(),
                    "raidStackedWaves" to world.raidStackedWaves.coerceIn(0, 100).toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    db.collection("worlds").document(world.worldId).set(payload).await()
                    dao.markWorldSynced(world.worldId)
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.WRITE, "worlds/${world.worldId}")
                }
            }
        }

        val pendingBlocks = dao.getPendingSyncBlocks()
        for (block in pendingBlocks) {
            val payload = mapOf(
                "blockKey" to block.blockKey,
                "worldId" to block.worldId,
                "x" to block.x.toLong(),
                "y" to block.y.toLong(),
                "z" to block.z.toLong(),
                "blockType" to block.blockType,
                "updatedBy" to uid,
                "updatedByName" to block.updatedByName.take(60).ifEmpty { currentDisplayName() },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                db.collection("worlds")
                    .document(block.worldId)
                    .collection("blocks")
                    .document(block.blockKey)
                    .set(payload)
                    .await()
                dao.markBlockSynced(block.worldId, block.blockKey)
                syncedCount++
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "worlds/${block.worldId}/blocks/${block.blockKey}")
            }
        }
        return syncedCount
    }
}
