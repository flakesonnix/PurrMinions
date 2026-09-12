package gay.nyaa.purrminions.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MinionActionTest {
    @Test
    fun `all minion actions should be accessible`() {
        assertEquals(MinionAction.MINE, MinionAction.valueOf("MINE"))
        assertEquals(MinionAction.FARM, MinionAction.valueOf("FARM"))
        assertEquals(MinionAction.CHOP, MinionAction.valueOf("CHOP"))
        assertEquals(MinionAction.FISH, MinionAction.valueOf("FISH"))
        assertEquals(MinionAction.KILL, MinionAction.valueOf("KILL"))
    }

    @Test
    fun `should have 5 actions`() {
        assertEquals(5, MinionAction.entries.size)
    }
}
