package ro.bankar.app.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import ro.bankar.app.R
import ro.bankar.app.data.Repository
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SBankAccountType
import java.time.Month
import kotlin.math.abs

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

val SBankAccountType.rString get() = when(this) {
    SBankAccountType.Debit -> R.string.account_debit
    SBankAccountType.Savings -> R.string.account_savings
    SBankAccountType.Credit -> R.string.account_credit
}

@SuppressLint("ComposableNaming")
@Composable
fun SharedFlow<Repository.Error>.handleWithSnackBar(snackBar: SnackbarHostState) {
    val context = LocalContext.current
    LaunchedEffect(true) {
        collect {
            if (it.message == 0 || !it.mustRetry) return@collect
            val result = snackBar.showSnackbar(
                message = context.getString(it.message),
                actionLabel = context.getString(R.string.retry)
            )
            if (result == SnackbarResult.ActionPerformed) it.retry(true)
        }
    }
}

@Composable
fun HideFABOnScroll(state: ScrollState, setFABShown: (Boolean) -> Unit) {
    var previousScrollAmount by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(state.value) {
        if (abs(state.value - previousScrollAmount) < 10) return@LaunchedEffect
        else setFABShown(state.value <= previousScrollAmount)
        previousScrollAmount = state.value
    }
}