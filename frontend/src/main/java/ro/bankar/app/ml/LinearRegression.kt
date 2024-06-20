package ro.bankar.app.ml

import kotlinx.datetime.LocalDate
import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression

class LinearRegression {
    private val model = OLSMultipleLinearRegression()

    fun fit(features: Array<DoubleArray>, values: DoubleArray) {
        model.newSampleData(values, features)
    }

    fun predictPoint(features: DoubleArray): Double {
        val beta = model.estimateRegressionParameters()
        var prediction = beta[0]
        for (i in 1 until beta.size) prediction += beta[i] * features[i - 1]
        return prediction
    }

    fun predict(features: Array<DoubleArray>) = features.map(::predictPoint)
}

object Features {
    fun fromDate(date: LocalDate) = doubleArrayOf(
        date.toEpochDays().toDouble(),
        date.dayOfYear.toDouble(),
        date.dayOfWeek.ordinal.toDouble(),
        date.dayOfMonth.toDouble()
    )
}