package gay.nyaa.purrminions.minions

import gay.nyaa.purrminions.domain.MinionAction
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionTier
import gay.nyaa.purrminions.domain.MinionType
import org.bukkit.Material

object Minions {
    fun cobblestoneMinion() =
        MinionType(
            id = MinionId("purr_minions", "COBBLESTONE_MINION"),
            action = MinionAction.MINE,
            displayName = "Cobblestone Minion",
            output = Material.COBBLESTONE,
            tiers =
                listOf(
                    MinionTier(tier = 1, tickInterval = 20, storageSlots = 9),
                    MinionTier(tier = 2, tickInterval = 18, storageSlots = 15),
                    MinionTier(tier = 3, tickInterval = 16, storageSlots = 21),
                    MinionTier(tier = 4, tickInterval = 14, storageSlots = 27),
                ),
        )

    fun wheatMinion() =
        MinionType(
            id = MinionId("purr_minions", "WHEAT_MINION"),
            action = MinionAction.FARM,
            displayName = "Wheat Minion",
            output = Material.WHEAT,
            tiers =
                listOf(
                    MinionTier(tier = 1, tickInterval = 25, storageSlots = 9),
                    MinionTier(tier = 2, tickInterval = 22, storageSlots = 15),
                    MinionTier(tier = 3, tickInterval = 19, storageSlots = 21),
                ),
        )

    fun oakMinion() =
        MinionType(
            id = MinionId("purr_minions", "OAK_MINION"),
            action = MinionAction.CHOP,
            displayName = "Oak Minion",
            output = Material.OAK_LOG,
            tiers =
                listOf(
                    MinionTier(tier = 1, tickInterval = 30, storageSlots = 9),
                    MinionTier(tier = 2, tickInterval = 26, storageSlots = 15),
                ),
        )

    fun all(): List<MinionType> = listOf(cobblestoneMinion(), wheatMinion(), oakMinion())
}
