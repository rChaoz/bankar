package ro.bankar.app.ui.main.friends

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import ro.bankar.app.ui.main.LocalSnackBar
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.theme.AppTheme
import kotlin.math.absoluteValue
import kotlin.math.sign

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
                    showAddFriendDialog = false
                    snackBar.showSnackbar(c.getString(R.string.friend_request_sent), withDismissAction = true)
                }
            }
        }

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
            val friendsState = repository.friends.collectAsState(initial = null)
            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.showFAB.component2())

            SurfaceList(modifier = Modifier.fillMaxSize()) {
                val friends = friendsState.value
                if (friends != null) {
                    if (friends.isEmpty()) InfoCard(onClick = model::showAddFriendDialog, text = R.string.no_friends)
                    else for (req in friends) Surface(onClick = { /*TODO*/ }) {
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
                                    text = with(req) { "$firstName ${if (middleName != null) "$middleName " else ""}$lastName" },
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                )
                                Text(text = "@${req.tag}", style = MaterialTheme.typography.titleSmall)
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
            val friendRequests = repository.friendRequests.collectAsState(initial = null)

            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.showFAB.component2())
            SurfaceList(modifier = Modifier.fillMaxSize()) {
                val requests = friendRequests.value
                if (requests != null) {
                    if (requests.isEmpty()) InfoCard(text = R.string.no_friend_requests)
                    else for (req in requests) Surface(onClick = { /*TODO*/ }) {
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
                                    text = with(req) { "$firstName ${if (middleName != null) "$middleName " else ""}$lastName" },
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                                )
                                Text(text = "@${req.tag}", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            val context = LocalContext.current
                            AcceptDeclineButtons(
                                onAccept = { model.onRespondToRequest(context, req.tag, true, repository) },
                                onDecline = { model.onRespondToRequest(context, req.tag, false, repository) }
                            )
                        }
                    }
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