package ro.bankar.app.ui.main.friend

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.handle
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SPublicUser
import ro.bankar.model.SPublicUserBase
import ro.bankar.model.SuccessResponse
import ro.bankar.util.todayHere

@Composable
fun FriendScreen(
    onDismiss: () -> Unit,
    user: SPublicUserBase,
    onClickOnUser: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    snackBar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    dropdownMenu: (@Composable (expanded: Boolean, onDismiss: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.padding(vertical = 8.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(32.dp))
                    }
                    if (onClickOnUser == null) BarUserProfile(user, modifier = Modifier.weight(1f))
                    else Surface(
                        onClick = onClickOnUser, color = Color.Transparent, modifier = Modifier
                            .weight(1f)
                    ) {
                        BarUserProfile(user)
                    }
                    if (dropdownMenu != null) {
                        Box(modifier = Modifier.padding(vertical = 8.dp)) {
                            var expanded by remember { mutableStateOf(false) }
                            IconButton(onClick = { expanded = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
                            }
                            dropdownMenu(expanded) { expanded = false }
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

@Composable
fun FriendDropdown(expanded: Boolean, onDismiss: () -> Unit, navigation: NavHostController, user: SPublicUserBase) {
    var showUnfriendDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    if (showUnfriendDialog) AlertDialog(
        onDismissRequest = { showUnfriendDialog = false },
        icon = {
            Icon(
                painter = painterResource(R.drawable.baseline_unfriend_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        dismissButton = {
            TextButton(onClick = { showUnfriendDialog = false }, enabled = !isLoading) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        confirmButton = {
            val context = LocalContext.current
            val repository = LocalRepository.current
            TextButton(enabled = !isLoading, onClick = {
                isLoading = true
                scope.launch {
                    repository.sendRemoveFriend(user.tag).handle(context) {
                        when (it) {
                            SuccessResponse -> {
                                showUnfriendDialog = false
                                onDismiss()
                                null
                            }
                            else -> context.getString(R.string.unknown_error)
                        }
                    }
                    isLoading = false
                }
            }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(text = stringResource(R.string.remove))
            }
        },
        title = { Text(text = stringResource(R.string.unfriend)) },
        text = {
            LoadingOverlay(isLoading) {
                Text(text = stringResource(R.string.unfriend_confirm, user.fullName))
            }
        },
    )

    DropdownMenu(expanded, onDismissRequest = { onDismiss() }) {
        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.transfer), contentDescription = null) },
            text = { Text(text = stringResource(R.string.send_money)) },
            onClick = { onDismiss(); navigation.navigate(MainNav.SendMoney(user)) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.transfer_request), contentDescription = null) },
            text = { Text(text = stringResource(R.string.request_money)) },
            onClick = { onDismiss(); navigation.navigate(MainNav.RequestMoney(user)) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.baseline_unfriend_24), contentDescription = null) },
            text = { Text(text = stringResource(R.string.unfriend)) },
            onClick = { onDismiss(); showUnfriendDialog = true },
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            )
        )
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
        val user = SPublicUser(
            "chaoz", "Matei", "Paul", "Trandafir",
            "RO", Clock.System.todayHere(), "", null, true
        )
        FriendScreen(
            onDismiss = {},
            user,
            dropdownMenu = { expanded, onDismiss -> FriendDropdown(expanded, onDismiss, navigation = rememberMockNavController(), user) }
        ) {
            Text(text = "Test content")
        }
    }
}