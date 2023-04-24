package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FilledIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    iconColor: Color = contentColorFor(color),
) {
    Surface(color = color, contentColor = iconColor, shape = CircleShape) {
        Box(modifier = Modifier.padding(4.dp)) {
            Icon(painter, contentDescription, modifier.size(size))
        }
    }
}