package ro.bankar.app.ui.main

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.components.PopupScreen
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SUserValidation

@Composable
fun Profile(onDismiss: () -> Unit) {
    val repository = LocalRepository.current
    LaunchedEffect(true) {repository.profile.requestEmit(true) }

    val profileData by repository.profile.collectAsState(initial = null)

    if(profileData == null) return

    val imagePicker = rememberLauncherForActivityResult(contract = CropImageContract()) {}

    PopupScreen(onDismiss, title = R.string.profile, buttons = { /*TODO*/ }) {
        //TODO Tot
        Row (horizontalArrangement = Arrangement.spacedBy(30.dp), modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)) {
            Box {
                Icon(Icons.Default.AccountCircle, stringResource(R.string.account), modifier = Modifier
                    .size(120.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                FilledIconButton(
                    onClick = {
                        @Suppress("DEPRECATION") // for Bitmap.CompressFormat.WEBP which is needed due to API level
                        imagePicker.launch(
                            CropImageContractOptions(
                                null, CropImageOptions(
                                    aspectRatioX = 1, aspectRatioY = 1, fixAspectRatio = true,
                                    minCropResultWidth = 150, minCropResultHeight = 150, cropShape = CropImageView.CropShape.OVAL,
                                    outputRequestWidth = SUserValidation.avatarSize, outputRequestHeight = SUserValidation.avatarSize,
                                    outputCompressFormat = Bitmap.CompressFormat.WEBP
                                )
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)

                ) {
                    Icon(Icons.Default.Create, stringResource(R.string.account))
                }
            }
            Column {
                Text(text = "@${profileData!!.tag}" , style = MaterialTheme.typography.titleLarge)
                Text(text = "Full Name", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(15.dp))
                Text(text = "Joined on ${profileData!!.joinDate}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth(),
            shadowElevation = 6.dp,
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(8.dp),
            ){
            Column (modifier = Modifier
                .padding(12.dp)){
                Text(text = "About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextField(
                    value = profileData!!.about,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth(),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(8.dp)){
            Column (modifier = Modifier
                .padding(12.dp)){
                Text(text = "Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = profileData!!.email)
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth(),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(8.dp)){
            Column (modifier = Modifier
                .padding(12.dp)){
                Text(text = "Phone Number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = profileData!!.phone)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()){
            Surface(modifier = Modifier
                .weight(1f),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)){
                Column (modifier = Modifier
                    .padding(12.dp)){
                    Text(text = "First Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = profileData!!.firstName)
                }
            }
            Surface(modifier = Modifier
                .weight(1f),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)){
                Column (modifier = Modifier
                    .padding(12.dp)){
                    Text(text = "Middle Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = profileData!!.middleName?:"")
                }
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth(),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(8.dp)){
            Column (modifier = Modifier
                .padding(12.dp)){
                Text(text = "Last Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = profileData!!.lastName)
            }
        }
        Surface(modifier = Modifier
            .fillMaxWidth(),
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(8.dp)){
            Column (modifier = Modifier
                .padding(12.dp)){
                Text(text = "Date of Birth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = profileData!!.dateOfBirth.toString())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()){
            Surface(modifier = Modifier
                .weight(1f),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)){
                Column (modifier = Modifier
                    .padding(12.dp)){
                    Text(text = "Country", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = profileData!!.countryCode)
                }
            }
            Surface(modifier = Modifier
                .weight(1f),
                tonalElevation = 1.dp,
                shape = RoundedCornerShape(8.dp)){
                Column (modifier = Modifier
                    .padding(12.dp)){
                    Text(text = "City", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = profileData!!.city)
                }
            }
        }
        FilledIconButton(
            onClick = { },
            modifier = Modifier


        ) {
            Icon(Icons.Default.Create, stringResource(R.string.account), modifier = Modifier
                .size(65.dp))
        }

    }

}

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    AppTheme {
        Profile({})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreviewDark() {
    AppTheme {
        Profile({})
    }
}