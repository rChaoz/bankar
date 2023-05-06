package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ButtonRow(
    currentValue: T,
    onValueChange: (T) -> Unit,
    values: List<T>,
    modifier: Modifier = Modifier,
    buttonContentGenerator: @Composable (T) -> Unit = { Text(text = it.toString()) }
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Row(modifier) {
            if (values.isNotEmpty()) {
                ButtonRowButton(values.first(), currentValue, onValueChange, Alignment.Start, buttonContentGenerator)
                for (i in 1 until values.lastIndex)
                    ButtonRowButton(values[i], currentValue, onValueChange, Alignment.CenterHorizontally, buttonContentGenerator)
                ButtonRowButton(values.last(), currentValue, onValueChange, Alignment.End, buttonContentGenerator)
            }
        }
    }
}

@Composable
private fun <T> RowScope.ButtonRowButton(
    value: T,
    currentValue: T,
    onValueChange: (T) -> Unit,
    alignment: Alignment.Horizontal,
    buttonContentGenerator: @Composable (T) -> Unit
) {
    OutlinedButton(
        onClick = { onValueChange(value) },
        modifier = Modifier.weight(1f),
        shape = when (alignment) {
            Alignment.Start -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
            Alignment.CenterHorizontally -> RectangleShape
            else -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
        },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (value == currentValue) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
        )
    ) {
        buttonContentGenerator(value)
    }
}