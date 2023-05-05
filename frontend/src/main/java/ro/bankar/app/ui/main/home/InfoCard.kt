package ro.bankar.app.ui.main.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun InfoCard(text: Int, tonalElevation: Dp = 1.dp) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = tonalElevation,
    ) {
        InfoCardContent(text = text)
    }
}

@Composable
fun InfoCard(text: Int, tonalElevation: Dp = 1.dp, onClick: () -> Unit) {
    Surface(
        onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = tonalElevation,
    ) {
        InfoCardContent(text = text)
    }
}

@Composable
private fun InfoCardContent(text: Int) {
    Box(modifier = Modifier.padding(vertical = 16.dp, horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}