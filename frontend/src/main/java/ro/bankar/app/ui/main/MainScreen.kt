package ro.bankar.app.ui.main

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.Visibility
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import kotlinx.coroutines.launch
import ro.bankar.app.LocalDataStore
import ro.bankar.app.Nav
import ro.bankar.app.R
import ro.bankar.app.USER_SESSION
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.mapCollectAsStateRetrying
import ro.bankar.app.removePreference
import ro.bankar.app.ui.components.NoImage
import ro.bankar.app.ui.components.Search
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.main.friends.FriendsTab
import ro.bankar.app.ui.main.home.HomeTab
import ro.bankar.app.ui.theme.AppTheme

val MainTabs = listOf(HomeTab)

abstract class MainTab<T : MainTab.MainTabModel>(val index: Int, val name: String, val title: Int) {
    companion object {
        val stateSaver = object : Saver<MutableState<MainTab<*>>, String> {
            override fun restore(value: String): MutableState<MainTab<*>>? = when (value) {
                FriendsTab.name -> mutableStateOf(FriendsTab)
                HomeTab.name -> mutableStateOf(HomeTab)
                else -> null
            }

            override fun SaverScope.save(value: MutableState<MainTab<*>>) = value.value.name
        }
    }

    abstract class MainTabModel : ViewModel() {
        abstract val showFAB: State<Boolean>
    }

    @Composable
    abstract fun Content(model: T, navigation: NavHostController)

    @Composable
    abstract fun FABContent(model: T, navigation: NavHostController)

    @Composable
    abstract fun viewModel(): T
}

val LocalSnackBar = compositionLocalOf { SnackbarHostState() }

@Composable
fun MainScreen(initialTab: MainTab<*>, navigation: NavHostController) {
    val (tab, setTab) = rememberSaveable(saver = MainTab.stateSaver) { mutableStateOf(initialTab) }
    // Separating this code into a different function is required to be able to tell the compiler that
    // tab.viewModel() is a valid parameter for tab.Content(), tab.FABContent()
    MainScreen(tab, setTab, navigation)
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun <T : MainTab.MainTabModel> MainScreen(tab: MainTab<T>, setTab: (MainTab<*>) -> Unit, navigation: NavHostController) {
    val tabModel = tab.viewModel()

    val snackBar = remember { SnackbarHostState() }
    val profileImage by LocalRepository.current.profile.mapCollectAsStateRetrying { it.avatar ?: NoImage }

    Search(topBar = { isSearchOpen, searchField ->
        Surface(color = MaterialTheme.colorScheme.secondary) {
            val startC = remember {
                ConstraintSet {
                    val (search, profile, title) = createRefsFor("search", "profile", "title")

                    constrain(search) {
                        linkTo(parent.start, profile.start, startMargin = 8.dp, endMargin = 8.dp)
                        top.linkTo(parent.top, 8.dp)
                        width = Dimension.fillToConstraints
                    }
                    constrain(profile) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end, 12.dp)
                    }
                    constrain(title) {
                        top.linkTo(search.bottom, 16.dp)
                        linkTo(parent.start, profile.start, startMargin = 12.dp, endMargin = 8.dp)
                        width = Dimension.fillToConstraints
                    }
                }
            }

            val endC = remember {
                ConstraintSet {
                    val (search, profile, title) = createRefsFor("search", "profile", "title")
                    constrain(search) {
                        linkTo(parent.start, parent.top, parent.end, parent.bottom, 8.dp, 8.dp, 8.dp, 8.dp)
                        width = Dimension.fillToConstraints
                    }
                    constrain(profile) {
                        start.linkTo(parent.end, 8.dp)
                        visibility = Visibility.Gone
                    }
                    constrain(title) {
                        top.linkTo(parent.top, 0.dp)
                        alpha = 0f
                    }
                }
            }

            MotionLayout(
                start = startC,
                end = endC,
                progress = animateFloatAsState(if (isSearchOpen) 1f else 0f, label = "MotionLayout progress").value,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.layoutId("search"), propagateMinConstraints = true) {
                    searchField()
                }
                ProfileRibbon(image = profileImage, modifier = Modifier.layoutId("profile"), onClick = { navigation.navigate(MainNav.Profile.route) })
                AnimatedContent(
                    targetState = tab,
                    label = "TopBar title change",
                    modifier = Modifier.layoutId("title"),
                    transitionSpec = { fadeIn() with fadeOut() }
                ) {
                    Text(text = stringResource(it.title), style = MaterialTheme.typography.displayMedium)
                }
            }
        }
    }, searchResults = {
        Surface {
            SurfaceList(modifier = Modifier.fillMaxSize()) {
                Surface(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Search item", modifier = Modifier.padding(16.dp))
                }
                Surface(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Search item", modifier = Modifier.padding(16.dp))
                }
                Surface(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Search item", modifier = Modifier.padding(16.dp))
                }
                Surface(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Search item", modifier = Modifier.padding(16.dp))
                }
                Surface(onClick = { /*TODO*/ }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Search item", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBar) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = tab == FriendsTab, onClick = { setTab(FriendsTab) }, icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_people_24),
                            contentDescription = stringResource(R.string.friends)
                        )
                    }, label = {
                        Text(text = stringResource(R.string.friends))
                    })
                    NavigationBarItem(selected = tab == HomeTab, onClick = { setTab(HomeTab) }, icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.home)
                        )
                    }, label = {
                        Text(text = stringResource(R.string.home))
                    })
                    // TODO Debug Only: Click Settings to sign out
                    val dataStore = LocalDataStore.current
                    val scope = rememberCoroutineScope()
                    NavigationBarItem(selected = false, onClick = {
                        scope.launch {
                            dataStore.removePreference(USER_SESSION)
                            navigation.navigate(Nav.NewUser.route) {
                                popUpTo(Nav.Main.route) { inclusive = true }
                            }
                        }
                    }, icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }, label = {
                        Text(text = stringResource(R.string.settings))
                    })
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = snackBar.currentSnackbarData == null && tabModel.showFAB.value,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    tab.FABContent(tabModel, navigation)
                }
            }
        ) { contentPadding ->
            CompositionLocalProvider(LocalSnackBar provides snackBar) {
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.padding(contentPadding),
                    label = "Main Tab",
                    transitionSpec = {
                        if (targetState.index > initialState.index)
                            slideInHorizontally { it / 4 } + fadeIn() with slideOutHorizontally { -it / 4 } + fadeOut()
                        else
                            slideInHorizontally { -it / 4 } + fadeIn() with slideOutHorizontally { it / 4 } + fadeOut()
                    }
                ) {
                    it.Content(it.viewModel(), navigation)
                }
            }
        }
    }
}

@Composable
private fun rememberMockNavController(): NavHostController {
    val context = LocalContext.current
    return remember {
        object : NavHostController(context) {
            override fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) {
                // do nothing
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    AppTheme {
        MainScreen(HomeTab, rememberMockNavController())
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreviewDark() {
    AppTheme {
        MainScreen(HomeTab, rememberMockNavController())
    }
}