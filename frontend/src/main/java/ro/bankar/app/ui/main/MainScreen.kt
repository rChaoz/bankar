package ro.bankar.app.ui.main

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.Visibility
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import ro.bankar.app.LocalDataStore
import ro.bankar.app.R
import ro.bankar.app.USER_SESSION
import ro.bankar.app.removePreference
import ro.bankar.app.ui.main.home.Home
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SUserValidation

private enum class Tabs {
    Home, Friends, Settings;
}

val LocalSnackBar = compositionLocalOf { SnackbarHostState() }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMotionApi::class)
@Composable
fun MainScreen() {
    var searching by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(Tabs.Home) }

    // To allow logout for testing
    if (tab == Tabs.Friends) {
        val dataStore = LocalDataStore.current
        LaunchedEffect(true) { dataStore.removePreference(USER_SESSION) }
    }

    val snackBar = remember { SnackbarHostState() }

    var imageURI by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(contract = CropImageContract()) {
        if (it.isSuccessful) imageURI = it.uriContent
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBar) },
        topBar = {
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
                            linkTo(parent.start, profile.start, startMargin = 12.dp, endMargin = 8.dp, bias = 0f)
                            width = Dimension.preferredWrapContent
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
                    progress = animateFloatAsState(if (searching) 1f else 0f, label = "MotionLayout progress").value,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SearchEverything(isSearchOpen = searching, onSearchOpenChange = { searching = it }, modifier = Modifier.layoutId("search"))
                    ProfileRibbon(modifier = Modifier.layoutId("profile"))
                    Text(text = "Overview", style = MaterialTheme.typography.displayMedium, modifier = Modifier.layoutId("title"))
                }
            }
        },
        bottomBar = {
            if (!searching) NavigationBar {
                NavigationBarItem(selected = tab == Tabs.Friends, onClick = { tab = Tabs.Friends }, icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_people_24),
                        contentDescription = stringResource(R.string.friends)
                    )
                }, label = {
                    Text(text = stringResource(R.string.friends))
                })
                NavigationBarItem(selected = tab == Tabs.Home, onClick = { tab = Tabs.Home }, icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.home)
                    )
                }, label = {
                    Text(text = stringResource(R.string.home))
                })
                NavigationBarItem(selected = tab == Tabs.Settings, onClick = { tab = Tabs.Settings }, icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings)
                    )
                }, label = {
                    Text(text = stringResource(R.string.settings))
                })
            }
        }
    ) { contentPadding ->
        CompositionLocalProvider(LocalSnackBar provides snackBar) {
            AnimatedContent(
                modifier = Modifier.padding(contentPadding),
                targetState = searching,
                label = "Search Content Animation",
                transitionSpec = {
                    if (targetState) slideIn { IntOffset(0, it.height) } + fadeIn() with ExitTransition.None
                    else EnterTransition.None with slideOut { IntOffset(0, it.height) } + fadeOut()
                }
            ) {
                if (it) {
                    Column {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
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
                } else {
                    Box {
                        Home()
                        Button(
                            onClick = {
                                @Suppress("DEPRECATION") // for Bitmap.CompressFormat.WEBP which is needed due to API level
                                imagePicker.launch(
                                    CropImageContractOptions(
                                        null, CropImageOptions(
                                            aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true,
                                            minCropResultWidth = 150, minCropResultHeight = 150, cropShape = CropImageView.CropShape.OVAL,
                                            outputRequestWidth = SUserValidation.avatarSize, outputRequestHeight = SUserValidation.avatarSize,
                                            outputCompressFormat = Bitmap.CompressFormat.WEBP
                                        )
                                    )
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterEnd),
                        ) {
                            Text(text = "hehe")
                        }
                        AsyncImage(model = imageURI, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    AppTheme {
        MainScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreviewDark() {
    AppTheme {
        MainScreen()
    }
}