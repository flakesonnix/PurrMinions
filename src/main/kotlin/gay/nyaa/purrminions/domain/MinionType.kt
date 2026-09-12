package gay.nyaa.purrminions.domain

import org.bukkit.Material

data class MinionTier(
    val tier: Int,
    val tickInterval: Int,
    val storageSlots: Int,
) {
    init {
        require(tier > 0) { "Tier must be positive" }
        require(tickInterval > 0) { "Tick interval must be positive" }
        require(storageSlots in 1..27) { "Storage slots must be 1-27" }
    }
}

data class MinionType(
    val id: MinionId,
    val action: MinionAction,
    val displayName: String,
    val output: Material,
    val tiers: List<MinionTier>,
) {
    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
        require(tiers.isNotEmpty()) { "Must have at least one tier" }
        require(tiers.sortedBy { it.tier } == tiers) { "Tiers must be sorted" }
    }

    fun getTier(tierNumber: Int): MinionTier? = tiers.find { it.tier == tierNumber }

    fun maxTier(): Int = tiers.maxOf { it.tier }
}
