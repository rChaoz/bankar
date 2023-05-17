package ro.bankar.app.ui.main.friends

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.data.collectRetrying
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackBar
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.safeDecodeFromString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SCountries
import ro.bankar.model.SDirection
import ro.bankar.model.SPublicUser
import kotlin.math.absoluteValue
import kotlin.math.sign

object FriendsTab : MainTab<FriendsTab.Model>(0, "friends", R.string.friends) {
    class Model : MainTabModel() {
        // Data
        var friends by mutableStateOf<List<SPublicUser>?>(null)
        var friendRequests by mutableStateOf<List<SPublicUser>?>(null)
        var countryData by mutableStateOf<SCountries?>(null)

        // FAB
        var scrollShowFAB = mutableStateOf(true)
        override val showFAB = derivedStateOf { scrollShowFAB.value && friends != null && friendRequests != null }
        lateinit var snackBar: SnackbarHostState

        // Add friend dialog
        var addFriendLoading by mutableStateOf(false)
        var showAddFriendDialog by mutableStateOf(false)
        var addFriendInput by mutableStateOf("")
        var addFriendError by mutableStateOf<Int?>(null)

        // Navigate to friend
        lateinit var onNavigateToFriend: (friend: SPublicUser) -> Unit

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
            val result = repository.sendAddFriend(addFriendInput.trim().removePrefix("@"))
            addFriendLoading = false
            when (result) {
                is SafeStatusResponse.InternalError -> addFriendError = result.message
                is SafeStatusResponse.Fail -> addFriendError = when (result.s.status) {
                    "user_not_found" -> R.string.user_not_found
                    "user_is_friend" -> R.string.user_already_friend
                    "cant_friend_self" -> R.string.cant_friend_self
                    else -> R.string.unknown_error
                }

                is SafeStatusResponse.Success -> {
                    addFriendLoading = true
                    repository.friendRequests.emitNow()
                    addFriendLoading = false
                    // Close dialog, show confirm message
                    showAddFriendDialog = false
                    snackBar.showSnackbar(c.getString(R.string.friend_request_sent), withDismissAction = true)
                }
            }
        }

        /**
         * Used to cancel an outgoing friend request
         */
        fun onCancelRequest(c: Context, tag: String, repository: Repository) = viewModelScope.launch {
            when (val result = repository.sendCancelFriendRequest(tag)) {
                is SafeStatusResponse.InternalError -> snackBar.showSnackbar(c.getString(result.message))
                is SafeStatusResponse.Fail ->
                    // Sometimes, the same request can be removed twice (due to fast click/lag), don't show error, just update screen
                    if (result.s.status == "request_not_found") repository.friendRequests.requestEmit()
                    else snackBar.showSnackbar(c.getString(R.string.unknown_error))

                is SafeStatusResponse.Success -> repository.friendRequests.requestEmit()
            }
        }

        /**
         * Used to accept/decline an incoming friend request
         */
        fun onRespondToRequest(c: Context, fromTag: String, accepted: Boolean, repository: Repository) = viewModelScope.launch {
            when (val result = repository.sendFriendRequestResponse(fromTag, accepted)) {
                is SafeStatusResponse.Success -> coroutineScope {
                    launch { repository.friends.emitNow() }
                    launch { repository.friendRequests.emitNow() }
                }

                is SafeStatusResponse.InternalError ->
                    snackBar.showSnackbar(c.getString(result.message), withDismissAction = true)

                is SafeStatusResponse.Fail ->
                    snackBar.showSnackbar(c.getString(R.string.unknown_error), withDismissAction = true)
            }
        }
    }

    @Composable
    override fun viewModel() = viewModel<Model>()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content(model: Model, navigation: NavHostController) {
        model.snackBar = LocalSnackBar.current
        // Load data
        val repository = LocalRepository.current
        model.repository = repository
        LaunchedEffect(true) {
            launch { repository.friends.collectRetrying { model.friends = it } }
            launch { repository.friendRequests.collectRetrying { model.friendRequests = it } }
            launch { repository.countryData.collectRetrying { model.countryData = it } }
        }

        model.onNavigateToFriend = { friend -> navigation.navigate(MainNav.Friend(friend)) }

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
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = pagerState.currentPage, indicator = { list ->
                val page = pagerState.currentPage
                val offset = pagerState.currentPageOffsetFraction

                val tab = list[page]
                val targetTab = list[page + offset.sign.toInt()]

                val targetP = offset.absoluteValue
                val currentP = 1f - targetP

                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .wrapContentSize(Alignment.BottomStart)
                        .width(tab.width + targetTab.width * targetP)
                        .offset(x = tab.left * currentP + targetTab.left * targetP - tab.width * targetP / 2)
                )
            }) {
                val scope = rememberCoroutineScope()
                for (tab in tabs) {
                    Tab(
                        selected = pagerState.currentPage == tab.index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(tab.index) }
                        },
                        text = { Text(text = stringResource(tab.title)) }
                    )
                }
            }
            HorizontalPager(
                pageCount = tabs.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) {
                tabs[it].Content(model, repository)
            }
        }
    }

    @Composable
    override fun FABContent(model: Model, navigation: NavHostController) {
        ExtendedFloatingActionButton(
            onClick = model::showAddFriendDialog,
            text = { Text(text = stringResource(R.string.add_friend)) },
            icon = { Icon(imageVector = Icons.Default.Add, null) }
        )
    }
}

private val tabs = listOf(FriendsTabs.Friends, FriendsTabs.FriendRequests)

private sealed class FriendsTabs(val index: Int, val title: Int) {
    @Composable
    abstract fun Content(model: FriendsTab.Model, repository: Repository)

    object Friends : FriendsTabs(0, R.string.friends) {
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
    }

    object SPublicUserSaver : Saver<SPublicUser?, String> {
        override fun restore(value: String): SPublicUser? = Json.safeDecodeFromString(value)
        override fun SaverScope.save(value: SPublicUser?) = if (value != null) Json.encodeToString(value) else null
    }

    object FriendRequests : FriendsTabs(1, R.string.friend_requests) {


        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        override fun Content(model: FriendsTab.Model, repository: Repository) {
            // Show information about friend request
            val (requestInfo, setRequestInfo) = rememberSaveable(stateSaver = SPublicUserSaver) { mutableStateOf(null) }

            requestInfo?.let {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(onDismissRequest = { setRequestInfo(null) }, sheetState = sheetState) {
                    Column(modifier = Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                            Avatar(image = it.avatar)
                            Column {
                                Text(text = it.fullName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "@${it.tag}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${model.countryData.nameFromCode(it.countryCode)}\n" +
                                            stringResource(R.string.joined_on, it.joinDate.format(true)),
                                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
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
                                    Text(text = it.about)
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
                        // TODO Friend requests clickable to view user profile
                        val (sentRequests, receivedRequests) = model.friendRequests!!.sortedBy { it.tag }.partition { it.requestDirection == SDirection.Sent }
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
        fun SentRequests(requests: List<SPublicUser>, setRequestInfo: (SPublicUser) -> Unit, onCancelRequest: (String) -> Unit) {
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
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(android.R.string.cancel))
                            }
                        }
                    }
                }
            }

        }

        @Composable
        private fun ReceivedRequests(requests: List<SPublicUser>, setRequestInfo: (SPublicUser) -> Unit, onRespondToRequest: (String, Boolean) -> Unit) {
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
                            .size(120.dp, 15.dp)
                            .grayShimmer(shimmer)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(70.dp, 13.dp)
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