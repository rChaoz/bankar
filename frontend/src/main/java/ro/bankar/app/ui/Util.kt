package ro.bankar.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintSetScope
import ro.bankar.app.R
import java.time.Month

fun ConstraintSetScope.createRefsFor(vararg ids: String) = ids.map { createRefFor(it) }

@Composable
fun monthStringResource(month: kotlinx.datetime.Month) = stringResource(id = when (month) {
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
})