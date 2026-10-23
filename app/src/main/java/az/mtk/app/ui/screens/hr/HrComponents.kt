package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * HR modulunun rəng palitrası — tətbiqin digər modullarındakı isti kağız +
 * terracotta üslubu (İnventar / Əməliyyatlar) ilə eynidir.
 */
internal object HrColors {
    val Background = Color(0xFFFAFAF8)
    val Surface = Color(0xFFFFFFFF)
    val Subtle = Color(0xFFF4F4EE)
    val Border = Color(0xFFE2E0D8)
    val Ink = Color(0xFF201A18)
    val Muted = Color(0xFF594139)
    val MutedSoft = Color(0xFF8D7168)
    val Terracotta = Color(0xFFD9531E)
    val TerracottaSoft = Color(0xFFFDF0E9)
    val Olive = Color(0xFF4F6B3A)
    val OliveSoft = Color(0xFFEAF0E2)
    val Error = Color(0xFFBA1A1A)
    val ErrorSoft = Color(0xFFFDECEA)
    val Amber = Color(0xFF9A5B00)
    val AmberSoft = Color(0xFFFFF3E0)
    val Blue = Color(0xFF33567F)
    val BlueSoft = Color(0xFFE8EEF7)
}

internal data class HrPill(
    val label: String,
    val selected: Boolean,
    val count: Int? = null,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HrTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HrColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = HrColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = HrColors.Ink)
            }
        },
        actions = {
            actions()
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Yenilə", tint = HrColors.Terracotta)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = HrColors.Background),
    )
}

@Composable
internal fun HrSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 14.sp, color = HrColors.MutedSoft) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = HrColors.MutedSoft) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, "Təmizlə", tint = HrColors.MutedSoft)
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HrColors.Terracotta,
            unfocusedBorderColor = HrColors.Border,
            focusedContainerColor = HrColors.Surface,
            unfocusedContainerColor = HrColors.Surface,
        ),
    )
}

@Composable
internal fun HrPillRow(
    pills: List<HrPill>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        pills.forEach { pill ->
            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .clickable { pill.onClick() },
                shape = RoundedCornerShape(19.dp),
                color = if (pill.selected) HrColors.Terracotta else HrColors.Subtle,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pill.label,
                        fontSize = 13.sp,
                        fontWeight = if (pill.selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (pill.selected) Color.White else HrColors.Ink,
                        maxLines = 1,
                    )
                    if (pill.count != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (pill.selected) Color.White.copy(alpha = 0.3f) else HrColors.Border,
                        ) {
                            Text(
                                text = pill.count.toString(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pill.selected) Color.White else HrColors.Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HrCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = HrColors.Surface,
        border = BorderStroke(1.dp, HrColors.Border),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
internal fun HrTag(
    text: String,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Surface(shape = RoundedCornerShape(7.dp), color = background, modifier = modifier) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
internal fun HrSectionHeader(
    title: String,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = HrColors.Ink,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, enabled = actionEnabled) {
                Text(actionLabel, fontSize = 12.sp, color = HrColors.Terracotta, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun HrInfoRow(label: String, value: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 11.sp, color = HrColors.MutedSoft)
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            fontSize = 14.sp,
            color = HrColors.Ink,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun HrSelectField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = HrColors.Muted, modifier = Modifier.padding(bottom = 6.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HrColors.Border),
            ) {
                Text(
                    text = options.firstOrNull { it.first == value }?.second ?: "Seçin…",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = if (value.isBlank()) HrColors.MutedSoft else HrColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = HrColors.MutedSoft)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (optionValue, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel, fontSize = 14.sp, color = HrColors.Ink) },
                        onClick = {
                            expanded = false
                            onSelect(optionValue)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun HrTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = placeholder?.let { { Text(it, fontSize = 13.sp, color = HrColors.MutedSoft) } },
        singleLine = singleLine,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HrColors.Terracotta,
            unfocusedBorderColor = HrColors.Border,
            focusedLabelColor = HrColors.Terracotta,
            unfocusedContainerColor = HrColors.Surface,
            focusedContainerColor = HrColors.Surface,
        ),
    )
}

@Composable
internal fun HrDateField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = HrColors.Muted, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(10)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("iiii-aa-gg", fontSize = 13.sp, color = HrColors.MutedSoft) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HrColors.Terracotta,
                unfocusedBorderColor = HrColors.Border,
                focusedContainerColor = HrColors.Surface,
                unfocusedContainerColor = HrColors.Surface,
            ),
        )
    }
}

@Composable
internal fun HrPaginationRow(
    page: Int,
    pageCount: Int,
    totalCount: Int,
    onPage: (Int) -> Unit,
) {
    val pages = pageCount.coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Səhifə $page / $pages · $totalCount qeyd",
            fontSize = 11.sp,
            color = HrColors.MutedSoft,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { onPage((page - 1).coerceAtLeast(1)) },
                enabled = page > 1,
                shape = RoundedCornerShape(9.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("‹", fontSize = 16.sp)
            }
            OutlinedButton(
                onClick = { onPage((page + 1).coerceAtMost(pages)) },
                enabled = page < pages,
                shape = RoundedCornerShape(9.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("›", fontSize = 16.sp)
            }
        }
    }
}

@Composable
internal fun HrFeedbackBanner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val background = if (isError) HrColors.ErrorSoft else HrColors.OliveSoft
    val foreground = if (isError) HrColors.Error else HrColors.Olive
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, foreground.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = foreground,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "Bağla",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = foreground.copy(alpha = 0.8f),
            modifier = Modifier.clickable { onDismiss() },
        )
    }
}

@Composable
internal fun HrLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = HrColors.Terracotta, strokeWidth = 2.dp)
    }
}

@Composable
internal fun HrErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Məlumat alınmadı", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 12.sp, color = HrColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(10.dp)) {
            Text("Yenidən cəhd et", fontSize = 13.sp)
        }
    }
}

@Composable
internal fun HrEmptyState(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HrColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = HrColors.MutedSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
internal fun HrPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) HrColors.Terracotta else HrColors.Terracotta.copy(alpha = 0.4f),
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
internal fun HrSecondaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HrColors.Border),
    ) {
        Text(text, fontSize = 13.sp, color = HrColors.Ink)
    }
}

@Composable
internal fun HrDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(HrColors.Border.copy(alpha = 0.7f)),
    )
}

@Composable
internal fun HrKeyValueGrid(items: List<Pair<String, String?>>, columns: Int = 2) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        HrInfoRow(label, value)
                    }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun HrScreenBackground(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HrColors.Background),
        content = content,
    )
}

@Composable
internal fun HrSheetTitle(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink)
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, "Bağla", tint = HrColors.MutedSoft)
        }
    }
}

@Composable
internal fun HrStaticSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = HrColors.Subtle,
        border = BorderStroke(1.dp, HrColors.Border),
    ) {
        Box(modifier = Modifier.padding(12.dp)) { content() }
    }
}

@Composable
internal fun HrDot(color: Color, size: Int = 8) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
    )
}
