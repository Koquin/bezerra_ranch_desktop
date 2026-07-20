package backend.repository

import backend.dashboard.AnimalDashboardCalculator
import backend.database.SQLiteDatabase
import backend.model.Animal
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AnimalRepositoryTest {
    @Test
    fun `executa ciclo completo de CRUD e calcula painel`() {
        val databaseFile = createTempFile("bezerra-ranch-", ".db").toFile()
        try {
            val repository = AnimalRepository(SQLiteDatabase(databaseFile))
            val created = repository.create(
                Animal(
                    cria = "BR-001", sexo = "F", fazenda = "Fazenda Norte",
                    grauSanguineo = "1/2", peso = 150.5,
                    dataNascimento = Instant.parse("2025-01-15T00:00:00Z")
                )
            )
            val id = assertNotNull(created.id)
            assertEquals(created, repository.findById(id))

            val updated = repository.update(id, created.copy(sexo = "M", fazenda = "Fazenda Sul"))
            assertEquals("M", updated?.sexo)
            assertEquals("Fazenda Sul", updated?.fazenda)
            assertEquals(1, repository.findAll().size)

            val dashboard = AnimalDashboardCalculator.calculate(
                repository.findAll(), Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC)
            )
            assertEquals(1, dashboard.quantidadeAnimais)
            assertEquals(1, dashboard.quantidadeMachos)
            assertEquals(0, dashboard.quantidadeFemeas)
            assertEquals("Fazenda Sul", dashboard.porFazenda.single().fazenda)
            assertEquals(18, dashboard.idadeEmMeses.single().idadeEmMeses)

            assertTrue(repository.delete(id))
            assertFalse(repository.delete(id))
            assertNull(repository.findById(id))
        } finally {
            databaseFile.delete()
        }
    }
}
