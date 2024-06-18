package ro.bankar.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.datetime.format.MonthNames
import ro.bankar.app.ui.theme.AppTheme
import ro.bankar.app.ui.theme.customColors

@Composable
fun ChartTest() {
    val modelProducer = remember {
        ChartEntryModelProducer(
            listOf(entryOf(1f, 2f), entryOf(2f, 6f), entryOf(3f, 4f)),
            listOf(entryOf(1f, 3f), entryOf(2f, 5f), entryOf(3f, 2f)),
        )
    }
    Column {
        val shape = Shapes.roundedCornerShape(40, 40, 0, 0)
        Chart(
            chart = columnChart(
                columns = listOf(
                    LineComponent(MaterialTheme.customColors.red.toArgb(), 6f, shape),
                    LineComponent(MaterialTheme.customColors.green.toArgb(), 6f, shape),
                )
            ),
            model = modelProducer.requireModel(),
            startAxis = rememberStartAxis(itemPlacer = AxisItemPlacer.Vertical.default(4)),
            bottomAxis = rememberBottomAxis(valueFormatter = { value, _ ->
                MonthNames.ENGLISH_ABBREVIATED.names[value.toInt()]
            })
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChartTestPreview() {
    AppTheme {
        ChartTest()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChartTestPreviewDark() {
    AppTheme(useDarkTheme = true) {
        ChartTest()
    }
}