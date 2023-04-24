package ro.bankar.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.bankar.app.ui.theme.AppTheme

@Composable
fun WelcomeScreen() {
    Surface(color = MaterialTheme.colorScheme.primary) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                "Welcome",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal=15.dp, vertical=30.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                ElevatedButton(onClick = {}, modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)) {
                    Text(text = "SIGN UP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                ElevatedButton(onClick = {}, modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)) {
                    Text(text = "LOG IN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Help", modifier = Modifier.clickable {  })
                    Text(text = "Contact Us", modifier = Modifier.clickable {  })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    AppTheme {
        WelcomeScreen()
    }
}