package ro.bankar.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.SafeStatusResponse
import ro.bankar.app.ui.components.VerifiableField
import ro.bankar.app.ui.components.verifiableSuspendingStateOf

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    // Don't allow exiting this screen via back button
    val activity = LocalContext.current.getActivity()
    BackHandler { activity.moveTaskToBack(true) }

    val repository = LocalRepository.current

    val scope = rememberCoroutineScope()
    val password = remember {
        verifiableSuspendingStateOf("", scope) {
            when (val result = repository.sendCheckPassword(it)) {
                is SafeStatusResponse.Success -> null
                is SafeStatusResponse.InternalError -> activity.getString(result.message)
                is SafeStatusResponse.Fail -> activity.getString(R.string.incorrect_password)
            }
        }
    }
    var showPassword by rememberSaveable { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.verification_password),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )

            VerifiableField(
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
                onDone = { scope.launch { password.checkSuspending(activity); if (password.verified) onUnlock() } },
                isLast = true
            )
            Button(
                onClick = { scope.launch { password.checkSuspending(activity); if (password.verified) onUnlock() } },
                enabled = password.value.isNotEmpty()
            ) {
                Text(text = stringResource(R.string.button_continue))
            }
        }
    }
}