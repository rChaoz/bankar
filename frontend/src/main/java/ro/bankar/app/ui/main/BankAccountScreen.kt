package ro.bankar.app.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.PagerTabs
import ro.bankar.app.ui.components.testAccount
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.main.home.topBorder
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.model.SBankAccount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BankAccountScreen(onDismiss: () -> Unit, data: SBankAccount, navigation: NavHostController) {
    val activity by LocalRepository.current.account(data.id).also { it.requestEmit() }.collectAsStateRetrying()

    NavScreen(onDismiss, title = R.string.bank_account) {
        Column {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .topBorder(8.dp, accountColors[data.color.coerceIn(accountColors.indices)])
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = data.name, style = MaterialTheme.typography.headlineSmall)
                        Amount(amount = data.balance, currency = data.currency, textStyle = MaterialTheme.typography.titleLarge)
                    }
                    Icon(painter = painterResource(R.drawable.bank_account), contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            PagerTabs(tabs = listOf(R.string.history, R.string.actions)) {
                if (it == 0) LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (activity == null) RecentActivityShimmer()
                    else {
                        val (_, transfers, transactions) = activity!!
                        RecentActivityContent(transfers, transactions, onNavigateToTransfer = {
                            navigation.navigate(MainNav.Transfer(it))
                        }, onNavigateToTransaction = {
                            navigation.navigate(MainNav.Transaction(it))
                        })
                    }
                } else Box(modifier = Modifier.fillMaxSize()) {
                    Text(text = "todo")
                }
            }
        }
    }
}

@Preview
@Composable
private fun BankAccountScreenPreview() {
    AppTheme {
        BankAccountScreen(onDismiss = {}, data = testAccount, rememberMockNavController())
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BankAccountScreenPreviewDark() {
    AppTheme {
        BankAccountScreen(onDismiss = {}, data = testAccount, rememberMockNavController())
    }
}