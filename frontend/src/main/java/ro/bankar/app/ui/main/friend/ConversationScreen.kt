package ro.bankar.app.ui.main.friend

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.RequestFlow
import ro.bankar.app.data.RequestSuccess
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SConversation
import ro.bankar.model.SDirection
import ro.bankar.model.SSendMessage
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SuccessResponse
import ro.bankar.util.format
import ro.bankar.util.here
import ro.bankar.util.todayHere
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

class ConversationScreenModel : ViewModel() {
    var sentMessages by mutableStateOf(emptyList<String>())
    var conversation by mutableStateOf<SConversation?>(null)
    var message by mutableStateOf("")

    lateinit var conversationFlow: RequestFlow<SConversation>
    lateinit var repository: Repository

    fun sendMessage(recipientTag: String) {
        val message = this.message.trim()
        sentMessages = buildList(sentMessages.size + 1) { add(message); addAll(sentMessages) }
        this.message = ""
        viewModelScope.launch {
            var retryInterval = 2
            while (true) {
                val result = repository.sendFriendMessage(recipientTag, message)
                if (result is RequestSuccess && result.response == SuccessResponse) break
                delay(retryInterval.seconds)
                if (retryInterval < 15) ++retryInterval
            }
        }
    }
}

private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE")
private val sameYearFormatter = DateTimeFormatter.ofPattern("dd MMM")

@Composable
fun ConversationScreen(tag: String, navigation: NavHostController) {
    val model = viewModel<ConversationScreenModel>()
    val repository = LocalRepository.current
    model.repository = repository
    val user = remember { repository.friends.map { it.find { friend -> friend.tag == tag } } }.collectAsState(null).value
    LaunchedEffect(true) {
        // Get messages
        launch {
            repository.conversation(tag).also {
                it.requestEmit()
                model.conversationFlow = it
            }.collect {
                // To update the "last message" text
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
            repository.socketFlow.filter { it is SSocketNotification.SMessageNotification && it.fromTag == tag }.collect {
                model.conversationFlow.requestEmit()
            }
        }
        // Update unseen messages count on opening/closing any conversation
        model.repository.friends.requestEmit()
    }

    if (user == null) {
        LoadingOverlay(true) {
            Box(Modifier.fillMaxSize())
        }
        return
    }

    FriendScreen(onDismiss = { navigation.popBackStack() }, user, dropdownMenu = { expanded, onDismiss ->
        FriendDropdown(expanded, onDismiss, navigation, user)
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
                    val today = Clock.System.todayHere()
                    val yesterday = today - DatePeriod(days = 1)
                    items(conv.size) { index ->
                        val item = conv[index]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (item.direction == SDirection.Sent) Alignment.End else Alignment.Start
                        ) {
                            val dateTime = item.timestamp.here()
                            val date = dateTime.date

                            val differentDate = if ((index == conv.lastIndex && date != today)
                                || (index != conv.lastIndex && date != conv[index + 1].timestamp.here().date)) {
                                // Display a date badge for older messages
                                val text = when(date) {
                                    today -> stringResource(R.string.today)
                                    yesterday -> stringResource(R.string.yesterday)
                                    in (today - DatePeriod(days = 7))..today -> weekdayFormatter.format(date.toJavaLocalDate())
                                    in LocalDate(today.year, Month.JANUARY, 1)..today -> sameYearFormatter.format(date.toJavaLocalDate())
                                    else -> date.format(true)
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .3f),
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
                                    )
                                }
                                true
                            } else false
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
                            if (index == 0 || differentDate
                                || conv[index - 1].timestamp.here().time.let { it.hour != dateTime.hour || it.minute != dateTime.minute })
                                Text(
                                    text = item.timestamp.here().time.format(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                        }
                    }
                }
                Surface(tonalElevation = 1.dp, border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant)) {
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
                        FilledIconButton(onClick = { model.sendMessage(user.tag) }, enabled = model.message.isNotBlank()) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
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
    AppTheme {
        ConversationScreen(
            tag = "koleci.alexandru",
            navigation = rememberMockNavController()
        )
    }
}