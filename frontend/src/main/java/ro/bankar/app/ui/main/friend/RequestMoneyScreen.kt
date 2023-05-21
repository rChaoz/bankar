package ro.bankar.app.ui.main.friend

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Clock
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SDirection
import ro.bankar.model.SPublicUser
import ro.bankar.util.todayHere

@Composable
fun RequestMoneyScreen(onDismiss: () -> Unit, user: SPublicUser) {
    SendRequestMoneyScreenBase(onDismiss, user, requesting = true)
}

@Preview
@Composable
private fun RequestMoneyScreenPreview() {
    AppTheme {
        RequestMoneyScreen(
            onDismiss = {}, user = SPublicUser(
                "koleci", "Alexandru", "Paul", "Koleci",
                "RO", Clock.System.todayHere(), "", SDirection.Sent, null
            )
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RequestMoneyScreenPreviewDark() {
    AppTheme {
        RequestMoneyScreen(
            onDismiss = {}, user = SPublicUser(
                "koleci", "Alexandru", "Paul", "Koleci",
                "RO", Clock.System.todayHere(), "", SDirection.Sent, null
            )
        )
    }
}