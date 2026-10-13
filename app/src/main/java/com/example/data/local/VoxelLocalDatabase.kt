package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_worlds")
data class LocalWorldEntity(
    @PrimaryKey val worldId: String,
    val name: String,
    val seed: Long,
    val gameMode: String,
    val srfMode: String,
    val difficulty: String = "MEDIUM",
    val ownerId: String,
    val ownerName: String,
    val isPublic: Boolean,
    val blockEditCount: Int,
    val parasiteEvolutionPhase: Int,
    val raidStackedWaves: Int,
    val pendingSync: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_blocks", primaryKeys = ["worldId", "blockKey"])
data class LocalBlockEntity(
    val worldId: String,
    val blockKey: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val blockType: String,
    val updatedBy: String,
    val updatedByName: String,
    val pendingSync: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface VoxelWorldDao {
    @Query("SELECT * FROM local_worlds ORDER BY updatedAtMillis DESC")
    fun observeAllWorlds(): Flow<List<LocalWorldEntity>>

    @Query("SELECT * FROM local_worlds WHERE worldId = :worldId LIMIT 1")
    suspend fun getWorldById(worldId: String): LocalWorldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorld(world: LocalWorldEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorlds(worlds: List<LocalWorldEntity>)

    @Query("DELETE FROM local_worlds WHERE worldId = :worldId")
    suspend fun deleteWorld(worldId: String)

    @Query("SELECT * FROM local_blocks WHERE worldId = :worldId")
    fun observeBlocksForWorld(worldId: String): Flow<List<LocalBlockEntity>>

    @Query("SELECT * FROM local_blocks WHERE worldId = :worldId")
    suspend fun getBlocksForWorld(worldId: String): List<LocalBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlock(block: LocalBlockEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlocks(blocks: List<LocalBlockEntity>)

    @Query("SELECT * FROM local_blocks WHERE pendingSync = 1")
    suspend fun getPendingSyncBlocks(): List<LocalBlockEntity>

    @Query("SELECT COUNT(*) FROM local_blocks WHERE pendingSync = 1")
    fun observePendingSyncCount(): Flow<Int>

    @Query("UPDATE local_blocks SET pendingSync = 0 WHERE worldId = :worldId AND blockKey = :blockKey")
    suspend fun markBlockSynced(worldId: String, blockKey: String)

    @Query("SELECT * FROM local_worlds WHERE pendingSync = 1")
    suspend fun getPendingSyncWorlds(): List<LocalWorldEntity>

    @Query("UPDATE local_worlds SET pendingSync = 0 WHERE worldId = :worldId")
    suspend fun markWorldSynced(worldId: String)
}

@Database(
    entities = [LocalWorldEntity::class, LocalBlockEntity::class],
    version = 2,
    exportSchema = false
)
abstract class VoxelLocalDatabase : RoomDatabase() {
    abstract fun voxelWorldDao(): VoxelWorldDao

    companion object {
        @Volatile
        private var INSTANCE: VoxelLocalDatabase? = null

        fun getInstance(context: Context): VoxelLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VoxelLocalDatabase::class.java,
                    "voxelcraft_offline.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
