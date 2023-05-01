package ro.bankar.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintSetScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import ro.bankar.app.R
import java.time.Month

fun ConstraintSetScope.createRefsFor(vararg ids: String) = ids.map { createRefFor(it) }

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

sealed class VerifiableState<T>(initialValue: T) {
    private val state = mutableStateOf(initialValue)
    private val errorState = mutableStateOf<String?>(null)
    private val verifiedState = mutableStateOf(false)

    var value: T
        get() = state.value
        set(value) {
            state.value = value
            verified = false
        }
    var error by errorState
        protected set
    var verified by verifiedState
        protected set
    val hasError get() = error != null

    @JvmName("setCustomError")
    fun setError(error: String) {
        this.error = error
        verified = false
    }

    fun clearError() {
        error = null
    }

    abstract fun check(context: Context)
}

class SimpleVerifiableState<T>(initialValue: T, private val verification: Context.(T) -> String?) : VerifiableState<T>(initialValue) {
    override fun check(context: Context) {
        if (verified) return
        error = context.verification(value)
        verified = error == null
    }
}

class SuspendVerifiableState<T>(initialValue: T, private val scope: CoroutineScope, private val verification: suspend Context.(T) -> String?) :
    VerifiableState<T>(initialValue) {

    private val verifyingState = mutableStateOf(false)
    private var job: Job? = null

    val verifying by verifyingState

    @Synchronized
    override fun check(context: Context) {
        if (verified) return
        job?.cancel()
        job = scope.launch {
            try {
                verifyingState.value = true
                checkSuspending(context)
            } finally {
                verifyingState.value = false
            }
        }
    }

    suspend fun checkSuspending(context: Context) {
        if (verified) return
        error = context.verification(value)
        verified = error == null
    }
}

fun <T> verifiableStateOf(initialValue: T, verification: Context.(T) -> String?) =
    SimpleVerifiableState(initialValue, verification)

fun <T> verifiableStateOf(initialValue: T, errorMessage: Int, verification: (T) -> Boolean) = SimpleVerifiableState(initialValue) {
    if (verification(it)) null else getString(errorMessage)
}

fun <T> verifiableSuspendingStateOf(initialValue: T, scope: CoroutineScope, verification: suspend Context.(T) -> String?) =
    SuspendVerifiableState(initialValue, scope, verification)

inline fun <reified T> StringFormat.safeDecodeFromString(string: String) = try {
    decodeFromString<T>(string)
} catch (_: Exception) {
    null
}