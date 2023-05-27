package ro.bankar.app.ui.components

import android.content.Context
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun VerifiableField(
    verifiableState: VerifiableState<String>,
    label: Int,
    type: KeyboardType,
    modifier: Modifier = Modifier,
    valueTransform: (String) -> String? = { it },
    id: String? = null,
    enabled: Boolean = true,
    showPassword: Boolean = true,
    autoCorrect: Boolean = false,
    isLast: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onDone: (KeyboardActionScope.() -> Unit)? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    multiLine: Boolean = false,
) {
    val context = LocalContext.current
    var mod = modifier.onFocusChanged {
        if (it.isFocused) verifiableState.clearError()
        else if (verifiableState.value.isNotEmpty()) verifiableState.check(context)
    }
    if (id != null) mod = mod.layoutId(id)
    OutlinedTextField(
        value = verifiableState.value,
        onValueChange = { valueTransform(it)?.let{ verifiableState.value = it } },
        modifier = mod,
        enabled = enabled,
        singleLine = !multiLine,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = { Text(text = stringResource(label)) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
            keyboardType = type,
            autoCorrect = autoCorrect,
            capitalization = capitalization
        ),
        keyboardActions = KeyboardActions(onDone = onDone),
        isError = verifiableState.hasError,
        supportingText = { Text(text = verifiableState.error ?: "") }
    )
}

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

    abstract fun check(context: Context, force: Boolean = true)
}

class SimpleVerifiableState<T>(initialValue: T, private val verification: Context.(T) -> String?) : VerifiableState<T>(initialValue) {
    override fun check(context: Context, force: Boolean) {
        if (verified && !force) return
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
    override fun check(context: Context, force: Boolean) {
        if (verified && !force) return
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