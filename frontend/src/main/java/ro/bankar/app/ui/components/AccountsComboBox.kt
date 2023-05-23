package ro.bankar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.R
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType

@Composable
fun AccountsComboBox(
    selectedAccount: MutableState<SBankAccount?>,
    accounts: List<SBankAccount>?,
    pickText: Int? = null,
    showBalance: Boolean = false,
    showNoAccountsFound: Boolean = false,
) {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                if (showNoAccountsFound) R.string.no_accounts_found else R.string.select_an_account
            ),
            onSelectItem = { selectedAccount.value = it },
            items = accounts,
            fillWidth = true
        ) { item, onClick ->
            DropdownMenuItem(text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .startBorder(3.dp, accountColors[item.color.coerceIn(accountColors.indices)])
                        .padding(start = 8.dp)
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = item.currency.format(item.balance),
                        color = item.balance.amountColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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

private fun startBorderShape(thickness: Float) = GenericShape { size, layoutDirection ->
    if (layoutDirection == LayoutDirection.Ltr) {
        arcTo(Rect(Offset(-thickness / 2, thickness / 2), thickness), 0f, -180f, true)
        arcTo(Rect(Offset(-thickness / 2, size.height - thickness / 2), thickness), 180f, -180f, false)
    } else {
        moveTo(size.width, 0f)
        lineTo(size.width - thickness, 0f)
        lineTo(size.width - thickness, size.height)
        lineTo(size.width, size.height)
    }
    close()
}

private fun Modifier.startBorder(thickness: Dp, color: Color) = composed {
    background(color, startBorderShape(with(LocalDensity.current) { thickness.toPx() }))
}