package controller

import model.AppState

class NavController(private val state: AppState) {
    fun navigate(to: String) {
        // Simple state change; real navigation would trigger view updates
        state.currentScreen = to
    }
}
