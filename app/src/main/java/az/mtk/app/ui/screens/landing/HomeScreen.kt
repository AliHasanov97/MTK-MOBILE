package az.mtk.app.ui.screens.landing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.Module
import az.mtk.app.data.modules
import az.mtk.app.data.residentFeatures
import az.mtk.app.data.steps
import az.mtk.app.data.api.ArtArticle
import az.mtk.app.data.api.LandingRepository
import az.mtk.app.ui.components.NewsCard
import az.mtk.app.ui.components.SectionHeader
import az.mtk.app.ui.theme.Civic
import az.mtk.app.util.Format

/**
 * Public landing page in the same "Civic Hearth" language as the signed-in APK:
 * a forest hero that flows out of the app bar, paper cards with hairline borders
 * and terracotta actions.
 */
@Composable
fun HomeScreen(
    repository: LandingRepository,
    onLoginClick: () -> Unit,
    onNavigateToNews: () -> Unit,
    onArticleClick: (String) -> Unit,
    onNavigateToContact: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var latestArticles by remember { mutableStateOf<List<ArtArticle>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            latestArticles = repository.getLatestArticles(count = 3)
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Failed to load articles", e)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Civic.Background),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item { HeroPanel(onLoginClick = onLoginClick) }

        item {
            SectionHeader(
                title = "Sistemin imkanları",
                subtitle = "Səkkiz modul, bir ortaq məlumat bazası",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)
            )
        }
        item {
            ModuleGrid(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
            )
        }

        item { Spacer(Modifier.height(22.dp)) }
        item {
            ResidentsPanel(
                onLoginClick = onLoginClick,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            SectionHeader(
                title = "Necə işləyir",
                subtitle = "Qeydiyyatdan hesabata dörd addım",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)
            )
        }
        item {
            StepsPanel(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)
            )
        }

        item {
            SectionHeader(
                title = "Son xəbərlər",
                subtitle = "Kooperativdən elanlar və yeniliklər",
                actionText = "Hamısı",
                onActionClick = onNavigateToNews,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp)
            )
        }
        item {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (latestArticles.isEmpty()) {
                    EmptyNewsCard()
                } else {
                    latestArticles.forEach { article ->
                        val publishedAt = article.publishedAt ?: article.createdAt ?: ""
                        val dateLabel = try {
                            Format.date(publishedAt)
                        } catch (e: Exception) {
                            publishedAt.substringBefore("T")
                        }

                        NewsCard(
                            category = "Xəbərlər",
                            categoryColor = Civic.Terracotta,
                            date = dateLabel,
                            title = article.title,
                            description = article.summary ?: article.content?.take(150) ?: "",
                            readTime = "2 dəq",
                            onClick = { onArticleClick(article.id) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(26.dp)) }
        item {
            ClosingPanel(
                onLoginClick = onLoginClick,
                onContactClick = onNavigateToContact,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

/* ---------- Hero ---------- */

private val heroStats = listOf(
    "8" to "Modul",
    "5" to "Rol",
    "1" to "Baza",
    "100%" to "Tarixçə"
)

@Composable
private fun HeroPanel(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(Civic.HeroGradient)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Civic.Lime)
                )
                Text(
                    text = "MƏNZİL-TİKİNTİ KOOPERATİVİ",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = Color.White
                )
            }
        }

        Text(
            text = buildAnnotatedString {
                append("Bütün işlər\n")
                withStyle(SpanStyle(color = Civic.Lime)) { append("bir sistemdə.") }
            },
            fontSize = 30.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.7).sp,
            color = Color.White
        )

        Text(
            text = "Binalar, mənzillər, hesablar, maliyyə, inventar və işçi heyəti — " +
                "hamısı bir yerdə. Şəffaf, dəqiq və sadə.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color.White.copy(alpha = 0.72f)
        )

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Civic.TerracottaVivid,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Sistemə daxil ol",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 12.dp)
            ) {
                heroStats.forEachIndexed { index, (value, label) ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = value,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.2).sp,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = label,
                            fontSize = 9.5.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

/* ---------- Modules ---------- */

@Composable
private fun ModuleGrid(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modules.chunked(2).forEach { rowModules ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowModules.forEach { module ->
                    ModuleTile(
                        module = module,
                        number = modules.indexOf(module) + 1,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowModules.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ModuleTile(
    module: Module,
    number: Int,
    modifier: Modifier = Modifier
) {
    val accent = moduleAccent(module.title)

    Card(
        modifier = modifier.defaultMinSize(minHeight = 118.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = moduleIcon(module.title),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = String.format("%02d", number),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Civic.MutedSoft.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = module.title,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = module.text,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = Civic.MutedSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun moduleIcon(title: String): ImageVector = when {
    title.contains("Binalar") -> Icons.Default.Apartment
    title.contains("Müqavilə") -> Icons.Default.Payments
    title.contains("Tranzaksiya") -> Icons.Default.SwapHoriz
    title.contains("Hesabat") -> Icons.Default.Analytics
    title.contains("İnventar") -> Icons.Default.Inventory2
    title.startsWith("HR") -> Icons.Default.Badge
    title.contains("Müraciət") -> Icons.Default.SupportAgent
    else -> Icons.Default.VerifiedUser
}

private fun moduleAccent(title: String): Color = when {
    title.contains("Müqavilə") || title.contains("Tranzaksiya") || title.contains("Hesabat") ->
        Civic.Terracotta
    title.contains("Binalar") || title.contains("İnventar") || title.contains("Müraciət") || title.startsWith("HR") ->
        Civic.Olive
    else -> Civic.Forest
}

/* ---------- Residents ---------- */

@Composable
private fun ResidentsPanel(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Container),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Civic.Terracotta.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "SAKİNLƏR ÜÇÜN",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = Civic.Terracotta,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Text(
                text = "Mənzil sahibisinizsə,\nhər şey əlinizdədir.",
                fontSize = 21.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = Civic.Ink
            )

            Text(
                text = "Şəxsi panelinizə daxil olun, borcunuzu yoxlayın, tarifləri görün və idarəyə müraciət göndərin.",
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Civic.Muted
            )

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Civic.Terracotta,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Şəxsi panelə daxil ol",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                residentFeatures.chunked(2).forEach { rowFeatures ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowFeatures.forEach { feature ->
                            ResidentFeatureTile(
                                title = feature.title,
                                text = feature.text,
                                icon = residentFeatureIcon(feature.title),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                        if (rowFeatures.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResidentFeatureTile(
    title: String,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Civic.Terracotta.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Civic.Terracotta,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Civic.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = Civic.MutedSoft,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun residentFeatureIcon(title: String): ImageVector = when {
    title.contains("Borc") -> Icons.Default.Payments
    title.contains("Hesabat") -> Icons.Default.Analytics
    title.contains("Müraciət") -> Icons.Default.SupportAgent
    else -> Icons.Default.Person
}

/* ---------- Steps ---------- */

@Composable
private fun StepsPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
            steps.forEachIndexed { index, step ->
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = Civic.Border)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Civic.Subtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Civic.Terracotta
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Civic.Ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = step.text,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = Civic.MutedSoft
                        )
                    }
                }
            }
        }
    }
}

/* ---------- News ---------- */

@Composable
private fun EmptyNewsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Civic.Subtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Newspaper,
                    contentDescription = null,
                    tint = Civic.MutedSoft,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column {
                Text(
                    text = "Hələ xəbər yayımlanmayıb",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Civic.Ink
                )
                Text(
                    text = "Yeni elanlar burada görünəcək.",
                    fontSize = 11.5.sp,
                    color = Civic.MutedSoft
                )
            }
        }
    }
}

/* ---------- Closing ---------- */

@Composable
private fun ClosingPanel(
    onLoginClick: () -> Unit,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Civic.HeroGradient)
            .padding(22.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "İdarəetmədə şəffaflıq",
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "İdarəçi tam mənzərəni, mühasib dəqiq rəqəmləri, sakin isə öz borcunu " +
                    "və müraciətinin gedişatını görür.",
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Civic.Forest
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Daxil ol", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onContactClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Bizimlə əlaqə",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}
