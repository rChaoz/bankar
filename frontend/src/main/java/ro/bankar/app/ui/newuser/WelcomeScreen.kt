package ro.bankar.app.ui.newuser

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.bankar.app.R
import ro.bankar.app.ui.theme.AppTheme

@Composable
fun WelcomeScreen(onSignIn: () -> Unit, onSignUp: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                modifier = Modifier.padding(top = 15.dp),
                painter = painterResource(R.drawable.logo_adaptive),
                contentDescription = stringResource(R.string.logo),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.welcome),
                modifier = Modifier.padding(horizontal = 30.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge,
            )
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                ElevatedButton(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sign_in).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                ElevatedButton(
                    onClick = onSignUp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ) {
                    Text(
                        text = stringResource(R.string.sign_up).uppercase(),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {}) {
                        Text(text = stringResource(R.string.help), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = {}) {
                        Text(text = stringResource(R.string.contact_us), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    AppTheme {
        WelcomeScreen({}, {})
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomeScreenPreviewDark() {
    AppTheme {
        WelcomeScreen({}, {})
    }
}