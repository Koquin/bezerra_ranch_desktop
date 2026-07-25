package backend

import java.io.File

object Config {
    private val systemEnv by lazy { System.getenv() }
    private val localEnv by lazy { loadEnvFile(File(".env")) }
    private val packagedEnv by lazy { loadEnvResource("supabase.properties") }

    val supabaseUrl: String? = configuredValue("SUPABASE_URL")
    val supabaseKey: String? = configuredValue("SUPABASE_ANON_KEY", "SUPABASE_KEY")
    /** Nome da tabela exposta pela API REST do Supabase. */
    val supabaseAnimalTable: String = configuredValue("SUPABASE_ANIMAL_TABLE") ?: "animal"

    /**
     * Prioridade: variável do sistema, .env local e configuração incorporada ao instalador.
     * Assim o desenvolvimento pode sobrescrever os valores empacotados sem alterar o código.
     */
    private fun configuredValue(vararg names: String): String? {
        val sources = listOf(systemEnv, localEnv, packagedEnv)
        return sources.firstNotNullOfOrNull { source ->
            names.firstNotNullOfOrNull { name ->
                source[name]?.trim()?.takeIf(String::isNotEmpty)
            }
        }
    }

    private fun loadEnvFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return parseEnvLines(file.readLines())
    }

    private fun loadEnvResource(name: String): Map<String, String> {
        val stream = Config::class.java.classLoader.getResourceAsStream(name) ?: return emptyMap()
        return stream.bufferedReader(Charsets.UTF_8).use { parseEnvLines(it.readLines()) }
    }

    private fun parseEnvLines(lines: List<String>): Map<String, String> {
        return lines.mapNotNull { line ->
            val trimmed = line.trim().removePrefix("\uFEFF")
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
            val index = trimmed.indexOf('=')
            if (index <= 0) return@mapNotNull null
            val key = trimmed.substring(0, index).trim()
            val value = trimmed.substring(index + 1).trim()
            key to value
        }.toMap()
    }
}
