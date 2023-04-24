package ro.bankar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
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
import ro.bankar.app.ui.theme.AppTheme
import kotlin.time.Duration.Companion.seconds

private enum class SignUpStep {
    Initial, Middle, Final;
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun SignUpScreen() {
    var signUpStep by rememberSaveable { mutableStateOf(SignUpStep.Initial) }
    val isLoading = remember { mutableStateOf(false) }
    val themeMode = LocalThemeMode.current

    Surface(color = MaterialTheme.colorScheme.primary) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.Center),
            ) {
                Text(
                    "Sign Up",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    shadowElevation = 5.dp,
                ) {
                    LoadingOverlay(isLoading.value) {
                        AnimatedContent(
                            targetState = signUpStep,
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
                                SignUpStep.Initial -> InitialSignUpStep(
                                    onStepComplete = { signUpStep = SignUpStep.Middle },
                                    isLoading,
                                )

                                SignUpStep.Middle -> MiddleSignUpStep(
                                    onGoBack = { signUpStep = SignUpStep.Initial },
                                    onStepComplete = { signUpStep = SignUpStep.Final },
                                    isLoading = isLoading,
                                )

                                SignUpStep.Final -> FinalSignUpStep(
                                    onGoBack = { signUpStep = SignUpStep.Middle },
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
                    .padding(bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Have an account already?",
                )
                TextButton(onClick = {}) {
                    Text(
                        text = "Log In",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.inverseSurface,
                    )
                }
            }
        }
    }
}

class InitialSignUpModel : ViewModel() {
    var full_name by mutableStateOf("")
    var date_of_birth by mutableStateOf("")
    var address by mutableStateOf("")
    var email by mutableStateOf("")
    var tag by mutableStateOf("")
    var password by mutableStateOf("")
    var confirm_password by mutableStateOf("")

    fun signup(onStepComplete: () -> Unit, focusManager: FocusManager? = null, setLoading: (Boolean) -> Unit) = viewModelScope.launch {
        focusManager?.clearFocus()
        setLoading(true)
        try {
            delay(3.seconds)
            onStepComplete()
        } finally {
            setLoading(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitialSignUpStep(onStepComplete: () -> Unit, isLoading: MutableState<Boolean>) {
    val (loading, setLoading) = isLoading
    val model: InitialSignUpModel = viewModel()
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
            leadingIcon = { Icon(Icons.Default.Face, "Account ID") },
            value = model.full_name,
            onValueChange = { model.full_name = it },
            label = {
                Text(text = "Full Name")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Text),
        )

        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DateRange, "Date of birth") },
            value = model.date_of_birth,
            onValueChange = { model.date_of_birth = it },
            label = {
                Text(text = "Date of Birth")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
        )

        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Home, "Address") },
            value = model.address,
            onValueChange = { model.address = it },
            label = {
                Text(text = "Address")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Text),
        )

        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, "E-mail") },
            value = model.email,
            onValueChange = { model.email = it },
            label = {
                Text(text = "E-mail")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Email),
        )

        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.AccountCircle, "User tag") },
            value = model.tag,
            onValueChange = { model.tag = it },
            label = {
                Text(text = "User Tag")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Password),
        )

        var showPassword1 by remember { mutableStateOf(false) }
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, "Account password") },
            trailingIcon = {
                IconButton(onClick = { showPassword1 = !showPassword1 }) {
                    Icon(
                        painterResource(
                            if (showPassword1) R.drawable.baseline_visibility_24
                            else R.drawable.baseline_visibility_off_24
                        ),
                        "Show password"
                    )
                }
            },
            visualTransformation = if (showPassword1) VisualTransformation.None else PasswordVisualTransformation(),
            value = model.password,
            onValueChange = { model.password = it },
            label = {
                Text(text = "Password")
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrect = false, keyboardType = KeyboardType.Password),
        )

        var showPassword2 by remember { mutableStateOf(false) }
        val condition = model.full_name.isNotEmpty() && model.date_of_birth.isNotEmpty() &&
                model.address.isNotEmpty() && model.email.isNotEmpty() &&
                model.tag.isNotEmpty() && model.password.isNotEmpty() &&
                model.confirm_password.isNotEmpty() && !loading && model.password == model.confirm_password

        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, "Account password") },
            trailingIcon = {
                IconButton(onClick = { showPassword2 = !showPassword2 }) {
                    Icon(
                        painterResource(
                            if (showPassword2) R.drawable.baseline_visibility_24
                            else R.drawable.baseline_visibility_off_24
                        ),
                        "Show password"
                    )
                }
            },
            visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
            value = model.confirm_password,
            onValueChange = { model.confirm_password = it },
            label = {
                Text(text = "Confirm Password")
            },
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { if (condition) model.signup(onStepComplete, focusManager, setLoading) })
        )

        Button(
            enabled = condition,
            onClick = { model.signup(onStepComplete, focusManager, setLoading) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = "Sign Up")
        }
    }

}

class MiddleSignUpModel : ViewModel() {
    var phone_number by mutableStateOf("")

    fun signup(onStepComplete: () -> Unit, focusManager: FocusManager? = null, setLoading: (Boolean) -> Unit) = viewModelScope.launch {
        focusManager?.clearFocus()
        setLoading(true)
        try {
            delay(3.seconds)
            onStepComplete()
        } finally {
            setLoading(false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiddleSignUpStep(onStepComplete: () -> Unit, onGoBack: () -> Unit, isLoading: MutableState<Boolean>) {
    val (loading, setLoading) = isLoading
    val model: MiddleSignUpModel = viewModel()
    val focusManager = LocalFocusManager.current
    BackHandler(onBack = onGoBack)

    Column(
        modifier = Modifier.padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(text = "Enter phone number:")
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, "Account ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = model.phone_number,
            placeholder = { Text("0733768565") },
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 10) model.phone_number = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = { model.signup(onStepComplete, focusManager, setLoading) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Confirm")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalSignUpStep(onGoBack: () -> Unit, isLoading: MutableState<Boolean>) {
    var code by remember { mutableStateOf("") }
    BackHandler(onBack = onGoBack)

    Column(
        modifier = Modifier.padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(text = "A 6-digit verification code has been sent to your mobile phone.\nPlease enter the code below:")
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
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
fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreen()
    }
}