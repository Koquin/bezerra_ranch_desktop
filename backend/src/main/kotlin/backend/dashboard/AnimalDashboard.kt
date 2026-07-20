package backend.dashboard

import backend.model.Animal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

data class FazendaResumo(val fazenda: String, val machos: Int, val femeas: Int, val total: Int)
data class GrauSanguineoResumo(val grauSanguineo: String, val quantidade: Int)
data class IdadeAnimal(val animalId: Long?, val cria: String?, val idadeEmMeses: Long)

data class AnimalDashboard(
    val quantidadeAnimais: Int,
    val quantidadeMachos: Int,
    val quantidadeFemeas: Int,
    val porFazenda: List<FazendaResumo>,
    val porGrauSanguineo: List<GrauSanguineoResumo>,
    val idadeEmMeses: List<IdadeAnimal>
)

object AnimalDashboardCalculator {
    fun calculate(animals: List<Animal>, clock: Clock = Clock.systemUTC()): AnimalDashboard {
        val farms = animals.groupBy { it.fazenda?.trim().orEmpty().ifBlank { "Sem fazenda" } }
            .map { (fazenda, group) ->
                val machos = group.count { it.sexo.isMacho() }
                val femeas = group.count { it.sexo.isFemea() }
                FazendaResumo(fazenda, machos, femeas, group.size)
            }
            .sortedBy { it.fazenda }
        val ages = animals.mapNotNull { animal ->
            animal.dataNascimento?.let { IdadeAnimal(animal.id, animal.cria, monthsBetween(it, clock)) }
        }.sortedByDescending { it.idadeEmMeses }

        return AnimalDashboard(
            quantidadeAnimais = animals.size,
            quantidadeMachos = animals.count { it.sexo.isMacho() },
            quantidadeFemeas = animals.count { it.sexo.isFemea() },
            porFazenda = farms,
            porGrauSanguineo = animals.groupingBy { it.grauSanguineo?.trim().orEmpty().ifBlank { "Não informado" } }
                .eachCount().map { GrauSanguineoResumo(it.key, it.value) }.sortedBy { it.grauSanguineo },
            idadeEmMeses = ages
        )
    }

    private fun monthsBetween(birthDate: Instant, clock: Clock): Long {
        val birth = birthDate.atZone(ZoneOffset.UTC).toLocalDate()
        val current = clock.instant().atZone(ZoneOffset.UTC).toLocalDate()
        return ChronoUnit.MONTHS.between(birth, current).coerceAtLeast(0)
    }

    private fun String?.isMacho() = this?.trim()?.uppercase() in setOf("M", "MACHO")
    private fun String?.isFemea() = this?.trim()?.uppercase() in setOf("F", "FÊMEA", "FEMEA")
}
