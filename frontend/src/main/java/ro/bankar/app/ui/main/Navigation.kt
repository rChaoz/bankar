package ro.bankar.app.ui.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.google.accompanist.navigation.animation.composable
import ro.bankar.app.ui.main.home.HomeTab

@Suppress("ConstPropertyName")
private const val mainTabRoutePrefix = "mainTab"

enum class MainNav(val route: String) {
    Home("$mainTabRoutePrefix/${HomeTab.name}"),
    NewBankAccount("createAccount"),
    Profile("profile");

    companion object {
        const val route = "main"
        const val tabsRoute = "$mainTabRoutePrefix/{tab}"
        val tabArguments = listOf(
            navArgument("tab") { defaultValue = HomeTab.name }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.mainNavigation(controller: NavHostController) {
    navigation(startDestination = MainNav.tabsRoute, route = MainNav.route) {
        composable(MainNav.tabsRoute, arguments = MainNav.tabArguments) { navEntry ->
            MainScreen(MainTabs.first { it.name == navEntry.arguments?.getString("tab") }, controller)
        }

        composable(MainNav.NewBankAccount.route) {
            NewBankAccountScreen(onDismiss = { controller.popBackStack() })
        }

        composable(MainNav.Profile.route) {
            Profile(onDismiss = { controller.popBackStack() })
        }
    }
}