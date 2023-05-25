package ro.bankar.app.ui.main.friend

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    bottomBar: @Composable () -> Unit = {},
    snackBar: SnackbarHostState = SnackbarHostState(),
    isLoading: Boolean = false,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(32.dp))
                    }
                    if (user.avatar == null)
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = stringResource(R.string.avatar), modifier = Modifier.size(48.dp))
                    else
                        AsyncImage(model = user.avatar, contentDescription = stringResource(R.string.avatar), modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape))
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
            }
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackBar) },
    ) { paddingValues ->
        LoadingOverlay(isLoading, modifier = Modifier.padding(paddingValues), content = content)
    }
}

@Preview
@Composable
private fun FriendScreenPreview() {
    AppTheme {
        FriendScreen(onDismiss = {}, user = SPublicUser(
            "koleci", "Alexandru", "Paul", "Koleci",
            "RO", Clock.System.todayHere(), "", null, true
        )) {
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
        )) {
            Text(text = "Test content")
        }
    }
}