package ro.bankar.app.ui.main.settings

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.alorma.compose.settings.storage.base.SettingValueState
import com.alorma.compose.settings.storage.base.getValue
import com.alorma.compose.settings.storage.base.setValue
import com.alorma.compose.settings.storage.datastore.rememberPreferenceDataStoreIntSettingState
import com.alorma.compose.settings.ui.SettingsCheckbox
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.launch
import ro.bankar.app.KeyLanguage
import ro.bankar.app.KeyPreferredCurrency
import ro.bankar.app.KeyTheme
import ro.bankar.app.LocalDataStore
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.data.collectRetrying
import ro.bankar.app.ui.components.AccountsComboBox
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.main.LocalSnackbar
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount

private enum class SettingsNav(val route: String) {
    Language("language"),
    Theme("theme"),
    Access("access"),
    ContactUs("contactUs"),
    PrimaryCurrency("primary_currency"),
    DefaultBankAccount("default_bank_account");

    companion object {
        const val route = "settings"
    }
}

object SettingsTab : MainTab<SettingsTab.Model>(2, "settings", R.string.settings) {
    class Model : MainTabModel() {
        override val showFAB = mutableStateOf(false)
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content(model: Model, navigation: NavHostController) {
        val settingsNav = rememberAnimatedNavController()
        AnimatedNavHost(
            navController = settingsNav,
            startDestination = SettingsNav.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) { it / 2 } + fadeIn(spring()) },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) { it / 2 } + fadeOut(spring()) }
        ) {
            composable(SettingsNav.route) {
                SettingsScreen(settingsNav)
            }
            composable(SettingsNav.Language.route) {
                LanguageScreen()
            }
            composable(SettingsNav.Theme.route) {
                ThemeScreen()
            }
            composable(SettingsNav.Access.route) {
                AccessScreen()
            }
            composable(SettingsNav.ContactUs.route) {
                ContactScreen()
            }
            composable(SettingsNav.PrimaryCurrency.route) {
                PrimaryCurrencyScreen()
            }
            composable(SettingsNav.DefaultBankAccount.route) {
                DefaultBankAccountScreen()
            }
        }
    }

    @Composable
    override fun FABContent(model: Model, navigation: NavHostController) {
        // empty
    }

    @Composable
    override fun viewModel(): Model = viewModel<Model>()
}

@Composable
fun SettingsScreen(navigation: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsMenuLink(
            icon = { Icon(painter = painterResource(R.drawable.baseline_language_24), contentDescription = null) },
            title = { Text(text = stringResource(id = R.string.language), style = MaterialTheme.typography.titleMedium) },
            onClick = { navigation.navigate(SettingsNav.Language.route) }
        )
        Divider()
        SettingsMenuLink(
            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            title = { Text(text = stringResource(R.string.access_options), style = MaterialTheme.typography.titleMedium) },
            subtitle = { Text(text = stringResource(R.string.access_options_desc)) },
            onClick = { navigation.navigate(SettingsNav.Access.route) },
        )
        Divider()
        SettingsMenuLink(
            icon = { Icon(painter = painterResource(R.drawable.baseline_palette_24), contentDescription = null) },
            title = { Text(text = stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium) },
            subtitle = { Text(text = stringResource(R.string.theme_desc)) },
            onClick = { navigation.navigate(SettingsNav.Theme.route) },
        )
        Divider()
        SettingsMenuLink(
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            title = { Text(text = stringResource(R.string.contact_us), style = MaterialTheme.typography.titleMedium) },
            onClick = { navigation.navigate(SettingsNav.ContactUs.route) },
        )
        Divider()
        SettingsMenuLink(
            icon = { Icon(painter = painterResource(R.drawable.baseline_money_24), contentDescription = null) },
            title = { Text(text = stringResource(R.string.preferred_currency), style = MaterialTheme.typography.titleMedium) },
            subtitle = { Text(text = stringResource(R.string.preferred_currency_desc)) },
            onClick = { navigation.navigate(SettingsNav.PrimaryCurrency.route) },
        )
        Divider()
        SettingsMenuLink(
            icon = { Icon(painter = painterResource(R.drawable.baseline_card_24), contentDescription = null) },
            title = { Text(text = stringResource(R.string.default_bank_account), style = MaterialTheme.typography.titleMedium) },
            subtitle = { Text(text = stringResource(R.string.default_bank_account_desk)) },
            onClick = { navigation.navigate(SettingsNav.DefaultBankAccount.route) },
        )
        Divider()
        SettingsMenuLink(
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "Disable Account", tint = Color.Red) },
            title = { Text(text = stringResource(id = R.string.disable_account), style = MaterialTheme.typography.titleMedium, color = Color.Red) },
            onClick = { /*TODO*/ },
        )
    }
}

enum class Language(val text: Int, val flag: Int?, val code: String?) {
    SystemDefault(R.string.system_default_language, null, null),
    Romanian(R.string.romanian, R.drawable.flag_ro, "ro"),
    English(R.string.english, R.drawable.flag_uk, "en");
}

@Composable
fun LanguageScreen() = Surface {
    val datastore = LocalDataStore.current
    var state by rememberPreferenceDataStoreIntSettingState(key = KeyLanguage.name, dataStore = datastore, defaultValue = 0)

    Column(modifier = Modifier.fillMaxSize()) {
        for (language in Language.values()) {
            if (language.ordinal != 0) {
                Divider()
            }
            SettingsMenuLink(
                icon = {
                    if (language.flag == null) Icon(painter = painterResource(R.drawable.baseline_smartphone_24), contentDescription = null)
                    else Image(painter = painterResource(language.flag), contentDescription = null)
                },
                title = { Text(text = stringResource(id = language.text), style = MaterialTheme.typography.titleMedium) },
                action = if (state == language.ordinal) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                } else null,
                onClick = { state = language.ordinal },
            )
        }
    }
}

@Composable
fun AccessScreen() = Surface {
    Column(modifier = Modifier.fillMaxSize()) {
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default., contentDescription = "PIN") },
            title = { Text(text = "PIN", style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/ },
        )
        Divider()
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Password") },
            title = { Text(text = stringResource(id = R.string.password), style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/ },
        )
        Divider()
        SettingsMenuLink(

            //icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Fingerprint") },
            title = { Text(text = stringResource(id = R.string.fingerprint), style = MaterialTheme.typography.titleMedium) },
            onClick = {/*TODO*/ },
        )
    }
}

enum class Theme(val title: Int, val icon: Int) {
    SystemDefault(R.string.system_default_theme, R.drawable.baseline_smartphone_24),
    Light(R.string.light_theme, R.drawable.baseline_light_mode_24),
    Dark(R.string.dark_theme, R.drawable.baseline_dark_mode_24)
}

@Composable
fun ThemeScreen() = Surface {
    val datastore = LocalDataStore.current
    var state by rememberPreferenceDataStoreIntSettingState(key = KeyTheme.name, dataStore = datastore, defaultValue = 0)

    Column(modifier = Modifier.fillMaxSize()) {
        for (theme in Theme.values()) {
            if (theme.ordinal != 0) Divider()
            SettingsMenuLink(
                icon = { Icon(painter = painterResource(theme.icon), contentDescription = null) },
                title = { Text(text = stringResource(theme.title), style = MaterialTheme.typography.titleMedium) },
                action = if (state == theme.ordinal) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                } else null,
                onClick = { state = theme.ordinal },
            )
        }
    }
}

@Composable
fun ContactScreen() = Surface {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.about_app), style = MaterialTheme.typography.titleMedium) },
            onClick = { /*TODO*/ },
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.faq), style = MaterialTheme.typography.titleMedium) },
            onClick = { /*TODO*/ },
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.send_us_email), style = MaterialTheme.typography.titleMedium) },
            onClick = { /*TODO*/ },
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.live_chat), style = MaterialTheme.typography.titleMedium) },
            onClick = { /*TODO*/ },
        )
        Divider()
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.call_us), style = MaterialTheme.typography.titleMedium) },
            onClick = { /*TODO*/ },
        )
    }
}

@Composable
fun PrimaryCurrencyScreen() = Surface {
    val datastore = LocalDataStore.current
    var state by rememberPreferenceDataStoreIntSettingState(key = KeyPreferredCurrency.name, dataStore = datastore, defaultValue = 0)

    Column(modifier = Modifier.fillMaxSize()) {
        for (currency in Currency.values()) {
            if (currency.ordinal == 0) Divider()
            SettingsMenuLink(
                title = { Text(text = currency.code, style = MaterialTheme.typography.titleMedium) },
                action = if (state == currency.ordinal) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                } else null,
                onClick = { state = currency.ordinal }
            )
        }
    }
}

@Composable
fun DefaultBankAccountScreen() = Surface {
    // Get data
    val repository = LocalRepository.current
    val state by repository.defaultAccount.collectAsStateRetrying()
    
    // Get accounts list
    var accounts by remember { mutableStateOf<List<SBankAccount>?>(null) }
    val account = remember { mutableStateOf<SBankAccount?>(null) }
    LaunchedEffect(state) {
        repository.accounts.collectRetrying { newAccounts ->
            accounts = newAccounts
            if (state != null) account.value = newAccounts.find { it.id == state!!.id }
        }
    }

    // Update preference on account change
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val context = LocalContext.current

    LoadingOverlay(isLoading = accounts == null || state == null || isLoading) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = stringResource(R.string.choose_default_bank_account))
                Spacer(modifier = Modifier.height(8.dp))
                AccountsComboBox(selectedAccount = account, accounts = accounts,
                    noneOptionText = R.string.dont_use_default_account, onPickAccount = {
                        isLoading = true
                        scope.launch {
                            when (val r = repository.sendDefaultAccount(it?.id, state!!.alwaysUse)) {
                                is SafeStatusResponse.InternalError ->
                                    launch { snackbar.showSnackbar(context.getString(r.message), withDismissAction = true) }
                                is SafeStatusResponse.Fail ->
                                    launch { snackbar.showSnackbar(context.getString(R.string.unknown_error), withDismissAction = true) }
                                is SafeStatusResponse.Success -> repository.defaultAccount.requestEmit()
                            }
                            isLoading = false
                        }
                    })
            }
            Divider()
            SettingsCheckbox(
                state = remember {
                    object : SettingValueState<Boolean> {
                        override var value: Boolean
                            get() = state?.alwaysUse ?: false
                            set(value) {
                                isLoading = true
                                scope.launch {
                                    when (val r = repository.sendDefaultAccount(state!!.id, value)) {
                                        is SafeStatusResponse.InternalError ->
                                            launch { snackbar.showSnackbar(context.getString(r.message), withDismissAction = true) }
                                        is SafeStatusResponse.Fail ->
                                            launch { snackbar.showSnackbar(context.getString(R.string.unknown_error), withDismissAction = true) }
                                        is SafeStatusResponse.Success -> repository.defaultAccount.requestEmit()
                                    }
                                    isLoading = false
                                }
                            }

                        override fun reset() {
                            value = false
                        }
                    }
                },
                title = { Text(text = stringResource(R.string.use_account_for_all_currencies)) },
                subtitle = { Text(text = stringResource(R.string.use_account_for_all_desc)) },
                enabled = account.value != null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsTabPreview() {
    AppTheme {
        SettingsTab.Content(SettingsTab.viewModel(), rememberMockNavController())
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsTabPreviewDark() {
    AppTheme {
        SettingsTab.Content(SettingsTab.viewModel(), rememberMockNavController())
    }
}