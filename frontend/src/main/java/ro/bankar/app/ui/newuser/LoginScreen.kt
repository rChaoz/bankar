package ro.bankar.app.ui.newuser

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.TAG
import ro.bankar.app.data.KeyUserSession
import ro.bankar.app.data.LocalDataStore
import ro.bankar.app.data.basicClient
import ro.bankar.app.data.setPreference
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.show
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.ErrorResponse
import ro.bankar.model.Response
import ro.bankar.model.SInitialLoginData
import ro.bankar.model.SSMSCodeData
import ro.bankar.model.SuccessResponse

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
    val snackbar = SnackbarHostState()

    // Set by model
    lateinit var onSuccess: () -> Unit
    lateinit var dataStore: DataStore<Preferences>

    fun goBack() {
        if (step == LoginStep.Final) step = LoginStep.Initial
    }

    private var loginSession by mutableStateOf<String?>(null)
    fun doInitial(focusManager: FocusManager, context: Context) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        usernameOrPasswordError = null
        try {
            // We need access to headers, so manually call instead of using safeRequest
            val response = basicClient.post("login/initial") {
                // Allow the user to input tag prefixed with '@' symbol
                setBody(SInitialLoginData(username.trim().removePrefix("@"), password))
            }
            when (val r = response.body<Response<Unit>>()) {
                SuccessResponse -> {
                    loginSession = response.headers["LoginSession"]
                    codeError = null
                    step = LoginStep.Final
                }
                is ErrorResponse -> when (r.message) {
                    "account_disabled" -> launch { snackbar.show(context.getString(R.string.account_disabled)) }
                    "invalid_username_or_password" -> usernameOrPasswordError = context.getString(R.string.invalid_user_or_pass)
                    else -> launch { snackbar.show(context.getString(R.string.unknown_error)) }
                }
                else -> launch { snackbar.show(context.getString(R.string.unknown_error)) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during login initial step", e)
            launch { snackbar.show(context.getString(R.string.connection_error)) }
        }
        isLoading = false
    }

    fun doFinal(focusManager: FocusManager, context: Context) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        codeError = null
        try {
            val response = basicClient.post("login/final") {
                header("LoginSession", loginSession)
                setBody(SSMSCodeData(code))
            }
            when (val r = response.body<Response<Unit>>()) {
                SuccessResponse -> {
                    val token = response.headers["Authorization"]?.removePrefix("Bearer ")
                    if (token.isNullOrBlank()) launch { snackbar.show(context.getString(R.string.invalid_server_response)) }
                    else {
                        dataStore.setPreference(KeyUserSession, token)
                        onSuccess()
                    }
                }
                is ErrorResponse -> when (r.message) {
                    "invalid_session", "session_expired" -> {
                        step = LoginStep.Initial
                        launch { snackbar.show(context.getString(R.string.login_session_expired)) }
                    }
                    "invalid_code" -> codeError = context.getString(R.string.incorrect_code)
                    else -> launch { snackbar.show(context.getString(R.string.unknown_error)) }
                }
                else -> launch { snackbar.show(context.getString(R.string.unknown_error)) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during login final step", e)
            launch { snackbar.show(context.getString(R.string.connection_error)) }
        }
        isLoading = false
    }
}

@Composable
fun LoginScreen(onSignUp: () -> Unit, onSuccess: () -> Unit) {
    val model: LoginModel = viewModel()
    model.dataStore = LocalDataStore.current
    model.onSuccess = onSuccess
    val themeMode = LocalThemeMode.current

    Scaffold(snackbarHost = { SnackbarHost(model.snackbar) }) { padding ->
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ThemeToggle(
                    isDarkMode = themeMode.isDarkMode,
                    onToggle = themeMode.toggleThemeMode,
                    modifier = Modifier
                        .padding(top = 10.dp, end = 10.dp)
                        .align(Alignment.End)
                )
                Spacer(modifier = Modifier.weight(.65f))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(10.dp)
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
                                        (slideInHorizontally { w -> w } togetherWith slideOutHorizontally { w -> -w })
                                    } else {
                                        (slideInHorizontally { w -> -w } togetherWith slideOutHorizontally { w -> w })
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
                Spacer(modifier = Modifier.weight(1f))
                AnimatedVisibility(model.step == LoginStep.Initial, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 15.dp)
                            .fillMaxWidth(),
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

@Composable
private fun InitialLoginStep(model: LoginModel) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .padding(25.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        LoginField(
            value = model.username,
            onValueChange = { model.username = it },
            label = R.string.phone_email_tag,
            leadingIcon = { Icon(Icons.Default.AccountCircle, stringResource(R.string.phone_email_tag)) },
            keyboardType = KeyboardType.Email,
            isError = model.usernameOrPasswordError != null,
        )
        val context = LocalContext.current
        var showPassword by remember { mutableStateOf(false) }
        LoginField(
            value = model.password,
            onValueChange = { model.password = it },
            label = R.string.password,
            leadingIcon = { Icon(Icons.Default.Lock, null) },
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
            keyboardType = KeyboardType.Password,
            showPassword = showPassword,
            isError = model.usernameOrPasswordError != null,
            supportingText = model.usernameOrPasswordError,
            onDone = { model.doInitial(focusManager, context) }
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
        val context = LocalContext.current
        LoginField(
            value = model.code,
            onValueChange = {
                if (it.all { char -> char.isDigit() } && it.length <= 6) {
                    model.code = it
                    model.codeError = null
                }
            },
            focusRequester,
            leadingIcon = { Icon(Icons.Default.Lock, stringResource(R.string.code)) },
            keyboardType = KeyboardType.NumberPassword,
            placeholder = "123456",
            isError = model.codeError != null,
            supportingText = model.codeError,
            textStyle = MaterialTheme.typography.bodyLarge,
            onDone = {
                if (model.code.length < 6) model.codeError = context.getString(R.string.code_has_6_digits)
                else model.doFinal(focusManager, context)
            }
        )

        Button(
            onClick = { model.doFinal(focusManager, context) },
            modifier = Modifier.align(Alignment.End),
            enabled = model.code.length == 6
        ) {
            Text(text = stringResource(R.string.confirm))
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    label: Int? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardType: KeyboardType = KeyboardType.Text,
    showPassword: Boolean = true,
    onDone: (KeyboardActionScope.() -> Unit)? = null,
) {
    var modifier = Modifier.fillMaxWidth()
    if (focusRequester != null) modifier = modifier.focusRequester(focusRequester)
    TextField(
        value,
        onValueChange,
        modifier,
        singleLine = true,
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        label = label?.let { { Text(text = stringResource(it)) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(text = it) } },
        isError = isError,
        textStyle = textStyle,
        placeholder = placeholder?.let { { Text(text = it) } },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = keyboardType, imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = onDone)
    )
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AppTheme {
        LoginScreen({}, {})
    }
}