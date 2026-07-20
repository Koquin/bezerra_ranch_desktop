package backend.controller

import backend.database.SQLiteDatabase
import backend.sync.DownloadSyncConfig
import backend.sync.DownloadSynchronizationService
import backend.sync.FullDownloadResult
import backend.sync.SQLiteSyncRepository
import backend.sync.SyncTable

/** Entrada da aplicação para sincronização de download. Não expõe upload. */
class SynchronizationController(
    private val service: DownloadSynchronizationService = DownloadSynchronizationService(
        sqlite = SQLiteSyncRepository(SQLiteDatabase())
    )
) {
    fun baixarTudo(config: DownloadSyncConfig = DownloadSyncConfig()): FullDownloadResult = service.downloadAll(config)
    fun baixarTabela(tabela: SyncTable, config: DownloadSyncConfig = DownloadSyncConfig()) = service.downloadTable(tabela, config)
}
