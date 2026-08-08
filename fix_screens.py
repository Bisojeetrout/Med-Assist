import os

with open('temp_screens.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Apply the changes to change Vitals to Medicines in bottom nav
text = text.replace('icon = { Icon(Icons.Default.Favorite, contentDescription = "Vitals") },', 'icon = { Icon(Icons.Default.Notifications, contentDescription = "Medicines") },')
text = text.replace('label = { Text("Vitals") },', 'label = { Text("Medicines") },')

if 'onShareWithPhysician: () -> Unit' not in text:
    text = text.replace('startWithFoodLog: Boolean = false', 'startWithFoodLog: Boolean = false,\n    onShareWithPhysician: () -> Unit = {},\n    physicians: List<com.example.swasthya.data.PhysicianEntity> = emptyList(),\n    onAddPhysician: (com.example.swasthya.data.PhysicianEntity) -> Unit = {},\n    onDeletePhysician: (com.example.swasthya.data.PhysicianEntity) -> Unit = {}')
    # Update ProfileScreen call to include these
    text = text.replace("""                3 -> ProfileScreen(user, onUpdateUser, onNavigateToProfile, onSignOut)""", """                3 -> ProfileScreen(
                    user = user,
                    onUpdateUser = onUpdateUser,
                    onEditProfile = onNavigateToProfile,
                    onSignOut = onSignOut,
                    physicians = physicians,
                    onAddPhysician = onAddPhysician,
                    onDeletePhysician = onDeletePhysician
                )""")

# Update the when(selectedTab) in DashboardScreen
old_vitals_call = """                1 -> VitalsScreen(
                    vitals = vitals,
                    onAddVitals = { newVitals ->
                        coroutineScope.launch { dao.insertVitals(newVitals) }
                    },
                    onNavigateToScanHistory = onNavigateToScanHistory,
                    onNavigateToGenericExplorer = onNavigateToGenericExplorer
                )"""
new_medicines_call = """                1 -> MedicinesScreen(
                    medicines = medicines,
                    onAddMedicine = onAddMedicine,
                    onUpdateMedicine = onUpdateMedicine,
                    onDeleteMedicine = onDeleteMedicine,
                    onNavigateToScanHistory = onNavigateToScanHistory,
                    onNavigateToGenericExplorer = onNavigateToGenericExplorer,
                    onScanPrescription = {
                        docScanner.getStartScanIntent(context as android.app.Activity)
                            .addOnSuccessListener { intentSender ->
                                docScannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener { e -> android.util.Log.e("Scanner", "Error starting scanner", e) }
                    },
                    onScanMedicine = {
                        pharmaLensScanCameraLauncher.launch(pharmaLensScanPhotoUri)
                    }
                )"""
text = text.replace(old_vitals_call, new_medicines_call)

old_records_call = """                2 -> RecordsScreen(medicines, onAddMedicine, onUpdateMedicine, onDeleteMedicine, reports, onAddReport, onNavigateToReports)"""
new_records_call = """                2 -> RecordsScreen(
                    reports = reports,
                    prescriptions = prescriptions,
                    medicineScans = medicineScans,
                    onAddReport = onAddReport,
                    onDeleteReport = { coroutineScope.launch { dao.deleteReport(it) } },
                    onDeletePrescription = { coroutineScope.launch { dao.deletePrescriptionHistory(it) } },
                    onDeleteMedicineScan = { coroutineScope.launch { dao.deleteMedicineScan(it) } },
                    onShareWithPhysician = onShareWithPhysician
                )"""
text = text.replace(old_records_call, new_records_call)
text = text.replace("""                2 -> RecordsScreen(medicines, onAddMedicine, onUpdateMedicine, onDeleteMedicine, reports, onAddReport, onNavigateToReports, onNavigateToScanHistory)""", new_records_call)

# Add state collections
old_state = """    var selectedTab by remember { mutableStateOf(if (startWithFoodLog) 2 else 0) }
    
    // Health Connect States"""
new_state = """    var selectedTab by remember { mutableStateOf(if (startWithFoodLog) 2 else 0) }
    
    val prescriptions by dao.getAllPrescriptionHistory().collectAsState(initial = emptyList())
    val medicineScans by dao.getAllMedicineScans().collectAsState(initial = emptyList())

    // Health Connect States"""
text = text.replace(old_state, new_state)

# Prescription Processing currentPrescriptionUri
old_rx_processing = """            // Prescription Processing
            LaunchedEffect(pendingPrescriptionFile) {"""
new_rx_processing = """            var currentPrescriptionUri by remember { mutableStateOf("") }
            // Prescription Processing
            LaunchedEffect(pendingPrescriptionFile) {"""
text = text.replace(old_rx_processing, new_rx_processing)

old_rx_result = """                            if (result != null) {
                                prescriptionAnalysisResult = result
                                showPrescriptionDialog = true"""
new_rx_result = """                            if (result != null) {
                                currentPrescriptionUri = file.absolutePath
                                prescriptionAnalysisResult = result
                                showPrescriptionDialog = true"""
text = text.replace(old_rx_result, new_rx_result)

old_rx_flow = """            if (showPrescriptionDialog && prescriptionAnalysisResult != null) {
                com.example.swasthya.ui.screens.PrescriptionScanFlow(
                    analysisResult = prescriptionAnalysisResult!!,
                    dao = dao,
                    onSaveToHistory = { historyEntity ->"""
new_rx_flow = """            if (showPrescriptionDialog && prescriptionAnalysisResult != null) {
                com.example.swasthya.ui.screens.PrescriptionScanFlow(
                    analysisResult = prescriptionAnalysisResult!!,
                    dao = dao,
                    localUri = currentPrescriptionUri,
                    onSaveToHistory = { historyEntity ->"""
text = text.replace(old_rx_flow, new_rx_flow)

old_lens = """            if (showPharmaLensScanDialog && pharmaLensScanAnalysis != null) {
                com.example.swasthya.ui.screens.MedicineScanResultBottomSheet(
                    analysis = pharmaLensScanAnalysis!!,
                    dao = dao,
                    onDismiss = { showPharmaLensScanDialog = false },"""
new_lens = """            if (showPharmaLensScanDialog && pharmaLensScanAnalysis != null) {
                com.example.swasthya.ui.screens.MedicineScanResultBottomSheet(
                    analysis = pharmaLensScanAnalysis!!,
                    dao = dao,
                    localUri = pharmaLensScanPhotoUri.toString(),
                    onDismiss = { showPharmaLensScanDialog = false },"""
text = text.replace(old_lens, new_lens)

# Now read the broken Screens.kt and extract the MedicinesScreen
with open('app/src/main/java/com/example/swasthya/ui/screens/Screens.kt', 'r', encoding='utf-8') as f:
    broken = f.read()

idx = broken.find('@Composable\nfun MedicinesScreen')
if idx != -1:
    medicines_screen_code = broken[idx:]
    text = text + "\n\n" + medicines_screen_code
else:
    print("Warning: MedicinesScreen not found in broken file!")

with open('app/src/main/java/com/example/swasthya/ui/screens/Screens.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Restored and updated!")
