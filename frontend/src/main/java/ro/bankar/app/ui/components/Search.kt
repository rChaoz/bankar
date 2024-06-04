package ro.bankar.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import ro.bankar.app.R

@Composable
fun Search(
    topBar: @Composable (isSearchOpen: Boolean, searchField: @Composable () -> Unit) -> Unit,
    searchResults: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var isSearchOpen by remember { mutableStateOf(false) }

    Column {
        topBar(isSearchOpen) { SearchField(isSearchOpen, onSearchOpenChange = { isSearchOpen = it }) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
            androidx.compose.animation.AnimatedVisibility(
                visible = isSearchOpen,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()
            ) {
                searchResults()
            }
        }
    }
}

@Composable
private fun SearchField(isSearchOpen: Boolean, onSearchOpenChange: (Boolean) -> Unit) {
    var searchValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    TextField(
        value = searchValue,
        onValueChange = { searchValue = it },
        modifier = Modifier.onFocusChanged {
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
                    ((slideIn { IntOffset(-it.width, 0) } + fadeIn()) togetherWith slideOut { IntOffset(-it.width, 0) } + fadeOut())
                        .using(SizeTransform(clip = false))
                }
            ) {
                if (it) {
                    BackHandler { onSearchOpenChange(false); searchValue = ""; focusManager.clearFocus() }
                    IconButton(onClick = { onSearchOpenChange(false); searchValue = ""; focusManager.clearFocus() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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