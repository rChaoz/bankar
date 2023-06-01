package ro.bankar.app.ui.main.friend

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.datetime.Clock
import ro.bankar.app.R
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SPublicUser
import ro.bankar.model.SPublicUserBase
import ro.bankar.util.todayHere

@Composable
fun FriendScreen(
    onDismiss: () -> Unit,
    user: SPublicUserBase,
    onClickOnUser: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackBar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    dropdownMenuContent: (@Composable ColumnScope.(hide: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(imageVector = Icons.Default.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(32.dp))
                    }
                    if (onClickOnUser == null) BarUserProfile(user, modifier = Modifier.weight(1f))
                    else Surface(onClick = onClickOnUser, color = Color.Transparent, modifier = Modifier
                        .weight(1f)) {
                        BarUserProfile(user)
                    }
                    if (dropdownMenuContent != null) {
                        Box(modifier = Modifier.padding(vertical = 8.dp)) {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
                            }
                            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                dropdownMenuContent { expanded = false }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackBar) },
    ) { paddingValues ->
        LoadingOverlay(isLoading, modifier = Modifier.padding(paddingValues), content = content)
    }
}

@Composable
private fun BarUserProfile(user: SPublicUserBase, modifier: Modifier = Modifier) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 8.dp, bottom = 8.dp, start = 2.dp, end = 8.dp)) {
        if (user.avatar == null)
            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = stringResource(R.string.avatar), modifier = Modifier.size(48.dp))
        else
            AsyncImage(
                model = user.avatar, contentDescription = stringResource(R.string.avatar), modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.fullName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = "@${user.tag}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }

@Preview
@Composable
private fun FriendScreenPreview() {
    AppTheme {
        FriendScreen(onDismiss = {}, onClickOnUser = {}, user = SPublicUser(
            "koleci", "Alexandru", "Paul", "Koleci",
            "RO", Clock.System.todayHere(), "", null, true
        )
        ) {
            Text(text = "Test content")
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FriendScreenPreviewDark() {
    AppTheme {
        FriendScreen(onDismiss = {}, user = SPublicUser(
            "chaoz", "Matei", "Paul", "Trandafir",
            "RO", Clock.System.todayHere(), "", null, true
        ), dropdownMenuContent = {}) {
            Text(text = "Test content")
        }
    }
}