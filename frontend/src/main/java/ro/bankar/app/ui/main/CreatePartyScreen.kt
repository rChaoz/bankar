package ro.bankar.app.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.SafeResponse
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.data.collectRetrying
import ro.bankar.app.ui.components.AccountsComboBox
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.processNumberValue
import ro.bankar.app.ui.safeDecodeFromString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SFriend
import ro.bankar.model.SSendRequestMoney
import kotlin.math.min

enum class CreatePartyStep {
    AccountAndTotal, PickFriends, ChooseAmounts
}

class CreatePartyScreenModel : ViewModel() {
    var step by mutableStateOf(CreatePartyStep.AccountAndTotal)

    // Choose account and total step
    val account = mutableStateOf<SBankAccount?>(null)
    val total = verifiableStateOf("", R.string.invalid_amount) { it.toDoubleOrNull() != null }
    val note = verifiableStateOf("") {
        when {
            it.isBlank() -> getString(R.string.note_cannot_be_empty)
            it.trim().length > SSendRequestMoney.maxNoteLength -> getString(R.string.note_too_long)
            else -> null
        }
    }

    // Pick friends step
    var allFriends by mutableStateOf<List<SFriend>?>(null)
    var added by mutableStateOf(emptySet<SFriend>())

    // Amounts step
    var amounts by mutableStateOf(emptyMap<SFriend, String>())
    var isLoading by mutableStateOf(false)

    lateinit var onDismiss: () -> Unit
    lateinit var snackbar: SnackbarHostState

    fun onCreateParty(context: Context, repository: Repository) = viewModelScope.launch {
        isLoading = true
        val amounts = amounts.map { it.key.tag to (it.value.removeSuffix(".").toDoubleOrNull() ?: 0.0) }
        when (val r = repository.sendCreateParty(account.value!!.id, note.value, amounts)) {
            is SafeResponse.InternalError ->
                launch { snackbar.showSnackbar(context.getString(r.message), withDismissAction = true) }
            is SafeResponse.Fail -> {
                val param = Json.safeDecodeFromString<InvalidParamResponse>(r.body)?.param
                launch {
                    snackbar.showSnackbar(
                        message = if (param == null) context.getString(R.string.unknown_error) else context.getString(R.string.invalid_field, param),
                        withDismissAction = true
                    )
                }
            }
            is SafeResponse.Success -> {
                repository.recentActivity.emitNow()
                onDismiss()
            }
        }
        isLoading = false
    }
}

@Composable
fun CreatePartyScreen(onDismiss: () -> Unit, initialAmount: Double, account: Int) {
    // Create model
    val model = viewModel<CreatePartyScreenModel>()
    model.onDismiss = onDismiss
    model.snackbar = remember { SnackbarHostState() }

    // Get friends list
    val repository = LocalRepository.current
    LaunchedEffect(true) {
        if (initialAmount != 0.0) model.total.value = initialAmount.toString().let {
            val index = it.indexOf('.')
            if (index == -1) it else it.substring(0, min(it.length, index + 3))
        }
        launch { repository.friends.collectRetrying { model.allFriends = it } }
        if (account != -1) launch {
            repository.accounts.collectRetrying { accounts ->
                accounts.find { it.id == account }?.let { model.account.value = it }
                cancel()
            }
        }
    }
    // Back button should go back to previous step
    BackHandler(enabled = model.step != CreatePartyStep.AccountAndTotal) { model.step = CreatePartyStep.values()[model.step.ordinal - 1] }

    NavScreen(
        onDismiss = {
            if (model.isLoading) return@NavScreen
            if (model.step != CreatePartyStep.AccountAndTotal) model.step = CreatePartyStep.values()[model.step.ordinal - 1] else onDismiss()
        },
        title = R.string.create_party,
        snackbar = model.snackbar
    ) {
        AnimatedContent(
            targetState = model.step,
            label = "Party step animation",
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) slideInHorizontally { w -> w } with slideOutHorizontally { w -> -w }
                else slideInHorizontally { w -> -w } with slideOutHorizontally { w -> w }
            }
        ) {
            when (it) {
                CreatePartyStep.AccountAndTotal -> AccountAndTotalStep(model)
                CreatePartyStep.PickFriends -> PickFriendsStep(model)
                CreatePartyStep.ChooseAmounts -> ChooseAmountsStep(model)
            }
        }
    }
}

@Composable
fun AccountAndTotalStep(model: CreatePartyScreenModel) {
    val accounts by LocalRepository.current.accounts.collectAsStateRetrying()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountsComboBox(selectedAccount = model.account, accounts = accounts, pickText = R.string.choose_account_request, showBalance = true)
            Text(text = stringResource(R.string.enter_bill_amount))
            VerifiableField(
                model.total,
                label = R.string.bill_amount,
                type = KeyboardType.Decimal,
                modifier = Modifier.fillMaxWidth(),
                valueTransform = ::processNumberValue,
                leadingIcon = { Icon(painter = painterResource(R.drawable.baseline_money_24), contentDescription = null) },
                trailingIcon = if (model.account.value == null) null else {
                    { Text(text = model.account.value!!.currency.code, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp)) }
                },
            )
            VerifiableField(
                model.note,
                label = R.string.note,
                type = KeyboardType.Text,
                autoCorrect = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(painter = painterResource(R.drawable.baseline_note_24), contentDescription = null) },
                isLast = true
            )
        }

        Divider()
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = model.onDismiss, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(android.R.string.cancel))
            }
            val context = LocalContext.current
            Button(onClick = {
                model.total.check(context)
                model.note.check(context)
                if (model.total.verified && model.note.verified) model.step = CreatePartyStep.PickFriends
            }, modifier = Modifier.weight(1f), enabled = model.account.value != null) {
                Text(text = stringResource(R.string.next))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PickFriendsStep(model: CreatePartyScreenModel) {
    val tween = tween<IntOffset>(400)
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = model.added.isNotEmpty()) {
            LazyRow(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                for (friend in model.added) item(key = "added-${friend.tag}") {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .width(80.dp)
                            .animateItemPlacement(tween),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box {
                            Avatar(image = friend.avatar, size = 48.dp)
                            FilledIconButton(
                                onClick = { model.added -= friend },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .offset(8.dp, (-8).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.remove),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = friend.firstName,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Normal
                        )
                    }
                }
            }
        }
        Divider()
        LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), modifier = Modifier.weight(1f)) {
            if (model.allFriends == null) items(5, key = { "shimmer-$it" }) {
                val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
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
                }
            } else items(model.allFriends!! - model.added, key = { "friend-${it.tag}" }) {
                Surface(onClick = { model.added += it }, tonalElevation = 1.dp, modifier = Modifier.animateItemPlacement(tween)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(image = it.avatar, size = 48.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = it.fullName,
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                            )
                            Text(text = "@${it.tag}", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
        Divider()
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { model.step = CreatePartyStep.AccountAndTotal }, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.back))
            }
            Button(onClick = { model.step = CreatePartyStep.ChooseAmounts }, modifier = Modifier.weight(1f), enabled = model.added.isNotEmpty()) {
                Text(text = stringResource(R.string.next))
            }
        }
    }
}

@Composable
private fun ChooseAmountsStep(model: CreatePartyScreenModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.total_amount))
            Text(text = model.account.value!!.currency.format(model.total.value.toDouble()), fontWeight = FontWeight.Bold)
        }
        LoadingOverlay(isLoading = model.isLoading, modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                for (friend in model.added) item(key = "friend-${friend.tag}") {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(image = friend.avatar, size = 48.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = friend.firstName,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                            )
                            Text(text = "@${friend.tag}", style = MaterialTheme.typography.titleSmall)
                        }
                        OutlinedTextField(
                            value = model.amounts[friend] ?: "",
                            onValueChange = { value ->
                                processNumberValue(value)?.let { model.amounts += friend to it }
                            },
                            modifier = Modifier.weight(.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }
        }
        Divider()
        val sum = model.amounts.values.sumOf { it.removeSuffix(".").toDoubleOrNull() ?: 0.0 }
        Row(
            modifier = Modifier
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 2.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.friend_amounts_total))
            Text(
                text = model.account.value!!.currency.format(sum),
                fontWeight = FontWeight.Bold
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 2.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.remaining_amount))
            Text(
                text = model.account.value!!.currency.format(model.total.value.toDouble() - sum),
                fontWeight = FontWeight.Bold
            )
        }
        Divider()
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { model.step = CreatePartyStep.PickFriends }, modifier = Modifier.weight(1f), enabled = !model.isLoading) {
                Text(text = stringResource(R.string.back))
            }
            val focusManager = LocalFocusManager.current
            val context = LocalContext.current
            val repository = LocalRepository.current
            Button(
                onClick = {
                    model.onCreateParty(context, repository)
                    focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f),
                enabled = !model.isLoading && sum <= model.total.value.toDouble()
                        && model.added.all { model.amounts[it]?.removeSuffix(".")?.toDoubleOrNull()?.let { n -> n > 0.0 } ?: false }
            ) {
                Text(text = stringResource(R.string.finish))
            }
        }
    }
}

@Preview
@Composable
private fun CreatePartyScreenPreview() {
    AppTheme {
        CreatePartyScreen(onDismiss = {}, 0.0, -1)
    }
}