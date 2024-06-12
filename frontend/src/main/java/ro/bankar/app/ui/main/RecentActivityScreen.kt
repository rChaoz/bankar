package ro.bankar.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.RecentActivityShimmerRow
import ro.bankar.app.ui.components.TimestampedItem
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SPartyPreview
import ro.bankar.util.here
import java.text.DateFormatSymbols

@Composable
fun RecentActivityScreen(onDismiss: () -> Unit, navigation: NavHostController) {
    val flow = LocalRepository.current.allRecentActivity
    val activity by flow.collectAsState(null)
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(true) {
        isRefreshing = true
        flow.requestEmitNow()
        isRefreshing = false
    }

    NavScreen(onDismiss, title = R.string.recent_activity) {
        @Suppress("DEPRECATION")
        SwipeRefresh(rememberSwipeRefreshState(isRefreshing), onRefresh = {
            scope.launch {
                isRefreshing = true
                flow.requestEmitNow()
                isRefreshing = false
            }
        }) {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                if (activity == null) RecentActivityShimmer()
                else {
                    val (transfers, transactions, parties) = activity!!
                    RecentActivityContent(transfers, transactions, parties, navigation)
                }
            }
        }
    }
}

@Suppress("FunctionName")
fun LazyListScope.RecentActivityShimmer() {
    item("shimmer") {
        val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
        Column(Modifier.padding(bottom = 12.dp)) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                    .size(150.dp, 14.dp)
                    .grayShimmer(shimmer)
            )
            repeat(5) { RecentActivityShimmerRow(shimmer) }
        }
    }
}

@Suppress("FunctionName")
fun LazyListScope.RecentActivityContent(
    transfers: List<SBankTransfer>,
    transactions: List<SCardTransaction>,
    parties: List<SPartyPreview>,
    navigation: NavHostController
) {
    val localizedMonths: Array<String> = DateFormatSymbols.getInstance().months
    val items = (transfers + transactions + parties).sortedDescending()
    items(items.size) { index ->
        val item = items[index]
        val dateTime = item.timestamp.here()
        Column {
            if (index == 0 || items[index - 1].timestamp.here().let { dateTime.year != it.year || dateTime.month != it.month }) Text(
                text = "%s %d".format(localizedMonths[dateTime.monthNumber - 1], dateTime.year),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
            )
            TimestampedItem(item, navigation)
        }
    }
}

@Preview
@Composable
private fun RecentActivityScreenPreview() {
    AppTheme {
        RecentActivityScreen(onDismiss = {}, navigation = rememberMockNavController())
    }
}