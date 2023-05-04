package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import ro.bankar.app.R
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType

@Composable
fun BankAccount(data: SBankAccount) {
    HomeCard(
        onClick = {}, // TODO
        title = data.name,
        icon = { Icon(painter = painterResource(R.drawable.bank_account), contentDescription = stringResource(R.string.bank_account)) },
        color = data.color.takeIf { it != 0 }?.let { Color(it) }
    ) {
        Amount(amount = data.balance, currency = data.currency, textStyle = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(12.dp))
        Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.fillMaxWidth(),
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
    type = SBankAccountType.DEBIT,
    balance = 1235.22,
    limit = 0.0,
    currency = "RON",
    name = "Debit Account",
    color = 0,
    interest = 0.0,
    interestDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
)

@Preview
@Composable
private fun BankAccountPreview() {
    AppTheme {
        BankAccount(sampleAccount)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BankAccountPreviewDark() {
    AppTheme {
        BankAccount(sampleAccount)
    }
}

@Preview
@Composable
fun BankAccountShimmerPreview() {
    AppTheme {
        BankAccountShimmer(rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}