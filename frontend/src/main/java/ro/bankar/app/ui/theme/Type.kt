@file:Suppress("PrivatePropertyName")

package ro.bankar.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ro.bankar.app.R

private val BebasNeue = FontFamily(Font(R.font.bebasneue_regular))

// Set of Material typography styles to start with
val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 48.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Medium,
        fontSize = 50.sp,
        lineHeight = 58.sp,
    ),
    displayLarge = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Medium,
        fontSize = 62.sp,
        lineHeight = 70.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = .2.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = .1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = .5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = .5.sp,
    ),
)