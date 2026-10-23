package az.mtk.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Tətbiqdəki bütün modal pəncərələr üçün vahid görünüş: ekranın aşağısından
 * yuxarıya açılan bottom-sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/**
 * `AlertDialog`-un parametr adları ilə eyni API — mövcud dialoqları minimal
 * dəyişikliklə bottom-sheet-ə köçürmək üçün.
 *
 * [scrollable] yalnız məzmunun öz daxili scroll-u olmayanda `true` edilməlidir;
 * əks halda ikiqat şaquli scroll yaranır.
 */
@Composable
fun AppSheetDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    scrollable: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
) {
    AppSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        scrollable = scrollable,
        containerColor = containerColor,
        shape = shape,
    ) {
        if (title != null) {
            title()
            Spacer(Modifier.height(12.dp))
        }
        text?.invoke()
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dismissButton != null) {
                dismissButton()
                Spacer(Modifier.width(8.dp))
            }
            confirmButton()
        }
    }
}
