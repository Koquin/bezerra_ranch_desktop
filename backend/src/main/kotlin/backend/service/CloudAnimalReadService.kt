package backend.service

import backend.Config
import backend.dashboard.AnimalDashboard
import backend.dashboard.AnimalDashboardCalculator
import backend.model.Animal
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Consulta somente leitura à tabela ANIMAL no Supabase.
 * Esta classe não acessa o SQLite e, portanto, não executa sincronização.
 */
class CloudAnimalReadService(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val url: String? = Config.supabaseUrl,
    private val anonKey: String? = Config.supabaseKey,
    private val table: String = Config.supabaseAnimalTable
) {
    fun listarAnimaisParaPainel(): List<Animal> {
        val baseUrl = requireNotNull(url) { "SUPABASE_URL não foi configurada no .env." }.trimEnd('/')
        val key = requireNotNull(anonKey) { "SUPABASE_ANON_KEY não foi configurada no .env." }
        val select = URLEncoder.encode("id,cria,sexo,fazenda,grau_sanguineo,data_nascimento", StandardCharsets.UTF_8)
        val request = HttpRequest.newBuilder(URI.create("$baseUrl/rest/v1/$table?select=$select"))
            .header("apikey", key)
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Não foi possível consultar os animais na nuvem (HTTP ${response.statusCode()})."
        }
        return response.body().toAnimals()
    }

    fun painel(): AnimalDashboard = AnimalDashboardCalculator.calculate(listarAnimaisParaPainel())

    private fun String.toAnimals(): List<Animal> {
        val array = JsonParser.parseString(this).asJsonArray
        return array.map { it.asJsonObject.toAnimal() }
    }

    private fun JsonObject.toAnimal() = Animal(
        id = long("id"),
        cria = string("cria"),
        sexo = string("sexo"),
        fazenda = string("fazenda"),
        grauSanguineo = string("grau_sanguineo"),
        dataNascimento = string("data_nascimento")?.let(Instant::parse)
    )

    private fun JsonObject.string(name: String): String? = takeIf { has(name) && !get(name).isJsonNull }?.get(name)?.asString
    private fun JsonObject.long(name: String): Long? = takeIf { has(name) && !get(name).isJsonNull }?.get(name)?.asLong
}
