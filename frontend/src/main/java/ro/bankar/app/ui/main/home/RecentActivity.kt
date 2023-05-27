package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.state.StateDialog
import com.maxkeppeler.sheets.state.models.State
import com.maxkeppeler.sheets.state.models.StateConfig
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.data.collectRetrying
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.AccountsComboBox
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.FilledIcon
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.RecentActivityRow
import ro.bankar.app.ui.components.RecentActivityShimmerRow
import ro.bankar.app.ui.components.Transaction
import ro.bankar.app.ui.components.Transfer
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackBar
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.friend.FriendCard
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.serializableSaver
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SCountries
import ro.bankar.banking.SExchangeData
import ro.bankar.banking.exchange
import ro.bankar.banking.rate
import ro.bankar.banking.reverseExchange
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SDirection
import ro.bankar.model.SPartyPreview
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.STransferRequest
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

class RecentActivityModel : ViewModel() {
    var countryData by mutableStateOf<SCountries?>(null)
    var exchangeData by mutableStateOf<SExchangeData?>(null)
    val noAccountsDialogState = UseCaseState()
    lateinit var repository: Repository
    lateinit var accounts: androidx.compose.runtime.State<List<SBankAccount>>
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivity(recentActivity: SRecentActivity, accounts: List<SBankAccount>, navigation: NavHostController) {
    val model = viewModel<RecentActivityModel>()
    model.repository = LocalRepository.current
    model.accounts = rememberUpdatedState(accounts)
    LaunchedEffect(true) {
        launch { model.repository.countryData.collectRetrying { model.countryData = it } }
        launch { model.repository.exchangeData.collectRetrying { model.exchangeData = it } }
    }

    StateDialog(
        state = model.noAccountsDialogState,
        config = StateConfig(State.Failure(labelText = stringResource(R.string.no_accounts_send_receive), postView = {
            TextButton(onClick = model.noAccountsDialogState::hide) {
                Text(text = stringResource(android.R.string.ok))
            }
        }))
    )

    HomeCard(
        title = stringResource(R.string.recent_activity),
        icon = { Icon(painter = painterResource(R.drawable.baseline_recent_24), contentDescription = null) }) {
        if (recentActivity.isEmpty()) InfoCard(text = R.string.no_recent_activity, tonalElevation = 0.dp)
        else {
            recentActivity.parties.forEach { party ->
                CreatedParty(party, onNavigate = { navigation.navigate(MainNav.ViewParty(party.id)) })
            }
            val (partyInvites, otherRequests) = recentActivity.transferRequests.partition { it.partyID != null }
            partyInvites.forEach { invitation ->
                ReceivedTransferRequest(
                    invitation,
                    model,
                    onNavigateToFriend = { navigation.navigate(MainNav.Friend(it)) },
                    onNavigateToParty = { navigation.navigate(MainNav.ViewParty(it)) }
                )
            }
            otherRequests.forEach { request ->
                if (request.direction == SDirection.Sent)
                    SentTransferRequest(
                        id = request.id,
                        fromName = "${request.user.firstName} ${request.user.lastName}",
                        amount = request.amount,
                        currency = request.currency
                    )
                else ReceivedTransferRequest(
                    request,
                    model,
                    onNavigateToFriend = { navigation.navigate(MainNav.Friend(it)) },
                    onNavigateToParty = { navigation.navigate(MainNav.ViewParty(it)) }
                )
            }

            // Merge display transfers and transactions
            val (transfers, transactions) = recentActivity
            var transferI = 0
            var transactionI = 0
            // Limit total number of entries to 3
            while (transferI + transactionI < 3 && (transferI < transfers.size || transactionI < transactions.size)) {
                if (transferI < transfers.size && (transactionI >= transactions.size || transfers[transferI].dateTime > transactions[transactionI].dateTime))
                    transfers[transferI++].let {
                        Transfer(it, onNavigate = {
                            navigation.navigate(if (it.sourceAccountID == null) MainNav.Transfer(it) else MainNav.SelfTransfer(it))
                        })
                    }
                else {
                    val transaction = transactions[transactionI++]
                    Transaction(transaction, onNavigate = { navigation.navigate(MainNav.Transaction(transaction)) })
                }
            }
            TextButton(
                onClick = { navigation.navigate(MainNav.RecentActivity.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(text = stringResource(R.string.see_more))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentActivityPreview() {
    AppTheme {
        LocalRepository.current.recentActivity.collectAsStateRetrying().value?.let {
            RecentActivity(it, emptyList(), rememberMockNavController())
        } ?: RecentActivityShimmer(shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecentActivityPreviewDark() {
    AppTheme {
        RecentActivity(SRecentActivity(emptyList(), emptyList(), emptyList(), emptyList()), emptyList(), rememberMockNavController())
    }
}

@Composable
private fun CreatedParty(party: SPartyPreview, onNavigate: () -> Unit) {
    RecentActivityRow(onClick = onNavigate, icon = {
        FilledIcon(
            painter = painterResource(R.drawable.split_bill),
            contentDescription = null,
            color = MaterialTheme.colorScheme.primary,
        )
    }, title = stringResource(R.string.my_party), subtitle = AnnotatedString(party.note)) {
        Column(horizontalAlignment = Alignment.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Amount(amount = party.collected, currency = party.currency, textStyle = MaterialTheme.typography.titleSmall)
                Text(text = "/", style = MaterialTheme.typography.titleSmall)
            }
            Amount(
                amount = party.total,
                currency = party.currency,
                textStyle = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SentTransferRequest(id: Int, fromName: String, amount: Double, currency: Currency) {
    var isLoading by remember { mutableStateOf(false) }

    RecentActivityRow(icon = {
        FilledIcon(
            painter = painterResource(R.drawable.transfer_request),
            contentDescription = stringResource(R.string.transfer_request),
            color = MaterialTheme.colorScheme.tertiary,
        )
    }, title = stringResource(R.string.to_s, fromName), subtitle = buildAnnotatedString {
        append(stringResource(if (amount > 0) R.string.sent else R.string.requesting))
        pushStyle(SpanStyle(color = (-amount).amountColor))
        append(currency.format(abs(amount)))
    }) {
        val scope = rememberCoroutineScope()

        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        else {
            val repository = LocalRepository.current
            val context = LocalContext.current
            val snackBar = LocalSnackBar.current

            OutlinedButton(
                onClick = {
                    isLoading = true
                    scope.launch {
                        when (val r = repository.sendCancelTransferRequest(id)) {
                            is SafeStatusResponse.InternalError -> launch { snackBar.showSnackbar(context.getString(r.message), withDismissAction = true) }
                            is SafeStatusResponse.Fail -> launch { snackBar.showSnackbar(context.getString(R.string.unknown_error), withDismissAction = true) }
                            is SafeStatusResponse.Success -> {
                                repository.accounts.requestEmit()
                                repository.recentActivity.emitNow()
                            }
                        }
                        isLoading = false
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    }
}

@Composable
private fun ReceivedTransferRequest(
    request: STransferRequest,
    model: RecentActivityModel,
    onNavigateToFriend: (SPublicUser) -> Unit,
    onNavigateToParty: (Int) -> Unit
) {
    var isDeclining by rememberSaveable { mutableStateOf(false) }
    var dialogVisible by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedAccount = rememberSaveable(stateSaver = serializableSaver()) { mutableStateOf(model.accounts.value.find { it.currency == request.currency }) }
    val exchangeRate by remember {
        derivedStateOf {
            val account = selectedAccount.value ?: return@derivedStateOf null
            if (account.currency == request.currency) return@derivedStateOf 1.0
            model.exchangeData?.rate(
                if (request.amount > 0) request.currency else account.currency,
                if (request.amount > 0) account.currency else request.currency
            )
        }
    }
    val exchangedAmount by remember {
        derivedStateOf {
            val account = selectedAccount.value ?: return@derivedStateOf null
            val data = model.exchangeData ?: return@derivedStateOf null
            if (account.currency == request.currency) return@derivedStateOf request.amount.absoluteValue
            if (request.amount > 0) data.exchange(request.currency, account.currency, request.amount)
            else data.reverseExchange(account.currency, request.currency, -request.amount)
        }
    }

    // Display transfer details with dialog
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val showLoading by remember { derivedStateOf { isLoading || model.exchangeData == null } }
    BottomDialog(
        visible = dialogVisible,
        onDismissRequest = { dialogVisible = false },
        buttonBar = {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = { dialogVisible = false }, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                val context = LocalContext.current
                FilledTonalButton(
                    enabled = !showLoading, onClick = {
                        scope.launch {
                            isLoading = true
                            // Use toasts instead of snackbar because snackbar isn't visible over dialog
                            when (val r = model.repository.sendRespondToTransferRequest(request.id, false, null)) {
                                is SafeStatusResponse.InternalError -> Toast.makeText(context, r.message, Toast.LENGTH_SHORT).show()
                                is SafeStatusResponse.Fail -> Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
                                is SafeStatusResponse.Success -> {
                                    model.repository.recentActivity.emitNow()
                                    dialogVisible = false
                                }
                            }
                            isLoading = false
                        }
                    }, modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.decline))
                }

                Button(
                    enabled = !showLoading && selectedAccount.value != null && exchangedAmount != null
                            && (exchangedAmount!! <= selectedAccount.value!!.spendable || request.amount > 0), onClick = {
                        scope.launch {
                            val account = selectedAccount.value ?: return@launch
                            isLoading = true
                            // Use toasts instead of snackbar because snackbar isn't visible over dialog
                            when (val r = model.repository.sendRespondToTransferRequest(request.id, true, account.id)) {
                                is SafeStatusResponse.InternalError -> Toast.makeText(context, r.message, Toast.LENGTH_SHORT).show()
                                is SafeStatusResponse.Fail -> Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
                                is SafeStatusResponse.Success -> {
                                    coroutineScope {
                                        launch { model.repository.accounts.emitNow() }
                                        launch { model.repository.recentActivity.emitNow() }
                                    }
                                    dialogVisible = false
                                }
                            }
                            isLoading = false
                        }
                    }, modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.accept))
                }
            }
        },
    ) {
        LoadingOverlay(showLoading) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                    .padding(vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (request.partyID != null) Text(
                    text = stringResource(R.string.party_invitation),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                if (request.user.isFriend) {
                    Surface(
                        onClick = { dialogVisible = false; onNavigateToFriend(request.user) },
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 4.dp
                    ) {
                        FriendCard(friend = request.user, model.countryData.nameFromCode(request.user.countryCode), modifier = Modifier.padding(12.dp))
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 4.dp
                    ) {
                        FriendCard(friend = request.user, model.countryData.nameFromCode(request.user.countryCode), modifier = Modifier.padding(12.dp))
                    }
                }
                if (request.partyID != null) {
                    OutlinedButton(
                        onClick = { dialogVisible = false; onNavigateToParty(request.partyID!!) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = stringResource(R.string.view_party))
                    }
                }

                Divider()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = stringResource(if (request.amount > 0) R.string.has_sent else R.string.has_requested))
                        Amount(
                            amount = request.amount.absoluteValue,
                            currency = request.currency,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            withPlusSign = request.amount > 0,
                            color = request.amount.amountColor
                        )
                    }
                    if (request.note.isNotBlank()) Text(
                        text = "\"${request.note}\"",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                Divider()
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    AccountsComboBox(
                        selectedAccount,
                        model.accounts.value,
                        pickText = if (request.amount > 0) R.string.choose_account_request else R.string.choose_account_send,
                        showBalance = true
                    )
                    val selectedCurrency = selectedAccount.value?.currency ?: Currency.EURO
                    AnimatedVisibility(visible = selectedAccount.value != null && selectedCurrency != request.currency) {
                        val rate = exchangeRate

                        Column {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            val fromCurrency = if (request.amount > 0) request.currency else selectedCurrency
                            val toCurrency = if (request.amount > 0) selectedCurrency else request.currency
                            if (rate == null || exchangedAmount == null) Text(
                                text = stringResource(R.string.exchange_not_available, fromCurrency, toCurrency),
                                style = MaterialTheme.typography.labelMedium
                            )
                            else {
                                Row {
                                    Text(text = stringResource(R.string.conversion_rate), style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = "%s = %s".format(fromCurrency.format(1.0), toCurrency.format(rate)),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row {
                                    Text(
                                        text = stringResource(if (request.amount > 0) R.string.you_will_receive else R.string.you_will_send),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = selectedCurrency.format(exchangedAmount ?: 0.0),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = request.amount.amountColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val account = selectedAccount.value
                                Text(
                                    text = stringResource(
                                        when {
                                            account == null -> R.string.remaining_balance
                                            account.type == SBankAccountType.Credit && request.amount > 0 -> R.string.credit_after_receiving
                                            account.type != SBankAccountType.Credit && request.amount > 0 -> R.string.balance_after_receiving
                                            account.type == SBankAccountType.Credit -> R.string.remaining_credit
                                            else -> R.string.remaining_balance
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (account != null) exchangedAmount?.let {
                                    val remaining = account.spendable + it * request.amount.sign
                                    Text(
                                        text = account.currency.format(remaining), style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold, color = remaining.amountColor
                                    )
                                } ?: Text(
                                    text = stringResource(R.string.error), style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.customColors.red
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    RecentActivityRow(
        elevated = true,
        onClick = { if (model.accounts.value.isEmpty()) model.noAccountsDialogState.show() else dialogVisible = true },
        icon = {
            FilledIcon(
                painter = painterResource(if (request.partyID == null) R.drawable.transfer_request else R.drawable.split_bill),
                contentDescription = stringResource(if (request.partyID == null) R.string.transfer_request else R.string.party_invitation),
                color = if (request.partyID == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
        },
        title = stringResource(
            if (request.partyID == null) R.string.from_s else R.string.party_invite_from, "${request.user.firstName} ${request.user.lastName}"
        ),
        subtitle = buildAnnotatedString {
            if (request.partyID == null) {
                append(stringResource(if (request.amount > 0) R.string.has_sent else R.string.has_requested))
                pushStyle(SpanStyle(color = request.amount.amountColor))
                append(request.currency.format(abs(request.amount)))
            } else {
                pushStyle(SpanStyle(color = MaterialTheme.customColors.red))
                append(request.currency.format(request.amount))
                pop()
                append(" • ", request.note)
            }
        }
    ) {
        val context = LocalContext.current
        val snackBar = LocalSnackBar.current

        if (isDeclining) CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
        else AcceptDeclineButtons(onAccept = { if (model.accounts.value.isEmpty()) model.noAccountsDialogState.show() else dialogVisible = true }, onDecline = {
            scope.launch {
                isDeclining = true
                when (val r = model.repository.sendRespondToTransferRequest(request.id, false, null)) {
                    is SafeStatusResponse.InternalError -> launch { snackBar.showSnackbar(context.getString(r.message), withDismissAction = true) }
                    is SafeStatusResponse.Fail -> launch { snackBar.showSnackbar(context.getString(R.string.unknown_error), withDismissAction = true) }
                    is SafeStatusResponse.Success -> model.repository.recentActivity.emitNow()
                }
                isDeclining = false
            }
        })
    }
}

@Composable
fun RecentActivityShimmer(shimmer: Shimmer) {
    HomeCard(title = stringResource(R.string.recent_activity), shimmer = shimmer, icon = {
        Icon(painter = painterResource(R.drawable.baseline_recent_24), contentDescription = null, modifier = Modifier.shimmer(shimmer))
    }) {
        repeat(3) { RecentActivityShimmerRow(shimmer) }
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(18.dp)
                .size(80.dp, 16.dp)
                .grayShimmer(shimmer)
        )
    }
}

@Preview
@Composable
private fun RecentActivityShimmerPreview() {
    AppTheme {
        RecentActivityShimmer(shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}