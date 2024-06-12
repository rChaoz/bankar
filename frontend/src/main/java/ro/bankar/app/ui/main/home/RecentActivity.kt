package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.FilledIcon
import ro.bankar.app.ui.components.ReceivedTransferRequestDialog
import ro.bankar.app.ui.components.RecentActivityRow
import ro.bankar.app.ui.components.RecentActivityShimmerRow
import ro.bankar.app.ui.components.SortedActivityItems
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackbar
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SCountries
import ro.bankar.banking.SExchangeData
import ro.bankar.model.SBankAccount
import ro.bankar.model.SDirection
import ro.bankar.model.SPartyPreview
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.STransferRequest
import kotlin.math.abs

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
        launch { model.repository.countryData.collect { model.countryData = it } }
        launch { model.repository.exchangeData.collect { model.exchangeData = it } }
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
            val (completedParties, pendingParties) = recentActivity.parties.partition(SPartyPreview::completed)
            // Display created parties & party invites first
            pendingParties.forEach { party ->
                PendingParty(party, onNavigate = { navigation.navigate(MainNav.ViewParty(party.id)) })
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
            // Then, display regular transfer requests
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

            // Finally, display transfers, transactions & completed parties
            val (transfers, transactions) = recentActivity
            SortedActivityItems(transfers, transactions, completedParties, navigation)

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

@Composable
private fun PendingParty(party: SPartyPreview, onNavigate: () -> Unit) {
    RecentActivityRow(onClick = onNavigate, elevated = true, icon = {
        FilledIcon(
            painter = painterResource(R.drawable.split_bill),
            contentDescription = null,
            color = MaterialTheme.colorScheme.primary,
        )
    }, title = stringResource(R.string.pending_party), subtitle = AnnotatedString(party.note)) {
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

@OptIn(ExperimentalMaterial3Api::class)
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
            val snackbar = LocalSnackbar.current

            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            repository.sendCancelTransferRequest(id).handleSuccess(this, snackbar, context) {}
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
    ReceivedTransferRequestDialog(
        visible = dialogVisible,
        onDismiss = { dialogVisible = false },
        user = request.user,
        requestID = request.id,
        amount = request.amount,
        currency = request.currency,
        partyID = request.partyID,
        note = request.note,
        onNavigateToFriend,
        onNavigateToParty
    )

    RecentActivityRow(
        elevated = true,
        onClick = {
            if (model.accounts.value.isEmpty()) model.noAccountsDialogState.show()
            else if (request.partyID != null) onNavigateToParty(request.partyID!!)
            else dialogVisible = true
        },
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
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbar = LocalSnackbar.current

        if (isDeclining) CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
        else AcceptDeclineButtons(onAccept = { if (model.accounts.value.isEmpty()) model.noAccountsDialogState.show() else dialogVisible = true }, onDecline = {
            scope.launch {
                isDeclining = true
                model.repository.sendRespondToTransferRequest(request.id, false, null).handleSuccess(this, snackbar, context) {}
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

@Preview(showBackground = true)
@Composable
private fun RecentActivityPreview() {
    AppTheme {
        LocalRepository.current.recentActivity.collectAsState(null).value?.let {
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

@Preview
@Composable
private fun RecentActivityShimmerPreview() {
    AppTheme {
        RecentActivityShimmer(shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}