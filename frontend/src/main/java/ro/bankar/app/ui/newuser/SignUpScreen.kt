package ro.bankar.app.ui.newuser

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.request.parameter
import io.ktor.http.path
import kotlinx.coroutines.launch
import ro.bankar.app.LocalDataStore
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.ktor.Response
import ro.bankar.app.ktor.ktorClient
import ro.bankar.app.ktor.safeGet
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.createRefsFor
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SCountries
import ro.bankar.model.SNewUser
import ro.bankar.model.StatusResponse

enum class SignUpStep {
    LoginInformation, PersonalInformation, PhoneNumber, SmsCode
}

class SignUpModel : ViewModel() {
    // Internal state
    var step by mutableStateOf(SignUpStep.LoginInformation)
    var isLoading by mutableStateOf(false)
    val snackBar = SnackbarHostState()

    // List of countries & country codes
    var countries by mutableStateOf<SCountries?>(null)

    // First step
    val tag = mutableStateOf("")
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")

    var tagLoading by mutableStateOf(false)

    // For errors, null means unverified, empty string means valid input (has been verified)
    val tagError = mutableStateOf<String?>(null)
    val emailError = mutableStateOf<String?>(null)
    val passwordError = mutableStateOf<String?>(null)
    val confirmPasswordError = mutableStateOf<String?>(null)

    // Second step
    var firstName by mutableStateOf("")
    var middleName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var dateOfBirth by mutableStateOf("")
    var address by mutableStateOf("")

    // Second step
    var phoneNumber by mutableStateOf("")

    // Final step
    var code by mutableStateOf("")

    // Set by model
    lateinit var onSuccess: () -> Unit
    var dataStore: DataStore<Preferences>? = null

    // Verify tag validity & that it isn't taken
    fun checkTag(c: Context) = viewModelScope.launch { checkTagImpl(c) }

    private suspend fun checkTagImpl(c: Context) {
        // Check that tag is valid
        when {
            tag.value.length < SNewUser.tagLengthRange.first -> c.getString(R.string.tag_too_short).format(SNewUser.tagLengthRange.first)
            tag.value.length > SNewUser.tagLengthRange.last -> c.getString(R.string.tag_too_long).format(SNewUser.tagLengthRange.last)
            !SNewUser.tagRegex.matches(tag.value) -> c.getString(R.string.invalid_tag)
            else -> null
        }?.let {
            tagError.value = it
            return
        }

        tagLoading = true
        tagError.value = null
        val result = ktorClient.safeGet<StatusResponse, StatusResponse> {
            url {
                path("check_tag")
                parameter("q", tag)
            }
        }
        tagLoading = false
        tagError.value = when (result) {
            is Response.Error -> c.getString(result.message)

            is Response.Fail -> when (result.s.status) {
                "invalid_tag" -> c.getString(R.string.invalid_tag)
                "exists" -> c.getString(R.string.tag_already_exists)
                else -> c.getString(R.string.unknown_error)
            }

            is Response.Success -> ""
        }
    }

    // Verify e-mail
    fun checkEmail(c: Context) {
        emailError.value = if (SNewUser.emailRegex.matches(email.value)) "" else c.getString(R.string.invalid_email)
    }

    // Verify that passwords are OK
    fun checkPasswords(c: Context, checkConfirm: Boolean) {
        passwordError.value = if (SNewUser.passwordRegex.matches(password.value)) ""
        else c.getString(R.string.password_doesnt_meet)
        if (!checkConfirm) {
            confirmPasswordError.value = null
            return
        }
        confirmPasswordError.value = if (password == confirmPassword) "" else c.getString(R.string.passwords_dont_match)
    }

    // Next button clicked
    fun onNext(c: Context) = viewModelScope.launch {
        isLoading = true
        when (step) {
            SignUpStep.LoginInformation -> {
                checkEmail(c)
                checkPasswords(c, true)
                checkTagImpl(c)
                if (countries == null) {
                    // Try getting the data again
                    launch {
                        countries = getCountries()
                        snackBar.showSnackbar(c.getString(R.string.connection_error))
                    }
                } else if (tagError.value == "" && emailError.value == "" && passwordError.value == "" && confirmPasswordError.value == "")
                    step = SignUpStep.PersonalInformation
            }

            else -> {}
        }
        isLoading = false
    }
}

private suspend fun getCountries(): SCountries? {
    val r = ktorClient.safeGet<SCountries, StatusResponse> {
        url.path("data/countries.json")
    }
    return if (r is Response.Success) r.result
    else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onSignIn: () -> Unit, onSuccess: () -> Unit) {
    val themeMode = LocalThemeMode.current
    val model: SignUpModel = viewModel()
    model.dataStore = LocalDataStore.current
    model.onSuccess = onSuccess

    BackHandler(enabled = model.step.ordinal != 0) {
        model.step = SignUpStep.values()[model.step.ordinal - 1]
    }

    LaunchedEffect(true) { model.countries = getCountries() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        snackbarHost = { SnackbarHost(model.snackBar) }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                .padding(padding)
                .padding(vertical = 15.dp)
        ) {
            ThemeToggle(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 10.dp),
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
                                    SignUpStep.LoginInformation -> LoginInformationStep(model)
                                    SignUpStep.PersonalInformation -> PersonalInformationStep(model)
                                    else -> {}
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
private fun LoginInformationStep(model: SignUpModel) {
    val context = LocalContext.current

    val constraints = ConstraintSet {
        val (tagIcon, emailIcon, passwordIcon) = createRefsFor("tagIcon", "emailIcon", "passwordIcon")
        val (tag, email, password, confirmPassword) = createRefsFor("tag", "email", "password", "confirmPassword")
        val (tagText, emailPasswordText, passwordRequirements) = createRefsFor("tagText", "emailPasswordText", "passwordRequirements")
        val (surface, button) = createRefsFor("surface", "button")
        val iconsBarrier = createEndBarrier(tagIcon, emailIcon, passwordIcon, margin = 12.dp)

        // Icons
        constrain(tagIcon) { linkTo(tag.top, tag.bottom, bottomMargin = 8.dp) }
        constrain(emailIcon) { linkTo(email.top, email.bottom, bottomMargin = 8.dp) }
        constrain(passwordIcon) { linkTo(password.top, password.bottom, bottomMargin = 8.dp) }

        // Text fields & texts
        constrain(tag) {
            top.linkTo(tagText.bottom)
            linkTo(iconsBarrier, parent.end)
            width = Dimension.fillToConstraints
        }
        constrain(emailPasswordText) { top.linkTo(tag.bottom) }
        constrain(email) {
            top.linkTo(emailPasswordText.bottom)
            linkTo(iconsBarrier, parent.end)
            width = Dimension.fillToConstraints
        }
        constrain(password) {
            top.linkTo(email.bottom)
            linkTo(iconsBarrier, parent.end)
            width = Dimension.fillToConstraints
        }
        constrain(confirmPassword) {
            top.linkTo(password.bottom)
            linkTo(iconsBarrier, parent.end)
            width = Dimension.fillToConstraints
        }
        constrain(passwordRequirements) { top.linkTo(confirmPassword.bottom) }

        // Show password checkbox & next button
        constrain(surface) { linkTo(button.top, button.bottom) }
        constrain(button) {
            top.linkTo(passwordRequirements.bottom, 8.dp)
            end.linkTo(parent.end)
        }
    }

    ConstraintLayout(
        constraints,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Icons
        Icon(painterResource(R.drawable.baseline_tag_24), stringResource(R.string.tag), Modifier.layoutId("tagIcon"))
        Icon(Icons.Default.Email, stringResource(R.string.email), Modifier.layoutId("emailIcon"))
        Icon(Icons.Default.Lock, stringResource(R.string.password), Modifier.layoutId("passwordIcon"))

        // Text fields
        SignUpField(model.tag, model.tagError, model::checkTag, label = R.string.tag, type = KeyboardType.Email, id = "tag",
            trailingIcon = if (model.tagLoading) {
                { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp) }
            } else if (model.tagError.value == "") {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(android.R.string.ok),
                        tint = MaterialTheme.customColors.green,
                    )
                }
            } else null
        )
        val (showPassword, setShowPassword) = remember { mutableStateOf(false) }
        SignUpField(model.email, model.emailError, model::checkEmail, label = R.string.email, type = KeyboardType.Email, id = "email")
        SignUpField(
            model.password, model.passwordError, { model.checkPasswords(it, false) }, label = R.string.password,
            type = KeyboardType.Password, id = "password", showPassword = showPassword
        )
        SignUpField(
            model.confirmPassword, model.confirmPasswordError, { model.checkPasswords(it, false) }, label = R.string.confirm_password,
            type = KeyboardType.Password, id = "confirmPassword", showPassword = showPassword, isLast = true
        )

        // Texts
        Text(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .layoutId("tagText"),
            text = stringResource(R.string.first_pick_a_tag)
        )
        Text(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .layoutId("emailPasswordText"),
            text = stringResource(R.string.next_email_password)
        )
        Text(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .layoutId("passwordRequirements"),
            text = stringResource(R.string.password_requirements),
            style = MaterialTheme.typography.labelSmall,
        )

        // Show password checkbox & signup button
        Surface(
            modifier = Modifier.layoutId("surface"),
            shape = RoundedCornerShape(8.dp),
            checked = showPassword,
            onCheckedChange = setShowPassword
        ) {
            Row(modifier = Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showPassword, onCheckedChange = setShowPassword)
                Text(stringResource(R.string.show_password))
            }
        }

        Button(onClick = { model.onNext(context) }, modifier = Modifier.layoutId("button")) {
            Text(stringResource(R.string.next))
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInformationStep(model: SignUpModel) {
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
            onClick = {},
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Confirm")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinalSignUpStep(model: SignUpModel) {
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
            onClick = {},
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Confirm")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpField(
    state: MutableState<String>,
    error: MutableState<String?>,
    validate: (Context) -> Unit,
    label: Int,
    type: KeyboardType,
    showPassword: Boolean = true,
    isLast: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    id: String
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = state.value,
        onValueChange = state.component2(),
        singleLine = true,
        modifier = Modifier
            .layoutId(id)
            .onFocusChanged {
                if (it.isFocused) error.value = null
                else if (state.value.isNotEmpty()) validate(context)
            },
        trailingIcon = trailingIcon,
        label = { Text(text = stringResource(label)) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = if (isLast) ImeAction.Done else ImeAction.Next, keyboardType = type),
        isError = !error.value.isNullOrEmpty(),
        supportingText = { Text(text = error.value ?: "") }
    )
}

@Preview(showBackground = true)
@Composable
private fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreen({}, {})
    }
}