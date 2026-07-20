package backend.sync

import backend.Config
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class DownloadedChunk(val records: List<JsonObject>, val bytes: Int, val latencyMillis: Long, val attempts: Int)

/** Cliente somente-leitura para os lotes do Supabase. HTTPS/TLS é obrigatório. */
class SupabaseDownloadClient(
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build(),
    private val url: String? = Config.supabaseUrl,
    private val anonKey: String? = Config.supabaseKey
) {
    fun download(table: SyncTable, offset: Long, limit: Int, maxRetries: Int): DownloadedChunk {
        val baseUrl = requireNotNull(url) { "SUPABASE_URL não foi configurada no .env." }.trimEnd('/')
        require(URI.create(baseUrl).scheme.equals("https", ignoreCase = true)) { "A sincronização requer uma URL HTTPS." }
        val key = requireNotNull(anonKey) { "SUPABASE_ANON_KEY não foi configurada no .env." }
        var lastError: Throwable? = null
        repeat(maxRetries + 1) { retry ->
            val start = System.nanoTime()
            try {
                val request = HttpRequest.newBuilder(URI.create("$baseUrl/rest/v1/${table.remoteName}?select=*&order=id.asc&offset=$offset&limit=$limit"))
                    .timeout(Duration.ofSeconds(60))
                    .header("apikey", key)
                    .header("Authorization", "Bearer $key")
                    .header("Accept", "application/json")
                    .header("Range-Unit", "items")
                    .header("Range", "$offset-${offset + limit - 1}")
                    .GET().build()
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
                if (response.statusCode() !in 200..299) error("Supabase respondeu HTTP ${response.statusCode()} para ${table.remoteName}.")
                val records = JsonParser.parseString(response.body().toString(Charsets.UTF_8)).asJsonArray.map { it.asJsonObject }
                return DownloadedChunk(records, response.body().size, (System.nanoTime() - start) / 1_000_000, retry + 1)
            } catch (error: Throwable) {
                lastError = error
                if (retry < maxRetries) Thread.sleep(250L * (1L shl retry))
            }
        }
        throw IllegalStateException("Falha ao baixar ${table.remoteName} após ${maxRetries + 1} tentativas.", lastError)
    }
}
