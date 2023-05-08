package ro.bankar.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ro.bankar.app.R

@Composable
fun PopupScreen(
    onDismiss: () -> Unit,
    title: Int,
    buttons: @Composable () -> Unit,
    snackBar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackBar) }
    ) { contentPadding ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)) {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            LoadingOverlay(isLoading) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Divider()
            buttons()
        }
    }
}

@Composable
fun PopupScreen(
    onDismiss: () -> Unit,
    title: Int,
    confirmText: Int,
    confirmEnabled: Boolean = true,
    onConfirm: () -> Unit,
    snackBar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    PopupScreen(onDismiss, title, buttons = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp), horizontalArrangement = Arrangement.SpaceAround
        ) {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled
            ) {
                Text(text = stringResource(confirmText))
            }
        }
    }, snackBar, isLoading, content)
}