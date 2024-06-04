package ro.bankar.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BottomDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    buttonBar: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (visible) Dialog(onDismissRequest, properties) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(remember { MutableInteractionSource() }, null, onClick = {
                    if (properties.dismissOnClickOutside) onDismissRequest()
                }),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shadowElevation = 3.dp,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 36.dp)
                    .clickable(remember { MutableInteractionSource() }, null) {}, // prevent clicks from reaching parent
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Box(modifier = Modifier.weight(1f, false)) {
                        content()
                    }
                    HorizontalDivider()
                    buttonBar()
                }
            }
        }
    }
}

@Composable
fun BottomDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    confirmButtonText: Int,
    confirmButtonEnabled: Boolean = true,
    onConfirmButtonClick: () -> Unit,
    content: @Composable () -> Unit
) {
    BottomDialog(visible, onDismissRequest, properties, buttonBar = {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onDismissRequest, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(android.R.string.cancel))
            }
            Button(onClick = onConfirmButtonClick, modifier = Modifier.weight(1f), enabled = confirmButtonEnabled) {
                Text(text = stringResource(confirmButtonText))
            }
        }
    }, content)
}