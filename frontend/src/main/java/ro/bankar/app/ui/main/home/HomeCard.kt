package ro.bankar.app.ui.main.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.format
import ro.bankar.banking.Currency

@Composable
fun HomeCard(
    title: String,
    icon: @Composable () -> Unit,
    shimmer: Shimmer? = null,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = .5.dp,
        shadowElevation = 3.dp
    ) {
        HomeCardContent(title, icon, shimmer, color, 5.dp, content)
    }
}

@Composable
fun HomeCard(
    onClick: () -> Unit,
    title: String,
    icon: @Composable () -> Unit,
    shimmer: Shimmer? = null,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = .5.dp,
        shadowElevation = 3.dp
    ) {
        HomeCardContent(title, icon, shimmer, color, 8.dp, content)
    }
}

@Composable
private fun HomeCardContent(
    title: String,
    icon: @Composable () -> Unit,
    shimmer: Shimmer? = null,
    color: Color? = null,
    borderThickness: Dp = 5.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.topBorder(borderThickness, color ?: MaterialTheme.colorScheme.inversePrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val mod = Modifier.weight(1f, fill = false)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = mod.run { if (shimmer != null) shimmer(shimmer) else this }
            )
            Box(modifier = Modifier.padding(4.dp)) {
                icon()
            }
        }
        content()
    }
}

private fun Modifier.topBorder(thickness: Dp, color: Color) = composed {
    with(LocalDensity.current) {
        val t = thickness.toPx()
        border(thickness, color, GenericShape { (width, _), _ ->
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, t)
            lineTo(0f, t)
            close()
        })
    }
}

@Composable
fun Amount(
    amount: Double,
    currency: Currency,
    modifier: Modifier = Modifier,
    color: Color = amount.amountColor,
    withPlusSign: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    fontWeight: FontWeight = FontWeight.Medium,
    shimmer: Shimmer? = null,
) {
    Text(
        text = currency.format(amount, withPlusSign),
        modifier = modifier.let { if (shimmer != null) it.shimmer(shimmer) else it },
        style = textStyle,
        fontWeight = fontWeight,
        color = color
    )
}