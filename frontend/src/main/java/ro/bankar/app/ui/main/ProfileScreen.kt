package ro.bankar.app.ui.main

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.maxkeppeker.sheets.core.models.base.SelectionButton
import com.maxkeppeker.sheets.core.models.base.UseCaseState
import com.maxkeppeler.sheets.info.InfoDialog
import com.maxkeppeler.sheets.info.models.InfoBody
import com.maxkeppeler.sheets.info.models.InfoSelection
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.minus
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.data.handle
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.ButtonField
import ro.bankar.app.ui.components.ComboBox
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.nameFromCode
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.banking.SCountries
import ro.bankar.banking.SCountry
import ro.bankar.model.ErrorResponse
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SNewUser
import ro.bankar.model.SUser
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.SUserValidation
import ro.bankar.model.SuccessResponse
import ro.bankar.util.format
import ro.bankar.util.todayHere
import java.io.ByteArrayOutputStream

class ProfileScreenModel : ViewModel() {
    var data by mutableStateOf<SUser?>(null)
    var countryData by mutableStateOf<SCountries?>(null)

    val email = verifiableStateOf("", R.string.invalid_email) { SUserValidation.emailRegex.matches(it.trim()) }
    val firstName = verifiableStateOf("", R.string.invalid_name) { SUserValidation.nameRegex.matches(it.trim()) }
    val middleName = verifiableStateOf("", R.string.invalid_name) { it.isBlank() || SUserValidation.nameRegex.matches(it.trim()) }
    val lastName = verifiableStateOf("", R.string.invalid_name) { SUserValidation.nameRegex.matches(it.trim()) }
    val dateOfBirth = verifiableStateOf(Clock.System.todayHere()) {
        val age = Clock.System.todayHere() - it
        when (age.years) {
            in 0..SUserValidation.ageRange.first -> getString(R.string.you_must_be_min_age)
            in SUserValidation.ageRange -> null
            else -> getString(R.string.invalid_date_of_birth)
        }
    }
    val country = mutableStateOf(SCountry("null", "null", "null", emptyList()))
    var state by mutableStateOf("")
    val city = verifiableStateOf("", R.string.invalid_city) { it.trim().length in SUserValidation.cityLengthRange }
    val address = verifiableStateOf("", R.string.invalid_address) { it.trim().length in SUserValidation.addressLengthRange }

    // To confirm changes
    var password = verifiableStateOf("", R.string.please_enter_password) { it.isNotEmpty() }

    @OptIn(ExperimentalFoundationApi::class)
    var passwordScrollRequester = BringIntoViewRequester()

    // Copy initial data into all fields and enable editing
    var editing by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)

    // countryData and data are assumed to be non-null for this function
    fun onEdit() {
        val data = this.data!!
        val country = countryData!!.find { it.code == data.countryCode } ?: countryData!![0]
        email.value = data.email
        firstName.value = data.firstName
        middleName.value = data.middleName ?: ""
        lastName.value = data.lastName
        dateOfBirth.value = data.dateOfBirth
        this.country.value = country
        state = data.state
        city.value = data.city
        address.value = data.address
        password.value = ""
        editing = true
    }


    fun onSave(context: Context, repository: Repository, snackBar: SnackbarHostState) = viewModelScope.launch {
        onSaveSuspending(context, repository, snackBar)
    }

    @OptIn(ExperimentalFoundationApi::class)
    suspend fun onSaveSuspending(context: Context, repository: Repository, snackbar: SnackbarHostState): Boolean = coroutineScope {
        email.check(context)
        firstName.check(context)
        middleName.check(context)
        lastName.check(context)
        dateOfBirth.check(context)
        city.check(context)
        address.check(context)
        password.check(context)
        if (!email.verified || !firstName.verified || !middleName.verified || !lastName.verified || !dateOfBirth.verified
            || !city.verified || !address.verified
        ) return@coroutineScope false
        if (!password.verified) {
            passwordScrollRequester.bringIntoView()
            return@coroutineScope false
        }
        isSaving = true
        repository.sendProfileUpdate(
            SNewUser(
                email.value, "", "", password.value, firstName.value.trim(), middleName.value.trim().ifEmpty { null }, lastName.value.trim(),
                dateOfBirth.value, country.value.code, state, city.value, address.value
            )
        ).handle(this, snackbar, context) {
            when (it) {
                SuccessResponse -> {
                    launch { snackbar.showSnackbar(context.getString(R.string.updated_successfully), withDismissAction = true) }
                    isSaving = false
                    editing = false
                    return@coroutineScope true
                }

                is ErrorResponse -> {
                    password.setError(context.getString(R.string.incorrect_password))
                    launch { passwordScrollRequester.bringIntoView() }
                    null
                }

                is InvalidParamResponse -> context.getString(R.string.invalid_field, it.param)
                else -> context.getString(R.string.unknown_error)
            }
        }
        isSaving = false
        return@coroutineScope false
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val model = viewModel<ProfileScreenModel>()
    val repository = LocalRepository.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        launch { repository.profile.collect { model.data = it } }
        launch { repository.countryData.collect { model.countryData = it } }
    }
    val data = model.data // extract to variable to prevent "variable has custom getter" null-check errors

    // Code to load picked image and submit to server
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    var isLoading by rememberSaveable { mutableStateOf(false) }
    // Need to provide a real context to 'rememberLauncherForActivityResult', not the crap that Context.createConfigurationContext is
    // CompositionalLocalProvider can't be used due to no return value
    val imagePicker = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        if (!result.isSuccessful || result.uriContent == null) return@rememberLauncherForActivityResult
        isLoading = true
        // We need lifecycleScope because the "Welcome back" (password screen) might be displaying on top due to long time spent by user in cropping activity
        lifecycleScope.launch {
            // Load image as bitmap
            val bitmap = withContext(Dispatchers.IO) { ImageLoader(context).execute(ImageRequest.Builder(context).data(result.uriContent).build()) }
                .drawable?.toBitmapOrNull(SUserValidation.avatarSize, SUserValidation.avatarSize)
            if (bitmap == null) {
                isLoading = false
                snackbar.showSnackbar(context.getString(R.string.error_loading_image), withDismissAction = true)
                return@launch
            }
            // Compress bitmap into memory
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
            // Send image to server
            repository.sendAboutOrPicture(SUserProfileUpdate(null, bytes.toByteArray())).handle(this, snackbar, context) {
                if (it != SuccessResponse) context.getString(R.string.profile_picture_problem) else null
            }
            isLoading = false
        }
    }

    // For loading animations
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    val textMod = if (data == null) Modifier.shimmer(shimmer) else Modifier

    // Logout confirm dialog
    val logoutDialogState = remember { UseCaseState() }
    InfoDialog(state = logoutDialogState, selection = InfoSelection(
        negativeButton = SelectionButton(android.R.string.cancel),
        positiveButton = SelectionButton(R.string.exit),
        onPositiveClick = {
            repository.logout()
            onLogout()
        }
    ), body = InfoBody.Default(
        bodyText = stringResource(R.string.confirm_logout),
        preBody = { Text(text = stringResource(R.string.logout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 18.dp)) }
    ))

    // Main view
    val scrollState = rememberScrollState()
    val (showFAB, setShowFAB) = remember { mutableStateOf(true) }
    HideFABOnScroll(state = scrollState, setShowFAB)
    NavScreen(
        onDismiss,
        title = R.string.profile,
        buttonIcon =
        if (model.editing) null else {
            { Icon(painter = painterResource(R.drawable.baseline_logout_24), contentDescription = stringResource(R.string.logout)) }
        },
        onIconButtonClick = { logoutDialogState.show() },
        isLoading = isLoading || model.isSaving,
        snackbar = snackbar,
        isFABVisible = data != null && model.countryData != null && (model.editing || showFAB),
        fabContent = {
            FloatingActionButton(onClick = {
                if (model.editing) model.onSave(context, repository, snackbar)
                else model.onEdit()
            }, shape = CircleShape) {
                if (model.editing) Icon(imageVector = Icons.Default.Check, contentDescription = stringResource(R.string.save))
                else Icon(imageVector = Icons.Default.Create, contentDescription = stringResource(R.string.edit))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = !model.editing) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .2f), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        ) {
                            Box {
                                Avatar(image = data?.avatar, shimmer = shimmer.takeIf { data == null }, modifier = Modifier.padding(8.dp))
                                if (data != null) {
                                    val imagePickerActivityTitle = stringResource(R.string.image_crop)
                                    FilledIconButton(
                                        onClick = {
                                            imagePicker.launch(
                                                CropImageContractOptions(
                                                    null, CropImageOptions(
                                                        activityTitle = imagePickerActivityTitle,
                                                        aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true,
                                                        minCropResultWidth = 300, minCropResultHeight = 300, cropShape = CropImageView.CropShape.OVAL,
                                                        outputRequestWidth = SUserValidation.avatarSize, outputRequestHeight = SUserValidation.avatarSize
                                                    )
                                                )
                                            )
                                        },
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    ) {
                                        Icon(Icons.Default.Create, stringResource(R.string.account))
                                    }
                                }
                            }
                            Column {
                                if (data == null) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp, 18.dp)
                                            .grayShimmer(shimmer)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp, 16.dp)
                                            .grayShimmer(shimmer)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp, 14.dp)
                                            .grayShimmer(shimmer)
                                    )
                                } else {
                                    Text(text = "@${data.tag}", style = MaterialTheme.typography.titleLarge)
                                    Text(text = data.fullName, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.joined_on, data.joinDate.format()),
                                        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.about), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, modifier = textMod
                            )
                            if (data == null) Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .grayShimmer(shimmer)
                            )
                            else {
                                // null means submitting (loading)
                                var editingAbout by rememberSaveable { mutableStateOf<Boolean?>(false) }
                                var aboutValue by rememberSaveable { mutableStateOf("") }
                                val aboutError = editingAbout == true && aboutValue.trim().length > SUserValidation.aboutMaxLength
                                val focusManager = LocalFocusManager.current
                                val focusRequester = remember { FocusRequester() }
                                val onDone: () -> Unit = {
                                    focusManager.clearFocus()
                                    editingAbout = null
                                    scope.launch {
                                        repository.sendAboutOrPicture(SUserProfileUpdate(aboutValue.trim(), null)).handle(this, snackbar, context) {
                                            when (it) {
                                                SuccessResponse -> {
                                                    editingAbout = false
                                                    null
                                                }

                                                is InvalidParamResponse -> context.getString(R.string.invalid_about)
                                                else -> context.getString(R.string.unknown_error)
                                            }
                                        }
                                        if (editingAbout == null) editingAbout = true
                                    }
                                }
                                TextField(
                                    value = if (editingAbout != false) aboutValue else data.about,
                                    onValueChange = { aboutValue = it },
                                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Light),
                                    placeholder = { Text(text = stringResource(R.string.nothing_here), color = MaterialTheme.colorScheme.outline) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    readOnly = editingAbout != true,
                                    singleLine = true,
                                    isError = aboutError,
                                    supportingText = if (aboutError) {
                                        { Text(text = stringResource(R.string.too_long)) }
                                    } else null,
                                    keyboardActions = KeyboardActions(onDone = { onDone() }),
                                    leadingIcon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
                                    trailingIcon = {
                                        AnimatedContent(targetState = editingAbout, label = "About Edit Icon") {
                                            when (it) {
                                                false -> IconButton(onClick = {
                                                    focusManager.clearFocus()
                                                    aboutValue = data.about
                                                    editingAbout = true
                                                    scope.launch {
                                                        delay(50)
                                                        // Wait until the TextField is no longer readonly, otherwise keyboard won't display
                                                        focusRequester.requestFocus()
                                                    }
                                                }) {
                                                    Icon(imageVector = Icons.Default.Create, contentDescription = stringResource(R.string.edit))
                                                }

                                                true -> IconButton(onClick = onDone) {
                                                    Icon(imageVector = Icons.Default.Done, contentDescription = stringResource(R.string.done))
                                                }

                                                null -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (model.editing) VerifiableField(model.email, R.string.email, type = KeyboardType.Email, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) })
            else Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.email), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod
                    )
                    if (data == null) Box(
                        modifier = Modifier
                            .size(200.dp, 16.dp)
                            .grayShimmer(shimmer)
                    )
                    else Text(text = data.email)
                }
            }
            AnimatedVisibility(visible = !model.editing) {
                Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.phone), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod
                        )
                        if (data == null) Box(
                            modifier = Modifier
                                .size(120.dp, 16.dp)
                                .grayShimmer(shimmer)
                        )
                        else Text(text = data.phone)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (model.editing) VerifiableField(
                    model.firstName, R.string.first_name, type = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words, modifier = Modifier.weight(1f)
                )
                else Surface(modifier = Modifier.weight(1f), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.first_name), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod
                        )
                        if (data == null) Box(
                            modifier = Modifier
                                .size(100.dp, 16.dp)
                                .grayShimmer(shimmer)
                        )
                        else Text(text = data.firstName)
                    }
                }
                if (model.editing) VerifiableField(
                    model.middleName, R.string.middle_name, type = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words, modifier = Modifier.weight(1f)
                )
                else Surface(modifier = Modifier.weight(1f), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.middle_name), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod
                        )
                        if (data == null) Box(
                            modifier = Modifier
                                .size(100.dp, 16.dp)
                                .grayShimmer(shimmer)
                        )
                        else Text(text = data.middleName ?: "")
                    }
                }
            }
            if (model.editing) VerifiableField(
                model.lastName, R.string.last_name, type = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words, modifier = Modifier.fillMaxWidth()
            )
            else Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.last_name), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod
                    )
                    if (data == null) Box(
                        modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer)
                    )
                    else Text(text = data.lastName)
                }
            }
            if (model.editing) ButtonField(value = model.dateOfBirth.value.format(true), onClick = {}, label = R.string.date_of_birth,
                modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = null) })
            else Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.date_of_birth), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod
                    )
                    if (data == null) Box(
                        modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer)
                    )
                    else Text(text = data.dateOfBirth.format(true))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (model.editing) ComboBox(
                    model.country.value.country, onSelectItem = {
                        if (it !== model.country.value) {
                            model.country.value = it
                            model.state = it.states[0]
                        }
                    }, modifier = Modifier.weight(1f), outlined = true, supportingText = "",
                    items = model.countryData!!
                ) { item, onClick -> DropdownMenuItem(text = { Text(text = item.country) }, onClick) }
                else Surface(modifier = Modifier.weight(1f), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.country), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod
                        )
                        if (data == null) Box(
                            modifier = Modifier
                                .size(100.dp, 16.dp)
                                .grayShimmer(shimmer)
                        )
                        else Text(text = model.countryData.nameFromCode(data.countryCode))
                    }
                }
                if (model.editing) ComboBox(
                    model.state, onSelectItem = { model.state = it },
                    modifier = Modifier.weight(1f), outlined = true, supportingText = "",
                    items = model.country.value.states
                ) { item, onClick -> DropdownMenuItem(text = { Text(text = item) }, onClick) }
                else Surface(modifier = Modifier.weight(1f), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.state_province), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod
                        )
                        if (data == null) Box(
                            modifier = Modifier
                                .size(100.dp, 16.dp)
                                .grayShimmer(shimmer)
                        )
                        else Text(text = data.state)
                    }
                }
            }
            if (model.editing) VerifiableField(
                model.city, R.string.city, type = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words, modifier = Modifier.fillMaxWidth(1f)
            )
            else Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.city), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod
                    )
                    if (data == null) Box(
                        modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer)
                    )
                    else Text(text = data.city)
                }
            }
            if (model.editing) VerifiableField(
                model.address, R.string.address, type = KeyboardType.Text, autoCorrect = true, modifier = Modifier.fillMaxWidth(1f)
            )
            else Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.address), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod
                    )
                    if (data == null) Box(
                        modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer)
                    )
                    else Text(text = data.address)
                }
            }

            if (model.editing) {
                var showPassword by remember { mutableStateOf(false) }
                VerifiableField(
                    model.password, R.string.password, type = KeyboardType.Password, trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                painterResource(
                                    if (showPassword) R.drawable.baseline_visibility_24
                                    else R.drawable.baseline_visibility_off_24
                                ),
                                stringResource(R.string.show_password)
                            )
                        }
                    }, showPassword = showPassword, isLast = true, modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(model.passwordScrollRequester)
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    AppTheme {
        ProfileScreen(onDismiss = {}, onLogout = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreviewDark() {
    AppTheme {
        ProfileScreen(onDismiss = {}, onLogout = {})
    }
}