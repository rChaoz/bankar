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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layoutId
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import io.ktor.client.request.parameter
import io.ktor.http.path
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.todayIn
import ro.bankar.app.LocalDataStore
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.ktor.Response
import ro.bankar.app.ktor.ktorClient
import ro.bankar.app.ktor.safeGet
import ro.bankar.app.ui.components.ButtonField
import ro.bankar.app.ui.components.ComboBox
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.createRefsFor
import ro.bankar.app.ui.monthStringResource
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
    var tagLoading by mutableStateOf(false) // used to display loading animation while verifying tag is unique
    val email = mutableStateOf("")
    val password = mutableStateOf("")
    val confirmPassword = mutableStateOf("")

    // For errors, null means unverified, empty string means valid input (has been verified)
    val tagError = mutableStateOf<String?>(null)
    val emailError = mutableStateOf<String?>(null)
    val passwordError = mutableStateOf<String?>(null)
    val confirmPasswordError = mutableStateOf<String?>(null)

    // Second step
    val firstName = mutableStateOf("")
    val middleName = mutableStateOf("")
    val lastName = mutableStateOf("")
    val dateOfBirth = mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()) - DatePeriod(18))

    val country = mutableStateOf("")
    val state = mutableStateOf("")
    val city = mutableStateOf("")
    val address = mutableStateOf("")

    // Errors
    val firstNameError = mutableStateOf<String?>(null)
    val middleNameError = mutableStateOf<String?>(null)
    val lastNameError = mutableStateOf<String?>(null)
    val dateOfBirthError = mutableStateOf<String?>(null)

    val countryError = mutableStateOf<String?>(null)
    val stateError = mutableStateOf<String?>(null)
    val cityError = mutableStateOf<String?>(null)
    val addressError = mutableStateOf<String?>(null)

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
                path("signup/check_tag")
                parameter("q", tag.value)
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
        confirmPasswordError.value = if (password.value == confirmPassword.value) "" else c.getString(R.string.passwords_dont_match)
    }

    // Verify that names are OK
    fun checkFirstName(c: Context) {
        firstNameError.value = if (SNewUser.nameRegex.matches(firstName.value.trim())) "" else c.getString(R.string.invalid_name)
    }

    fun checkMiddleName(c: Context) {
        middleNameError.value = if (SNewUser.nameRegex.matches(middleName.value.trim())) "" else c.getString(R.string.invalid_name)
    }

    fun checkLastName(c: Context) {
        lastNameError.value = if (SNewUser.nameRegex.matches(lastName.value.trim())) "" else c.getString(R.string.invalid_name)
    }

    fun checkDateOfBirth(c: Context) {
        val age = Clock.System.todayIn(TimeZone.currentSystemDefault()) - dateOfBirth.value
        dateOfBirthError.value = when (age.years) {
            in 0..SNewUser.ageRange.first -> c.getString(R.string.you_must_be_min_age)
            in SNewUser.ageRange -> ""
            else -> c.getString(R.string.invalid_date_of_birth)
        }
    }

    fun checkCity(c: Context) {
        cityError.value = if (city.value.trim().length in SNewUser.cityLengthRange) "" else c.getString(R.string.invalid_city)
    }

    fun checkAddress(c: Context) {
        addressError.value = if (address.value.trim().length in SNewUser.addressLengthRange) "" else c.getString(R.string.invalid_address)
    }

    // Next button clicked
    fun onNext(c: Context, focusManager: FocusManager) = viewModelScope.launch {
        isLoading = true
        focusManager.clearFocus()
        when (step) {
            SignUpStep.LoginInformation -> {
                checkEmail(c)
                checkPasswords(c, true)
                checkTagImpl(c)
                if (countries == null) {
                    // Try getting the data again
                    launch { loadCountries(this@SignUpModel) }
                    snackBar.showSnackbar(c.getString(R.string.connection_error))
                } else if (tagError.value == "" && emailError.value == "" && passwordError.value == "" && confirmPasswordError.value == "")
                    step = SignUpStep.PersonalInformation
            }
            SignUpStep.PersonalInformation -> {
                checkFirstName(c)
                checkMiddleName(c)
                checkLastName(c)
                checkDateOfBirth(c)
                checkCity(c)
                checkAddress(c)
                if (firstNameError.value == "" && middleNameError.value == "" && lastNameError.value == ""
                    && dateOfBirthError.value == "" && cityError.value == "" && addressError.value == "") step = SignUpStep.PhoneNumber
            }

            else -> {}
        }
        isLoading = false
    }
}

private suspend fun loadCountries(model: SignUpModel) {
    val r = ktorClient.safeGet<SCountries, StatusResponse> {
        url.path("data/countries.json")
    }
    if (r is Response.Success) {
        model.countries = r.result
        model.country.value = r.result[0].country
        model.state.value = r.result[0].states[0]
    }
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

    LaunchedEffect(true) { loadCountries(model) }

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
    val constraints = remember {
        ConstraintSet {
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
                top.linkTo(tagText.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(emailPasswordText) { top.linkTo(tag.bottom, 4.dp) }
            constrain(email) {
                top.linkTo(emailPasswordText.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(password) {
                top.linkTo(email.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(confirmPassword) {
                top.linkTo(password.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(passwordRequirements) { top.linkTo(confirmPassword.bottom, 4.dp) }

            // Show password checkbox & next button
            constrain(surface) { linkTo(button.top, button.bottom) }
            constrain(button) {
                top.linkTo(passwordRequirements.bottom, 12.dp)
                end.linkTo(parent.end)
            }
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
            modifier = Modifier.layoutId("tagText"),
            text = stringResource(R.string.first_pick_a_tag)
        )
        Text(
            modifier = Modifier.layoutId("emailPasswordText"),
            text = stringResource(R.string.next_email_password)
        )
        Text(
            modifier = Modifier.layoutId("passwordRequirements"),
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

        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        Button(onClick = { model.onNext(context, focusManager) }, modifier = Modifier.layoutId("button")) {
            Text(stringResource(R.string.next))
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInformationStep(model: SignUpModel) {
    val context = LocalContext.current
    val datePickerDialog = rememberUseCaseState(embedded = true)
    val selectedDate = remember {
        CalendarSelection.Date(selectedDate = model.dateOfBirth.value.toJavaLocalDate(), onSelectDate = {
            model.dateOfBirth.value = it.toKotlinLocalDate()
            model.checkDateOfBirth(context)
        })
    }
    CompositionLocalProvider(LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 1.dp) {
        CalendarDialog(
            state = datePickerDialog, selection = selectedDate,
            config = CalendarConfig(yearSelection = true, boundary = Clock.System.todayIn(TimeZone.currentSystemDefault()).let {
                (it - DatePeriod(110)).toJavaLocalDate()..(it - DatePeriod(10)).toJavaLocalDate()
            }),
        )
    }

    val constraints = remember {
        ConstraintSet {
            val (iconName, iconDate, iconCity, iconAddress) = createRefsFor("iconName", "iconDate", "iconCity", "iconAddress")
            val (firstName, middleName, lastName, dateOfBirth) = createRefsFor("firstName", "middleName", "lastName", "dateOfBirth")
            val (country, state, city, address, button) = createRefsFor("country", "state", "city", "address", "button")
            val iconsBarrier = createEndBarrier(iconName, iconCity, iconAddress, margin = 12.dp)

            // Icons
            constrain(iconName) { linkTo(firstName.top, firstName.bottom, bottomMargin = 8.dp) }
            constrain(iconDate) { linkTo(dateOfBirth.top, dateOfBirth.bottom, bottomMargin = 8.dp) }
            constrain(iconCity) { linkTo(country.top, country.bottom, bottomMargin = 8.dp) }
            constrain(iconAddress) { linkTo(address.top, address.bottom, bottomMargin = 8.dp) }

            // Fields
            constrain(firstName) {
                linkTo(iconsBarrier, middleName.start, endMargin = 8.dp)
                width = Dimension.fillToConstraints
            }
            constrain(middleName) {
                linkTo(firstName.end, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(lastName) {
                top.linkTo(firstName.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(dateOfBirth) {
                top.linkTo(lastName.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(country) {
                top.linkTo(dateOfBirth.bottom, 4.dp)
                linkTo(iconsBarrier, state.start, endMargin = 8.dp)
                width = Dimension.fillToConstraints
            }
            constrain(state) {
                top.linkTo(dateOfBirth.bottom, 4.dp)
                linkTo(country.end, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(city) {
                top.linkTo(country.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }
            constrain(address) {
                top.linkTo(city.bottom, 4.dp)
                linkTo(iconsBarrier, parent.end)
                width = Dimension.fillToConstraints
            }

            // Button
            constrain(button) {
                top.linkTo(address.bottom, 12.dp)
                end.linkTo(parent.end)
            }
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
        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.layoutId("iconName"))
        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.layoutId("iconDate"))
        Icon(painter = painterResource(R.drawable.baseline_city_24), contentDescription = null, modifier = Modifier.layoutId("iconCity"))
        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.layoutId("iconAddress"))

        // Fields
        SignUpField(model.firstName, model.firstNameError, model::checkFirstName, label = R.string.first_name, type = KeyboardType.Text, id = "firstName")
        SignUpField(model.middleName, model.middleNameError, model::checkMiddleName, label = R.string.middle_name, type = KeyboardType.Text, id = "middleName")
        SignUpField(model.lastName, model.lastNameError, model::checkLastName, label = R.string.last_name, type = KeyboardType.Text, id = "lastName")
        ButtonField(
            value = with(model.dateOfBirth.value) { "$dayOfMonth ${monthStringResource(month)} $year" },
            onClick = { datePickerDialog.show() },
            label = R.string.date_of_birth,
            supportingText = "",
            modifier = Modifier.layoutId("dateOfBirth"),
        )
        ComboBox(
            selectedItem = model.country.value,
            onSelectItem = { item -> model.country.value = item; model.state.value = model.countries!!.first { it.country == item }.states[0] },
            items = remember { model.countries!!.map { it.country } },
            label = R.string.country,
            modifier = Modifier.layoutId("country"),
            supportingText = "",
        )
        ComboBox(
            selectedItem = model.state.value,
            onSelectItem = { model.state.value = it },
            items = remember(model.country.value) { model.countries!!.first { it.country == model.country.value }.states },
            label = R.string.state_province,
            modifier = Modifier.layoutId("state"),
            supportingText = "",
        )
        SignUpField(model.city, model.cityError, model::checkCity, label = R.string.city, type = KeyboardType.Text, id = "city")
        SignUpField(model.address, model.addressError, model::checkAddress, label = R.string.address,
            type = KeyboardType.Text, id = "address", multiLine = true)

        // Button
        val focusManager = LocalFocusManager.current
        Button(onClick = { model.onNext(context, focusManager) }, modifier = Modifier.layoutId("button")) {
            Text(stringResource(R.string.next))
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
    id: String,
    label: Int,
    type: KeyboardType,
    showPassword: Boolean = true,
    isLast: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    multiLine: Boolean = false,
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = state.value,
        onValueChange = state.component2(),
        singleLine = !multiLine,
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

@Preview
@Composable
private fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreen({}, {})
    }
}