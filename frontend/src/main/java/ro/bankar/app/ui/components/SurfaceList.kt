package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurfaceList(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            content()
        }
    }
}
