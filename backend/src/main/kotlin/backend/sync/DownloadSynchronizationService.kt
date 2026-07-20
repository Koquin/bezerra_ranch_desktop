package backend.sync

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.logging.Logger
import kotlin.math.roundToInt

data class DownloadSyncConfig(
    val targetChunkBytes: Int = 512 * 1024,
    val concurrency: Int = 4,
    val initialRowsPerChunk: Int = 1_000,
    val maxRetries: Int = 3
) {
    init {
        require(targetChunkBytes in 256 * 1024..2 * 1024 * 1024) { "O chunk deve ter entre 256 KB e 2 MB." }
        require(concurrency in 1..4) { "A concorrência deve estar entre 1 e 4." }
        require(initialRowsPerChunk > 0) { "O limite de linhas deve ser positivo." }
    }
}

data class ChunkMetric(val offset: Long, val records: Int, val bytes: Int, val latencyMillis: Long, val saveMillis: Long, val attempts: Int)
data class TableDownloadResult(val table: SyncTable, val records: Int, val chunks: List<ChunkMetric>, val totalMillis: Long) {
    val throughputBytesPerSecond: Long
        get() = if (totalMillis == 0L) 0 else chunks.sumOf { it.bytes }.toLong() * 1_000 / totalMillis
}
data class FullDownloadResult(val tables: List<TableDownloadResult>) {
    val totalRecords: Int get() = tables.sumOf { it.records }
}

/**
 * Baixa e persiste dados remotos. Não contém função de upload: nesta fase o
 * Supabase nunca é alterado pelo desktop.
 */
class DownloadSynchronizationService(
    private val client: SupabaseDownloadClient = SupabaseDownloadClient(),
    private val sqlite: SQLiteSyncRepository
) {
    fun downloadAll(config: DownloadSyncConfig = DownloadSyncConfig()): FullDownloadResult =
        FullDownloadResult(SyncTable.values().map { downloadTable(it, config) })

    fun downloadTable(table: SyncTable, config: DownloadSyncConfig = DownloadSyncConfig()): TableDownloadResult {
        val startedAt = System.nanoTime()
        val metrics = mutableListOf<ChunkMetric>()
        var offset = 0L
        var rowsPerChunk = config.initialRowsPerChunk
        val executor = Executors.newFixedThreadPool(config.concurrency)
        try {
            while (true) {
                val usedRowsPerChunk = rowsPerChunk
                val offsets = (0 until config.concurrency).map { offset + it.toLong() * rowsPerChunk }
                val futures = offsets.map { chunkOffset ->
                    executor.submit(Callable { chunkOffset to client.download(table, chunkOffset, rowsPerChunk, config.maxRetries) })
                }
                val chunks = futures.map { it.get() }.sortedBy { it.first }
                chunks.forEach { (chunkOffset, chunk) ->
                    val saveStartedAt = System.nanoTime()
                    sqlite.upsertChunk(table, chunk.records)
                    metrics += ChunkMetric(
                        offset = chunkOffset, records = chunk.records.size, bytes = chunk.bytes,
                        latencyMillis = chunk.latencyMillis,
                        saveMillis = (System.nanoTime() - saveStartedAt) / 1_000_000,
                        attempts = chunk.attempts
                    )
                    logger.info("sync-download table=${table.remoteName} offset=$chunkOffset records=${chunk.records.size} bytes=${chunk.bytes} latencyMs=${chunk.latencyMillis} saveMs=${metrics.last().saveMillis} attempts=${chunk.attempts}")
                }
                if (chunks.any { it.second.records.size < rowsPerChunk }) break
                val averageBytes = chunks.map { it.second.bytes }.average()
                rowsPerChunk = (rowsPerChunk * config.targetChunkBytes / averageBytes)
                    .roundToInt().coerceIn(50, 1_000)
                offset += chunks.size.toLong() * usedRowsPerChunk
            }
        } finally {
            executor.shutdownNow()
        }
        return TableDownloadResult(table, metrics.sumOf { it.records }, metrics.toList(), (System.nanoTime() - startedAt) / 1_000_000)
    }

    private companion object {
        val logger: Logger = Logger.getLogger(DownloadSynchronizationService::class.java.name)
    }
}
