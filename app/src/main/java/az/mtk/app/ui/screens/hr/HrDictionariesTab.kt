package az.mtk.app.ui.screens.hr

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az.mtk.app.data.api.HrEnums
import az.mtk.app.ui.components.AppSheet
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.mtkViewModel

@Composable
internal fun HrDictionariesTab(viewModel: HrDictionariesViewModel = mtkViewModel { HrDictionariesViewModel() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize()) {
        HrPillRow(
            pills = HrDictionarySection.entries.map { section ->
                HrPill(
                    label = section.label,
                    selected = state.section == section,
                    onClick = { viewModel.selectSection(section) },
                )
            },
        )
        Spacer(Modifier.height(8.dp))
        HrSearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "Ad üzrə axtar…",
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        state.actionMessage?.let { HrFeedbackBanner(it, isError = false) { viewModel.clearFeedback() } }
        state.actionError?.let { HrFeedbackBanner(it, isError = true) { viewModel.clearFeedback() } }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            HrPrimaryButton(
                text = if (state.section == HrDictionarySection.JOBS) "+ Yeni vəzifə" else "+ Yeni ocaq",
                onClick = viewModel::openCreate,
            )
        }

        when {
            state.loading && state.jobs.isEmpty() && state.institutions.isEmpty() -> HrLoading()
            state.error != null -> HrErrorState(state.error!!) { viewModel.load() }
            else -> {
                val items: List<Pair<String, String>> = when (state.section) {
                    HrDictionarySection.JOBS -> state.jobs.map { it.id to it.name }
                    HrDictionarySection.INSTITUTIONS -> state.institutions.map {
                        it.id to "${it.name} · ${HrEnums.institutionTypeLabel(it.type)}"
                    }
                }
                if (items.isEmpty()) {
                    HrEmptyState(
                        title = "Siyahı boşdur",
                        text = if (state.query.isBlank()) {
                            "Yeni qeyd əlavə etmək üçün yuxarıdaki düymədən istifadə edin."
                        } else {
                            "Axtarışa uyğun nəticə tapılmadı."
                        },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items, key = { it.first }) { (id, name) ->
                            HrCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        color = HrColors.Ink,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    IconButton(
                                        onClick = {
                                            val type = HrEnums.institutionTypeValue(
                                                state.institutions.firstOrNull { it.id == id }?.type,
                                            )
                                            viewModel.openEdit(id, name.substringBefore(" · "), type)
                                        },
                                    ) {
                                        Icon(Icons.Default.Edit, "Redaktə", tint = HrColors.Muted, modifier = Modifier.height(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.requestDelete(id, name.substringBefore(" · ")) },
                                    ) {
                                        Icon(Icons.Default.Delete, "Sil", tint = HrColors.Error, modifier = Modifier.height(18.dp))
                                    }
                                }
                            }
                        }
                        item {
                            HrPaginationRow(
                                page = state.page,
                                pageCount = state.pageCount,
                                totalCount = state.totalCount,
                                onPage = viewModel::goToPage,
                            )
                        }
                        item { Spacer(Modifier.height(18.dp)) }
                    }
                }
            }
        }
    }

    if (state.formOpen) {
        AppSheet(onDismissRequest = viewModel::closeForm, scrollable = true) {
            HrSheetTitle(
                if (state.editingId == null) "Yeni qeyd" else "Redaktə",
                viewModel::closeForm,
            )
            Spacer(Modifier.height(8.dp))
            HrTextField("Ad", state.formName, onValueChange = viewModel::setFormName)
            if (state.section == HrDictionarySection.INSTITUTIONS) {
                Spacer(Modifier.height(10.dp))
                HrSelectField(
                    label = "Növ",
                    value = state.formType.toString(),
                    options = HrEnums.INSTITUTION_TYPES.map { it.value.toString() to it.label },
                    onSelect = { viewModel.setFormType(it.toIntOrNull() ?: 1) },
                )
            }
            state.formError?.let {
                Text(it, fontSize = 12.sp, color = HrColors.Error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HrSecondaryButton(text = "Ləğv et", onClick = viewModel::closeForm, enabled = !state.saving)
                HrPrimaryButton(
                    text = if (state.saving) "Saxlanılır…" else "Yadda saxla",
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    val deleteName = state.deleteTargetName
    if (deleteName != null) {
        AppSheetDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Silinsin?", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HrColors.Ink) },
            text = { Text("«$deleteName» silinəcək.", fontSize = 13.sp, color = HrColors.Muted) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete, enabled = !state.deleting) {
                    Text(if (state.deleting) "Silinir…" else "Sil", color = HrColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("Ləğv et", color = HrColors.Muted) }
            },
        )
    }
}
