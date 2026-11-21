package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.entities.ForeignerResidentEntity
import com.example.data.local.entities.InternalMovementPermitEntity
import com.example.data.local.entities.MofaVisaRequestEntity
import com.example.data.local.entities.PassportSeizureEntity
import com.example.data.local.entities.PortOfficerEntity
import com.example.data.local.entities.RefugeeRecordEntity
import com.example.data.local.entities.SecurityReportEntity
import com.example.data.local.entities.TravelAgencyEntity
import com.example.data.local.entities.UndocumentedMigrantEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.domain.model.SecurityReportType
import com.example.domain.model.SecuritySeverity
import com.example.ui.navigation.ModuleIds
import com.example.ui.viewmodel.ModulesViewModel

private data class SixLabels(
    val l1: String,
    val l2: String,
    val l3: String,
    val l4: String,
    val l5: String,
    val l6: String
)

@Composable
fun ModuleRecordCreateDialog(
    moduleId: String,
    directoryTab: Int,
    currentUser: UserAccountEntity,
    viewModel: ModulesViewModel,
    onDismiss: () -> Unit
) {
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var field3 by remember { mutableStateOf("") }
    var field4 by remember { mutableStateOf("") }
    var field5 by remember { mutableStateOf("") }
    var field6 by remember { mutableStateOf("") }
    var selectedReportType by remember { mutableStateOf(SecurityReportType.WANTED) }
    var selectedSeverity by remember { mutableStateOf(SecuritySeverity.HIGH) }

    val labels = when (moduleId) {
        ModuleIds.SEIZURES -> SixLabels("اسم حامل الجواز", "رقم الجواز", "الجنسية", "سبب الضبط", "رقم المحضر (تلقائي)", "ملاحظات الفحص")
        ModuleIds.MOFA_VISA -> SixLabels("اسم مقدم الطلب", "رقم الجواز", "المنظمة / الجهة", "رقم المذكرة", "رقم الرحلة / الخط", "الغرض")
        ModuleIds.RESIDENTS -> SixLabels("الاسم الكامل", "رقم الجواز", "الجنسية", "رقم الإقامة", "الكفيل", "تاريخ الانتهاء")
        ModuleIds.REFUGEES -> SixLabels("الاسم الكامل", "رقم المفوضية UNHCR", "الجنسية", "عدد أفراد الأسرة", "المخيم / المحافظة", "ملاحظات")
        ModuleIds.MIGRANTS -> SixLabels("الاسم الكامل", "الجنسية المدعاة", "رقم الملف", "موقع الضبط", "حالة الترحيل", "ملاحظات")
        ModuleIds.PERMITS_QR -> SixLabels("اسم الوفد", "المنظمة", "اسم قائد الوفد", "جواز القائد", "من محافظة", "إلى محافظة")
        ModuleIds.SECURITY_REPORTS -> SixLabels("الاسم المطلوب", "رقم الجواز / الهوية", "رقم التعميم", "تاريخ الميلاد", "الجهة المصدرة", "سبب التعميم")
        ModuleIds.AGENCIES_OFFICERS -> if (directoryTab == 0) {
            SixLabels("اسم الوكالة", "رقم الترخيص", "اسم المدير", "رقم الهاتف", "المنفذ", "ملاحظات")
        } else {
            SixLabels("اسم الضابط", "الرقم الوظيفي", "الرتبة", "المنفذ", "الوردية", "الهاتف")
        }
        else -> SixLabels("الحقل 1", "الحقل 2", "الحقل 3", "الحقل 4", "الحقل 5", "الحقل 6")
    }
    val (label1, label2, label3, label4, label5, label6) = labels

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة سجل جديد") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(field1, { field1 = it }, label = { Text(label1) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(field2, { field2 = it }, label = { Text(label2) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(field3, { field3 = it }, label = { Text(label3) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(field4, { field4 = it }, label = { Text(label4) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(field5, { field5 = it }, label = { Text(label5) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(field6, { field6 = it }, label = { Text(label6) }, modifier = Modifier.fillMaxWidth())

                if (moduleId == ModuleIds.SECURITY_REPORTS) {
                    Text("نوع البلاغ:")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(SecurityReportType.WANTED, SecurityReportType.TRAVEL_BAN, SecurityReportType.FORGED_DOC).forEach { rt ->
                            FilterChip(selected = selectedReportType == rt, onClick = { selectedReportType = rt }, label = { Text(rt.arabicLabel) })
                        }
                    }
                    Text("مستوى الخطورة:")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SecuritySeverity.entries.forEach { sev ->
                            FilterChip(selected = selectedSeverity == sev, onClick = { selectedSeverity = sev }, label = { Text(sev.arabicLabel) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val autoCode = (System.currentTimeMillis() % 1_000_000).toString()
                    when (moduleId) {
                        ModuleIds.SEIZURES -> viewModel.saveSeizure(
                            PassportSeizureEntity(
                                seizureNumber = field5.ifBlank { "SZ-$autoCode" },
                                holderName = field1,
                                passportNumber = field2,
                                nationality = field3,
                                seizureReason = field4.ifBlank { "فحص أمني" },
                                portName = currentUser.assignedPort,
                                officerName = currentUser.fullName,
                                forensicNotes = field6
                            ),
                            currentUser
                        )
                        ModuleIds.MOFA_VISA -> viewModel.saveMofaRequest(
                            MofaVisaRequestEntity(
                                applicantName = field1,
                                passportNumber = field2,
                                organizationName = field3.ifBlank { "وزارة الخارجية" },
                                memoNumber = field4.ifBlank { "MOFA-$autoCode" },
                                flightNumber = field5,
                                entryPort = currentUser.assignedPort,
                                purpose = field6
                            ),
                            currentUser
                        )
                        ModuleIds.RESIDENTS -> viewModel.saveResident(
                            ForeignerResidentEntity(
                                fullName = field1,
                                passportNumber = field2,
                                nationality = field3,
                                residencyNumber = field4,
                                sponsorName = field5,
                                expiryDate = field6,
                                portOfEntry = currentUser.assignedPort
                            ),
                            currentUser
                        )
                        ModuleIds.REFUGEES -> viewModel.saveRefugee(
                            RefugeeRecordEntity(
                                fullName = field1,
                                unhcrNumber = field2.ifBlank { "UNHCR-$autoCode" },
                                nationality = field3,
                                familySize = field4.toIntOrNull() ?: 1,
                                campOrGovernorate = field5,
                                arrivalPort = currentUser.assignedPort,
                                vulnerabilityNotes = field6
                            ),
                            currentUser
                        )
                        ModuleIds.MIGRANTS -> viewModel.saveMigrant(
                            UndocumentedMigrantEntity(
                                fullName = field1,
                                claimedNationality = field2,
                                caseNumber = field3.ifBlank { "MIG-$autoCode" },
                                apprehensionLocation = field4,
                                deportationStatus = field5.ifBlank { "قيد الإجراءات" },
                                portOrSector = currentUser.assignedPort,
                                officerName = currentUser.fullName,
                                notes = field6
                            ),
                            currentUser
                        )
                        ModuleIds.PERMITS_QR -> viewModel.savePermit(
                            InternalMovementPermitEntity(
                                permitCode = "PRM-$autoCode",
                                delegationName = field1,
                                organization = field2,
                                leaderName = field3,
                                leaderPassport = field4,
                                originGovernorate = field5,
                                destinationGovernorate = field6,
                                validFrom = "اليوم",
                                validUntil = "+7 أيام",
                                issuedBy = currentUser.fullName
                            ),
                            currentUser
                        )
                        ModuleIds.SECURITY_REPORTS -> viewModel.saveSecurityReport(
                            SecurityReportEntity(
                                subjectName = field1,
                                passportOrIdNumber = field2,
                                circularNumber = field3.ifBlank { "CIR-$autoCode" },
                                birthDate = field4,
                                issuingAuthority = field5.ifBlank { "الإدارة العامة" },
                                reason = field6.ifBlank { "تعميم أمني" },
                                reportType = selectedReportType,
                                severity = selectedSeverity,
                                createdBy = currentUser.fullName
                            ),
                            currentUser
                        )
                        ModuleIds.AGENCIES_OFFICERS -> if (directoryTab == 0) {
                            viewModel.saveAgency(
                                TravelAgencyEntity(
                                    agencyName = field1,
                                    licenseNumber = field2,
                                    managerName = field3,
                                    phone = field4,
                                    portName = field5.ifBlank { currentUser.assignedPort },
                                    notes = field6
                                ),
                                currentUser
                            )
                        } else {
                            viewModel.savePortOfficer(
                                PortOfficerEntity(
                                    fullName = field1,
                                    badgeNumber = field2,
                                    rankTitle = field3,
                                    assignedPort = field4.ifBlank { currentUser.assignedPort },
                                    shiftName = field5.ifBlank { "الوردية الصباحية" },
                                    phone = field6
                                ),
                                currentUser
                            )
                        }
                    }
                    onDismiss()
                },
                enabled = field1.isNotBlank()
            ) { Text("حفظ") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
