package ro.bankar.app.ui.main

import android.content.res.Configuration
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
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.data.mapCollectAsStateRetrying
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.format
import ro.bankar.app.ui.main.friend.FriendCard
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SDirection
import ro.bankar.model.SPublicUser
import ro.bankar.model.SPublicUserBase
import ro.bankar.util.here
import ro.bankar.util.nowUTC

@Composable
fun ExternalTransferDetailsScreen(
    onDismiss: () -> Unit,
    data: SBankTransfer,
    onNavigateToFriend: (SPublicUserBase) -> Unit,
    onNavigateToAccount: (SBankAccount) -> Unit,
) {
    val countryData by LocalRepository.current.countryData.collectAsStateRetrying()
    val bankAccount by LocalRepository.current.accounts.mapCollectAsStateRetrying { accounts -> accounts.find { it.id == data.accountID } }

    NavScreen(onDismiss, title = R.string.transfer) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Amount(
                            amount = if (data.direction == SDirection.Received) data.relevantAmount else -data.relevantAmount,
                            currency = data.relevantCurrency,
                            withPlusSign = true,
                            textStyle = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = data.dateTime.toInstant(TimeZone.UTC).here().format(true),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    FilledIconButton(onClick = { /*TODO*/ }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(R.drawable.split_bill),
                            contentDescription = stringResource(R.string.split_bill)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(if (data.direction == SDirection.Sent) R.string.to else R.string.from), fontWeight = FontWeight.Bold)
                if (data.user != null && data.user!!.isFriend) Surface(
                    onClick = { onNavigateToFriend(data.user!!) },
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FriendCard(friend = data.user!!, country = countryData.nameFromCode(data.user!!.countryCode), modifier = Modifier.padding(12.dp))
                }
                else Surface(shape = MaterialTheme.shapes.small, tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    if (data.user != null) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FriendCard(friend = data.user!!, country = countryData.nameFromCode(data.user!!.countryCode), modifier = Modifier.padding(12.dp))
                        Text(text = stringResource(R.string.not_a_friend), color = MaterialTheme.colorScheme.outline, fontStyle = FontStyle.Italic)
                    }
                    else Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = data.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.not_user), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.iban), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = data.iban)
                    }
                }
            }
            Divider()
            if (data.direction == SDirection.Received && data.exchangedAmount != null) {
                Column(modifier = Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(text = stringResource(R.string.conversion_rate))
                        Text(
                            text = "%s = %s".format(
                                data.relevantCurrency.format(1.0),
                                data.currency.format(data.amount / data.exchangedAmount!!)
                            ), fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        Text(text = stringResource(R.string.received_amount))
                        Text(
                            text = data.currency.format(data.amount),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Divider()
            }
            TransferAccountCard(text = R.string.bank_account, bankAccount, onNavigateToAccount)

            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.message), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (data.note.isNotEmpty()) Text(text = data.note)
                    else Text(text = stringResource(R.string.no_message), fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Preview
@Composable
private fun ExternalTransferDetailsScreenPreview() {
    AppTheme {
        ExternalTransferDetailsScreen(
            onDismiss = {}, data = SBankTransfer(
                SDirection.Sent, 1, null, null, "Koleci Alexandru", "RO7832479823420", 25.0, 122.3,
                Currency.EURO, Currency.ROMANIAN_LEU, "bing chilling, take this", Clock.System.nowUTC()
            ), onNavigateToFriend = {}, onNavigateToAccount = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExternalTransferDetailsScreenPreviewDark() {
    AppTheme {
        ExternalTransferDetailsScreen(
            onDismiss = {}, data = SBankTransfer(
                SDirection.Received, 1, null, SPublicUser(
                    "toaster", "Big", null, "Toaster", "RO",
                    Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 5), "toastin' around", null, true
                ), "Big Toaster", "RO7832479823420", 25.0, 122.3,
                Currency.EURO, Currency.ROMANIAN_LEU, "bing chilling, take this", Clock.System.nowUTC()
            ), onNavigateToFriend = {}, onNavigateToAccount = {}
        )
    }
}