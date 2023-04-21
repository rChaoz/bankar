package ro.bankar.app.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen() {
    val pager = rememberPagerState()
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
                    "Sign In",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    shadowElevation = 5.dp,
                ) {
                    HorizontalPager(
                        userScrollEnabled = false,
                        pageCount = 2,
                        state = pager,
                        modifier = Modifier.animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val focusManager = LocalFocusManager.current
                        val coroutineScope = rememberCoroutineScope()
                        if (it == 0) InitialLoginStep {
                            coroutineScope.launch {
                                pager.animateScrollToPage(1)
                            }
                            // Because focused field will prevent page from un-loading
                            focusManager.clearFocus()
                        }
                        else FinalLoginStep {
                            coroutineScope.launch {
                                pager.animateScrollToPage(0)
                            }
                            // Same
                            focusManager.clearFocus()
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
                    text = "Don't have an account yet?",
                )
                TextButton(onClick = {}) {
                    Text(
                        text = "Create one now",
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
    var loading by mutableStateOf(false)

    fun login(onStepComplete: () -> Unit, focusManager: FocusManager? = null) = viewModelScope.launch {
        focusManager?.clearFocus()
        loading = true
        try {
            delay(3.seconds)
            onStepComplete()
        } catch (e: Exception) {
            Log.d("BanKAR", "login: a crapat")
        } finally {
            loading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitialLoginStep(onStepComplete: () -> Unit) {
    val model: InitialLoginModel = viewModel()
    val focusManager = LocalFocusManager.current

    LoadingOverlay(isLoading = model.loading) {
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
                leadingIcon = { Icon(Icons.Default.AccountCircle, "Account ID") },
                value = model.username,
                onValueChange = { model.username = it },
                label = {
                    Text(text = "Phone, e-mail or tag")
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
            )

            var showPassword by remember { mutableStateOf(false) }
            TextField(
                singleLine = true,
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Lock, "Account password") },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            painterResource(
                                if (showPassword) R.drawable.baseline_visibility_24
                                else R.drawable.baseline_visibility_off_24
                            ),
                            "Show password"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                value = model.password,
                onValueChange = { model.password = it },
                label = {
                    Text(text = "Password")
                },
                keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password),
                keyboardActions = KeyboardActions(onDone = { model.login(onStepComplete, focusManager) })
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {}) {
                    Text(text = "Forgot password?")
                }
                Button(
                    enabled = model.username.isNotEmpty() && model.password.isNotEmpty() && !model.loading,
                    onClick = { model.login(onStepComplete, focusManager) },
                ) {
                    Text(text = "Sign In")
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalLoginStep(onGoBack: () -> Unit) {
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
        Text(text = "A 6-digit verification code has been sent to your mobile phone.\nPlease enter the code below:")
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            leadingIcon = { Icon(Icons.Default.Lock, "Account ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = code,
            placeholder = { Text("123456") },
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) code = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = {},
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Confirm")
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