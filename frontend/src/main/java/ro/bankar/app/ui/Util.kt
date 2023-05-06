package ro.bankar.app.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.stringResource
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import ro.bankar.app.R
import ro.bankar.app.ui.theme.customColors
import java.time.Month

@Composable
fun monthStringResource(month: kotlinx.datetime.Month) = stringResource(
    id = when (month) {
        Month.JANUARY -> R.string.january
        Month.FEBRUARY -> R.string.february
        Month.MARCH -> R.string.march
        Month.APRIL -> R.string.april
        Month.MAY -> R.string.may
        Month.JUNE -> R.string.june
        Month.JULY -> R.string.july
        Month.AUGUST -> R.string.august
        Month.SEPTEMBER -> R.string.september
        Month.OCTOBER -> R.string.october
        Month.NOVEMBER -> R.string.november
        Month.DECEMBER -> R.string.december
    }
)

inline fun <reified T> StringFormat.safeDecodeFromString(string: String) = try {
    decodeFromString<T>(string)
} catch (_: Exception) {
    null
}

fun Modifier.grayShimmer(shimmer: Shimmer) = shimmer(shimmer).composed { background(MaterialTheme.customColors.shimmer) }