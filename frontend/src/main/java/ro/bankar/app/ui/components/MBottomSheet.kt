package ro.bankar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp

/**
 * Modal bottom sheet wrapper to allow context to be passed through, to preserve custom language
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    context: Context = LocalContext.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest, modifier, sheetState, shape, containerColor, contentColor, tonalElevation, scrimColor, dragHandle) {
        CompositionLocalProvider(LocalContext provides context) { content() }
    }
}