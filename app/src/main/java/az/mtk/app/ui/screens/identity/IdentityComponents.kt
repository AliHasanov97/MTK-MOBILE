package az.mtk.app.ui.screens.identity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors
import az.mtk.app.ui.screens.operations.OpsPrimaryButton
import az.mtk.app.ui.screens.operations.OpsSecondaryButton
import az.mtk.app.util.Format

/** İstifadəçinin adı və soyadı; boş olduqda email/username göstərilir. */
internal fun identityUserName(firstName: String?, lastName: String?, fallback: String? = null): String {
    val name = listOfNotNull(firstName, lastName)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    return name.ifBlank { fallback?.takeIf { it.isNotBlank() } ?: "—" }
}

@Composable
internal fun IdentitySegmented(
    selected: IdentityTab,
    onSelect: (IdentityTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = OpsColors.Subtle,
        border = BorderStroke(1.dp, OpsColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IdentityTab.entries.forEach { tab ->
                val active = tab == selected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (active) OpsColors.Surface else Color.Transparent,
                    shadowElevation = if (active) 1.dp else 0.dp,
                ) {
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(vertical = 9.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) OpsColors.Ink else OpsColors.MutedSoft,
                    )
                }
            }
        }
    }
}

@Composable
internal fun IdentitySummaryCard(
    title: String,
    subtitle: String,
    count: Int,
    countLabel: String,
    extra: String? = null,
) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChip(
                text = "$count $countLabel",
                foreground = OpsColors.Terracotta,
                background = OpsColors.Container,
            )
        }

        if (!extra.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = extra,
                fontSize = 11.sp,
                color = OpsColors.Muted,
            )
        }
    }
}

@Composable
internal fun IdentityAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = OpsColors.Container,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = Format.initials(name),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OpsColors.Terracotta,
                )
            }
        }
    }
}

@Composable
internal fun UserStatusChip(status: String?, modifier: Modifier = Modifier) {
    val normalized = status?.lowercase()
    val label = when (normalized) {
        "active" -> "Aktiv"
        "inactive" -> "Deaktiv"
        "blocked" -> "Bloklanıb"
        null, "" -> "—"
        else -> status
    }
    val foreground = when (normalized) {
        "active" -> OpsColors.Olive
        "blocked" -> OpsColors.Error
        else -> OpsColors.Muted
    }
    val background = when (normalized) {
        "active" -> OpsColors.OliveSoft
        "blocked" -> OpsColors.ErrorSoft
        else -> OpsColors.Subtle
    }
    OpsChip(
        text = label,
        foreground = foreground,
        background = background,
        modifier = modifier,
    )
}

@Composable
internal fun RoleStatusChip(isActive: Boolean, modifier: Modifier = Modifier) {
    OpsChip(
        text = if (isActive) "Aktiv" else "Deaktiv",
        foreground = if (isActive) OpsColors.Olive else OpsColors.MutedSoft,
        background = if (isActive) OpsColors.OliveSoft else OpsColors.Subtle,
        modifier = modifier,
    )
}

@Composable
internal fun IdentityTagChip(text: String, modifier: Modifier = Modifier) {
    OpsChip(
        text = text,
        foreground = OpsColors.Blue,
        background = OpsColors.BlueSoft,
        modifier = modifier,
    )
}

internal fun roleTypeLabel(roleType: String?): String = when (roleType?.trim()?.lowercase()) {
    null, "" -> "—"
    "default" -> "Standart"
    "realm" -> "Sistem"
    "custom" -> "Fərdi"
    "group" -> "Qrup"
    else -> roleType
}

@Composable
internal fun IdentityInlineError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OpsColors.ErrorSoft,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            fontSize = 11.sp,
            color = OpsColors.Error,
        )
    }
}

@Composable
internal fun IdentityConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppSheet(
        onDismissRequest = onDismiss,
        containerColor = OpsColors.Background,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = OpsColors.Muted,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OpsSecondaryButton(
                text = "Ləğv et",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
            OpsPrimaryButton(
                text = if (busy) "Silinir…" else confirmLabel,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !busy,
            )
        }
    }
}

@Composable
internal fun IdentityEmptyActionButton(
    text: String,
    onClick: () -> Unit,
) {
    OpsPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    )
}

@Composable
internal fun IdentitySelectableRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    supporting: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) OpsColors.Terracotta else OpsColors.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Text(
                text = "✓",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OpsColors.Terracotta,
            )
        }
    }
}
