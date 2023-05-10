package ro.bankar.app.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import ro.bankar.app.R
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SBankAccountType
import ro.bankar.util.todayHere
import java.time.format.DateTimeFormatter
import kotlin.math.abs

inline fun <reified T> StringFormat.safeDecodeFromString(string: String) = try {
    decodeFromString<T>(string)
} catch (_: Exception) {
    null
}

fun Modifier.grayShimmer(shimmer: Shimmer) = shimmer(shimmer).composed { background(MaterialTheme.customColors.shimmer) }

val SBankAccountType.rString get() = when(this) {
    SBankAccountType.Debit -> R.string.account_debit
    SBankAccountType.Savings -> R.string.account_savings
    SBankAccountType.Credit -> R.string.account_credit
}

@Composable
fun HideFABOnScroll(state: ScrollState, setFABShown: (Boolean) -> Unit) {
    var previousScrollAmount by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(state.value) {
        if (abs(state.value - previousScrollAmount) < 20) return@LaunchedEffect
        else setFABShown(state.value <= previousScrollAmount)
        previousScrollAmount = state.value
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")!!
private val longDateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")!!
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")!!

fun LocalDate.format(long: Boolean = false) = toJavaLocalDate().format(if (long) longDateFormatter else dateFormatter)!!
fun LocalTime.format() = toJavaLocalTime().format(timeFormatter)!!

fun LocalDateTime.format(long: Boolean = false) = if (date == Clock.System.todayHere()) time.format()
else "${date.format(long)} • ${time.format()}"