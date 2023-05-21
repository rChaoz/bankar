package ro.bankar.app.ui.main.home

import android.content.res.Configuration
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.FilledIcon
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackBar
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.model.SDirection
import ro.bankar.model.SRecentActivity
import ro.bankar.util.here
import kotlin.math.abs

@Composable
fun RecentActivity(recentActivity: SRecentActivity) {
    HomeCard(
        title = stringResource(R.string.recent_activity),
        icon = { Icon(painter = painterResource(R.drawable.baseline_recent_24), contentDescription = null) }) {
        if (recentActivity.isEmpty()) InfoCard(text = R.string.no_recent_activity, tonalElevation = 0.dp)
        else {
            val (partyInvites, otherRequests) = recentActivity.transferRequests.partition { it.partyID != null }
            partyInvites.forEach {
                PartyInvite(
                    fromName = "${it.firstName} ${it.lastName}",
                    amount = it.amount,
                    currency = it.currency,
                    place = it.note
                )
            }
            otherRequests.forEach {
                if (it.direction == SDirection.Sent)
                    SentTransferRequest(
                        id = it.id,
                        fromName = "${it.firstName} ${it.lastName}",
                        amount = it.amount,
                        currency = it.currency
                    )
                else
                    ReceivedTransferRequest(
                        fromName = "${it.firstName} ${it.lastName}",
                        amount = it.amount,
                        currency = it.currency
                    )
            }

            // Merge display transfers and transactions
            val (transfers, transactions) = recentActivity
            var transferI = 0
            var transactionI = 0
            // Limit total number of entries to 3
            while (transferI + transactionI < 3 && (transferI < transfers.size || transactionI < transactions.size)) {
                if (transferI < transfers.size && (transactionI < transactions.size || transfers[transferI].dateTime < transactions[transactionI].dateTime)) {
                    val transfer = transfers[transferI++]
                    val amount = if (transfer.direction == SDirection.Sent) -transfer.amount else transfer.amount
                    Transfer(name = transfer.fullName, time = transfer.dateTime.toInstant(TimeZone.UTC), amount = amount, currency = transfer.currency)
                } else {
                    val transaction = transactions[transactionI++]
                    Payment(
                        title = transaction.title, time = transaction.dateTime.toInstant(TimeZone.UTC),
                        amount = transaction.amount, currency = transaction.currency
                    )
                }
            }
            TextButton(
                onClick = { /*TODO*/ },
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
            RecentActivity(it)
        } ?: RecentActivityShimmer(shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecentActivityPreviewDark() {
    AppTheme {
        RecentActivity(SRecentActivity(emptyList(), emptyList(), emptyList()))
    }
}

@Composable
fun RecentActivityShimmer(shimmer: Shimmer) {
    HomeCard(title = stringResource(R.string.recent_activity), shimmer = shimmer, icon = {
        Icon(painter = painterResource(R.drawable.baseline_recent_24), contentDescription = null, modifier = Modifier.shimmer(shimmer))
    }) {
        repeat(3) { ShimmerRow(shimmer) }
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
fun RecentActivityShimmerPreview() {
    AppTheme {
        RecentActivityShimmer(shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}

@Composable
private fun ShimmerRow(shimmer: Shimmer) {
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

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun RecentActivityRow(
    noinline icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    elevated: Boolean = false,
    noinline trailingContent: @Composable () -> Unit
) {
    RecentActivityRow(icon, title, AnnotatedString(subtitle), elevated, trailingContent)
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun RecentActivityRow(
    noinline onClick: () -> Unit,
    noinline icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    elevated: Boolean = false,
    noinline trailingContent: @Composable () -> Unit
) {
    RecentActivityRow(onClick, icon, title, AnnotatedString(subtitle), elevated, trailingContent)
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
private fun RecentActivityRow(
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
private fun RecentActivityRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: AnnotatedString,
    elevated: Boolean = false,
    trailingContent: @Composable () -> Unit
) {
    Surface(tonalElevation = if (elevated) 8.dp else 0.dp) {
        RecentActivityRowBase(icon, title, subtitle, trailingContent)
    }
}

@Composable
private fun PartyInvite(fromName: String, amount: Double, currency: Currency, place: String) {
    RecentActivityRow(elevated = true, icon = {
        FilledIcon(
            painter = painterResource(id = R.drawable.share_bill),
            contentDescription = null,
            color = MaterialTheme.colorScheme.primary,
        )
    }, title = stringResource(R.string.party_invite_from, fromName), subtitle = buildAnnotatedString {
        pushStyle(SpanStyle(color = MaterialTheme.customColors.red))
        append(currency.format(amount))
        pop()
        append(" • ", place)
    }) {
        AcceptDeclineButtons(onAccept = {}, onDecline = {})
    }
}

@Composable
private fun SentTransferRequest(id: Int, fromName: String, amount: Double, currency: Currency) {
    var isLoading by remember { mutableStateOf(false) }

    RecentActivityRow(elevated = true, icon = {
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

        if (isLoading) CircularProgressIndicator()
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
private fun ReceivedTransferRequest(fromName: String, amount: Double, currency: Currency) {
    RecentActivityRow(elevated = true, icon = {
        FilledIcon(
            painter = painterResource(R.drawable.transfer_request),
            contentDescription = stringResource(R.string.transfer_request),
            color = MaterialTheme.colorScheme.tertiary,
        )
    }, title = stringResource(R.string.from_s, fromName), subtitle = buildAnnotatedString {
        append(stringResource(if (amount > 0) R.string.has_sent else R.string.has_requested))
        pushStyle(SpanStyle(color = amount.amountColor))
        append(currency.format(abs(amount)))
    }) {
        AcceptDeclineButtons(onAccept = {}, onDecline = {})
    }
}

@Composable
private fun Payment(title: String, time: Instant, amount: Double, currency: Currency) {
    RecentActivityRow(onClick = {}, icon = {
        FilledIcon(
            painter = painterResource(R.drawable.payment),
            contentDescription = stringResource(R.string.payment),
            color = MaterialTheme.colorScheme.secondary,
        )
    }, title = title, subtitle = time.here().format()) {
        Amount(-amount, currency)
    }
}

@Composable
private fun Transfer(name: String, time: Instant, amount: Double, currency: Currency) {
    RecentActivityRow(
        onClick = {},
        icon = {
            FilledIcon(
                painter = painterResource(R.drawable.transfer),
                contentDescription = stringResource(R.string.transfer),
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        title = stringResource(if (amount > 0) R.string.from_s else R.string.to_s, name),
        subtitle = time.here().format()
    ) {
        Amount(amount, currency, withPlusSign = true)
    }
}