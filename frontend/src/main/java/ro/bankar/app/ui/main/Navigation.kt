package ro.bankar.app.ui.main

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.bankar.app.Nav
import ro.bankar.app.ui.main.friend.ConversationScreen
import ro.bankar.app.ui.main.friend.FriendProfileScreen
import ro.bankar.app.ui.main.friend.RequestMoneyScreen
import ro.bankar.app.ui.main.friend.SendMoneyScreen
import ro.bankar.app.ui.main.friends.FriendsTab
import ro.bankar.app.ui.main.home.HomeTab
import ro.bankar.app.ui.main.settings.SettingsTab
import ro.bankar.model.SBankAccount
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
private const val newTransferRoutePrefix = "newTransfer"

private const val bankAccountRoutePrefix = "bankAccount"
private const val bankCardRoutePrefix = "bankCard"
private const val createPartyRoutePrefix = "createParty"
private const val viewPartyRoutePrefix = "viewParty"

@Suppress("FunctionName")
enum class MainNav(val route: String) {
    // Home
    Friends("$mainTabRoutePrefix/${FriendsTab.name}"),
    Home("$mainTabRoutePrefix/${HomeTab.name}"),
    Settings("$mainTabRoutePrefix/${SettingsTab.name}"),
    // Profile & bank account stuff
    Profile("profile"),
    NewBankAccount("createAccount"),
    Statements("statements"),
    // Details & transfer screens
    Transaction("$transactionRoutePrefix/{transaction}"),
    Transfer("$transferRoutePrefix/{transfer}"), SelfTransfer("$selfTransferRoutePrefix/{transfer}"),
    NewTransfer("$newTransferRoutePrefix/{from}?to={to}"),
    // Accounts & recent activity
    RecentActivity("recentActivity"),
    BankAccount("$bankAccountRoutePrefix/{account}"),
    BankCard("$bankCardRoutePrefix?account={account}&card={card}"),
    // Friends
    Friend("$friendRoutePrefix/{friend}"), Conversation("$conversationRoutePrefix/{friend}"),
    SendMoney("$sendMoneyRoutePrefix/{friend}"), RequestMoney("$requestMoneyRoutePrefix/{friend}"),
    // Parties
    CreateParty("$createPartyRoutePrefix?amount={amount}&account={account}"), ViewParty("$viewPartyRoutePrefix/{id}");

    companion object {
        const val route = "main"
        const val tabsRoute = "$mainTabRoutePrefix/{tab}"
        val tabArguments = listOf(
            navArgument("tab") { defaultValue = HomeTab.name },
        )
        val createPartyArguments = listOf(
            navArgument("amount") { defaultValue = 0f; type = NavType.FloatType },
            navArgument("account") { defaultValue = -1; type = NavType.IntType },
        )
        val viewPartyArguments = listOf(
            navArgument("id") { type = NavType.IntType },
        )
        val newTransferArguments = listOf(
            navArgument("from") { type = NavType.IntType },
            navArgument("to") { defaultValue = -1; type = NavType.IntType }
        )
        val bankCardArguments = listOf(
            navArgument("account") { type = NavType.IntType },
            navArgument("card") { type = NavType.IntType },
        )

        fun Friend(user: SPublicUserBase) = "$friendRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun Conversation(user: SPublicUserBase) = "$conversationRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun SendMoney(user: SPublicUserBase) = "$sendMoneyRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun RequestMoney(user: SPublicUserBase) = "$requestMoneyRoutePrefix/${Uri.encode(Json.encodeToString(user))}"
        fun Transaction(data: SCardTransaction) = "$transactionRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
        fun Transfer(data: SBankTransfer) = "$transferRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
        fun NewTransfer(from: SBankAccount, to: SBankAccount?) = "$newTransferRoutePrefix/${from.id}${if (to != null) "?to=${to.id}" else ""}"
        fun SelfTransfer(data: SBankTransfer) = "$selfTransferRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
        fun BankAccount(data: SBankAccount) = "$bankAccountRoutePrefix/${Uri.encode(Json.encodeToString(data))}"
        fun BankCard(account: Int, card: Int) = "$bankCardRoutePrefix?account=$account&card=$card"
        fun CreateParty(amount: Double, account: Int) = "$createPartyRoutePrefix?amount=$amount&account=$account"
        fun ViewParty(id: Int) = "$viewPartyRoutePrefix/$id"
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.mainNavigation(controller: NavHostController) {
    navigation(startDestination = MainNav.tabsRoute, route = MainNav.route) {
        // Main tabs
        composable(MainNav.tabsRoute, arguments = MainNav.tabArguments) { navEntry ->
            MainScreen(MainTabs.find { it.name == navEntry.arguments!!.getString("tab") } ?: HomeTab, controller)
        }
        // Profile & bank accounts
        composable(MainNav.Profile.route) {
            ProfileScreen(onDismiss = controller::popBackStack, onLogout = {
                controller.navigate(Nav.NewUser.route) {
                    popUpTo(Nav.Main.route) { inclusive = true }
                }
            })
        }
        composable(MainNav.NewBankAccount.route) {
            NewBankAccountScreen(onDismiss = controller::popBackStack)
        }
        composable(MainNav.Statements.route) {
            StatementsScreen(onDismiss = controller::popBackStack)
        }
        // Details screens
        composable(MainNav.Transaction.route) {
            TransactionDetailsScreen(
                onDismiss = controller::popBackStack,
                data = Json.decodeFromString(it.arguments!!.getString("transaction")!!),
                onCreateParty = { amount, account -> controller.navigate(MainNav.CreateParty(amount, account)) },
                onNavigateToCard = { accountID, cardID -> controller.navigate(MainNav.BankCard(accountID, cardID)) }
            )
        }
        composable(MainNav.Transfer.route) { entry ->
            ExternalTransferDetailsScreen(
                onDismiss = controller::popBackStack,
                data = Json.decodeFromString(entry.arguments!!.getString("transfer")!!),
                onNavigateToFriend = { controller.navigate(MainNav.Friend(it)) },
                onNavigateToAccount = { controller.navigate(MainNav.BankAccount(it)) },
                onNavigateToParty = { controller.navigate(MainNav.ViewParty(it)) },
                onCreateParty = { amount, account -> controller.navigate(MainNav.CreateParty(amount, account)) }
            )
        }
        composable(MainNav.SelfTransfer.route) { entry ->
            SelfTransferDetailsScreen(
                onDismiss = controller::popBackStack,
                data = Json.decodeFromString(entry.arguments!!.getString("transfer")!!),
                onNavigateToAccount = { controller.navigate(MainNav.BankAccount(it)) }
            )
        }
        composable(MainNav.NewTransfer.route, arguments = MainNav.newTransferArguments) { entry ->
            TransferScreen(
                onDismiss = controller::popBackStack,
                sourceAccountID = entry.arguments!!.getInt("from"),
                targetAccountID = entry.arguments!!.getInt("to").let { if (it == -1) null else it },
                onNavigateToAccount = { account -> controller.navigate(MainNav.BankAccount(account)) }
            )
        }
        // Accounts & recent activity
        composable(MainNav.RecentActivity.route) {
            RecentActivityScreen(onDismiss = controller::popBackStack, navigation = controller)
        }
        composable(MainNav.BankAccount.route) {
            val account = Json.decodeFromString<SBankAccount>(it.arguments!!.getString("account")!!)
            BankAccountScreen(
                onDismiss = controller::popBackStack,
                data = account,
                navigation = controller,
                onNavigateToCard = { cardID -> controller.navigate(MainNav.BankCard(account.id, cardID)) }
            )
        }
        composable(MainNav.BankCard.route, arguments = MainNav.bankCardArguments) { entry ->
            BankCardScreen(
                onDismiss = controller::popBackStack,
                navigation = controller,
                accountID = entry.arguments!!.getInt("account"),
                cardID = entry.arguments!!.getInt("card"),
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
        // Parties
        composable(MainNav.CreateParty.route, arguments = MainNav.createPartyArguments) { entry ->
            val amount = entry.arguments!!.getFloat("amount")
            val account = entry.arguments!!.getInt("account")
            CreatePartyScreen(onDismiss = controller::popBackStack, amount.toDouble(), account)
        }
        composable(MainNav.ViewParty.route, arguments = MainNav.viewPartyArguments) { entry ->
            ViewPartyScreen(onDismiss = controller::popBackStack, partyID = entry.arguments!!.getInt("id"), onNavigateToFriend = {
                controller.navigate(MainNav.Friend(it))
            }, onNavigateToTransfer = {
                controller.navigate(MainNav.Transfer(it))
            })
        }
    }
}