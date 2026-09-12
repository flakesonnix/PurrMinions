package gay.nyaa.purrminions.registry

import gay.nyaa.purrminions.domain.MinionAction
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionTier
import gay.nyaa.purrminions.domain.MinionType
import org.bukkit.Material
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MinionRegistryTest {
    private lateinit var registry: MinionRegistry

    @BeforeEach
    fun setup() {
        registry = MinionRegistry()
    }

    @Test
    fun `register should add minion type`() {
        val minion = createTestMinion("COBBLESTONE_MINION")
        registry.register(minion)

        assertEquals(1, registry.size())
        assertTrue(registry.contains(minion.id))
    }

    @Test
    fun `register duplicate should throw`() {
        val minion = createTestMinion("COBBLESTONE_MINION")
        registry.register(minion)

        assertThrows<IllegalArgumentException> {
            registry.register(minion)
        }
    }

    @Test
    fun `get should return registered minion`() {
        val minion = createTestMinion("COBBLESTONE_MINION")
        registry.register(minion)

        val retrieved = registry.get(minion.id)
        assertNotNull(retrieved)
        assertEquals(minion.id, retrieved!!.id)
    }

    @Test
    fun `get should return null for unregistered minion`() {
        val id = MinionId("test", "NONEXISTENT")
        assertNull(registry.get(id))
    }

    @Test
    fun `all should return all registered minions`() {
        val minion1 = createTestMinion("COBBLESTONE_MINION")
        val minion2 = createTestMinion("WHEAT_MINION")

        registry.register(minion1)
        registry.register(minion2)

        val all = registry.all()
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == minion1.id })
        assertTrue(all.any { it.id == minion2.id })
    }

    @Test
    fun `all should return empty list when no minions registered`() {
        assertEquals(0, registry.all().size)
    }

    @Test
    fun `contains should return true for registered minion`() {
        val minion = createTestMinion("COBBLESTONE_MINION")
        registry.register(minion)

        assertTrue(registry.contains(minion.id))
    }

    @Test
    fun `contains should return false for unregistered minion`() {
        val id = MinionId("test", "NONEXISTENT")
        assertFalse(registry.contains(id))
    }

    @Test
    fun `size should return correct count`() {
        assertEquals(0, registry.size())

        registry.register(createTestMinion("COBBLESTONE_MINION"))
        assertEquals(1, registry.size())

        registry.register(createTestMinion("WHEAT_MINION"))
        assertEquals(2, registry.size())
    }

    @Test
    fun `registry should be thread-safe`() {
        val minions = (1..10).map { createTestMinion("MINION_$it") }

        // Register minions concurrently
        minions.parallelStream().forEach { registry.register(it) }

        assertEquals(10, registry.size())
    }

    private fun createTestMinion(key: String) = MinionType(
        id = MinionId("test", key),
        action = MinionAction.MINE,
        displayName = "Test Minion",
        output = Material.COBBLESTONE,
        tiers = listOf(MinionTier(1, 20, 9))
    )
}
