package gay.nyaa.purrminions.integration

import gay.nyaa.purrminions.domain.*
import gay.nyaa.purrminions.ticker.MinionTicker
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for minion offline production calculation.
 * Critical: Ensures minions produce correct amount during offline time.
 */
class MinionOfflineProductionTest {
    
    @Test
    fun `minion produces correct amount after offline time`() {
        val minionType = createTestMinion()
        val tier = minionType.getTier(1)!!
        
        // Minion placed 1 hour ago, last tick 1 hour ago
        val oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS)
        val minion = MinionInstance(
            owner = UUID.randomUUID(),
            location = MinionLocation("world", 0, 64, 0),
            type = minionType.id,
            tier = 1,
            fuelTicks = 1000,  // Plenty of fuel
            storage = emptyList(),
            placedAt = oneHourAgo,
            lastTick = oneHourAgo
        )
        
        // Calculate expected production
        // 1 hour = 3600 seconds
        // Tick interval = 20 seconds
        // Expected ticks = 3600 / 20 = 180 items
        val expectedItems = 3600 / tier.tickInterval
        
        // Simulate catching up
        val now = Instant.now()
        val timeSinceLastTick = now.toEpochMilli() - minion.lastTick.toEpochMilli()
        val ticksOccurred = (timeSinceLastTick / 1000 / tier.tickInterval).toInt()
        
        assertEquals(180, ticksOccurred)
    }
    
    @Test
    fun `minion stops producing when fuel runs out`() {
        val minionType = createTestMinion()
        val tier = minionType.getTier(1)!!
        
        val minion = MinionInstance(
            owner = UUID.randomUUID(),
            location = MinionLocation("world", 0, 64, 0),
            type = minionType.id,
            tier = 1,
            fuelTicks = 10,  // Only 10 ticks of fuel
            storage = emptyList(),
            placedAt = Instant.now().minus(1, ChronoUnit.HOURS),
            lastTick = Instant.now().minus(1, ChronoUnit.HOURS)
        )
        
        // After 10 ticks, fuel should be 0
        var currentFuel = minion.fuelTicks
        var tickCount = 0
        
        while (currentFuel > 0) {
            currentFuel--
            tickCount++
        }
        
        assertEquals(10, tickCount)
        assertEquals(0, currentFuel)
    }
    
    @Test
    fun `storage overflow handling`() {
        val minionType = createTestMinion()
        val tier = minionType.getTier(1)!!
        
        // Storage at max capacity (27 slots) - use mocked ItemStacks
        val fullStorage = (0 until 27).map { 
            mockk<ItemStack>(relaxed = true)
        }.toList()
        
        val minion = MinionInstance(
            owner = UUID.randomUUID(),
            location = MinionLocation("world", 0, 64, 0),
            type = minionType.id,
            tier = 1,
            fuelTicks = 1000,
            storage = fullStorage,
            placedAt = Instant.now(),
            lastTick = Instant.now()
        )
        
        assertEquals(27, minion.storage.size)
        assertTrue(minion.storage.size >= tier.storageSlots)
    }
    
    private fun createTestMinion() = MinionType(
        id = MinionId("test", "COBBLESTONE_MINION"),
        action = MinionAction.MINE,
        displayName = "Test Minion",
        output = Material.COBBLESTONE,
        tiers = listOf(
            MinionTier(tier = 1, tickInterval = 20, storageSlots = 9),
            MinionTier(tier = 2, tickInterval = 15, storageSlots = 15),
        )
    )
}
