package ro.bankar.app.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.bankar.app.R
import ro.bankar.app.ui.main.home.Home
import ro.bankar.app.ui.theme.AppTheme

enum class MainTabs(val route: String) {
    Home("home"), Friends("friends"), Settings("settings");
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var searchValue by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(MainTabs.Home) }
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchEverything(
                        value = searchValue,
                        onValueChange = { searchValue = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(text = "Overview", style = MaterialTheme.typography.displayMedium)
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == MainTabs.Friends, onClick = { tab = MainTabs.Friends }, icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_people_24),
                        contentDescription = stringResource(R.string.friends)
                    )
                }, label = {
                    Text(text = stringResource(R.string.friends))
                })
                NavigationBarItem(selected = tab == MainTabs.Home, onClick = { tab = MainTabs.Home }, icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.home)
                    )
                }, label = {
                    Text(text = stringResource(R.string.home))
                })
                NavigationBarItem(selected = tab == MainTabs.Settings, onClick = { tab = MainTabs.Settings }, icon = {
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
        Box(modifier = Modifier.padding(contentPadding)) {
            Home()
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    AppTheme {
        HomeScreen()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreviewDark() {
    AppTheme {
        HomeScreen()
    }
}