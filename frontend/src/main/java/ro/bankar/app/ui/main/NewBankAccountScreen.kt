package ro.bankar.app.ui.main

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.request.url
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.data.ktorClient
import ro.bankar.app.data.safeGet
import ro.bankar.app.ui.components.ButtonRow
import ro.bankar.app.ui.components.ComboBox
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.rString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SCreditData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.StatusResponse
import java.text.DecimalFormat

class NewBankAccountModel : ViewModel() {
    var accountType by mutableStateOf(SBankAccountType.Debit)
    var name = verifiableStateOf("") { newName ->
        SNewBankAccount.nameLengthRange.let {
            if (newName.trim().length in it) null
            else getString(R.string.invalid_bank_account_name, it.first, it.last)
        }
    }
    var color by mutableStateOf(0)
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

    tailrec suspend fun loadCreditData(snackBar: SnackbarHostState, context: Context) {
        val r = ktorClient.safeGet<List<SCreditData>, StatusResponse> { url("data/credit.json") }
        if (r !is SafeStatusResponse.Success) {
            snackBar.showSnackbar(context.getString(R.string.connection_error), context.getString(R.string.retry))
            loadCreditData(snackBar, context)
        } else creditData = r.result
    }

    var isLoading by mutableStateOf(false)
    fun onCreate(context: Context, repository: Repository, snackBar: SnackbarHostState, onDismiss: () -> Unit) {
        name.check(context)
        currency.check(context)
        creditAmount.check(context)
        if (!name.verified || !currency.verified || (accountType == SBankAccountType.Credit && !creditAmount.verified)) return

        isLoading = true
        viewModelScope.launch {
            val result = repository.sendCreateAccount(
                SNewBankAccount(accountType, name.value, color, currency.value, creditAmount.value.toDoubleOrNull() ?: 0.0)
            )
            isLoading = false
            when (result) {
                is SafeStatusResponse.Fail -> snackBar.showSnackbar(context.getString(R.string.unable_new_bank_account, result.s.param))
                is SafeStatusResponse.InternalError -> snackBar.showSnackbar(context.getString(R.string.connection_error), withDismissAction = true)
                is SafeStatusResponse.Success -> {
                    onDismiss()
                    repository.accounts.requestEmit(true)
                }
            }
        }
    }
}

@Composable
fun NewBankAccountScreen(onDismiss: () -> Unit) {
    val model = viewModel<NewBankAccountModel>()
    val snackBar = remember { SnackbarHostState() }

    // Load credit data on composition
    val context = LocalContext.current
    LaunchedEffect(true) {
        model.name.value = context.getString(R.string.bank_account)
        model.loadCreditData(snackBar, context)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBar) }
    ) { contentPadding ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)) {
            Surface(color = MaterialTheme.colorScheme.secondary) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.ArrowBack, stringResource(R.string.back), modifier = Modifier.size(32.dp))
                    }
                    Text(
                        text = stringResource(R.string.open_bank_account),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            LoadingOverlay(isLoading = model.creditData == null || model.isLoading) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.what_account_type))
                    ButtonRow(
                        currentValue = model.accountType,
                        onValueChange = { model.accountType = it; model.currency.check(context, true) },
                        values = SBankAccountType.values().toList(),
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
                        items = Currency.values().toList(),
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
                        Text(
                            text = stringResource(
                                R.string.credit_amount_range,
                                decimalFormat.format(data?.minAmount),
                                decimalFormat.format(data?.maxAmount),
                                model.currency.value.code
                            )
                        )
                        VerifiableField(model.creditAmount, label = R.string.credit_amount, type = KeyboardType.Decimal, modifier = Modifier.fillMaxWidth())
                        Text(text = stringResource(R.string.credit_interest_is, data?.interest ?: 0.0), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(text = stringResource(R.string.personalize_new_account))
                    VerifiableField(model.name, R.string.account_name, KeyboardType.Text, modifier = Modifier.fillMaxWidth(), isLast = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for ((index, color) in accountColors.withIndex()) {
                            OutlinedIconToggleButton(
                                checked = index == model.color,
                                onCheckedChange = { model.color = index },
                                colors = IconButtonDefaults.outlinedIconToggleButtonColors(checkedContainerColor = color.copy(alpha = .4f)),
                                border = null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp), horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                val repository = LocalRepository.current
                Button(
                    onClick = { model.onCreate(context, repository, snackBar, onDismiss) },
                    enabled = model.accountType != SBankAccountType.Credit || model.currencyCreditData != null
                ) {
                    Text(text = stringResource(R.string.open_account))
                }
            }
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