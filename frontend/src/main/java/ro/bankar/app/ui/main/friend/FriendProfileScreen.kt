package ro.bankar.app.ui.main.friend

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.datetime.Clock
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.format
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SDirection
import ro.bankar.model.SPublicUser
import ro.bankar.util.todayHere

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfileScreen(profile: SPublicUser, navigation: NavHostController) {
    val repository = LocalRepository.current
    val countryData by repository.countryData.collectAsStateRetrying()
    NavScreen(onDismiss = { navigation.popBackStack() }, title = R.string.friend_profile) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(tonalElevation = 2.dp, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(image = profile.avatar, size = 120.dp)
                    Text(text = profile.fullName, style = MaterialTheme.typography.headlineSmall)
                    Text(text = "@${profile.tag}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.joined_on, profile.joinDate.format(true)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = countryData.nameFromCode(profile.countryCode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth(.8f)) {
                            ProfileButton(onClick = { navigation.navigate(MainNav.Conversation(profile)) }, text = R.string.send_message) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                            ProfileButton(onClick = { navigation.navigate(MainNav.SendMoney(profile)) }, text = R.string.send_money) {
                                Icon(painter = painterResource(R.drawable.transfer), contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                            ProfileButton(onClick = { navigation.navigate(MainNav.RequestMoney(profile)) }, text = R.string.request_money) {
                                Icon(painter = painterResource(R.drawable.transfer_request), contentDescription = null, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.about.ifEmpty { stringResource(R.string.nothing_here) },
                        color = if (profile.about.isEmpty()) MaterialTheme.colorScheme.outline else Color.Unspecified
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.recent_transfers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // TODO Load transfers
                    Text(
                        text = stringResource(R.string.no_friend_recent_transfers),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ProfileButton(onClick: () -> Unit, text: Int, icon: @Composable () -> Unit) {
    Surface(
        onClick,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.weight(1f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon()
            Text(
                text = stringResource(text),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun FriendProfileScreenPreview() {
    AppTheme {
        FriendProfileScreen(
            navigation = rememberMockNavController(), profile = SPublicUser(
                "maximus", "Andi", "Paul", "Koleci", "RO",
                Clock.System.todayHere(), "bing chilling; iaurti!", SDirection.Sent, null
            )
        )
    }
}