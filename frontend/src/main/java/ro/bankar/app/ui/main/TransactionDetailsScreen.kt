package ro.bankar.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ro.bankar.app.R
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.format
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.SCardTransaction
import ro.bankar.util.here
import ro.bankar.util.nowUTC

@Composable
fun TransactionDetailsScreen(onDismiss: () -> Unit, data: SCardTransaction) {
    NavScreen(onDismiss, title = R.string.transaction) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Amount(amount = data.amount, currency = data.currency, withPlusSign = true, textStyle = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = data.dateTime.toInstant(TimeZone.UTC).here().format(true),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = data.title, style = MaterialTheme.typography.titleLarge)
                }
                FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(R.drawable.split_bill),
                        contentDescription = stringResource(R.string.split_bill)
                    )
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.paid_with), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(
                        onClick = {},
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_card_24),
                                contentDescription = stringResource(R.string.card),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(text = stringResource(R.string.card_last_4_template, data.cardLastFour), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = data.details)
                }
            }
            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.reference), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = data.reference.toString())
                }
            }
        }
    }
}

@Preview
@Composable
private fun TransactionDetailsScreenPreview() {
    AppTheme {
        TransactionDetailsScreen(
            onDismiss = {}, data = SCardTransaction(
                21837129371927L, 1, "4838", 25.67,
                Currency.ROMANIAN_LEU, Clock.System.nowUTC(), "TacoBell", "taco bell lore"
            )
        )
    }
}