package gay.nyaa.purrminions.domain

import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.time.Instant
import java.util.UUID

data class MinionLocation(
    val world: String,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun toLocation(server: org.bukkit.Server): Location? {
        val world = server.getWorld(world) ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    companion object {
        fun from(loc: Location): MinionLocation =
            MinionLocation(
                world = loc.world.name,
                x = loc.blockX,
                y = loc.blockY,
                z = loc.blockZ,
            )
    }
}

data class MinionInstance(
    val owner: UUID,
    val location: MinionLocation,
    val type: MinionId,
    val tier: Int,
    val fuelTicks: Int,
    val storage: List<ItemStack>,
    val placedAt: Instant,
    val lastTick: Instant,
) {
    fun withFuel(newFuelTicks: Int): MinionInstance = copy(fuelTicks = newFuelTicks)

    fun withStorage(newStorage: List<ItemStack>): MinionInstance = copy(storage = newStorage)

    fun withTier(newTier: Int): MinionInstance = copy(tier = newTier)

    fun withLastTick(time: Instant): MinionInstance = copy(lastTick = time)

    fun addToStorage(item: ItemStack): MinionInstance {
        val mutableStorage = storage.toMutableList()
        mutableStorage.add(item)
        return copy(storage = mutableStorage)
    }

    fun consumeFuel(): MinionInstance = copy(fuelTicks = maxOf(0, fuelTicks - 1))

    fun isFueled(): Boolean = fuelTicks > 0
}
