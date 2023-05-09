package ro.bankar.app.ui.main

import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.bankar.app.R
import ro.bankar.app.TAG
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.ui.components.PopupScreen
import ro.bankar.app.ui.format
import ro.bankar.app.ui.grayShimmer
import ro.bankar.app.ui.handleWithSnackBar
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.SUserValidation
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProfileScreen(onDismiss: () -> Unit) {
    val repository = LocalRepository.current
    val snackBar = remember { SnackbarHostState() }
    repository.errorFlow.handleWithSnackBar(snackBar)
    LaunchedEffect(true) { repository.profile.requestEmit(true) }

    val dataState = repository.profile.collectAsState(initial = null)
    val data = dataState.value // extract to variable to prevent "variable has custom getter" null-check errors
    data?.avatar.let { Log.d(TAG, "avatar is null? ${it == null}") }

    LaunchedEffect(dataState.value) {
        Log.d(TAG, "dataState is now ${if (dataState.value == null) "null" else "not null"}")
    }

    // Code to load picked image and submit to server
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(contract = CropImageContract()) {
        if (!it.isSuccessful || it.uriContent == null) return@rememberLauncherForActivityResult
        isLoading = true
        scope.launch {
            // Load image as bitmap
            val bitmap = withContext(Dispatchers.IO) { ImageLoader(context).execute(ImageRequest.Builder(context).data(it.uriContent).build()) }
                .drawable?.toBitmapOrNull(SUserValidation.avatarSize, SUserValidation.avatarSize)
            if (bitmap == null) {
                isLoading = false
                snackBar.showSnackbar(context.getString(R.string.error_loading_image), withDismissAction = true)
                return@launch
            }
            // Compress bitmap into memory as WEBP
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
            // Send image to server
            when (val result = repository.sendAboutOrPicture(SUserProfileUpdate(null, bytes.toByteArray()))) {
                is SafeStatusResponse.InternalError -> launch { snackBar.showSnackbar(context.getString(result.message), withDismissAction = true) }
                is SafeStatusResponse.Fail -> launch { snackBar.showSnackbar(context.getString(R.string.profile_picture_problem), withDismissAction = true) }
                is SafeStatusResponse.Success -> {
                    repository.profile.requestEmit(true)
                    // Wait an additional second for the image to update
                    delay(1.seconds)
                }
            }
            isLoading = false
        }
    }

    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
    val textMod = if (data == null) Modifier.shimmer(shimmer) else Modifier
    PopupScreen(onDismiss, title = R.string.profile, isLoading = isLoading, snackBar = snackBar, isFABVisible = data != null, fabContent = {
        FloatingActionButton(onClick = { /*TODO*/ }, shape = CircleShape) {
            Icon(imageVector = Icons.Default.Create, contentDescription = stringResource(R.string.edit))
        }
    }) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
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
                        if (data == null) Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.avatar),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(100.dp)
                                .shimmer()
                        ) else if (data.avatar == null) Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.avatar),
                            modifier = Modifier.size(100.dp)
                        ) else AsyncImage(
                            model = data.avatar,
                            contentDescription = stringResource(R.string.avatar),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(100.dp)
                                .clip(CircleShape)
                        )

                        if (data != null) {
                            val imagePickerActivityTitle = stringResource(R.string.image_crop)
                            FilledIconButton(
                                onClick = {
                                    imagePicker.launch(
                                        CropImageContractOptions(
                                            null, CropImageOptions(
                                                activityTitle = imagePickerActivityTitle,
                                                aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true,
                                                minCropResultWidth = 150, minCropResultHeight = 150, cropShape = CropImageView.CropShape.OVAL,
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
                                    .size(100.dp, 22.dp)
                                    .grayShimmer(shimmer)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
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
                                text = with(data) { "$firstName ${if (middleName != null) "$middleName " else ""}$lastName" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(R.string.joined_on, data.joinDate.format()),
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                shadowElevation = 6.dp,
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = stringResource(R.string.about), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod)
                    if (data == null) Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .grayShimmer(shimmer)
                    )
                    else TextField(
                        value = data.about,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true
                    )
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
                    Text(text = stringResource(R.string.email), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod)
                    if (data == null) Box(modifier = Modifier
                        .size(200.dp, 16.dp)
                        .grayShimmer(shimmer))
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
                    Text(text = stringResource(R.string.phone), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod)
                    if (data == null) Box(modifier = Modifier
                        .size(120.dp, 16.dp)
                        .grayShimmer(shimmer))
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
                        Text(text = stringResource(R.string.first_name), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod)
                        if (data == null) Box(modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer))
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
                        Text(text = stringResource(R.string.middle_name), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod)
                        if (data == null) Box(modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer))
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
                    Text(text = stringResource(R.string.last_name), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod)
                    if (data == null) Box(modifier = Modifier
                        .size(100.dp, 16.dp)
                        .grayShimmer(shimmer))
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
                    Text(text = stringResource(R.string.date_of_birth), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = textMod)
                    if (data == null) Box(modifier = Modifier
                        .size(100.dp, 16.dp)
                        .grayShimmer(shimmer))
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
                        Text(text = stringResource(R.string.country), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod)
                        if (data == null) Box(modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer))
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
                        Text(text = stringResource(R.string.city), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = textMod)
                        if (data == null) Box(modifier = Modifier
                            .size(100.dp, 16.dp)
                            .grayShimmer(shimmer))
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