package ro.bankar.app.ui.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.google.accompanist.navigation.animation.composable

enum class MainNav(val route: String) {
    Home("home"), NewBankAccount("createAccount");

    companion object {
        const val route = "main"
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.mainNavigation(controller: NavHostController) {
    navigation(startDestination = MainNav.Home.route, route = MainNav.route) {
        composable(MainNav.Home.route) {
            MainScreen(controller)
        }
        composable(MainNav.NewBankAccount.route) {
            NewBankAccountScreen(onDismiss = { controller.popBackStack() })
        }
    }
}