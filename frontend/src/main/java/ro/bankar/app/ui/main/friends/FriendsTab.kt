package ro.bankar.app.ui.main.friends

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.theme.AppTheme
import kotlin.math.absoluteValue
import kotlin.math.sign

object FriendsTab : MainTab<FriendsTab.Model>(0, "friends", R.string.friends) {
    class Model : MainTabModel() {
        override val showFAB = mutableStateOf(true)

    }

    @Composable
    override fun viewModel() = viewModel<Model>()

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content(model: Model, navigation: NavHostController) {
        val repository = LocalRepository.current
        LaunchedEffect(true) { repository.friends.requestEmit(true) }

        val pagerState = rememberPagerState(0)
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = pagerState.currentPage, indicator = { list ->
                val page = pagerState.currentPage
                val offset = pagerState.currentPageOffsetFraction

                val tab = list[page]
                val targetTab = list[page + offset.sign.toInt()]

                val targetP = offset.absoluteValue
                val currentP = 1f - targetP

                TabRowDefaults.Indicator(modifier = Modifier
                    .wrapContentSize(Alignment.BottomStart)
                    .width(tab.width + targetTab.width * targetP)
                    .offset(x = tab.left * currentP + targetTab.left * targetP - tab.width * targetP / 2))
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
                tabs[it].Content(model)
            }
        }
    }

    @Composable
    override fun FABContent(model: Model, navigation: NavHostController) {
        ExtendedFloatingActionButton(
            onClick = { /*TODO*/ },
            text = { Text(text = stringResource(R.string.add_friend)) },
            icon = { Icon(imageVector = Icons.Default.Add, null) }
        )
    }
}

private val tabs = listOf(FriendsTabs.Friends, FriendsTabs.FriendRequests)

private sealed class FriendsTabs(val index: Int, val title: Int) {
    @Composable
    abstract fun Content(model: FriendsTab.Model)

    object Friends : FriendsTabs(0, R.string.friends) {
        @Composable
        override fun Content(model: FriendsTab.Model) {
            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.showFAB.component2())
            Text(text = "Content 1")
        }
    }

    object FriendRequests : FriendsTabs(1, R.string.friend_requests) {
        @Composable
        override fun Content(model: FriendsTab.Model) {
            val scrollState = rememberScrollState()
            HideFABOnScroll(state = scrollState, setFABShown = model.showFAB.component2())
            Text(text = "Content 2")
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