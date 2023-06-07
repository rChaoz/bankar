package ro.bankar.app.ui.main.settings

import android.content.res.Configuration
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import ro.bankar.app.KeyAuthenticationPin
import ro.bankar.app.KeyFingerprintEnabled
import ro.bankar.app.KeyLanguage
import ro.bankar.app.KeyPreferredCurrency
import ro.bankar.app.KeyTheme
import ro.bankar.app.LocalActivity
import ro.bankar.app.LocalDataStore
import ro.bankar.app.R
import ro.bankar.app.collectPreferenceAsState
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.fold
import ro.bankar.app.data.handleSuccess
import ro.bankar.app.removePreference
import ro.bankar.app.setPreference
import ro.bankar.app.ui.components.AccountsComboBox
import ro.bankar.app.ui.components.BottomDialog
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.main.LocalSnackbar
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.rememberMockNavController
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.Currency
import ro.bankar.model.ErrorResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SuccessResponse

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
        override val backButtonAction = mutableStateOf<(() -> Unit)?>(null)
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content(model: Model, navigation: NavHostController) {
        val settingsNav = rememberAnimatedNavController()

        LaunchedEffect(settingsNav) {
            val onBack = { settingsNav.popBackStack(); Unit }
            settingsNav.currentBackStackEntryFlow.collect {
                model.backButtonAction.value = if (it.destination.route != SettingsNav.route) onBack else null
            }
        }

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
private fun SettingsScreen(navigation: NavHostController) {
    var authenticateDialogVisible by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val repository = LocalRepository.current
    val scope = rememberCoroutineScope()

    var password by rememberSaveable { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val onDone: () -> Unit = {
        isLoading = true
        scope.launch {
            repository.sendCheckPassword(password).fold(
                onFail = { context.getString(it) },
                onSuccess = {
                    when (it) {
                        SuccessResponse -> {
                            authenticateDialogVisible = false
                            navigation.navigate(SettingsNav.Access.route)
                            null
                        }
                        is ErrorResponse -> R.string.incorrect_password
                        else -> R.string.unknown_error
                    }
                }
            )
            isLoading = false
        }
    }

    BottomDialog(
        visible = authenticateDialogVisible,
        onDismissRequest = { authenticateDialogVisible = false },
        confirmButtonText = R.string.confirm,
        onConfirmButtonClick = onDone
    ) {
        val requester = remember { FocusRequester() }
        LaunchedEffect(true) {
            password = ""
            passwordError = null
            requester.requestFocus()
        }
        LoadingOverlay(isLoading) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.verification_password))
                var showPassword by rememberSaveable { mutableStateOf(false) }
                TextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = { Text(text = stringResource(R.string.password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(requester),
                    isError = passwordError != null,
                    supportingText = {
                        if (passwordError != null) Text(text = passwordError!!)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                painter = painterResource(if (showPassword) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24),
                                contentDescription = stringResource(R.string.show_password)
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                )
            }
        }
    }

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
            onClick = { authenticateDialogVisible = true },
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
private fun LanguageScreen() = Surface {
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
private fun AccessScreen() = Surface {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val fingerprintAvailable = remember { BiometricManager.from(context).canAuthenticate(Authenticators.BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS }

    // Get data
    val datastore = LocalDataStore.current
    val dataPin by datastore.collectPreferenceAsState(key = KeyAuthenticationPin, defaultValue = null)

    // PIN entry dialog
    var pinDialogVisible by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }

    val onDone = { newPin: String? ->
        scope.launch {
            if (newPin == null) datastore.removePreference(KeyAuthenticationPin)
            else datastore.setPreference(KeyAuthenticationPin, newPin)
            snackbar.showSnackbar(context.getString(if (newPin == null) R.string.pin_disabled else R.string.pin_changed), withDismissAction = true)
        }
        pinDialogVisible = false
    }

    BottomDialog(
        visible = pinDialogVisible,
        onDismissRequest = { pinDialogVisible = false },
        buttonBar = {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(onClick = { pinDialogVisible = false }, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(onClick = { onDone(null) }, modifier = Modifier.weight(1f), enabled = dataPin != null) {
                    Text(text = stringResource(R.string.disable_pin))
                }
                Button(onClick = { onDone(pin) }, modifier = Modifier.weight(1f), enabled = pin.length in 4..8) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        }
    ) {
        val requester = remember { FocusRequester() }
        LaunchedEffect(true) {
            requester.requestFocus()
        }
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(R.string.enter_new_pin))
            TextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (pin.length in 4..8) onDone(pin) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester),
                leadingIcon = { Icon(painter = painterResource(R.drawable.baseline_pin_24), contentDescription = null) }
            )
        }
    }

    // Fingerprint
    val fingerprintEnabled by datastore.collectPreferenceAsState(key = KeyFingerprintEnabled, defaultValue = false)
    val activity = LocalActivity.current
    val prompt = remember {
        activity?.let {
            BiometricPrompt(it, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    scope.launch { datastore.setPreference(KeyFingerprintEnabled, true) }
                }
            })
        }
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login))
            .setSubtitle(context.getString(R.string.enable_biometric))
            .setNegativeButtonText(context.getString(android.R.string.cancel))
            .build()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsCheckbox(
            icon = { Icon(painter = painterResource(R.drawable.baseline_pin_24), contentDescription = null) },
            title = { Text(text = stringResource(R.string.pin), style = MaterialTheme.typography.titleMedium) },
            subtitle = { Text(text = stringResource(R.string.pin_desc)) },
            state = remember {
                object : SettingValueState<Boolean> {
                    override var value
                        get() = dataPin != null
                        set(_) {}

                    override fun reset() {}
                }
            },
            onCheckedChange = { pin = ""; pinDialogVisible = true }
        )
        if (fingerprintAvailable && prompt != null) {
            Divider()
            SettingsCheckbox(
                icon = { Icon(painter = painterResource(R.drawable.baseline_fingerprint_24), contentDescription = null) },
                title = { Text(text = stringResource(id = R.string.fingerprint), style = MaterialTheme.typography.titleMedium) },
                subtitle = { Text(text = stringResource(R.string.fingerprint_desc)) },
                state = remember {
                    object : SettingValueState<Boolean> {
                        override var value
                            get() = fingerprintEnabled
                            set(_) {}

                        override fun reset() {}
                    }
                },
                onCheckedChange = {
                    if (fingerprintEnabled) scope.launch { datastore.setPreference(KeyFingerprintEnabled, false) }
                    else prompt.authenticate(promptInfo)
                }
            )
        }
    }
}

enum class Theme(val title: Int, val icon: Int) {
    SystemDefault(R.string.system_default_theme, R.drawable.baseline_smartphone_24),
    Light(R.string.light_theme, R.drawable.baseline_light_mode_24),
    Dark(R.string.dark_theme, R.drawable.baseline_dark_mode_24)
}

@Composable
private fun ThemeScreen() = Surface {
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
private fun ContactScreen() = Surface {
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
private fun PrimaryCurrencyScreen() = Surface {
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
private fun DefaultBankAccountScreen() = Surface {
    // Get data
    val repository = LocalRepository.current
    val state by repository.defaultAccount.collectAsState(null)

    // Get accounts list
    var accounts by remember { mutableStateOf<List<SBankAccount>?>(null) }
    val account = remember { mutableStateOf<SBankAccount?>(null) }
    LaunchedEffect(state) {
        repository.accounts.collect { newAccounts ->
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
                            repository.sendDefaultAccount(it?.id, state!!.alwaysUse).handleSuccess(this, snackbar, context) {
                                repository.defaultAccount.emitNow()
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
                                    repository.sendDefaultAccount(state!!.id, value).handleSuccess(this, snackbar, context) {
                                        repository.defaultAccount.emitNow()
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