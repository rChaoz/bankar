package ro.bankar.app.ui.main

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ro.bankar.app.R
import ro.bankar.app.ui.components.Avatar

private val ribbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(0f, size.height)
    lineTo(size.width / 2, size.height - size.width / 5)
    lineTo(size.width, size.height)
    lineTo(size.width, 0f)
    close()
}

@Composable
fun ProfileRibbon(image: ByteArray?, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick,
        modifier = modifier,
        shape = ribbonShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp, 120.dp)
                .padding(top = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Avatar(
                image = image,
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentDescription = R.string.profile,
                size = 65.dp,
                nullIsLoading = true
            )
        }
    }
}