package gay.nyaa.purrminions.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MinionIdTest {
    @Test
    fun `valid minion id should be created`() {
        val id = MinionId("purrminions", "COBBLESTONE_MINION")
        assertEquals("purrminions", id.namespace)
        assertEquals("COBBLESTONE_MINION", id.key)
        assertEquals("purrminions:COBBLESTONE_MINION", id.toString())
    }

    @Test
    fun `parse should create valid id`() {
        val id = MinionId.parse("purrminions:WHEAT_MINION")
        assertEquals("purrminions", id.namespace)
        assertEquals("WHEAT_MINION", id.key)
    }

    @Test
    fun `blank namespace should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionId("", "COBBLESTONE_MINION")
        }
    }

    @Test
    fun `blank key should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionId("purrminions", "")
        }
    }

    @Test
    fun `uppercase namespace should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionId("PurrMinions", "COBBLESTONE_MINION")
        }
    }

    @Test
    fun `lowercase key should throw`() {
        assertThrows<IllegalArgumentException> {
            MinionId("purrminions", "cobblestone_minion")
        }
    }

    @Test
    fun `invalid format should throw on parse`() {
        assertThrows<IllegalArgumentException> {
            MinionId.parse("invalidformat")
        }
    }

    @Test
    fun `namespace with underscores is valid`() {
        val id = MinionId("purr_minions", "TEST_MINION")
        assertEquals("purr_minions", id.namespace)
    }

    @Test
    fun `key with numbers is valid`() {
        val id = MinionId("purrminions", "MINION_V2")
        assertEquals("MINION_V2", id.key)
    }
}
