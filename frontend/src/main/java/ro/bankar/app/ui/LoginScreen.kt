package ro.bankar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.theme.AppTheme
import kotlin.time.Duration.Companion.seconds

private enum class LoginStep {
    Initial, Final;
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen() {
    var loginStep by rememberSaveable { mutableStateOf(LoginStep.Initial) }
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    shadowElevation = 5.dp,
                ) {
                    val isLoading = remember { mutableStateOf(false) }
                    LoadingOverlay(isLoading.value) {
                        AnimatedContent(
                            targetState = loginStep,
                            label = "Login Step Animation",
                            transitionSpec = {
                                if (targetState == LoginStep.Final) {
                                    (slideInHorizontally { w -> w } with slideOutHorizontally { w -> -w })
                                } else {
                                    (slideInHorizontally { w -> -w } with slideOutHorizontally { w -> w })
                                }
                            }
                        ) {
                            when (it) {
                                LoginStep.Initial -> InitialLoginStep(
                                    onStepComplete = { loginStep = LoginStep.Final },
                                    isLoading,
                                )

                                LoginStep.Final -> FinalLoginStep(
                                    onGoBack = { loginStep = LoginStep.Initial },
                                    isLoading,
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dont_have_account_yet),
                )
                TextButton(onClick = {}) {
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

class InitialLoginModel : ViewModel() {
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    fun login(onStepComplete: () -> Unit, setIsLoading: (Boolean) -> Unit, focusManager: FocusManager? = null) = viewModelScope.launch {
        focusManager?.clearFocus()
        setIsLoading(true)
        try {
            delay(3.seconds)
            onStepComplete()
        } finally {
            setIsLoading(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitialLoginStep(onStepComplete: () -> Unit, isLoading: MutableState<Boolean>) {
    val model: InitialLoginModel = viewModel()
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
        val (loading, setLoading) = isLoading
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
            keyboardActions = KeyboardActions(onDone = { model.login(onStepComplete, setLoading, focusManager) })
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
                enabled = model.username.isNotEmpty() && model.password.isNotEmpty() && !loading,
                onClick = { model.login(onStepComplete, setLoading, focusManager) },
            ) {
                Text(text = stringResource(R.string.sign_in))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalLoginStep(onGoBack: () -> Unit, isLoading: MutableState<Boolean>) {
    var code by remember { mutableStateOf("") }
    BackHandler(onBack = onGoBack)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(text = stringResource(R.string.six_digit_code_sent))
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            leadingIcon = { Icon(Icons.Default.Lock, stringResource(R.string.code)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            value = code,
            placeholder = { Text("123456") },
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) code = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = {},
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.confirm))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AppTheme {
        LoginScreen()
    }
}