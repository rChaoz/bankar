package ro.bankar.app.ui.main

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.state.StateDialog
import com.maxkeppeler.sheets.state.models.State
import com.maxkeppeler.sheets.state.models.StateConfig
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handle
import ro.bankar.app.ui.components.AccountAmountInput
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.format
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.checkIBAN
import ro.bankar.banking.exchange
import ro.bankar.model.MAX_NOTE_LENGTH
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SuccessResponse

class TransferViewModel(private val onDismiss: () -> Unit, private val targetAccountID: Int?) : ViewModel() {
    var sourceAccount by mutableStateOf<SBankAccount?>(null)
    var targetAccount by mutableStateOf<SBankAccount?>(null)
    val amount = mutableStateOf("0")
    var isLoading by mutableStateOf(false)
    val recipientName = verifiableStateOf("", R.string.invalid_name) { it.length >= 5 && it.contains(' ') }
    val recipientIban = verifiableStateOf("", R.string.invalid_iban, ::checkIBAN)
    var note = verifiableStateOf("", R.string.message_too_long) { it.trim().length <= MAX_NOTE_LENGTH }

    val ready by derivedStateOf {
        if (sourceAccount == null || (targetAccountID != null && targetAccount == null)) false
        else {
            val amount = amount.value.toDoubleOrNull()
            amount != null && amount <= sourceAccount!!.spendable
        }
    }

    var result by mutableStateOf<Boolean?>(null)
    val resultState = UseCaseState(onDismissRequest = { onDismiss() })

    fun onTransfer(context: Context, snackbar: SnackbarHostState, repository: Repository) {
        note.check(context)
        if (targetAccountID == null) {
            recipientName.check(context)
            recipientIban.check(context)
        }
        if (!note.verified || (targetAccountID == null && (!recipientName.verified || !recipientIban.verified))) return
        val amount = amount.value.toDoubleOrNull()
        if (amount == null) {
            viewModelScope.launch { snackbar.showSnackbar(context.getString(R.string.invalid_amount), withDismissAction = true) }
            return
        }
        isLoading = true
        viewModelScope.launch {
            val r =
                if (targetAccountID != null) repository.sendOwnTransfer(sourceAccount!!, targetAccount!!, amount, note.value)
                else repository.sendExternalTransfer(sourceAccount!!, recipientIban.value, amount, note.value)
            r.handle(this, snackbar, context) { response ->
                when {
                    response == SuccessResponse -> {
                        result = true
                        resultState.show()
                        repository.accounts.requestEmit()
                        repository.recentActivity.requestEmit()
                        null
                    }
                    response is NotFoundResponse && targetAccountID == null -> {
                        result = false
                        resultState.show()
                        null
                    }
                    else -> context.getString(R.string.unknown_error)
                }
            }
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(onDismiss: () -> Unit, sourceAccountID: Int, targetAccountID: Int?, onNavigateToAccount: (SBankAccount) -> Unit) {
    val model = viewModel<TransferViewModel> { TransferViewModel(onDismiss, targetAccountID) }
    val snackBar = remember { SnackbarHostState() }

    val repository = LocalRepository.current
    val accountsFlow = repository.accounts
    LaunchedEffect(true) {
        launch { accountsFlow.map { accounts -> accounts.find { it.id == sourceAccountID } }.collect { model.sourceAccount = it } }
        launch { accountsFlow.map { accounts -> accounts.find { it.id == targetAccountID } }.collect { model.targetAccount = it } }
    }

    // Result dialog
    val statePostView: @Composable () -> Unit = {
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(android.R.string.ok))
        }
    }
    StateDialog(
        state = model.resultState,
        config = StateConfig(
            if (model.result == true) State.Success(stringResource(R.string.transfer_sent), postView = statePostView)
            else State.Failure(stringResource(if (model.result == null) R.string.unknown_error else R.string.invalid_iban), postView = statePostView)
        ),
        properties = DialogProperties()
    )

    val context = LocalContext.current
    val fromCurrency = model.sourceAccount?.currency
    val toCurrency = model.targetAccount?.currency
    val exchanging = fromCurrency != null && toCurrency != null && fromCurrency != toCurrency

    NavScreen(
        onDismiss,
        title = if (targetAccountID != null) (if (exchanging) R.string.exchange else R.string.own_transfer) else R.string.transfer,
        isLoading = model.isLoading,
        snackbar = snackBar,
        confirmText = R.string.transfer,
        confirmEnabled = model.ready,
        onConfirm = { model.onTransfer(context, snackBar, repository) },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TransferAccountCard(text = R.string.from_account, model.sourceAccount, onNavigateToAccount)
            if (targetAccountID != null) TransferAccountCard(text = R.string.to_account, model.targetAccount, onNavigateToAccount)
            else Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) {
                Text(text = stringResource(R.string.enter_recipient_details))
                VerifiableField(
                    model.recipientName,
                    R.string.full_name,
                    KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth(),
                    capitalization = KeyboardCapitalization.Words,
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) }
                )
                VerifiableField(
                    model.recipientIban,
                    R.string.iban,
                    KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth(),
                    valueTransform = { value -> value.uppercase().filter { it in 'A'..'Z' || it.isDigit() } },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.bank_account), contentDescription = null) }
                )

            }

            AccountAmountInput(
                modifier = Modifier.padding(top = 16.dp),
                account = model.sourceAccount,
                amount = model.amount,
                showRemainingBalance = true
            )

            val exchangeData = repository.exchangeData.collectAsState(null).value
            if (exchanging && exchangeData != null && fromCurrency != null && toCurrency != null) {
                val rate = exchangeData.exchange(fromCurrency, toCurrency, 1.0)
                if (rate != null) Row(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 6.dp)) {
                    Text(text = stringResource(R.string.conversion_rate))
                    Text(
                        text = "%s = %s".format(fromCurrency.format(1.0), toCurrency.format(rate)),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            VerifiableField(
                model.note,
                R.string.note,
                KeyboardType.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                leadingIcon = { Icon(painter = painterResource(R.drawable.baseline_note_24), contentDescription = null) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferScreenPreview() {
    AppTheme {
        TransferScreen(onDismiss = {}, sourceAccountID = 1, targetAccountID = null, onNavigateToAccount = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransferScreenPreviewDark() {
    AppTheme(useDarkTheme = true) {
        TransferScreen(onDismiss = {}, sourceAccountID = 1, targetAccountID = 2, onNavigateToAccount = {})
    }
}