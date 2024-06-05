package ro.bankar.app.ui.main

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handle
import ro.bankar.app.ui.components.ButtonRow
import ro.bankar.app.ui.components.ComboBox
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.VerifiableState
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.rString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SCreditData
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SuccessResponse
import java.text.DecimalFormat

class NewBankAccountModel : ViewModel() {
    var accountType by mutableStateOf(SBankAccountType.Debit)
    var name = verifiableStateOf("") { newName ->
        if (newName.isBlank()) return@verifiableStateOf null
        SNewBankAccount.nameLengthRange.let {
            if (newName.trim().length in it) null
            else getString(R.string.invalid_bank_account_name, it.first, it.last)
        }
    }
    var color = mutableIntStateOf(0)
    var currency = verifiableStateOf(Currency.ROMANIAN_LEU, R.string.no_credit_for_currency) {
        accountType != SBankAccountType.Credit || currencyCreditData != null
    }

    var creditData by mutableStateOf<List<SCreditData>?>(null)
    val currencyCreditData: SCreditData? by derivedStateOf { creditData?.find { it.currency == currency.value } }
    var creditAmount = verifiableStateOf("", R.string.invalid_amount) {
        val data = currencyCreditData
        val amount = it.toDoubleOrNull()
        data != null && amount != null && amount in data.amountRange
    }

    var isLoading by mutableStateOf(false)
    fun onCreate(context: Context, repository: Repository, snackBar: SnackbarHostState, onDismiss: () -> Unit) {
        name.check(context)
        currency.check(context)
        creditAmount.check(context)
        if (!name.verified || !currency.verified || (accountType == SBankAccountType.Credit && !creditAmount.verified)) return

        isLoading = true
        viewModelScope.launch {
            repository.sendCreateAccount(
                SNewBankAccount(accountType, name.value.trim().ifEmpty { context.getString(R.string.s_account, context.getString(accountType.rString)) },
                    color.intValue, currency.value, creditAmount.value.toDoubleOrNull() ?: 0.0)
            ).handle(this, snackBar, context) {
                when (it) {
                    SuccessResponse -> {
                        repository.accounts.emitNow()
                        onDismiss()
                        null
                    }
                    is InvalidParamResponse -> context.getString(R.string.unable_new_bank_account, it.param)
                    else -> context.getString(R.string.unknown_error)
                }
            }
            isLoading = false
        }
    }
}

@Composable
fun NewBankAccountScreen(onDismiss: () -> Unit) {
    val model = viewModel<NewBankAccountModel>()
    val snackBar = remember { SnackbarHostState() }

    // Load credit data on composition
    val context = LocalContext.current
    val repository = LocalRepository.current
    LaunchedEffect(true) { repository.creditData.collect { model.creditData = it } }

    NavScreen(
        onDismiss,
        title = R.string.open_bank_account,
        snackbar = snackBar,
        isLoading = model.creditData == null || model.isLoading,
        confirmText = R.string.open_account,
        confirmEnabled = model.accountType != SBankAccountType.Credit || model.currencyCreditData != null,
        onConfirm = {
            model.onCreate(context, repository, snackBar, onDismiss)
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.what_account_type))
            ButtonRow(
                currentValue = model.accountType,
                onValueChange = { model.accountType = it; model.currency.check(context, true) },
                values = SBankAccountType.entries,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(it.rString)
                )
            }
            ComboBox(
                selectedItemText = model.currency.value.code,
                onSelectItem = { model.currency.value = it; model.currency.check(context) },
                label = R.string.currency,
                items = Currency.entries,
                fillWidth = true,
                isError = model.currency.hasError,
                supportingText = model.currency.error ?: ""
            ) { item, onClick ->
                DropdownMenuItem(text = { Text(text = item.code) }, onClick)
            }
            // Display credit data and options
            val data = model.currencyCreditData
            AnimatedVisibility(visible = model.accountType == SBankAccountType.Credit && data != null) {
                val decimalFormat = remember { DecimalFormat("#.##") }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.credit_amount_range,
                            decimalFormat.format(data?.minAmount ?: 0.0),
                            decimalFormat.format(data?.maxAmount ?: 0.0),
                            model.currency.value.code
                        )
                    )
                    VerifiableField(model.creditAmount, label = R.string.credit_amount, type = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.credit_interest_is, data?.interest ?: 0.0),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            BankAccountPersonalisation(title = R.string.personalize_new_account, name = model.name,
                color = model.color, accountType = model.accountType)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewBankAccountPreview() {
    AppTheme {
        NewBankAccountScreen(onDismiss = {})
    }
}

@Composable
fun BankAccountPersonalisation(
    title: Int,
    name: VerifiableState<String>,
    color: MutableState<Int>,
    accountType: SBankAccountType,
    modifier: Modifier = Modifier
) = Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(text = stringResource(title))
    var accountNameFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    OutlinedTextField(
        value = if (accountNameFocused) name.value else stringResource(R.string.s_account, stringResource(accountType.rString)),
        onValueChange = { name.value = it },
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                accountNameFocused = it.isFocused
                if (it.isFocused) name.clearError()
                else if (name.value.isNotEmpty()) name.check(context)
            },
        singleLine = true,
        label = { Text(text = stringResource(R.string.account_name)) },
        isError = name.hasError,
        supportingText = { Text(text = name.error ?: "") }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center
    ) {
        for ((index, c) in accountColors.withIndex()) {
            OutlinedIconToggleButton(
                checked = index == color.value,
                onCheckedChange = { color.value = index },
                colors = IconButtonDefaults.outlinedIconToggleButtonColors(checkedContainerColor = c.copy(alpha = .4f)),
                border = null
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(c, CircleShape)
                )
            }
        }
    }
}