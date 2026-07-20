package backend.model

import java.time.Instant

/**
 * Registro local equivalente à tabela ANIMAL do banco em nuvem.
 *
 * [dataNascimento] corresponde à coluna `data`; fotos e campos opcionais podem
 * ser nulos até que sejam preenchidos no cadastro.
 */
data class Animal(
    val id: Long? = null,
    val cria: String? = null,
    val mae: String? = null,
    val sexo: String? = null,
    val raca: String? = null,
    val peso: Double? = null,
    val pelagem: String? = null,
    val dataNascimento: Instant? = null,
    val fazenda: String? = null,
    val observacao: String? = null,
    val foto1: String? = null,
    val foto2: String? = null,
    val foto3: String? = null,
    val locationCidade: String? = null,
    val locationBairro: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val usuarioId: Long? = null,
    val criadoEm: Instant? = null,
    val atualizadoEm: Instant? = null,
    val status: String? = null,
    val lote: String? = null,
    val pasto: String? = null,
    val grauSanguineo: String? = null
)
