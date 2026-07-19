package backend

import java.io.File

object Config {
    private val env by lazy { loadEnvFile(File(".env")) }

    val supabaseUrl: String? = env["SUPABASE_URL"]
    val supabaseKey: String? = env["SUPABASE_KEY"]

    private fun loadEnvFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return file.readLines()
            .mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
                val idx = trimmed.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = trimmed.substring(0, idx).trim()
                val value = trimmed.substring(idx + 1).trim()
                key to value
            }
            .toMap()
    }
}
