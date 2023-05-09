package ro.bankar.app.ui.main.home

import android.content.res.Configuration
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ro.bankar.app.R
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.FilledIcon
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SRecentActivity
import ro.bankar.model.STransferRequest
import ro.bankar.model.TransferDirection
import ro.bankar.util.here
import ro.bankar.util.nowHere
import ro.bankar.util.nowUTC

@Composable
fun RecentActivity(recentActivity: SRecentActivity) {
    HomeCard(
        title = stringResource(R.string.recent_activity),
        icon = { Icon(painter = painterResource(R.drawable.baseline_recent_24), contentDescription = null) }) {
        if (recentActivity.isEmpty()) InfoCard(text = R.string.no_recent_activity, tonalElevation = 0.dp)
        else {
            val (partyInvites, otherRequests) = recentActivity.transferRequests.partition { it.partyID != null }
            partyInvites.forEach {
                PartyInvite(fromName = "${it.firstName} ${it.lastName}", time = it.dateTime.toInstant(TimeZone.UTC), place = it.note)
            }
            @Suppress("ForEachParameterNotUsed")
            otherRequests.forEach {
                // TODO Add transfer request component
            }

            // Merge display transfers and transactions
            val (transfers, transactions) = recentActivity
            var transferI = 0
            var transactionI = 0
            while (transferI < transfers.size && transactionI < transactions.size) {
                if (transfers[transferI].dateTime < transactions[transactionI].dateTime) {
                    val transfer = transfers[transferI++]
                    val amount = if (transfer.direction == TransferDirection.Sent) -transfer.amount else transfer.amount
                    Transfer(name = transfer.fullName, time = transfer.dateTime.toInstant(TimeZone.UTC), amount = amount, currency = transfer.currency.code)
                } else {
                    val transaction = transactions[transactionI++]
                    Payment(title = transaction.title, time = transaction.dateTime.toInstant(TimeZone.UTC),
                        amount = transaction.amount, currency = transaction.currency.code)
                }
            }
            TextButton(onClick = { /*TODO*/ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = stringResource(R.string.see_more))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentActivityPreview() {
    AppTheme {
        RecentActivity(
            SRecentActivity(
                listOf(
                    SBankTransfer(TransferDirection.Received, "Koleci 1", "testIBAN",
                        25.215, Currency.EURO, "middd", Clock.System.nowHere()),
                    SBankTransfer(TransferDirection.Sent, "Koleci 2", "testIBAN!!",
                        15.0, Currency.US_DOLLAR, ":/", Clock.System.nowHere()),
                ),
                listOf(SCardTransaction(1L, 2, "1373",
                    23.2354, Currency.ROMANIAN_LEU, Clock.System.nowUTC(), "Sushi Terra", "1234 idk")),
                listOf(
                    STransferRequest(
                        TransferDirection.Received, "Big", null, "Boy",
                        50.25, Currency.ROMANIAN_LEU, "Tesla Dealer", 5, Clock.System.nowUTC()
                    )
                ),
            )
        )
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
        Spacer(modifier = Modifier.height(6.dp))
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
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clip(CircleShape)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
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
                    .size(150.dp, 11.dp)
                    .grayShimmer(shimmer)
            )
        }
    }
}

@Composable
private fun RecentActivityRow(icon: @Composable () -> Unit, title: String, subtitle: String, trailingContent: @Composable () -> Unit) {
    Surface(onClick = {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailingContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartyInvite(fromName: String, time: Instant, place: String) {
    RecentActivityRow(icon = {
        FilledIcon(
            painter = painterResource(id = R.drawable.share_bill),
            contentDescription = null,
            color = MaterialTheme.colorScheme.secondary,
        )
    }, title = stringResource(R.string.party_invite_from, fromName), subtitle = stringResource(R.string.date_time_at_place, time.here().format(), place)) {
        AcceptDeclineButtons(onAccept = {}, onDecline = {})
    }
}

@Composable
private fun Payment(title: String, time: Instant, amount: Double, currency: String) {
    RecentActivityRow(icon = {
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
private fun Transfer(name: String, time: Instant, amount: Double, currency: String) {
    RecentActivityRow(
        icon = {
            FilledIcon(
                painter = painterResource(R.drawable.baseline_transfer_24),
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