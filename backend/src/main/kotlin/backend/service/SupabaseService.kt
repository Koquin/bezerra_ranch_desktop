package backend.service

import backend.SupabaseClient

object SupabaseService {
    private val client = SupabaseClient()
    fun ping(): Boolean = client.ping()
}
