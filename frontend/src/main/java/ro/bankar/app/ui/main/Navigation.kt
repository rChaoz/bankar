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
import ro.bankar.app.ui.main.friend.SendMoneyScreen
import ro.bankar.app.ui.main.friends.FriendsTab
import ro.bankar.app.ui.main.home.HomeTab
import ro.bankar.model.SPublicUser

@Suppress("ConstPropertyName")
private const val mainTabRoutePrefix = "mainTab"

@Suppress("ConstPropertyName")
private const val friendRoutePrefix = "friend"

@Suppress("ConstPropertyName")
private const val sendMoneyRoutePrefix = "sendMoneyTo"

@Suppress("FunctionName")
enum class MainNav(val route: String) {
    // Profile
    Profile("profile"),
    // Home
    Friends("$mainTabRoutePrefix/${FriendsTab.name}"), Home("$mainTabRoutePrefix/${HomeTab.name}"),
    NewBankAccount("createAccount"),
    // Friends
    Friend("$friendRoutePrefix/{friend}"), SendMoney("$sendMoneyRoutePrefix/{friend}");

    companion object {
        const val route = "main"
        const val tabsRoute = "$mainTabRoutePrefix/{tab}"
        val tabArguments = listOf(
            navArgument("tab") { defaultValue = HomeTab.name }
        )

        fun Friend(user: SPublicUser) = "$friendRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun SendMoney(user: SPublicUser) = "$sendMoneyRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.mainNavigation(controller: NavHostController) {
    navigation(startDestination = MainNav.tabsRoute, route = MainNav.route) {
        // Profile
        composable(MainNav.Profile.route) {
            ProfileScreen(onDismiss = controller::popBackStack)
        }
        // Home
        composable(MainNav.tabsRoute, arguments = MainNav.tabArguments) { navEntry ->
            MainScreen(MainTabs.first { it.name == navEntry.arguments!!.getString("tab") }, controller)
        }
        composable(MainNav.NewBankAccount.route) {
            NewBankAccountScreen(onDismiss = controller::popBackStack)
        }
        // Friends
        composable(MainNav.Friend.route) {
            FriendProfileScreen(profile = Json.decodeFromString(it.arguments!!.getString("friend")!!), navigation = controller)
        }
        composable(MainNav.SendMoney.route) {
            SendMoneyScreen(user = Json.decodeFromString(it.arguments!!.getString("friend")!!), onDismiss = controller::popBackStack)
        }
    }
}