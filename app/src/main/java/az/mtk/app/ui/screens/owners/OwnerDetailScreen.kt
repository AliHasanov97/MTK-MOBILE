package az.mtk.app.ui.screens.owners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import az.mtk.app.AppGraph
import az.mtk.app.data.api.OwnerDetailDto
import az.mtk.app.ui.components.AvatarCircle
import az.mtk.app.ui.components.EmptyState
import az.mtk.app.ui.components.ErrorState
import az.mtk.app.ui.components.KeyValueHeader
import az.mtk.app.ui.components.ListRow
import az.mtk.app.ui.components.LoadingState
import az.mtk.app.ui.components.MtkCard
import az.mtk.app.ui.components.Pill
import az.mtk.app.ui.components.SimpleTopBar
import az.mtk.app.ui.components.Tone
import az.mtk.app.ui.mtkViewModel
import az.mtk.app.ui.theme.MtkColors
import az.mtk.app.util.Format
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OwnerDetailState(
    val owner: OwnerDetailDto? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class OwnerDetailViewModel(private val ownerId: String) : ViewModel() {
    private val _state = MutableStateFlow(OwnerDetailState())
    val state: StateFlow<OwnerDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { AppGraph.repository.owner(ownerId) }
                .onSuccess { _state.value = OwnerDetailState(owner = it, loading = false) }
                .onFailure { _state.value = OwnerDetailState(loading = false, error = it.toUserMessage()) }
        }
    }
}

@Composable
fun OwnerDetailScreen(
    ownerId: String,
    onBack: () -> Unit,
    onOpenApartment: (String) -> Unit,
    viewModel: OwnerDetailViewModel = mtkViewModel { OwnerDetailViewModel(ownerId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(
            title = state.owner?.fullName ?: "Sahib",
            subtitle = "Sakin məlumatları",
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::load)
                else -> {
                    val owner = state.owner ?: return@Column
                    MtkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(Format.initials(owner.fullName), size = 52.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(owner.fullName ?: "—", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    owner.email ?: "E-poçt yoxdur",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    owner.phoneNumber ?: "Telefon yoxdur",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MtkColors.MutedSoft,
                                )
                            }
                            Pill(
                                if (owner.isActive == false) "Qeyri-aktiv" else "Aktiv",
                                if (owner.isActive == false) Tone.Neutral else Tone.Success,
                            )
                        }
                    }

                    KeyValueHeader("Mənzillər (${owner.apartments?.size ?: 0})")
                    val apartments = owner.apartments.orEmpty()
                    if (apartments.isEmpty()) {
                        EmptyState("Mənzil qeydiyyatı yoxdur")
                    } else {
                        MtkCard(padding = 4.dp) {
                            apartments.forEachIndexed { index, apartment ->
                                ListRow(
                                    title = "Mənzil ${apartment.apartmentNumber ?: "—"}",
                                    subtitle = listOfNotNull(
                                        apartment.building?.name,
                                        apartment.floor?.let { "$it-ci mərtəbə" },
                                        apartment.areaSquareMeters?.let { Format.area(it) },
                                    ).joinToString(" · "),
                                    meta = apartment.roomCount?.let { "$it otaq" },
                                    onClick = { onOpenApartment(apartment.id) },
                                )
                                if (index != apartments.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    KeyValueHeader("Qarajlar (${owner.garages?.size ?: 0})")
                    val garages = owner.garages.orEmpty()
                    if (garages.isEmpty()) {
                        EmptyState("Qaraj qeydiyyatı yoxdur")
                    } else {
                        MtkCard(padding = 4.dp) {
                            garages.forEachIndexed { index, garage ->
                                ListRow(
                                    title = "Qaraj ${garage.garageNumber ?: "—"}",
                                    subtitle = Format.garageType(garage.type),
                                    meta = garage.description,
                                )
                                if (index != garages.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
