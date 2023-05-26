@file:Suppress("ConstPropertyName")

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
import ro.bankar.app.ui.main.friend.ConversationScreen
import ro.bankar.app.ui.main.friend.FriendProfileScreen
import ro.bankar.app.ui.main.friend.RequestMoneyScreen
import ro.bankar.app.ui.main.friend.SendMoneyScreen
import ro.bankar.app.ui.main.friends.FriendsTab
import ro.bankar.app.ui.main.home.HomeTab
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SPublicUserBase

private const val mainTabRoutePrefix = "mainTab"
private const val friendRoutePrefix = "friend"
private const val conversationRoutePrefix = "conversationWith"
private const val sendMoneyRoutePrefix = "sendMoneyTo"
private const val requestMoneyRoutePrefix = "requestMoneyFrom"
private const val transactionRoutePrefix = "transaction"
private const val transferRoutePrefix = "transfer"
private const val selfTransferRoutePrefix = "selfTransfer"

@Suppress("FunctionName")
enum class MainNav(val route: String) {
    // Profile
    Profile("profile"),
    // Home
    Friends("$mainTabRoutePrefix/${FriendsTab.name}"), Home("$mainTabRoutePrefix/${HomeTab.name}"),
    NewBankAccount("createAccount"),
    // Details screens
    Transaction("$transactionRoutePrefix/{transaction}"),
    Transfer("$transferRoutePrefix/{transfer}"), SelfTransfer("$selfTransferRoutePrefix/{transfer}"),
    // Friends
    Friend("$friendRoutePrefix/{friend}"), Conversation("$conversationRoutePrefix/{friend}"),
    SendMoney("$sendMoneyRoutePrefix/{friend}"), RequestMoney("$requestMoneyRoutePrefix/{friend}");

    companion object {
        const val route = "main"
        const val tabsRoute = "$mainTabRoutePrefix/{tab}"
        val tabArguments = listOf(
            navArgument("tab") { defaultValue = HomeTab.name }
        )

        fun Friend(user: SPublicUserBase) = "$friendRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun Conversation(user: SPublicUserBase) = "$conversationRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun SendMoney(user: SPublicUserBase) = "$sendMoneyRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun RequestMoney(user: SPublicUserBase) = "$requestMoneyRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun Transaction(data: SCardTransaction) = "$transactionRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
        fun Transfer(data: SBankTransfer) = "$transferRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
        fun SelfTransfer(data: SBankTransfer) = "$selfTransferRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
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
        // Details screens
        composable(MainNav.Transaction.route) {
            TransactionDetailsScreen(onDismiss = controller::popBackStack, data = Json.decodeFromString(it.arguments!!.getString("transaction")!!))
        }
        composable(MainNav.Transfer.route) { entry ->
            ExternalTransferDetailsScreen(
                onDismiss = controller::popBackStack,
                data = Json.decodeFromString(entry.arguments!!.getString("transfer")!!),
                onNavigateToFriend = { controller.navigate(MainNav.Friend(it)) },
                onNavigateToAccount = {} // TODO
            )
        }
        composable(MainNav.SelfTransfer.route) { entry ->
            SelfTransferDetailsScreen(
                onDismiss = controller::popBackStack,
                data = Json.decodeFromString(entry.arguments!!.getString("transfer")!!),
                onNavigateToAccount = {} // TODO
            )
        }
        // Friends
        composable(MainNav.Friend.route) {
            FriendProfileScreen(profile = Json.decodeFromString(it.arguments!!.getString("friend")!!), navigation = controller)
        }
        composable(MainNav.Conversation.route) {
            ConversationScreen(user = Json.decodeFromString(it.arguments!!.getString("friend")!!), navigation = controller)
        }
        composable(MainNav.SendMoney.route) {
            SendMoneyScreen(user = Json.decodeFromString(it.arguments!!.getString("friend")!!), onDismiss = controller::popBackStack)
        }
        composable(MainNav.RequestMoney.route) {
            RequestMoneyScreen(user = Json.decodeFromString(it.arguments!!.getString("friend")!!), onDismiss = controller::popBackStack)
        }
    }
}