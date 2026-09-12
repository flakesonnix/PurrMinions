package gay.nyaa.purrminions

import com.purrcore.PurrCorePlugin
import gay.nyaa.purrminions.command.MinionCommand
import gay.nyaa.purrminions.db.MinionRepository
import gay.nyaa.purrminions.listener.MinionListener
import gay.nyaa.purrminions.manager.MinionManager
import gay.nyaa.purrminions.minions.Minions
import gay.nyaa.purrminions.registry.MinionRegistry
import gay.nyaa.purrminions.ticker.MinionTicker
import gay.nyaa.purritems.PurrItemsPlugin
import org.bukkit.plugin.java.JavaPlugin

class PurrMinionsPlugin : JavaPlugin() {
    private lateinit var minionManager: MinionManager
    private lateinit var minionTicker: MinionTicker

    override fun onEnable() {
        val purrCore = server.pluginManager.getPlugin("PurrCore") as? PurrCorePlugin
        if (purrCore == null) {
            logger.severe("PurrCore not found! Disabling...")
            server.pluginManager.disablePlugin(this)
            return
        }

        val purrItems = server.pluginManager.getPlugin("PurrItems") as? PurrItemsPlugin
        if (purrItems == null) {
            logger.severe("PurrItems not found! Disabling...")
            server.pluginManager.disablePlugin(this)
            return
        }

        val database = purrCore.database
        val i18n = purrCore.i18n
        val itemsAPI = purrItems.api

        val repository = MinionRepository(database, logger)
        repository.migrate()

        val registry = MinionRegistry()
        Minions.all().forEach { registry.register(it) }
        logger.info("Registered ${registry.size()} minion types")

        minionManager = MinionManager(registry, repository)
        minionManager.loadAllMinions()
        logger.info("Loaded ${minionManager.getAllMinions().size} active minions")

        minionTicker = MinionTicker(this, registry, minionManager, logger)
        minionTicker.start()

        server.pluginManager.registerEvents(MinionListener(minionManager, registry, itemsAPI, i18n), this)

        getCommand("minion")?.setExecutor(MinionCommand(minionManager, registry, i18n))

        logger.info("PurrMinions enabled!")
    }

    override fun onDisable() {
        if (::minionTicker.isInitialized) {
            minionTicker.stop()
        }

        // Save all active minions
        if (::minionManager.isInitialized) {
            minionManager.getAllMinions().forEach { minion ->
                // Already persisted via manager updates
            }
        }

        logger.info("PurrMinions disabled!")
    }
}
