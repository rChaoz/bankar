package ro.bankar.app.ui.main.friends

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.fold
import ro.bankar.app.data.handle
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.MBottomSheet
import ro.bankar.app.ui.components.PagerTabs
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackbar
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.main.friend.FriendCard
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.serializableSaver
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.SCountries
import ro.bankar.model.ErrorResponse
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.SDirection
import ro.bankar.model.SFriend
import ro.bankar.model.SFriendRequest
import ro.bankar.model.SPublicUserBase
import ro.bankar.model.SuccessResponse

object FriendsTab : MainTab<FriendsTab.Model>(0, "friends", R.string.friends) {
    class Model : MainTabModel() {
        var currentTabIndex by mutableStateOf(0)

        // Data
        var friends by mutableStateOf<List<SFriend>?>(null)
        var friendRequests by mutableStateOf<List<SFriendRequest>?>(null)
        var countryData by mutableStateOf<SCountries?>(null)

        // FAB
        var scrollShowFAB = mutableStateOf(true)
        override val showFAB = derivedStateOf { scrollShowFAB.value && friends != null && friendRequests != null }
        lateinit var snackbar: SnackbarHostState

        // Add friend dialog
        var addFriendLoading by mutableStateOf(false)
        var showAddFriendDialog by mutableStateOf(false)
        var addFriendInput by mutableStateOf("")
        var addFriendError by mutableStateOf<Int?>(null)

        // Go to friends tab
        lateinit var onGoToFriendsTab: () -> Unit

        // Navigate to friend/conversation
        lateinit var onNavigateToConversation: (friend: SPublicUserBase) -> Unit
        lateinit var onNavigateToFriend: (friend: SPublicUserBase) -> Unit
        lateinit var onCreateParty: () -> Unit

        // Swipe to refresh
        lateinit var repository: Repository
        var isRefreshing by mutableStateOf(false)
            private set

        fun refresh() = viewModelScope.launch {
            isRefreshing = true
            coroutineScope {
                launch { repository.friends.emitNow() }
                launch { repository.friendRequests.emitNow() }
            }
            isRefreshing = false
        }

        fun showAddFriendDialog() {
            addFriendInput = ""
            addFriendError = null
            showAddFriendDialog = true
        }

        fun onAddFriend(c: Context, repository: Repository) = viewModelScope.launch {
            addFriendLoading = true
            repository.sendAddFriend(addFriendInput.trim().removePrefix("@")).fold(
                onFail = { addFriendError = it },
                onSuccess = {
                    when (it) {
                        SuccessResponse -> {
                            repository.friendRequests.emitNow()
                            // Close dialog, show confirm message
                            showAddFriendDialog = false
                            launch { snackbar.showSnackbar(c.getString(R.string.friend_request_sent), withDismissAction = true) }
                        }
                        is NotFoundResponse -> addFriendError = R.string.user_not_found
                        is ErrorResponse -> addFriendError = when (it.message) {
                            "user_is_friend" -> R.string.user_already_friend
                            "cant_friend_self" -> R.string.cant_friend_self
                            "exists" -> R.string.friend_request_exists
                            else -> R.string.unknown_error
                        }
                        else -> addFriendError = R.string.unknown_error
                    }
                }
            )
            addFriendLoading = false
        }

        /**
         * Used to cancel an outgoing friend request
         */
        fun onCancelRequest(context: Context, tag: String, repository: Repository) = viewModelScope.launch {
            repository.sendCancelFriendRequest(tag).handle(this, snackbar, context) {
                when {
                    it == SuccessResponse -> repository.friendRequests.requestEmit()
                    it is ErrorResponse && it.message == "request_not_found" -> repository.friendRequests.requestEmit()
                    else -> return@handle context.getString(R.string.unknown_error)
                }
                null
            }
        }

        /**
         * Used to accept/decline an incoming friend request
         */
        fun onRespondToRequest(context: Context, fromTag: String, accepted: Boolean, repository: Repository) = viewModelScope.launch {
            repository.sendFriendRequestResponse(fromTag, accepted).handleSuccess(this, snackbar, context) {
                coroutineScope {
                    launch { repository.friends.emitNow() }
                    launch { repository.friendRequests.emitNow() }
                }
            }
        }
    }

    @Composable
    override fun viewModel() = viewModel<Model>()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content(model: Model, navigation: NavHostController) {
        model.snackbar = LocalSnackbar.current
        // Load data
        val repository = LocalRepository.current
        model.repository = repository
        LaunchedEffect(true) {
            launch { repository.friends.collect { model.friends = it } }
            launch { repository.friendRequests.collect { model.friendRequests = it } }
            launch { repository.countryData.collect { model.countryData = it } }
        }

        model.onNavigateToConversation = { friend -> navigation.navigate(MainNav.Conversation(friend)) }
        model.onNavigateToFriend = { friend -> navigation.navigate(MainNav.Friend(friend)) }
        model.onCreateParty = { navigation.navigate(MainNav.CreateParty.route) }

        // "Add friend" dialog
        val context = LocalContext.current
        BottomDialog(
            visible = model.showAddFriendDialog,
            onDismissRequest = { model.showAddFriendDialog = false },
            confirmButtonText = R.string.add_friend,
            confirmButtonEnabled = model.addFriendInput.isNotBlank(),
            onConfirmButtonClick = { model.onAddFriend(context, repository) }
        ) {
            LoadingOverlay(isLoading = model.addFriendLoading) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.add_friend_by))
                    val requester = remember { FocusRequester() }
                    LaunchedEffect(key1 = true) { requester.requestFocus() }
                    TextField(
                        value = model.addFriendInput,
                        onValueChange = { model.addFriendInput = it; model.addFriendError = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(requester),
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.friend)) },
                        isError = model.addFriendError != null,
                        supportingText = { Text(text = model.addFriendError?.let { context.getString(it) } ?: "") },
                        leadingIcon = { Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null) }
                    )
                }
            }
        }

        val pagerState = rememberPagerState(0)
        val scope = rememberCoroutineScope()
        model.currentTabIndex = pagerState.currentPage
        model.onGoToFriendsTab = { scope.launch { pagerState.animateScrollToPage(FriendsTabs.Friends.index) } }

        PagerTabs(tabs = tabs.map { it.title }, pagerState = pagerState) {
            tabs[it].Content(model, repository)
        }
    }

    @Composable
    override fun FABContent(model: Model, navigation: NavHostController) {
        val tab = tabs[model.currentTabIndex]
        if (tab.fabText != null) ExtendedFloatingActionButton(
            onClick = { tab.fabAction(model) },
            text = { Text(text = stringResource(tab.fabText)) },
            icon = { Icon(imageVector = Icons.Default.Add, null) }
        )
        // Otherwise the FAB won't render, no idea why
        else Box(modifier = Modifier.size(1.dp))
    }
}

private val tabs = listOf(FriendsTabs.Conversations, FriendsTabs.Friends, FriendsTabs.FriendRequests)

private sealed class FriendsTabs(val index: Int, val title: Int, val fabText: Int?) {
    @Composable
    abstract fun Content(model: FriendsTab.Model, repository: Repository)
    abstract fun fabAction(model: FriendsTab.Model)

    object Conversations : FriendsTabs(0, R.string.conversations, null) {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        override fun Content(model: FriendsTab.Model, repository: Repository) {
            @Suppress("DEPRECATION") val swipeRefreshState = rememberSwipeRefreshState(model.isRefreshing)
            @Suppress("DEPRECATION") SwipeRefresh(state = swipeRefreshState, onRefresh = model::refresh) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SurfaceList(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        val conversations = model.friends?.filter { it.lastMessage != null }?.sortedByDescending { it.lastMessage!!.dateTime }
                        if (conversations == null) ShimmerFriends()
                        else if (conversations.isEmpty()) InfoCard(onClick = model.onGoToFriendsTab, text = R.string.no_conversations)
                        else {
                            for (friend in conversations) Surface(onClick = { model.onNavigateToConversation(friend) }, tonalElevation = 1.dp) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgedBox(badge = {
                                        if (friend.unreadMessageCount > 0)
                                            Badge(
                                                modifier = Modifier
                                                    .offset((-6).dp, 6.dp)
                                                    .shadow(2.dp, CircleShape)
                                            ) {
                                                val desc = stringResource(R.string.n_unread_messages, friend.unreadMessageCount)
                                                Text(text = friend.unreadMessageCount.toString(), modifier = Modifier.semantics { contentDescription = desc })
                                            }
                                    }) {
                                        Avatar(image = friend.avatar, size = 48.dp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friend.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row {
                                            if (friend.lastMessage!!.direction == SDirection.Sent) Text(
                                                text = stringResource(R.string.you_s),
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = friend.lastMessage!!.message.replace('\n', ' '),
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun fabAction(model: FriendsTab.Model) {}
    }

    object Friends : FriendsTabs(1, R.string.friends, R.string.create_party) {
        @Composable
        override fun Content(model: FriendsTab.Model, repository: Repository) {
            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.scrollShowFAB.component2())

            @Suppress("DEPRECATION") val swipeRefreshState = rememberSwipeRefreshState(model.isRefreshing)
            @Suppress("DEPRECATION") SwipeRefresh(state = swipeRefreshState, onRefresh = model::refresh) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SurfaceList(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (model.friends == null) ShimmerFriends()
                        else if (model.friends!!.isEmpty())
                            InfoCard(onClick = model::showAddFriendDialog, text = R.string.no_friends)
                        else {
                            for (friend in model.friends!!) Surface(onClick = { model.onNavigateToFriend(friend) }, tonalElevation = 1.dp) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(image = friend.avatar, size = 48.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friend.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(text = "@${friend.tag}", style = MaterialTheme.typography.titleSmall)
                                    }
                                    // TODO Implement menu to allow remove friend, more options
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.options))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun fabAction(model: FriendsTab.Model) = model.onCreateParty()
    }

    object FriendRequests : FriendsTabs(2, R.string.requests, R.string.add_friend) {
        @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
        @Composable
        override fun Content(model: FriendsTab.Model, repository: Repository) {
            // Show information about friend request
            val (requestInfo, setRequestInfo) = rememberSaveable(stateSaver = serializableSaver<SFriendRequest?>()) { mutableStateOf(null) }

            requestInfo?.let {
                val sheetState = rememberModalBottomSheetState()
                MBottomSheet(onDismissRequest = { setRequestInfo(null) }, sheetState = sheetState) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .padding(WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FriendCard(friend = it, country = model.countryData.nameFromCode(it.countryCode), modifier = Modifier.padding(horizontal = 12.dp))
                        if (it.about.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                tonalElevation = 3.dp,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = stringResource(R.string.about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = it.about.ifEmpty { stringResource(R.string.nothing_here) },
                                        color = if (it.about.isEmpty()) MaterialTheme.colorScheme.outline else Color.Unspecified
                                    )
                                }
                            }
                        }
                        Divider()
                        val scope = rememberCoroutineScope()
                        TextButton(modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    setRequestInfo(null)
                                }
                            }) {
                            Text(text = stringResource(id = R.string.close))
                        }
                    }
                }
            }

            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.scrollShowFAB.component2())
            @Suppress("DEPRECATION") val swipeRefreshState = rememberSwipeRefreshState(model.isRefreshing)
            @Suppress("DEPRECATION") SwipeRefresh(state = swipeRefreshState, onRefresh = model::refresh) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (model.friendRequests == null) ShimmerFriends()
                    else if (model.friendRequests!!.isEmpty()) {
                        InfoCard(text = R.string.no_friend_requests)
                    } else {
                        val (sentRequests, receivedRequests) = model.friendRequests!!.sortedBy { it.tag }.partition { it.direction == SDirection.Sent }
                        val context = LocalContext.current
                        if (sentRequests.isNotEmpty())
                            SentRequests(
                                requests = sentRequests,
                                setRequestInfo = setRequestInfo,
                                onCancelRequest = { tag -> model.onCancelRequest(context, tag, repository) }
                            )
                        if (receivedRequests.isNotEmpty())
                            ReceivedRequests(
                                requests = receivedRequests,
                                setRequestInfo = setRequestInfo,
                                onRespondToRequest = { tag, accepted -> model.onRespondToRequest(context, tag, accepted, repository) }
                            )
                    }
                }
            }
        }

        @Composable
        fun SentRequests(requests: List<SFriendRequest>, setRequestInfo: (SFriendRequest) -> Unit, onCancelRequest: (String) -> Unit) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.sent_requests),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                )
                SurfaceList(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    for (req in requests) Surface(onClick = { setRequestInfo(req) }, tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(image = req.avatar, size = 48.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.fullName,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                )
                                Text(text = "@${req.tag}", style = MaterialTheme.typography.titleSmall)
                            }
                            OutlinedButton(
                                onClick = { onCancelRequest(req.tag) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(text = stringResource(android.R.string.cancel))
                            }
                        }
                    }
                }
            }

        }

        @Composable
        private fun ReceivedRequests(requests: List<SFriendRequest>, setRequestInfo: (SFriendRequest) -> Unit, onRespondToRequest: (String, Boolean) -> Unit) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.received_requests),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                )
                SurfaceList(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    for (req in requests) Surface(onClick = { setRequestInfo(req) }, tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(image = req.avatar, size = 48.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.fullName,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                )
                                Text(text = "@${req.tag}", style = MaterialTheme.typography.titleSmall)
                            }
                            AcceptDeclineButtons(
                                onAccept = { onRespondToRequest(req.tag, true) },
                                onDecline = { onRespondToRequest(req.tag, false) }
                            )
                        }
                    }
                }
            }
        }

        override fun fabAction(model: FriendsTab.Model) = model.showAddFriendDialog()
    }
}

@Composable
private fun ShimmerFriends() {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    Column {
        repeat(5) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .grayShimmer(shimmer)
                )
                Column {
                    Box(
                        modifier = Modifier
                            .size(180.dp, 15.dp)
                            .grayShimmer(shimmer)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp, 13.dp)
                            .grayShimmer(shimmer)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FriendsPreview() {
    AppTheme {
        FriendsTab.Content(FriendsTab.viewModel(), rememberNavController())
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FriendsPreviewDark() {
    AppTheme {
        FriendsTab.Content(FriendsTab.viewModel(), rememberNavController())
    }
}