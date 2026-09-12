package gay.nyaa.purrminions.listener

import com.purrcore.i18n.I18n
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionLocation
import gay.nyaa.purrminions.manager.MinionManager
import gay.nyaa.purrminions.registry.MinionRegistry
import gay.nyaa.purritems.api.PurrItemsAPI
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent

class MinionListener(
    private val manager: MinionManager,
    private val registry: MinionRegistry,
    private val itemsAPI: PurrItemsAPI,
    private val i18n: I18n,
) : Listener {
    @EventHandler
    fun onPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (!itemsAPI.isPurrItem(item)) return

        val itemId = itemsAPI.getItemId(item) ?: return

        // Check if it's a minion item (format: namespace:TYPE_MINION_T1)
        if (!itemId.key.endsWith("_MINION_T1")) return

        val clickedBlock = event.clickedBlock ?: return
        val placementLocation = clickedBlock.getRelative(event.blockFace).location

        // Extract minion ID and tier from item
        val minionIdStr = itemId.key.replace("_T1", "")
        val minionId = MinionId(itemId.namespace, minionIdStr)
        val tier = 1

        // Check if minion type exists
        if (!registry.contains(minionId)) {
            event.player.sendMessage(i18n.t("minion.invalid-type"))
            return
        }

        // Attempt placement
        when (val result = manager.placeMinion(event.player, placementLocation, minionId, tier)) {
            is MinionManager.PlacementResult.Success -> {
                event.isCancelled = true
                item.amount -= 1

                // Place visual block (player head or similar)
                placementLocation.block.type = Material.PLAYER_HEAD

                val typeName = registry.get(minionId)?.displayName ?: minionId.toString()
                event.player.sendMessage(i18n.t("minion.placed", "type" to typeName))
            }

            MinionManager.PlacementResult.PlayerLimitReached -> {
                event.player.sendMessage(i18n.t("minion.limit-player"))
            }

            MinionManager.PlacementResult.ChunkLimitReached -> {
                event.player.sendMessage(i18n.t("minion.limit-chunk"))
            }

            MinionManager.PlacementResult.LocationOccupied -> {
                event.player.sendMessage(i18n.t("minion.location-occupied"))
            }
        }
    }

    @EventHandler
    fun onBreak(event: BlockBreakEvent) {
        val location = MinionLocation.from(event.block.location)
        val minion = manager.breakMinion(location) ?: return

        event.isCancelled = true

        // Drop minion item
        val minionType = registry.get(minion.type) ?: return
        val itemId = "${minion.type}_T${minion.tier}"
        val minionItem = itemsAPI.createItem(itemId, 1)

        if (minionItem != null) {
            event.block.world.dropItemNaturally(event.block.location, minionItem)
        }

        // Drop storage contents
        minion.storage.forEach { item ->
            event.block.world.dropItemNaturally(event.block.location, item)
        }

        event.block.type = Material.AIR
        event.player.sendMessage(i18n.t("minion.broken"))
    }
}
