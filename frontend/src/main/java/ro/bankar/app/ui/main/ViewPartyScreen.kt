package ro.bankar.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.ReceivedTransferRequestDialog
import ro.bankar.app.ui.components.SurfaceList
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.friend.UserCard
import ro.bankar.app.ui.main.friend.UserSurfaceCard
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SCountries
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SPartyMember
import ro.bankar.model.SPublicUserBase

@Composable
fun ViewPartyScreen(onDismiss: () -> Unit, partyID: Int, onNavigateToFriend: (SPublicUserBase) -> Unit, onNavigateToTransfer: (SBankTransfer) -> Unit) {
    val repository = LocalRepository.current
    val countryData by repository.countryData.collectAsState(null)
    val flow = repository.partyData(partyID)
    val partyState = remember { flow.also { it.requestEmit() } }.collectAsState(null)
    val party = partyState.value // extract to variable to allow null checks

    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    var isLoading by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Accept invite dialog
    var showAcceptDialog by remember { mutableStateOf(false) }
    if (party?.requestID != null) ReceivedTransferRequestDialog(
        visible = showAcceptDialog,
        onDismiss = { showAcceptDialog = false },
        user = null,
        requestID = party.requestID!!,
        amount = -party.self.amount,
        currency = party.currency,
        partyID = null,
        note = null,
        onNavigateToFriend = onNavigateToFriend,
        onNavigateToParty = {},
        showDeclineOption = false
    )

    NavScreen(
        onDismiss,
        title = R.string.party,
        isLoading = isLoading,
        snackbar = snackbar,
        bottomBar = {
            Column {
                HorizontalDivider()
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.close))
                    }
                    if (party != null && party.self.status == SPartyMember.Status.Host && party.members.any { it.status == SPartyMember.Status.Pending }) {
                        // The user is the host - display cancel button if party isn't completed
                        TextButton(enabled = !isLoading, modifier = Modifier.weight(1f), onClick = {
                            scope.launch {
                                isLoading = true
                                repository.sendCancelParty(partyID).handleSuccess(this, snackbar, context) {
                                    repository.recentActivity.emitNow()
                                    onDismiss()
                                }
                                isLoading = false
                            }
                        }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Text(text = stringResource(R.string.cancel_party))
                        }
                    }
                    // If we are a member and status is pending, show accept/decline buttons
                    if (party?.requestID != null) {
                        TextButton(onClick = {
                            isLoading = true
                            scope.launch {
                                repository.sendRespondToTransferRequest(party.requestID!!, false, null)
                                    .handleSuccess(this, snackbar, context) {
                                        coroutineScope {
                                            launch { flow.emitNow() }
                                            launch { repository.recentActivity.emitNow() }
                                        }
                                        onDismiss()
                                    }
                                isLoading = false
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(R.string.decline), color = MaterialTheme.customColors.red)
                        }
                        TextButton(
                            onClick = { showAcceptDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.customColors.green)
                        ) {
                            Text(text = stringResource(R.string.accept))
                        }
                    }
                }
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
                    if (party.self.status == SPartyMember.Status.Host) Surface(shape = MaterialTheme.shapes.small, tonalElevation = 4.dp) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(R.string.you_are_party_host), fontStyle = FontStyle.Italic, modifier = Modifier.padding(16.dp))
                        }
                    }
                    else UserSurfaceCard(
                        user = party.host,
                        isFriend = party.host.isFriend,
                        country = countryData.nameFromCode(party.host.countryCode),
                        snackbar = snackbar,
                        onClick = { onNavigateToFriend(party.host) }
                    )
                }
                HorizontalDivider()
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
                HorizontalDivider()
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
                    if (party.self.status != SPartyMember.Status.Host) {
                        // Show self first, highlighted
                        Surface(tonalElevation = 2.dp) {
                            PartyMember(party.self, countryData, snackbar, currency = party.currency, showYou = true)
                        }
                        // Then the other members
                        for (member in party.members) Surface {
                            PartyMember(member, countryData, snackbar, currency = party.currency)
                        }
                    } // If host, entries are clickable to view transfer details
                    else {
                        for (member in party.members) {
                            if (member.transfer != null) Surface(onClick = { onNavigateToTransfer(member.transfer!!) }) {
                                PartyMember(member, countryData, snackbar, currency = party.currency, onNavigateToFriend = onNavigateToFriend)
                            }
                            else Surface {
                                PartyMember(member, countryData, snackbar, currency = party.currency, onNavigateToFriend = onNavigateToFriend)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PartyMember(
    member: SPartyMember,
    countryData: SCountries?,
    snackbar: SnackbarHostState,
    currency: Currency,
    showYou: Boolean = false,
    onNavigateToFriend: ((SPublicUserBase) -> Unit)? = null
) {
    var showAddFriend by remember { mutableStateOf(false) }
    val addFriendState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showAddFriend) ModalBottomSheet(sheetState = addFriendState, onDismissRequest = { showAddFriend = false }) {
        Column(
            modifier = Modifier
                .padding(WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserCard(
                user = member.profile,
                country = countryData.nameFromCode(member.profile.countryCode),
                snackbar = snackbar,
                isFriend = member.profile.isFriend
            )
            HorizontalDivider()
            TextButton(onClick = { scope.launch { addFriendState.hide(); showAddFriend = false} }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.close))
            }
        }
    }

    Row(
        modifier = Modifier.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            image = member.profile.avatar,
            size = 48.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable {
                    if (member.profile.isFriend) onNavigateToFriend?.invoke(member.profile)
                    else showAddFriend = true
                }
        )
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
                    SPartyMember.Status.Cancelled -> MaterialTheme.customColors.red
                    SPartyMember.Status.Accepted -> MaterialTheme.customColors.green
                    SPartyMember.Status.Host -> throw IllegalStateException("Host should not be in party members")
                }
            )
            Text(
                text = stringResource(
                    @Suppress("KotlinConstantConditions")
                    when (member.status) {
                        SPartyMember.Status.Pending -> R.string.status_pending
                        SPartyMember.Status.Cancelled -> R.string.status_declined
                        SPartyMember.Status.Accepted -> R.string.status_accepted
                        SPartyMember.Status.Host -> throw IllegalStateException("Host should not be in party members")
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
        ViewPartyScreen(onDismiss = {}, partyID = 1, onNavigateToFriend = {}, onNavigateToTransfer = {})
    }
}

@Preview
@Composable
private fun PartyInformationScreenPreviewDark() {
    AppTheme {
        ViewPartyScreen(onDismiss = {}, partyID = 1, onNavigateToFriend = {}, onNavigateToTransfer = {})
    }
}