package az.mtk.app.ui.screens.hr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import az.mtk.app.data.api.HrEnums
import az.mtk.app.data.api.HrJobDto
import az.mtk.app.ui.components.AppSheet

/** Yeni işçi forması — bütün bölmələr bir vərəqdə (web `EmployeeFormFields`). */
@Composable
internal fun HrEmployeeFormSheet(
    jobs: List<HrJobDto>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (HrEmployeeForm) -> Unit,
) {
    var form by remember { mutableStateOf(HrEmployeeForm()) }

    AppSheet(onDismissRequest = onDismiss, scrollable = true) {
        HrSheetTitle("Yeni işçi", onDismiss)
        Text(
            text = "Şəxsi, ailə, əlaqə və iş məlumatlarını doldurun.",
            fontSize = 12.sp,
            color = HrColors.MutedSoft,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        FormSection("Şəxsi məlumatlar") {
            HrTextField("Soyad *", form.surname) { form = form.copy(surname = it) }
            HrTextField("Ad *", form.name) { form = form.copy(name = it) }
            HrTextField("Ata adı *", form.fathersName) { form = form.copy(fathersName = it) }
            HrSelectField(
                label = "Cins",
                value = form.gender.toString(),
                options = HrEnums.GENDERS.map { it.value.toString() to it.label },
                onSelect = { form = form.copy(gender = it.toIntOrNull() ?: 1) },
            )
            HrDateField("Doğum tarixi", form.birthDate) { form = form.copy(birthDate = it) }
            HrTextField("Milliyyət", form.nationality) { form = form.copy(nationality = it) }
            HrTextField("FİN kod", form.finCode) { form = form.copy(finCode = it.take(20)) }
            HrTextField("Şəxsiyyət vəsiqəsinin nömrəsi", form.idCardNumber) {
                form = form.copy(idCardNumber = it)
            }
            HrTextField("Sosial sığorta nömrəsi (SSN)", form.socialSecurityNumber) {
                form = form.copy(socialSecurityNumber = it)
            }
        }

        FormSection("Ailə və sosial vəziyyət") {
            HrSelectField(
                label = "Ailə vəziyyəti",
                value = form.maritalStatus,
                options = hrOptionPairs(HrEnums.MARITAL_STATUSES, allLabel = "—"),
                onSelect = { form = form.copy(maritalStatus = it) },
            )
            HrTextField("Uşaqların sayı", form.numberOfChildren, keyboardType = KeyboardType.Number) {
                form = form.copy(numberOfChildren = it.filter(Char::isDigit))
            }
            HrTextField("14 yaşadək uşaqların sayı", form.childrenUnder14Count, keyboardType = KeyboardType.Number) {
                form = form.copy(childrenUnder14Count = it.filter(Char::isDigit))
            }
            HrSelectField(
                label = "Hərbi xidmət",
                value = form.militaryService,
                options = hrOptionPairs(HrEnums.MILITARY_SERVICES, allLabel = "—"),
                onSelect = { form = form.copy(militaryService = it) },
            )
            HrSelectField(
                label = "Təhsil",
                value = form.education,
                options = hrOptionPairs(HrEnums.EDUCATION_LEVELS, allLabel = "—"),
                onSelect = { form = form.copy(education = it) },
            )
            FormCheck("Veteran", form.veteran) { form = form.copy(veteran = it) }
            FormCheck("Əlillik", form.disability) { form = form.copy(disability = it) }
            FormCheck("Qarabağ və azad olunmuş ərazilərdə çalışır", form.isKarabakhWorker) {
                form = form.copy(isKarabakhWorker = it)
            }
            FormCheck("Tək valideyn", form.isSingleParent) { form = form.copy(isSingleParent = it) }
            FormCheck("Əlil uşağı var", form.hasDisabledChild) { form = form.copy(hasDisabledChild = it) }
        }

        FormSection("Əlaqə") {
            HrTextField("Mobil telefon", form.phoneNumber, keyboardType = KeyboardType.Phone) {
                form = form.copy(phoneNumber = it.take(20))
            }
            HrTextField("Ev telefonu", form.homePhoneNumber, keyboardType = KeyboardType.Phone) {
                form = form.copy(homePhoneNumber = it.take(20))
            }
            HrTextField("E-poçt", form.email, keyboardType = KeyboardType.Email) {
                form = form.copy(email = it)
            }
            HrTextField("Qeydiyyat ünvanı", form.registeredAddress) {
                form = form.copy(registeredAddress = it)
            }
            HrTextField("Faktiki ünvan", form.currentAddress) { form = form.copy(currentAddress = it) }
        }

        FormSection("İş məlumatları") {
            HrSelectField(
                label = "Vəzifə *",
                value = form.jobId,
                options = listOf("" to "Seçin…") + jobs.map { it.id to it.name },
                onSelect = { form = form.copy(jobId = it) },
            )
            HrDateField("İşə başlama tarixi *", form.startWorkDate) {
                form = form.copy(startWorkDate = it)
            }
            HrSelectField(
                label = "İş rejimi",
                value = form.employmentType.toString(),
                options = HrEnums.EMPLOYMENT_TYPES.map { it.value.toString() to it.label },
                onSelect = { form = form.copy(employmentType = it.toIntOrNull() ?: 1) },
            )
            HrSelectField(
                label = "İş həftəsi",
                value = form.workingDays,
                options = HrEnums.WORKING_DAYS.map { it.value.toString() to it.label },
                onSelect = { form = form.copy(workingDays = it) },
            )
            HrTextField("İllik əsas məzuniyyət (gün)", form.vacationDays, keyboardType = KeyboardType.Number) {
                form = form.copy(vacationDays = it.filter(Char::isDigit))
            }
        }

        FormSection("Müqavilə və bank") {
            HrTextField("Əmək müqaviləsinin nömrəsi", form.contractNumber) {
                form = form.copy(contractNumber = it)
            }
            HrTextField("Əsas əmək haqqı (₼)", form.baseSalary, keyboardType = KeyboardType.Decimal) {
                form = form.copy(baseSalary = it)
            }
            HrTextField("Maaş bankı", form.salaryBankName) { form = form.copy(salaryBankName = it) }
            HrTextField("Bank hesabı", form.employeeBankAccountNumber) {
                form = form.copy(employeeBankAccountNumber = it)
            }
        }

        error?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = HrColors.Error,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            HrSecondaryButton(text = "Ləğv et", onClick = onDismiss, enabled = !saving)
            HrPrimaryButton(
                text = if (saving) "Saxlanılır…" else "Yarat",
                onClick = { onSave(form) },
                enabled = !saving,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = HrColors.Ink,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        Spacer(Modifier.height(2.dp))
        HrDivider()
    }
}

@Composable
internal fun FormCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = HrColors.Terracotta),
        )
        Text(label, fontSize = 13.sp, color = HrColors.Ink)
    }
}
