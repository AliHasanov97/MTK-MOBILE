package az.mtk.app.ui.screens.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.OwnershipRecordDto
import az.mtk.app.data.api.PaymentDto
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.graphics.Color

private object ExtColors {
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
    val Forest = Color(0xFF284033)
    val ForestDeep = Color(0xFF1B2D23)
    val Olive = Color(0xFF6F8559)
    val Lime = Color(0xFFD2EBB7)
    val Error = Color(0xFFBA1A1A)
}

@Composable
fun FinanceStatsSection(property: PropertyDetail, payments: List<PaymentDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ExtColors.Surface),
        border = BorderStroke(1.dp, ExtColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Balance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Balans",
                    fontSize = 12.sp,
                    color = ExtColors.Muted,
                )
                val balance = property.balance ?: 0.0
                val balanceColor = when {
                    balance < -0.01 -> ExtColors.Error
                    balance > 0.01 -> ExtColors.Olive
                    else -> ExtColors.Ink
                }
                Text(
                    text = String.format("%.2f ₼", kotlin.math.abs(balance)),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor,
                )
                if (balance < -0.01) {
                    Text(
                        "borc",
                        fontSize = 11.sp,
                        color = ExtColors.Error,
                    )
                } else if (balance > 0.01) {
                    Text(
                        "avans",
                        fontSize = 11.sp,
                        color = ExtColors.Olive,
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(60.dp),
                color = ExtColors.Border,
            )

            // Last payment
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Son ödəniş",
                    fontSize = 12.sp,
                    color = ExtColors.Muted,
                )
                val lastPayment = payments.maxByOrNull { it.paymentDate ?: "" }
                Text(
                    text = lastPayment?.let { formatDate(it.paymentDate) } ?: "—",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ExtColors.Ink,
                )
            }
        }
    }
}

@Composable
fun OwnershipHistorySection(history: List<OwnershipRecordDto>) {
    SectionCard(
        title = "Mülkiyyət tarixçəsi",
        icon = Icons.Default.History,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            history.take(5).forEach { record ->
                OwnershipHistoryCard(record)
                if (record != history.last()) {
                    HorizontalDivider(color = ExtColors.Border)
                }
            }
            if (history.size > 5) {
                Text(
                    "+ ${history.size - 5} daha",
                    fontSize = 12.sp,
                    color = ExtColors.Muted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun OwnershipHistoryCard(record: OwnershipRecordDto) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    record.previousOwnerName ?: "—",
                    fontSize = 13.sp,
                    color = ExtColors.Muted,
                )
                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = ExtColors.Muted,
                )
                Text(
                    record.newOwnerName ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ExtColors.Ink,
                )
            }
            Text(
                formatDate(record.transferDate),
                fontSize = 11.sp,
                color = ExtColors.MutedSoft,
            )
        }
    }
}

@Composable
fun ChargesPaymentsSection(charges: List<ChargeDto>, payments: List<PaymentDto>) {
    var selectedTab by remember { mutableStateOf(0) }

    println("💰 ChargesPaymentsSection: charges=${charges.size}, payments=${payments.size}")

    SectionCard(
        title = "Maliyyə",
        icon = Icons.Default.AccountBalance,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Borclar (${charges.size})") },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Ödənişlər (${payments.size})") },
                    modifier = Modifier.weight(1f),
                )
            }

            // Content
            if (selectedTab == 0 && charges.isNotEmpty()) {
                charges.take(5).forEach { charge ->
                    ChargeCard(charge)
                }
                if (charges.size > 5) {
                    Text(
                        "+ ${charges.size - 5} daha",
                        fontSize = 12.sp,
                        color = ExtColors.Muted,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            } else if (selectedTab == 1 && payments.isNotEmpty()) {
                payments.take(5).forEach { payment ->
                    PaymentCard(payment)
                }
                if (payments.size > 5) {
                    Text(
                        "+ ${payments.size - 5} daha",
                        fontSize = 12.sp,
                        color = ExtColors.Muted,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            } else {
                Text(
                    if (selectedTab == 0) "Borc yoxdur" else "Ödəniş yoxdur",
                    fontSize = 13.sp,
                    color = ExtColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun ChargeCard(charge: ChargeDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ExtColors.Surface),
        border = BorderStroke(1.dp, ExtColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: Description + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        formatChargeDescription(charge),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExtColors.Ink,
                    )
                    Text(
                        formatDate(charge.createdAt),
                        fontSize = 11.sp,
                        color = ExtColors.Muted,
                    )
                }

                val statusColor = when (charge.status) {
                    2 -> ExtColors.Olive // Paid
                    1 -> Color(0xFFFF9800) // PartiallyPaid
                    else -> ExtColors.Error // Unpaid
                }
                val statusText = when (charge.status) {
                    2 -> "Ödənilib"
                    1 -> "Qismən"
                    else -> "Ödənməyib"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        statusText,
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            HorizontalDivider(color = ExtColors.Border)

            // Financial details - 3 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Məbləğ
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Məbləğ",
                        fontSize = 10.sp,
                        color = ExtColors.Muted,
                    )
                    Text(
                        String.format("%.2f ₼", charge.amount ?: 0.0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExtColors.Ink,
                    )
                }

                // Ödənilib
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Ödənilib",
                        fontSize = 10.sp,
                        color = ExtColors.Muted,
                    )
                    val paidAmount = charge.paidAmount ?: 0.0
                    Text(
                        String.format("%.2f ₼", paidAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (paidAmount > 0.01) ExtColors.Olive else ExtColors.Muted,
                    )
                }

                // Qalıq borc
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Qalıq borc",
                        fontSize = 10.sp,
                        color = ExtColors.Muted,
                    )
                    val remaining = (charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0)
                    Text(
                        String.format("%.2f ₼", remaining),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining > 0.01) ExtColors.Error else ExtColors.Olive,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

private fun formatChargeDescription(charge: ChargeDto): String {
    val rateType = charge.rateType
    val period = charge.period ?: "—"

    if (rateType == 0 && charge.areaSquareMeters != null && charge.rateAmount != null) {
        // PerSquareMeter
        return "Aylıq mənzil haqqı ($period): ${charge.areaSquareMeters.toInt()} m² × ${String.format("%.2f", charge.rateAmount)} ₼/m²"
    }
    if (rateType == 1 && charge.rateAmount != null) {
        // FixedGarage
        return "Aylıq qaraj haqqı ($period): sabit ${String.format("%.2f", charge.rateAmount)} ₼"
    }
    // Manual charge
    return charge.description ?: "Haqq ($period)"
}

@Composable
private fun PaymentCard(payment: PaymentDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ExtColors.Surface),
        border = BorderStroke(1.dp, ExtColors.Olive.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (payment.paymentMethod) {
                            0 -> Icons.Default.Money
                            1 -> Icons.Default.AccountBalance
                            2 -> Icons.Default.CreditCard
                            else -> Icons.Default.Payment
                        },
                        null,
                        tint = ExtColors.Olive,
                        modifier = Modifier.size(18.dp),
                    )
                    val method = when (payment.paymentMethod) {
                        0 -> "Nəğd ödəniş"
                        1 -> "Bank köçürməsi"
                        2 -> "Kart ilə"
                        else -> "Ödəniş"
                    }
                    Text(
                        method,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExtColors.Ink,
                    )
                }
                Text(
                    formatDate(payment.paymentDate),
                    fontSize = 11.sp,
                    color = ExtColors.Muted,
                )
                if (!payment.notes.isNullOrBlank()) {
                    Text(
                        payment.notes,
                        fontSize = 11.sp,
                        color = ExtColors.Muted,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    String.format("+%.2f ₼", payment.amount ?: 0.0),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExtColors.Olive,
                )
                val statusText = when (payment.status) {
                    1 -> "Tamamlandı"
                    0 -> "Gözləyir"
                    else -> "—"
                }
                Text(
                    statusText,
                    fontSize = 10.sp,
                    color = ExtColors.Olive,
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "—"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateStr)
        date?.let { outputFormat.format(it) } ?: dateStr
    } catch (e: Exception) {
        dateStr.substringBefore('T')
    }
}
