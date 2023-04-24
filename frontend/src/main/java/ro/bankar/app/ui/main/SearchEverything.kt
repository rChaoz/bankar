package ro.bankar.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ro.bankar.app.R

@Composable
fun SearchEverything(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value,
        onValueChange,
        modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
    ) { innerTextField ->
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center, propagateMinConstraints = true) {
                    if (value.isEmpty()) Text(
                        text = stringResource(R.string.search_everyting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    innerTextField()
                }
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = stringResource(R.string.profile))
                }
            }
        }
    }
}