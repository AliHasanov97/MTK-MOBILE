package az.mtk.app.ui.screens.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.ui.components.FeatureCard
import az.mtk.app.ui.components.NewsCard
import az.mtk.app.ui.components.SectionHeader
import az.mtk.app.ui.theme.MtkColors

@Composable
fun HomeScreen(
    repository: az.mtk.app.data.api.LandingRepository,
    onNavigateToLogin: () -> Unit,
    onNavigateToNews: () -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var latestArticles by remember { mutableStateOf<List<az.mtk.app.data.api.ArtArticle>>(emptyList()) }

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
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Hero Section
        item {
            HeroSection(onNavigateToLogin = onNavigateToLogin)
        }

        // Modules Section - All 8 modules in grid
        item {
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MtkColors.Primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "SİSTEMİN İMKANLARI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MtkColors.Primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Səkkiz modul,\nbir ortaq məlumat.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Hər modul digərləri ilə bağlıdır: məlumat bir dəfə daxil edilir, hesablama, maliyyə və hesabatlarda avtomatik görünür.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        // Module grid - 2 columns
        item {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                az.mtk.app.data.modules.chunked(2).forEach { rowModules ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowModules.forEachIndexed { index, module ->
                            val moduleIndex = az.mtk.app.data.modules.indexOf(module)
                            ModuleGridCard(
                                module = module,
                                number = moduleIndex + 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Add spacer if odd number
                        if (rowModules.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Residents Section
        item {
            Spacer(Modifier.height(32.dp))
            ResidentsSection(
                onLoginClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // News Section
        item {
            Spacer(Modifier.height(32.dp))
            SectionHeader(
                title = "Son xəbərlər.",
                actionText = "Bütün xəbərlər",
                onActionClick = onNavigateToNews,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            NewsSection(
                articles = latestArticles,
                onArticleClick = onArticleClick,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Summary Section
        item {
            Spacer(Modifier.height(32.dp))
            SummarySection(
                onActionClick = onNavigateToLogin,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun HeroSection(
    onNavigateToLogin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MtkColors.Primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(16.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Label
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MtkColors.Primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "★ MƏNZİL-TİKİNTİ KOOPERATİVİ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MtkColors.Primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Headline
            Text(
                text = "Bütün işlər\nbir sistemdə.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            // Description
            Text(
                text = "Binalar, mənzillər, kommunal hesablar, maliyyə, inventar və işçi heyəti — hər şey bir yerdə idarə olunur. Şəffaf, dəqiq, sadə.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            // CTA Button
            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MtkColors.Primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sistemə daxil ol",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Image placeholder (if needed)
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MtkColors.SurfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Apartment,
                        contentDescription = "Building",
                        tint = MtkColors.Primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleGridCard(
    module: az.mtk.app.data.Module,
    number: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: icon and number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = module.icon,
                    fontSize = 28.sp,
                    color = MtkColors.Primary
                )
                Text(
                    text = String.format("%02d", number),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }

            // Title
            Text(
                text = module.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Description
            Text(
                text = module.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                fontSize = 11.sp
            )

            Spacer(Modifier.weight(1f))

            // Points
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                module.points.take(2).forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = MtkColors.Primary,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResidentsSection(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MtkColors.Primary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "SAKİNLƏR ÜÇÜN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Mənzil sahibisinizsə,\nhər şey əlinizdədir.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Şəxsi panelinizə daxil olun, borcunuzu yoxlayın və idarəyə müraciət göndərin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onLoginClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MtkColors.Primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Şəxsi panelə daxil ol",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Features grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                az.mtk.app.data.residentFeatures.chunked(2).forEach { rowFeatures ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowFeatures.forEach { feature ->
                            ResidentFeatureCard(
                                feature = feature,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowFeatures.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResidentFeatureCard(
    feature: az.mtk.app.data.ResidentFeature,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = feature.icon,
                fontSize = 24.sp,
                color = MtkColors.Primary
            )
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = feature.text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NewsSection(
    articles: List<az.mtk.app.data.api.ArtArticle>,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (articles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hələ ki xəbər yoxdur",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            articles.forEach { article ->
                val publishedAt = article.publishedAt ?: article.createdAt ?: ""
                val dateLabel = try {
                    az.mtk.app.util.Format.date(publishedAt)
                } catch (e: Exception) {
                    publishedAt.substringBefore("T")
                }

                NewsCard(
                    category = "Xəbərlər",
                    categoryColor = MtkColors.Primary,
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

@Composable
private fun SummarySection(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MtkColors.Primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "İdarəetmədə şəffaflıq",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "İdarəçi tam mənzərəni, mühasib dəqiq rəqəmləri, sakin isə öz borcunu və müraciətinin gedişatını görür.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MtkColors.Primary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daxil ol",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Contact link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Bizimlə əlaqə",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}
