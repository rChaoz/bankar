package ro.bankar.app.ui.main.friend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.handle
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.ErrorResponse
import ro.bankar.model.Response
import ro.bankar.model.SPublicUserBase
import ro.bankar.model.SuccessResponse
import ro.bankar.util.format

@Composable
fun UserCard(
    user: SPublicUserBase,
    country: String,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState? = null,
    showAddFriend: Boolean = false
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar(image = user.avatar)
            Column {
                Text(text = user.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "@${user.tag}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$country\n" +
                            stringResource(R.string.joined_on, user.joinDate.format(true)),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline
                )
            }
        }
        if (showAddFriend) {
            val repository = LocalRepository.current
            var addingFriend by remember { mutableStateOf(false) }
            val friendAdded by remember { repository.friendRequests.map { requests -> requests.any { it.tag == user.tag } } }.collectAsState(false)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            if (addingFriend) CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(40.dp),
                strokeWidth = 3.dp
            )
            else if (!friendAdded) {
                FilledTonalButton(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = {
                        addingFriend = true
                        scope.launch {
                            val handle: suspend (Response<Unit>) -> String? = {
                                when (it) {
                                    SuccessResponse -> {
                                        repository.friendRequests.emitNow()
                                        null
                                    }
                                    is ErrorResponse -> context.getString(when (it.message) {
                                        "user_is_friend" -> R.string.user_already_friend
                                        "exists" -> R.string.friend_request_exists
                                        else -> R.string.unknown_error
                                    })
                                    else -> context.getString(R.string.unknown_error)
                                }
                            }
                            if (snackbar != null) repository.sendAddFriend(user.tag).handle(this, snackbar, context) { handle(it) }
                            else repository.sendAddFriend(user.tag).handle(context) { handle(it) }
                            addingFriend = false
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.add_friend))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.customColors.green)
                    Text(
                        text = stringResource(R.string.friend_request_sent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.customColors.green
                    )
                }
            }
        }
    }
}