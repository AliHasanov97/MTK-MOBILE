package az.mtk.app.ui.screens.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.ArticleStatus
import az.mtk.app.ui.screens.operations.OpsCard
import az.mtk.app.ui.screens.operations.OpsChip
import az.mtk.app.ui.screens.operations.OpsColors

@Composable
internal fun NewsStatusChip(status: String?, modifier: Modifier = Modifier) {
    val (foreground, background) = when (status) {
        ArticleStatus.PUBLISHED -> OpsColors.Olive to OpsColors.OliveSoft
        ArticleStatus.DRAFT -> OpsColors.Amber to OpsColors.AmberSoft
        ArticleStatus.ARCHIVED -> OpsColors.Muted to OpsColors.Subtle
        else -> OpsColors.MutedSoft to OpsColors.Subtle
    }
    OpsChip(
        text = ArticleStatus.label(status),
        foreground = foreground,
        background = background,
        modifier = modifier,
    )
}

@Composable
internal fun NewsAttachmentChip(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    OpsChip(
        text = "📎 $count",
        foreground = OpsColors.Blue,
        background = OpsColors.BlueSoft,
        modifier = modifier,
    )
}

@Composable
internal fun NewsSummaryCard(
    totalCount: Int,
    filtered: Boolean,
) {
    OpsCard(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xəbərlər",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpsColors.Ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Sayt və tətbiq üçün elanlar",
                    fontSize = 11.sp,
                    color = OpsColors.MutedSoft,
                )
            }
            OpsChip(
                text = "$totalCount xəbər",
                foreground = OpsColors.Terracotta,
                background = OpsColors.Container,
            )
        }
        if (filtered) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Süzgəc tətbiq olunub",
                fontSize = 11.sp,
                color = OpsColors.Muted,
            )
        }
    }
}

@Composable
internal fun NewsMetaRow(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = OpsColors.MutedSoft,
        maxLines = 2,
    )
}

@Composable
internal fun NewsEditorSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpsColors.Ink,
        )
        content()
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
}

@Composable
internal fun NewsInlineError(message: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
internal fun NewsFileRow(
    name: String,
    supporting: String?,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "📎",
            modifier = Modifier.padding(end = 8.dp),
            fontSize = 13.sp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OpsColors.Ink,
                maxLines = 1,
            )
            if (!supporting.isNullOrBlank()) {
                Text(
                    text = supporting,
                    fontSize = 10.sp,
                    color = OpsColors.MutedSoft,
                    maxLines = 1,
                )
            }
        }
        androidx.compose.material3.TextButton(
            onClick = onAction,
            enabled = enabled,
        ) {
            Text(
                text = actionLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) OpsColors.Terracotta else OpsColors.MutedSoft,
            )
        }
    }
}
