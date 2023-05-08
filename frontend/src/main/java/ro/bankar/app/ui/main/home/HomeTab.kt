package ro.bankar.app.ui.main.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.ui.HideFABOnScroll
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.MainTab
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.model.SBankAccount
import ro.bankar.model.SRecentActivity

object HomeTab : MainTab<HomeTab.Model>(1, "home", R.string.home) {
    class Model : MainTabModel() {
        val scrollShowFAB = mutableStateOf(true)
        var accounts by mutableStateOf<List<SBankAccount>?>(null)
        var recentActivity by mutableStateOf<SRecentActivity?>(null)

        // Only show FAB after data has loaded
        override val showFAB = derivedStateOf { scrollShowFAB.value && accounts != null && recentActivity != null }
    }

    @Composable
    override fun viewModel() = viewModel<Model>()

    @Composable
    override fun Content(model: Model, navigation: NavHostController) {
        val repository = LocalRepository.current

        // Get accounts data
        LaunchedEffect(true) {
            repository.accounts.requestEmit(true)
            repository.recentActivity.requestEmit(true)
            launch { repository.accounts.collect { model.accounts = it } }
            launch { repository.recentActivity.collect { model.recentActivity = it } }
        }

        // Hide FAB when scrolling down
        val scrollState = rememberScrollState()
        HideFABOnScroll(state = scrollState, setFABShown = model.scrollShowFAB.component2())

        // TODO Pull to refresh?
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)
            if (model.recentActivity == null) RecentActivityShimmer(shimmer)
            else RecentActivity(model.recentActivity!!)

            if (model.accounts == null) {
                BankAccountShimmer(shimmer)
                AssetsShimmer(shimmer)
            } else {
                for (account in model.accounts!!) BankAccount(data = account)
                if (model.accounts!!.isEmpty()) InfoCard(text = R.string.no_bank_accounts, onClick = {
                    navigation.navigate(MainNav.NewBankAccount.route)
                })
                Assets()
            }
        }
    }

    @Composable
    override fun FABContent(model: Model, navigation: NavHostController) {
        ExtendedFloatingActionButton(
            text = { Text(text = stringResource(R.string.bank_account)) },
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.create)) },
            onClick = { navigation.navigate(MainNav.NewBankAccount.route) }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    AppTheme {
        HomeTab.Content(HomeTab.viewModel(), rememberNavController())
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun HomePreviewDark() {
    AppTheme {
        HomeTab.Content(HomeTab.viewModel(), rememberNavController())
    }
}