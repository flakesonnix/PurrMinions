package gay.nyaa.purrminions.db

import com.purrcore.db.Database
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionInstance
import gay.nyaa.purrminions.domain.MinionLocation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MinionRepositoryTest {
    private lateinit var mockDatabase: Database
    private lateinit var mockLogger: Logger
    private lateinit var mockConnection: Connection
    private lateinit var repository: MinionRepository

    @BeforeEach
    fun setup() {
        mockDatabase = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockConnection = mockk(relaxed = true)

        every { mockDatabase.getConnection() } returns mockConnection

        repository = MinionRepository(mockDatabase, mockLogger)
    }

    @Test
    fun `migrate should create table`() {
        val mockStatement = mockk<Statement>(relaxed = true)
        every { mockConnection.createStatement() } returns mockStatement
        every { mockStatement.execute(any()) } returns true

        repository.migrate()

        verify { mockStatement.execute(match { it.contains("CREATE TABLE IF NOT EXISTS player_minions") }) }
    }

    @Test
    fun `save should insert minion`() {
        val minion = createTestMinion()
        val mockStatement = mockk<PreparedStatement>(relaxed = true)

        every { mockConnection.prepareStatement(any()) } returns mockStatement
        every { mockStatement.executeUpdate() } returns 1

        repository.save(minion)

        verify { mockStatement.setString(1, minion.owner.toString()) }
        verify { mockStatement.setString(2, minion.location.world) }
        verify { mockStatement.setInt(3, minion.location.x) }
        verify { mockStatement.setInt(4, minion.location.y) }
        verify { mockStatement.setInt(5, minion.location.z) }
        verify { mockStatement.setString(6, minion.type.toString()) }
        verify { mockStatement.setInt(7, minion.tier) }
        verify { mockStatement.setInt(8, minion.fuelTicks) }
        verify { mockStatement.executeUpdate() }
    }

    @Test
    fun `loadAll should return all minions`() {
        val mockStatement = mockk<Statement>(relaxed = true)
        val mockResultSet = mockResultSet()

        every { mockConnection.createStatement() } returns mockStatement
        every { mockStatement.executeQuery(any()) } returns mockResultSet
        every { mockResultSet.next() } returnsMany listOf(true, false)

        val minions = repository.loadAll()

        assertEquals(1, minions.size)
        assertEquals("world", minions[0].location.world)
    }

    @Test
    fun `loadByOwner should filter by owner`() {
        val owner = UUID.randomUUID()
        val mockStatement = mockk<PreparedStatement>(relaxed = true)
        val mockResultSet = mockResultSet()

        every { mockConnection.prepareStatement(any()) } returns mockStatement
        every { mockStatement.executeQuery() } returns mockResultSet
        every { mockResultSet.next() } returnsMany listOf(true, false)

        val minions = repository.loadByOwner(owner)

        verify { mockStatement.setString(1, owner.toString()) }
        assertEquals(1, minions.size)
    }

    @Test
    fun `delete should remove minion`() {
        val location = MinionLocation("world", 100, 64, 200)
        val mockStatement = mockk<PreparedStatement>(relaxed = true)

        every { mockConnection.prepareStatement(any()) } returns mockStatement
        every { mockStatement.executeUpdate() } returns 1

        repository.delete(location)

        verify { mockStatement.setString(1, location.world) }
        verify { mockStatement.setInt(2, location.x) }
        verify { mockStatement.setInt(3, location.y) }
        verify { mockStatement.setInt(4, location.z) }
        verify { mockStatement.executeUpdate() }
    }

    @Test
    fun `serialization should handle empty storage`() {
        val minion = createTestMinion(storage = emptyList())
        val mockStatement = mockk<PreparedStatement>(relaxed = true)

        every { mockConnection.prepareStatement(any()) } returns mockStatement
        every { mockStatement.executeUpdate() } returns 1

        repository.save(minion)

        verify { mockStatement.setString(9, "[]") }
    }

    @Test
    fun `deserialization should handle empty json`() {
        val mockStatement = mockk<Statement>(relaxed = true)
        val mockResultSet = mockk<ResultSet>(relaxed = true)

        every { mockConnection.createStatement() } returns mockStatement
        every { mockStatement.executeQuery(any()) } returns mockResultSet
        every { mockResultSet.next() } returnsMany listOf(true, false)
        every { mockResultSet.getString("owner") } returns UUID.randomUUID().toString()
        every { mockResultSet.getString("world") } returns "world"
        every { mockResultSet.getInt("x") } returns 100
        every { mockResultSet.getInt("y") } returns 64
        every { mockResultSet.getInt("z") } returns 200
        every { mockResultSet.getString("minion_id") } returns "test:COBBLESTONE_MINION"
        every { mockResultSet.getInt("tier") } returns 1
        every { mockResultSet.getInt("fuel_ticks") } returns 100
        every { mockResultSet.getString("storage_json") } returns "[]"
        every { mockResultSet.getLong("placed_at") } returns Instant.now().toEpochMilli()
        every { mockResultSet.getLong("last_tick") } returns Instant.now().toEpochMilli()

        val minions = repository.loadAll()

        assertEquals(1, minions.size)
        assertEquals(0, minions[0].storage.size)
    }

    private fun mockResultSet(): ResultSet {
        val mockResultSet = mockk<ResultSet>(relaxed = true)
        val now = Instant.now().toEpochMilli()

        every { mockResultSet.getString("owner") } returns UUID.randomUUID().toString()
        every { mockResultSet.getString("world") } returns "world"
        every { mockResultSet.getInt("x") } returns 100
        every { mockResultSet.getInt("y") } returns 64
        every { mockResultSet.getInt("z") } returns 200
        every { mockResultSet.getString("minion_id") } returns "test:COBBLESTONE_MINION"
        every { mockResultSet.getInt("tier") } returns 1
        every { mockResultSet.getInt("fuel_ticks") } returns 100
        every { mockResultSet.getString("storage_json") } returns "[]"
        every { mockResultSet.getLong("placed_at") } returns now
        every { mockResultSet.getLong("last_tick") } returns now

        return mockResultSet
    }

    private fun createTestMinion(
        owner: UUID = UUID.randomUUID(),
        location: MinionLocation = MinionLocation("world", 100, 64, 200),
        type: MinionId = MinionId("test", "COBBLESTONE_MINION"),
        tier: Int = 1,
        fuelTicks: Int = 100,
        storage: List<ItemStack> = emptyList()
    ) = MinionInstance(
        owner = owner,
        location = location,
        type = type,
        tier = tier,
        fuelTicks = fuelTicks,
        storage = storage,
        placedAt = Instant.now(),
        lastTick = Instant.now()
    )
}
