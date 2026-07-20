package backend.sync

/** Tabelas permitidas na sincronização. Os nomes não vêm de entrada do usuário. */
enum class SyncTable(val remoteName: String, val columns: List<SyncColumn>) {
    ANIMAL("animal", listOf(
        "id", "cria", "mae", "sexo", "raca", "peso", "pelagem", "data_nascimento", "fazenda", "observacao", "foto1", "foto2", "foto3", "location_cidade", "location_bairro", "location_latitude", "location_longitude", "usuario_id", "criado_em", "atualizado_em", "status", "lote", "pasto", "grau_sanguineo"
    ).syncColumns()),
    BAIXA_LOG("baixa_log", listOf(
        "id", "animal_id", "data_morte", "fazenda", "foto1", "foto2", "foto3", "audio", "descricao", "usuario_id", "criado_em", "atualizado_em", "location_cidade", "location_bairro", "location_latitude", "location_longitude", "tipo_baixa"
    ).syncColumns()),
    NASCIMENTO_LOG("nascimento_log", listOf(
        "id", "cria", "mae", "sexo", "raca", "pelagem", "data_nascimento", "fazenda", "observacao", "foto1", "foto2", "foto3", "usuario_id", "criado_em", "atualizado_em", "location_cidade", "location_bairro", "location_latitude", "location_longitude", "animal_id", "peso", "lote", "pasto"
    ).syncColumns()),
    SOLICITACAO_FAIXA("solicitacao_faixa", listOf(
        "id", "usuario_id", "usuario_nome", "prefixo", "inicio_atual", "max_atual", "restantes", "solicitado_em", "status", "atualizado_em"
    ).syncColumns()),
    TRANSFERENCIA_LOG("transferencia_log", listOf(
        "id", "animal_id", "fazenda_origem", "fazenda_destino", "lote_origem", "lote_destino", "pasto_origem", "pasto_destino", "usuario_id", "data_transferencia", "data_registro", "atualizado_em", "is_inconsistency"
    ).syncColumns()),
    USUARIO("usuario", listOf(
        "id", "nome", "login", "senha_hash", "ativo", "cria_prefixo", "cria_inicio", "cria_max", "is_admin", "criado_em", "atualizado_em"
    ).syncColumns());
}

data class SyncColumn(val name: String, val type: SyncValueType)
enum class SyncValueType { INTEGER, REAL, BOOLEAN, TEXT }

private fun List<String>.syncColumns(): List<SyncColumn> = map { SyncColumn(it, syncTypeFor(it)) }
private fun syncTypeFor(name: String): SyncValueType = when (name) {
    "id", "animal_id", "usuario_id", "inicio_atual", "max_atual", "restantes", "cria_inicio", "cria_max" -> SyncValueType.INTEGER
    "peso", "location_latitude", "location_longitude" -> SyncValueType.REAL
    "ativo", "is_admin", "is_inconsistency" -> SyncValueType.BOOLEAN
    else -> SyncValueType.TEXT
}
