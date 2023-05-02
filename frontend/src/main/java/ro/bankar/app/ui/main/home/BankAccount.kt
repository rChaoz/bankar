package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import ro.bankar.app.R
import ro.bankar.app.ui.theme.AppTheme

@Composable
fun BankAccount() {
    HomeCard(
        title = "Debit Account",
        icon = { Icon(painter = painterResource(R.drawable.bank_account), contentDescription = stringResource(R.string.bank_account)) }
    ) {
        Amount(amount = 1225.37f, currency = "RON", textStyle = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(12.dp))
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

@Preview
@Composable
private fun BankAccountPreview() {
    AppTheme {
        BankAccount()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BankAccountPreviewDark() {
    AppTheme {
        BankAccount()
    }
}