package ro.bankar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.valentinilk.shimmer.Shimmer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackbar
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.friend.UserSurfaceCard
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.serializableSaver
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.banking.exchange
import ro.bankar.banking.rate
import ro.bankar.banking.reverseExchange
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SDirection
import ro.bankar.model.SPartyPreview
import ro.bankar.model.SPublicUser
import ro.bankar.model.STimestamped
import ro.bankar.model.STransferRequest
import ro.bankar.util.format
import ro.bankar.util.here
import kotlin.math.absoluteValue
import kotlin.math.sign

@Composable
fun RecentActivityRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: AnnotatedString,
    trailingContent: @Composable () -> Unit
) {
    Surface(tonalElevation = 8.dp) {
        RecentActivityRowBase(icon, title, subtitle, trailingContent)
    }
}

@Composable
fun RecentActivityRow(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: AnnotatedString,
    elevated: Boolean = false,
    trailingContent: @Composable () -> Unit
) {
    Surface(onClick, tonalElevation = if (elevated) 8.dp else 0.dp) {
        RecentActivityRowBase(icon, title, subtitle, trailingContent)
    }
}

@Composable
private fun RecentActivityRowBase(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: AnnotatedString,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent()
    }
}

@Composable
fun Transaction(data: SCardTransaction, onNavigate: () -> Unit) {
    RecentActivityRow(onClick = onNavigate, icon = {
        FilledIcon(
            painter = painterResource(R.drawable.transaction),
            contentDescription = stringResource(R.string.transaction),
            color = MaterialTheme.colorScheme.secondary,
        )
    }, title = data.title, subtitle = AnnotatedString(data.dateTime.toInstant(TimeZone.UTC).here().format())) {
        Amount(-data.amount, data.currency)
    }
}

@Composable
fun Transfer(data: SBankTransfer, onNavigate: () -> Unit) {
    RecentActivityRow(
        onClick = onNavigate,
        icon = {
            FilledIcon(
                painter = painterResource(
                    when {
                        data.partyID != null -> R.drawable.split_bill
                        data.direction != null -> R.drawable.transfer
                        else -> R.drawable.self_transfer
                    }
                ),
                contentDescription = stringResource(R.string.transfer),
                color = if (data.partyID != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
        },
        title = when {
            data.direction == SDirection.Received -> stringResource(R.string.from_s, data.fullName)
            // For a party transfer, direction is always 'Sent'
            data.direction == SDirection.Sent -> stringResource(R.string.to_s, data.fullName)
            data.exchangedAmount == null -> stringResource(R.string.self_transfer)
            else -> stringResource(R.string.exchange)
        },
        subtitle = AnnotatedString(data.dateTime.toInstant(TimeZone.UTC).here().format())
    ) {
        when {
            data.direction != null ->
                Amount(
                    if (data.direction == SDirection.Sent) -data.relevantAmount else data.relevantAmount,
                    if (data.direction == SDirection.Sent) data.currency else data.exchangedCurrency,
                    withPlusSign = true
                )

            data.exchangedAmount == null ->
                Amount(data.amount, data.currency, color = MaterialTheme.colorScheme.onSurface)

            else ->
                Column(horizontalAlignment = Alignment.End) {
                    Amount(amount = -data.amount, currency = data.currency, textStyle = MaterialTheme.typography.titleSmall)
                    Amount(
                        amount = data.exchangedAmount!!,
                        currency = data.exchangedCurrency,
                        withPlusSign = true,
                        textStyle = MaterialTheme.typography.titleSmall
                    )
                }
        }
    }
}

@Composable
fun ReceivedTransferRequestDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    user: SPublicUser?,
    requestID: Int,
    amount: Double,
    currency: Currency,
    partyID: Int?,
    note: String?,
    onNavigateToFriend: (SPublicUser) -> Unit,
    onNavigateToParty: (Int) -> Unit,
    showDeclineOption: Boolean = true
) {
    val repository = LocalRepository.current
    val scope = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<SBankAccount>?>(null) }
    val exchangeData by repository.exchangeData.collectAsState(null)
    val countryData by repository.countryData.collectAsState(null)

    val selectedAccount = rememberSaveable(stateSaver = serializableSaver()) { mutableStateOf<SBankAccount?>(null) }
    LaunchedEffect(true) {
        repository.accounts.collect { newAccounts ->
            accounts = newAccounts
            if (selectedAccount.value == null) selectedAccount.value = newAccounts.firstOrNull { it.currency == currency }
        }
    }

    val exchangeRate by remember {
        derivedStateOf {
            val account = selectedAccount.value ?: return@derivedStateOf null
            if (account.currency == currency) return@derivedStateOf 1.0
            exchangeData?.rate(
                if (amount > 0) currency else account.currency,
                if (amount > 0) account.currency else currency
            )
        }
    }
    val exchangedAmount by remember {
        derivedStateOf {
            val account = selectedAccount.value ?: return@derivedStateOf null
            val data = exchangeData ?: return@derivedStateOf null
            if (account.currency == currency) return@derivedStateOf amount.absoluteValue
            if (amount > 0) data.exchange(currency, account.currency, amount)
            else data.reverseExchange(account.currency, currency, -amount)
        }
    }
    var isLoading by remember { mutableStateOf(false) }
    val showLoading by remember { derivedStateOf { isLoading || exchangeData == null } }

    BottomDialog(
        visible, onDismiss,
        buttonBar = {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                val context = LocalContext.current
                if (showDeclineOption) TextButton(
                    enabled = !showLoading, onClick = {
                        scope.launch {
                            isLoading = true
                            // Use toasts instead of snackbar because snackbar isn't visible over dialog
                            repository.sendRespondToTransferRequest(requestID, false, null).handleSuccess(context) {
                                repository.recentActivity.emitNow()
                                onDismiss()
                            }
                            isLoading = false
                        }
                    }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(R.string.decline))
                }

                TextButton(
                    enabled = !showLoading && selectedAccount.value != null && exchangedAmount != null
                            && (exchangedAmount!! <= selectedAccount.value!!.spendable || amount > 0), onClick = {
                        scope.launch {
                            val account = selectedAccount.value ?: return@launch
                            isLoading = true
                            // Use toasts instead of snackbar because snackbar isn't visible over dialog
                            repository.sendRespondToTransferRequest(requestID, true, account.id).handleSuccess(context) {
                                coroutineScope {
                                    launch { repository.accounts.emitNow() }
                                    launch { repository.recentActivity.emitNow() }
                                }
                                onDismiss()
                            }
                            isLoading = false
                        }
                    }, modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.accept), color = MaterialTheme.customColors.green)
                }
            }
        },
    ) {
        LoadingOverlay(showLoading) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp)
            ) {
                if (partyID != null) Text(
                    text = stringResource(R.string.party_invitation),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .align(Alignment.CenterHorizontally)
                )
                @Suppress("ControlFlowWithEmptyBody")
                if (user == null) ;
                else UserSurfaceCard(
                    user,
                    country = countryData.nameFromCode(user.countryCode),
                    modifier = Modifier.padding(12.dp),
                    snackbar = LocalSnackbar.current,
                    isFriend = user.isFriend,
                    onClick = { onDismiss(); onNavigateToFriend(user) }
                )
                if (partyID != null) {
                    OutlinedButton(
                        onClick = { onDismiss(); onNavigateToParty(partyID) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = stringResource(R.string.view_party))
                    }
                }

                if (user != null) Divider()

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
                        Text(text = stringResource(if (amount > 0) R.string.has_sent else R.string.has_requested))
                        Amount(
                            amount = amount.absoluteValue,
                            currency = currency,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            withPlusSign = amount > 0,
                            color = amount.amountColor
                        )
                    }
                    if (!note.isNullOrBlank()) Text(
                        text = "\"$note\"",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                Divider()
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    AccountsComboBox(
                        selectedAccount,
                        accounts,
                        pickText = if (amount > 0) R.string.choose_account_request else R.string.choose_account_send,
                        showBalance = true
                    )
                    val selectedCurrency = selectedAccount.value?.currency ?: Currency.EURO
                    AnimatedVisibility(visible = selectedAccount.value != null && selectedCurrency != currency) {
                        val rate = exchangeRate

                        Column {
                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            val fromCurrency = if (amount > 0) currency else selectedCurrency
                            val toCurrency = if (amount > 0) selectedCurrency else currency
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
                                        text = stringResource(if (amount > 0) R.string.you_will_receive else R.string.you_will_send),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = selectedCurrency.format(exchangedAmount ?: 0.0),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = amount.amountColor
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
                                            account.type == SBankAccountType.Credit && amount > 0 -> R.string.credit_after_receiving
                                            account.type != SBankAccountType.Credit && amount > 0 -> R.string.balance_after_receiving
                                            account.type == SBankAccountType.Credit -> R.string.remaining_credit
                                            else -> R.string.remaining_balance
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                if (account != null) exchangedAmount?.let {
                                    val remaining = account.spendable + it * amount.sign
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
}

@Composable
fun CompletedParty(data: SPartyPreview, onNavigate: () -> Unit) {
    RecentActivityRow(
        onClick = onNavigate, icon = {
            FilledIcon(
                painter = painterResource(R.drawable.split_bill),
                contentDescription = null,
                color = MaterialTheme.colorScheme.primary,
            )
        }, title = stringResource(R.string.completed_party),
        subtitle = AnnotatedString(data.dateTime.toInstant(TimeZone.UTC).here().format())
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Amount(amount = data.collected, currency = data.currency, textStyle = MaterialTheme.typography.titleSmall)
                Text(text = "/", style = MaterialTheme.typography.titleSmall)
            }
            Amount(
                amount = data.total,
                currency = data.currency,
                textStyle = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun SortedActivityItems(
    transfers: List<SBankTransfer>,
    transactions: List<SCardTransaction>,
    parties: List<SPartyPreview>,
    navigation: NavHostController
) {
    for (item in (transfers + transactions + parties).sortedDescending()) TimestampedItem(item, navigation)
}


/**
 * 'item' must not be [STransferRequest] or incomplete [SPartyPreview].
 */
@Composable
fun TimestampedItem(item: STimestamped, navigation: NavHostController) = when (item) {
    is SBankTransfer -> Transfer(item, onNavigate = {
        navigation.navigate(if (item.direction != null) MainNav.Transfer(item) else MainNav.SelfTransfer(item))
    })
    is SCardTransaction -> Transaction(item, onNavigate = { navigation.navigate(MainNav.Transaction(item)) })
    is SPartyPreview -> if (!item.completed) throw IllegalArgumentException("Party item must be completed") else CompletedParty(item, onNavigate = {
        navigation.navigate(MainNav.ViewParty(item.id))
    })
    is STransferRequest -> throw IllegalArgumentException("Item may not be transfer request")
}

@Composable
fun RecentActivityShimmerRow(shimmer: Shimmer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clip(CircleShape)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .grayShimmer(shimmer)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(200.dp, 14.dp)
                    .grayShimmer(shimmer)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(120.dp, 11.dp)
                    .grayShimmer(shimmer)
            )
        }
    }
}