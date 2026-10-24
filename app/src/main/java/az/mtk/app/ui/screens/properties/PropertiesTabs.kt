package az.mtk.app.ui.screens.properties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.AppGraph
import az.mtk.app.data.api.BuildingsApi
import az.mtk.app.data.api.BuildingDto
import az.mtk.app.data.api.CreateBuildingRequest
import az.mtk.app.data.api.CreateGarageRequest
import az.mtk.app.data.api.CreatePassiveOwnerRequest
import az.mtk.app.data.api.IdentityApi
import az.mtk.app.data.api.MtkService
import az.mtk.app.data.api.OwnerItemDto
import az.mtk.app.data.api.SearchBody
import az.mtk.app.data.api.UsrRegisterUserRequest
import az.mtk.app.data.auth.Roles
import az.mtk.app.ui.components.AppSheetDialog
import az.mtk.app.ui.theme.Civic
import az.mtk.app.util.toUserMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- binalar

/**
 * Binalar tab — MTK-UI's "Bina siyahısı": ad, ünvan, status və mənzil sayı.
 * Kart toxunuşu bina detallarına (sxem, mənzil tipləri, mənzillər) aparır.
 */
@Composable
internal fun BuildingsContent(
    buildings: List<BuildingDto>,
    apartmentCounts: Map<String, Int>,
    query: String,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBuildingClick: (String) -> Unit,
) {
    val filtered = remember(buildings, query) {
        val term = query.trim().lowercase()
        if (term.isEmpty()) {
            buildings
        } else {
            buildings.filter { building ->
                building.name.lowercase().contains(term) ||
                    building.fullAddress?.lowercase()?.contains(term) == true ||
                    building.description?.lowercase()?.contains(term) == true
            }
        }
    }

    when {
        loading && buildings.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Civic.Terracotta)
        }

        error != null && buildings.isEmpty() -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(error, fontSize = 14.sp, color = Civic.Muted)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) { Text("Yenidən cəhd et") }
        }

        filtered.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (query.isBlank()) "Hələ heç bir bina qeydə alınmayıb." else "Nəticə tapılmadı.",
                fontSize = 14.sp,
                color = Civic.Muted,
            )
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = filtered, key = { it.id }) { building ->
                BuildingCard(
                    building = building,
                    apartmentCount = apartmentCounts[building.id] ?: 0,
                    onClick = { onBuildingClick(building.id) },
                )
            }
        }
    }
}

@Composable
private fun BuildingCard(
    building: BuildingDto,
    apartmentCount: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Civic.Surface),
        border = BorderStroke(1.dp, Civic.Border),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Civic.Forest.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Apartment,
                    null,
                    tint = Civic.Forest,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = building.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Civic.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = building.fullAddress?.takeIf { it.isNotBlank() } ?: "Ünvan göstərilməyib",
                    fontSize = 12.sp,
                    color = Civic.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val active = building.status?.equals("active", ignoreCase = true) != false
                    Surface(
                        shape = CircleShape,
                        color = if (active) Civic.Olive.copy(alpha = 0.12f) else Civic.Subtle,
                    ) {
                        Text(
                            text = building.status?.takeIf { it.isNotBlank() } ?: "—",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Civic.Olive else Civic.MutedSoft,
                        )
                    }
                    Text(
                        text = "$apartmentCount mənzil",
                        fontSize = 11.sp,
                        color = Civic.MutedSoft,
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Civic.MutedSoft.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- yaratma formaları

/** Bina yaratma — MTK-UI CreateBuildingModal ilə eyni sahələr. */
@Composable
internal fun CreateBuildingSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember { AppGraph.service(BuildingsApi::class.java) }

    var name by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canSave = name.isNotBlank() && street.isNotBlank() && city.isNotBlank() && !saving

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { FormTitle("Yeni bina") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormField("Ad", name) { name = it }
                FormField("Küçə", street) { street = it }
                FormField("Şəhər", city) { city = it }
                FormField("Rayon", district) { district = it }
                FormField("Qeyd", description) { description = it }
                error?.let { FormErrorText(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        error = null
                        runCatching {
                            api.createBuilding(
                                CreateBuildingRequest(
                                    name = name.trim(),
                                    street = street.trim(),
                                    city = city.trim(),
                                    district = district.trim().ifBlank { null },
                                    description = description.trim().ifBlank { null },
                                )
                            )
                        }.onSuccess {
                            saving = false
                            onSaved()
                        }.onFailure {
                            saving = false
                            error = it.toUserMessage()
                        }
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) { Text(if (saving) "Saxlanılır…" else "Yarat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Ləğv et", color = Civic.Muted)
            }
        },
        containerColor = Civic.Surface,
    )
}

private val garageTypes = listOf(
    "OpenParking" to "Açıq dayanacaq",
    "CoveredGarage" to "Örtülü qaraj",
    "Storage" to "Anbar yeri",
)

/** Qaraj yaratma — MTK-UI CreateGarageModal ilə eyni sahələr. */
@Composable
internal fun CreateGarageSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val api = remember { AppGraph.service(BuildingsApi::class.java) }
    val service = remember { AppGraph.service(MtkService::class.java) }

    var number by remember { mutableStateOf("") }
    var typeKey by remember { mutableStateOf(garageTypes.first().first) }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedOwner by remember { mutableStateOf<OwnerItemDto?>(null) }
    var ownerQuery by remember { mutableStateOf("") }
    var ownerResults by remember { mutableStateOf<List<OwnerItemDto>>(emptyList()) }
    var ownerLoading by remember { mutableStateOf(false) }

    LaunchedEffect(ownerQuery) {
        if (ownerQuery.isBlank()) {
            ownerResults = emptyList()
            ownerLoading = false
            return@LaunchedEffect
        }
        delay(350)
        ownerLoading = true
        ownerResults = runCatching {
            service.owners(SearchBody(searchTerm = ownerQuery.trim(), pageSize = 8)).data?.items.orEmpty()
        }.getOrDefault(emptyList())
        ownerLoading = false
    }

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { FormTitle("Yeni qaraj") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FormField("Nömrə", number) { number = it }

                Text("Növ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Civic.Muted)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    garageTypes.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (key, label) ->
                                FilterChip(
                                    selected = typeKey == key,
                                    onClick = { typeKey = key },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                FormField("Qeyd", description) { description = it }

                OwnerPicker(
                    selected = selectedOwner,
                    owners = ownerResults,
                    query = ownerQuery,
                    loading = ownerLoading,
                    label = "Sahib (istəyə bağlı)",
                    onQueryChange = { ownerQuery = it },
                    onSelect = { selectedOwner = it },
                )

                error?.let { FormErrorText(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        error = null
                        runCatching {
                            val response = api.createGarage(
                                CreateGarageRequest(
                                    ownerId = selectedOwner?.id,
                                    garageNumber = number.trim(),
                                    garageType = typeKey,
                                    description = description.trim().ifBlank { null },
                                )
                            )
                            if (response.success == false) {
                                error(response.message ?: "Qaraj yaradıla bilmədi.")
                            }
                        }.onSuccess {
                            saving = false
                            if (error == null) onSaved()
                        }.onFailure {
                            saving = false
                            error = it.toUserMessage()
                        }
                    }
                },
                enabled = number.isNotBlank() && !saving,
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) { Text(if (saving) "Saxlanılır…" else "Yarat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Ləğv et", color = Civic.Muted)
            }
        },
        containerColor = Civic.Surface,
    )
}

private enum class OwnerCreateMode { PASSIVE, ACCOUNT }

/**
 * Sakin yaratma — MTK-UI CreateOwnerModal: passiv sahib və ya sistemə daxil
 * ola bilən hesablı sahib (Keycloak istifadəçisi + owner rolu).
 */
@Composable
internal fun CreateOwnerSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val buildingsApi = remember { AppGraph.service(BuildingsApi::class.java) }
    val identityApi = remember { AppGraph.service(IdentityApi::class.java) }

    var mode by remember { mutableStateOf(OwnerCreateMode.PASSIVE) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var accountCreated by remember { mutableStateOf(false) }

    val canSave = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
        !saving && (mode == OwnerCreateMode.ACCOUNT || phone.isNotBlank())

    if (accountCreated) {
        AppSheetDialog(
            onDismissRequest = onSaved,
            title = { FormTitle("Hesablı sahib yaradıldı") },
            text = {
                Text(
                    "İstifadəçi hesabı uğurla yaradıldı. Sahib qeydi arxa planda avtomatik " +
                        "yaranır — siyahıda bir neçə saniyə ərzində görünəcək.",
                    fontSize = 13.sp,
                    color = Civic.Muted,
                )
            },
            confirmButton = {
                Button(
                    onClick = onSaved,
                    colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
                ) { Text("Bağla") }
            },
            containerColor = Civic.Surface,
        )
        return
    }

    AppSheetDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { FormTitle("Yeni sahib") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == OwnerCreateMode.PASSIVE,
                        onClick = { mode = OwnerCreateMode.PASSIVE },
                        label = { Text("Passiv sahib", fontSize = 12.sp) },
                    )
                    FilterChip(
                        selected = mode == OwnerCreateMode.ACCOUNT,
                        onClick = { mode = OwnerCreateMode.ACCOUNT },
                        label = { Text("Hesablı sahib", fontSize = 12.sp) },
                    )
                }

                Text(
                    text = if (mode == OwnerCreateMode.PASSIVE) {
                        "Hesabı olmayan sahib — yalnız məlumat saxlamaq üçün; sistemə daxil ola bilməz."
                    } else {
                        "Sistemə daxil ola bilən istifadəçi hesabı yaradılır və sahib kimi qeydə alınır."
                    },
                    fontSize = 11.sp,
                    color = Civic.MutedSoft,
                )

                FormField("Ad", firstName) { firstName = it }
                FormField("Soyad", lastName) { lastName = it }
                FormField("Telefon", phone) { phone = it }
                FormField("E-poçt", email) { email = it }

                if (mode == OwnerCreateMode.PASSIVE) {
                    FormField("Qeyd", notes) { notes = it }
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Şifrə", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                error?.let { FormErrorText(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        error = null
                        runCatching {
                            if (mode == OwnerCreateMode.PASSIVE) {
                                buildingsApi.createPassiveOwner(
                                    CreatePassiveOwnerRequest(
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        phoneNumber = phone.trim(),
                                        email = email.trim(),
                                        notes = notes.trim().ifBlank { null },
                                    )
                                )
                            } else {
                                val response = identityApi.registerUser(
                                    UsrRegisterUserRequest(
                                        email = email.trim(),
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        password = password,
                                        phoneNumber = phone.trim().ifBlank { null },
                                        roleNames = listOf(Roles.OWNER),
                                    )
                                )
                                check(response.userId != null) {
                                    "İstifadəçi hesabı yaradıla bilmədi."
                                }
                            }
                        }.onSuccess {
                            saving = false
                            if (mode == OwnerCreateMode.PASSIVE) onSaved() else accountCreated = true
                        }.onFailure {
                            saving = false
                            error = it.toUserMessage()
                        }
                    }
                },
                enabled = canSave && (mode == OwnerCreateMode.PASSIVE || password.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = Civic.Terracotta),
            ) { Text(if (saving) "Saxlanılır…" else "Yarat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("Ləğv et", color = Civic.Muted)
            }
        },
        containerColor = Civic.Surface,
    )
}

// ---------------------------------------------------------------- kiçik ortaq formalar

@Composable
private fun FormTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Civic.Ink,
    )
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
}

@Composable
private fun FormErrorText(text: String) {
    Text(text = text, fontSize = 12.sp, color = Civic.Error)
}
