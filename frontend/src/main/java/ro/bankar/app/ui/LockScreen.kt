package ro.bankar.app.ui

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ro.bankar.app.KeyAuthenticationPin
import ro.bankar.app.KeyFingerprintEnabled
import ro.bankar.app.LocalActivity
import ro.bankar.app.LocalDataStore
import ro.bankar.app.R
import ro.bankar.app.collectPreferenceAsState
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.fold
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableStateOf
import ro.bankar.app.ui.components.verifiableSuspendingStateOf
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SuccessResponse

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    // Don't allow exiting this screen via back button
    val context = LocalContext.current
    val activity = LocalActivity.current
    BackHandler(enabled = activity != null) { activity!!.moveTaskToBack(true) }

    val repository = LocalRepository.current
    val scope = rememberCoroutineScope()
    val datastore = LocalDataStore.current

    // Password authentication
    val password = remember {
        verifiableSuspendingStateOf("", scope) { string ->
            repository.sendCheckPassword(string).fold(
                onFail = { getString(it) },
                onSuccess = { if (it is SuccessResponse) null else getString(R.string.incorrect_password) }
            )
        }
    }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    // PIN authentication
    val correctPIN by datastore.collectPreferenceAsState(KeyAuthenticationPin, defaultValue = null)
    var usingPin by remember { mutableStateOf(false) }
    LaunchedEffect(correctPIN) { if (correctPIN != null) usingPin = true }
    val pin = remember { verifiableStateOf("", R.string.incorrect_pin) { it == correctPIN } }

    // Fingerprint
    val fingerprintEnabled by datastore.collectPreferenceAsState(key = KeyFingerprintEnabled, defaultValue = false)
    val prompt = remember {
        activity?.let {
            BiometricPrompt(it, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlock()
                }
            })
        }
    }
    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login))
            .setNegativeButtonText(context.getString(android.R.string.cancel))
            .build()
    }
    if (fingerprintEnabled && prompt != null) LaunchedEffect(true) {
        datastore.data.first { if (it[KeyFingerprintEnabled] == true) prompt.authenticate(promptInfo); true }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(30.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.padding(top = 15.dp),
                painter = painterResource(R.drawable.logo_adaptive),
                contentDescription = stringResource(R.string.logo),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.welcome_back),
                modifier = Modifier.padding(horizontal = 30.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(if (usingPin) R.string.verification_pin else R.string.verification_password),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )

            val onDone: () -> Unit = {
                if (usingPin) {
                    pin.check(context)
                    if (pin.verified) onUnlock()
                } else scope.launch { password.checkSuspending(context); if (password.verified) onUnlock() }
            }

            if (usingPin) VerifiableField(
                pin,
                label = R.string.pin,
                type = KeyboardType.NumberPassword,
                showPassword = showPassword,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            painter = painterResource(if (showPassword) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24),
                            contentDescription = stringResource(R.string.show_pin)
                        )
                    }
                },
                onDone = { onDone() },
                isLast = true
            )
            else VerifiableField(
                password,
                label = R.string.password,
                type = KeyboardType.Password,
                showPassword = showPassword,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            painter = painterResource(if (showPassword) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24),
                            contentDescription = stringResource(R.string.show_password)
                        )
                    }
                },
                onDone = { scope.launch { password.checkSuspending(context); if (password.verified) onUnlock() } },
                isLast = true
            )
            if (fingerprintEnabled && prompt != null) FilledIconButton(onClick = { prompt.authenticate(promptInfo) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.baseline_fingerprint_24),
                    contentDescription = stringResource(R.string.use_fingerprint),
                    modifier = Modifier.size(32.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                if (correctPIN != null) {
                    TextButton(onClick = { usingPin = !usingPin }) {
                        Text(text = stringResource(if (usingPin) R.string.use_password else R.string.use_pin))
                    }
                }
                Button(
                    onClick = onDone,
                    enabled = (usingPin && pin.value.length in 4..8) || (!usingPin && password.value.isNotEmpty())
                ) {
                    Text(text = stringResource(R.string.button_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun LockScreenPreview() {
    AppTheme {
        LockScreen(onUnlock = {})
    }
}