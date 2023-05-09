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
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.components.AcceptDeclineButtons
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.LocalSnackBar
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SDirection
import ro.bankar.model.SPublicUser
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.time.Duration.Companion.seconds

object FriendsTab : MainTab<FriendsTab.Model>(0, "friends", R.string.friends) {
    class Model : MainTabModel() {
        override val showFAB = mutableStateOf(true)
        lateinit var snackBar: SnackbarHostState

        var addFriendLoading by mutableStateOf(false)
        var showAddFriendDialog by mutableStateOf(false)
        var addFriendInput by mutableStateOf("")
        var addFriendError by mutableStateOf<Int?>(null)

        fun showAddFriendDialog() {
            addFriendInput = ""
            addFriendError = null
            showAddFriendDialog = true
        }

        fun onAddFriend(c: Context, repository: Repository) = viewModelScope.launch {
            addFriendLoading = true
            val result = repository.sendAddFriend(addFriendInput.trim())
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
                    repository.friendRequests.requestEmit(false)
                    // Ensure we have time to update friend requests list
                    addFriendLoading = true
                    delay(1.seconds)
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
                    if (result.s.status == "request_not_found") repository.friendRequests.requestEmit(false)
                    else snackBar.showSnackbar(c.getString(R.string.unknown_error))
                is SafeStatusResponse.Success -> repository.friendRequests.requestEmit(false)
            }
        }

        /**
         * Used to accept/decline an incoming friend request
         */
        fun onRespondToRequest(c: Context, fromTag: String, accepted: Boolean, repository: Repository) = viewModelScope.launch {
            when (val result = repository.sendFriendRequestResponse(fromTag, accepted)) {
                is SafeStatusResponse.Success -> {
                    repository.friends.requestEmit(false)
                    repository.friendRequests.requestEmit(false)
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
        val repository = LocalRepository.current
        LaunchedEffect(true) {
            repository.friends.requestEmit(true)
            repository.friendRequests.requestEmit(true)
        }

        // Add friend dialog
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
                    TextField(
                        value = model.addFriendInput,
                        onValueChange = { model.addFriendInput = it; model.addFriendError = null },
                        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxSize(),
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
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
            val friends by repository.friends.collectAsState(initial = null)
            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.showFAB.component2())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .verticalScroll(scrollState)
            ) {
                SurfaceList(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (friends == null) ShimmerFriends()
                    else if (friends!!.isEmpty()) InfoCard(onClick = model::showAddFriendDialog, text = R.string.no_friends)
                    else {
                        for (friend in friends!!) Surface(onClick = { /*TODO*/ }, tonalElevation = 1.dp) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (friend.avatar == null) Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.avatar),
                                    modifier = Modifier.size(48.dp)
                                )
                                else AsyncImage(
                                    model = friend.avatar,
                                    contentDescription = stringResource(R.string.avatar),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                                Column {
                                    Text(
                                        text = friend.fullName,
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                    )
                                    Text(text = "@${friend.tag}", style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(modifier = Modifier.weight(1f))
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

    object FriendRequests : FriendsTabs(1, R.string.friend_requests) {
        @Composable
        override fun Content(model: FriendsTab.Model, repository: Repository) {
            val friendRequests by repository.friendRequests.collectAsState(initial = null)

            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.showFAB.component2())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (friendRequests == null) ShimmerFriends()
                else if (friendRequests!!.isEmpty()) {
                    InfoCard(text = R.string.no_friend_requests)
                } else {
                    // TODO Friend requests clickable to view user profile
                    val (sentRequests, receivedRequests) = friendRequests!!.sortedBy { it.tag }.partition { it.requestDirection == SDirection.Sent }
                    val context = LocalContext.current
                    if (sentRequests.isNotEmpty())
                        SentRequests(requests = sentRequests, onCancelRequest = { tag -> model.onCancelRequest(context, tag, repository) })
                    if (receivedRequests.isNotEmpty())
                        ReceivedRequests(
                            requests = receivedRequests,
                            onRespondToRequest = { tag, accepted -> model.onRespondToRequest(context, tag, accepted, repository) }
                        )
                }
            }
        }

        @Composable
        fun SentRequests(requests: List<SPublicUser>, onCancelRequest: (String) -> Unit) {
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
                    for (req in requests) Surface(tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (req.avatar == null) Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.avatar),
                                modifier = Modifier.size(48.dp)
                            )
                            else AsyncImage(
                                model = req.avatar,
                                contentDescription = stringResource(R.string.avatar),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Column {
                                Text(
                                    text = req.fullName,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                )
                                Text(text = "@${req.tag}", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { onCancelRequest(req.tag) }, colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ), border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
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
        private fun ReceivedRequests(requests: List<SPublicUser>, onRespondToRequest: (String, Boolean) -> Unit) {
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
                    for (req in requests) Surface(onClick = { /*TODO*/ }, tonalElevation = 1.dp) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (req.avatar == null) Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.avatar),
                                modifier = Modifier.size(48.dp)
                            )
                            else AsyncImage(
                                model = req.avatar,
                                contentDescription = stringResource(R.string.avatar),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Column {
                                Text(
                                    text = req.fullName,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                )
                                Text(text = "@${req.tag}", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.weight(1f))
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
        repeat(4) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
private fun HomePreview() {
    AppTheme {
        FriendsTab.Content(FriendsTab.viewModel(), rememberNavController())
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomePreviewDark() {
    AppTheme {
        FriendsTab.Content(FriendsTab.viewModel(), rememberNavController())
    }
}