package ro.bankar.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import ro.bankar.app.R
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.processNumberValue
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType

@Composable
fun AccountAmountInput(
    modifier: Modifier = Modifier,
    account: SBankAccount?,
    amount: MutableState<String>,
    showRemainingBalance: Boolean,
    shimmer: Shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window),
    currency: Currency? = account?.currency,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (currency != null) {
            // Centered text field
            BasicTextField(
                value = amount.value,
                onValueChange = { value -> processNumberValue(value)?.let { amount.value = it } },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            ) { field ->
                SubcomposeLayout { constraints ->
                    val textWidth = subcompose("text") {
                        Text(text = amount.value, style = MaterialTheme.typography.headlineLarge)
                    }[0].measure(constraints.copy(minWidth = 0)).width
                    val content = subcompose("textField", field)[0].measure(constraints)

                    val pad = 20.dp.roundToPx()
                    val outline = subcompose("outline") {
                        Box(
                            modifier = Modifier
                                .alpha(.5f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        )
                    }[0].measure(Constraints.fixed(textWidth + pad, content.height + pad))
                    layout(constraints.maxWidth, content.height + pad) {
                        content.place(constraints.maxWidth / 2 - textWidth / 2, pad / 2)
                        outline.place(constraints.maxWidth / 2 - textWidth / 2 - pad / 2, 0)
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = currency.code, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp, 50.dp)
                    .grayShimmer(shimmer)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(120.dp, 40.dp)
                    .grayShimmer(shimmer)
            )
        }

        if (account != null && showRemainingBalance) {
            Text(
                text = stringResource(if (account.type == SBankAccountType.Credit) R.string.remaining_credit else R.string.remaining_balance),
                style = MaterialTheme.typography.labelMedium
            )
            amount.value.ifEmpty { "0" }.toDoubleOrNull()?.let {
                val remaining = account.spendable - it
                Text(text = account.currency.format(remaining), style = MaterialTheme.typography.labelMedium, color = remaining.amountColor)
            } ?: Text(text = stringResource(R.string.error), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.customColors.red)
        }
    }
}