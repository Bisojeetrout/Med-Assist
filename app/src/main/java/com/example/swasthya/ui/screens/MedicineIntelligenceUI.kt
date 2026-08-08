package com.example.swasthya.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.swasthya.PrescriptionAnalysisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineScanResultBottomSheet(
    analysis: MedicineAnalysis,
    dao: SwasthyaDao,
    localUri: String = "",
    onDismiss: () -> Unit,
    onSaveScan: (MedicineScanEntity) -> Unit
) {
    var dbMatchProduct by remember { mutableStateOf<IndianProductEntity?>(null) }
    var dbMatchOneMg by remember { mutableStateOf<OneMgMedicineEntity?>(null) }
    var alternativesJan by remember { mutableStateOf<List<JanAushadhiEntity>>(emptyList()) }
    var alternativesBranded by remember { mutableStateOf<List<IndianProductEntity>>(emptyList()) }
    var isSearching by remember { mutableStateOf(true) }

    LaunchedEffect(analysis) {
        val matchesProduct = dao.searchProductsByName(analysis.name.take(6))
        val matchesOneMg = dao.searchOneMgByName(analysis.name.take(6))
        
        dbMatchProduct = matchesProduct.firstOrNull()
        dbMatchOneMg = matchesOneMg.firstOrNull()
        
        val comp = dbMatchProduct?.primaryIngredient ?: dbMatchOneMg?.saltComposition ?: analysis.genericName
        val form = dbMatchProduct?.dosageForm ?: analysis.dosageForm
        
        if (comp.isNotBlank() && comp != "Unknown") {
            val shortComp = comp.split(" ", "+", ",").firstOrNull { it.length > 3 } ?: comp
            alternativesJan = dao.searchJanAushadhiAlternatives("%$shortComp%").take(1)
            alternativesBranded = dao.searchAlternatives("%$shortComp%", "%$form%").filter { it.brandName != analysis.name }.take(2)
        }
        isSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Medicine", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Medicine Analysis", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isSearching) {
                item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
            } else {
                item {
                    val hasDbMatch = dbMatchProduct != null || dbMatchOneMg != null
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasDbMatch) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Matched", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Database matched • AI explained", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = "AI Generated", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI-generated information • Database match not found", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            val name = dbMatchProduct?.brandName ?: dbMatchOneMg?.name ?: analysis.name
                            val comp = dbMatchProduct?.activeIngredients ?: dbMatchOneMg?.saltComposition ?: analysis.genericName
                            val strength = dbMatchProduct?.primaryStrength ?: analysis.strength
                            val form = dbMatchProduct?.dosageForm ?: analysis.dosageForm
                            val manufacturer = dbMatchProduct?.manufacturer ?: dbMatchOneMg?.manufacturer ?: analysis.manufacturer
                            val price = dbMatchProduct?.priceInr ?: dbMatchOneMg?.mrp
                            
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Generic/Salt: $comp", fontSize = 14.sp)
                            Text("Strength: $strength", fontSize = 14.sp)
                            Text("Form: $form", fontSize = 14.sp)
                            Text("Manufacturer: $manufacturer", fontSize = 14.sp)
                            
                            if (price != null) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Price: ₹$price", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            }
                        }
                    }
                }

                if (alternativesJan.isNotEmpty() || alternativesBranded.isNotEmpty()) {
                    item {
                        Text("Affordable Alternatives", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                        Text("Do not switch prescribed medicines without confirming with your doctor/pharmacist.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    
                    items(alternativesJan) { alt ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PMBJP Generic (Jan Aushadhi)", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(alt.genericName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Price: ₹${alt.mrp} (${alt.unitSize})", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    items(alternativesBranded) { alt ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Branded Alternative", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(alt.brandName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${alt.manufacturer}", fontSize = 12.sp)
                                Text("Price: ₹${alt.priceInr} (${alt.packSize} ${alt.packUnit})", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onSaveScan(
                                MedicineScanEntity(
                                    medicineName = dbMatchProduct?.brandName ?: dbMatchOneMg?.name ?: analysis.name,
                                    composition = dbMatchProduct?.activeIngredients ?: dbMatchOneMg?.saltComposition ?: analysis.genericName,
                                    strength = dbMatchProduct?.primaryStrength ?: analysis.strength,
                                    dosageForm = dbMatchProduct?.dosageForm ?: analysis.dosageForm,
                                    manufacturer = dbMatchProduct?.manufacturer ?: dbMatchOneMg?.manufacturer ?: analysis.manufacturer,
                                    price = dbMatchProduct?.priceInr ?: dbMatchOneMg?.mrp ?: "N/A",
                                    dosage = "",
                                    frequency = "",
                                    duration = "",
                                    source = "Medicine Box",
                                    localUri = localUri
                                )
                            )
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save to History")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScanFlow(
    analysisResult: PrescriptionAnalysisResult,
    dao: SwasthyaDao,
    localUri: String = "",
    onSaveToHistory: (PrescriptionHistoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = "Prescription", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Prescription Summary")
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("General Context", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(analysisResult.context, fontSize = 14.sp)
                            Text("Note: This does not confirm a diagnosis.", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                
                items(analysisResult.medicines.size) { index ->
                    val med = analysisResult.medicines[index]
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    var dbMatchProduct by remember { mutableStateOf<IndianProductEntity?>(null) }
                    var dbMatchOneMg by remember { mutableStateOf<OneMgMedicineEntity?>(null) }
                    
                    LaunchedEffect(med) {
                        val nameSearch = med.medicineName.take(6)
                        if (nameSearch.isNotBlank()) {
                            dbMatchProduct = dao.searchProductsByName(nameSearch).firstOrNull()
                            dbMatchOneMg = dao.searchOneMgByName(nameSearch).firstOrNull()
                        }
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(med.medicineName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand")
                            }
                            
                            val name = dbMatchProduct?.brandName ?: dbMatchOneMg?.name ?: med.medicineName
                            val strength = dbMatchProduct?.primaryStrength ?: med.strength
                            val form = dbMatchProduct?.dosageForm ?: med.dosageForm
                            
                            Text("Strength: $strength | Form: $form", fontSize = 14.sp)
                            Text("Directions: ${med.dose} - ${med.frequency} (${med.duration})", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            
                            if (isExpanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                if (dbMatchProduct != null || dbMatchOneMg != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Matched", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Database matched", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (med.instructions.isNotBlank()) {
                                    Text("Instructions: ${med.instructions}", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isSaving) return@Button
                isSaving = true
                val historyEntity = PrescriptionHistoryEntity(
                    extractedText = "Prescription Scanned",
                    medicinesJson = analysisResult.medicines.joinToString("; ") { "${it.medicineName} (${it.dose}, ${it.frequency})" },
                    analysisContext = analysisResult.context,
                    localUri = localUri
                )
                onSaveToHistory(historyEntity)
                onDismiss()
            }) {
                Text(if (isSaving) "Saving..." else "Save Prescription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
