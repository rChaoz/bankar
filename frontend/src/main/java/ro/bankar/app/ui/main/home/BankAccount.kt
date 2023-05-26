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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.R
import ro.bankar.app.ui.format
import ro.bankar.app.ui.rString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType

@Composable
fun BankAccount(data: SBankAccount, onNavigate: () -> Unit) {
    HomeCard(
        onClick = onNavigate,
        title = data.name,
        icon = { Icon(painter = painterResource(R.drawable.bank_account), contentDescription = stringResource(R.string.bank_account)) },
        color = accountColors[data.color.coerceIn(accountColors.indices)]
    ) {
        Text(
            text = stringResource(data.type.rString),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)) {
            Amount(amount = data.balance, currency = data.currency, textStyle = MaterialTheme.typography.headlineMedium)
            if (data.type == SBankAccountType.Credit) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.limit_is), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    Text(text = data.currency.format(data.limit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.customColors.red)
                }
            }
        }
        Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = stringResource(R.string.new_payment))
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = stringResource(androidx.compose.ui.R.string.dropdown_menu))
            }
        }
    }
}

@Composable
fun BankAccountShimmer(shimmer: Shimmer) {
    HomeCard(
        title = stringResource(R.string.bank_account),
        icon = {
            Icon(
                painter = painterResource(R.drawable.bank_account),
                contentDescription = stringResource(R.string.bank_account),
                modifier = Modifier.shimmer(shimmer)
            )
        },
        shimmer = shimmer
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(200.dp, 32.dp)
                .shimmer(shimmer)
                .background(MaterialTheme.customColors.green.copy(alpha = .6f))
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

private val sampleAccount = SBankAccount(
    id = 0,
    iban = "RO24RBNK1921081333473500",
    type = SBankAccountType.Debit,
    balance = 1235.22,
    limit = 0.0,
    currency = Currency.ROMANIAN_LEU,
    name = "Debit Account",
    color = 0,
    interest = 0.0,
)

@Preview
@Composable
private fun BankAccountPreview() {
    AppTheme {
        BankAccount(sampleAccount, onNavigate = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BankAccountPreviewDark() {
    AppTheme {
        BankAccount(sampleAccount, onNavigate = {})
    }
}

@Preview
@Composable
fun BankAccountShimmerPreview() {
    AppTheme {
        BankAccountShimmer(rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}