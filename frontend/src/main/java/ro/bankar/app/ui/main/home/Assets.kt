package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ro.bankar.app.R
import ro.bankar.app.ui.theme.AppTheme

@Composable
fun Assets() {
    HomeCard(title = stringResource(R.string.assets), icon = {
        Amount(amount = 1225.35f, currency = "RON", textStyle = MaterialTheme.typography.headlineSmall)
    }) {
        Row(horizontalArrangement = Arrangement.SpaceAround) {
            
        }
    }
}

@Preview
@Composable
private fun AssetsPreview() {
    AppTheme {
        Assets()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AssetsPreviewDark() {
    AppTheme {
        Assets()
    }
}

@Composable
private fun AssetsColumn(icon: @Composable () -> Unit, title: Int, amount: Float) {
    
}