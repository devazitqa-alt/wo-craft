# Security Specification (`security_spec.md`)

## 1. Data Invariants

1. **Authentication Prerequisite**: All reads, queries, creates, updates, and deletes across `/worlds`, `/worlds/{worldId}/blocks`, and `/worlds/{worldId}/players` MUST require an authenticated user (`request.auth != null`).
2. **World Creation & Ownership**:
   - Any signed-in user may create a document in `/worlds/{worldId}` only if `incoming().ownerId == request.auth.uid`.
   - `ownerId`, `seed`, `createdAt`, and `isPublic` (only owner can toggle or keep immutable) are strictly typed and size-bounded.
   - `list` queries on `/worlds` MUST enforce `resource.data.isPublic == true || resource.data.ownerId == request.auth.uid` so clients must filter by `whereEqualTo("isPublic", true)` or `whereEqualTo("ownerId", uid)`.
3. **Block Edits (`/worlds/{worldId}/blocks/{blockKey}`)**:
   - Block coordinate keys must match `isValidId(blockKey)`.
   - Every block edit document must contain `worldId`, `x`, `y`, `z`, `blockType`, `updatedBy`, and `updatedAt`.
   - `incoming().updatedBy == request.auth.uid` and `incoming().worldId == worldId`.
   - `blockType` must be a valid string bounded in length (`<= 32`).
4. **Multiplayer Presence & SRF State (`/worlds/{worldId}/players/{playerId}`)**:
   - A player can only create, update, or delete their own presence document (`request.auth.uid == playerId` and `incoming().userId == request.auth.uid`).
   - Presence fields (`displayName`, `x`, `y`, `z`, `gameMode`, `health`, `hunger`, `isFlying`, `activeSrfMode`, `srfRaidWave`, `updatedAt`) are strictly validated for type and bounds.

## 2. The "Dirty Dozen" Payloads

1. **Unauthenticated World List**: Unauthenticated client queries `/worlds`. -> **Rejected**.
2. **Unfiltered World List**: Authenticated client queries `/worlds` without `.where("isPublic", "==", true)` or `.where("ownerId", "==", uid)`. -> **Rejected** (`rules are not filters`).
3. **Spoofed World Owner**: User Alice creates a world with `ownerId: "bob_456"`. -> **Rejected** (`incoming().ownerId == request.auth.uid`).
4. **Ghost Field Injection (Shadow Update)**: User Alice updates a world or block with an unallowed field `isAdmin: true`. -> **Rejected** (`keys().hasOnly(...)` / `affectedKeys().hasOnly(...)`).
5. **Cross-User World Deletion**: User Bob attempts to delete Alice's world (`ownerId == "alice_123"`). -> **Rejected** (`existing().ownerId == request.auth.uid`).
6. **Immutable Field Mutation**: User Alice attempts to change `ownerId`, `seed`, or `createdAt` on an existing world. -> **Rejected** (`incoming().ownerId == existing().ownerId`, etc.).
7. **Value Poisoning (Oversized String)**: User Alice writes a 5,000-character `name` to `/worlds/{worldId}`. -> **Rejected** (`data.name.size() <= 60`).
8. **Spoofed Block Author**: User Alice writes a block edit in `/worlds/{worldId}/blocks/{blockKey}` with `updatedBy: "bob_456"`. -> **Rejected** (`incoming().updatedBy == request.auth.uid`).
9. **Mismatched World ID in Block Subcollection**: User writes a block under `/worlds/world_1/blocks/0_64_0` with `worldId: "world_2"`. -> **Rejected** (`incoming().worldId == worldId`).
10. **Cross-Player Presence Hijack**: User Bob attempts to update `/worlds/{worldId}/players/alice_123`. -> **Rejected** (`request.auth.uid == playerId`).
11. **Out-of-Bounds Health/Hunger Poisoning**: User updates player presence with `health: 99999` or `health: -5`. -> **Rejected** (`data.health >= 0 && data.health <= 20`).
12. **Future Timestamp Injection**: Client submits a forged `updatedAt` timestamp far in the future (`> request.time`). -> **Rejected** (`incoming().updatedAt <= request.time`).
