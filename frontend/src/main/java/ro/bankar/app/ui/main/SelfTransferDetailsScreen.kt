package ro.bankar.app.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.datetime.Clock
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.components.AccountCard
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.mapCollectAsState
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankTransfer
import ro.bankar.util.format
import ro.bankar.util.here

@Composable
fun SelfTransferDetailsScreen(onDismiss: () -> Unit, data: SBankTransfer, onNavigateToAccount: (SBankAccount) -> Unit) {
    val sourceAccount by LocalRepository.current.accounts.mapCollectAsState(null) { accounts -> accounts.find { it.id == data.sourceAccountID } }
    val destinationAccount by LocalRepository.current.accounts.mapCollectAsState(null) { accounts -> accounts.find { it.id == data.accountID } }

    NavScreen(onDismiss, title = if (data.exchangedAmount == null) R.string.self_transfer else R.string.exchange) {
        Column(modifier = Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
                if (data.exchangedAmount != null) Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Amount(
                            amount = -data.amount,
                            currency = data.currency,
                            textStyle = MaterialTheme.typography.headlineSmall,
                        )
                        Amount(
                            amount = data.exchangedAmount!!,
                            currency = data.exchangedCurrency,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            withPlusSign = true
                        )
                    }
                else Amount(
                    amount = data.amount,
                    currency = data.currency,
                    textStyle = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = data.timestamp.here().format(true),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (data.exchangedAmount != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Text(text = stringResource(R.string.conversion_rate))
                        Text(
                            text = "%s = %s".format(
                                data.relevantCurrency.format(1.0),
                                data.currency.format(data.amount / data.exchangedAmount!!)
                            ), fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            HorizontalDivider()
            TransferAccountCard(text = R.string.from_account, sourceAccount, onNavigateToAccount)
            TransferAccountCard(text = R.string.to_account, destinationAccount, onNavigateToAccount)

            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (data.note.isNotEmpty()) Text(text = data.note)
                    else Text(text = stringResource(R.string.no_note), fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
fun TransferAccountCard(text: Int, account: SBankAccount?, onNavigateToAccount: (SBankAccount) -> Unit) {
    Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(text), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            account?.let {
                Surface(
                    onClick = { onNavigateToAccount(it) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(.8f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    AccountCard(account = it, modifier = Modifier.padding(8.dp))
                }
            } ?: Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(.8f)
                    .height(50.dp)
                    .grayShimmer(rememberShimmer(shimmerBounds = ShimmerBounds.View))
            )
        }
    }
}

@Preview
@Composable
private fun SelfTransferDetailsScreenPreview() {
    AppTheme {
        SelfTransferDetailsScreen(onDismiss = {}, data = SBankTransfer(
            null, 1, 2, null, "Full Name", "hehe_iban", null, 100.0, null,
            Currency.ROMANIAN_LEU, Currency.ROMANIAN_LEU, "self transfer lore", Clock.System.now()
        ), onNavigateToAccount = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelfTransferDetailsScreenPreviewDark() {
    AppTheme {
        SelfTransferDetailsScreen(onDismiss = {}, data = SBankTransfer(
            null, 1, 2, null, "Full Name", "hehe_iban", null, 100.0, 19.45,
            Currency.ROMANIAN_LEU, Currency.EURO, "self transfer lore", Clock.System.now()
        ), onNavigateToAccount = {})
    }
}