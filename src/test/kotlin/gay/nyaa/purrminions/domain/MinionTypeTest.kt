package gay.nyaa.purrminions.domain

import org.bukkit.Material
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MinionTypeTest {
    private val testMinionId = MinionId("test", "COBBLESTONE_MINION")

    @Test
    fun `valid minion type should be created`() {
        val type = MinionType(
            id = testMinionId,
            action = MinionAction.MINE,
            displayName = "Cobblestone Minion",
            output = Material.COBBLESTONE,
            tiers = listOf(
                MinionTier(1, 20, 9),
                MinionTier(2, 15, 15)
            )
        )

        assertEquals(testMinionId, type.id)
        assertEquals(MinionAction.MINE, type.action)
        assertEquals("Cobblestone Minion", type.displayName)
        assertEquals(Material.COBBLESTONE, type.output)
        assertEquals(2, type.tiers.size)
    }

    @Test
    fun `blank display name should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionType(
                id = testMinionId,
                action = MinionAction.MINE,
                displayName = "",
                output = Material.COBBLESTONE,
                tiers = listOf(MinionTier(1, 20, 9))
            )
        }
    }

    @Test
    fun `empty tiers should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionType(
                id = testMinionId,
                action = MinionAction.MINE,
                displayName = "Test",
                output = Material.COBBLESTONE,
                tiers = emptyList()
            )
        }
    }

    @Test
    fun `unsorted tiers should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionType(
                id = testMinionId,
                action = MinionAction.MINE,
                displayName = "Test",
                output = Material.COBBLESTONE,
                tiers = listOf(
                    MinionTier(2, 15, 15),
                    MinionTier(1, 20, 9)
                )
            )
        }
    }

    @Test
    fun `getTier should return tier`() {
        val type = createTestMinionType()
        val tier = type.getTier(1)

        assertNotNull(tier)
        assertEquals(1, tier!!.tier)
        assertEquals(20, tier.tickInterval)
    }

    @Test
    fun `getTier should return null for invalid tier`() {
        val type = createTestMinionType()
        val tier = type.getTier(99)

        assertNull(tier)
    }

    @Test
    fun `maxTier should return highest tier`() {
        val type = createTestMinionType()
        assertEquals(3, type.maxTier())
    }

    @Test
    fun `MinionTier with valid values should be created`() {
        val tier = MinionTier(1, 20, 9)

        assertEquals(1, tier.tier)
        assertEquals(20, tier.tickInterval)
        assertEquals(9, tier.storageSlots)
    }

    @Test
    fun `MinionTier with zero tier should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionTier(0, 20, 9)
        }
    }

    @Test
    fun `MinionTier with negative tier should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionTier(-1, 20, 9)
        }
    }

    @Test
    fun `MinionTier with zero tick interval should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionTier(1, 0, 9)
        }
    }

    @Test
    fun `MinionTier with invalid storage slots should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionTier(1, 20, 0)
        }
        assertThrows<IllegalArgumentException> {
            MinionTier(1, 20, 28)
        }
    }

    @Test
    fun `MinionTier with 27 storage slots is valid`() {
        val tier = MinionTier(1, 20, 27)
        assertEquals(27, tier.storageSlots)
    }

    private fun createTestMinionType() = MinionType(
        id = testMinionId,
        action = MinionAction.MINE,
        displayName = "Test Minion",
        output = Material.COBBLESTONE,
        tiers = listOf(
            MinionTier(1, 20, 9),
            MinionTier(2, 15, 15),
            MinionTier(3, 10, 21)
        )
    )
}
