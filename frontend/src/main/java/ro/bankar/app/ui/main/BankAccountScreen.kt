package ro.bankar.app.ui.main

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handleValue
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.CardCard
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.MultiFab
import ro.bankar.app.ui.components.MultiFabItem
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.PagerTabs
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.main.home.Amount
import ro.bankar.app.ui.main.home.InfoCard
import ro.bankar.app.ui.main.home.topBorder
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.color
import ro.bankar.model.SBankAccount

class BankAccountScreenModel(private val repository: Repository, private val accountID: Int, private val onNavigateToCard: (Int) -> Unit) : ViewModel() {
    val accountData = repository.account(accountID)

    private var _showNewCardDialog by mutableStateOf(false)
    var showNewCardDialog
        set(value) { _showNewCardDialog = value; if (value) newCardName.value = "" }
        get() = _showNewCardDialog

    var newCardLoading by mutableStateOf(false)
    val newCardName = verifiableStateOf("") {
        when {
            it.length < 2 -> getString(R.string.too_short)
            it.length > 30 -> getString(R.string.too_long)
            else -> null
        }
    }

    val snackbar = SnackbarHostState()

    fun onCreateCard(context: Context) {
        newCardName.check(context)
        if (!newCardName.verified) return
        newCardLoading = true
        viewModelScope.launch {
            repository.sendCreateCard(accountID, newCardName.value).handleValue(this, snackbar, context) { cardID ->
                showNewCardDialog = false
                onNavigateToCard(cardID)
            }
            newCardLoading = false
        }
    }
}

@Composable
fun BankAccountScreen(onDismiss: () -> Unit, data: SBankAccount, navigation: NavHostController, onNavigateToCard: (Int) -> Unit) {
    val repository = LocalRepository.current
    val context = LocalContext.current
    val model = viewModel<BankAccountScreenModel> { BankAccountScreenModel(repository, data.id, onNavigateToCard) }
    val activity = model.accountData.collectAsState(null).value

    // New virtual card dialog
    BottomDialog(
        model.showNewCardDialog,
        onDismissRequest = { model.showNewCardDialog = false },
        confirmButtonEnabled = model.newCardName.value.isNotEmpty(),
        confirmButtonText = R.string.create_card,
        onConfirmButtonClick = { model.onCreateCard(context) }
    ) {
        LoadingOverlay(model.newCardLoading) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.create_virtual_card),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                VerifiableField(
                    model.newCardName,
                    R.string.card_name,
                    KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth(),
                    isLast = true,
                    onDone = { model.onCreateCard(context) },
                    capitalization = KeyboardCapitalization.Sentences,
                    placeholder = R.string.my_card
                )
            }
        }
    }

    val pagerState = rememberPagerState { 3 }
    NavScreen(
        onDismiss,
        title = R.string.bank_account,
        isFABVisible = pagerState.currentPage == 1,
        snackbar = model.snackbar,
        fabContent = {
            MultiFab(
                Icons.Default.Add, listOf(
                    MultiFabItem("virtual", R.drawable.bank_account, R.string.virtual_card),
                    MultiFabItem("physical", R.drawable.baseline_card_24, R.string.physical_card),
                )
            ) { id ->
                when (id) {
                    "virtual" -> model.showNewCardDialog = true
                    "physical" -> {}
                }
            }
        }
    ) {
        Column {
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .topBorder(8.dp, data.color())
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = data.name, style = MaterialTheme.typography.headlineSmall)
                        Amount(amount = data.balance, currency = data.currency, textStyle = MaterialTheme.typography.titleLarge)
                    }
                    Icon(painter = painterResource(R.drawable.bank_account), contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            PagerTabs(tabs = listOf(R.string.history, R.string.cards, R.string.actions), pagerState = pagerState) { tab ->
                when (tab) {
                    0 -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (activity == null) RecentActivityShimmer()
                        else if (activity.transfers.isEmpty() && activity.transactions.isEmpty() && activity.parties.isEmpty()) item {
                            InfoCard(R.string.no_recent_activity)
                        } else {
                            val (_, transfers, transactions, parties) = activity
                            RecentActivityContent(transfers, transactions, parties, navigation)
                        }
                    }
                    1 -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (activity == null) CardCard(null)
                        else {
                            if (activity.cards.isEmpty()) InfoCard(R.string.no_cards)
                            else for (card in activity.cards) CardCard(card, onClick = { onNavigateToCard(card.id) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    2 -> Box(modifier = Modifier.fillMaxSize()) {
                        Text(text = "todo")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BankAccountScreenPreview() {
    AppTheme {
        val repository = LocalRepository.current
        val data = remember { runBlocking { repository.accounts.first()[0] } }
        BankAccountScreen(onDismiss = {}, data = data, navigation = rememberMockNavController(), onNavigateToCard = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BankAccountScreenPreviewDark() {
    AppTheme {
        val repository = LocalRepository.current
        val data = remember { runBlocking { repository.accounts.first()[1] } }
        BankAccountScreen(onDismiss = {}, data = data, navigation = rememberMockNavController(), onNavigateToCard = {})
    }
}