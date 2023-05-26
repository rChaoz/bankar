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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.datetime.Month
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.RecentActivityShimmerRow
import ro.bankar.app.ui.components.Transaction
import ro.bankar.app.ui.components.Transfer
import ro.bankar.app.ui.grayShimmer
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import java.text.DateFormatSymbols

@Composable
fun RecentActivityScreen(onDismiss: () -> Unit, navigation: NavHostController) {
    val activity by LocalRepository.current.allRecentActivity.also { it.requestEmit() }.collectAsStateRetrying()

    NavScreen(onDismiss, title = R.string.recent_activity) {
        LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
            if (activity == null) RecentActivityShimmer()
            else {
                val (transfers, transactions) = activity!!
                RecentActivityContent(transfers, transactions, onNavigateToTransfer = {
                    navigation.navigate(MainNav.Transfer(it))
                }, onNavigateToTransaction = {
                    navigation.navigate(MainNav.Transaction(it))
                })
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
    onNavigateToTransfer: (SBankTransfer) -> Unit,
    onNavigateToTransaction: (SCardTransaction) -> Unit,
) {
    var transferI = 0
    var transactionI = 0

    val localizedMonths: Array<String> = DateFormatSymbols.getInstance().months
    var previousMonth: Month? = null
    while (transferI + transactionI < transfers.size + transactions.size) {
        if (transferI < transfers.size && (transactionI >= transactions.size || transfers[transferI].dateTime > transactions[transactionI].dateTime)) {
            val transfer = transfers[transferI++]
            if (transfer.dateTime.month != previousMonth) transfer.dateTime.date.let {
                previousMonth = it.month
                item("month-${it.monthNumber}-${it.year}") {
                    Text(
                        text = "%s %d".format(localizedMonths[it.monthNumber - 1], it.year),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
            }
            item(key = "transfer-$transferI") { Transfer(transfer, onNavigate = { onNavigateToTransfer(transfer) }) }
        } else {
            val transaction = transactions[transactionI++]
            if (transaction.dateTime.month != previousMonth) transaction.dateTime.date.let {
                previousMonth = it.month
                item("month-${it.monthNumber}-${it.year}") {
                    Text(
                        text = "%s %d".format(localizedMonths[it.monthNumber - 1], it.year),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
            }
            item(key = "transaction-$transactionI") { Transaction(transaction, onNavigate = { onNavigateToTransaction(transaction) }) }
        }
    }
}