package az.mtk.app.ui.screens.operations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.AppSheetDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal object OpsColors {
    val Background = Color(0xFFFAFAF8)
    val Surface = Color(0xFFFFFFFF)
    val Subtle = Color(0xFFF4F4EE)
    val Container = Color(0xFFF8EBE6)
    val Border = Color(0xFFE2E0D8)
    val Ink = Color(0xFF201A18)
    val Muted = Color(0xFF594139)
    val MutedSoft = Color(0xFF8D7168)
    val Terracotta = Color(0xFFD9531E)
    val TerracottaVivid = Color(0xFFE85D04)
    val Olive = Color(0xFF4F6B3A)
    val OliveSoft = Color(0xFFEAF0E2)
    val Error = Color(0xFFBA1A1A)
    val ErrorSoft = Color(0xFFFDECEA)
    val Amber = Color(0xFF9A5B00)
    val AmberSoft = Color(0xFFFFF3E0)
    val Blue = Color(0xFF33567F)
    val BlueSoft = Color(0xFFE8EEF7)
}

internal data class OpsPill(
    val label: String,
    val selected: Boolean,
    val count: Int? = null,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpsTopBar(
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
                    color = OpsColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = OpsColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri", tint = OpsColors.Ink)
            }
        },
        actions = {
            actions()
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Yenilə", tint = OpsColors.Terracotta)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = OpsColors.Background),
    )
}

@Composable
internal fun OpsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 14.sp, color = OpsColors.MutedSoft) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = OpsColors.MutedSoft) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Close, "Təmizlə", tint = OpsColors.MutedSoft)
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OpsColors.Terracotta,
            unfocusedBorderColor = OpsColors.Border,
            focusedContainerColor = OpsColors.Surface,
            unfocusedContainerColor = OpsColors.Surface,
        ),
    )
}

@Composable
internal fun OpsPillRow(
    pills: List<OpsPill>,
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
                color = if (pill.selected) OpsColors.Terracotta else OpsColors.Subtle,
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
                        color = if (pill.selected) Color.White else OpsColors.Ink,
                        maxLines = 1,
                    )
                    if (pill.count != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (pill.selected) Color.White.copy(alpha = 0.3f) else OpsColors.Border,
                        ) {
                            Text(
                                text = pill.count.toString(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pill.selected) Color.White else OpsColors.Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OpsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = OpsColors.Surface,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
internal fun OpsChip(
    text: String,
    foreground: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
    }
}

@Composable
internal fun OpsChargeStatusChip(status: Int?) {
    val (label, fg, bg) = when (status) {
        0 -> Triple("Ödənilməyib", OpsColors.Amber, OpsColors.AmberSoft)
        1 -> Triple("Qismən ödənilib", OpsColors.Blue, OpsColors.BlueSoft)
        2 -> Triple("Ödənilib", OpsColors.Olive, OpsColors.OliveSoft)
        3 -> Triple("Ləğv edilib", OpsColors.MutedSoft, OpsColors.Subtle)
        else -> Triple("—", OpsColors.MutedSoft, OpsColors.Subtle)
    }
    OpsChip(label, fg, bg)
}

@Composable
internal fun OpsInfoRow(label: String, value: String, valueColor: Color = OpsColors.Ink) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OpsColors.MutedSoft,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
internal fun OpsSectionTitle(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, fontSize = 12.sp, color = OpsColors.Terracotta, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun OpsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OpsColors.Terracotta,
            contentColor = Color.White,
            disabledContainerColor = OpsColors.Border,
            disabledContentColor = OpsColors.MutedSoft,
        ),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun OpsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (danger) OpsColors.Error else OpsColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (danger) OpsColors.Error else OpsColors.Ink,
        ),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun OpsFeedback(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isError) OpsColors.ErrorSoft else OpsColors.OliveSoft,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 12.sp,
                color = if (isError) OpsColors.Error else OpsColors.Olive,
            )
            TextButton(onClick = onDismiss) {
                Text("Bağla", fontSize = 11.sp, color = OpsColors.Muted)
            }
        }
    }
}

@Composable
internal fun OpsEmpty(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("₼", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = OpsColors.Border)
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 12.sp, color = OpsColors.MutedSoft)
    }
}

@Composable
internal fun OpsLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = OpsColors.Terracotta, strokeWidth = 2.dp)
    }
}

@Composable
internal fun OpsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Məlumat alınmadı", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpsColors.Error)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 12.sp, color = OpsColors.Muted)
        Spacer(Modifier.height(12.dp))
        OpsSecondaryButton("Yenidən cəhd et", onRetry)
    }
}

@Composable
internal fun OpsPagination(
    page: Int,
    total: Int,
    pageSize: Int = 20,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val pageCount = maxOf(1, (total + pageSize - 1) / pageSize)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Cəmi $total · səhifə ${page + 1}/$pageCount",
            fontSize = 11.sp,
            color = OpsColors.MutedSoft,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpsSecondaryButton("Əvvəlki", onPrev, enabled = page > 0)
            OpsSecondaryButton("Növbəti", onNext, enabled = page + 1 < pageCount)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpsFormSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OpsColors.Background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(OpsColors.Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
internal fun OpsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = placeholder?.let { { Text(it, fontSize = 13.sp, color = OpsColors.MutedSoft) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OpsColors.Terracotta,
            unfocusedBorderColor = OpsColors.Border,
            focusedContainerColor = OpsColors.Surface,
            unfocusedContainerColor = OpsColors.Surface,
        ),
    )
}

@Composable
internal fun OpsSelectField(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Seçin…",
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = OpsColors.MutedSoft)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OpsColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = OpsColors.Surface,
                    contentColor = OpsColors.Ink,
                ),
            ) {
                Text(
                    text = options.firstOrNull { it.first == selectedId }?.second ?: placeholder,
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ArrowDropDown, null, tint = OpsColors.MutedSoft)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(OpsColors.Surface),
            ) {
                options.forEach { (id, text) ->
                    DropdownMenuItem(
                        text = { Text(text, fontSize = 13.sp, color = OpsColors.Ink) },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OpsDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text("YYYY-MM-DD", fontSize = 13.sp, color = OpsColors.MutedSoft) },
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, "Tarix seç", tint = OpsColors.Terracotta)
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OpsColors.Terracotta,
            unfocusedBorderColor = OpsColors.Border,
            focusedContainerColor = OpsColors.Surface,
            unfocusedContainerColor = OpsColors.Surface,
        ),
    )

    if (showPicker) {
        val initialMillis = runCatching {
            LocalDate.parse(value.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        AppSheet(
            onDismissRequest = { showPicker = false },
            scrollable = true,
            containerColor = OpsColors.Background,
        ) {
            Text("Tarix seç", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink)
            Spacer(Modifier.height(8.dp))
            DatePicker(state = pickerState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpsSecondaryButton("Ləğv et", { showPicker = false })
                Spacer(Modifier.width(8.dp))
                OpsPrimaryButton(
                    text = "Seç",
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onValueChange(date.toString())
                        }
                        showPicker = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun OpsFormActions(
    saving: Boolean,
    submitLabel: String,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OpsSecondaryButton("Ləğv et", onCancel, modifier = Modifier.weight(1f))
        OpsPrimaryButton(
            text = if (saving) "Saxlanılır…" else submitLabel,
            onClick = onSubmit,
            modifier = Modifier.weight(1f),
            enabled = enabled && !saving,
        )
    }
}

@Composable
internal fun OpsDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSheetDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpsColors.Ink) },
        text = { Column(content = content) },
        confirmButton = {},
        containerColor = OpsColors.Surface,
    )
}
