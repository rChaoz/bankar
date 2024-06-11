package ro.bankar.app.ui.main

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.trimmedLength
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.info.InfoDialog
import com.maxkeppeler.sheets.info.models.InfoBody
import com.maxkeppeler.sheets.info.models.InfoSelection
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.ui.amountColor
import ro.bankar.app.ui.components.AccountAmountInput
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.LabeledIconButton
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme

class BankCardScreenModel(private val repository: Repository, private val accountID: Int, private val cardID: Int) : ViewModel() {
    val cardData = repository.card(accountID, cardID)
    val newCardName = verifiableStateOf("") {
        when {
            it.length < 2 -> getString(R.string.too_short)
            it.length > 30 -> getString(R.string.too_long)
            else -> null
        }
    }
    var showRenameDialog by mutableStateOf(false)

    val newLimit = mutableStateOf("")
    var showChangeLimitDialog by mutableStateOf(false)

    var isLoadingChangeLimit by mutableStateOf(false)
    var isLoadingResetLimit by mutableStateOf(false)

    val snackbar = SnackbarHostState()

    fun onRename(context: Context) {
        newCardName.check(context)
        if (!newCardName.verified) return
        isLoadingChangeLimit = true
        viewModelScope.launch {
            repository.sendUpdateCard(accountID, cardID, newCardName.value, -1.0).handleSuccess(context) {
                cardData.emitNow()
                repository.account(accountID).requestEmit()
                showRenameDialog = false
            }
            isLoadingChangeLimit = false
        }
    }

    fun onChangeLimit(context: Context, clear: Boolean) {
        isLoadingChangeLimit = true
        viewModelScope.launch {
            repository.sendUpdateCard(accountID, cardID, "", if (clear) 0.0 else newLimit.value.toDouble()).handleSuccess(context) {
                cardData.emitNow()
                repository.account(accountID).requestEmit()
                showChangeLimitDialog = false
            }
            isLoadingChangeLimit = false
        }
    }

    fun onResetLimit(context: Context) {
        isLoadingResetLimit = true
        viewModelScope.launch {
            repository.sendResetCardLimit(accountID, cardID).handleSuccess(viewModelScope, snackbar, context) {
                cardData.emitNow()
            }
            isLoadingResetLimit = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankCardScreen(onDismiss: () -> Unit, navigation: NavHostController, accountID: Int, cardID: Int) {
    val repository = LocalRepository.current
    val context = LocalContext.current
    val model = viewModel { BankCardScreenModel(repository, accountID, cardID) }
    val data = model.cardData.collectAsState(null).value
    val shimmer = rememberShimmer(ShimmerBounds.Window)

    // Show details dialog
    var showDetails by remember { mutableStateOf(false) }
    val showDetailsDialog = rememberUseCaseState()
    InfoDialog(
        showDetailsDialog,
        header = Header.Default(stringResource(R.string.are_you_sure)),
        body = InfoBody.Default(stringResource(R.string.are_you_sure_show_card_details)),
        selection = InfoSelection(onPositiveClick = { showDetails = true })
    )

    // Rename card dialog
    BottomDialog(
        model.showRenameDialog,
        onDismissRequest = { model.showRenameDialog = false },
        confirmButtonEnabled = model.newCardName.value.isNotEmpty(),
        confirmButtonText = R.string.rename,
        onConfirmButtonClick = { model.onRename(context) }) {
        LoadingOverlay(model.isLoadingChangeLimit) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.rename_card), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 12.dp))
                VerifiableField(
                    model.newCardName,
                    R.string.card_name,
                    KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth(),
                    isLast = true,
                    onDone = { model.onRename(context) },
                    capitalization = KeyboardCapitalization.Sentences,
                    placeholder = R.string.my_card
                )
            }
        }
    }

    // Change limit dialog
    BottomDialog(
        model.showChangeLimitDialog,
        onDismissRequest = { model.showChangeLimitDialog = false },
        buttonBar = {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = { model.showChangeLimitDialog = false }, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = { model.onChangeLimit(context, true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
                ) {
                    Text(text = stringResource(R.string.no_limit))
                }
                Button(
                    onClick = { model.onChangeLimit(context, false) },
                    modifier = Modifier.weight(1f),
                    enabled = model.newLimit.value.toDoubleOrNull() != null
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        }
    ) {
        LoadingOverlay(model.isLoadingChangeLimit) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.change_limit),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                AccountAmountInput(
                    account = null,
                    currency = data!!.currency,
                    amount = model.newLimit,
                    showRemainingBalance = false
                )
            }
        }
    }

    NavScreen(onDismiss, R.string.card, snackbar = model.snackbar) {
        Column {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .animateContentSize(), verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (data == null) {
                            Box(
                                modifier = Modifier
                                    .size(150.dp, 28.dp)
                                    .grayShimmer(shimmer)
                            )
                            Box(
                                modifier = Modifier
                                    .size(220.dp, 25.dp)
                                    .grayShimmer(shimmer)
                            )
                        } else {
                            Text(text = data.name, style = MaterialTheme.typography.headlineSmall)
                            if (showDetails) {
                                Text(
                                    text = formatCardNumber(data.number!!),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "%02d/%02d".format(data.expirationMonth!!.value, data.expirationYear!! % 100),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = data.cvv!!,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else Text(
                                text = stringResource(R.string.card_last_4_template, data.lastFour),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(painter = painterResource(R.drawable.baseline_card_24), contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }

            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(.7f)
                    .align(Alignment.CenterHorizontally)
            ) {
                LabeledIconButton(
                    onClick = {
                        if (showDetails) showDetails = false
                        else showDetailsDialog.show()
                    },
                    text = if (showDetails) R.string.hide_details else R.string.show_details,
                    enabled = data != null,
                ) {
                    Icon(
                        painter = painterResource(if (showDetails) R.drawable.baseline_visibility_off_24 else R.drawable.baseline_visibility_24),
                        contentDescription = null
                    )
                }
                LabeledIconButton(
                    onClick = { model.showRenameDialog = true; model.newCardName.value = data!!.name },
                    text = R.string.rename,
                    enabled = data != null
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                }
            }

            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.spending),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    if (data == null) {
                        Row {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .height(90.dp)
                                        .weight(1f)
                                        .padding(horizontal = 20.dp)
                                        .grayShimmer(shimmer)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stringResource(R.string.spent_this_month),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.limit),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.change_limit),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.currency.format(data.limitCurrent),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .weight(1f),
                                color = data.limitCurrent.amountColor
                            )
                            Text(
                                text = if (data.limit != 0.0) data.currency.format(data.limit) else stringResource(R.string.no_limit),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .weight(1f)
                            )
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                IconButton(onClick = { model.showChangeLimitDialog = true; model.newLimit.value = data.limit.toString().removeSuffix(".0") }) {
                                    Icon(imageVector = Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = { model.onResetLimit(context) },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            enabled = !model.isLoadingResetLimit && data.limitCurrent != 0.0
                        ) {
                            Text(text = stringResource(R.string.reset_limit))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Surface(modifier = Modifier.padding(horizontal = 12.dp), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.recent_activity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (data == null) RecentActivityShimmer()
                    else if (data.transactions.isEmpty()) item {
                        InfoCard(R.string.no_recent_card_activity, tonalElevation = 0.dp)
                    }
                    else RecentActivityContent(emptyList(), data.transactions, emptyList(), navigation)
                }
            }
        }
    }
}

private fun formatCardNumber(number: String) = buildString {
    repeat(4) {
        append(number.substring(it * 4, it * 4 + 4))
        append("  ")
    }
    setLength(trimmedLength())
}

@Preview
@Composable
private fun BankCardScreenPreview() {
    AppTheme {
        BankCardScreen(onDismiss = {}, rememberMockNavController(), 1, 1)
    }
}