package ro.bankar.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.date_time.DateTimeDialog
import com.maxkeppeler.sheets.date_time.models.DateTimeConfig
import com.maxkeppeler.sheets.date_time.models.DateTimeSelection
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.DefaultAlpha
import com.patrykandpatrick.vico.core.axis.Axis
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.component.text.textComponent
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import ro.bankar.app.R
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.Repository
import ro.bankar.app.ui.collectAsMutableState
import ro.bankar.app.ui.components.NavScreen
import ro.bankar.app.ui.components.PagerTabs
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SDirection
import ro.bankar.util.atStartOfMonth
import ro.bankar.util.here
import ro.bankar.util.todayHere
import java.time.format.FormatStyle
import kotlin.math.round

class AccountStatsModel(repository: Repository, accountID: Int) : ViewModel() {
    val account = repository.account(accountID)
    var selectedMonth = MutableStateFlow(Clock.System.todayHere().atStartOfMonth())

    private val combinedFlow = account.combine(selectedMonth) { a, b -> a to b }

    private val monthlyGraphData = combinedFlow.map { (account, selectedMonth) ->
        val items = (account.transfers + account.transactions)
            .groupingBy { item -> item.timestamp.here().date.atStartOfMonth() }
            .fold(0.0 to 0.0) { (incoming, outgoing), item ->
                when (item) {
                    is SBankTransfer -> if (item.direction == SDirection.Sent) incoming to (outgoing + item.amount) else (incoming + item.amount) to outgoing
                    is SCardTransaction -> incoming to (outgoing + item.amount)
                    else -> throw IllegalStateException("impossible")
                }
            }
        // Take last 6 months
        val incomingEntries = mutableListOf<ChartEntry>()
        val outgoingEntries = mutableListOf<ChartEntry>()
        for (date in (0..5).map { selectedMonth - DatePeriod(months = it) }) {
            val (incoming, outgoing) = items[date] ?: (0.0 to 0.0)
            incomingEntries += entryOf((date.monthNumber - 1 + date.year * 12).toFloat(), incoming)
            outgoingEntries += entryOf((date.monthNumber - 1 + date.year * 12).toFloat(), outgoing)
        }
        listOf(incomingEntries as List<ChartEntry>, outgoingEntries as List<ChartEntry>)
    }
    val monthlyModelProducer = ChartEntryModelProducer()

    private val perMonthGraphData = combinedFlow.map { (account, selectedMonth) ->
        val items = (account.transfers + account.transactions)
            .groupingBy { item -> item.timestamp.here().date }
            .fold(0.0) { amount, item ->
                when (item) {
                    is SBankTransfer -> if (item.direction == SDirection.Sent) amount - item.amount else amount + item.amount
                    is SCardTransaction -> amount - item.amount
                    else -> throw IllegalStateException("impossible")
                }
            }.toSortedMap()
        var current = account.base.spendable
        for (key in items.keys.reversed()) {
            current += items[key]!!
            items[key] = current
        }
        val monthData = items.mapNotNull { (date, amount) ->
            if (date.year != selectedMonth.year || date.month != selectedMonth.month) null
            else entryOf(date.dayOfMonth, amount)
        }
        listOf(monthData)
    }
    val perMonthModelProducer = ChartEntryModelProducer()

    init {
        viewModelScope.launch {
            monthlyGraphData.collect {
                @Suppress("DeferredResultUnused")
                monthlyModelProducer.setEntriesSuspending(it)
            }
        }
        viewModelScope.launch {
            perMonthGraphData.collect {
                @Suppress("DeferredResultUnused")
                perMonthModelProducer.setEntriesSuspending(it)
            }
        }
    }
}

private val monthFormat = LocalDate.Format {
    monthName(MonthNames.ENGLISH_FULL)
    char(' ')
    year()
}

@Composable
fun AccountStatsScreen(onDismiss: () -> Unit, accountID: Int) {
    val repository = LocalRepository.current
    val model = viewModel<AccountStatsModel> { AccountStatsModel(repository, accountID) }
    val itemPlacer = remember {
        val default = AxisItemPlacer.Vertical.default(maxItemCount = 5)
        object : AxisItemPlacer.Vertical by default {
            override fun getLabelValues(context: ChartDrawContext, axisHeight: Float, maxLabelHeight: Float, position: AxisPosition.Vertical) =
                default.getLabelValues(context, axisHeight, maxLabelHeight, position).map { round(it / 10) * 10 }
        }
    }
    val monthPicker = rememberUseCaseState()
    var selectedMonth by model.selectedMonth.collectAsMutableState()
    DateTimeDialog(
        monthPicker,
        header = Header.Default(stringResource(R.string.select_month)),
        selection = DateTimeSelection.Date(
            selectedDate = selectedMonth.toJavaLocalDate(),
            dateFormatStyle = FormatStyle.MEDIUM
        ) { selectedMonth = it.toKotlinLocalDate().atStartOfMonth() },
        config = DateTimeConfig(maxYear = Clock.System.todayHere().year)
    )

    NavScreen(onDismiss, title = R.string.statistics) {
        PagerTabs(listOf(R.string.per_month, R.string.detailed)) { tab ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (tab) {
                    0 -> {
                        Text(text = stringResource(R.string.spending_per_month), style = MaterialTheme.typography.titleLarge)

                        val shape = Shapes.roundedCornerShape(50, 50, 0, 0)
                        Chart(
                            columnChart(
                                columns = listOf(
                                    LineComponent(MaterialTheme.customColors.red.toArgb(), 6f, shape),
                                    LineComponent(MaterialTheme.customColors.green.toArgb(), 6f, shape),
                                ),
                                innerSpacing = 2.dp,
                                spacing = 10.dp
                            ),
                            model.monthlyModelProducer,
                            startAxis = rememberStartAxis(itemPlacer = itemPlacer, sizeConstraint = Axis.SizeConstraint.Exact(40f)),
                            bottomAxis = rememberBottomAxis(
                                valueFormatter = { month, _ -> MonthNames.ENGLISH_ABBREVIATED.names[month.toInt() % 12] },
                            ),
                            marker = remember {
                                MarkerComponent(
                                    textComponent {
                                        textSizeSp = 15f
                                        padding.bottomDp = 6f
                                    },
                                    null,
                                    null
                                )
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    1 -> {
                        Text(text = stringResource(R.string.detailed_spending), style = MaterialTheme.typography.titleLarge)

                        val color = MaterialTheme.colorScheme.secondary
                        Chart(
                            lineChart(
                                lines = listOf(
                                    LineChart.LineSpec(
                                        lineColor = color.toArgb(),
                                        lineThicknessDp = 4f,
                                        lineBackgroundShader = DynamicShaders.fromBrush(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    color.copy(alpha = DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                                    color.copy(alpha = DefaultAlpha.LINE_BACKGROUND_SHADER_END),
                                                ),
                                            ),
                                        )
                                    ),
                                    LineChart.LineSpec(lineColor = MaterialTheme.colorScheme.tertiary.toArgb(), lineThicknessDp = 2f)
                                ),
                                axisValuesOverrider = remember(selectedMonth) {
                                    object : AxisValuesOverrider<ChartEntryModel> {
                                        override fun getMinX(model: ChartEntryModel) = 1f

                                        // Get last day of month
                                        override fun getMaxX(model: ChartEntryModel) =
                                            (selectedMonth.atStartOfMonth() + DatePeriod(months = 1) - DatePeriod(days = 1)).dayOfMonth.toFloat()
                                    }
                                }
                            ),
                            model.perMonthModelProducer,
                            startAxis = rememberStartAxis(itemPlacer = itemPlacer, sizeConstraint = Axis.SizeConstraint.Exact(60f)),
                            bottomAxis = rememberBottomAxis(),
                            getXStep = { 4f },
                            marker = remember {
                                MarkerComponent(
                                    textComponent {
                                        textSizeSp = 15f
                                        padding.bottomDp = 6f
                                    },
                                    null,
                                    null
                                )
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (tab == 0) Text(text = stringResource(R.string.up_to), style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = monthFormat.format(selectedMonth),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = { monthPicker.show() },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .weight(1f),
                    ) {
                        Text(text = stringResource(R.string.change_month), textAlign = TextAlign.Center)
                    }
                }
            }
        }

    }
}

@Composable
@Preview(showBackground = true)
private fun AccountStatsScreenPreview() {
    AppTheme {
        AccountStatsScreen({}, 1)
    }
}