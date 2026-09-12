package gay.nyaa.purrminions.ticker

import gay.nyaa.purrminions.domain.MinionInstance
import gay.nyaa.purrminions.domain.MinionType
import gay.nyaa.purrminions.manager.MinionManager
import gay.nyaa.purrminions.registry.MinionRegistry
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class MinionTicker(
    private val plugin: Plugin,
    private val registry: MinionRegistry,
    private val manager: MinionManager,
    private val logger: Logger,
) {
    private val activeMinionLocations = ConcurrentHashMap.newKeySet<String>()
    private var taskId: Int = -1

    fun start() {
        taskId =
            plugin.server.scheduler.runTaskTimerAsynchronously(
                plugin,
                Runnable { tick() },
                20L, // 1 second delay
                20L, // 1 second interval
            ).taskId
        logger.info("MinionTicker started")
    }

    fun stop() {
        if (taskId != -1) {
            plugin.server.scheduler.cancelTask(taskId)
            taskId = -1
        }
        logger.info("MinionTicker stopped")
    }

    private fun tick() {
        val minions = manager.getAllMinions()
        minions.forEach { minion ->
            try {
                tickMinion(minion)
            } catch (e: Exception) {
                logger.warning("Error ticking minion at ${minion.location}: ${e.message}")
            }
        }
    }

    private fun tickMinion(minion: MinionInstance) {
        val minionType = registry.get(minion.type) ?: return
        val tier = minionType.getTier(minion.tier) ?: return

        // Check if enough time has passed
        val now = Instant.now()
        val timeSinceLastTick = now.toEpochMilli() - minion.lastTick.toEpochMilli()
        if (timeSinceLastTick < tier.tickInterval * 1000) {
            return
        }

        // Check fuel
        if (!minion.isFueled()) {
            return
        }

        // Generate resource
        val output = ItemStack(minionType.output, 1)
        val updatedMinion =
            minion
                .addToStorage(output)
                .consumeFuel()
                .withLastTick(now)

        manager.updateMinion(updatedMinion)
    }
}
