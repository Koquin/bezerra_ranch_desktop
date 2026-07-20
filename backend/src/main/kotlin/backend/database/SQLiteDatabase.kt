package backend.database

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/** Banco offline; nenhuma instrução deste componente acessa a nuvem. */
class SQLiteDatabase(databaseFile: File = defaultDatabaseFile()) {
    private val jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"

    init {
        databaseFile.parentFile?.mkdirs()
        Class.forName("org.sqlite.JDBC")
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                TABLES.forEach(statement::execute)
                migrateAnimalDataColumn(statement)
                statement.execute("CREATE INDEX IF NOT EXISTS idx_animal_fazenda ON animal(fazenda)")
                statement.execute("CREATE INDEX IF NOT EXISTS idx_animal_sexo ON animal(sexo)")
                statement.execute("CREATE INDEX IF NOT EXISTS idx_baixa_log_animal ON baixa_log(animal_id)")
                statement.execute("CREATE INDEX IF NOT EXISTS idx_nascimento_log_animal ON nascimento_log(animal_id)")
                statement.execute("CREATE INDEX IF NOT EXISTS idx_transferencia_log_animal ON transferencia_log(animal_id)")
            }
        }
    }

    fun connection(): Connection = DriverManager.getConnection(jdbcUrl)

    private fun migrateAnimalDataColumn(statement: java.sql.Statement) {
        val columns = statement.executeQuery("PRAGMA table_info(animal)").use { result ->
            buildSet { while (result.next()) add(result.getString("name")) }
        }
        // Compatibilidade com a primeira versão local, que chamava a coluna de `data`.
        if ("data" in columns && "data_nascimento" !in columns) {
            statement.execute("ALTER TABLE animal ADD COLUMN data_nascimento TEXT")
            statement.execute("UPDATE animal SET data_nascimento = data WHERE data_nascimento IS NULL")
        }
    }

    companion object {
        private fun defaultDatabaseFile(): File = File("data/bezerra-ranch.db")

        private val TABLES = listOf(
            """CREATE TABLE IF NOT EXISTS animal (
                id INTEGER PRIMARY KEY, cria TEXT, mae TEXT, sexo TEXT, raca TEXT, peso REAL,
                pelagem TEXT, data_nascimento TEXT, fazenda TEXT, observacao TEXT,
                foto1 TEXT, foto2 TEXT, foto3 TEXT, location_cidade TEXT, location_bairro TEXT,
                location_latitude REAL, location_longitude REAL, usuario_id INTEGER,
                criado_em TEXT, atualizado_em TEXT, status TEXT, lote TEXT, pasto TEXT, grau_sanguineo TEXT
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS baixa_log (
                id INTEGER PRIMARY KEY, animal_id INTEGER, data_morte TEXT, fazenda TEXT,
                foto1 TEXT, foto2 TEXT, foto3 TEXT, audio TEXT, descricao TEXT, usuario_id INTEGER,
                criado_em TEXT, atualizado_em TEXT, location_cidade TEXT, location_bairro TEXT,
                location_latitude REAL, location_longitude REAL, tipo_baixa TEXT
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS nascimento_log (
                id INTEGER PRIMARY KEY, cria TEXT, mae TEXT, sexo TEXT, raca TEXT, pelagem TEXT,
                data_nascimento TEXT, fazenda TEXT, observacao TEXT, foto1 TEXT, foto2 TEXT, foto3 TEXT,
                usuario_id INTEGER, criado_em TEXT, atualizado_em TEXT, location_cidade TEXT,
                location_bairro TEXT, location_latitude REAL, location_longitude REAL, animal_id INTEGER,
                peso REAL, lote TEXT, pasto TEXT
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS solicitacao_faixa (
                id INTEGER PRIMARY KEY, usuario_id INTEGER, usuario_nome TEXT, prefixo TEXT,
                inicio_atual INTEGER, max_atual INTEGER, restantes INTEGER, solicitado_em TEXT,
                status TEXT, atualizado_em TEXT
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS transferencia_log (
                id INTEGER PRIMARY KEY, animal_id INTEGER, fazenda_origem TEXT, fazenda_destino TEXT,
                lote_origem TEXT, lote_destino TEXT, pasto_origem TEXT, pasto_destino TEXT,
                usuario_id INTEGER, data_transferencia TEXT, data_registro TEXT, atualizado_em TEXT,
                is_inconsistency INTEGER
            )""".trimIndent(),
            """CREATE TABLE IF NOT EXISTS usuario (
                id INTEGER PRIMARY KEY, nome TEXT, login TEXT, senha_hash TEXT, ativo INTEGER,
                cria_prefixo TEXT, cria_inicio INTEGER, cria_max INTEGER, is_admin INTEGER,
                criado_em TEXT, atualizado_em TEXT
            )""".trimIndent()
        )
    }
}
