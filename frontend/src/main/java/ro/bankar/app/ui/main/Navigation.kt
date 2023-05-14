package ro.bankar.app.ui.main

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.google.accompanist.navigation.animation.composable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.bankar.app.ui.main.friend.FriendProfileScreen
import ro.bankar.app.ui.main.friends.FriendsTab
import ro.bankar.app.ui.main.home.HomeTab
import ro.bankar.model.SPublicUser

@Suppress("ConstPropertyName")
private const val mainTabRoutePrefix = "mainTab"
@Suppress("ConstPropertyName")
private const val friendRoutePrefix = "mainTab"

@Suppress("FunctionName")
enum class MainNav(val route: String) {
    Friends("$mainTabRoutePrefix/${FriendsTab.name}"), Home("$mainTabRoutePrefix/${HomeTab.name}"),
    NewBankAccount("createAccount"),

    Profile("profile"),
    Friend("$friendRoutePrefix/{friend}");

    companion object {
        const val route = "main"
        const val tabsRoute = "$mainTabRoutePrefix/{tab}"
        val tabArguments = listOf(
            navArgument("tab") { defaultValue = HomeTab.name }
        )

        fun Friend(data: SPublicUser) = "$friendRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.mainNavigation(controller: NavHostController) {
    navigation(startDestination = MainNav.tabsRoute, route = MainNav.route) {
        composable(MainNav.tabsRoute, arguments = MainNav.tabArguments) { navEntry ->
            MainScreen(MainTabs.first { it.name == navEntry.arguments!!.getString("tab") }, controller)
        }

        composable(MainNav.NewBankAccount.route) {
            NewBankAccountScreen(onDismiss = controller::popBackStack)
        }

        composable(MainNav.Profile.route) {
            ProfileScreen(onDismiss = controller::popBackStack)
        }

        composable(MainNav.Friend.route) {
            FriendProfileScreen(profile = Json.decodeFromString(it.arguments!!.getString("friend")!!), onDismiss = controller::popBackStack)
        }
    }
}