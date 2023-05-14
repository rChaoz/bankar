@file:Suppress("PrivatePropertyName")

package ro.bankar.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ro.bankar.app.R

private val BebasNeue = FontFamily(Font(R.font.bebasneue_regular))

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Medium,
        fontSize = 62.sp,
        lineHeight = 70.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Medium,
        fontSize = 50.sp,
        lineHeight = 58.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        lineHeight = 46.sp,
    ),

    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = .3.sp
    ),

    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = .1.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = .2.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = .1.sp,
    ),

    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = .1.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = .5.sp,
    ),
    labelSmall = TextStyle(
        fontStyle = FontStyle.Italic,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = .4.sp,
    ),
)