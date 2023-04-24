package ro.bankar.app.ui.newuser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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

enum class SignUpStep {
    Initial, Middle, Final;
}

class SignUpModel : ViewModel() {
    var step by mutableStateOf(SignUpStep.Initial)
    var isLoading by mutableStateOf(false)

    // First step
    var fullName by mutableStateOf("")
    var dateOfBirth by mutableStateOf("")
    var address by mutableStateOf("")
    var email by mutableStateOf("")
    var tag by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    // Second step
    var phoneNumber by mutableStateOf("")

    // Final step
    var code by mutableStateOf("")
    lateinit var onSuccess: State<() -> Unit>

    fun goBack() {
        if (step == SignUpStep.Middle) step = SignUpStep.Initial
        else if (step == SignUpStep.Final) step = SignUpStep.Middle
    }

    fun doInitialStep(focusManager: FocusManager) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        try {
            delay(2.seconds)
            step = SignUpStep.Middle
        } finally {
            isLoading = false
        }
    }

    fun doMiddleStep(focusManager: FocusManager) = viewModelScope.launch {
        focusManager.clearFocus()
        isLoading = true
        try {
            delay(2.seconds)
            step = SignUpStep.Final
        } finally {
            isLoading = false
        }
    }

    fun doFinalStep(focusManager: FocusManager) = viewModelScope.launch {
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SignUpScreen(onSignIn: () -> Unit, onSuccess: () -> Unit) {
    val themeMode = LocalThemeMode.current
    val model: SignUpModel = viewModel()

    val dataStore = LocalDataStore.current
    val ioScope = rememberCoroutineScope()
    model.onSuccess = rememberUpdatedState {
        ioScope.launch {
            dataStore?.edit { it[USER_SESSION] = "Test" }
            onSuccess()
        }
    }

    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ThemeToggle(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 10.dp, end = 10.dp),
                isDarkMode = themeMode.isDarkMode,
                onToggle = themeMode.toggleThemeMode,
            )
            Box(
                modifier = Modifier.weight(1f),
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
                                    SignUpStep.Initial -> InitialSignUpStep(model)
                                    SignUpStep.Middle -> MiddleSignUpStep(model)
                                    SignUpStep.Final -> FinalSignUpStep(model)
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.have_account_already),
            )
            TextButton(onClick = onSignIn) {
                Text(
                    text = stringResource(R.string.sign_in),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.inverseSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InitialSignUpStep(model: SignUpModel) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Face, "Account ID") },
            value = model.fullName,
            onValueChange = { model.fullName = it },
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
            value = model.dateOfBirth,
            onValueChange = { model.dateOfBirth = it },
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
        val condition = model.fullName.isNotEmpty() && model.dateOfBirth.isNotEmpty() &&
                model.address.isNotEmpty() && model.email.isNotEmpty() &&
                model.tag.isNotEmpty() && model.password.isNotEmpty() &&
                model.confirmPassword.isNotEmpty() && !model.isLoading && model.password == model.confirmPassword

        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { showPassword2 = !showPassword2 }) {
                    Icon(
                        painter = painterResource(
                            if (showPassword2) R.drawable.baseline_visibility_24
                            else R.drawable.baseline_visibility_off_24
                        ),
                        contentDescription = stringResource(R.string.show_password)
                    )
                }
            },
            visualTransformation = if (showPassword2) VisualTransformation.None else PasswordVisualTransformation(),
            value = model.confirmPassword,
            onValueChange = { model.confirmPassword = it },
            label = {
                Text(text = "Confirm Password")
            },
            keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { if (condition) model.doInitialStep(focusManager) })
        )

        Button(
            enabled = condition,
            onClick = { model.doInitialStep(focusManager) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.sign_up))
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiddleSignUpStep(model: SignUpModel) {
    val focusManager = LocalFocusManager.current
    BackHandler(onBack = model::goBack)

    Column(
        modifier = Modifier.padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(text = "Enter phone number:")
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = model.phoneNumber,
            placeholder = { Text("0733768565") },
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 10) model.phoneNumber = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = { model.doMiddleStep(focusManager) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Confirm")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalSignUpStep(model: SignUpModel) {
    val focusManager = LocalFocusManager.current
    BackHandler(onBack = model::goBack)
    LaunchedEffect(true) {
        model.code = ""
    }

    Column(
        modifier = Modifier.padding(25.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text(text = stringResource(R.string.six_digit_code_sent))
        TextField(
            singleLine = true,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, "Account ID") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            value = model.code,
            placeholder = { Text("123456") },
            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) model.code = it },
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick =  { model.doFinalStep(focusManager) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Confirm")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreen({}, {})
    }
}