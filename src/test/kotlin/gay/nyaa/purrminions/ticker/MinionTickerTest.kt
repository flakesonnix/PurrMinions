package gay.nyaa.purrminions.ticker

import gay.nyaa.purrminions.domain.MinionAction
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionInstance
import gay.nyaa.purrminions.domain.MinionLocation
import gay.nyaa.purrminions.domain.MinionTier
import gay.nyaa.purrminions.domain.MinionType
import gay.nyaa.purrminions.manager.MinionManager
import gay.nyaa.purrminions.registry.MinionRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitScheduler
import org.bukkit.scheduler.BukkitTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals

class MinionTickerTest {
    private lateinit var mockPlugin: Plugin
    private lateinit var mockRegistry: MinionRegistry
    private lateinit var mockManager: MinionManager
    private lateinit var mockLogger: Logger
    private lateinit var mockServer: Server
    private lateinit var mockScheduler: BukkitScheduler
    private lateinit var ticker: MinionTicker

    private val testMinionId = MinionId("test", "COBBLESTONE_MINION")
    private lateinit var testMinionType: MinionType

    @BeforeEach
    fun setup() {
        mockPlugin = mockk(relaxed = true)
        mockRegistry = mockk(relaxed = true)
        mockManager = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockServer = mockk(relaxed = true)
        mockScheduler = mockk(relaxed = true)

        every { mockPlugin.server } returns mockServer
        every { mockServer.scheduler } returns mockScheduler

        // Mock Material to avoid Bukkit initialization
        testMinionType = MinionType(
            id = testMinionId,
            action = MinionAction.MINE,
            displayName = "Test Minion",
            output = mockk(relaxed = true),
            tiers = listOf(
                MinionTier(1, 20, 9),
                MinionTier(2, 15, 15)
            )
        )

        ticker = MinionTicker(mockPlugin, mockRegistry, mockManager, mockLogger)
    }

    @Test
    fun `start should schedule task`() {
        val mockTask = mockk<BukkitTask>(relaxed = true)
        every { mockTask.taskId } returns 123
        every { mockScheduler.runTaskTimerAsynchronously(any(), any<Runnable>(), any(), any()) } returns mockTask

        ticker.start()

        verify { mockScheduler.runTaskTimerAsynchronously(mockPlugin, any<Runnable>(), 20L, 20L) }
    }

    @Test
    fun `stop should cancel task`() {
        val mockTask = mockk<BukkitTask>(relaxed = true)
        every { mockTask.taskId } returns 123
        every { mockScheduler.runTaskTimerAsynchronously(any(), any<Runnable>(), any(), any()) } returns mockTask

        ticker.start()
        ticker.stop()

        verify { mockScheduler.cancelTask(123) }
    }

    @Test
    fun `tick should process fueled minion after interval`() {
        val now = Instant.now()
        val lastTick = now.minus(21, ChronoUnit.SECONDS) // 21 seconds ago, past 20s interval

        val minion = MinionInstance(
            owner = UUID.randomUUID(),
            location = MinionLocation("world", 100, 64, 200),
            type = testMinionId,
            tier = 1,
            fuelTicks = 10,
            storage = emptyList(),
            placedAt = now,
            lastTick = lastTick
        )

        every { mockManager.getAllMinions() } returns listOf(minion)
        every { mockRegistry.get(testMinionId) } returns testMinionType

        // Simulate tick by calling the runnable
        val capturedRunnable = captureRunnable()
        capturedRunnable.run()

        verify { mockManager.updateMinion(match<MinionInstance> { 
            it.fuelTicks == 9 && // Fuel consumed
            it.storage.size == 1 // Item added
        }) }
    }

    @Test
    fun `tick should not process minion without fuel`() {
        val minion = createTestMinion(fuelTicks = 0)

        every { mockManager.getAllMinions() } returns listOf(minion)
        every { mockRegistry.get(testMinionId) } returns testMinionType

        val capturedRunnable = captureRunnable()
        capturedRunnable.run()

        verify(exactly = 0) { mockManager.updateMinion(any()) }
    }

    @Test
    fun `tick should not process minion before interval`() {
        val now = Instant.now()
        val lastTick = now.minus(5, ChronoUnit.SECONDS) // Only 5 seconds ago

        val minion = MinionInstance(
            owner = UUID.randomUUID(),
            location = MinionLocation("world", 100, 64, 200),
            type = testMinionId,
            tier = 1,
            fuelTicks = 10,
            storage = emptyList(),
            placedAt = now,
            lastTick = lastTick
        )

        every { mockManager.getAllMinions() } returns listOf(minion)
        every { mockRegistry.get(testMinionId) } returns testMinionType

        val capturedRunnable = captureRunnable()
        capturedRunnable.run()

        verify(exactly = 0) { mockManager.updateMinion(any()) }
    }

    @Test
    fun `tick should handle unregistered minion type`() {
        val minion = createTestMinion()

        every { mockManager.getAllMinions() } returns listOf(minion)
        every { mockRegistry.get(testMinionId) } returns null

        val capturedRunnable = captureRunnable()
        capturedRunnable.run()

        verify(exactly = 0) { mockManager.updateMinion(any()) }
    }

    @Test
    fun `tick should handle invalid tier`() {
        val minion = createTestMinion(tier = 99) // Invalid tier

        every { mockManager.getAllMinions() } returns listOf(minion)
        every { mockRegistry.get(testMinionId) } returns testMinionType

        val capturedRunnable = captureRunnable()
        capturedRunnable.run()

        verify(exactly = 0) { mockManager.updateMinion(any()) }
    }

    @Test
    fun `tick should process multiple minions`() {
        val now = Instant.now()
        val lastTick = now.minus(21, ChronoUnit.SECONDS)

        val minion1 = createTestMinion(location = MinionLocation("world", 0, 64, 0), lastTick = lastTick)
        val minion2 = createTestMinion(location = MinionLocation("world", 10, 64, 10), lastTick = lastTick)

        every { mockManager.getAllMinions() } returns listOf(minion1, minion2)
        every { mockRegistry.get(testMinionId) } returns testMinionType

        val capturedRunnable = captureRunnable()
        capturedRunnable.run()

        verify(exactly = 2) { mockManager.updateMinion(any()) }
    }

    @Test
    fun `tick should handle exceptions gracefully`() {
        val minion = createTestMinion(lastTick = Instant.now().minus(21, ChronoUnit.SECONDS))

        every { mockManager.getAllMinions() } returns listOf(minion)
        every { mockRegistry.get(testMinionId) } throws RuntimeException("Test error")

        val capturedRunnable = captureRunnable()
        capturedRunnable.run() // Should not throw

        verify { mockLogger.warning(match<String> { it.contains("Error ticking minion") }) }
    }

    private fun captureRunnable(): Runnable {
        var captured: Runnable? = null
        val mockTask = mockk<BukkitTask>(relaxed = true)
        every { mockTask.taskId } returns 123
        every { mockScheduler.runTaskTimerAsynchronously(any(), any<Runnable>(), any(), any()) } answers {
            captured = arg(1)
            mockTask
        }
        ticker.start()
        return captured!!
    }

    private fun createTestMinion(
        owner: UUID = UUID.randomUUID(),
        location: MinionLocation = MinionLocation("world", 100, 64, 200),
        type: MinionId = testMinionId,
        tier: Int = 1,
        fuelTicks: Int = 10,
        lastTick: Instant = Instant.now()
    ) = MinionInstance(
        owner = owner,
        location = location,
        type = type,
        tier = tier,
        fuelTicks = fuelTicks,
        storage = emptyList(),
        placedAt = Instant.now(),
        lastTick = lastTick
    )
}
