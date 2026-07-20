package backend.repository

import backend.database.SQLiteDatabase
import backend.model.Animal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant

class AnimalRepository(private val database: SQLiteDatabase) {
    fun create(animal: Animal): Animal {
        val now = Instant.now()
        val persisted = animal.copy(criadoEm = animal.criadoEm ?: now, atualizadoEm = animal.atualizadoEm ?: now)
        database.connection().use { connection ->
            val sql = if (persisted.id == null) INSERT_SQL else INSERT_WITH_ID_SQL
            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
                bindAnimal(statement, persisted, includeId = persisted.id != null)
                statement.executeUpdate()
                val id = persisted.id ?: statement.generatedKeys.use { keys ->
                    check(keys.next()) { "SQLite não retornou o identificador do animal criado." }
                    keys.getLong(1)
                }
                return persisted.copy(id = id)
            }
        }
    }

    fun findById(id: Long): Animal? = database.connection().use { connection ->
        connection.prepareStatement("SELECT * FROM animal WHERE id = ?").use { statement ->
            statement.setLong(1, id)
            statement.executeQuery().use { results -> if (results.next()) results.toAnimal() else null }
        }
    }

    fun findAll(): List<Animal> = database.connection().use { connection ->
        connection.prepareStatement("SELECT * FROM animal ORDER BY id DESC").use { statement ->
            statement.executeQuery().use { results -> results.asAnimals() }
        }
    }

    fun update(id: Long, animal: Animal): Animal? {
        val current = findById(id) ?: return null
        val updated = animal.copy(id = id, criadoEm = current.criadoEm, atualizadoEm = Instant.now())
        database.connection().use { connection ->
            connection.prepareStatement(UPDATE_SQL).use { statement ->
                bindAnimal(statement, updated, includeId = false)
                statement.setLong(24, id)
                return if (statement.executeUpdate() == 1) updated else null
            }
        }
    }

    fun delete(id: Long): Boolean = database.connection().use { connection ->
        connection.prepareStatement("DELETE FROM animal WHERE id = ?").use { statement ->
            statement.setLong(1, id)
            statement.executeUpdate() == 1
        }
    }

    private fun bindAnimal(statement: PreparedStatement, animal: Animal, includeId: Boolean) {
        var index = 1
        if (includeId) statement.setLong(index++, animal.id!!)
        statement.setNullableString(index++, animal.cria); statement.setNullableString(index++, animal.mae)
        statement.setNullableString(index++, animal.sexo); statement.setNullableString(index++, animal.raca)
        statement.setNullableDouble(index++, animal.peso); statement.setNullableString(index++, animal.pelagem)
        statement.setNullableInstant(index++, animal.dataNascimento); statement.setNullableString(index++, animal.fazenda)
        statement.setNullableString(index++, animal.observacao); statement.setNullableString(index++, animal.foto1)
        statement.setNullableString(index++, animal.foto2); statement.setNullableString(index++, animal.foto3)
        statement.setNullableString(index++, animal.locationCidade); statement.setNullableString(index++, animal.locationBairro)
        statement.setNullableDouble(index++, animal.locationLatitude); statement.setNullableDouble(index++, animal.locationLongitude)
        statement.setNullableLong(index++, animal.usuarioId); statement.setNullableInstant(index++, animal.criadoEm)
        statement.setNullableInstant(index++, animal.atualizadoEm); statement.setNullableString(index++, animal.status)
        statement.setNullableString(index++, animal.lote); statement.setNullableString(index++, animal.pasto)
        statement.setNullableString(index, animal.grauSanguineo)
    }

    private fun ResultSet.asAnimals(): List<Animal> = buildList { while (next()) add(toAnimal()) }

    private fun ResultSet.toAnimal() = Animal(
        id = getLong("id"), cria = getString("cria"), mae = getString("mae"), sexo = getString("sexo"),
        raca = getString("raca"), peso = getNullableDouble("peso"), pelagem = getString("pelagem"),
        dataNascimento = getInstant("data_nascimento"), fazenda = getString("fazenda"), observacao = getString("observacao"),
        foto1 = getString("foto1"), foto2 = getString("foto2"), foto3 = getString("foto3"),
        locationCidade = getString("location_cidade"), locationBairro = getString("location_bairro"),
        locationLatitude = getNullableDouble("location_latitude"), locationLongitude = getNullableDouble("location_longitude"),
        usuarioId = getNullableLong("usuario_id"), criadoEm = getInstant("criado_em"), atualizadoEm = getInstant("atualizado_em"),
        status = getString("status"), lote = getString("lote"), pasto = getString("pasto"), grauSanguineo = getString("grau_sanguineo")
    )

    private fun PreparedStatement.setNullableString(index: Int, value: String?) = setObject(index, value)
    private fun PreparedStatement.setNullableDouble(index: Int, value: Double?) = setObject(index, value)
    private fun PreparedStatement.setNullableLong(index: Int, value: Long?) = setObject(index, value)
    private fun PreparedStatement.setNullableInstant(index: Int, value: Instant?) = setObject(index, value?.toString())
    private fun ResultSet.getNullableDouble(column: String): Double? = getDouble(column).takeUnless { wasNull() }
    private fun ResultSet.getNullableLong(column: String): Long? = getLong(column).takeUnless { wasNull() }
    private fun ResultSet.getInstant(column: String): Instant? = getString(column)?.let(Instant::parse)

    private companion object {
        const val COLUMNS = "cria, mae, sexo, raca, peso, pelagem, data_nascimento, fazenda, observacao, foto1, foto2, foto3, location_cidade, location_bairro, location_latitude, location_longitude, usuario_id, criado_em, atualizado_em, status, lote, pasto, grau_sanguineo"
        const val VALUES = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
        const val INSERT_SQL = "INSERT INTO animal ($COLUMNS) VALUES ($VALUES)"
        const val INSERT_WITH_ID_SQL = "INSERT INTO animal (id, $COLUMNS) VALUES (?, $VALUES)"
        const val UPDATE_SQL = "UPDATE animal SET cria=?, mae=?, sexo=?, raca=?, peso=?, pelagem=?, data_nascimento=?, fazenda=?, observacao=?, foto1=?, foto2=?, foto3=?, location_cidade=?, location_bairro=?, location_latitude=?, location_longitude=?, usuario_id=?, criado_em=?, atualizado_em=?, status=?, lote=?, pasto=?, grau_sanguineo=? WHERE id=?"
    }
}
