package ro.bankar.app.ui.main

import android.content.res.Configuration
import android.graphics.Bitmap
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import ro.bankar.model.SUserValidation

@Composable
fun ProfileScreen(onDismiss: () -> Unit) {
    val repository = LocalRepository.current
    LaunchedEffect(true) { repository.profile.requestEmit(true) }

    val data by repository.profile.collectAsState(initial = null)
    if (data == null) return // TODO shimmer effect profile

    val imagePicker = rememberLauncherForActivityResult(contract = CropImageContract()) {}

    PopupScreen(onDismiss, title = R.string.profile, fabContent = {
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
                        Icon(
                            Icons.Default.AccountCircle, stringResource(R.string.account), modifier = Modifier.size(100.dp)
                        )
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
                        Text(text = "@${data!!.tag}", style = MaterialTheme.typography.titleLarge)
                        Text(text = with(data!!) { "$firstName ${if (middleName != null) "$middleName " else ""}$lastName" },
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(15.dp))
                        Text(text = "Joined on ${data!!.joinDate}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
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
                    Text(text = "About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextField(
                        value = data!!.about,
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
                    Text(text = "Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = data!!.email)
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
                    Text(text = "Phone Number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = data!!.phone)
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
                        Text(text = "First Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = data!!.firstName)
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
                        Text(text = "Middle Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = data!!.middleName ?: "")
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
                    Text(text = "Last Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = data!!.lastName)
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
                    Text(text = "Date of Birth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = data!!.dateOfBirth.toString())
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
                        Text(text = "Country", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = data!!.countryCode)
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
                        Text(text = "City", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = data!!.city)
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
        ProfileScreen({})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreviewDark() {
    AppTheme {
        ProfileScreen({})
    }
}