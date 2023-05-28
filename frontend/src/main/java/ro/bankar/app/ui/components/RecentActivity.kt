package ro.bankar.app.ui.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ro.bankar.app.R
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SDirection
import ro.bankar.util.format
import ro.bankar.util.here

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
                painter = painterResource(if (data.direction != null) R.drawable.transfer else R.drawable.self_transfer),
                contentDescription = stringResource(R.string.transfer),
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        title = when {
            data.direction == SDirection.Received -> stringResource(R.string.from_s, data.fullName)
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
                    Amount(amount = data.exchangedAmount!!, currency = data.exchangedCurrency, withPlusSign = true, textStyle = MaterialTheme.typography.titleSmall)
                }
        }
    }
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