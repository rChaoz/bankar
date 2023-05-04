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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ro.bankar.app.R
import ro.bankar.app.ui.components.FilledIcon
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors

@Composable
fun RecentActivity() {
    HomeCard(
        title = stringResource(R.string.recent_activity),
        icon = { Icon(painter = painterResource(R.drawable.baseline_recent_24), contentDescription = null) }) {
        PartyInvite(fromName = "Andi Koleci", time = LocalTime(16, 20), place = "Tesla Dealer")
        Payment(title = "MUULT Sushi :3", date = Clock.System.todayIn(TimeZone.currentSystemDefault()), amount = 23.2354, currency = "RON")
        Transfer(name = "Hehe", dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), amount = 25.215, currency = "EUR")
        Transfer(name = "Hihi", dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()), amount = -15.0, currency = "USD")
        TextButton(onClick = { /*TODO*/ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = stringResource(R.string.see_more))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentActivityPreview() {
    AppTheme {
        RecentActivity()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecentActivityPreviewDark() {
    AppTheme {
        RecentActivity()
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
            Box(modifier = Modifier
                .size(28.dp)
                .grayShimmer(shimmer))
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
private fun PartyInvite(fromName: String, time: LocalTime, place: String) {
    RecentActivityRow(icon = {
        FilledIcon(
            painter = painterResource(id = R.drawable.share_bill),
            contentDescription = null,
            color = MaterialTheme.colorScheme.secondary,
        )
    }, title = stringResource(R.string.party_invite_from).format(fromName), subtitle = "${time.hour}:${time.minute} • $place") {
        Row {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.customColors.green,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                    onClick = {},
                    shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
                ) {
                    Icon(
                        modifier = Modifier.padding(start = 2.dp),
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.accept)
                    )
                }
                FilledIconButton(
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.customColors.red,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                    onClick = {},
                    shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 2.dp),
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.decline)
                    )
                }
            }
        }
    }
}

@Composable
private fun Payment(title: String, date: LocalDate, amount: Double, currency: String) {
    RecentActivityRow(icon = {
        FilledIcon(
            painter = painterResource(R.drawable.payment),
            contentDescription = stringResource(R.string.payment),
            color = MaterialTheme.colorScheme.secondary,
        )
    }, title = title, subtitle = "${date.dayOfMonth}.${date.monthNumber}.${date.year}") {
        Amount(-amount, currency)
    }
}

@Composable
private fun Transfer(name: String, dateTime: LocalDateTime, amount: Double, currency: String) {
    RecentActivityRow(
        icon = {
            FilledIcon(
                painter = painterResource(R.drawable.baseline_transfer_24),
                contentDescription = stringResource(R.string.transfer),
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        title = stringResource(if (amount > 0) R.string.from_s else R.string.to_s).format(name),
        subtitle = with(dateTime) { "$hour:$minute • $dayOfMonth.$monthNumber.$year" }
    ) {
        Amount(amount, currency, withPlusSign = true)
    }
}