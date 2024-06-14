package ro.bankar.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.components.CardCard
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SBankCard
import ro.bankar.model.SCardTransaction
import ro.bankar.util.format
import ro.bankar.util.here

@Composable
fun TransactionDetailsScreen(onDismiss: () -> Unit, data: SCardTransaction, onCreateParty: (Double, Int) -> Unit, onNavigateToCard: (Int, Int) -> Unit) {
    val repository = LocalRepository.current
    val deletedCard = SBankCard(0, stringResource(R.string.deleted_card), null, "••••", null, null, null, null, 0.0, 0.0, data.currency, emptyList())
    val card = remember {
        repository.account(data.accountID).map { acc -> acc.cards.find { it.id == data.cardID } ?: deletedCard }
    }.collectAsState(null).value

    NavScreen(onDismiss, title = R.string.transaction) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Amount(amount = -data.amount, currency = data.currency, withPlusSign = true, textStyle = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = data.timestamp.here().format(true),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = data.title, style = MaterialTheme.typography.titleLarge)
                }
                FilledIconButton(onClick = { onCreateParty(data.amount, data.accountID) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(R.drawable.split_bill),
                        contentDescription = stringResource(R.string.split_bill)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.paid_with), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    CardCard(
                        card,
                        onClick = if (card === deletedCard) null else ({ onNavigateToCard(data.accountID, data.cardID) }),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
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
        val repository = LocalRepository.current
        val data = remember { runBlocking { repository.account(1).first().cards[0].transactions[0] } }
        TransactionDetailsScreen(
            onDismiss = {}, data = data, onNavigateToCard = { _, _ -> }, onCreateParty = { _, _ -> }
        )
    }
}