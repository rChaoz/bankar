package ro.bankar.app.ui.components

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import ro.bankar.app.R

@Composable
fun ThemeToggle(
    isDarkMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledIconButton(onToggle, modifier, enabled) {
        Icon(
            painter = painterResource(if (isDarkMode) R.drawable.baseline_dark_mode_24 else R.drawable.baseline_light_mode_24),
            contentDescription = if (isDarkMode) "Dark mode" else "Light mode"
        )
    }
}