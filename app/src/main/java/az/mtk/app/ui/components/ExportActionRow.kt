package az.mtk.app.ui.components


import az.mtk.app.data.files.FileRepository
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch

/**
 * A small action row that triggers a backend file export (PDF/Excel), shows
 * progress, downloads it and opens it with an external viewer.
 */
@Composable
fun ExportActionRow(
    title: String,
    subtitle: String? = null,
    path: String,
    method: String = "GET",
    body: String? = null,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf("idle") }
    var message by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
            }
            message?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state == "error") MtkColors.Negative else MtkColors.Positive,
                )
            }
        }
        TextButton(
            onClick = {
                if (state == "busy") return@TextButton
                state = "busy"
                message = null
                scope.launch {
                    runCatching { FileRepository.download(path, method, body) }
                        .onSuccess { result ->
                            state = "done"
                            val opened = FileRepository.registerAndOpen(context, result)
                            message = if (opened) "Fayl endirildi və açıldı" else "Endirildi: ${result.fileName}"
                        }
                        .onFailure {
                            state = "error"
                            message = it.toUserMessage()
                        }
                }
            },
        ) {
            if (state == "busy") {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = MtkColors.Green,
                )
            } else {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    tint = MtkColors.Green,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                if (state == "busy") "Endirilir…" else "Endir",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
