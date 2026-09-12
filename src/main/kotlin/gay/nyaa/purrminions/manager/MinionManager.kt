package gay.nyaa.purrminions.manager

import gay.nyaa.purrminions.db.MinionRepository
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionInstance
import gay.nyaa.purrminions.domain.MinionLocation
import gay.nyaa.purrminions.registry.MinionRegistry
import org.bukkit.Location
import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class MinionManager(
    private val registry: MinionRegistry,
    private val repository: MinionRepository,
) {
    private val activeMinions = ConcurrentHashMap<String, MinionInstance>()

    // Limits
    private val maxMinionsPerPlayer = 20
    private val maxMinionsPerChunk = 4

    fun loadAllMinions() {
        val minions = repository.loadAll()
        minions.forEach { minion ->
            val key = locationKey(minion.location)
            activeMinions[key] = minion
        }
    }

    fun placeMinion(
        player: Player,
        location: Location,
        minionId: MinionId,
        tier: Int,
    ): PlacementResult {
        // Check player limit
        val playerMinions = getPlayerMinions(player.uniqueId)
        if (playerMinions.size >= maxMinionsPerPlayer) {
            return PlacementResult.PlayerLimitReached
        }

        // Check chunk limit
        val chunkMinions = getChunkMinions(location)
        if (chunkMinions.size >= maxMinionsPerChunk) {
            return PlacementResult.ChunkLimitReached
        }

        // Check if location occupied
        val minionLocation = MinionLocation.from(location)
        if (activeMinions.containsKey(locationKey(minionLocation))) {
            return PlacementResult.LocationOccupied
        }

        // Create minion
        val now = Instant.now()
        val minion =
            MinionInstance(
                owner = player.uniqueId,
                location = minionLocation,
                type = minionId,
                tier = tier,
                fuelTicks = 0,
                storage = emptyList(),
                placedAt = now,
                lastTick = now,
            )

        activeMinions[locationKey(minionLocation)] = minion
        repository.save(minion)

        return PlacementResult.Success(minion)
    }

    fun breakMinion(location: MinionLocation): MinionInstance? {
        val key = locationKey(location)
        val minion = activeMinions.remove(key) ?: return null
        repository.delete(location)
        return minion
    }

    fun updateMinion(minion: MinionInstance) {
        val key = locationKey(minion.location)
        activeMinions[key] = minion
        repository.save(minion)
    }

    fun getMinion(location: MinionLocation): MinionInstance? {
        return activeMinions[locationKey(location)]
    }

    fun getPlayerMinions(uuid: UUID): List<MinionInstance> {
        return activeMinions.values.filter { it.owner == uuid }
    }

    fun getAllMinions(): List<MinionInstance> {
        return activeMinions.values.toList()
    }

    private fun getChunkMinions(location: Location): List<MinionInstance> {
        val chunkX = location.blockX shr 4
        val chunkZ = location.blockZ shr 4
        return activeMinions.values.filter {
            it.location.world == location.world.name &&
                (it.location.x shr 4) == chunkX &&
                (it.location.z shr 4) == chunkZ
        }
    }

    private fun locationKey(location: MinionLocation): String =
        "${location.world}:${location.x}:${location.y}:${location.z}"

    sealed class PlacementResult {
        data class Success(val minion: MinionInstance) : PlacementResult()

        object PlayerLimitReached : PlacementResult()

        object ChunkLimitReached : PlacementResult()

        object LocationOccupied : PlacementResult()
    }
}
