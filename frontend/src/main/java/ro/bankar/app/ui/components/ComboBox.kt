package ro.bankar.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ComboBox(
    selectedItemText: String,
    onSelectItem: (T) -> Unit,
    items: List<T>,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    outlined: Boolean = false,
    enabled: Boolean = true,
    label: Int? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    dropdownItemGenerator: @Composable (item: T, onClick: () -> Unit) -> Unit = { item, onClick ->
        DropdownMenuItem(text = { Text(text = item.toString()) }, onClick = onClick)
    }
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled && expanded) expanded = false
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it && enabled }, modifier = modifier) {
        if (outlined) OutlinedTextField(
            value = selectedItemText,
            onValueChange = {},
            singleLine = true, readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(
                        animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ComboBox icon rotation").value
                    )
                )
            },
            modifier = Modifier
                .let { if (fillWidth) it.fillMaxWidth() else it }
                .menuAnchor(),
            enabled = enabled,
            label = label?.let { { Text(text = stringResource(label)) } },
            isError = isError,
            supportingText = supportingText?.let { { Text(text = it) } },
        ) else TextField(
            value = selectedItemText,
            onValueChange = {},
            singleLine = true, readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(
                        animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ComboBox icon rotation").value
                    )
                )
            },
            modifier = Modifier
                .let { if (fillWidth) it.fillMaxWidth() else it }
                .menuAnchor(),
            enabled = enabled,
            label = label?.let { { Text(text = stringResource(label)) } },
            isError = isError,
            supportingText = supportingText?.let { { Text(text = it) } },
        )
        ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            for (item in items) dropdownItemGenerator(item) { onSelectItem(item); expanded = false }
        }
    }
}