package ro.bankar.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ComboBox(
    selectedItem: T,
    onSelectItem: (T) -> Unit,
    items: List<T>,
    modifier: Modifier = Modifier,
    label: Int,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = setExpanded, modifier = modifier) {
        OutlinedTextField(
            value = selectedItem.toString(),
            onValueChange = {},
            singleLine = true, readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.rotate(
                        animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ComboBox icon rotation").value
                    )
                )
            },
            modifier = Modifier.menuAnchor(),
            label = { Text(text = stringResource(label)) },
            isError = isError,
            supportingText = supportingText?.let { { Text(text = it) } },
        )
        ExposedDropdownMenu(expanded, onDismissRequest = { setExpanded(false) }) {
            for (item in items) DropdownMenuItem(text = { Text(text = item.toString()) }, onClick = { onSelectItem(item); setExpanded(false) })
        }
    }
}