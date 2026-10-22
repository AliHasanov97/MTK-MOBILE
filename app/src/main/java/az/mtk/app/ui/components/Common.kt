package az.mtk.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.ui.theme.MtkColors

@Composable
fun BrandMark(size: Dp = 38.dp, letterSize: Int = 20) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(
                Brush.linearGradient(
                    colors = listOf(MtkColors.Green, MtkColors.GreenDark),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "M",
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = letterSize.sp,
        )
    }
}

@Composable
fun BrandRow(modifier: Modifier = Modifier, showTagline: Boolean = false) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Spacer(Modifier.width(10.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "MTK",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MtkColors.Positive),
                )
            }
            if (showTagline) {
                Text(
                    "İdarəetmə sistemi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MtkColors.MutedSoft,
                )
            }
        }
    }
}

@Composable
fun MtkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(shape)
                        .clickable(onClick = onClick)
                } else Modifier,
            ),
        color = containerColor,
        shape = shape,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

enum class Tone { Neutral, Brand, Success, Warning, Danger, Info }

@Composable
fun Pill(
    text: String,
    tone: Tone = Tone.Neutral,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    val (bg, fg, dotColor) = when (tone) {
        Tone.Neutral -> Triple(MtkColors.Soft, MtkColors.Muted, MtkColors.MutedSoft)
        Tone.Brand -> Triple(MtkColors.IconBoxBg, MtkColors.Green, MtkColors.Green)
        Tone.Success -> Triple(MtkColors.PositiveBg, MtkColors.Positive, MtkColors.Positive)
        Tone.Warning -> Triple(MtkColors.WarningBg, MtkColors.Warning, MtkColors.Warning)
        Tone.Danger -> Triple(MtkColors.NegativeBg, MtkColors.Negative, MtkColors.Negative)
        Tone.Info -> Triple(MtkColors.InfoBg, MtkColors.Info, MtkColors.Info)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    caption: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    MtkCard(
        modifier = modifier,
        onClick = onClick,
        padding = 14.dp,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MtkColors.MutedSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MtkColors.IconBoxBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MtkColors.Green,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MtkColors.MutedSoft,
            )
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = MtkColors.MutedSoft) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MtkColors.MutedSoft, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Təmizlə", tint = MtkColors.MutedSoft, modifier = Modifier.size(18.dp))
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MtkColors.Green,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MtkColors.Green,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    meta: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                } else Modifier,
            )
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(13.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MtkColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (meta != null) {
                Spacer(Modifier.height(3.dp))
                Text(meta, style = MaterialTheme.typography.labelSmall, color = MtkColors.MutedSoft)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MtkColors.MutedUltraSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun AvatarCircle(
    initials: String,
    size: Dp = 40.dp,
    background: Color = MtkColors.Green,
    foreground: Color = Color.White,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = foreground,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MtkColors.Muted,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.58f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun LoadingState(message: String = "Yüklənir…") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MtkColors.Green,
        )
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
    }
}

@Composable
fun EmptyState(text: String, icon: ImageVector = Icons.Outlined.Inbox) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MtkColors.Soft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MtkColors.MutedSoft,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MtkColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    MtkCard(
        padding = 18.dp,
        containerColor = MtkColors.NegativeBg.copy(alpha = 0.5f),
        borderColor = MtkColors.Negative.copy(alpha = 0.25f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MtkColors.Negative,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Xəta baş verdi",
                style = MaterialTheme.typography.titleMedium,
                color = MtkColors.Negative,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onRetry,
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Yenidən cəhd et", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SimpleTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = if (onBack != null) 6.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = MtkColors.Ink,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MtkColors.MutedSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (action != null) action()
            }
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun ModuleIcon(
    icon: ImageVector? = null,
    glyph: String? = null,
    tint: Color = MtkColors.Green,
    background: Color = MtkColors.IconBoxBg,
    size: Dp = 40.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.52f),
            )
        } else if (glyph != null) {
            Text(
                glyph,
                color = tint,
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun KeyValueHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MtkColors.MutedSoft,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp),
    )
}

@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = MtkColors.Muted, modifier = Modifier.size(20.dp))
    }
}
