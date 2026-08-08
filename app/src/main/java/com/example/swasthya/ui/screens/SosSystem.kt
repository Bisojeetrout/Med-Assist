package com.example.swasthya.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.telephony.SmsManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.swasthya.data.SosEventEntity
import com.example.swasthya.data.SwasthyaDao
import com.example.swasthya.data.UserEntity
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyToolsScreen(
    onBack: () -> Unit,
    onNavigateToFindBlood: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        val tools = listOf(
            EmergencyToolItem("Emergency Services", Icons.Default.Call, Color(0xFFD32F2F)),
            EmergencyToolItem("Find Blood", Icons.Default.Favorite, Color(0xFFC2185B)),
            EmergencyToolItem("Emergency Medicine", Icons.Default.AddCircle, Color(0xFF1976D2)),
            EmergencyToolItem("Nearby Hospitals", Icons.Default.Home, Color(0xFF388E3C)),
            EmergencyToolItem("Medical Card", Icons.Default.Person, Color(0xFFF57C00)),
            EmergencyToolItem("Share Location", Icons.Default.LocationOn, Color(0xFF1976D2))
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            items(tools) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable { 
                            if (tool.name == "Find Blood") {
                                onNavigateToFindBlood()
                            } else {
                                /* Future implementation */
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(tool.color.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = tool.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

data class EmergencyToolItem(val name: String, val icon: ImageVector, val color: Color)

@Composable
fun SosCountdownDialog(
    user: UserEntity,
    dao: SwasthyaDao,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableStateOf(user.sosCountdownSeconds) }
    var isExecuting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var executionError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val executeSos = {
        isExecuting = true
        coroutineScope.launch {
            try {
                executeEmergencyProtocol(context, user, dao)
                showSuccess = true
            } catch (e: Exception) {
                executionError = e.message ?: "Failed to execute SOS"
            } finally {
                isExecuting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!user.sosCountdownEnabled) {
            executeSos()
        } else {
            while (countdown > 0 && !isExecuting && !showSuccess) {
                delay(1000)
                if (!isExecuting && !showSuccess) {
                    countdown--
                }
            }
            if (countdown == 0 && !isExecuting && !showSuccess) {
                executeSos()
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) },
            title = { Text("Emergency Alert Sent") },
            text = {
                val contacts = getContactList(user.emergencyContactsJson)
                Column {
                    Text("Message sent to:")
                    contacts.forEach { c ->
                        Text("• ${c.name} (${c.phone})")
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) { Text("Done") }
            },
            dismissButton = {
                OutlinedButton(onClick = { executeSos() }) { Text("Send Again") }
            }
        )
    } else if (executionError != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("SOS Failed") },
            text = { Text(executionError!!) },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
    } else if (user.sosCountdownEnabled && !isExecuting) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss on outside tap */ },
            title = { Text("🚨 Emergency SOS", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Emergency alert will be sent in:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = countdown.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            },
            confirmButton = {
                Button(onClick = { executeSos() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Send Now")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    } else if (isExecuting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Sending SOS...") },
            text = { CircularProgressIndicator() },
            confirmButton = {}
        )
    }
}

data class EmergencyContact(val name: String, val phone: String)

fun getContactList(jsonStr: String): List<EmergencyContact> {
    val list = mutableListOf<EmergencyContact>()
    try {
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(EmergencyContact(obj.getString("name"), obj.getString("phone")))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

suspend fun executeEmergencyProtocol(context: Context, user: UserEntity, dao: SwasthyaDao) {
    // 1. Check Contacts
    val contacts = getContactList(user.emergencyContactsJson).toMutableList()
    if (contacts.isEmpty()) {
        if (user.emergencyContactPhone.isNotBlank()) {
            contacts.add(EmergencyContact(user.emergencyContactName.ifBlank { "Emergency Contact" }, user.emergencyContactPhone))
        } else {
            throw Exception("No emergency contacts configured.")
        }
    }

    // 2. Get Location
    var locationLink = "Location unavailable"
    val hasLocPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (hasLocPerm) {
        try {
            val fusedLoc = LocationServices.getFusedLocationProviderClient(context)
            val location: Location? = fusedLoc.lastLocation.await()
            if (location != null) {
                locationLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        locationLink = "Location permission denied."
    }

    // 3. Build Message
    val baseMessage = user.customSosMessage.ifBlank { "I may need immediate assistance. Please contact me immediately." }
    val fullMessage = "🚨 EMERGENCY ALERT\n\n$baseMessage\n\nCurrent Location:\n$locationLink"

    // 4. Send SMS
    val hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    if (hasSmsPerm) {
        val smsManager = SmsManager.getDefault()
        for (contact in contacts) {
            try {
                smsManager.sendTextMessage(contact.phone, null, fullMessage, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } else {
        throw Exception("SMS permission denied.")
    }

    // 5. Save Event Locally
    val contactsJson = JSONArray()
    contacts.forEach { 
        val obj = JSONObject()
        obj.put("name", it.name)
        obj.put("phone", it.phone)
        contactsJson.put(obj)
    }
    dao.insertSosEvent(SosEventEntity(
        contactsMessagedJson = contactsJson.toString(),
        locationLink = locationLink
    ))

    // 6. Auto Call
    if (user.autoCallAfterSos && contacts.isNotEmpty()) {
        val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        if (hasCallPerm) {
            try {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contacts.first().phone}"))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
