package ro.bankar.app.ui.main.home

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.SafeResponse
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.format
import ro.bankar.app.ui.main.BankAccountPersonalisation
import ro.bankar.app.ui.rString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.accountColors
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SNewBankAccount
import ro.bankar.util.formatIBAN

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BankAccount(data: SBankAccount, onNavigate: () -> Unit) = Box { // To prevent parent Column from adding another spacer for the bottom sheet(s)
    var showCustomiseSheet by remember { mutableStateOf(false) }
    // Menu sheet
    val scope = rememberCoroutineScope()
    var showMenuSheet by remember { mutableStateOf(false) }
    val menuSheetState = rememberModalBottomSheetState(true)
    if (showMenuSheet) ModalBottomSheet(sheetState = menuSheetState, onDismissRequest = { showMenuSheet = false }) {
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .padding(WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues())
        ) {
            val iban = formatIBAN(data.iban)
            // Share IBAN
            val shareIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, iban)
                type = "text/plain"
            }, stringResource(R.string.share_iban))
            val context = LocalContext.current
            Surface(onClick = {
                scope.launch { menuSheetState.hide(); showMenuSheet = false }
                context.startActivity(shareIntent)
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.share_iban))
                        Text(text = iban, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Print statements
            Surface(onClick = { /* TODO */ }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.baseline_file_24), contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.create_statement))
                        Text(
                            text = stringResource(R.string.download_pdf),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Customise
            Surface(onClick = {
                scope.launch { menuSheetState.hide(); showMenuSheet = false }
                showCustomiseSheet = true
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.baseline_palette_24), contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.customize))
                        Text(
                            text = stringResource(R.string.change_name_and_color),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // Customise sheet
    var isCustomiseLoading by remember { mutableStateOf(false) }
    val customiseSheetState = rememberModalBottomSheetState(true) { !isCustomiseLoading }
    if (showCustomiseSheet) ModalBottomSheet(sheetState = customiseSheetState, onDismissRequest = { showCustomiseSheet = false }) {
        LoadingOverlay(isCustomiseLoading) {
            Column(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .padding(WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val name = remember {
                    verifiableStateOf(data.name) { newName ->
                        if (newName.isBlank()) return@verifiableStateOf null
                        SNewBankAccount.nameLengthRange.let {
                            if (newName.trim().length in it) null
                            else getString(R.string.invalid_bank_account_name, it.first, it.last)
                        }
                    }
                }
                val color = remember { mutableStateOf(data.color) }

                BankAccountPersonalisation(
                    title = R.string.personalize_your_account,
                    name,
                    color,
                    accountType = data.type,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Divider()
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(modifier = Modifier.weight(1f), onClick = {
                        scope.launch { customiseSheetState.hide(); showCustomiseSheet = false }
                    }) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                    val context = LocalContext.current
                    val repository = LocalRepository.current
                    Button(modifier = Modifier.weight(1f), onClick = {
                        isCustomiseLoading = true
                        scope.launch {
                            when (val r = repository.sendCustomiseAccount(data.id, name.value.trim(), color.value)) {
                                is SafeResponse.InternalError -> Toast.makeText(context, r.message, Toast.LENGTH_SHORT).show()
                                is SafeResponse.Fail -> Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                                is SafeResponse.Success -> {
                                    repository.accounts.emitNow()
                                    showCustomiseSheet = false
                                }
                            }
                            isCustomiseLoading = false
                        }
                    }) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            }
        }
    }

    HomeCard(
        onClick = onNavigate,
        title = data.name,
        icon = { Icon(painter = painterResource(R.drawable.bank_account), contentDescription = stringResource(R.string.bank_account)) },
        color = accountColors[data.color.coerceIn(accountColors.indices)]
    ) {
        Text(
            text = stringResource(data.type.rString),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Amount(amount = data.balance, currency = data.currency, textStyle = MaterialTheme.typography.headlineMedium)
            if (data.type == SBankAccountType.Credit) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(R.string.limit_is), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    Text(text = data.currency.format(data.limit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.customColors.red)
                }
            }
        }
        Divider(thickness = 2.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { /*TODO*/ }) {
                Text(text = stringResource(R.string.new_payment))
            }
            IconButton(onClick = { showMenuSheet = true }) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = stringResource(androidx.compose.ui.R.string.dropdown_menu))
            }
        }
    }
}

@Composable
fun BankAccountShimmer(shimmer: Shimmer) {
    HomeCard(
        title = stringResource(R.string.bank_account),
        icon = {
            Icon(
                painter = painterResource(R.drawable.bank_account),
                contentDescription = stringResource(R.string.bank_account),
                modifier = Modifier.shimmer(shimmer)
            )
        },
        shimmer = shimmer
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(200.dp, 32.dp)
                .shimmer(shimmer)
                .background(MaterialTheme.customColors.green.copy(alpha = .6f))
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

private val sampleAccount = SBankAccount(
    id = 0,
    iban = "RO24RBNK1921081333473500",
    type = SBankAccountType.Debit,
    balance = 1235.22,
    limit = 0.0,
    currency = Currency.ROMANIAN_LEU,
    name = "Debit Account",
    color = 0,
    interest = 0.0,
)

@Preview
@Composable
private fun BankAccountPreview() {
    AppTheme {
        BankAccount(sampleAccount, onNavigate = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BankAccountPreviewDark() {
    AppTheme {
        BankAccount(sampleAccount, onNavigate = {})
    }
}

@Preview
@Composable
fun BankAccountShimmerPreview() {
    AppTheme {
        BankAccountShimmer(rememberShimmer(shimmerBounds = ShimmerBounds.Window))
    }
}