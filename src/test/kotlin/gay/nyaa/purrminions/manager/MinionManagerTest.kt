package gay.nyaa.purrminions.manager

import gay.nyaa.purrminions.db.MinionRepository
import gay.nyaa.purrminions.domain.MinionAction
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionInstance
import gay.nyaa.purrminions.domain.MinionLocation
import gay.nyaa.purrminions.domain.MinionTier
import gay.nyaa.purrminions.domain.MinionType
import gay.nyaa.purrminions.registry.MinionRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MinionManagerTest {
    private lateinit var mockRegistry: MinionRegistry
    private lateinit var mockRepository: MinionRepository
    private lateinit var manager: MinionManager
    private lateinit var mockPlayer: Player
    private lateinit var mockWorld: World

    private val testMinionId = MinionId("test", "COBBLESTONE_MINION")
    private val testLocation = MinionLocation("world", 100, 64, 200)

    @BeforeEach
    fun setup() {
        mockRegistry = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        mockWorld = mockk(relaxed = true)

        manager = MinionManager(mockRegistry, mockRepository)

        every { mockPlayer.uniqueId } returns UUID.randomUUID()
        every { mockWorld.name } returns "world"
    }

    @Test
    fun `loadAllMinions should load from repository`() {
        val minion = createTestMinion()
        every { mockRepository.loadAll() } returns listOf(minion)

        manager.loadAllMinions()

        val retrieved = manager.getMinion(minion.location)
        assertNotNull(retrieved)
        assertEquals(minion.location, retrieved!!.location)
    }

    @Test
    fun `placeMinion should succeed with valid conditions`() {
        val location = Location(mockWorld, 100.0, 64.0, 200.0)
        every { mockRepository.save(any()) } returns Unit

        val result = manager.placeMinion(mockPlayer, location, testMinionId, 1)

        assertTrue(result is MinionManager.PlacementResult.Success)
        verify { mockRepository.save(any()) }
    }

    @Test
    fun `placeMinion should fail when player limit reached`() {
        val playerUuid = UUID.randomUUID()
        every { mockPlayer.uniqueId } returns playerUuid
        
        // Pre-fill with 20 minions owned by the same player
        val minions = (0..19).map { createTestMinion(owner = playerUuid, location = MinionLocation("world", it, 64, it)) }
        every { mockRepository.loadAll() } returns minions
        manager.loadAllMinions()

        val location = Location(mockWorld, 200.0, 64.0, 200.0)
        val result = manager.placeMinion(mockPlayer, location, testMinionId, 1)

        assertTrue(result is MinionManager.PlacementResult.PlayerLimitReached)
    }

    @Test
    fun `placeMinion should fail when chunk limit reached`() {
        // Place 4 minions in same chunk (0-15 blocks)
        val minions = (0..3).map { createTestMinion(location = MinionLocation("world", it, 64, it)) }
        every { mockRepository.loadAll() } returns minions
        manager.loadAllMinions()

        // Try to place 5th minion in same chunk
        val location = Location(mockWorld, 4.0, 64.0, 4.0)
        val result = manager.placeMinion(mockPlayer, location, testMinionId, 1)

        assertTrue(result is MinionManager.PlacementResult.ChunkLimitReached)
    }

    @Test
    fun `placeMinion should fail when location occupied`() {
        val minion = createTestMinion()
        every { mockRepository.loadAll() } returns listOf(minion)
        manager.loadAllMinions()

        val location = Location(mockWorld, 100.0, 64.0, 200.0)
        val result = manager.placeMinion(mockPlayer, location, testMinionId, 1)

        assertTrue(result is MinionManager.PlacementResult.LocationOccupied)
    }

    @Test
    fun `breakMinion should remove minion`() {
        val minion = createTestMinion()
        every { mockRepository.loadAll() } returns listOf(minion)
        manager.loadAllMinions()

        val broken = manager.breakMinion(minion.location)

        assertNotNull(broken)
        assertEquals(minion.location, broken!!.location)
        verify { mockRepository.delete(minion.location) }

        // Verify minion is gone
        assertNull(manager.getMinion(minion.location))
    }

    @Test
    fun `breakMinion should return null for non-existent minion`() {
        val result = manager.breakMinion(MinionLocation("world", 999, 64, 999))
        assertNull(result)
    }

    @Test
    fun `updateMinion should save to repository`() {
        val minion = createTestMinion()
        every { mockRepository.loadAll() } returns listOf(minion)
        manager.loadAllMinions()

        val updated = minion.withFuel(200)
        manager.updateMinion(updated)

        verify { mockRepository.save(updated) }

        val retrieved = manager.getMinion(minion.location)
        assertEquals(200, retrieved!!.fuelTicks)
    }

    @Test
    fun `getMinion should return minion at location`() {
        val minion = createTestMinion()
        every { mockRepository.loadAll() } returns listOf(minion)
        manager.loadAllMinions()

        val retrieved = manager.getMinion(minion.location)
        assertNotNull(retrieved)
        assertEquals(minion.location, retrieved!!.location)
    }

    @Test
    fun `getMinion should return null for empty location`() {
        val result = manager.getMinion(MinionLocation("world", 999, 64, 999))
        assertNull(result)
    }

    @Test
    fun `getPlayerMinions should return player's minions only`() {
        val player1Uuid = UUID.randomUUID()
        val player2Uuid = UUID.randomUUID()

        val minion1 = createTestMinion(owner = player1Uuid, location = MinionLocation("world", 0, 64, 0))
        val minion2 = createTestMinion(owner = player1Uuid, location = MinionLocation("world", 10, 64, 10))
        val minion3 = createTestMinion(owner = player2Uuid, location = MinionLocation("world", 20, 64, 20))

        every { mockRepository.loadAll() } returns listOf(minion1, minion2, minion3)
        manager.loadAllMinions()

        val player1Minions = manager.getPlayerMinions(player1Uuid)
        assertEquals(2, player1Minions.size)
        assertTrue(player1Minions.all { it.owner == player1Uuid })
    }

    @Test
    fun `getAllMinions should return all minions`() {
        val minion1 = createTestMinion(location = MinionLocation("world", 0, 64, 0))
        val minion2 = createTestMinion(location = MinionLocation("world", 10, 64, 10))

        every { mockRepository.loadAll() } returns listOf(minion1, minion2)
        manager.loadAllMinions()

        val all = manager.getAllMinions()
        assertEquals(2, all.size)
    }

    @Test
    fun `chunk limit should consider chunk boundaries correctly`() {
        // Place 4 minions in chunk (0,0) - blocks 0-15, 0-15
        val minions = listOf(
            createTestMinion(location = MinionLocation("world", 0, 64, 0)),
            createTestMinion(location = MinionLocation("world", 5, 64, 5)),
            createTestMinion(location = MinionLocation("world", 10, 64, 10)),
            createTestMinion(location = MinionLocation("world", 15, 64, 15))
        )
        every { mockRepository.loadAll() } returns minions
        manager.loadAllMinions()

        // Try to place in different chunk (16,16) - should succeed
        val location = Location(mockWorld, 16.0, 64.0, 16.0)
        val result = manager.placeMinion(mockPlayer, location, testMinionId, 1)

        assertTrue(result is MinionManager.PlacementResult.Success)
    }

    private fun createTestMinion(
        owner: UUID = UUID.randomUUID(),
        location: MinionLocation = testLocation,
        type: MinionId = testMinionId,
        tier: Int = 1,
        fuelTicks: Int = 100
    ) = MinionInstance(
        owner = owner,
        location = location,
        type = type,
        tier = tier,
        fuelTicks = fuelTicks,
        storage = emptyList(),
        placedAt = Instant.now(),
        lastTick = Instant.now()
    )

    private fun createTestMinionType() = MinionType(
        id = testMinionId,
        action = MinionAction.MINE,
        displayName = "Test Minion",
        output = Material.COBBLESTONE,
        tiers = listOf(MinionTier(1, 20, 9))
    )
}
