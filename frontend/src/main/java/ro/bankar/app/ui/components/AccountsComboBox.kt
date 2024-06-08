package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.R
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType

private val noneOptionAccount = SBankAccount(
    -1, "", SBankAccountType.Debit, 0.0, 0.0, Currency.ROMANIAN_LEU, "", 0, 0.0
)

@Composable
fun AccountsComboBox(
    selectedAccount: MutableState<SBankAccount?>,
    accounts: List<SBankAccount>?,
    modifier: Modifier = Modifier,
    pickText: Int? = null,
    showBalance: Boolean = false,
    noneOptionText: Int? = null,
    onPickAccount: (SBankAccount?) -> Unit = {},
) {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val textMod = if (accounts == null) Modifier.shimmer(shimmer) else Modifier
        if (pickText != null) Text(text = stringResource(pickText), modifier = textMod)

        if (accounts == null) Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .grayShimmer(shimmer)
        )
        else ComboBox(
            selectedItemText = selectedAccount.value?.name ?: stringResource(
                if (accounts.isEmpty()) R.string.no_accounts_found else (noneOptionText ?: R.string.select_an_account)
            ),
            onSelectItem = { item -> item.takeIf { it !== noneOptionAccount }.let { selectedAccount.value = it; onPickAccount(it) } },
            items = if (noneOptionText == null) accounts else listOf(noneOptionAccount) + accounts,
            fillWidth = true,
            enabled = accounts.isNotEmpty(),
        ) { item, onClick ->
            DropdownMenuItem(text = {
                if (item === noneOptionAccount) Text(text = stringResource(noneOptionText!!))
                else AccountCard(account = item)
            }, onClick)
        }

        if (showBalance) {
            val account = selectedAccount.value
            if (accounts == null) Box(modifier = Modifier
                .size(100.dp, 18.dp)
                .grayShimmer(shimmer))
            else if (account != null) Row {
                Text(text = stringResource(if (account.type == SBankAccountType.Credit) R.string.available_credit else R.string.current_balance))
                Text(text = account.currency.format(account.spendable), color = MaterialTheme.customColors.green, fontWeight = FontWeight.Medium)
            }
        }
    }
}