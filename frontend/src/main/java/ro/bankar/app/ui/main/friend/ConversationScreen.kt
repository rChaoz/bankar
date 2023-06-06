package ro.bankar.app.ui.main.friend

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.RequestFail
import ro.bankar.app.data.RequestFlow
import ro.bankar.app.data.RequestSuccess
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SConversation
import ro.bankar.model.SDirection
import ro.bankar.model.SPublicUser
import ro.bankar.model.SPublicUserBase
import ro.bankar.model.SSendMessage
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SuccessResponse
import ro.bankar.util.format
import ro.bankar.util.here
import ro.bankar.util.todayHere
import kotlin.time.Duration.Companion.seconds

class ConversationScreenModel : ViewModel() {
    var sentMessages by mutableStateOf(emptyList<String>())
    var conversation by mutableStateOf<SConversation?>(null)
    var message by mutableStateOf("")

    lateinit var conversationFlow: RequestFlow<SConversation>
    lateinit var repository: Repository

    fun sendMessage(context: Context, recipientTag: String) {
        val message = this.message.trim()
        sentMessages = buildList(sentMessages.size + 1) { add(message); addAll(sentMessages) }
        this.message = ""
        viewModelScope.launch {
            sendMessageImpl(context, message, recipientTag)
        }
    }

    private tailrec suspend fun sendMessageImpl(context: Context, message: String, recipientTag: String) {
        when (val result = repository.sendFriendMessage(recipientTag, message)) {
            is RequestFail -> {
                // Retry on connection/internal error
                delay(2.seconds)
                sendMessageImpl(context, message, recipientTag)
            }
            is RequestSuccess -> {
                if (result.response == SuccessResponse) conversationFlow.requestEmit()
                else {
                    // TODO Make message red with retry button or remove it from list, show error message
                    //Toast.makeText(context, R.string.unable_to_send_message)
                }
            }
        }
    }
}

@Composable
fun ConversationScreen(user: SPublicUserBase, navigation: NavHostController) {
    val model = viewModel<ConversationScreenModel>()
    val repository = LocalRepository.current
    model.repository = repository
    LaunchedEffect(key1 = true) {
        // Get messages
        launch {
            repository.conversation(user.tag).also {
                model.conversationFlow = it
                it.requestEmit()
            }.collect {
                // See below
                model.repository.friends.requestEmit()
                // Check what messages are new
                val newMessages = it.take(it.size - (model.conversation?.size ?: 0))
                model.conversation = it
                // Remove new messages from pending messages list
                val sentMessages = model.sentMessages.toMutableList()
                for (message in newMessages) if (message.direction == SDirection.Sent) sentMessages.remove(message.message)
                model.sentMessages = sentMessages
            }
        }
        // When a new message is received, re-get messages
        launch {
            repository.socketFlow.filter { it is SSocketNotification.SMessageNotification && it.fromTag == user.tag }.collect {
                model.conversationFlow.requestEmit()
            }
        }
        // Update unseen messages count on opening/closing any conversation
        model.repository.friends.requestEmit()
    }

    FriendScreen(onDismiss = { navigation.popBackStack() }, user, dropdownMenuContent = {
        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.transfer), contentDescription = null) },
            text = { Text(text = stringResource(R.string.send_money)) },
            onClick = { it(); navigation.navigate(MainNav.SendMoney(user)) }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(painter = painterResource(R.drawable.transfer_request), contentDescription = null) },
            text = { Text(text = stringResource(R.string.request_money)) },
            onClick = { it(); navigation.navigate(MainNav.RequestMoney(user)) }
        )
    }, onClickOnUser = { navigation.navigate(MainNav.Friend(user)) }) {
        val conv = model.conversation
        if (conv != null) {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
                ) {
                    if (model.sentMessages.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.sending),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        items(model.sentMessages) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                                Surface(
                                    modifier = Modifier
                                        .padding(start = 50.dp)
                                        .alpha(.7f),
                                    shadowElevation = 1.dp,
                                    shape = sentMessageShape(cornerSize = 8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = it, modifier = Modifier
                                            .padding(10.dp)
                                            .padding(end = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    items(conv.size) {
                        val item = conv[it]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (item.direction == SDirection.Sent) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                modifier = if (item.direction == SDirection.Sent) Modifier.padding(start = 50.dp) else Modifier.padding(end = 50.dp),
                                shadowElevation = 1.dp,
                                shape = if (item.direction == SDirection.Sent) sentMessageShape(cornerSize = 8.dp) else receivedMessageShape(cornerSize = 8.dp),
                                color = if (item.direction == SDirection.Sent) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = item.message,
                                    modifier = Modifier.padding(8.dp)
                                        .run { if (item.direction == SDirection.Sent) padding(end = 8.dp) else padding(start = 8.dp) }
                                )
                            }
                            if (it == 0 || conv[it - 1].direction != item.direction || conv[it - 1].dateTime.date != item.dateTime.date
                                || conv[it - 1].dateTime.hour != item.dateTime.hour || conv[it - 1].dateTime.minute != item.dateTime.minute
                            )
                                Text(
                                    text = item.dateTime.toInstant(TimeZone.UTC).here().format(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                        }
                    }
                }
                val context = LocalContext.current
                Surface(tonalElevation = 2.dp, shadowElevation = 1.dp) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = model.message,
                            shape = RoundedCornerShape(20.dp),
                            onValueChange = { if (it.trim().length <= SSendMessage.maxLength) model.message = it },
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            placeholder = { Text(text = stringResource(R.string.type_a_message)) },
                            colors = TextFieldDefaults.colors(unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent),
                            maxLines = 4
                        )
                        FilledIconButton(onClick = { model.sendMessage(context, user.tag) }, enabled = model.message.isNotBlank()) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = stringResource(R.string.send))
                        }
                    }
                }
            }
        } else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun sentMessageShape(cornerSize: Dp): Shape {
    val corner = with(LocalDensity.current) { cornerSize.toPx() }
    return GenericShape { size, layoutDirection ->
        if (layoutDirection == LayoutDirection.Ltr) rightMarkerMessageShape(size, corner)
        else leftMarkerMessageShape(size, corner)
    }
}

@Composable
private fun receivedMessageShape(cornerSize: Dp): Shape {
    val corner = with(LocalDensity.current) { cornerSize.toPx() }
    return GenericShape { size, layoutDirection ->
        if (layoutDirection == LayoutDirection.Ltr) leftMarkerMessageShape(size, corner)
        else rightMarkerMessageShape(size, corner)
    }
}

private fun Path.leftMarkerMessageShape(size: Size, cornerSize: Float) {
    moveTo(0f, 0f)
    arcTo(Rect(Offset(size.width - cornerSize, cornerSize), cornerSize), -90f, 90f, false)
    arcTo(Rect(Offset(size.width - cornerSize, size.height - cornerSize), cornerSize), 0f, 90f, false)
    arcTo(Rect(Offset(cornerSize * 2, size.height - cornerSize), cornerSize), 90f, 90f, false)
    lineTo(cornerSize, cornerSize / 1.5f)
    close()
}

private fun Path.rightMarkerMessageShape(size: Size, cornerSize: Float) {
    moveTo(size.width, 0f)
    arcTo(Rect(Offset(cornerSize, cornerSize), cornerSize), -90f, -90f, false)
    arcTo(Rect(Offset(cornerSize, size.height - cornerSize), cornerSize), 180f, -90f, false)
    arcTo(Rect(Offset(size.width - cornerSize * 2, size.height - cornerSize), cornerSize), 90f, -90f, false)
    lineTo(size.width - cornerSize, cornerSize / 1.5f)
    close()
}

@Preview
@Composable
private fun ConversationScreenPreview() {
    AppTheme(useDarkTheme = false) {
        ConversationScreen(
            user = SPublicUser(
                "koleci", "Alexandru", "Paul", "Koleci",
                "RO", Clock.System.todayHere(), "", null, true
            ), navigation = rememberMockNavController()
        )
    }
}