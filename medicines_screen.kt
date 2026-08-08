@Composable
fun MedicinesScreen(
    medicines: List<com.example.swasthya.data.MedicineEntity>,
    onAddMedicine: (com.example.swasthya.data.MedicineEntity) -> Unit,
    onUpdateMedicine: (com.example.swasthya.data.MedicineEntity) -> Unit,
    onDeleteMedicine: (com.example.swasthya.data.MedicineEntity) -> Unit,
    onNavigateToScanHistory: () -> Unit,
    onNavigateToGenericExplorer: () -> Unit,
    onScanPrescription: () -> Unit,
    onScanMedicine: () -> Unit
) {
    var currentView by remember { mutableStateOf("Menu") }

    if (currentView == "Medications") {
        MedicationsScreen(
            medicines = medicines,
            onAddMedicine = onAddMedicine,
            onUpdateMedicine = onUpdateMedicine,
            onDeleteMedicine = onDeleteMedicine,
            onNavigateBack = { currentView = "Menu" }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Medicines", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Manage your medications and prescriptions", fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            // My Medicines / Today's Schedule / Reminders
            Card(
                modifier = Modifier.fillMaxWidth().clickable { currentView = "Medications" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Medications", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My Medicines & Reminders", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Today's schedule and reminders", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scan Medicine (Pharma Lens)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onScanMedicine() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = "Scan Medicine", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan Medicine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("AI analysis of medicine strips", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Scan Prescription
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onScanPrescription() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = "Scan Prescription", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scan Prescription", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Digitize your prescriptions", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Generic/cheaper alternatives
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToGenericExplorer() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Generic Explorer", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Generic Alternatives", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Find cheaper Jan Aushadhi alternatives", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Drug Interaction Shield (Placeholder)
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth().clickable { 
                    android.widget.Toast.makeText(context, "Drug Interaction Shield coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Interaction Shield", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Drug Interaction Shield", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Check for dangerous interactions", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Medicine History
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToScanHistory() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "Medicine History", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medicine History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("View previously scanned medicines", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
