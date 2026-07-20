package backend.sync

import backend.database.SQLiteDatabase
import com.google.gson.JsonObject
import java.sql.Connection

/** Insere ou atualiza um lote inteiro em uma única transação local. */
class SQLiteSyncRepository(private val database: SQLiteDatabase) {
    fun upsertChunk(table: SyncTable, records: List<JsonObject>): Int {
        if (records.isEmpty()) return 0
        require(records.all { it.has("id") && !it.get("id").isJsonNull }) {
            "Todo registro baixado de ${table.remoteName} precisa ter id."
        }
        database.connection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(upsertSql(table)).use { statement ->
                    records.forEach { record ->
                        table.columns.forEachIndexed { index, column ->
                            statement.setObject(index + 1, record.toSqlValue(column))
                        }
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
                return records.size
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun upsertSql(table: SyncTable): String {
        val names = table.columns.joinToString(", ") { it.name }
        val values = table.columns.joinToString(", ") { "?" }
        val updates = table.columns.filterNot { it.name == "id" }
            .joinToString(", ") { "${it.name}=excluded.${it.name}" }
        return "INSERT INTO ${table.remoteName} ($names) VALUES ($values) ON CONFLICT(id) DO UPDATE SET $updates"
    }

    private fun JsonObject.toSqlValue(column: SyncColumn): Any? {
        if (!has(column.name) || get(column.name).isJsonNull) return null
        val value = get(column.name).asJsonPrimitive
        return when (column.type) {
            SyncValueType.INTEGER -> value.asLong
            SyncValueType.REAL -> value.asDouble
            SyncValueType.BOOLEAN -> if (value.asBoolean) 1 else 0
            SyncValueType.TEXT -> value.asString
        }
    }
}
