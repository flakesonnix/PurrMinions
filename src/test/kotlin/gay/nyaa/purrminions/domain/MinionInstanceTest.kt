package gay.nyaa.purrminions.domain

import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MinionInstanceTest {
    private val testLocation = MinionLocation("world", 100, 64, 200)
    private val testMinionId = MinionId("test", "COBBLESTONE_MINION")
    private val testOwner = UUID.randomUUID()
    private val now = Instant.now()

    @Test
    fun `minion instance should be created`() {
        val minion = MinionInstance(
            owner = testOwner,
            location = testLocation,
            type = testMinionId,
            tier = 1,
            fuelTicks = 100,
            storage = emptyList(),
            placedAt = now,
            lastTick = now
        )

        assertEquals(testOwner, minion.owner)
        assertEquals(testLocation, minion.location)
        assertEquals(testMinionId, minion.type)
        assertEquals(1, minion.tier)
        assertEquals(100, minion.fuelTicks)
    }

    @Test
    fun `withFuel should update fuel ticks`() {
        val minion = createTestMinion(fuelTicks = 100)
        val updated = minion.withFuel(200)

        assertEquals(200, updated.fuelTicks)
        assertEquals(100, minion.fuelTicks) // Original unchanged
    }

    @Test
    fun `withStorage should update storage`() {
        val minion = createTestMinion()
        val mockItem = mockk<ItemStack>(relaxed = true)
        val items = listOf(mockItem)
        val updated = minion.withStorage(items)

        assertEquals(1, updated.storage.size)
        assertEquals(0, minion.storage.size) // Original unchanged
    }

    @Test
    fun `withTier should update tier`() {
        val minion = createTestMinion(tier = 1)
        val updated = minion.withTier(2)

        assertEquals(2, updated.tier)
        assertEquals(1, minion.tier)
    }

    @Test
    fun `withLastTick should update last tick`() {
        val minion = createTestMinion()
        val newTime = Instant.now().plusSeconds(60)
        val updated = minion.withLastTick(newTime)

        assertEquals(newTime, updated.lastTick)
    }

    @Test
    fun `addToStorage should add item`() {
        val minion = createTestMinion()
        val mockItem = mockk<ItemStack>(relaxed = true)
        val updated = minion.addToStorage(mockItem)

        assertEquals(1, updated.storage.size)
    }

    @Test
    fun `addToStorage should append to existing storage`() {
        val mockItem1 = mockk<ItemStack>(relaxed = true)
        val mockItem2 = mockk<ItemStack>(relaxed = true)
        val minion = createTestMinion(storage = listOf(mockItem1))
        val updated = minion.addToStorage(mockItem2)

        assertEquals(2, updated.storage.size)
    }

    @Test
    fun `consumeFuel should decrease fuel by 1`() {
        val minion = createTestMinion(fuelTicks = 10)
        val updated = minion.consumeFuel()

        assertEquals(9, updated.fuelTicks)
    }

    @Test
    fun `consumeFuel should not go below 0`() {
        val minion = createTestMinion(fuelTicks = 0)
        val updated = minion.consumeFuel()

        assertEquals(0, updated.fuelTicks)
    }

    @Test
    fun `isFueled returns true when fuel available`() {
        val minion = createTestMinion(fuelTicks = 1)
        assertTrue(minion.isFueled())
    }

    @Test
    fun `isFueled returns false when no fuel`() {
        val minion = createTestMinion(fuelTicks = 0)
        assertFalse(minion.isFueled())
    }

    @Test
    fun `MinionLocation from Location should extract coordinates`() {
        val mockWorld = mockk<World>()
        every { mockWorld.name } returns "world"

        val location = Location(mockWorld, 100.5, 64.7, 200.3)
        val minionLocation = MinionLocation.from(location)

        assertEquals("world", minionLocation.world)
        assertEquals(100, minionLocation.x)
        assertEquals(64, minionLocation.y)
        assertEquals(200, minionLocation.z)
    }

    @Test
    fun `MinionLocation toLocation should create Location`() {
        val mockServer = mockk<Server>()
        val mockWorld = mockk<World>()
        every { mockServer.getWorld("world") } returns mockWorld

        val minionLocation = MinionLocation("world", 100, 64, 200)
        val location = minionLocation.toLocation(mockServer)

        assertNotNull(location)
        assertEquals(100.0, location!!.x)
        assertEquals(64.0, location.y)
        assertEquals(200.0, location.z)
    }

    @Test
    fun `MinionLocation toLocation returns null for invalid world`() {
        val mockServer = mockk<Server>()
        every { mockServer.getWorld("invalid") } returns null

        val minionLocation = MinionLocation("invalid", 0, 0, 0)
        val location = minionLocation.toLocation(mockServer)

        assertNull(location)
    }

    private fun createTestMinion(
        owner: UUID = testOwner,
        location: MinionLocation = testLocation,
        type: MinionId = testMinionId,
        tier: Int = 1,
        fuelTicks: Int = 100,
        storage: List<ItemStack> = emptyList(),
        placedAt: Instant = now,
        lastTick: Instant = now
    ) = MinionInstance(
        owner = owner,
        location = location,
        type = type,
        tier = tier,
        fuelTicks = fuelTicks,
        storage = storage,
        placedAt = placedAt,
        lastTick = lastTick
    )
}
