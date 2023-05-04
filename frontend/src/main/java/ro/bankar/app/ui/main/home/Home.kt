package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.LocalCustomColors
import ro.bankar.app.ui.theme.customColors

@Composable
fun Home() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RecentActivity()
        BankAccount()
        Assets()
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AppTheme {
        Home()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomePreviewDark() {
    AppTheme {
        Home()
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
fun HomeCard(title: String, icon: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = .5.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.topBorder(5.dp, MaterialTheme.colorScheme.inversePrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                icon()
            }
            content()
        }
    }
}

@Composable
fun Amount(
    amount: Float,
    currency: String,
    modifier: Modifier = Modifier,
    withPlusSign: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge
) {
    Text(
        text = "%${if (withPlusSign) "+.2f" else ".2f"} %s".format(amount, currency),
        modifier = modifier,
        style = textStyle,
        color = if (amount < 0) MaterialTheme.customColors.red else LocalCustomColors.current.green
    )
}