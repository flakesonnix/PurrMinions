package gay.nyaa.purrminions.db

import com.purrcore.db.Database
import gay.nyaa.purrminions.domain.MinionId
import gay.nyaa.purrminions.domain.MinionInstance
import gay.nyaa.purrminions.domain.MinionLocation
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

class MinionRepository(
    private val database: Database,
    private val logger: Logger,
) {
    fun migrate() {
        database.getConnection().use { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS player_minions (
                    owner VARCHAR(36),
                    world VARCHAR(64),
                    x INT,
                    y INT,
                    z INT,
                    minion_id VARCHAR(64),
                    tier INT,
                    fuel_ticks INT,
                    storage_json TEXT,
                    placed_at BIGINT,
                    last_tick BIGINT,
                    PRIMARY KEY (world, x, y, z)
                )
                """.trimIndent(),
            )
        }
    }

    fun save(minion: MinionInstance) {
        database.getConnection().use { conn ->
            val stmt =
                conn.prepareStatement(
                    """
                    INSERT INTO player_minions (owner, world, x, y, z, minion_id, tier, fuel_ticks, storage_json, placed_at, last_tick)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (world, x, y, z) DO UPDATE SET
                        tier = excluded.tier,
                        fuel_ticks = excluded.fuel_ticks,
                        storage_json = excluded.storage_json,
                        last_tick = excluded.last_tick
                    """.trimIndent(),
                )
            stmt.setString(1, minion.owner.toString())
            stmt.setString(2, minion.location.world)
            stmt.setInt(3, minion.location.x)
            stmt.setInt(4, minion.location.y)
            stmt.setInt(5, minion.location.z)
            stmt.setString(6, minion.type.toString())
            stmt.setInt(7, minion.tier)
            stmt.setInt(8, minion.fuelTicks)
            stmt.setString(9, serializeStorage(minion.storage))
            stmt.setLong(10, minion.placedAt.toEpochMilli())
            stmt.setLong(11, minion.lastTick.toEpochMilli())
            stmt.executeUpdate()
        }
    }

    fun loadAll(): List<MinionInstance> {
        database.getConnection().use { conn ->
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM player_minions")

            val minions = mutableListOf<MinionInstance>()
            while (rs.next()) {
                minions.add(mapMinion(rs))
            }
            return minions
        }
    }

    fun loadByOwner(owner: UUID): List<MinionInstance> {
        database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("SELECT * FROM player_minions WHERE owner = ?")
            stmt.setString(1, owner.toString())
            val rs = stmt.executeQuery()

            val minions = mutableListOf<MinionInstance>()
            while (rs.next()) {
                minions.add(mapMinion(rs))
            }
            return minions
        }
    }

    fun delete(location: MinionLocation) {
        database.getConnection().use { conn ->
            val stmt = conn.prepareStatement("DELETE FROM player_minions WHERE world = ? AND x = ? AND y = ? AND z = ?")
            stmt.setString(1, location.world)
            stmt.setInt(2, location.x)
            stmt.setInt(3, location.y)
            stmt.setInt(4, location.z)
            stmt.executeUpdate()
        }
    }

    private fun mapMinion(rs: ResultSet): MinionInstance =
        MinionInstance(
            owner = UUID.fromString(rs.getString("owner")),
            location =
                MinionLocation(
                    world = rs.getString("world"),
                    x = rs.getInt("x"),
                    y = rs.getInt("y"),
                    z = rs.getInt("z"),
                ),
            type = MinionId.parse(rs.getString("minion_id")),
            tier = rs.getInt("tier"),
            fuelTicks = rs.getInt("fuel_ticks"),
            storage = deserializeStorage(rs.getString("storage_json")),
            placedAt = Instant.ofEpochMilli(rs.getLong("placed_at")),
            lastTick = Instant.ofEpochMilli(rs.getLong("last_tick")),
        )

    private fun serializeStorage(items: List<ItemStack>): String {
        if (items.isEmpty()) return "[]"
        
        return try {
            // Use ItemLog's ItemSerializer if available
            val serializerClass = Class.forName("com.itemlog.serialization.ItemSerializer")
            val serializer = serializerClass.getDeclaredConstructor().newInstance()
            val serializeMethod = serializerClass.getMethod("serialize", ItemStack::class.java)
            
            val jsonArray = com.google.gson.JsonArray()
            for (item in items) {
                val itemJson = serializeMethod.invoke(serializer, item) as String?
                if (itemJson != null) {
                    jsonArray.add(com.google.gson.JsonParser.parseString(itemJson))
                }
            }
            com.google.gson.GsonBuilder().create().toJson(jsonArray)
        } catch (e: Exception) {
            logger.warning("Failed to serialize minion storage: ${e.message}")
            "[]"
        }
    }

    private fun deserializeStorage(json: String): List<ItemStack> {
        if (json.isBlank() || json == "[]") return emptyList()
        
        return try {
            // Use ItemLog's ItemSerializer if available
            val serializerClass = Class.forName("com.itemlog.serialization.ItemSerializer")
            val serializer = serializerClass.getDeclaredConstructor().newInstance()
            val deserializeMethod = serializerClass.getMethod("deserialize", String::class.java)
            
            val jsonArray = com.google.gson.JsonParser.parseString(json).asJsonArray
            val items = mutableListOf<ItemStack>()
            
            for (element in jsonArray) {
                val itemJson = element.toString()
                val item = deserializeMethod.invoke(serializer, itemJson) as ItemStack?
                if (item != null) {
                    items.add(item)
                }
            }
            items
        } catch (e: Exception) {
            logger.warning("Failed to deserialize minion storage: ${e.message}")
            emptyList()
        }
    }
}
