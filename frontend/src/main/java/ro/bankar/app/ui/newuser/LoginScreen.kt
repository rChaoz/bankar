package ro.bankar.app.ui.newuser

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.bankar.app.LocalDataStore
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.USER_SESSION
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.theme.AppTheme
import kotlin.time.Duration.Companion.seconds

enum class LoginStep {
    Initial, Final;
}
class LoginModel : ViewModel() {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var step by mutableStateOf(LoginStep.Initial)
    var code by mutableStateOf("")
    lateinit var onSuccess: State<() -> Unit>

    fun goBack() {
        if (step == LoginStep.Final) step = LoginStep.Initial
    }

    fun doInitial(focusManager: FocusManager) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        try {
            delay(2.seconds)
            step = LoginStep.Final
        } finally {
            isLoading = false
        }
    }

    fun doFinal(focusManager: FocusManager) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        try {
            delay(2.seconds)
            onSuccess.value()
        } finally {
            isLoading = false
        }
    }
}

@Composable
fun LoginScreen(onSignUp: () -> Unit, onSuccess: () -> Unit) {
    val model: LoginModel = viewModel()
    val scope = rememberCoroutineScope()
    val dataStore = LocalDataStore.current
    model.onSuccess = rememberUpdatedState {
        scope.launch {
            dataStore?.edit { it[USER_SESSION] = "test" }
            onSuccess()
        }
    }
    val themeMode = LocalThemeMode.current

    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                    shadowElevation = 5.dp,
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
                        text = stringResource(R.string.dont_have_account_yet),
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
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AccountCircle, stringResource(R.string.phone_email_tag)) },
            value = model.username,
            onValueChange = { model.username = it },
            label = {
                Text(text = stringResource(R.string.phone_email_tag))
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
        )

        var showPassword by remember { mutableStateOf(false) }
        TextField(
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
            value = model.password,
            onValueChange = { model.password = it },
            label = {
                Text(text = stringResource(R.string.password))
            },
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { model.doInitial(focusManager) })
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
                onClick = { model.doInitial(focusManager) },
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
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) model.code = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = { model.doFinal(focusManager) },
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