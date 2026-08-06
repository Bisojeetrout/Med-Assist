package com.example.swasthya.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.swasthya.GeminiHelper
import com.example.swasthya.MedicineAnalysis
import com.example.swasthya.PrescribedMedicine
import com.example.swasthya.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScanResultBottomSheet(
    analysis: MedicineAnalysis,
    dao: SwasthyaDao,
    onDismiss: () -> Unit,
    onFindCheaperAlternative: (composition: String, dosageForm: String) -> Unit,
    onSaveScan: (MedicineScanEntity) -> Unit
) {
    var dbMatch by remember { mutableStateOf<IndianProductEntity?>(null) }
    var dbComposition by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(true) }

    LaunchedEffect(analysis) {
        val matches = dao.searchProductsByName(analysis.name.take(6))
        dbMatch = matches.firstOrNull()
        dbComposition = dbMatch?.primaryIngredient ?: analysis.name
        isSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Medicine", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Medicine Analysis", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(analysis.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Strength: ${analysis.strength}", fontSize = 14.sp)
                        Text("Form: ${analysis.dosageForm}", fontSize = 14.sp)
                        Text("Manufacturer: ${dbMatch?.manufacturer ?: analysis.manufacturer}", fontSize = 14.sp)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        if (dbMatch != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Matched", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exact Match Found in Database", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Composition: ${dbMatch?.activeIngredients ?: "Unknown"}", fontSize = 14.sp)
                            Text("Pack Size: ${dbMatch?.packSize} ${dbMatch?.packUnit}", fontSize = 14.sp)
                            Text("Price: ₹${dbMatch?.priceInr}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Not Found", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Not found in local database", color = Color(0xFFE65100), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            onFindCheaperAlternative(dbComposition ?: analysis.name, analysis.dosageForm)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Find Alternatives")
                    }
                    Button(
                        onClick = {
                            onSaveScan(
                                MedicineScanEntity(
                                    medicineName = analysis.name,
                                    composition = dbMatch?.primaryIngredient ?: "",
                                    strength = analysis.strength,
                                    dosageForm = analysis.dosageForm,
                                    manufacturer = dbMatch?.manufacturer ?: analysis.manufacturer,
                                    price = dbMatch?.priceInr ?: "N/A",
                                    dosage = "",
                                    frequency = "",
                                    duration = "",
                                    source = "Medicine Box"
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save to History")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionConfirmationDialog(
    extractedMedicines: List<PrescribedMedicine>,
    onSaveToSchedule: (List<MedicineEntity>) -> Unit,
    onSaveToHistory: (List<MedicineScanEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    var editableMedicines by remember { mutableStateOf(extractedMedicines.map { it.copy() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Prescription")
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(editableMedicines.size) { index ->
                    val med = editableMedicines[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = med.medicineName,
                                onValueChange = { newName ->
                                    val newList = editableMedicines.toMutableList()
                                    newList[index] = med.copy(medicineName = newName)
                                    editableMedicines = newList
                                },
                                label = { Text("Medicine Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = med.strength,
                                    onValueChange = { newVal ->
                                        val newList = editableMedicines.toMutableList()
                                        newList[index] = med.copy(strength = newVal)
                                        editableMedicines = newList
                                    },
                                    label = { Text("Strength") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = med.frequency,
                                    onValueChange = { newVal ->
                                        val newList = editableMedicines.toMutableList()
                                        newList[index] = med.copy(frequency = newVal)
                                        editableMedicines = newList
                                    },
                                    label = { Text("Frequency") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Map to MedicineEntity for schedule
                val scheduleEntities = editableMedicines.map {
                    MedicineEntity(
                        name = it.medicineName,
                        dosage = "${it.dose} ${it.dosageForm}",
                        schedule = it.frequency,
                        explanation = it.instructions,
                        timeInMillis = System.currentTimeMillis() + 3600000, // Dummy schedule
                        timeLabel = "08:00 AM",
                        reminderType = "Daily"
                    )
                }
                
                // Map to ScanEntity for history
                val historyEntities = editableMedicines.map {
                    MedicineScanEntity(
                        medicineName = it.medicineName,
                        composition = "",
                        strength = it.strength,
                        dosageForm = it.dosageForm,
                        manufacturer = "",
                        price = "",
                        dosage = it.dose,
                        frequency = it.frequency,
                        duration = it.duration,
                        source = "Prescription"
                    )
                }
                
                onSaveToSchedule(scheduleEntities)
                onSaveToHistory(historyEntities)
                onDismiss()
            }) {
                Text("Save All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
