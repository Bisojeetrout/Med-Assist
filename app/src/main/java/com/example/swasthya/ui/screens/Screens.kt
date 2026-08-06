package com.example.swasthya.ui.screens

import com.example.swasthya.R
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.swasthya.data.MedicineEntity
import com.example.swasthya.data.VitalsEntity
import com.example.swasthya.data.UserEntity
import com.example.swasthya.data.FoodEntity
import com.example.swasthya.data.ReportEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.swasthya.MedicineReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AuthScreen(onNavigateToDashboard: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    
    LaunchedEffect(Unit) {
        if (auth.currentUser != null) {
            onNavigateToDashboard()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.medassist_logo_circle),
            contentDescription = "MedAssist Logo",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("MedAssist", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Your personal health companion", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(64.dp))
        
        val onSignInTrigger = {
            coroutineScope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId("368924275183-7n8svof54eog86otqbdfi6482incj49n.apps.googleusercontent.com")
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(context, request)
                    val credential = result.credential
                    
                    if (credential is androidx.credentials.CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        auth.signInWithCredential(authCredential).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onNavigateToDashboard()
                            } else {
                                Log.e("Auth", "Firebase Auth Failed", task.exception)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Auth", "Google Sign In Failed", e)
                }
            }
        }

        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.btn_google_continue),
            contentDescription = "Continue with Google",
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSignInTrigger() },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DashboardScreen(
    dao: com.example.swasthya.data.SwasthyaDao,
    user: UserEntity? = null,
    onUpdateUser: (UserEntity) -> Unit = {},
    vitals: List<VitalsEntity>,
    medicines: List<MedicineEntity>,
    onAddVitals: (VitalsEntity) -> Unit,
    onAddMedicine: (MedicineEntity) -> Unit,
    onUpdateMedicine: (MedicineEntity) -> Unit = {},
    onDeleteMedicine: (MedicineEntity) -> Unit = {},
    reports: List<ReportEntity> = emptyList(),
    onAddReport: (ReportEntity) -> Unit = {},
    foods: List<FoodEntity> = emptyList(),
    onAddFood: (FoodEntity) -> Unit = {},
    onDeleteFood: (FoodEntity) -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToInsights: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateToGenericExplorer: () -> Unit = {},
    onNavigateToScanHistory: () -> Unit = {},
    onSignOut: () -> Unit = {},
    startWithFoodLog: Boolean = false,
    onShareWithPhysician: () -> Unit = {},
    physicians: List<com.example.swasthya.data.PhysicianEntity> = emptyList(),
    onAddPhysician: (com.example.swasthya.data.PhysicianEntity) -> Unit = {},
    onDeletePhysician: (com.example.swasthya.data.PhysicianEntity) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(if (startWithFoodLog) 2 else 0) }
    
    // Health Connect States
    var steps by remember { mutableStateOf("0") }
    var hr by remember { mutableStateOf("0") }
    var calories by remember { mutableStateOf("0") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val smsSentReceiver = remember {
        object : android.content.BroadcastReceiver() {
            private var lastToastTime = 0L
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                val now = System.currentTimeMillis()
                if (now - lastToastTime < 2000) return
                lastToastTime = now
                
                when (resultCode) {
                    android.app.Activity.RESULT_OK -> {
                        android.widget.Toast.makeText(context, "SMS sent successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        android.widget.Toast.makeText(context, "Generic SMS failure. Check network/SIM settings.", android.widget.Toast.LENGTH_LONG).show()
                    }
                    android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        android.widget.Toast.makeText(context, "No cell service available.", android.widget.Toast.LENGTH_LONG).show()
                    }
                    android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> {
                        android.widget.Toast.makeText(context, "Failed: Null PDU.", android.widget.Toast.LENGTH_LONG).show()
                    }
                    android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        android.widget.Toast.makeText(context, "Radio off (Airplane mode).", android.widget.Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        android.widget.Toast.makeText(context, "SMS failed to send (Error code: $resultCode).", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(context) {
        val filter = android.content.IntentFilter("com.example.swasthya.SMS_SENT")
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            smsSentReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(smsSentReceiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    )

    val requestPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(permissions)) {
            coroutineScope.launch {
                try {
                    val client = HealthConnectClient.getOrCreate(context)
                    val now = Instant.now()
                    val startOfDay = now.truncatedTo(ChronoUnit.DAYS)
                    
                    val stepsResponse = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                    steps = stepsResponse.records.sumOf { it.count }.toString()
                    
                    val hrResponse = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                    if (hrResponse.records.isNotEmpty()) hr = hrResponse.records.last().samples.last().beatsPerMinute.toString()
                    
                    val calResponse = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                    calories = calResponse.records.sumOf { it.energy.inKilocalories }.toLong().toString()
                    
                    com.example.swasthya.FirestoreSync.syncDailyActivity(user?.phone ?: "anonymous", steps, hr, calories)
                    
                    android.widget.Toast.makeText(context, "Sync successful!", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) { Log.e("HealthConnect", "Failed to read data", e) }
            }
        } else {
            android.widget.Toast.makeText(context, "Permissions not fully granted.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    val onSyncRequested: () -> Unit = {
        coroutineScope.launch {
            try {
                val status = HealthConnectClient.getSdkStatus(context)
                if (status == HealthConnectClient.SDK_AVAILABLE) {
                    val client = HealthConnectClient.getOrCreate(context)
                    val granted = client.permissionController.getGrantedPermissions()
                    if (granted.containsAll(permissions)) {
                        val now = Instant.now()
                        val startOfDay = now.truncatedTo(ChronoUnit.DAYS)
                        
                        val stepsResponse = client.readRecords(ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                        steps = stepsResponse.records.sumOf { it.count }.toString()
                        
                        val hrResponse = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                        if (hrResponse.records.isNotEmpty()) hr = hrResponse.records.last().samples.last().beatsPerMinute.toString()
                        
                        val calResponse = client.readRecords(ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeRangeFilter = TimeRangeFilter.between(startOfDay, now)))
                        calories = calResponse.records.sumOf { it.energy.inKilocalories }.toLong().toString()
                        
                        com.example.swasthya.FirestoreSync.syncDailyActivity(user?.phone ?: "anonymous", steps, hr, calories)
                        
                        android.widget.Toast.makeText(context, "Sync successful!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        requestPermissions.launch(permissions)
                    }
                } else if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                    android.widget.Toast.makeText(context, "Health Connect needs an update! (Code: 3)", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Health Connect is unavailable on this device! (Code: 2)", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("HealthConnect", "Sync error", e)
            }
        }
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onSyncRequested()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasSmsPermission) {
            permissionsToRequest.add(android.Manifest.permission.SEND_SMS)
        }

        val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation || !hasCoarseLocation) {
            permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            onSyncRequested()
        }
    }
    
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    var pendingScanReportFile by remember { mutableStateOf<File?>(null) }
    var scanReportNameInput by remember { mutableStateOf("") }
    var isUploadingScanReport by remember { mutableStateOf(false) }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val docScanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val docScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.let { pdf ->
                val uri = pdf.uri
                val fileName = "Scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val outFile = File(context.filesDir, fileName)
                    val outputStream = FileOutputStream(outFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    pendingScanReportFile = outFile
                    scanReportNameInput = ""
                } catch (e: Exception) {
                    Log.e("Scanner", "Error saving scanned file", e)
                }
            } ?: scanResult?.pages?.firstOrNull()?.let { page ->
                val uri = page.imageUri
                val fileName = "Scan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val outFile = File(context.filesDir, fileName)
                    val outputStream = FileOutputStream(outFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    pendingScanReportFile = outFile
                    scanReportNameInput = ""
                } catch (e: Exception) {
                    Log.e("Scanner", "Error saving scanned file", e)
                }
            }
        }
    }

    val docPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = "Upload_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outFile = File(context.filesDir, fileName)
                val outputStream = FileOutputStream(outFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                pendingScanReportFile = outFile
                scanReportNameInput = ""
            } catch (e: Exception) {
                Log.e("PDFPicker", "Error saving PDF file", e)
            }
        }
    }

    var isPharmaLensScannerLoading by remember { mutableStateOf(false) }
    var pharmaLensScanAnalysis by remember { mutableStateOf<com.example.swasthya.MedicineAnalysis?>(null) }
    var showPharmaLensScanDialog by remember { mutableStateOf(false) }
    val pharmaLensScanPhotoFile = remember { File(context.filesDir, "PharmaScan_${System.currentTimeMillis()}.jpg") }
    val pharmaLensScanPhotoUri = remember { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pharmaLensScanPhotoFile) }

    val pharmaLensScanCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            isPharmaLensScannerLoading = true
            coroutineScope.launch {
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, pharmaLensScanPhotoUri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, pharmaLensScanPhotoUri)
                }
                val analysis = com.example.swasthya.GeminiHelper.analyzeMedicine(bitmap)
                pharmaLensScanAnalysis = analysis
                isPharmaLensScannerLoading = false
                if (analysis != null) {
                    showPharmaLensScanDialog = true
                } else {
                    android.widget.Toast.makeText(context, "AI service is temporarily unavailable. Please try again shortly.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var showFoodScanDialog by remember { mutableStateOf(false) }
    var foodScanDesc by remember { mutableStateOf("") }
    var foodScanPhotoUri by remember { mutableStateOf<String?>(null) }
    var isUploadingFoodScan by remember { mutableStateOf(false) }
    var selectedFoodForPopup by remember { mutableStateOf<FoodEntity?>(null) }

    val foodScanPhotoFile = remember { File(context.filesDir, "FoodScan_${System.currentTimeMillis()}.jpg") }
    val foodScanPhotoUriToPass = remember { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", foodScanPhotoFile) }
    val foodScanCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            foodScanPhotoUri = foodScanPhotoFile.absolutePath
        }
    }
    val foodScanGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            val file = File(context.cacheDir, "Gallery_Food_${System.currentTimeMillis()}.jpg")
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            foodScanPhotoUri = file.absolutePath
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Vitals") },
                    label = { Text("Vitals") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Records") },
                    label = { Text("Records") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab != 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isFabMenuExpanded) {
                    // Option 1: PDF Upload Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text("PDF Upload (Reports)", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                docPdfPickerLauncher.launch("application/pdf")
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "PDF Upload", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Option 2: Food Upload
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Calorie Estimation", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                foodScanPhotoUri = null
                                foodScanDesc = ""
                                showFoodScanDialog = true
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Food Upload", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Option 3: Pharma Lens Medicine Check
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Pharma Lens Medicine Check", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                pharmaLensScanCameraLauncher.launch(pharmaLensScanPhotoUri)
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Pharma Lens", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Option 4: Prescription Scan
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Prescription Scan", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        FloatingActionButton(
                            onClick = {
                                isFabMenuExpanded = false
                                docScanner.getStartScanIntent(context as Activity)
                                    .addOnSuccessListener { intentSender ->
                                        docScannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    }
                                    .addOnFailureListener { e -> Log.e("Scanner", "Error starting scanner", e) }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Prescription Scan", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Main FAB (Camera Icon or X Close Icon)
                FloatingActionButton(
                    onClick = {
                        isFabMenuExpanded = !isFabMenuExpanded
                    },
                    containerColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFabMenuExpanded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    if (isFabMenuExpanded) {
                        Icon(Icons.Default.Close, contentDescription = "Close Menu")
                    } else {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_camera),
                            contentDescription = "Camera Options"
                        )
                    }
                }
            }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeScreen(user = user, steps = steps, hr = hr, calories = calories, vitals = vitals, foods = foods, reports = reports, medicines = medicines, onNavigateToInsights = onNavigateToInsights, onShareWithPhysician = onShareWithPhysician, onAddFood = onAddFood, onDeleteFood = onDeleteFood, onNavigateToVitals = { selectedTab = 1 }, onScanPrescription = {
                        docScanner.getStartScanIntent(context as android.app.Activity)
                            .addOnSuccessListener { intentSender ->
                                docScannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener { e -> android.util.Log.e("Scanner", "Error starting scanner", e) }
                    },
                    onScanMedicine = {
                        pharmaLensScanCameraLauncher.launch(pharmaLensScanPhotoUri)
                    },
                    onGenericExplorer = onNavigateToGenericExplorer)
                1 -> VitalsScreen(user, onUpdateUser, vitals, onAddVitals, foods, onAddFood, onDeleteFood = onDeleteFood, onSyncRequested = {})
                2 -> RecordsScreen(medicines, onAddMedicine, onUpdateMedicine, onDeleteMedicine, reports, onAddReport, onNavigateToReports, onNavigateToScanHistory)
                3 -> ProfileScreen(
                    user = user,
                    onUpdateUser = onUpdateUser,
                    onEditProfile = onNavigateToProfile,
                    onSignOut = onSignOut,
                    physicians = physicians,
                    onAddPhysician = onAddPhysician,
                    onDeletePhysician = onDeletePhysician
                )
            }

            // Scrim overlay
            if (isFabMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            isFabMenuExpanded = false
                        }
                )
            }

            // Prescription / Report naming Dialog
            if (pendingScanReportFile != null) {
                AlertDialog(
                    onDismissRequest = { pendingScanReportFile = null },
                    title = { Text("Name Your Report") },
                    text = {
                        Column {
                            Text("What kind of report is this? (e.g. Blood Test, Prescription, X-Ray)")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = scanReportNameInput,
                                onValueChange = { scanReportNameInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Report Name") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val file = pendingScanReportFile!!
                                val finalName = if (scanReportNameInput.isNotBlank()) scanReportNameInput else file.name
                                isUploadingScanReport = true
                                pendingScanReportFile = null
                                coroutineScope.launch {
                                    val aiSummary = try {
                                        com.example.swasthya.GeminiHelper.analyzeMedicalReport(context, file.absolutePath)
                                    } catch(e: Exception) { null }
                                    
                                    val cloudUrl = try {
                                        uploadFileToCloudinary(file.absolutePath)
                                    } catch(e: Exception) { null }
                                    
                                    onAddReport(
                                        ReportEntity(
                                            fileName = finalName,
                                            localUri = file.absolutePath,
                                            cloudUrl = cloudUrl,
                                            reportSummary = aiSummary,
                                            uploadDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
                                        )
                                    )
                                    isUploadingScanReport = false
                                    android.widget.Toast.makeText(context, "Report Saved Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingScanReportFile = null }) { Text("Cancel") }
                    }
                )
            }

            // Food Scan log Dialog
            if (showFoodScanDialog) {
                AlertDialog(
                    onDismissRequest = { showFoodScanDialog = false },
                    title = { Text("Calorie Estimation") },
                    text = {
                        Column {
                            Text("Take a photo or choose from gallery to estimate calories.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { foodScanCameraLauncher.launch(foodScanPhotoUriToPass) }, 
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_camera), contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Camera")
                                }
                                Button(
                                    onClick = { foodScanGalleryLauncher.launch("image/*") }, 
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_gallery), contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gallery")
                                }
                            }
                            if (foodScanPhotoUri != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text("Image selected!", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    TextButton(onClick = { foodScanPhotoUri = null }) {
                                        Text("Clear Image", color = Color.Red)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = foodScanDesc,
                                onValueChange = { foodScanDesc = it },
                                label = { Text("What's in this? (e.g. 2 eggs, toast)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isUploadingFoodScan = true
                                coroutineScope.launch {
                                    var cloudUrl: String? = null
                                    var aiAnalysis: String? = null
                                    var calories: Int? = null
                                    var analysis: com.example.swasthya.FoodAnalysis? = null
                                    if (foodScanPhotoUri != null) {
                                        cloudUrl = uploadFileToCloudinary(foodScanPhotoUri!!)
                                    }
                                    try {
                                        val bitmap = foodScanPhotoUri?.let { android.graphics.BitmapFactory.decodeFile(it) }
                                        analysis = com.example.swasthya.GeminiHelper.analyzeFood(bitmap, foodScanDesc)
                                        if (analysis?.success == true) {
                                            aiAnalysis = """
                                                Dish: ${analysis?.dishName}
                                                Weight: ${analysis?.weightGrams}g
                                                Calories: ${analysis?.calories} kcal
                                                Macros: ${analysis?.carbohydrates}g Carbs | ${analysis?.protein}g Protein | ${analysis?.fats}g Fat
                                                Vitamins: ${analysis?.vitaminsAndMinerals}
                                                Warnings: ${analysis?.deficiencyWarnings}
                                            """.trimIndent()
                                            calories = analysis?.calories
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    val newFood = FoodEntity(
                                        description = foodScanDesc,
                                        photoUri = foodScanPhotoUri,
                                        cloudUrl = cloudUrl,
                                        aiAnalysis = aiAnalysis,
                                        calories = calories,
                                        carbs = analysis?.carbohydrates,
                                        protein = analysis?.protein,
                                        fat = analysis?.fats,
                                        dishName = analysis?.dishName,
                                        weightGrams = analysis?.weightGrams,
                                        micronutrients = analysis?.vitaminsAndMinerals,
                                        deficiencyWarnings = analysis?.deficiencyWarnings
                                    )
                                    onAddFood(newFood)
                                    isUploadingFoodScan = false
                                    showFoodScanDialog = false
                                    selectedFoodForPopup = newFood
                                }
                            },
                            enabled = !isUploadingFoodScan
                        ) {
                            if (isUploadingFoodScan) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...")
                            } else {
                                Text("Save")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFoodScanDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Food Scan Result Pop-up (Immediate feedback)
            if (selectedFoodForPopup != null) {
                AlertDialog(
                    onDismissRequest = { selectedFoodForPopup = null },
                    title = { Text(selectedFoodForPopup!!.description.ifBlank { "Logged Meal" }) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            if (selectedFoodForPopup!!.photoUri != null) {
                                coil.compose.AsyncImage(
                                    model = selectedFoodForPopup!!.photoUri,
                                    contentDescription = "Food Image",
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (selectedFoodForPopup!!.aiAnalysis != null) {
                                Text(
                                    text = "AI Analysis:\n${selectedFoodForPopup!!.aiAnalysis}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text("Logged: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(selectedFoodForPopup!!.timestamp))}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedFoodForPopup = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Medicine Scan Result
            if (showPharmaLensScanDialog && pharmaLensScanAnalysis != null) {
                com.example.swasthya.ui.screens.MedicineScanResultBottomSheet(
                    analysis = pharmaLensScanAnalysis!!,
                    dao = dao,
                    onDismiss = { showPharmaLensScanDialog = false },
                    onFindCheaperAlternative = { comp, group ->
                        showPharmaLensScanDialog = false
                        onNavigateToGenericExplorer()
                    },
                    onSaveScan = { scanEntity ->
                        coroutineScope.launch {
                            dao.insertMedicineScan(scanEntity)
                            android.widget.Toast.makeText(context, "Saved to Scan History", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showPharmaLensScanDialog = false
                    }
                )
            }

            // AI Processing Overlay
            if (isUploadingScanReport || isPharmaLensScannerLoading) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("AI Processing, please wait...")
                        }
                    }
                )
            }
        }
    }
}

suspend fun uploadFileToCloudinary(filePath: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val file = java.io.File(filePath)
        val cloudName = "sx0xwr9g"
        val uploadPreset = "swasthya_preset"
        val url = java.net.URL("https://api.cloudinary.com/v1_1/$cloudName/auto/upload")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.doOutput = true
        connection.requestMethod = "POST"
        val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        
        val outputStream = java.io.DataOutputStream(connection.outputStream)
        
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
        outputStream.writeBytes("$uploadPreset\r\n")
        
        outputStream.writeBytes("--$boundary\r\n")
        outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
        
        val mimeType = if (file.name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
        outputStream.writeBytes("Content-Type: $mimeType\r\n\r\n")
        
        val fileInputStream = java.io.FileInputStream(file)
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        fileInputStream.close()
        outputStream.writeBytes("\r\n")
        
        outputStream.writeBytes("--$boundary--\r\n")
        outputStream.flush()
        outputStream.close()
        
        val responseCode = connection.responseCode
        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = org.json.JSONObject(response)
            val secureUrl = jsonObject.getString("secure_url")
            return@withContext secureUrl
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}

@Composable
fun HomeScreen(
    user: UserEntity? = null,
    steps: String, 
    hr: String, 
    calories: String, 
    vitals: List<VitalsEntity>, 
    foods: List<FoodEntity>,
    reports: List<ReportEntity> = emptyList(),
    medicines: List<MedicineEntity> = emptyList(),
    onNavigateToInsights: (String, String, String) -> Unit = { _, _, _ -> },
    onShareWithPhysician: () -> Unit = {},
    onAddFood: (FoodEntity) -> Unit = {},
    onDeleteFood: (FoodEntity) -> Unit = {},
    onNavigateToVitals: () -> Unit = {},
    onScanPrescription: () -> Unit = {},
    onScanMedicine: () -> Unit = {},
    onGenericExplorer: () -> Unit = {}
) {
    var currentView by remember { mutableStateOf("Home") }

    var drugInteractionResult by remember { mutableStateOf<com.example.swasthya.DrugInteractionResult?>(null) }
    var isLoadingInteractions by remember { mutableStateOf(false) }
    var showInteractionDetailDialog by remember { mutableStateOf(false) }

    LaunchedEffect(medicines) {
        val medicineNames = medicines.mapNotNull { it.name }.filter { it.isNotBlank() }
        if (medicineNames.size >= 2) {
            isLoadingInteractions = true
            try {
                val result = com.example.swasthya.GeminiHelper.checkDrugInteractions(medicineNames)
                drugInteractionResult = result
            } catch (e: Exception) {
                drugInteractionResult = com.example.swasthya.DrugInteractionResult(
                    hasInteraction = false,
                    description = "Unable to complete interaction screening: ${e.message}",
                    interactedDrugs = emptyList()
                )
            } finally {
                isLoadingInteractions = false
            }
        } else {
            drugInteractionResult = null
        }
    }

    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val caloriesConsumed = foods.filter { it.timestamp >= todayStart }.sumOf { it.calories ?: 0 }

        var showQuickSummaryDialog by remember { mutableStateOf(false) }
        var quickSummaryText by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()
        var showSosDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current

        val triggerCall = {
            val isEmergencyServices = user?.sosContactPreference == "112" || user?.sosContactPreference == "911"
            val contact = if (isEmergencyServices) "112" else user?.emergencyContactPhone?.takeIf { it.isNotBlank() } ?: "112"
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$contact"))
            context.startActivity(intent)
        }

        val triggerText = {
            val isEmergencyServices = user?.sosContactPreference == "112" || user?.sosContactPreference == "911"
            val contact = if (isEmergencyServices) "112" else user?.emergencyContactPhone?.takeIf { it.isNotBlank() } ?: "112"
            
            val cleanContact = contact.filter { it.isDigit() || it == '+' }
            val defaultMsg = "EMERGENCY! I need help. Blood Type: ${user?.bloodGroup ?: "Unknown"}, Conditions: ${user?.disease ?: "None"}. Please contact me."
            val baseMessage = if (user?.customSosMessage.isNullOrBlank()) defaultMsg else user!!.customSosMessage

            val sendSmsWithMessage: (String) -> Unit = { messageBody ->
                val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.SEND_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    try {
                        val subscriptionId = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                            android.telephony.SubscriptionManager.getDefaultSmsSubscriptionId()
                        } else {
                            android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
                        }

                        val smsManager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                            val systemSmsManager = context.getSystemService(android.telephony.SmsManager::class.java)
                            if (subscriptionId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                                systemSmsManager?.createForSubscriptionId(subscriptionId) ?: systemSmsManager
                            } else {
                                systemSmsManager
                            } ?: @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()
                        } else {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1 &&
                                subscriptionId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                                @Suppress("DEPRECATION")
                                android.telephony.SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                            } else {
                                @Suppress("DEPRECATION")
                                android.telephony.SmsManager.getDefault()
                            }
                        }

                        val SENT_ACTION = "com.example.swasthya.SMS_SENT"
                        val sentIntent = android.app.PendingIntent.getBroadcast(
                            context,
                            0,
                            android.content.Intent(SENT_ACTION),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )

                        val parts = smsManager.divideMessage(messageBody)
                        val sentIntents = ArrayList<android.app.PendingIntent>()
                        for (i in 0 until parts.size) {
                            sentIntents.add(sentIntent)
                        }

                        smsManager.sendMultipartTextMessage(cleanContact, null, parts, sentIntents, null)
                        android.widget.Toast.makeText(context, "Sending SOS Message...", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Auto SMS failed. Opening SMS app.", android.widget.Toast.LENGTH_LONG).show()
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("smsto:${android.net.Uri.encode(cleanContact)}")
                            putExtra("sms_body", messageBody)
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                } else {
                    android.widget.Toast.makeText(context, "SMS Permission not granted. Opening SMS app.", android.widget.Toast.LENGTH_SHORT).show()
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("smsto:${android.net.Uri.encode(cleanContact)}")
                        putExtra("sms_body", messageBody)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }

            val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFineLocation || hasCoarseLocation) {
                android.widget.Toast.makeText(context, "Fetching location for SOS...", android.widget.Toast.LENGTH_SHORT).show()
                try {
                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
                    
                    var locationSent = false
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    
                    val fallbackRunnable = Runnable {
                        if (!locationSent) {
                            locationSent = true
                            sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
                        }
                    }
                    
                    // Set a strict 4-second timeout to fetch GPS location
                    handler.postDelayed(fallbackRunnable, 4000)

                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location: android.location.Location? ->
                        if (!locationSent) {
                            locationSent = true
                            handler.removeCallbacks(fallbackRunnable)
                            if (location != null) {
                                val mapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                                sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: $mapsLink")
                            } else {
                                // Try last known location
                                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: android.location.Location? ->
                                    if (lastLoc != null) {
                                        val mapsLink = "https://maps.google.com/?q=${lastLoc.latitude},${lastLoc.longitude}"
                                        sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: $mapsLink")
                                    } else {
                                        sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
                                    }
                                }.addOnFailureListener {
                                    sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
                                }
                            }
                        }
                    }.addOnFailureListener {
                        if (!locationSent) {
                            locationSent = true
                            handler.removeCallbacks(fallbackRunnable)
                            // Try last known location
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: android.location.Location? ->
                                if (lastLoc != null) {
                                    val mapsLink = "https://maps.google.com/?q=${lastLoc.latitude},${lastLoc.longitude}"
                                    sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: $mapsLink")
                                } else {
                                    sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
                                }
                            }.addOnFailureListener {
                                sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location permission denied")
                } catch (e: Exception) {
                    sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
                }
            } else {
                android.widget.Toast.makeText(context, "Location permission not granted.", android.widget.Toast.LENGTH_SHORT).show()
                sendSmsWithMessage("$baseMessage\n\n🚨 EMERGENCY SOS! I need immediate help. My current location: Location unavailable")
            }
        }

        if (showInteractionDetailDialog && drugInteractionResult != null) {
            val result = drugInteractionResult!!
            AlertDialog(
                onDismissRequest = { showInteractionDetailDialog = false },
                title = {
                    Text(
                        text = if (result.hasInteraction) "⚠️ Potential Drug Interaction" else "✅ Medication Compatibility",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (result.hasInteraction) "Risk Summary:" else "Status Summary:",
                            fontWeight = FontWeight.Bold
                        )
                        Text(result.description)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Disclaimer: This check is for informational purposes only and is powered by Google Gemini. Please consult your physician or pharmacist to verify your medication schedule.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showInteractionDetailDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showSosDialog) {
            AlertDialog(
                onDismissRequest = { showSosDialog = false },
                title = { Text("Emergency Action") },
                text = { Text("What do you want to do?") },
                confirmButton = {
                    Button(
                        onClick = { showSosDialog = false; triggerCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Call") }
                },
                dismissButton = {
                    Button(
                        onClick = { showSosDialog = false; triggerText() },
                    ) { Text("Text (SMS)") }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good morning,", fontSize = 16.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user?.name?.takeIf { it.isNotBlank() } ?: "User", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👋", fontSize = 24.sp)
                    }
                    Text("Let's take care of your health today.", fontSize = 14.sp, color = Color.Gray)
                }
                IconButton(onClick = { /* Notifications */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }

            // Emergency Alert
            Button(
                onClick = { 
                    when (user?.sosActionPreference) {
                        "Call" -> triggerCall()
                        "Text" -> triggerText()
                        else -> showSosDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Emergency", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("EMERGENCY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // Today's Medicines
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💊 Today's Medicines", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val todaysMeds = medicines.filter { !it.isTaken }
                    if (todaysMeds.isEmpty()) {
                        Text("No medicines added – Scan Prescription", color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        todaysMeds.take(3).forEach { med ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(med.name ?: "Unknown Medicine", fontWeight = FontWeight.Bold)
                                    Text("${med.dosage} - ${med.timeLabel}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Text("Not Taken", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            if (med != todaysMeds.take(3).last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            // Medication Safety (Compact)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showInteractionDetailDialog = true },
                colors = CardDefaults.cardColors(containerColor = if (drugInteractionResult?.hasInteraction == true) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val tintColor = if (drugInteractionResult?.hasInteraction == true) Color.Red else Color(0xFF2E7D32)
                    Icon(
                        if (drugInteractionResult?.hasInteraction == true) Icons.Default.Warning else Icons.Default.CheckCircle, 
                        contentDescription = "Safety", 
                        tint = tintColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medication Safety", fontWeight = FontWeight.Bold, color = tintColor)
                        if (isLoadingInteractions) {
                            Text("Analyzing active medications...", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Text(
                                if (drugInteractionResult?.hasInteraction == true) "Interaction alerts require attention!" else "No interaction alerts", 
                                fontSize = 12.sp, 
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Quick Actions
            Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanPrescription,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("Scan\nPrescription", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
                    }
                }
                
                Button(
                    onClick = onScanMedicine,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("Scan\nMedicine", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
                    }
                }
                
                Button(
                    onClick = onGenericExplorer,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                        Text("Generic\nExplorer", fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp)
                    }
                }
            }

            // AI Health Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "AI", tint = Color(0xFF7B1FA2))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Health Summary", fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (quickSummaryText == null) {
                        LaunchedEffect(Unit) {
                            val todayStartCalendar = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val summary = com.example.swasthya.GeminiHelper.getQuickSummary(
                                steps, hr, calories, foods.count { it.timestamp >= todayStartCalendar }, reports, medicines
                            )
                            quickSummaryText = summary
                        }
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                    } else {
                        Text(quickSummaryText!!, fontSize = 14.sp, color = Color.DarkGray)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { onNavigateToInsights(steps, hr, calories) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("View Full Insights", fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                    }
                }
            }

            // Compact Today's Health
            Text("Today's Health", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Steps Card
                Card(modifier = Modifier.weight(1f).aspectRatio(0.9f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👟", fontSize = 24.sp)
                        Text("Steps", fontSize = 12.sp, color = Color.Gray)
                        Text(steps, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1565C0))
                    }
                }
                // HR Card
                Card(modifier = Modifier.weight(1f).aspectRatio(0.9f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❤️", fontSize = 24.sp)
                        Text("HR", fontSize = 12.sp, color = Color.Gray)
                        Text(hr, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFC62828))
                    }
                }
                // Calories Card
                Card(modifier = Modifier.weight(1f).aspectRatio(0.9f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6))) {
                    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 24.sp)
                        Text("Calories", fontSize = 12.sp, color = Color.Gray)
                        Text(calories, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFEF6C00))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

@Composable
fun FoodLogScreen(
    foods: List<FoodEntity>,
    onAddFood: (FoodEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var selectedFoodForPopup by remember { mutableStateOf<FoodEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Food Log", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        Button(
            onClick = { showAddFoodDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Log Food")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log New Meal")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        foods.forEach { food ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { selectedFoodForPopup = food },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (food.photoUri != null) {
                        Icon(Icons.Default.Search, contentDescription = "Photo attached", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(food.description.ifBlank { "Logged Meal" }, fontWeight = FontWeight.Bold)
                        Text("Logged: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date(food.timestamp))}", fontSize = 12.sp)
                    }
                }
            }
        }
        if (foods.isEmpty()) {
            Text("No meals logged yet.", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
    
    if (selectedFoodForPopup != null) {
        AlertDialog(
            onDismissRequest = { selectedFoodForPopup = null },
            title = { Text(selectedFoodForPopup!!.description.ifBlank { "Logged Meal" }) },
            text = {
                Column {
                    if (selectedFoodForPopup!!.photoUri != null) {
                        coil.compose.AsyncImage(
                            model = selectedFoodForPopup!!.photoUri,
                            contentDescription = "Food Image",
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (selectedFoodForPopup!!.aiAnalysis != null) {
                        Text(
                            text = "AI Analysis:\n${selectedFoodForPopup!!.aiAnalysis}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("Logged: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date(selectedFoodForPopup!!.timestamp))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFoodForPopup = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddFoodDialog) {
        var foodDesc by remember { mutableStateOf("") }
        var foodPhotoUri by remember { mutableStateOf<String?>(null) }
        var isUploadingFood by remember { mutableStateOf(false) }
        val foodPhotoFile = remember { java.io.File(context.filesDir, "Food_${System.currentTimeMillis()}.jpg") }
        val foodPhotoUriToPass = remember { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", foodPhotoFile) }
        val foodCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                foodPhotoUri = foodPhotoFile.absolutePath
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAddFoodDialog = false },
            title = { Text("Log Food") },
            text = {
                Column {
                    Text("Take a photo and describe the ingredients for AI calorie estimation.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { foodCameraLauncher.launch(foodPhotoUriToPass) }, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (foodPhotoUri == null) "Take Photo (Required)" else "Photo Captured!")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = foodDesc,
                        onValueChange = { foodDesc = it },
                        label = { Text("What's in this? (e.g. 2 eggs, toast)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUploadingFood = true
                        coroutineScope.launch {
                            var cloudUrl: String? = null
                            var aiAnalysis: String? = null
                            var calories: Int? = null
                            var analysis: com.example.swasthya.FoodAnalysis? = null
                            if (foodPhotoUri != null) {
                                cloudUrl = uploadFileToCloudinary(foodPhotoUri!!)
                            }
                            try {
                                val bitmap = foodPhotoUri?.let { android.graphics.BitmapFactory.decodeFile(it) }
                                analysis = com.example.swasthya.GeminiHelper.analyzeFood(bitmap, foodDesc)
                                if (analysis?.success == true) {
                                    aiAnalysis = """
                                        Dish: ${analysis?.dishName}
                                        Weight: ${analysis?.weightGrams}g
                                        Calories: ${analysis?.calories} kcal
                                        Macros: ${analysis?.carbohydrates}g Carbs | ${analysis?.protein}g Protein | ${analysis?.fats}g Fat
                                        Vitamins: ${analysis?.vitaminsAndMinerals}
                                        Warnings: ${analysis?.deficiencyWarnings}
                                    """.trimIndent()
                                    calories = analysis?.calories
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            onAddFood(FoodEntity(
                                description = foodDesc,
                                photoUri = foodPhotoUri,
                                cloudUrl = cloudUrl,
                                aiAnalysis = aiAnalysis,
                                calories = calories,
                                carbs = analysis?.carbohydrates,
                                protein = analysis?.protein,
                                fat = analysis?.fats,
                                dishName = analysis?.dishName,
                                weightGrams = analysis?.weightGrams,
                                micronutrients = analysis?.vitaminsAndMinerals,
                                deficiencyWarnings = analysis?.deficiencyWarnings
                            ))
                            isUploadingFood = false
                            showAddFoodDialog = false
                        }
                    },
                    enabled = !isUploadingFood
                ) {
                    if (isUploadingFood) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading...")
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFoodDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun VitalsScreen(
    user: UserEntity?,
    onUpdateUser: (UserEntity) -> Unit,
    vitals: List<VitalsEntity>,
    onAddVitals: (VitalsEntity) -> Unit,
    foods: List<FoodEntity>,
    onAddFood: (FoodEntity) -> Unit,
    onDeleteFood: (FoodEntity) -> Unit = {},
    onSyncRequested: () -> Unit
) {
    var selectedMood by remember { mutableStateOf("") }
    var painLevel by remember { mutableFloatStateOf(1f) }
    var selectedEnergy by remember { mutableStateOf("") }
    var sleepDuration by remember { mutableStateOf("") }
    
    val symptomsOptions = listOf("Fever", "Cough", "Headache", "Nausea", "Fatigue", "Body Ache")
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }

    val moodOptions = listOf("Happy", "Neutral", "Sad", "Anxious", "Angry")
    val energyOptions = listOf("High", "Medium", "Low")
    var showAIPopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Daily Check-in Form
        Text("Daily Check-in", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Track your mental and physical state", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Mood", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moodOptions.forEach { mood ->
                val isSelected = selectedMood == mood
                OutlinedButton(
                    onClick = { selectedMood = mood },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Text(mood)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Pain Level (1-10): ${painLevel.toInt()}", fontWeight = FontWeight.Bold)
        Slider(
            value = painLevel,
            onValueChange = { painLevel = it },
            valueRange = 1f..10f,
            steps = 8
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Energy Level", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            energyOptions.forEach { energy ->
                val isSelected = selectedEnergy == energy
                OutlinedButton(
                    onClick = { selectedEnergy = energy },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(energy)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = sleepDuration,
            onValueChange = { sleepDuration = it },
            label = { Text("Sleep Duration (hours)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Symptoms", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            symptomsOptions.chunked(2).forEach { rowOptions ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowOptions.forEach { symptom ->
                        val isSelected = selectedSymptoms.contains(symptom)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedSymptoms = if (checked) selectedSymptoms + symptom else selectedSymptoms - symptom
                                }
                            )
                            Text(symptom, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Additional Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                onAddVitals(
                    VitalsEntity(
                        date = currentDate,
                        mood = selectedMood,
                        painLevel = painLevel.toInt(),
                        energyLevel = selectedEnergy,
                        sleepDuration = sleepDuration,
                        symptoms = selectedSymptoms.joinToString(", "),
                        notes = notes
                    )
                )
                // Clear fields after log
                selectedMood = ""
                painLevel = 1f
                selectedEnergy = ""
                sleepDuration = ""
                selectedSymptoms = setOf()
                notes = ""
                showAIPopup = true
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Log Vitals (Local Check-in)")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Smart Watch Integration
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = "Watch", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sync with Smartwatch", fontWeight = FontWeight.Bold)
                    Text("Pull data to your Dashboard", fontSize = 12.sp)
                }
                Button(onClick = { onSyncRequested() }) { Text("Sync") }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        CalorieTrackerSection(user, onUpdateUser, foods, onAddFood, onDeleteFood)
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAIPopup) {
        AlertDialog(
            onDismissRequest = { showAIPopup = false },
            title = { Text("Given to AI to analyze (Test)") },
            text = { Text("Your daily check-in has been stored locally. In the future, this data will be automatically sent to the AI for analysis.") },
            confirmButton = {
                Button(onClick = { showAIPopup = false }) {
                    Text("OK")
                }
            }
        )
    }
}
@Composable
fun CalorieTrackerSection(
    user: UserEntity?,
    onUpdateUser: (UserEntity) -> Unit,
    foods: List<FoodEntity>,
    onAddFood: (FoodEntity) -> Unit,
    onDeleteFood: (FoodEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // UI states
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showEditLimitDialog by remember { mutableStateOf(false) }
    var newLimitStr by remember { mutableStateOf("") }
    
    // Dialog flows
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showAnalysisResultDialog by remember { mutableStateOf(false) }
    
    // Add Food states
    var foodDesc by remember { mutableStateOf("") }
    var foodPhotoUri by remember { mutableStateOf<String?>(null) }
    var isUploadingFood by remember { mutableStateOf(false) }
    
    // Analysis Result states
    var currentAnalysisResult by remember { mutableStateOf<com.example.swasthya.FoodAnalysis?>(null) }
    var currentCloudUrl by remember { mutableStateOf<String?>(null) }

    val foodPhotoFile = remember { java.io.File(context.filesDir, "Food_${System.currentTimeMillis()}.jpg") }
    val foodPhotoUriToPass = remember { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", foodPhotoFile) }
    val foodCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) { foodPhotoUri = foodPhotoFile.absolutePath }
    }
    val localFoodGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            val file = java.io.File(context.cacheDir, "Gallery_Food_${System.currentTimeMillis()}.jpg")
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            foodPhotoUri = file.absolutePath
        }
    }

    // Filter foods for selected date
    val startOfDay = java.util.Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

    val foodsForDate = foods.filter { it.timestamp in startOfDay..endOfDay }
    val consumedFoods = foodsForDate.filter { it.isConsumed }
    val totalCaloriesConsumed = consumedFoods.sumOf { it.calories ?: 0 }
    val dailyLimit = user?.dailyCalorieLimit ?: 2000

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Calorie Tracker", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                // Simple Date Picker navigation (prev/next day)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedDateMillis -= 24 * 60 * 60 * 1000 }) { Icon(Icons.Default.ArrowBack, "Prev Day") }
                    Text(java.text.SimpleDateFormat("MMM dd", java.util.Locale.US).format(java.util.Date(selectedDateMillis)), fontWeight = FontWeight.Medium)
                    IconButton(onClick = { selectedDateMillis += 24 * 60 * 60 * 1000 }) { Icon(Icons.Default.ArrowForward, "Next Day") }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("$totalCaloriesConsumed", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF2E7D32))
                    Text("Consumed today", fontSize = 14.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("/ $dailyLimit kcal", fontSize = 16.sp, color = Color.Gray)
                        IconButton(onClick = { 
                            newLimitStr = dailyLimit.toString()
                            showEditLimitDialog = true 
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Limit", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (totalCaloriesConsumed.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (totalCaloriesConsumed > dailyLimit) Color.Red else Color(0xFF4CAF50),
                trackColor = Color(0xFFC8E6C9)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    foodDesc = ""
                    foodPhotoUri = null
                    showAddFoodDialog = true 
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Analyze Food")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze New Food")
            }
        }
    }

    Text("Foods Consumed", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
    if (consumedFoods.isEmpty()) {
        Text("No foods consumed on this date.", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
    } else {
        consumedFoods.forEach { food ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (food.photoUri != null) {
                        coil.compose.AsyncImage(
                            model = food.photoUri,
                            contentDescription = "Food",
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(food.dishName ?: food.description.ifBlank { "Unknown Food" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${food.calories ?: 0} kcal | ${food.protein ?: 0}g Protein", fontSize = 14.sp, color = Color.DarkGray)
                        if (!food.deficiencyWarnings.isNullOrBlank() && food.deficiencyWarnings != "None detected") {
                            Text("Warning: ${food.deficiencyWarnings}", fontSize = 12.sp, color = Color(0xFFD32F2F))
                        }
                    }
                    IconButton(onClick = { onDeleteFood(food) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Food", tint = Color.Red)
                    }
                }
            }
        }
    }

    if (showEditLimitDialog) {
        AlertDialog(
            onDismissRequest = { showEditLimitDialog = false },
            title = { Text("Edit Daily Limit") },
            text = {
                OutlinedTextField(
                    value = newLimitStr,
                    onValueChange = { if (it.length <= 4) newLimitStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Limit (max 6000)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newLimit = newLimitStr.toIntOrNull() ?: 2000
                    if (newLimit in 1..6000 && user != null) {
                        onUpdateUser(user.copy(dailyCalorieLimit = newLimit))
                    }
                    showEditLimitDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditLimitDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddFoodDialog) {
        AlertDialog(
            onDismissRequest = { showAddFoodDialog = false },
            title = { Text("Additional Information") },
            text = {
                Column {
                    Text("Provide photo and any additional details (weight, oil used) for accurate estimation.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { foodCameraLauncher.launch(foodPhotoUriToPass) }, modifier = Modifier.weight(1f)) {
                            Icon(painter = painterResource(id = android.R.drawable.ic_menu_camera), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Camera")
                        }
                        Button(onClick = { localFoodGalleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(painter = painterResource(id = android.R.drawable.ic_menu_gallery), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gallery")
                        }
                    }
                    if (foodPhotoUri != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Image selected!", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            TextButton(onClick = { foodPhotoUri = null }) {
                                Text("Clear Image", color = Color.Red)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = foodDesc,
                        onValueChange = { foodDesc = it },
                        label = { Text("E.g., 200g, less oil, cooked in butter") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUploadingFood = true
                        coroutineScope.launch {
                            if (foodPhotoUri != null) {
                                currentCloudUrl = uploadFileToCloudinary(foodPhotoUri!!)
                            }
                            try {
                                val bitmap = foodPhotoUri?.let { android.graphics.BitmapFactory.decodeFile(it) }
                                currentAnalysisResult = com.example.swasthya.GeminiHelper.analyzeFood(bitmap, foodDesc)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            isUploadingFood = false
                            showAddFoodDialog = false
                            if (currentAnalysisResult != null) {
                                showAnalysisResultDialog = true
                            }
                        }
                    },
                    enabled = !isUploadingFood
                ) {
                    if (isUploadingFood) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Text("Analyze")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFoodDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAnalysisResultDialog && currentAnalysisResult != null) {
        val result = currentAnalysisResult!!
        AlertDialog(
            onDismissRequest = { showAnalysisResultDialog = false },
            title = { Text("Analysis Result") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!result.success) {
                        Text("Analysis Failed: ${result.errorMessage}", color = Color.Red)
                    } else {
                        Text(result.dishName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Est. Weight: ${result.weightGrams}g", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Summary", fontWeight = FontWeight.Bold)
                        Text("${result.calories} kcal | ${result.protein}g Protein | ${result.carbohydrates}g Carbs | ${result.fats}g Fat")
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Micronutrients", fontWeight = FontWeight.Bold)
                        Text(result.vitaminsAndMinerals, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (result.deficiencyWarnings.isNotBlank() && result.deficiencyWarnings != "None detected") {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFD32F2F))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Deficiency Alerts", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(result.deficiencyWarnings, fontSize = 14.sp, color = Color(0xFFD32F2F))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Are you going to consume this?", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddFood(FoodEntity(
                        description = foodDesc,
                        photoUri = foodPhotoUri,
                        cloudUrl = currentCloudUrl,
                        aiAnalysis = "${result.calories} kcal, ${result.protein}g protein",
                        calories = result.calories,
                        carbs = result.carbohydrates,
                        protein = result.protein,
                        fat = result.fats,
                        dishName = result.dishName,
                        weightGrams = result.weightGrams,
                        micronutrients = result.vitaminsAndMinerals,
                        deficiencyWarnings = result.deficiencyWarnings,
                        isConsumed = true
                    ))
                    showAnalysisResultDialog = false
                    currentAnalysisResult = null
                }) { Text("Yes (Track)") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onAddFood(FoodEntity(
                        description = foodDesc,
                        photoUri = foodPhotoUri,
                        cloudUrl = currentCloudUrl,
                        aiAnalysis = "${result.calories} kcal, ${result.protein}g protein",
                        calories = result.calories,
                        carbs = result.carbohydrates,
                        protein = result.protein,
                        fat = result.fats,
                        dishName = result.dishName,
                        weightGrams = result.weightGrams,
                        micronutrients = result.vitaminsAndMinerals,
                        deficiencyWarnings = result.deficiencyWarnings,
                        isConsumed = false
                    ))
                    showAnalysisResultDialog = false
                    currentAnalysisResult = null
                }) { Text("No (Inquiry Only)") }
            }
        )
    }
}

@Composable
fun RecordsScreen(
    medicines: List<MedicineEntity>, 
    onAddMedicine: (MedicineEntity) -> Unit,
    onUpdateMedicine: (MedicineEntity) -> Unit = {},
    onDeleteMedicine: (MedicineEntity) -> Unit = {},
    reports: List<ReportEntity> = emptyList(),
    onAddReport: (ReportEntity) -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToScanHistory: () -> Unit = {}
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
            Text("Records", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Your health data in one place", fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Medical Reports Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToReports() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.List, contentDescription = "Reports", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medical Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("View lab reports, prescriptions and documents", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scan History Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToScanHistory() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = "Scan History", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medicine Scan History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("View previously scanned medicines and prescriptions", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Medications Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { currentView = "Medications" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = "Medications", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medications & Reminders", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Manage your medicines and meal reminders", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onNavigateToReports) {
                    Text("See all")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val recentReports = reports.take(3)
            recentReports.forEach { report ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, contentDescription = "Report")
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(report.fileName, fontWeight = FontWeight.Bold)
                            Text("Uploaded: ${report.uploadDate}", fontSize = 12.sp)
                        }
                    }
                }
            }
            if (recentReports.isEmpty()) {
                Text("No recent reports.", color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stay Safe Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Light orange background
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Safe", tint = Color(0xFFEF6C00), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Stay Safe", fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00))
                        Text("Always keep your health records updated and share with your physician when needed.", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MedicationsScreen(
    medicines: List<MedicineEntity>,
    onAddMedicine: (MedicineEntity) -> Unit,
    onUpdateMedicine: (MedicineEntity) -> Unit,
    onDeleteMedicine: (MedicineEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var selectedMedicineForPopup by remember { mutableStateOf<MedicineEntity?>(null) }
    val selectedMedicinesForDeletion = remember { androidx.compose.runtime.mutableStateListOf<MedicineEntity>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    var pharmaLensAnalysis by remember { mutableStateOf<com.example.swasthya.MedicineAnalysis?>(null) }
    var isPharmaLensLoading by remember { mutableStateOf(false) }
    var showPharmaLensDialog by remember { mutableStateOf(false) }
    val pharmaLensPhotoFile = remember { java.io.File(context.filesDir, "Pharma_${System.currentTimeMillis()}.jpg") }
    val pharmaLensPhotoUri = remember { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pharmaLensPhotoFile) }
    
    val pharmaLensCameraLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            isPharmaLensLoading = true
            coroutineScope.launch {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, pharmaLensPhotoUri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, pharmaLensPhotoUri)
                }
                val analysis = com.example.swasthya.GeminiHelper.analyzeMedicine(bitmap)
                pharmaLensAnalysis = analysis
                isPharmaLensLoading = false
                if (analysis != null) {
                    showPharmaLensDialog = true
                } else {
                    android.widget.Toast.makeText(context, "AI service is temporarily unavailable. Please try again shortly.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Medications & Reminders", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { showAddMedicineDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Reminder (Medicine/Meal)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pharma Lens Scanner
        Button(
            onClick = { pharmaLensCameraLauncher.launch(pharmaLensPhotoUri) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isPharmaLensLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Icon(Icons.Default.Search, contentDescription = "Scan")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pharma Lens Medicine Scanner")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Schedule", fontWeight = FontWeight.Bold)
            if (selectedMedicinesForDeletion.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedMedicinesForDeletion.size} selected", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                    androidx.compose.material3.IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                    }
                    androidx.compose.material3.IconButton(onClick = { selectedMedicinesForDeletion.clear() }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        medicines.forEach { med ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val isSelected = selectedMedicinesForDeletion.contains(med)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { 
                                if (isSelected) selectedMedicinesForDeletion.remove(med)
                                else selectedMedicinesForDeletion.add(med)
                            },
                            onTap = {
                                if (selectedMedicinesForDeletion.isNotEmpty()) {
                                    if (isSelected) selectedMedicinesForDeletion.remove(med)
                                    else selectedMedicinesForDeletion.add(med)
                                } else {
                                    selectedMedicineForPopup = med
                                }
                            }
                        )
                    }
                    .padding(8.dp), 
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (med.photoUri != null) {
                        Icon(Icons.Default.Search, contentDescription = "Photo attached", modifier = Modifier.size(24.dp).padding(end = 8.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text(med.name ?: "Unknown Medicine", fontWeight = FontWeight.Bold)
                        Text("${med.timeLabel} Ã¢â‚¬Â¢ ${med.reminderType}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = !med.isTaken, onCheckedChange = { isChecked ->
                        val updatedMed = med.copy(isTaken = !isChecked)
                        onUpdateMedicine(updatedMed)
                        
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val intent = android.content.Intent(context, com.example.swasthya.MedicineReminderReceiver::class.java)
                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                            context,
                            med.timeInMillis.hashCode(),
                            intent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        if (updatedMed.isTaken) {
                            alarmManager.cancel(pendingIntent)
                            android.widget.Toast.makeText(context, "Reminder turned off", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            intent.putExtra("MEDICINE_NAME", updatedMed.name ?: "Your Medicine")
                            intent.putExtra("REMINDER_TYPE", updatedMed.reminderType)
                            intent.putExtra("IS_MEAL", updatedMed.reminderType.contains("MEAL"))
                            val newIntent = android.app.PendingIntent.getBroadcast(context, med.timeInMillis.hashCode(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                            try {
                                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, updatedMed.timeInMillis, newIntent)
                                android.widget.Toast.makeText(context, "Reminder turned on", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, updatedMed.timeInMillis, newIntent)
                            }
                        }
                    })
                }
            }
        }
    }

    if (selectedMedicineForPopup != null) {
        AlertDialog(
            onDismissRequest = { selectedMedicineForPopup = null },
            title = { Text(selectedMedicineForPopup!!.name ?: "Medicine Details") },
            text = {
                Column {
                    if (selectedMedicineForPopup!!.hasImage && selectedMedicineForPopup!!.photoUri != null) {
                        coil.compose.AsyncImage(
                            model = selectedMedicineForPopup!!.photoUri,
                            contentDescription = "Pill Image",
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("Dosage: ${selectedMedicineForPopup!!.dosage}", fontWeight = FontWeight.Bold)
                    Text("Schedule: ${selectedMedicineForPopup!!.schedule}")
                    Text("Time: ${selectedMedicineForPopup!!.timeLabel} (${selectedMedicineForPopup!!.reminderType})")
                    if (selectedMedicineForPopup!!.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Notes: ${selectedMedicineForPopup!!.explanation}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMedicineForPopup = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAddMedicineDialog) {
        var newMedName by remember { mutableStateOf("") }
        var newMedDosage by remember { mutableStateOf("") }
        var newMedSchedule by remember { mutableStateOf("") }
        var newMedExplanation by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }
        
        var newMedPhotoUri by remember { mutableStateOf<String?>(null) }
        var newMedTimeInMillis by remember { mutableStateOf(java.util.Calendar.getInstance().timeInMillis) }
        var newMedTimeLabel by remember { mutableStateOf("08:00 AM") }
        var isAlarm by remember { mutableStateOf(false) }
        var isMealReminder by remember { mutableStateOf(false) }

        val photoFile = remember { java.io.File(context.filesDir, "Med_${System.currentTimeMillis()}.jpg") }
        val photoUri = remember { androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile) }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                newMedPhotoUri = photoFile.absolutePath
            }
        }
        
        val timePickerDialog = remember {
            android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val now = java.util.Calendar.getInstance()
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                    cal.set(java.util.Calendar.MINUTE, minute)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    
                    if (hourOfDay < now.get(java.util.Calendar.HOUR_OF_DAY) || 
                        (hourOfDay == now.get(java.util.Calendar.HOUR_OF_DAY) && minute < now.get(java.util.Calendar.MINUTE))) {
                        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                    newMedTimeInMillis = cal.timeInMillis
                    newMedTimeLabel = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(cal.time)
                },
                8, 0, false
            )
        }

        AlertDialog(
            onDismissRequest = { showAddMedicineDialog = false },
            title = { Text("Add Reminder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMedName,
                        onValueChange = { newMedName = it },
                        label = { Text("Medicine Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMedDosage,
                        onValueChange = { newMedDosage = it },
                        label = { Text("Dosage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMedSchedule,
                        onValueChange = { newMedSchedule = it },
                        label = { Text("Schedule (e.g. Morning)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMedExplanation,
                        onValueChange = { newMedExplanation = it },
                        label = { Text("Explanation / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { cameraLauncher.launch(photoUri) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (newMedPhotoUri == null) "Take Photo of Pill/Bottle" else "Photo Captured!")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { timePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Time: $newMedTimeLabel")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Alert Type: ")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAlarm) "Alarm" else "Notification", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = isAlarm, onCheckedChange = { isAlarm = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Is this a Meal Reminder?")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = isMealReminder, onCheckedChange = { isMealReminder = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUploading = true
                        coroutineScope.launch {
                            var cloudUrl: String? = null
                            if (newMedPhotoUri != null) {
                                cloudUrl = uploadFileToCloudinary(newMedPhotoUri!!)
                            }
                            val med = MedicineEntity(
                                name = newMedName.ifBlank { null },
                                dosage = newMedDosage,
                                schedule = newMedSchedule,
                                explanation = newMedExplanation,
                                photoUri = newMedPhotoUri,
                                cloudImageUrl = cloudUrl,
                                hasImage = newMedPhotoUri != null,
                                timeInMillis = newMedTimeInMillis,
                                timeLabel = newMedTimeLabel,
                                reminderType = if (isAlarm) {
                                    if (isMealReminder) "ALARM_MEAL" else "ALARM"
                                } else {
                                    if (isMealReminder) "NOTIFICATION_MEAL" else "NOTIFICATION"
                                }
                            )
                            onAddMedicine(med)
                            
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                            val intent = android.content.Intent(context, com.example.swasthya.MedicineReminderReceiver::class.java).apply {
                                putExtra("MEDICINE_NAME", med.name ?: "Your Medicine")
                                putExtra("REMINDER_TYPE", med.reminderType)
                                putExtra("IS_MEAL", med.reminderType.contains("MEAL"))
                            }
                            val pendingIntent = android.app.PendingIntent.getBroadcast(
                                context,
                                med.timeInMillis.hashCode(),
                                intent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                            )

                            val diffInMillis = med.timeInMillis - System.currentTimeMillis()
                            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diffInMillis)
                            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
                            val timeRemainingMsg = if (hours > 0) {
                                "Reminder set for $hours hours and $minutes minutes from now"
                            } else if (minutes > 0) {
                                "Reminder set for $minutes minutes from now"
                            } else {
                                "Reminder set for less than a minute from now"
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, med.timeInMillis, pendingIntent)
                                    android.widget.Toast.makeText(context, timeRemainingMsg, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, med.timeInMillis, pendingIntent)
                                    android.widget.Toast.makeText(context, "Please grant Exact Alarms permission for precise timing!", android.widget.Toast.LENGTH_LONG).show()
                                    try {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    } catch (e: Exception) {}
                                }
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, med.timeInMillis, pendingIntent)
                                android.widget.Toast.makeText(context, timeRemainingMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                            
                            isUploading = false
                            showAddMedicineDialog = false
                            android.widget.Toast.makeText(context, "Medicine Saved!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMedicineDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Reminders") },
            text = { Text("Are you sure you want to delete ${selectedMedicinesForDeletion.size} reminder(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val intent = android.content.Intent(context, com.example.swasthya.MedicineReminderReceiver::class.java)
                        selectedMedicinesForDeletion.forEach { med ->
                            onDeleteMedicine(med)
                            val pendingIntent = android.app.PendingIntent.getBroadcast(context, med.timeInMillis.hashCode(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                            alarmManager.cancel(pendingIntent)
                        }
                        selectedMedicinesForDeletion.clear()
                        showDeleteConfirmation = false
                        android.widget.Toast.makeText(context, "Reminders deleted", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    if (showPharmaLensDialog && pharmaLensAnalysis != null) {
        PharmaLensResultDialog(
            analysis = pharmaLensAnalysis!!,
            onDismiss = { showPharmaLensDialog = false },
            onConfirmConsume = { timesPerDay ->
                showPharmaLensDialog = false
                val cal = java.util.Calendar.getInstance()
                onAddMedicine(MedicineEntity(
                    name = pharmaLensAnalysis!!.name,
                    dosage = pharmaLensAnalysis!!.dosageForm,
                    schedule = "$timesPerDay times a day",
                    explanation = pharmaLensAnalysis!!.strength,
timeInMillis = cal.timeInMillis,
                    timeLabel = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).format(cal.time),
                    reminderType = "Medicine",
                    hasImage = false
                ))
            }
        )
    }
}

@Composable
fun ProfileNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = titleColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosSettingsDialog(
    user: UserEntity?,
    onUpdateUser: (UserEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var contactPreference by remember { mutableStateOf(user?.sosContactPreference ?: "Ask") }
    var actionPreference by remember { mutableStateOf(user?.sosActionPreference ?: "Ask") }
    var customMsg by remember { mutableStateOf(user?.customSosMessage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SOS Emergency Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Who to Contact:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { contactPreference = "Ask" }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = contactPreference == "Ask",
                                    onClick = { contactPreference = "Ask" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Me Every Time", fontSize = 14.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { contactPreference = "112" }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = contactPreference == "112" || contactPreference == "911",
                                    onClick = { contactPreference = "112" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Emergency Services (112)", fontSize = 14.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { contactPreference = "Custom" }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = contactPreference == "Custom",
                                    onClick = { contactPreference = "Custom" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Emergency Contact", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Default Action:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actionPreference = "Ask" }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = actionPreference == "Ask",
                                    onClick = { actionPreference = "Ask" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Me (Show Dialog)", fontSize = 14.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actionPreference = "Call" }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = actionPreference == "Call",
                                    onClick = { actionPreference = "Call" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Direct Phone Call", fontSize = 14.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { actionPreference = "Text" }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = actionPreference == "Text",
                                    onClick = { actionPreference = "Text" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send SOS Text (SMS)", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Custom SMS Message:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customMsg,
                        onValueChange = { customMsg = it },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        placeholder = { Text("Leave blank to include medical conditions & blood group...") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    user?.let {
                        onUpdateUser(it.copy(
                            sosContactPreference = if (contactPreference == "911") "112" else contactPreference,
                            sosActionPreference = actionPreference,
                            customSosMessage = customMsg
                        ))
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AppSettingsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("App Version", color = Color.Gray)
                    Text("1.0.0 (Med Assist)", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sync Engine", color = Color.Gray)
                    Text("Firestore & Cloudinary", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Health Connect", color = Color.Gray)
                    Text("Active", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Helpline Location", color = Color.Gray)
                    Text("India (112)", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun HelpSupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text("We are here to help you!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                Text("If you encounter any issues or have questions regarding medical logs, report scanning, or SOS services, please reach out to us:")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("support@medassist.in", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("1800-112-4567 (Toll-Free)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ProfileScreen(
    user: UserEntity? = null,
    onUpdateUser: (UserEntity) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onSignOut: () -> Unit = {},
    physicians: List<com.example.swasthya.data.PhysicianEntity> = emptyList(),
    onAddPhysician: (com.example.swasthya.data.PhysicianEntity) -> Unit = {},
    onDeletePhysician: (com.example.swasthya.data.PhysicianEntity) -> Unit = {}
) {
    var expandedPersonalInfo by remember { mutableStateOf(false) }
    val name = user?.name?.takeIf { it.isNotBlank() } ?: "John Doe"
    val age = user?.age?.takeIf { it.isNotBlank() } ?: "N/A"
    val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "johndoe@example.com"
    
    val context = LocalContext.current
    
    var showSosDialog by remember { mutableStateOf(false) }
    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddPhysicianDialog by remember { mutableStateOf(false) }
    var selectedPhysicianForDelete by remember { mutableStateOf<com.example.swasthya.data.PhysicianEntity?>(null) }

    if (showSosDialog) {
        SosSettingsDialog(
            user = user,
            onUpdateUser = onUpdateUser,
            onDismiss = { showSosDialog = false }
        )
    }

    if (showAppSettingsDialog) {
        AppSettingsDialog(
            onDismiss = { showAppSettingsDialog = false }
        )
    }

    if (showHelpDialog) {
        HelpSupportDialog(
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showAddPhysicianDialog) {
        var docName by remember { mutableStateOf("") }
        var docHospital by remember { mutableStateOf("") }
        var docPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPhysicianDialog = false },
            title = { Text("Add Physician", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        label = { Text("Physician Name (e.g. Dr. Sarah)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = docHospital,
                        onValueChange = { docHospital = it },
                        label = { Text("Hospital / Clinic") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = docPhone,
                        onValueChange = { docPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docName.isNotBlank() && docPhone.isNotBlank()) {
                            onAddPhysician(
                                com.example.swasthya.data.PhysicianEntity(
                                    name = docName,
                                    hospital = docHospital,
                                    phone = docPhone
                                )
                            )
                            showAddPhysicianDialog = false
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhysicianDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (selectedPhysicianForDelete != null) {
        val physician = selectedPhysicianForDelete!!
        AlertDialog(
            onDismissRequest = { selectedPhysicianForDelete = null },
            title = { Text("Delete Physician?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Are you sure you want to remove ${physician.name} from your care team?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePhysician(physician)
                        selectedPhysicianForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPhysicianForDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IconButton(
            onClick = { showAppSettingsDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "App Settings",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(email, fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(0.55f),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "My Care Team (Physicians)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (physicians.isEmpty()) {
                        Text(
                            text = "No physicians added yet. Tap below to add your care team.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column {
                            physicians.forEachIndexed { index, physician ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(physician.id) {
                                            detectTapGestures(
                                                onLongPress = { selectedPhysicianForDelete = physician },
                                                onTap = {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${physician.phone}"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("ProfileScreen", "Error dial intent", e)
                                                    }
                                                }
                                            )
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Physician Profile",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(physician.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (physician.hospital.isNotBlank()) {
                                            Text(physician.hospital, fontSize = 13.sp, color = Color.Gray)
                                        }
                                        Text(physician.phone, fontSize = 13.sp, color = Color.Gray)
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${physician.phone}"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.util.Log.e("ProfileScreen", "Error dial intent", e)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call Physician",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                
                                if (index < physicians.size - 1) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedButton(
                        onClick = { showAddPhysicianDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Physician", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedPersonalInfo = !expandedPersonalInfo }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Personal Information",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expandedPersonalInfo) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        androidx.compose.animation.AnimatedVisibility(visible = expandedPersonalInfo) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                                
                                ProfileDetailRow("Age", age)
                                ProfileDetailRow("Blood Group", user?.bloodGroup?.takeIf { it.isNotBlank() } ?: "Not specified")
                                ProfileDetailRow("Weight / Height", "${user?.weight?.takeIf { it.isNotBlank() } ?: "-"} kg / ${user?.height?.takeIf { it.isNotBlank() } ?: "-"} cm")
                                ProfileDetailRow("Phone", user?.phone?.takeIf { it.isNotBlank() } ?: "Not specified")
                                ProfileDetailRow("Medical Conditions", user?.disease?.takeIf { it.isNotBlank() } ?: "None")
                                ProfileDetailRow("Goals", user?.expectedGoals?.takeIf { it.isNotBlank() } ?: "None set")
                            }
                        }
                    }
                }

                ProfileNavigationRow(
                    icon = Icons.Default.Warning,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = "SOS / Emergency Settings",
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showSosDialog = true }
                )

                ProfileNavigationRow(
                    icon = Icons.Default.Settings,
                    iconColor = Color(0xFF673AB7),
                    title = "App Settings",
                    onClick = { showAppSettingsDialog = true }
                )

                ProfileNavigationRow(
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFF009688),
                    title = "Help & Support",
                    onClick = { showHelpDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TextButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sign Out",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InsightsScreen(
    user: UserEntity?,
    vitals: List<VitalsEntity>,
    medicines: List<MedicineEntity>,
    reports: List<ReportEntity>,
    foods: List<FoodEntity>,
    steps: String,
    hr: String,
    calories: String,
    viewModel: com.example.swasthya.ui.viewmodels.InsightsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var chatMessages by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var chatInput by remember { mutableStateOf("") }
    var isChatLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.ensureContextLoaded(user, vitals, medicines, reports, foods, steps, hr, calories)
        viewModel.generateInsights(user, vitals, medicines, reports, foods, steps, hr, calories)
        chatMessages = listOf("model" to "Hello! I am your AI Health Assistant. What questions do you have about your health status?")
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("✨ AI Health Insights") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
        ) {
            when (val state = uiState) {
                is com.example.swasthya.ui.viewmodels.InsightsUiState.Idle -> {}
                is com.example.swasthya.ui.viewmodels.InsightsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Building your health context...", color = Color.Gray)
                        }
                    }
                }
                is com.example.swasthya.ui.viewmodels.InsightsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = Color.Red)
                    }
                }
                is com.example.swasthya.ui.viewmodels.InsightsUiState.Success -> {
                    val analysis = state.response
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            InsightCard("🩺 Health Summary", analysis.summary, Color(0xFFE3F2FD), Color(0xFF1565C0))
                        }
                        if (analysis.needsMedicalAttention) {
                            item {
                                InsightCard("⚠️ Medical Attention Advised", analysis.warnings, Color(0xFFFFEBEE), Color(0xFFC62828))
                            }
                        } else if (analysis.importantFindings.isNotBlank()) {
                            item {
                                InsightCard("📌 Important Findings", analysis.importantFindings, Color(0xFFFFF3E0), Color(0xFFEF6C00))
                            }
                        }
                        item {
                            InsightCard("🥗 Nutrition Insights", analysis.nutritionInsights, Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        }
                        item {
                            InsightCard("🏃‍♂️ Activity Insights", analysis.activityInsights, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Chat with your AI Assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                            HorizontalDivider()
                        }
                    
                    items(chatMessages) { (role, message) ->
                        val isUser = role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isUser) MaterialTheme.colorScheme.primary else Color.White)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = message, 
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else Color.Black
                                )
                            }
                        }
                    }
                    
                    if (isChatLoading) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
                
                // Chat Input Area
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask a question...") },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (chatInput.isNotBlank() && !isChatLoading) {
                                    val userMsg = chatInput
                                    chatInput = ""
                                    chatMessages = chatMessages + ("user" to userMsg)
                                    isChatLoading = true
                                    coroutineScope.launch {
                                        val chatResponse = viewModel.sendChatMessage(chatMessages, userMsg)
                                        chatMessages = chatMessages + ("model" to chatResponse)
                                        isChatLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
                        }
                    }
                } // closes Surface
            } // closes Success
        } // closes when
    } // closes Column
} // closes Scaffold
} // closes InsightsScreen

@Composable
fun InsightCard(title: String, content: String, bgColor: Color, iconColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = iconColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(content, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun PharmaLensResultDialog(
    analysis: com.example.swasthya.MedicineAnalysis,
    onDismiss: () -> Unit,
    onConfirmConsume: (String) -> Unit
) {
    var accepted by remember { mutableStateOf(false) }
    var timesPerDay by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💊 Pharma Lens Analysis") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Medicine: ${analysis.name}", fontWeight = FontWeight.Bold)
                Text("Strength: ${analysis.strength}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Form: ${analysis.dosageForm}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Manufacturer: ${analysis.manufacturer}")
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!accepted) {
                    Text("Are you going to consume this drug?", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text("No")
                        }
                        Button(onClick = { accepted = true }) {
                            Text("Yes")
                        }
                    }
                } else {
                    Text("How many times a day?", fontWeight = FontWeight.Bold)
                    androidx.compose.material3.OutlinedTextField(
                        value = timesPerDay,
                        onValueChange = { timesPerDay = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (accepted) {
                Button(onClick = { onConfirmConsume(timesPerDay) }) {
                    Text("Save to Schedule")
                }
            }
        },
        dismissButton = {
            if (accepted) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}



