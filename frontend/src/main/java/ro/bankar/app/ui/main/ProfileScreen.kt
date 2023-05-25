package ro.bankar.app.ui.main

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.data.collectAsStateRetrying
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.components.Avatar
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.SUserValidation
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onDismiss: () -> Unit) {
    val repository = LocalRepository.current
    val snackBar = remember { SnackbarHostState() }

    val dataState = repository.profile.collectAsStateRetrying()
    val data = dataState.value // extract to variable to prevent "variable has custom getter" null-check errors

    // Code to load picked image and submit to server
    val context = LocalContext.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope
    var isLoading by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(contract = CropImageContract()) {
        if (!it.isSuccessful || it.uriContent == null) return@rememberLauncherForActivityResult
        isLoading = true
        // We need lifecycleScope because the "Welcome back" (password screen) might be displaying on top due to long time spent by user in cropping activity
        lifecycleScope.launch {
            // Load image as bitmap
            val bitmap = withContext(Dispatchers.IO) { ImageLoader(context).execute(ImageRequest.Builder(context).data(it.uriContent).build()) }
                .drawable?.toBitmapOrNull(SUserValidation.avatarSize, SUserValidation.avatarSize)
            if (bitmap == null) {
                isLoading = false
                snackBar.showSnackbar(context.getString(R.string.error_loading_image), withDismissAction = true)
                return@launch
            }
            // Compress bitmap into memory
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
            // Send image to server
            when (val result = repository.sendAboutOrPicture(SUserProfileUpdate(null, bytes.toByteArray()))) {
                is SafeStatusResponse.InternalError -> launch { snackBar.showSnackbar(context.getString(result.message), withDismissAction = true) }
                is SafeStatusResponse.Fail -> launch { snackBar.showSnackbar(context.getString(R.string.profile_picture_problem), withDismissAction = true) }
                is SafeStatusResponse.Success ->
                    // Wait for image update to be received
                    repository.profile.emitNow()
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
        positiveButton = SelectionButton(R.string.yes),
        onPositiveClick = { repository.logout() }
    ), body = InfoBody.Default(
        bodyText = stringResource(R.string.confirm_logout),
        preBody = { Text(text = stringResource(R.string.logout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 18.dp)) }
    ))

    val scrollState = rememberScrollState()
    var showFAB by remember { mutableStateOf(true) }
    HideFABOnScroll(state = scrollState, setFABShown = { showFAB = it })
    NavScreen(
        onDismiss,
        title = R.string.profile,
        buttonIcon = { Icon(painter = painterResource(R.drawable.baseline_logout_24), contentDescription = stringResource(R.string.logout)) },
        onIconButtonClick = { logoutDialogState.show() },
        isLoading = isLoading,
        snackBar = snackBar,
        isFABVisible = data != null && showFAB,
        fabContent = {
            FloatingActionButton(onClick = { /*TODO*/ }, shape = CircleShape) {
                Icon(imageVector = Icons.Default.Create, contentDescription = stringResource(R.string.edit))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .2f), shape = RoundedCornerShape(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                ) {
                    Box {
                        Avatar(image = data?.avatar, nullIsLoading = data == null, modifier = Modifier.padding(8.dp))
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
                            Text(
                                text = data.fullName,
                                style = MaterialTheme.typography.titleMedium
                            )
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
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                        val scope = rememberCoroutineScope()
                        val onDone: () -> Unit = {
                            focusManager.clearFocus()
                            editingAbout = null
                            scope.launch {
                                when (val result = repository.sendAboutOrPicture(SUserProfileUpdate(aboutValue.trim(), null))) {
                                    is SafeStatusResponse.InternalError ->
                                        launch { snackBar.showSnackbar(context.getString(result.message), withDismissAction = true) }
                                    is SafeStatusResponse.Fail ->
                                        launch { snackBar.showSnackbar(context.getString(R.string.invalid_about), withDismissAction = true) }
                                    is SafeStatusResponse.Success -> {
                                        repository.profile.emitNow()
                                        editingAbout = false
                                    }
                                }
                                if (editingAbout == null) editingAbout = true
                            }
                        }
                        TextField(
                            value = if (editingAbout != false) aboutValue else data.about,
                            onValueChange = { aboutValue = it },
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                ) {
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .weight(1f),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
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
                Surface(
                    modifier = Modifier
                        .weight(1f),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                ) {
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.date_of_birth), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod
                    )
                    if (data == null) Box(
                        modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer)
                    )
                    else Text(text = data.dateOfBirth.toString())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .weight(1f),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.country), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod
                        )
                        if (data == null) Box(
                            modifier = Modifier
                                .size(100.dp, 16.dp)
                                .grayShimmer(shimmer)
                        )
                        else Text(text = data.countryCode)
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f),
                    tonalElevation = 1.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    AppTheme {
        ProfileScreen(onDismiss = {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreviewDark() {
    AppTheme {
        ProfileScreen(onDismiss = {})
    }
}