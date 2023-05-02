package ro.bankar.app.ui.newuser

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.http.path
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import ro.bankar.app.LocalDataStore
import ro.bankar.app.LocalThemeMode
import ro.bankar.app.R
import ro.bankar.app.USER_SESSION
import ro.bankar.app.ktor.SafeResponse
import ro.bankar.app.ktor.SafeStatusResponse
import ro.bankar.app.ktor.ktorClient
import ro.bankar.app.ktor.safeGet
import ro.bankar.app.ktor.safePost
import ro.bankar.app.ktor.safeRequest
import ro.bankar.app.setPreference
import ro.bankar.app.ui.VerifiableState
import ro.bankar.app.ui.components.ButtonField
import ro.bankar.app.ui.components.ComboBox
import ro.bankar.app.ui.components.LoadingOverlay
import ro.bankar.app.ui.components.ThemeToggle
import ro.bankar.app.ui.createRefsFor
import ro.bankar.app.ui.monthStringResource
import ro.bankar.app.ui.safeDecodeFromString
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.app.ui.verifiableStateOf
import ro.bankar.app.ui.verifiableSuspendingStateOf
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SCountries
import ro.bankar.model.SCountry
import ro.bankar.model.SNewUser
import ro.bankar.model.SSMSCodeData
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
    val tag = verifiableSuspendingStateOf("", viewModelScope) {
        // Check that tag is valid
        when {
            it.length < SNewUser.tagLengthRange.first -> getString(R.string.tag_too_short).format(SNewUser.tagLengthRange.first)
            it.length > SNewUser.tagLengthRange.last -> getString(R.string.tag_too_long).format(SNewUser.tagLengthRange.last)
            !SNewUser.tagRegex.matches(it) -> getString(R.string.invalid_tag)
            else -> null
        }?.let { error ->
            return@verifiableSuspendingStateOf error
        }

        val result = ktorClient.safeGet<StatusResponse, StatusResponse> {
            url("signup/check_tag")
            parameter("q", it)
        }

        when (result) {
            is SafeStatusResponse.InternalError -> getString(result.message)
            is SafeStatusResponse.Fail -> when (result.s.status) {
                "invalid_tag" -> getString(R.string.invalid_tag)
                "exists" -> getString(R.string.tag_already_exists)
                else -> getString(R.string.unknown_error)
            }
            is SafeStatusResponse.Success -> null
        }
    }
    val email = verifiableStateOf("", R.string.invalid_email) { SNewUser.emailRegex.matches(it.trim()) }
    val password = verifiableStateOf("", R.string.password_doesnt_meet) { SNewUser.passwordRegex.matches(it) }
    val confirmPassword = verifiableStateOf("", R.string.password_doesnt_meet) { it == password.value }

    // Second step
    val firstName = verifiableStateOf("", R.string.invalid_name) { SNewUser.nameRegex.matches(it.trim()) }
    val middleName = verifiableStateOf("", R.string.invalid_name) { it.isEmpty() || SNewUser.nameRegex.matches(it.trim()) }
    val lastName = verifiableStateOf("", R.string.invalid_name) { SNewUser.nameRegex.matches(it.trim()) }
    val dateOfBirth = verifiableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()) - DatePeriod(18)) {
        val age = Clock.System.todayIn(TimeZone.currentSystemDefault()) - it
        when (age.years) {
            in 0..SNewUser.ageRange.first -> getString(R.string.you_must_be_min_age)
            in SNewUser.ageRange -> null
            else -> getString(R.string.invalid_date_of_birth)
        }
    }
    val country = mutableStateOf(SCountry("null", "null", "null", emptyList()))
    val state = mutableStateOf("")
    val city = verifiableStateOf("", R.string.invalid_city) { it.trim().length in SNewUser.cityLengthRange }
    val address = verifiableStateOf("", R.string.invalid_address) { it.trim().length in SNewUser.addressLengthRange }

    // Final steps
    val phoneCountry = mutableStateOf<SCountry?>(null)
    val countryCode get() = (phoneCountry.value ?: country.value).dialCode
    val phone = verifiableStateOf("", R.string.invalid_phone) {
        SNewUser.phoneRegex.matches(countryCode + it)
    }
    var code by mutableStateOf("")
    var codeError by mutableStateOf("")

    // Set by model
    lateinit var onSuccess: () -> Unit
    lateinit var dataStore: DataStore<Preferences>

    // Next (or confirm) button clicked
    private var signupSession by mutableStateOf<String?>(null)
    fun onNext(c: Context, focusManager: FocusManager): Job = viewModelScope.launch {
        isLoading = true
        focusManager.clearFocus()
        when (step) {
            SignUpStep.LoginInformation -> {
                email.check(c)
                password.check(c)
                confirmPassword.check(c)
                tag.checkSuspending(c)
                if (!tag.verified || !email.verified || !password.verified || !confirmPassword.verified) {
                    isLoading = false
                    return@launch
                }

                // Check e-mail isn't taken
                val result = ktorClient.safeGet<StatusResponse, StatusResponse> {
                    url("signup/check_email")
                    parameter("q", email.value)
                }
                when (result) {
                    is SafeStatusResponse.InternalError -> {
                        isLoading = false
                        snackBar.showSnackbar(c.getString(result.message), withDismissAction = true)
                        return@launch
                    }
                    is SafeStatusResponse.Fail -> {
                        when (result.s.status) {
                            "invalid_email" -> email.setError(c.getString(R.string.invalid_email))
                            "exists" -> {
                                email.setError(c.getString(R.string.already_registered))
                                launch { snackBar.showSnackbar(c.getString(R.string.email_already_in_use), withDismissAction = true) }
                            }
                            else -> launch { snackBar.showSnackbar(c.getString(R.string.invalid_server_response), withDismissAction = true) }
                        }
                        isLoading = false
                        return@launch
                    }
                    is SafeStatusResponse.Success -> {}
                }

                if (countries == null) {
                    // Try getting the data again
                    launch { loadCountries() }
                    snackBar.showSnackbar(c.getString(R.string.connection_error), withDismissAction = true)
                } else if (tag.verified && email.verified && password.verified && confirmPassword.verified)
                    step = SignUpStep.PersonalInformation
            }
            SignUpStep.PersonalInformation -> {
                firstName.check(c)
                middleName.check(c)
                lastName.check(c)
                dateOfBirth.check(c)
                city.check(c)
                address.check(c)
                if (firstName.verified && middleName.verified && lastName.verified && dateOfBirth.verified && city.verified && address.verified)
                    step = SignUpStep.PhoneNumber
            }
            SignUpStep.PhoneNumber -> {
                phone.check(c)
                if (phone.verified) {
                    val result = ktorClient.safePost<StatusResponse, InvalidParamResponse> {
                        url("signup/initial")
                        setBody(
                            SNewUser(
                                email = email.value,
                                tag = tag.value,
                                phone = countryCode + phone.value,
                                password = password.value,
                                firstName = firstName.value.trim(),
                                middleName = middleName.value.trim().takeIf(String::isNotEmpty),
                                lastName = lastName.value.trim(),
                                dateOfBirth = dateOfBirth.value,
                                countryCode = country.value.code,
                                state = state.value,
                                city = city.value.trim(),
                                address = address.value.trim(),
                            )
                        )
                    }
                    launch {
                        when (result) {
                            is SafeStatusResponse.InternalError -> snackBar.showSnackbar(c.getString(result.message), withDismissAction = true)
                            is SafeStatusResponse.Fail ->
                                if (result.s.status == "already_exists") {
                                    if (result.s.param == "phone") {
                                        phone.setError(c.getString(R.string.already_registered))
                                        snackBar.showSnackbar(c.getString(R.string.number_already_in_use), withDismissAction = true)
                                    } else snackBar.showSnackbar(c.getString(R.string.user_already_exists).format(result.s.param), withDismissAction = true)
                                } else snackBar.showSnackbar(c.getString(R.string.invalid_field).format(result.s.param), withDismissAction = true)
                            is SafeStatusResponse.Success -> {
                                code = ""
                                signupSession = result.r.headers["SignupSession"]
                                step = SignUpStep.SmsCode
                            }
                        }
                    }
                }
            }
            SignUpStep.SmsCode -> {
                if (code.length != 6) {
                    codeError = c.getString(R.string.code_has_6_digits)
                    isLoading = false
                    return@launch
                }
                codeError = ""
                val result = ktorClient.safeRequest<StatusResponse>(HttpStatusCode.Created) {
                    post("signup/final") {
                        header("SignupSession", signupSession)
                        setBody(SSMSCodeData(code))
                    }
                }
                launch {
                    when (result) {
                        is SafeResponse.InternalError -> snackBar.showSnackbar(c.getString(result.message), withDismissAction = true)
                        is SafeResponse.Fail -> {
                            val sessionStatus = Json.safeDecodeFromString<StatusResponse>(result.body)
                            val existsStatus = Json.safeDecodeFromString<InvalidParamResponse>(result.body)
                            when {
                                sessionStatus?.status == "invalid_session" || sessionStatus?.status == "session_expired" -> {
                                    if (snackBar.showSnackbar(c.getString(R.string.signup_session_expired), actionLabel = c.getString(R.string.resend))
                                        == SnackbarResult.ActionPerformed
                                    ) {
                                        step = SignUpStep.PhoneNumber
                                        onNext(c, focusManager)
                                    }
                                }
                                sessionStatus?.status == "invalid_code" -> codeError = c.getString(R.string.incorrect_code)
                                existsStatus?.status == "already_exists" ->
                                    snackBar.showSnackbar(c.getString(R.string.user_already_exists).format(existsStatus.param), withDismissAction = true)
                                else -> snackBar.showSnackbar(c.getString(R.string.invalid_server_response), withDismissAction = true)
                            }
                        }
                        is SafeResponse.Success -> {
                            result.r.headers["Authorization"]?.removePrefix("Bearer ")?.let { dataStore.setPreference(USER_SESSION, it) }
                            onSuccess()
                        }
                    }
                }
            }
        }
        isLoading = false
    }

    // Load country data
    suspend fun loadCountries() {
        val r = ktorClient.safeGet<SCountries, StatusResponse> {
            url.path("data/countries.json")
        }
        if (r is SafeStatusResponse.Success) {
            countries = r.result
            country.value = r.result[0]
            state.value = r.result[0].states[0]
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onSignIn: () -> Unit, onSuccess: () -> Unit) {
    val themeMode = LocalThemeMode.current
    val model: SignUpModel = viewModel()
    model.dataStore = LocalDataStore.current
    model.onSuccess = onSuccess

    val onBack = { model.step = SignUpStep.values()[model.step.ordinal - 1] }
    BackHandler(enabled = model.step.ordinal != 0, onBack)

    LaunchedEffect(true) { model.loadCountries() }

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
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = model.step != SignUpStep.values().first(),
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Text(
                            "Sign Up",
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
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
                                    if (targetState == SignUpStep.SmsCode || initialState == SignUpStep.SmsCode)
                                        EnterTransition.None with ExitTransition.None
                                    else if (targetState.ordinal > initialState.ordinal) {
                                        (slideInHorizontally { w -> w } with slideOutHorizontally { w -> -w })
                                    } else {
                                        (slideInHorizontally { w -> -w } with slideOutHorizontally { w -> w })
                                    }
                                }
                            ) {
                                when (it) {
                                    SignUpStep.LoginInformation -> LoginInformationStep(model)
                                    SignUpStep.PersonalInformation -> PersonalInformationStep(model)
                                    SignUpStep.PhoneNumber, SignUpStep.SmsCode -> PhoneNumberStep(model)
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = model.step == SignUpStep.LoginInformation) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(R.string.have_account_already))
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
        SignUpField(model.tag, label = R.string.tag, type = KeyboardType.Email, id = "tag",
            trailingIcon = if (model.tag.verifying) {
                { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp) }
            } else if (model.tag.verified) {
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
        SignUpField(model.email, label = R.string.email, type = KeyboardType.Email, id = "email")
        SignUpField(model.password, label = R.string.password, type = KeyboardType.Password, id = "password", showPassword = showPassword)
        SignUpField(
            model.confirmPassword, label = R.string.confirm_password,
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
            model.dateOfBirth.check(context)
        })
    }
    CompositionLocalProvider(LocalAbsoluteTonalElevation provides LocalAbsoluteTonalElevation.current + 1.dp) {
        CalendarDialog(
            state = datePickerDialog, selection = selectedDate,
            config = CalendarConfig(yearSelection = true, monthSelection = true, boundary = Clock.System.todayIn(TimeZone.currentSystemDefault()).let {
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
        SignUpField(model.firstName, label = R.string.first_name, type = KeyboardType.Text, id = "firstName")
        SignUpField(model.middleName, label = R.string.middle_name, type = KeyboardType.Text, id = "middleName")
        SignUpField(model.lastName, label = R.string.last_name, type = KeyboardType.Text, id = "lastName")
        ButtonField(
            value = with(model.dateOfBirth.value) { "$dayOfMonth ${monthStringResource(month)} $year" },
            onClick = { datePickerDialog.show() },
            label = R.string.date_of_birth,
            supportingText = "",
            modifier = Modifier.layoutId("dateOfBirth"),
        )
        ComboBox(
            selectedItemText = model.country.value.country,
            onSelectItem = { item ->
                if (item !== model.country.value) {
                    model.country.value = item
                    model.state.value = item.states[0]
                }
            },
            items = model.countries!!,
            label = R.string.country,
            modifier = Modifier.layoutId("country"),
            supportingText = "",
            dropdownItemGenerator = { item, onClick -> DropdownMenuItem(text = { Text(text = item.country) }, onClick) }
        )
        ComboBox(
            selectedItemText = model.state.value,
            onSelectItem = { model.state.value = it },
            items = model.country.value.states,
            label = R.string.state_province,
            modifier = Modifier.layoutId("state"),
            supportingText = "",
        )
        SignUpField(model.city, label = R.string.city, type = KeyboardType.Text, id = "city")
        SignUpField(model.address, label = R.string.address, type = KeyboardType.Text, id = "address", multiLine = true)

        // Button
        val focusManager = LocalFocusManager.current
        Button(onClick = { model.onNext(context, focusManager) }, modifier = Modifier.layoutId("button")) {
            Text(stringResource(R.string.next))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneNumberStep(model: SignUpModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .padding(20.dp)
            .animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = stringResource(R.string.enter_mobile_phone))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.padding(bottom = 12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            ComboBox(
                selectedItemText = model.countryCode,
                onSelectItem = { model.phoneCountry.value = it },
                label = R.string.prefix,
                enabled = model.step == SignUpStep.PhoneNumber,
                items = model.countries!!, modifier = Modifier.width(110.dp), dropdownItemGenerator = { item, onClick ->
                    DropdownMenuItem(text = {
                        Column {
                            Text(text = item.country, style = MaterialTheme.typography.labelMedium)
                            Text(text = item.dialCode)
                        }
                    }, onClick)
                },
                isError = model.phone.hasError,
                supportingText = ""
            )
            SignUpField(
                model.phone, label = R.string.phone, type = KeyboardType.Phone,
                enabled = model.step == SignUpStep.PhoneNumber, filter = { it.all(Char::isDigit) },
                modifier = Modifier.weight(1f), isLast = true
            )
        }
        if (model.step == SignUpStep.PhoneNumber) {
            Button(modifier = Modifier.align(Alignment.End), onClick = { model.onNext(context, focusManager) }) {
                Text(text = stringResource(R.string.next))
            }
        } else {
            Text(text = stringResource(R.string.code_was_sent).format(model.countryCode + model.phone.value))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = model.code,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 6) model.code = it },
                    singleLine = true,
                    supportingText = { Text(text = model.codeError) },
                    isError = model.codeError.isNotEmpty(),
                    placeholder = { Text(text = "123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    keyboardActions = KeyboardActions(onDone = { model.onNext(context, focusManager) })
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { model.step = SignUpStep.PhoneNumber }) {
                    Text(text = stringResource(R.string.edit_phone))
                }
                Button(onClick = { model.onNext(context, focusManager) }) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpField(
    verifiableState: VerifiableState<String>,
    label: Int,
    type: KeyboardType,
    modifier: Modifier = Modifier,
    filter: (String) -> Boolean = { true },
    id: String? = null,
    enabled: Boolean = true,
    showPassword: Boolean = true,
    isLast: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    multiLine: Boolean = false,
) {
    val context = LocalContext.current
    var mod = modifier.onFocusChanged {
        if (it.isFocused) verifiableState.clearError()
        else if (verifiableState.value.isNotEmpty()) verifiableState.check(context)
    }
    if (id != null) mod = mod.layoutId(id)
    OutlinedTextField(
        value = verifiableState.value,
        onValueChange = { if (filter(it)) verifiableState.value = it },
        modifier = mod,
        enabled = enabled,
        singleLine = !multiLine,
        trailingIcon = trailingIcon,
        label = { Text(text = stringResource(label)) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = if (isLast) ImeAction.Done else ImeAction.Next, keyboardType = type),
        isError = verifiableState.hasError,
        supportingText = { Text(text = verifiableState.error ?: "") }
    )
}

@Preview
@Composable
private fun SignUpScreenPreview() {
    AppTheme {
        SignUpScreen({}, {})
    }
}