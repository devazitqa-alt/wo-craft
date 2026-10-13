package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BlockType
import com.example.data.model.GameMode
import com.example.data.model.SrfMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VoxelRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createWorld_and_writeBlockEdit_succeedsForAuthenticatedUser() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = VoxelRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWorld(
                name = "Test Seed World",
                seed = 987654321L,
                gameMode = GameMode.SURVIVAL,
                srfMode = SrfMode.RAID_FARM,
                isPublic = true
            )
        }
        assertTrue(createResult.isSuccess)
        val worldId = createResult.getOrThrow()
        assertTrue(worldId.isNotEmpty())

        val blockResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.writeBlockEdit(
                worldId = worldId,
                x = 2,
                y = 64,
                z = -3,
                blockType = BlockType.DIAMOND_ORE
            )
        }
        assertTrue(blockResult.isSuccess)

        val blocks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWorldBlocks(worldId).first { it.isNotEmpty() }
        }
        assertTrue(blocks.any { it.blockType == BlockType.DIAMOND_ORE.name })
    }

    @Test
    fun deleteWorld_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = VoxelRepository(firestore)
        val worldId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createWorld(
                name = "Alice Private Realm",
                seed = 12345L,
                gameMode = GameMode.CREATIVE,
                srfMode = SrfMode.NONE,
                isPublic = true
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = VoxelRepository(firestore)
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.deleteWorld(worldId)
        }
        assertTrue(deleteResult.isFailure)
        val exception = deleteResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observePublicWorlds_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = VoxelRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicWorlds().first()
            }
            fail("Expected FirebaseFirestoreException for unauthenticated caller")
        } catch (e: Throwable) {
            var current: Throwable? = e
            var firestoreEx: FirebaseFirestoreException? = null
            while (current != null) {
                if (current is FirebaseFirestoreException) {
                    firestoreEx = current
                    break
                }
                current = current.cause
            }
            assertNotNull("Expected FirebaseFirestoreException in causal chain, got $e", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@voxelcraft.test"
        const val BOB_EMAIL = "bob@voxelcraft.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
