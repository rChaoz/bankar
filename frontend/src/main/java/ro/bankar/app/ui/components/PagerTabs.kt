package ro.bankar.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerTabs(
    tabs: List<Int>,
    pagerState: PagerState = rememberPagerState { tabs.size },
    tabContent: @Composable PagerScope.(Int) -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage, indicator = { list ->
            val page = pagerState.currentPage
            val offset = pagerState.currentPageOffsetFraction

            val tab = list[page]
            val targetTab = list[page + offset.sign.toInt()]

            val targetP = offset.absoluteValue
            val currentP = 1f - targetP

            TabRowDefaults.Indicator(
                modifier = Modifier
                    .wrapContentSize(Alignment.BottomStart)
                    .width(tab.width + targetTab.width * targetP)
                    .offset(x = tab.left * currentP + targetTab.left * targetP - tab.width * targetP / 2)
            )
        }) {
            for (index in tabs.indices) {
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = { Text(text = stringResource(tabs[index]), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Normal) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageContent = tabContent
        )
    }
}