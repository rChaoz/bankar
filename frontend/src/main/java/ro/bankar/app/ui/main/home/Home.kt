package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.LocalCustomColors
import ro.bankar.app.ui.theme.customColors
import kotlin.math.abs

@Composable
fun Home(setShowFab: (Boolean) -> Unit) {
    val repo = LocalRepository.current
    LaunchedEffect(true) {
        repo.accounts.requestEmit()
    }
    val accounts by repo.accounts.collectAsState(null)
    // Hide FAB when scrolling down
    val scrollState = rememberScrollState()
    var previousScrollAmount by remember { mutableStateOf(0) }
    LaunchedEffect(scrollState.value) {
        if (abs(scrollState.value - previousScrollAmount) < 10) return@LaunchedEffect
        else if (scrollState.value > previousScrollAmount) setShowFab(false)
        else setShowFab(true)
        previousScrollAmount = scrollState.value
    }

    // TODO Pull to refresh?
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (accounts == null) {
            val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
            RecentActivityShimmer(shimmer)
            BankAccountShimmer(shimmer)
            AssetsShimmer(shimmer)
        } else {
            RecentActivity()
            for (account in accounts!!) BankAccount(data = account)
            if (accounts!!.isEmpty()) InfoCard(text = R.string.no_bank_accounts, onClick = {})
            Assets()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AppTheme {
        Home(setShowFab = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomePreviewDark() {
    AppTheme {
        Home(setShowFab = {})
    }
}

private fun Modifier.topBorder(thickness: Dp, color: Color) = composed {
    with(LocalDensity.current) {
        val t = thickness.toPx()
        border(thickness, color, GenericShape { (width, _), _ ->
            moveTo(0f, 0f)
            lineTo(width, 0f)
            lineTo(width, t)
            lineTo(0f, t)
            close()
        })
    }
}

@Composable
private fun HomeCardContent(
    title: String,
    icon: @Composable () -> Unit,
    shimmer: Shimmer? = null,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.topBorder(5.dp, color ?: MaterialTheme.colorScheme.inversePrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title, style = MaterialTheme.typography.titleLarge,
                modifier = if (shimmer == null) Modifier else Modifier.shimmer(shimmer)
            )
            Box(modifier = Modifier.padding(4.dp)) {
                icon()
            }
        }
        content()
    }
}

@Composable
fun HomeCard(
    title: String,
    icon: @Composable () -> Unit,
    shimmer: Shimmer? = null,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = .5.dp,
        shadowElevation = 3.dp
    ) {
        HomeCardContent(title, icon, shimmer, color, content)
    }
}

@Composable
fun HomeCard(
    onClick: () -> Unit,
    title: String,
    icon: @Composable () -> Unit,
    shimmer: Shimmer? = null,
    color: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = .5.dp,
        shadowElevation = 3.dp
    ) {
        HomeCardContent(title, icon, shimmer, color, content)
    }
}

@Composable
fun Amount(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    withPlusSign: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    shimmer: Shimmer? = null,
) {
    Text(
        text = "%${if (withPlusSign) "+.2f" else ".2f"} %s".format(amount, currency),
        modifier = modifier.let { if (shimmer != null) it.shimmer(shimmer) else it },
        style = textStyle,
        color = if (amount < 0) MaterialTheme.customColors.red else LocalCustomColors.current.green
    )
}