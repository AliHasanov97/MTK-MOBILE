package az.mtk.app.ui.screens.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.ChargeDto
import az.mtk.app.data.api.OwnershipRecordDto
import az.mtk.app.data.api.PaymentDto
import az.mtk.app.util.Format
import java.util.Locale

// ---------------------------------------------------------------- formatting

/** "46.87" — the same 2-decimal figure the panel prints (dot, not locale comma). */
internal fun formatNumber(value: Double): String = String.format(Locale.US, "%.2f", value)

internal fun money(value: Double?): String = value?.let { "${formatNumber(it)} ₼" } ?: "—"

/**
 * Signed balance, exactly like the panel's formatSigned(): "0 ₼", "+12.00 ₼",
 * "-30.50 ₼" — a debt keeps its minus sign instead of turning into an
 * unsigned amount with a "borc" label.
 */
internal fun signedMoney(value: Double?): String {
    val amount = value ?: 0.0
    if (kotlin.math.abs(amount) < 0.005) return "0 ₼"
    return (if (amount > 0) "+" else "-") + formatNumber(kotlin.math.abs(amount)) + " ₼"
}

/**
 * Spells out how a charge's amount was calculated from the rate snapshot it
 * was created with — mirrors MTK-UI's chargeDescription().
 */
internal fun chargeDescription(charge: ChargeDto): String {
    val period = Format.period(charge.period)
    val area = charge.areaSquareMeters
    val rate = charge.rateAmount

    return when {
        charge.rateType == 0 && area != null && rate != null ->
            "Aylıq mənzil haqqı ($period): ${formatNumber(area)} m² × ${formatNumber(rate)} ₼/m²"

        charge.rateType == 1 && rate != null ->
            "Aylıq qaraj haqqı ($period): sabit ${formatNumber(rate)} ₼"

        else -> charge.description ?: "Haqq ($period)"
    }
}

private fun chargeStatusColor(status: Int?): Color = when (status) {
    2 -> DetailColors.Olive
    1 -> Color(0xFFB26A00)
    3 -> DetailColors.MutedSoft
    else -> DetailColors.Error
}

// ------------------------------------------------------------- hero finance

@Composable
internal fun FinanceStatsSection(
    property: PropertyDetail,
    payments: List<PaymentDto>,
    canPay: Boolean,
    onPay: () -> Unit,
) {
    val balance = property.balance ?: 0.0
    val lastPayment = payments.maxByOrNull { it.paymentDate.orEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Balans + "Ödə" — the web hero's first stat.
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Balans", fontSize = 12.sp, color = DetailColors.Muted)
                Text(
                    text = signedMoney(balance),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        balance < -0.005 -> DetailColors.Error
                        balance > 0.005 -> DetailColors.Olive
                        else -> DetailColors.Ink
                    },
                )
                if (canPay && property.ownerId != null) {
                    OutlinedButton(
                        onClick = onPay,
                        enabled = balance < -0.005,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DetailColors.Terracotta),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DetailColors.Terracotta,
                        ),
                    ) {
                        Text("Ödə", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier.height(72.dp),
                color = DetailColors.Border,
            )

            // Son ödəniş — the web hero's second stat.
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Son ödəniş", fontSize = 12.sp, color = DetailColors.Muted)
                Text(
                    text = Format.dateTime(lastPayment?.paymentDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Ink,
                )
                if (lastPayment?.amount != null) {
                    Text(
                        text = money(lastPayment.amount),
                        fontSize = 11.sp,
                        color = DetailColors.MutedSoft,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------- ownership history

@Composable
internal fun OwnershipHistorySection(
    history: List<OwnershipRecordDto>,
    creators: Map<String, String>,
) {
    SectionCard(
        title = "Mülkiyyət tarixçəsi",
        icon = Icons.Default.History,
    ) {
        if (history.isEmpty()) {
            Text(
                "Hələ mülkiyyət köçürülməsi qeydə alınmayıb.",
                fontSize = 13.sp,
                color = DetailColors.Muted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                history.forEach { record ->
                    OwnershipHistoryCard(
                        record = record,
                        performedBy = creators[record.id],
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnershipHistoryCard(record: OwnershipRecordDto, performedBy: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DetailColors.Subtle,
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Format.dateTime(record.transferDate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Ink,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = record.previousOwnerName ?: "—",
                    fontSize = 13.sp,
                    color = DetailColors.Muted,
                )
                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = DetailColors.Terracotta,
                )
                Text(
                    text = record.newOwnerName ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DetailColors.Ink,
                )
            }

            Text(
                text = "Əməliyyatı icra etdi: ${performedBy ?: "—"}",
                fontSize = 11.sp,
                color = DetailColors.MutedSoft,
            )
        }
    }
}

// -------------------------------------------------- charges and payments

@Composable
internal fun ChargesPaymentsSection(
    charges: List<ChargeDto>,
    onChargeClick: (ChargeDto) -> Unit,
) {
    val sortedCharges = remember(charges) { charges.sortedBy { it.createdAt.orEmpty() } }

    SectionCard(
        title = "Borclar və ödənişlər",
        icon = Icons.Default.AccountBalance,
    ) {
        if (sortedCharges.isEmpty()) {
            Text(
                "Hələ heç bir haqq yaranmayıb.",
                fontSize = 13.sp,
                color = DetailColors.Muted,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sortedCharges.forEach { charge ->
                    // The web's ChargesTable prints the "Əmlak" column whenever the
                    // charge carries an apartment/garage id — same here.
                    ChargeCard(charge, showProperty = true) { onChargeClick(charge) }
                }
            }
        }
    }
}

@Composable
internal fun ChargeCard(
    charge: ChargeDto,
    showProperty: Boolean = false,
    onClick: () -> Unit,
) {
    val statusColor = chargeStatusColor(charge.status)
    val remaining = (charge.amount ?: 0.0) - (charge.paidAmount ?: 0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DetailColors.Surface),
        border = BorderStroke(1.dp, DetailColors.Border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (showProperty && !charge.propertyLabel.isNullOrBlank()) {
                        Text(
                            charge.propertyLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DetailColors.Terracotta,
                        )
                    }
                    Text(
                        chargeDescription(charge),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DetailColors.Ink,
                    )
                    Text(
                        text = Format.dateTime(charge.createdAt),
                        fontSize = 11.sp,
                        color = DetailColors.Muted,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        Format.chargeStatus(charge.status),
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            HorizontalDivider(color = DetailColors.Border)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Məbləğ", fontSize = 10.sp, color = DetailColors.Muted)
                    Text(
                        money(charge.amount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DetailColors.Ink,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Ödənilib", fontSize = 10.sp, color = DetailColors.Muted)
                    Text(
                        money(charge.paidAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if ((charge.paidAmount ?: 0.0) > 0.005) DetailColors.Olive else DetailColors.Muted,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("Qalıq borc", fontSize = 10.sp, color = DetailColors.Muted)
                    Text(
                        money(remaining),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining > 0.005) DetailColors.Error else DetailColors.Olive,
                    )
                }
            }

            Text(
                "Təfərrüat üçün toxunun",
                fontSize = 10.sp,
                color = DetailColors.MutedSoft,
            )
        }
    }
}
