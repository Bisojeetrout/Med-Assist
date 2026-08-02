package com.example.swasthya.ui.screens

import android.util.Log
import androidx.compose.foundation.background
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
import com.example.swasthya.data.ReportEntity
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
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Swasthya", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Your personal health companion", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(64.dp))
        Button(
            onClick = {
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
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = "Google Icon")
            Spacer(Modifier.width(8.dp))
            Text("Sign in with Google", fontSize = 18.sp)
        }
    }
}

@Composable
fun DashboardScreen(
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
    onNavigateToReports: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToInsights: (String, String, String) -> Unit = { _, _, _ -> },
    onSignOut: () -> Unit = {},
    startWithFoodLog: Boolean = false,
    onShareWithPhysician: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(if (startWithFoodLog) 2 else 0) }
    
    // Health Connect States
    var steps by remember { mutableStateOf("0") }
    var hr by remember { mutableStateOf("0") }
    var calories by remember { mutableStateOf("0") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
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
                }
            }
        } catch(e: Exception) { }
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
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeScreen(user = user, steps = steps, hr = hr, calories = calories, vitals = vitals, foods = foods, reports = reports, medicines = medicines, onNavigateToInsights = onNavigateToInsights, onShareWithPhysician = onShareWithPhysician)
                1 -> VitalsScreen(vitals, onAddVitals, onSyncRequested = {})
                2 -> RecordsScreen(medicines, onAddMedicine, onUpdateMedicine, onDeleteMedicine, reports, onAddReport, foods, onAddFood, onNavigateToReports, startWithFoodLog)
                3 -> ProfileScreen(user, onUpdateUser, onNavigateToProfile, onSignOut)
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
    onShareWithPhysician: () -> Unit = {}
) {
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val caloriesConsumed = foods.filter { it.timestamp >= todayStart }.sumOf { it.calories ?: 0 }

    var showQuickSummaryDialog by remember { mutableStateOf(false) }
    var quickSummaryText by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showSosDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val triggerCall = {
        val contact = if (user?.sosContactPreference == "911") "911" else user?.emergencyContactPhone?.takeIf { it.isNotBlank() } ?: "911"
        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$contact"))
        context.startActivity(intent)
    }

    val triggerText = {
        val contact = if (user?.sosContactPreference == "911") "911" else user?.emergencyContactPhone?.takeIf { it.isNotBlank() } ?: "911"
        val defaultMsg = "EMERGENCY! I need help. Blood Type: ${user?.bloodGroup ?: "Unknown"}, Conditions: ${user?.disease ?: "None"}. Please contact me."
        val msg = if (user?.customSosMessage.isNullOrBlank()) defaultMsg else user!!.customSosMessage
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:$contact")).apply {
            putExtra("sms_body", msg)
        }
        context.startActivity(intent)
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            title = { Text("Emergency Action") },
            text = { Text("What do you want to do?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        triggerCall()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Call")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        triggerText()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Text (SMS)")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emergency Alert
        Button(
            onClick = { 
                when (user?.sosActionPreference) {
                    "Call" -> triggerCall()
                    "Text" -> triggerText()
                    else -> showSosDialog = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Emergency", tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("SOS / EMERGENCY ALERT", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Health Connect Dashboard Cards
        Text("Today's Activity", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Steps Card
            Card(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)) // Light Blue
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👟", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(steps, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1565C0))
                    Text("Steps", fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            // HR Card
            Card(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)) // Light Red
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("❤️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(hr, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFC62828))
                    Text("bpm", fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            // Calories Card
            Card(
                modifier = Modifier.weight(1f).aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)) // Light Orange
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(calories, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFEF6C00))
                    Text("kcal", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.weight(1f).height(100.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Light Green
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍽️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$caloriesConsumed", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                    Text("Consumed", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick AI Summary Button
        Button(
            onClick = { showQuickSummaryDialog = true },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
        ) {
            Text("✨ Quick AI Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // AI Health Insights Button
        Button(
            onClick = { onNavigateToInsights(steps, hr, calories) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        ) {
            Text("📊 Full AI Health Insights", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        
        
        // One-Tap Share
        Button(
            onClick = onShareWithPhysician,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Share")
            Spacer(Modifier.width(8.dp))
            Text("One-Tap Share with Physician")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showQuickSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showQuickSummaryDialog = false },
            title = { Text("✨ Quick AI Summary") },
            text = {
                if (quickSummaryText == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating summary...")
                    }
                    LaunchedEffect(Unit) {
                        val todayStartCalendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val summary = com.example.swasthya.GeminiHelper.getQuickSummary(
                            steps, hr, calories, foods.count { it.timestamp >= todayStartCalendar }, reports, medicines
                        )
                        quickSummaryText = summary
                    }
                } else {
                    Text(quickSummaryText!!)
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showQuickSummaryDialog = false
                    quickSummaryText = null 
                }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun VitalsScreen(
    vitals: List<VitalsEntity>,
    onAddVitals: (VitalsEntity) -> Unit,
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
fun RecordsScreen(
    medicines: List<MedicineEntity>, 
    onAddMedicine: (MedicineEntity) -> Unit,
    onUpdateMedicine: (MedicineEntity) -> Unit = {},
    onDeleteMedicine: (MedicineEntity) -> Unit = {},
    reports: List<ReportEntity> = emptyList(),
    onAddReport: (ReportEntity) -> Unit = {},
    foods: List<FoodEntity> = emptyList(),
    onAddFood: (FoodEntity) -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    startWithFoodLog: Boolean = false
) {
    val context = LocalContext.current
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var selectedMedicineForPopup by remember { mutableStateOf<MedicineEntity?>(null) }
    var selectedFoodForPopup by remember { mutableStateOf<FoodEntity?>(null) }
    
    val selectedMedicinesForDeletion = remember { androidx.compose.runtime.mutableStateListOf<MedicineEntity>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
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
                if (analysis != null) showPharmaLensDialog = true
            }
        }
    }

    var expandedSection by remember { mutableStateOf<String?>(if (startWithFoodLog) "Food" else "Medicines") }
    var showAddFoodDialog by remember { mutableStateOf(startWithFoodLog) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Drug Interaction Alert Placeholder ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)) // Light red
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color.Red)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Drug Interaction Alert", fontWeight = FontWeight.Bold, color = Color.Red)
                    Text("Warning: Do not take Aspirin with Ibuprofen.", color = Color.DarkGray, fontSize = 14.sp)
                }
            }
        }

        // --- Medical Reports Section ---
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToReports() }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Medical Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }

        // --- Medicines Section ---
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == "Medicines") null else "Medicines" }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Medicines & Reminders", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Icon(if (expandedSection == "Medicines") androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == "Medicines") {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        Text("${med.timeLabel} • ${med.reminderType}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(checked = !med.isTaken, onCheckedChange = { isChecked ->
                                        val updatedMed = med.copy(isTaken = !isChecked)
                                        onUpdateMedicine(updatedMed)
                                        
                                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                        val intent = Intent(context, MedicineReminderReceiver::class.java)
                                        val pendingIntent = PendingIntent.getBroadcast(
                                            context,
                                            med.timeInMillis.hashCode(),
                                            intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        
                                        if (updatedMed.isTaken) {
                                            alarmManager.cancel(pendingIntent)
                                            android.widget.Toast.makeText(context, "Reminder turned off", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            intent.putExtra("MEDICINE_NAME", updatedMed.name ?: "Your Medicine")
                                            intent.putExtra("REMINDER_TYPE", updatedMed.reminderType)
                                            intent.putExtra("IS_MEAL", updatedMed.reminderType.contains("MEAL"))
                                            val newIntent = PendingIntent.getBroadcast(context, med.timeInMillis.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                                            try {
                                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, updatedMed.timeInMillis, newIntent)
                                                android.widget.Toast.makeText(context, "Reminder turned on", android.widget.Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                alarmManager.set(AlarmManager.RTC_WAKEUP, updatedMed.timeInMillis, newIntent)
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Food Log Section ---
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expandedSection = if (expandedSection == "Food") null else "Food" }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Food Consumed", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Icon(if (expandedSection == "Food") androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                androidx.compose.animation.AnimatedVisibility(visible = expandedSection == "Food") {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                        Text("Logged: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(food.timestamp))}", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (foods.isEmpty()) {
                            Text("No meals logged yet.", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
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

    if (showAddMedicineDialog) {
        var newMedName by remember { mutableStateOf("") }
        var newMedDosage by remember { mutableStateOf("") }
        var newMedSchedule by remember { mutableStateOf("") }
        var newMedExplanation by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        
        var newMedPhotoUri by remember { mutableStateOf<String?>(null) }
        var newMedTimeInMillis by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
        var newMedTimeLabel by remember { mutableStateOf("08:00 AM") }
        var isAlarm by remember { mutableStateOf(false) }
        var isMealReminder by remember { mutableStateOf(false) }

        val photoFile = remember { File(context.filesDir, "Med_${System.currentTimeMillis()}.jpg") }
        val photoUri = remember { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile) }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                newMedPhotoUri = photoFile.absolutePath
            }
        }
        
        val timePickerDialog = remember {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val now = Calendar.getInstance()
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    
                    // If time is in the past (strictly earlier hour or earlier minute), schedule for tomorrow
                    if (hourOfDay < now.get(Calendar.HOUR_OF_DAY) || 
                        (hourOfDay == now.get(Calendar.HOUR_OF_DAY) && minute < now.get(Calendar.MINUTE))) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    newMedTimeInMillis = cal.timeInMillis
                    newMedTimeLabel = SimpleDateFormat("hh:mm a", Locale.US).format(cal.time)
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
                            
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
                                putExtra("MEDICINE_NAME", med.name ?: "Your Medicine")
                                putExtra("REMINDER_TYPE", med.reminderType)
                                putExtra("IS_MEAL", med.reminderType.contains("MEAL"))
                            }
                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                med.timeInMillis.hashCode(),
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, med.timeInMillis, pendingIntent)
                                    android.widget.Toast.makeText(context, timeRemainingMsg, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    alarmManager.set(AlarmManager.RTC_WAKEUP, med.timeInMillis, pendingIntent)
                                    android.widget.Toast.makeText(context, "Please grant Exact Alarms permission for precise timing!", android.widget.Toast.LENGTH_LONG).show()
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                    } catch (e: Exception) {}
                                }
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, med.timeInMillis, pendingIntent)
                                android.widget.Toast.makeText(context, timeRemainingMsg, android.widget.Toast.LENGTH_LONG).show()
                            }
                            
                            expandedSection = "Medicines"
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
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val intent = Intent(context, MedicineReminderReceiver::class.java)
                        selectedMedicinesForDeletion.forEach { med ->
                            onDeleteMedicine(med)
                            val pendingIntent = PendingIntent.getBroadcast(context, med.timeInMillis.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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
    
    if (showAddFoodDialog) {
        val coroutineScope = rememberCoroutineScope()
        var foodDesc by remember { mutableStateOf("") }
        var foodPhotoUri by remember { mutableStateOf<String?>(null) }
        var isUploadingFood by remember { mutableStateOf(false) }
        val foodPhotoFile = remember { File(context.filesDir, "Food_${System.currentTimeMillis()}.jpg") }
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
                            if (foodPhotoUri != null) {
                                cloudUrl = uploadFileToCloudinary(foodPhotoUri!!)
                                try {
                                    val bitmap = android.graphics.BitmapFactory.decodeFile(foodPhotoUri!!)
                                    val analysis = com.example.swasthya.GeminiHelper.analyzeFood(bitmap, foodDesc)
                                    aiAnalysis = analysis.summary
                                    calories = analysis.calories
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            onAddFood(FoodEntity(description = foodDesc, photoUri = foodPhotoUri, cloudUrl = cloudUrl, aiAnalysis = aiAnalysis, calories = calories))
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
    
    if (showPharmaLensDialog && pharmaLensAnalysis != null) {
        PharmaLensResultDialog(
            analysis = pharmaLensAnalysis!!,
            onDismiss = { showPharmaLensDialog = false },
            onConfirmConsume = { timesPerDay ->
                showPharmaLensDialog = false
                val cal = java.util.Calendar.getInstance()
                onAddMedicine(MedicineEntity(
                    name = pharmaLensAnalysis!!.brand,
                    dosage = pharmaLensAnalysis!!.dosage,
                    schedule = "$timesPerDay times a day",
                    explanation = pharmaLensAnalysis!!.use,
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
fun ProfileScreen(user: UserEntity? = null, onUpdateUser: (UserEntity) -> Unit = {}, onEditProfile: () -> Unit = {}, onSignOut: () -> Unit = {}) {
    val name = user?.name?.takeIf { it.isNotBlank() } ?: "John Doe"
    val age = user?.age?.takeIf { it.isNotBlank() } ?: "N/A"
    val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "johndoe@example.com"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile Pic",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(email, fontSize = 16.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text("Edit Profile")
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Personal Information
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Age:")
                    Text(age, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Blood Group:")
                    Text(user?.bloodGroup?.takeIf { it.isNotBlank() } ?: "Not specified", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Weight / Height:")
                    val w = user?.weight?.takeIf { it.isNotBlank() } ?: "-"
                    val h = user?.height?.takeIf { it.isNotBlank() } ?: "-"
                    Text("$w kg / $h cm", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Phone:")
                    Text(user?.phone?.takeIf { it.isNotBlank() } ?: "Not specified", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Medical Conditions:")
                    Text(user?.disease?.takeIf { it.isNotBlank() } ?: "None", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Goals:")
                    Text(user?.expectedGoals?.takeIf { it.isNotBlank() } ?: "None set", fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // My Doctors Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("My Care Team (Physicians)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "Doc", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Dr. Sarah Jenkins", fontWeight = FontWeight.Bold)
                        Text("City General Hospital", fontSize = 14.sp)
                        Text("+1-555-0198", fontSize = 14.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Button(onClick = { /* Add doc */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add Physician")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // SOS Settings Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SOS / Emergency Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Who to Contact:", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = user?.sosContactPreference == "Ask",
                        onClick = { user?.let { onUpdateUser(it.copy(sosContactPreference = "Ask")) } }
                    )
                    Text("Ask Me")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = user?.sosContactPreference == "911",
                        onClick = { user?.let { onUpdateUser(it.copy(sosContactPreference = "911")) } }
                    )
                    Text("Emergency Services (911)")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = user?.sosContactPreference == "Custom",
                        onClick = { user?.let { onUpdateUser(it.copy(sosContactPreference = "Custom")) } }
                    )
                    Text("Custom Contact")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Default Action:", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = user?.sosActionPreference == "Ask",
                        onClick = { user?.let { onUpdateUser(it.copy(sosActionPreference = "Ask")) } }
                    )
                    Text("Ask Me")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = user?.sosActionPreference == "Call",
                        onClick = { user?.let { onUpdateUser(it.copy(sosActionPreference = "Call")) } }
                    )
                    Text("Call")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(
                        selected = user?.sosActionPreference == "Text",
                        onClick = { user?.let { onUpdateUser(it.copy(sosActionPreference = "Text")) } }
                    )
                    Text("Text (SMS)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Custom SMS Message:", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = user?.customSosMessage ?: "",
                    onValueChange = { newText -> user?.let { onUpdateUser(it.copy(customSosMessage = newText)) } },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    placeholder = { Text("Leave blank to use default medical SOS message...") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onSignOut) {
            Text("Sign Out", color = Color.Red)
        }
        Spacer(modifier = Modifier.height(24.dp))
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
                Text("Brand: ${analysis.brand}", fontWeight = FontWeight.Bold)
                Text("Generic: ${analysis.genericName}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Use: ${analysis.use}")
                Text("Dosage: ${analysis.dosage}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Manufacturer: ${analysis.manufacturer}")
                Text("Genuine Check: ${analysis.isGenuine}", color = if (analysis.isGenuine.contains("Yes", ignoreCase = true)) Color(0xFF2E7D32) else Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Side Effects: ${analysis.sideEffects}")
                Text("Interactions: ${analysis.interactions}", color = Color(0xFFC62828))
                
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
