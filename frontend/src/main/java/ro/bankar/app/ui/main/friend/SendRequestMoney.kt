package ro.bankar.app.ui.main.friend

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handleValue
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.components.AccountsComboBox
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.processNumberValue
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SPublicUserBase
import ro.bankar.model.SSendRequestMoney

class SendRequestMoneyModel : ViewModel() {
    var accounts by mutableStateOf<List<SBankAccount>?>(null)
    var selectedAccount = mutableStateOf<SBankAccount?>(null)
    var amount by mutableStateOf("0")
    var note = verifiableStateOf("", R.string.message_too_long) { it.trim().length <= SSendRequestMoney.maxNoteLength }

    var isLoading by mutableStateOf(false)

    var result by mutableStateOf<Int?>(null)
    lateinit var onDismiss: () -> Unit
    var resultState = UseCaseState(onDismissRequest = { onDismiss() })
    var noAccountsState = UseCaseState(onDismissRequest = { onDismiss() })

    fun onSend(context: Context, snackbar: SnackbarHostState, recipient: String, repository: Repository, requesting: Boolean) {
        note.check(context)
        val account = selectedAccount.value
        if (!note.verified || account == null) return
        val amount = amount.toDoubleOrNull()
        if (amount == null) {
            viewModelScope.launch { snackbar.showSnackbar(context.getString(R.string.invalid_amount), withDismissAction = true) }
            return
        }
        isLoading = true
        viewModelScope.launch {
            val r = if (requesting) repository.sendTransferRequest(recipient, account, amount, note.value.trim())
            else repository.sendTransfer(recipient, account, amount, note.value.trim())
            r.handleValue(this, snackbar, context) {
                this@SendRequestMoneyModel.result =
                    if (it == "sent") R.string.money_sent_successfully
                    else if (!requesting) R.string.money_sent_request
                    else R.string.request_sent
                resultState.show()
                repository.accounts.requestEmit()
                repository.recentActivity.requestEmit()
            }
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendRequestMoneyScreenBase(onDismiss: () -> Unit, user: SPublicUserBase, requesting: Boolean) {
    // Get data
    val repository = LocalRepository.current
    val model = viewModel<SendRequestMoneyModel>()
    model.onDismiss = onDismiss
    LaunchedEffect(key1 = true) {
        repository.accounts.collect {
            model.accounts = it
            if (it.isEmpty()) model.noAccountsState.show()
            if (model.selectedAccount.value == null) model.selectedAccount.value = it.firstOrNull()
        }
    }

    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    val snackBar = remember { SnackbarHostState() }

    // No accounts dialog
    StateDialog(state = model.noAccountsState, config = StateConfig(State.Failure(labelText = stringResource(R.string.no_accounts_send_receive), postView = {
        TextButton(
            onClick = onDismiss
        ) {
            Text(text = stringResource(android.R.string.ok))
        }
    })), properties = DialogProperties())

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
            AccountsComboBox(
                selectedAccount = model.selectedAccount,
                accounts = model.accounts,
                pickText = if (requesting) R.string.choose_account_request else R.string.choose_account_send,
                showBalance = true
            )
            Spacer(modifier = Modifier.height(40.dp))
            // Centered text field
            val account = model.selectedAccount.value
            if (account != null) Column {
                BasicTextField(
                    value = model.amount,
                    onValueChange = { value -> processNumberValue(value)?.let { model.amount = it } },
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
            else Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(200.dp, 50.dp)
                        .grayShimmer(shimmer)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp, 40.dp)
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
                enabled = account != null && model.amount.toDoubleOrNull()?.let { it > 0 && (requesting || it <= account.spendable) } ?: false,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(if (requesting) R.string.request_money else R.string.send_money))
            }
        }
    }
}