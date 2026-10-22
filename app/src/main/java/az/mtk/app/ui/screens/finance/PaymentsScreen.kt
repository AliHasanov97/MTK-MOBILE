package az.mtk.app.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.data.api.FinCreatePaymentRequest
import az.mtk.app.data.api.FinPaymentMethod
import az.mtk.app.data.api.FinPaymentResponse
import az.mtk.app.data.api.FinanceRepository
import az.mtk.app.data.api.OwnerDetailDto
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.finGarageTypeLabel
import az.mtk.app.data.api.finPaymentMethodLabel
import az.mtk.app.data.api.finPaymentStatusLabel
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.MtkModal
import az.mtk.app.ui.components.MtkNumberField
import az.mtk.app.ui.components.MtkOption
import az.mtk.app.ui.components.MtkSelect
import az.mtk.app.ui.components.MtkTextField
import az.mtk.app.ui.components.Pager
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.PrimaryButton
import az.mtk.app.ui.components.SearchField
import az.mtk.app.ui.components.SearchableSelect
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.launch

class PaymentsViewModel : ViewModel() {
    val pager = Pager<FinPaymentResponse>(viewModelScope) { page, query ->
        FinanceRepository.payments(page, search = query)
    }

    fun createPayment(
        ownerId: String,
        apartmentId: String?,
        garageId: String?,
        amount: Double,
        paymentMethod: Int,
        notes: String?,
        onDone: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                FinanceRepository.createPayment(
                    FinCreatePaymentRequest(
                        ownerId = ownerId,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        apartmentId = apartmentId,
                        garageId = garageId,
                    ),
                )
            }.onSuccess {
                pager.refresh()
                onDone(null)
            }.onFailure { onDone(it.toUserMessage()) }
        }
    }
}

@Composable
fun PaymentsScreen(
    onBack: (() -> Unit)?,
    showTopBar: Boolean = false,
    viewModel: PaymentsViewModel = mtkViewModel { PaymentsViewModel() },
) {
    val state by viewModel.pager.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (showTopBar) {
            SimpleTopBar(
                title = "Ödənişlər",
                subtitle = if (state.loading) "Yüklənir…" else "${state.total} ödəniş",
                onBack = onBack,
            )
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            SearchField(
                value = state.query,
                onValueChange = viewModel.pager::onQueryChange,
                placeholder = "Sakin və ya əmlak üzrə…",
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "+ Yeni ödəniş", onClick = { showCreate = true })
        }

        when {
            state.loading && state.items.isEmpty() -> LoadingState("Ödənişlər yüklənir…")
            state.error != null && state.items.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel.pager::refresh)
            state.items.isEmpty() -> EmptyState("Ödəniş tapılmadı")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { payment ->
                    val completed = payment.status == 1
                    MtkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    payment.partyName ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    payment.propertyLabel ?: "Ümumi ödəniş",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Pill(
                                finPaymentStatusLabel(payment.status),
                                if (completed) Tone.Success else Tone.Warning,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Text(
                                    Format.dateTime(payment.paymentDate),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MtkColors.MutedSoft,
                                )
                                Text(
                                    finPaymentMethodLabel(payment.paymentMethod),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Text(
                                Format.money(payment.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                if (state.items.size < state.total) {
                    item {
                        OutlinedButton(
                            onClick = viewModel.pager::loadMore,
                            enabled = !state.loadingMore,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        ) {
                            Text(
                                if (state.loadingMore) "Yüklənir…" else "Daha çox (${state.items.size}/${state.total})",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    item { ErrorState(state.error!!, onRetry = viewModel.pager::refresh) }
                }
            }
        }
    }

    if (showCreate) {
        PaymentCreateModal(
            onDismiss = { showCreate = false },
            onSubmit = { ownerId, apartmentId, garageId, amount, method, notes, onDone ->
                viewModel.createPayment(ownerId, apartmentId, garageId, amount, method, notes, onDone)
            },
        )
    }
}

private sealed interface PayTarget {
    data object General : PayTarget
    data class Apartment(val id: String) : PayTarget
    data class Garage(val id: String) : PayTarget
}

@Composable
private fun PaymentCreateModal(
    onDismiss: () -> Unit,
    onSubmit: (
        ownerId: String,
        apartmentId: String?,
        garageId: String?,
        amount: Double,
        paymentMethod: Int,
        notes: String?,
        onDone: (String?) -> Unit,
    ) -> Unit,
) {
    var selectedOwner by remember { mutableStateOf<OwnerItemDto?>(null) }
    var ownerDetail by remember { mutableStateOf<OwnerDetailDto?>(null) }
    var ownerError by remember { mutableStateOf<String?>(null) }
    var target by remember { mutableStateOf<PayTarget>(PayTarget.General) }
    var method by remember { mutableStateOf(FinPaymentMethod.CASH) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val numericAmount = amount.replace(',', '.').toDoubleOrNull()
    val canSubmit = selectedOwner != null && numericAmount != null && numericAmount > 0

    MtkModal(
        title = "Yeni ödəniş",
        subtitle = "Sahibi seçin, istəsəniz konkret mənzil/qaraja hədəfləyin və ödənişi qeyd edin.",
        onDismiss = onDismiss,
        confirmLabel = if (saving) "Saxlanılır…" else "Ödənişi qeyd et",
        confirmEnabled = canSubmit && !saving,
        onConfirm = {
            val owner = selectedOwner ?: return@MtkModal
            if (!canSubmit || saving) return@MtkModal
            saving = true
            error = null
            onSubmit(
                owner.id,
                (target as? PayTarget.Apartment)?.id,
                (target as? PayTarget.Garage)?.id,
                numericAmount!!,
                method,
                notes.trim().ifBlank { null },
            ) { err ->
                saving = false
                if (err == null) onDismiss() else error = err
            }
        },
    ) {
        SearchableSelect(
            label = "Sahib",
            required = true,
            selectedLabel = selectedOwner?.fullName,
            searchPlaceholder = "Ad, email və ya telefon…",
            onLoad = { query ->
                val page = FinanceRepository.owners(page = 0, search = query)
                page.items
            },
            itemLabel = { it.fullName ?: "—" },
            onSelect = { owner ->
                selectedOwner = owner
                ownerDetail = null
                target = PayTarget.General
                ownerError = null
                scope.launch {
                    runCatching { FinanceRepository.owner(owner.id) }
                        .onSuccess { ownerDetail = it }
                        .onFailure { ownerError = it.toUserMessage() }
                }
            },
            onClear = {
                selectedOwner = null
                ownerDetail = null
                target = PayTarget.General
            },
        )

        if (selectedOwner != null) {
            Spacer(Modifier.height(10.dp))
            if (ownerDetail == null && ownerError == null) {
                Text("Əmlaklar yüklənir…", style = MaterialTheme.typography.bodySmall, color = MtkColors.Muted)
            } else if (ownerError != null) {
                Text(ownerError!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
            } else {
                val detail = ownerDetail
                val targetOptions = buildList {
                    add(MtkOption("general", "Ümumi (konkret mənzil/qaraja bağlı deyil)"))
                    detail?.apartments.orEmpty().forEach { a ->
                        add(MtkOption("a:${a.id}", "Mənzil ${a.apartmentNumber} — ${a.building?.name ?: ""}"))
                    }
                    detail?.garages.orEmpty().forEach { g ->
                        add(MtkOption("g:${g.id}", "Qaraj ${g.garageNumber} (${finGarageTypeLabel(garageTypeOrdinal(g.type))})"))
                    }
                }
                val currentTarget = target
                val targetValue = when (currentTarget) {
                    PayTarget.General -> "general"
                    is PayTarget.Apartment -> "a:${currentTarget.id}"
                    is PayTarget.Garage -> "g:${currentTarget.id}"
                }
                MtkSelect(
                    value = targetValue,
                    onValueChange = { value ->
                        target = when {
                            value == "general" -> PayTarget.General
                            value.startsWith("a:") -> PayTarget.Apartment(value.removePrefix("a:"))
                            else -> PayTarget.Garage(value.removePrefix("g:"))
                        }
                    },
                    label = "Hədəf",
                    options = targetOptions,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        MtkNumberField(
            value = amount,
            onValueChange = { amount = it },
            label = "Məbləğ (₼)",
            required = true,
        )
        Spacer(Modifier.height(10.dp))
        MtkSelect(
            value = method.toString(),
            onValueChange = { method = it.toIntOrNull() ?: FinPaymentMethod.CASH },
            label = "Ödəniş üsulu",
            required = true,
            options = listOf(
                MtkOption(FinPaymentMethod.CASH.toString(), "Nağd"),
                MtkOption(FinPaymentMethod.BANK_TRANSFER.toString(), "Bank köçürməsi"),
                MtkOption(FinPaymentMethod.CARD.toString(), "Kart"),
            ),
        )
        Spacer(Modifier.height(10.dp))
        MtkTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Qeyd (istəyə bağlı)",
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MtkColors.Negative)
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** Garage type string -> ordinal (MTK-UI GARAGE_TYPES sırası). */
private fun garageTypeOrdinal(type: String?): Int = when (type) {
    "OpenParking" -> 0
    "CoveredGarage" -> 1
    "Storage" -> 2
    else -> -1
}
