package com.deep.sideglance.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Welcome : Screen("welcome")
    object Dashboard : Screen("dashboard")
    object Pairing : Screen("pairing")
}
