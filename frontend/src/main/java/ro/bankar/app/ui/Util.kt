package ro.bankar.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import ro.bankar.app.R
import ro.bankar.app.TAG
import ro.bankar.app.ui.theme.customColors
import ro.bankar.banking.Currency
import ro.bankar.banking.SCountries
import ro.bankar.model.SBankAccountType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> {
        Log.w(TAG, "Context $this is not an activity")
        null
    }
}

/**
 * Returns a state for a preference key in a DataStore. A `null` value means the key is not set.
 * The state is mutable and can be used to change the value in the DataStore.
 */
@Composable
fun <T : Any> rememberPreferenceDataStoreSettingState(
    dataStore: DataStore<Preferences>,
    key: Preferences.Key<T>,
    defaultValue: T? = null,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MutableState<T?> {
    val initialValue = runBlocking { dataStore.data.first()[key] }
    val value by remember { dataStore.data.map { it[key] } }.collectAsState(initialValue)
    return object : MutableState<T?> {
        override var value: T?
            get() = value ?: defaultValue
            set(value) {
                coroutineScope.launch {
                    if (value != null) dataStore.edit { it[key] = value }
                    else dataStore.edit { it -= key }
                }
            }

        override fun component1() = value
        override fun component2() = { value: T? -> this.value = value }
    }
}

fun Modifier.grayShimmer(shimmer: Shimmer) = shimmer(shimmer).composed { background(MaterialTheme.customColors.shimmer) }

val SBankAccountType.rString
    get() = when (this) {
        SBankAccountType.Debit -> R.string.account_debit
        SBankAccountType.Savings -> R.string.account_savings
        SBankAccountType.Credit -> R.string.account_credit
    }

fun SCountries?.nameFromCode(code: String) = this?.find { it.code == code }?.country ?: code

val Double.amountColor @Composable get() = if (this < 0) MaterialTheme.customColors.red else MaterialTheme.customColors.green

@Composable
fun HideFABOnScroll(state: ScrollState, setFABShown: (Boolean) -> Unit) {
    var previousScrollAmount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(state.value) {
        if (abs(state.value - previousScrollAmount) < 20) return@LaunchedEffect
        else setFABShown(state.value <= previousScrollAmount)
        previousScrollAmount = state.value
    }
}

@Composable
fun rememberMockNavController(): NavHostController {
    val context = LocalContext.current
    return remember {
        object : NavHostController(context) {
            override fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) {
                // do nothing
            }

            override fun popBackStack(): Boolean {
                // do nothing
                return false
            }
        }
    }
}

// Show snackbar with dismiss action
suspend fun SnackbarHostState.show(message: String) = coroutineScope { showSnackbar(message, withDismissAction = true) }

@Composable
fun <T, R> Flow<T>.mapCollectAsState(initial: R, mapFunc: suspend (T) -> R) = produceState(initialValue = initial) {
    map(mapFunc).collect { value = it }
}

// Read-only constant states
private class ConstantState<T>(override val value: T) : State<T>
fun <T> stateOf(t: T): State<T> = ConstantState(t)

// Currency formatting
fun Currency.format(amount: Double, showPlusSign: Boolean = false, separator: String = " ") =
    "${if (showPlusSign) "%+.2f" else "%.2f"}$separator%s".format(amount, this.code)

/**
 * Saver for kotlinx.serialization.Serializable classes
 */
class SerializableSaver<T>(private val serializer: KSerializer<T>) : Saver<T, String> {
    override fun restore(value: String) = runCatching { Json.decodeFromString(serializer, value) }.getOrNull()
    override fun SaverScope.save(value: T) = Json.encodeToString(serializer, value)
}

/**
 * Obtains a saver for a type. The type must be a class annotated with [@Serializable][Serializable].
 */
inline fun <reified T> serializableSaver() = SerializableSaver(serializer<T>())

/**
 * onValueChanged function for number TextField
 */
fun processNumberValue(value: String) : String? {
    // Remove invalid characters
    val num = value.filter { it.isDigit() || it == '.' }
    // Reject invalid inputs
    if (!(num.isEmpty() || num.removeSuffix(".").toIntOrNull() != null || num.toDoubleOrNull() != null)
        || num.count { it == '.' } > 1
    ) return null
    // Remove leading zeros, only allow up to 10 digits before decimal and 2 digits after
    val decimal = num.indexOf('.')
    val before = if (decimal == -1) num else num.substring(0, decimal)
    val after = if (decimal == -1) "" else num.substring(decimal + 1, min(decimal + 3, num.length))
    if (before.length > 10 || after.length > 2) return null
    return before.substring(max(0, before.indexOfFirst { it != '0' })) + (if (decimal != -1) "." else "") + after
}