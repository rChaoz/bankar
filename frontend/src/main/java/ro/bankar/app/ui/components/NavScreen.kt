package ro.bankar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ro.bankar.app.R

@Composable
fun NavScreen(
    onDismiss: () -> Unit,
    title: Int,
    buttonIcon: (@Composable () -> Unit)? = null,
    onIconButtonClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    isFABVisible: Boolean = true,
    fabContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.displaySmall,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    if (buttonIcon != null) {
                        IconButton(onClick = onIconButtonClick, content = buttonIcon)
                    } else Spacer(modifier = Modifier.width(8.dp))
                }
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            AnimatedVisibility(visible = isFABVisible, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                fabContent()
            }
        }
    ) { contentPadding ->
        LoadingOverlay(isLoading, modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun NavScreen(
    onDismiss: () -> Unit,
    title: Int,
    buttonIcon: (@Composable () -> Unit)? = null,
    onButtonIconClick: () -> Unit = {},
    confirmText: Int,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    cancelText: Int = android.R.string.cancel,
    snackbar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    isFABVisible: Boolean = true,
    fabContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    NavScreen(onDismiss, title, buttonIcon, onButtonIconClick, bottomBar = {
        Column {
            Divider()
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(cancelText))
                }
                Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(confirmText))
                }
            }
        }
    }, snackbar, isLoading, isFABVisible, fabContent, content)
}