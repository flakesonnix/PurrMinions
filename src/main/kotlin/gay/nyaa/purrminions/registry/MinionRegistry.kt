package gay.nyaa.purrminions.registry

import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionType
import java.util.concurrent.ConcurrentHashMap

class MinionRegistry {
    private val minions = ConcurrentHashMap<MinionId, MinionType>()

    fun register(minion: MinionType) {
        val existing = minions.putIfAbsent(minion.id, minion)
        require(existing == null) { "Minion ${minion.id} already registered" }
    }

    fun get(id: MinionId): MinionType? = minions[id]

    fun all(): List<MinionType> = minions.values.toList()

    fun contains(id: MinionId): Boolean = minions.containsKey(id)

    fun size(): Int = minions.size
}
