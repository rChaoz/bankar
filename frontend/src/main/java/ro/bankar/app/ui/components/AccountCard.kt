package ro.bankar.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.format
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType

@Composable
fun AccountCard(account: SBankAccount, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(start = 3.dp)
            .startBorder(3.dp, accountColors[account.color.coerceIn(accountColors.indices)])
            .padding(start = 8.dp)
    ) {
        Text(text = account.name, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = account.currency.format(account.balance),
            color = account.balance.amountColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

val testAccount = SBankAccount(1, "RO873240982734893", SBankAccountType.Debit, 125.62, 0.0,
    Currency.ROMANIAN_LEU, "My Debit Account", 2, 0.0)

@Preview
@Composable
private fun AccountCardPreview() {
    AppTheme {
        Surface(shape = MaterialTheme.shapes.small) {
            AccountCard(testAccount, modifier = Modifier)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountCardPreviewDark() {
    AppTheme {
        Surface(shape = MaterialTheme.shapes.small) {
            AccountCard(testAccount, modifier = Modifier.padding(12.dp))
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