package ro.bankar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.bankar.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(useDarkTheme = false) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Surface {
                        LoginScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Surface(color = MaterialTheme.colorScheme.primary) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.Center),
            ) {
                Text(
                    "Sign In",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    shadowElevation = 5.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(25.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        TextField(
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.AccountCircle, "Account ID") },
                            value = username,
                            onValueChange = { username = it },
                            placeholder = {
                                Text(text = "Phone, e-mail or tag", color = Color.Gray)
                            }
                        )
                        TextField(
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, "Account password") },
                            visualTransformation = PasswordVisualTransformation(),
                            value = password,
                            onValueChange = { password = it },
                            placeholder = {
                                Text(text = "Password", color = Color.Gray)
                            }
                        )
                        Text(text = "Forgot password?", modifier = Modifier.clickable {})
                        Button(onClick = {}, modifier = Modifier.align(Alignment.End)) {
                            Text(text = "Sign In")
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Don't have an account yet?",
                )
                Text(
                    text = "Create one now",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.clickable {},
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AppTheme {
        LoginScreen()
    }
}