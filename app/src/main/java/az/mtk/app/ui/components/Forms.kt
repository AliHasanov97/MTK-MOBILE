package az.mtk.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---- Text fields -----------------------------------------------------------

@Composable
fun MtkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    required: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        Text(
            buildLabel(label, required),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.Muted,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            placeholder = placeholder?.let {
                { Text(it, style = MaterialTheme.typography.bodyMedium, color = MtkColors.MutedUltraSoft) }
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MtkColors.Green,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MtkColors.Green,
            ),
        )
    }
}

@Composable
fun MtkNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    required: Boolean = false,
) = MtkTextField(
    value = value,
    onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == '.' || it == ',' }) },
    label = label,
    modifier = modifier,
    placeholder = placeholder,
    required = required,
    keyboardType = KeyboardType.Decimal,
)

// ---- Select ----------------------------------------------------------------

data class MtkOption(val value: String, val label: String)

@Composable
fun MtkSelect(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<MtkOption>,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            buildLabel(label, required),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.Muted,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { expanded = true },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = options.firstOrNull { it.value == value }?.label ?: "Seçin…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (value != null) MtkColors.Ink else MtkColors.MutedUltraSoft,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ChevronDownIcon(expanded)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (option.value == value) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (option.value == value) MtkColors.Green else MtkColors.Ink,
                            )
                        },
                        onClick = {
                            onValueChange(option.value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChevronDownIcon(expanded: Boolean) {
    Icon(
        Icons.Filled.KeyboardArrowDown,
        contentDescription = null,
        tint = MtkColors.MutedSoft,
        modifier = Modifier
            .size(18.dp)
            .rotate(if (expanded) 180f else 0f),
    )
}

// ---- Date field ------------------------------------------------------------

@Composable
fun MtkDateField(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            buildLabel(label, required),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.Muted,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showPicker = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value?.let { az.mtk.app.util.Format.date(it.toString()) } ?: "Seçin…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value != null) MtkColors.Ink else MtkColors.MutedUltraSoft,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MtkColors.MutedSoft,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
    if (showPicker) {
        val initialMillis = (value ?: LocalDate.now())
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK", color = MtkColors.Green, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Ləğv et", color = MtkColors.Muted) }
            },
        ) {
            DatePicker(state = pickerState, title = null)
        }
    }
}

// ---- Buttons ---------------------------------------------------------------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MtkColors.Green,
            contentColor = Color.White,
            disabledContainerColor = MtkColors.Green.copy(alpha = 0.45f),
        ),
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MtkColors.Green),
        border = BorderStroke(1.dp, MtkColors.Green.copy(alpha = 0.4f)),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

// ---- Modal (bottom sheet) --------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MtkModal(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    confirmLabel: String = "Yadda saxla",
    confirmEnabled: Boolean = true,
    confirmLoading: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            content = content,
        )
        if (onConfirm != null) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                PrimaryButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    loading = confirmLoading,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ---- Confirm dialog --------------------------------------------------------

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Təsdiqlə",
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MtkColors.Muted)
        },
        confirmButton = {
            if (danger) {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MtkColors.Negative,
                        contentColor = Color.White,
                    ),
                ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
            } else {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MtkColors.Green, contentColor = Color.White),
                ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ləğv et", color = MtkColors.Muted)
            }
        },
    )
}

// ---- Searchable select (API-backed dropdown) --------------------------------

@Composable
fun <T> SearchableSelect(
    label: String,
    required: Boolean = false,
    selectedLabel: String?,
    searchPlaceholder: String,
    onLoad: suspend (String) -> List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<T>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            buildLabel(label, required),
            style = MaterialTheme.typography.labelSmall,
            color = MtkColors.Muted,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { open = true; query = "" },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel ?: "Seçin…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedLabel != null) MtkColors.Ink else MtkColors.MutedUltraSoft,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selectedLabel != null && onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Təmizlə", tint = MtkColors.MutedSoft, modifier = Modifier.size(15.dp))
                    }
                } else {
                    Spacer(Modifier.width(20.dp))
                }
            }
        }
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Seçim et", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = searchPlaceholder,
                )
                Spacer(Modifier.height(8.dp))
            }
            LaunchedEffect(query) {
                delay(300)
                loading = true
                error = null
                runCatching { onLoad(query) }
                    .onSuccess { results = it }
                    .onFailure { error = it.toUserMessage() }
                loading = false
            }
            when {
                loading -> LoadingState("Axtarılır…")
                error != null -> Text(
                    error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MtkColors.Negative,
                    modifier = Modifier.padding(16.dp),
                )
                results.isEmpty() -> EmptyState("Nəticə tapılmadı")
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    items(results) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelect(item)
                                    open = false
                                }
                                .padding(horizontal = 12.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MtkColors.MutedUltraSoft,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                itemLabel(item),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun buildLabel(label: String, required: Boolean): String =
    if (required) "$label *" else label

@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MtkColors.Green),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
