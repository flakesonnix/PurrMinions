package gay.nyaa.purrminions.command

import com.purrcore.i18n.I18n
import gay.nyaa.purrminions.manager.MinionManager
import gay.nyaa.purrminions.registry.MinionRegistry
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MinionCommand(
    private val manager: MinionManager,
    private val registry: MinionRegistry,
    private val i18n: I18n,
) : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(i18n.t("minion.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "list" -> listMinions(sender)
            "upgrade" -> sender.sendMessage(i18n.t("minion.upgrade-not-implemented"))
            else -> sender.sendMessage(i18n.t("minion.usage"))
        }

        return true
    }

    private fun listMinions(player: Player) {
        val minions = manager.getPlayerMinions(player.uniqueId)

        if (minions.isEmpty()) {
            player.sendMessage(i18n.t("minion.list-empty"))
            return
        }

        player.sendMessage(i18n.t("minion.list-header", "count" to minions.size.toString()))
        minions.forEach { minion ->
            val type = registry.get(minion.type)
            val typeName = type?.displayName ?: minion.type.toString()
            player.sendMessage(
                i18n.t(
                    "minion.list-entry",
                    "type" to typeName,
                    "tier" to minion.tier.toString(),
                    "location" to "${minion.location.x}, ${minion.location.y}, ${minion.location.z}",
                    "fuel" to minion.fuelTicks.toString(),
                ),
            )
        }
    }
}
