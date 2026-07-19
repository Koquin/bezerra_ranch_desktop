package backend

class SupabaseClient {
    private val url = Config.supabaseUrl
    private val key = Config.supabaseKey

    init {
        // Placeholder: in a real app you would initialize an HTTP client here
        if (url == null || key == null) {
            println("[SupabaseClient] Supabase configuration not found in .env")
        } else {
            println("[SupabaseClient] Supabase configured: $url")
        }
    }

    // Example placeholder method
    fun ping(): Boolean {
        return url != null && key != null
    }
}
