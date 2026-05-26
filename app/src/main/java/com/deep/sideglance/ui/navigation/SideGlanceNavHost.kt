package com.deep.sideglance.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deep.sideglance.data.ServiceLocator
import com.deep.sideglance.domain.model.User
import com.deep.sideglance.ui.screens.dashboard.DashboardScreen
import com.deep.sideglance.ui.screens.login.LoginScreen
import com.deep.sideglance.ui.screens.onboarding.OnboardingScreen
import com.deep.sideglance.ui.screens.pair.PairingScreen
import com.deep.sideglance.ui.screens.welcome.WelcomeScreen
import kotlinx.coroutines.launch

@Composable
fun SideGlanceNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val scope = rememberCoroutineScope()
    val authRepository = remember { ServiceLocator.authRepository }
    val partnerRepository = remember { ServiceLocator.partnerRepository }
    
    // Auth State
    val currentUser by authRepository.currentUser.collectAsState()
    
    // Local flow session checks
    var isCheckingSession by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        authRepository.checkUserSession()
        isCheckingSession = false
    }

    if (isCheckingSession) {
        // Simple loading screen
        return
    }

    // Determine start destination
    val startDestination = when {
        currentUser != null -> Screen.Dashboard.route
        else -> Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { displayName, email, idToken, photoUrl ->
                    authRepository.signInWithGoogle(
                        idToken = idToken,
                        displayName = displayName,
                        email = email,
                        photoUrl = photoUrl
                    ).onSuccess {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }.map { }
                }
            )
        }

        composable(Screen.Welcome.route) {
            val user = currentUser
            WelcomeScreen(
                displayName = user?.displayName ?: "User",
                onProceed = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val user = currentUser
            if (user != null) {
                DashboardScreen(
                    user = user,
                    onNavigateToPairing = {
                        navController.navigate(Screen.Pairing.route)
                    },
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Pairing.route) {
            val user = currentUser
            if (user != null) {
                var pairingCode by remember { mutableStateOf("") }
                LaunchedEffect(user.uid) {
                    pairingCode = partnerRepository.getPairingCode()
                }

                PairingScreen(
                    pairingCode = pairingCode,
                    onPairSubmitted = { code ->
                        partnerRepository.pairWithPartner(code).onSuccess {
                            navController.popBackStack()
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}
