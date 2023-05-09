package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ro.bankar.app.R
import ro.bankar.app.ui.theme.customColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptDeclineButtons(onAccept: () -> Unit, onDecline: () -> Unit) {
    Row {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.customColors.green,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                onClick = onAccept,
                shape = RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50),
            ) {
                Icon(
                    modifier = Modifier.padding(start = 2.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.accept)
                )
            }
            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.customColors.red,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
                onClick = onDecline,
                shape = RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
            ) {
                Icon(
                    modifier = Modifier.padding(end = 2.dp),
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.decline)
                )
            }
        }
    }
}