package backend.sync

import backend.database.SQLiteDatabase
import com.google.gson.JsonParser
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals

class SQLiteSyncRepositoryTest {
    @Test
    fun `salva lote e atualiza registros remotos pelo id`() {
        val file = createTempFile("bezerra-sync-", ".db").toFile()
        try {
            val database = SQLiteDatabase(file)
            val repository = SQLiteSyncRepository(database)
            val first = JsonParser.parseString("""[{"id":1,"cria":"BR-01","peso":130.5,"usuario_id":9}]""").asJsonArray.map { it.asJsonObject }
            val updated = JsonParser.parseString("""[{"id":1,"cria":"BR-01A","peso":150.0,"usuario_id":9},{"id":2,"cria":"BR-02"}]""").asJsonArray.map { it.asJsonObject }

            assertEquals(1, repository.upsertChunk(SyncTable.ANIMAL, first))
            assertEquals(2, repository.upsertChunk(SyncTable.ANIMAL, updated))
            database.connection().use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*), MAX(peso), MAX(cria) FROM animal").use { result ->
                        result.next()
                        assertEquals(2, result.getInt(1))
                        assertEquals(150.0, result.getDouble(2))
                        assertEquals("BR-02", result.getString(3))
                    }
                    statement.executeQuery("SELECT cria FROM animal WHERE id = 1").use { result ->
                        result.next()
                        assertEquals("BR-01A", result.getString(1))
                    }
                }
            }
        } finally {
            file.delete()
        }
    }
}
