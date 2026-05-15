package com.univesp.lumme.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.univesp.lumme.domain.repository.AuthRepository
import com.univesp.lumme.presentation.ui.auth.LoginScreen
import com.univesp.lumme.presentation.ui.dashboard.DashboardScreen
import com.univesp.lumme.presentation.ui.devices.DevicesScreen
import com.univesp.lumme.presentation.viewmodel.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val DEVICES = "devices"
}

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    authRepo: AuthRepository
) : ViewModel() {
    val authenticated: StateFlow<Boolean?> = authRepo.isAuthenticated
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@Composable
fun LummeNavGraph(
    oauthState: AuthState,
    navController: NavHostController = rememberNavController(),
    gateVm: AuthGateViewModel = hiltViewModel()
) {
    val authenticated by gateVm.authenticated.collectAsState()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) { LoginScreen() }
        composable(Routes.DASHBOARD) {
            DashboardScreen(onDevicesClick = { navController.navigate(Routes.DEVICES) })
        }
        composable(Routes.DEVICES) {
            DevicesScreen(onBack = { navController.popBackStack() })
        }
    }

    // Navega para Dashboard quando autenticado (JWT salvo OU acabou de logar)
    LaunchedEffect(authenticated, oauthState) {
        val shouldGoToDashboard = authenticated == true || oauthState is AuthState.Authenticated
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (shouldGoToDashboard && currentRoute == Routes.LOGIN) {
            navController.navigate(Routes.DASHBOARD) {
                popUpTo(Routes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
