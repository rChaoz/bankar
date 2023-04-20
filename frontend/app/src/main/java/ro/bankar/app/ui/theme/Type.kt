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

private val Roboto = FontFamily(
    Font(R.font.roboto_thin, FontWeight.Thin),
    Font(R.font.roboto_thinitalic, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_lightitalic, FontWeight.Light, FontStyle.Italic),

    Font(R.font.roboto_regular, FontWeight.Light),

    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.roboto_bold, FontWeight.Bold),
    Font(R.font.roboto_bolditalic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.roboto_black, FontWeight.Black),
    Font(R.font.roboto_blackitalic, FontWeight.Black, FontStyle.Italic),
)

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
    /* Text styles to override
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
titleLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Normal,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
),
labelSmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.5.sp
)
*/
)