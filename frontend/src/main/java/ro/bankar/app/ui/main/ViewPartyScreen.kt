package ro.bankar.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.friend.FriendCard
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.model.SPartyMember
import ro.bankar.model.SPublicUserBase

@Composable
fun ViewPartyScreen(onDismiss: () -> Unit, partyID: Int, onNavigateToFriend: (SPublicUserBase) -> Unit) {
    val repository = LocalRepository.current
    val countryData by repository.countryData.collectAsState(null)
    val partyState = remember { repository.partyData(partyID).also { it.requestEmit() } }.collectAsState(null)
    val party = partyState.value

    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    var isLoading by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    NavScreen(
        onDismiss,
        title = R.string.party,
        isLoading = isLoading,
        snackbar = snackbar,
        cancelText = R.string.close,
        // TODO If not host, show accept/decline buttons
        confirmText = R.string.cancel_party,
        onConfirm = {
            scope.launch {
                isLoading = true
                repository.sendCancelParty(partyID).handleSuccess(this, snackbar, context) {
                    repository.recentActivity.emitNow()
                    onDismiss()
                }
                isLoading = false
            }
        }
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (party == null) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(80.dp)
                        .grayShimmer(shimmer)
                )
                else Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.party_host), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val host = party.host
                    if (host == null) Surface(shape = MaterialTheme.shapes.small, tonalElevation = 4.dp) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.you_are_party_host), fontStyle = FontStyle.Italic, modifier = Modifier.padding(16.dp))
                        }
                    }
                    else Surface(onClick = { onNavigateToFriend(host) }, shape = MaterialTheme.shapes.small, tonalElevation = 4.dp) {
                        FriendCard(
                            friend = host, country = countryData.nameFromCode(host.countryCode), modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
                Divider()
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (party == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .grayShimmer(shimmer)
                        )
                        Box(
                            modifier = Modifier
                                .size(100.dp, 14.dp)
                                .grayShimmer(shimmer)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(R.string.total_amount))
                            Text(text = party.currency.format(party.total))
                        }

                        Surface(shape = MaterialTheme.shapes.small, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = stringResource(R.string.note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = party.note)
                            }
                        }
                    }
                }
                Divider()
                if (party == null) repeat(3) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .grayShimmer(shimmer)
                        )
                        Column(modifier = Modifier.weight(1f)) {
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
                        Box(
                            modifier = Modifier
                                .size(40.dp, 40.dp)
                                .grayShimmer(shimmer)
                        )
                    }
                } else SurfaceList(modifier = Modifier.padding(vertical = 12.dp)) {
                    // Show self first, highlighted
                    party.members.find { !it.profile.isFriend }?.let {
                        Surface(tonalElevation = 2.dp) {
                            PartyMember(it, currency = party.currency, showYou = true)
                        }
                    }
                    // Then the other members
                    for (member in party.members) {
                        if (member.profile.isFriend) Surface(onClick = { onNavigateToFriend(member.profile) }) {
                            PartyMember(member, currency = party.currency)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyMember(member: SPartyMember, currency: Currency, showYou: Boolean = false) {
    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(image = member.profile.avatar, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (showYou) stringResource(R.string.you_brackets_s, member.profile.firstName) else member.profile.firstName,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
            )
            Text(text = "@${member.profile.tag}", style = MaterialTheme.typography.titleSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Amount(
                amount = member.amount,
                currency = currency,
                color = when (member.status) {
                    SPartyMember.Status.Pending -> MaterialTheme.colorScheme.onSurface
                    SPartyMember.Status.Declined -> MaterialTheme.customColors.red
                    SPartyMember.Status.Accepted -> MaterialTheme.customColors.green
                }
            )
            Text(
                text = stringResource(
                    when (member.status) {
                        SPartyMember.Status.Pending -> R.string.status_pending
                        SPartyMember.Status.Declined -> R.string.status_declined
                        SPartyMember.Status.Accepted -> R.string.status_accepted
                    }
                ), fontStyle = FontStyle.Italic
            )
        }
    }
}

@Preview
@Composable
private fun PartyInformationScreenPreview() {
    AppTheme {
        ViewPartyScreen(onDismiss = {}, partyID = 1, onNavigateToFriend = {})
    }
}

@Preview
@Composable
private fun PartyInformationScreenPreviewDark() {
    AppTheme {
        ViewPartyScreen(onDismiss = {}, partyID = 1, onNavigateToFriend = {})
    }
}