package ro.bankar.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.with
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import ro.bankar.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEverything(isSearchOpen: Boolean, onSearchOpenChange: (Boolean) -> Unit, modifier: Modifier) {
    var searchValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    TextField(
        value = searchValue,
        onValueChange = { searchValue = it },
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !isSearchOpen) onSearchOpenChange(true)
        },
        singleLine = true,
        label = { Text(text = stringResource(R.string.search_everything)) },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        leadingIcon = {
            AnimatedContent(
                targetState = isSearchOpen,
                label = "Search Everything Icon",
                transitionSpec = {
                    (slideIn { IntOffset(-it.width, 0) } + fadeIn() with slideOut { IntOffset(-it.width, 0) } + fadeOut())
                        .using(SizeTransform(clip = false))
                }
            ) {
                if (it) {
                    BackHandler { onSearchOpenChange(false); searchValue = ""; focusManager.clearFocus() }
                    IconButton(onClick = { onSearchOpenChange(false); searchValue = ""; focusManager.clearFocus() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                } else Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.search))
            }
        },
        trailingIcon = {
            AnimatedVisibility(visible = searchValue.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = { searchValue = "" }) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        }
    )
}