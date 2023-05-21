package ro.bankar.app.ui.main.friend

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.state.StateDialog
import com.maxkeppeler.sheets.state.models.State
import com.maxkeppeler.sheets.state.models.StateConfig
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.SafeResponse
import ro.bankar.app.data.collectRetrying
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.components.ComboBox
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.safeDecodeFromString
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SPublicUser
import ro.bankar.model.SSendRequestMoney
import kotlin.math.max
import kotlin.math.min

class SendRequestMoneyModel : ViewModel() {
    var accounts by mutableStateOf<List<SBankAccount>?>(null)
    var selectedAccount by mutableStateOf<SBankAccount?>(null)
    var amount by mutableStateOf("0")
    var note = verifiableStateOf("", R.string.message_too_long) { it.trim().length <= SSendRequestMoney.maxNoteLength }

    var isLoading by mutableStateOf(false)

    var result by mutableStateOf<Int?>(null)
    lateinit var onDismiss: () -> Unit
    var resultState by mutableStateOf(UseCaseState(onDismissRequest = { onDismiss() }))

    fun onSend(context: Context, snackBar: SnackbarHostState, recipient: String, repository: Repository, requesting: Boolean) {
        note.check(context)
        val account = selectedAccount
        if (!note.verified || account == null) return
        val amount = amount.toDoubleOrNull()
        if (amount == null) {
            viewModelScope.launch { snackBar.showSnackbar(context.getString(R.string.invalid_amount), withDismissAction = true) }
            return
        }
        isLoading = true
        viewModelScope.launch {
            when (
                val r = if (requesting) repository.sendTransferRequest(recipient, account, amount, note.value.trim())
                else repository.sendTransfer(recipient, account, amount, note.value.trim())
            ) {
                is SafeResponse.InternalError -> launch { snackBar.showSnackbar(context.getString(r.message), withDismissAction = true) }
                is SafeResponse.Success -> {
                    this@SendRequestMoneyModel.result =
                        if (r.result.status == "sent") R.string.money_sent_successfully
                        else if (!requesting) R.string.money_sent_request
                        else R.string.request_sent
                    resultState.show()
                    repository.accounts.requestEmit()
                    repository.recentActivity.requestEmit()
                }
                is SafeResponse.Fail -> {
                    val invalidParam = Json.safeDecodeFromString<InvalidParamResponse>(r.body)
                    launch {
                        snackBar.showSnackbar(
                            if (invalidParam != null) context.getString(R.string.invalid_field, invalidParam.param)
                            else context.getString(R.string.unknown_error),
                            withDismissAction = true
                        )
                    }
                }
            }
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendRequestMoneyScreenBase(onDismiss: () -> Unit, user: SPublicUser, requesting: Boolean) {
    // Get data
    val repository = LocalRepository.current
    val model = viewModel<SendRequestMoneyModel>()
    model.onDismiss = onDismiss
    LaunchedEffect(key1 = true) {
        repository.accounts.collectRetrying {
            model.accounts = it
            if (model.selectedAccount == null) model.selectedAccount = it.firstOrNull()
        }
    }

    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    val textMod = if (model.accounts == null) Modifier.shimmer(shimmer) else Modifier
    val snackBar = remember { SnackbarHostState() }

    // Result dialog
    StateDialog(state = model.resultState, config = StateConfig(State.Success(labelText = model.result?.let { stringResource(it) }, postView = {
        TextButton(
            onClick = onDismiss
        ) {
            Text(text = stringResource(android.R.string.ok))
        }
    })), properties = DialogProperties())

    FriendScreen(onDismiss, user, isLoading = model.isLoading, snackBar = snackBar) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = stringResource(if (requesting) R.string.choose_account_request else R.string.choose_account_send), modifier = textMod)
            val accounts = model.accounts
            if (accounts == null) Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .grayShimmer(shimmer)
            )
            else ComboBox(
                selectedItemText = model.selectedAccount?.name ?: stringResource(R.string.select_an_account),
                onSelectItem = { model.selectedAccount = it },
                items = accounts,
                fillWidth = true
            ) { item, onClick ->
                DropdownMenuItem(text = {
                    Column {
                        Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = item.currency.format(item.balance),
                            color = item.balance.amountColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }, onClick)
            }
            val account = model.selectedAccount
            if (account != null) {
                Row {
                    Text(text = stringResource(if (account.type == SBankAccountType.Credit) R.string.available_credit else R.string.current_balance))
                    Text(text = account.currency.format(account.spendable), color = MaterialTheme.customColors.green, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            // Centered text field
            if (account != null) Column {
                BasicTextField(
                    value = model.amount,
                    onValueChange = { value ->
                        // Remove invalid characters
                        val num = value.filter { it.isDigit() || it == '.' }
                        // Reject invalid inputs
                        if (!(num.isEmpty() || num.removeSuffix(".").toIntOrNull() != null || num.toDoubleOrNull() != null)
                            || num.count { it == '.' } > 1
                        ) return@BasicTextField
                        // Remove leading zeros, only allow up to 10 digits before decimal and 2 digits after
                        val decimal = num.indexOf('.')
                        val before = if (decimal == -1) num else num.substring(0, decimal)
                        val after = if (decimal == -1) "" else num.substring(decimal + 1, min(decimal + 3, num.length))
                        if (before.length > 10 || after.length > 2) return@BasicTextField
                        model.amount = before.substring(max(0, before.indexOfFirst { it != '0' })) + (if (decimal != -1) "." else "") + after
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                ) { field ->
                    SubcomposeLayout { constraints ->
                        val textWidth = subcompose("text") {
                            Text(text = model.amount, style = MaterialTheme.typography.headlineLarge)
                        }[0].measure(constraints.copy(minWidth = 0)).width
                        val content = subcompose("textField", field)[0].measure(constraints)

                        val pad = 20.dp.roundToPx()
                        val outline = subcompose("outline") {
                            Box(
                                modifier = Modifier
                                    .alpha(.5f)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            )
                        }[0].measure(Constraints.fixed(textWidth + pad, content.height + pad))
                        layout(constraints.maxWidth, content.height + pad) {
                            content.place(constraints.maxWidth / 2 - textWidth / 2, pad / 2)
                            outline.place(constraints.maxWidth / 2 - textWidth / 2 - pad / 2, 0)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(text = account.currency.code, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.headlineSmall)
                }
            }
            else Column {
                Box(
                    modifier = Modifier
                        .size(200.dp, 50.dp)
                        .align(Alignment.CenterHorizontally)
                        .grayShimmer(shimmer)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp, 40.dp)
                        .align(Alignment.CenterHorizontally)
                        .grayShimmer(shimmer)
                )
            }
            if (account != null && !requesting) {
                Column(modifier = Modifier.align(Alignment.CenterHorizontally), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(if (account.type == SBankAccountType.Credit) R.string.remaining_credit else R.string.remaining_balance),
                        style = MaterialTheme.typography.labelMedium
                    )
                    model.amount.ifEmpty { "0" }.toDoubleOrNull()?.let {
                        val remaining = account.spendable - it
                        Text(text = account.currency.format(remaining), style = MaterialTheme.typography.labelMedium, color = remaining.amountColor)
                    } ?: Text(text = stringResource(R.string.error), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.customColors.red)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (account != null)
                VerifiableField(model.note, label = R.string.message, type = KeyboardType.Text, autoCorrect = true, modifier = Modifier.fillMaxWidth())
            else Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .grayShimmer(shimmer)
            )
            Spacer(modifier = Modifier.weight(1f))
            val context = LocalContext.current
            Button(
                onClick = { model.onSend(context, snackBar, user.tag, repository, requesting) },
                enabled = account != null && model.amount.toDoubleOrNull()?.let { it > 0 && it <= account.spendable } ?: false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(if (requesting) R.string.request_money else R.string.send_money))
            }
        }
    }
}