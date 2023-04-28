package ro.bankar.app.ui.newuser

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.bankar.app.LocalDataStore
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.USER_SESSION
import ro.bankar.app.ktor.Response
import ro.bankar.app.ktor.ktorClient
import ro.bankar.app.ktor.safePost
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SFinalLoginData
import ro.bankar.model.SInitialLoginData
import ro.bankar.model.StatusResponse

enum class LoginStep {
    Initial, Final;
}

class LoginModel : ViewModel() {
    // Fields
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var code by mutableStateOf("")
    // Field errors
    var usernameOrPasswordError by mutableStateOf<String?>(null)
    var codeError by mutableStateOf<String?>(null)

    // Internal state
    var isLoading by mutableStateOf(false)
    var step by mutableStateOf(LoginStep.Initial)
    val snackBar = SnackbarHostState()

    // Set by model
    lateinit var onSuccess: () -> Unit
    var dataStore: DataStore<Preferences>? = null

    fun goBack() {
        if (step == LoginStep.Final) step = LoginStep.Initial
    }

    private var loginSession: String? = null
    fun doInitial(focusManager: FocusManager, c: Context) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        usernameOrPasswordError = null
        val result = ktorClient.safePost<StatusResponse, StatusResponse> {
            url.appendPathSegments("login/initial")
            setBody(SInitialLoginData(username, password))
        }
        isLoading = false
        when (result) {
            is Response.Success -> {
                loginSession = result.r.headers["LoginSession"]
                step = LoginStep.Final
            }
            is Response.Error -> {
                snackBar.showSnackbar(c.getString(result.message), withDismissAction = true)
            }
            is Response.Fail -> {
                when (result.s.status) {
                    "account_disabled" -> snackBar.showSnackbar(c.getString(R.string.account_disabled), withDismissAction = true)
                    "invalid_username_or_password" -> usernameOrPasswordError = c.getString(R.string.invalid_user_or_pass)
                    else -> snackBar.showSnackbar(c.getString(R.string.unknown_error), withDismissAction = true)
                }
            }
        }
    }

    fun doFinal(focusManager: FocusManager, context: Context) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        codeError = null
        val result = ktorClient.safePost<StatusResponse, StatusResponse> {
            url.appendPathSegments("login/final")
            header("LoginSession", loginSession)
            setBody(SFinalLoginData(code))
        }
        isLoading = false
        when (result) {
            is Response.Success -> {
                dataStore?.edit { store -> result.r.headers["Authorization"]?.removePrefix("Bearer ")?.let { store[USER_SESSION] = it } }
                onSuccess()
            }
            is Response.Error -> {
                snackBar.showSnackbar(context.getString(result.message), withDismissAction = true)
            }
            is Response.Fail -> {
                when (result.s.status) {
                    "invalid_session", "session_expired" -> {
                        step = LoginStep.Initial
                        snackBar.showSnackbar(context.getString(R.string.login_session_expired), withDismissAction = true)
                    }
                    "invalid_code" -> codeError = context.getString(R.string.wrong_login_code)
                    else -> snackBar.showSnackbar(context.getString(R.string.unknown_error), withDismissAction = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onSignUp: () -> Unit, onSuccess: () -> Unit) {
    val model: LoginModel = viewModel()
    model.dataStore = LocalDataStore.current
    model.onSuccess = onSuccess
    val themeMode = LocalThemeMode.current

    Scaffold(snackbarHost = { SnackbarHost(model.snackBar) }) { padding ->
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                ThemeToggle(
                    isDarkMode = themeMode.isDarkMode,
                    onToggle = themeMode.toggleThemeMode,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.Center),
                ) {
                    Text(
                        stringResource(R.string.sign_in),
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(15.dp),
                        shadowElevation = 3.dp,
                    ) {
                        LoadingOverlay(model.isLoading) {
                            AnimatedContent(
                                targetState = model.step,
                                label = "Login Step Animation",
                                transitionSpec = {
                                    if (targetState.ordinal > initialState.ordinal) {
                                        (slideInHorizontally { w -> w } with slideOutHorizontally { w -> -w })
                                    } else {
                                        (slideInHorizontally { w -> -w } with slideOutHorizontally { w -> w })
                                    }
                                }
                            ) {
                                when (it) {
                                    LoginStep.Initial -> InitialLoginStep(model)
                                    LoginStep.Final -> FinalLoginStep(model)
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = model.step == LoginStep.Initial,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.padding(bottom = 15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_have_account_yet),
                        )
                        TextButton(onClick = onSignUp, enabled = !model.isLoading) {
                            Text(
                                text = stringResource(R.string.create_one_now),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.inverseSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitialLoginStep(model: LoginModel) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .padding(25.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        TextField(
            value = model.username,
            onValueChange = { model.username = it },
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AccountCircle, stringResource(R.string.phone_email_tag)) },
            label = {
                Text(text = stringResource(R.string.phone_email_tag))
            },
            isError = model.usernameOrPasswordError != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
        )

        val context = LocalContext.current
        var showPassword by remember { mutableStateOf(false) }
        TextField(
            value = model.password,
            onValueChange = { model.password = it },
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, stringResource(R.string.password)) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        painterResource(
                            if (showPassword) R.drawable.baseline_visibility_24
                            else R.drawable.baseline_visibility_off_24
                        ),
                        stringResource(R.string.show_password)
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            label = {
                Text(text = stringResource(R.string.password))
            },
            isError = model.usernameOrPasswordError != null,
            supportingText = model.usernameOrPasswordError?.let { { Text(text = it) } },
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { model.doInitial(focusManager, context) })
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = {}) {
                Text(text = stringResource(R.string.forgot_password))
            }
            Button(
                enabled = model.username.isNotEmpty() && model.password.isNotEmpty() && !model.isLoading,
                onClick = { model.doInitial(focusManager, context) },
            ) {
                Text(text = stringResource(R.string.sign_in))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalLoginStep(model: LoginModel) {
    val focusManager = LocalFocusManager.current
    BackHandler(onBack = model::goBack)
    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(true) {
        model.code = ""
        delay(500)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .padding(25.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(text = stringResource(R.string.six_digit_code_sent))
        // TODO Custom code field
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            leadingIcon = { Icon(Icons.Default.Lock, stringResource(R.string.code)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            value = model.code,
            placeholder = { Text("123456") },
            isError = model.codeError != null,
            supportingText = model.codeError?.let { { Text(text = it) } },
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) model.code = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        val context = LocalContext.current
        Button(
            onClick = { model.doFinal(focusManager, context) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.confirm))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AppTheme {
        LoginScreen({}, {})
    }
}