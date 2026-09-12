package com.mangalore.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.mangalore.app.R

val RubikFontFamily = FontFamily(
    Font(R.font.rubik_medium)
)

/** Drop-in equivalent of the iOS `Font.rubik(size:weight:)` helper — weight
 * is accepted for call-site symmetry but the bundled font only has one
 * (Medium) weight, same as the iOS app. */
fun rubik(size: Int) = TextStyle(fontFamily = RubikFontFamily, fontSize = size.sp)

val MangaloreTypography = Typography(
    bodyLarge = TextStyle(fontFamily = RubikFontFamily, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = RubikFontFamily, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = RubikFontFamily, fontSize = 12.sp),
    titleLarge = TextStyle(fontFamily = RubikFontFamily, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = RubikFontFamily, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = RubikFontFamily, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = RubikFontFamily, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = RubikFontFamily, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = RubikFontFamily, fontSize = 11.sp)
)
