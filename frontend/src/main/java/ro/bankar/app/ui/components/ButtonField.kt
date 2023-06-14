package ro.bankar.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle

/**
 * Button that looks like an OutlinedTextField
 */
@Composable
fun ButtonField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    label: Int,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape
) {
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(true) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) onClick()
        }
    }
    OutlinedTextField(
        value = value, onValueChange = {},
        modifier = modifier,
        readOnly = true,
        textStyle = textStyle, label = { Text(text = stringResource(label)) }, placeholder = null,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } }, isError = isError,
        singleLine = true,
        shape = shape,
        interactionSource = interactionSource,
    )
}