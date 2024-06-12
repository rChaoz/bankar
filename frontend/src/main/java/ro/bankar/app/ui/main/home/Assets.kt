package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import ro.bankar.app.R
import ro.bankar.app.data.KeyPreferredCurrency
import ro.bankar.app.data.LocalDataStore
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SExchangeData
import ro.bankar.banking.exchange
import ro.bankar.model.SBankAccount

@Composable
fun Assets(accounts: List<SBankAccount>, exchangeData: SExchangeData) {
    val data = LocalDataStore.current.data
    val preferredCurrencyIndex by remember { data.mapNotNull { it[KeyPreferredCurrency] } }.collectAsState(0)
    val preferredCurrency = Currency.entries.getOrElse(preferredCurrencyIndex) { Currency.EURO }

    val totalCash = remember(preferredCurrency, accounts) {
        accounts.sumOf { account ->
            if (account.currency == preferredCurrency) account.balance
            else exchangeData.exchange(account.currency, preferredCurrency, account.balance) ?: 0.0
        }
    }

    HomeCard(title = stringResource(R.string.assets), icon = {
        Amount(amount = .215 + totalCash, currency = preferredCurrency, textStyle = MaterialTheme.typography.headlineSmall)
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(.3f))
            AssetsColumn(icon = {
                Icon(painter = painterResource(R.drawable.cash), contentDescription = null, modifier = Modifier.size(32.dp))
            }, title = R.string.cash, amount = totalCash, currency = preferredCurrency, onClick = {}, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(.3f))
            AssetsColumn(icon = {
                Icon(painter = painterResource(R.drawable.stocks), contentDescription = null, modifier = Modifier.size(32.dp))
            }, title = R.string.stocks, amount = 0.0, currency = Currency.ROMANIAN_LEU, onClick = {}, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(.3f))
            AssetsColumn(icon = {
                Icon(painter = painterResource(R.drawable.crypto), contentDescription = null, modifier = Modifier.size(32.dp))
            }, title = R.string.crypto, amount = .215, currency = preferredCurrency, onClick = {}, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(.3f))
        }
    }
}

@Preview
@Composable
private fun AssetsPreview() {
    AppTheme {
        val repository = LocalRepository.current
        Assets(runBlocking { repository.accounts.first() }, runBlocking { repository.exchangeData.first() })
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssetsPreviewDark() {
    AppTheme {
        val repository = LocalRepository.current
        Assets(runBlocking { repository.accounts.first() }, runBlocking { repository.exchangeData.first() })
    }
}

@Composable
fun AssetsShimmer(shimmer: Shimmer) {
    HomeCard(title = stringResource(R.string.assets), shimmer = shimmer, icon = {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(180.dp, 32.dp)
                .shimmer(shimmer)
                .background(MaterialTheme.customColors.green.copy(alpha = .6f))
        )
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            repeat(3) {
                Spacer(modifier = Modifier.weight(.3f))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .grayShimmer(shimmer)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(.3f))
        }
    }
}

@Preview
@Composable
private fun AssetsShimmerPreview() {
    AppTheme {
        AssetsShimmer(shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}

@Composable
private fun AssetsColumn(icon: @Composable () -> Unit, title: Int, amount: Double, currency: Currency, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick, modifier, tonalElevation = 3.dp, shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Text(text = stringResource(title), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = currency.format(amount, separator = "\n"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.customColors.green,
                textAlign = TextAlign.Center
            )
        }
    }

}