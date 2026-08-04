package com.example.swasthya.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swasthya.data.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialUser: UserEntity? = null,
    onComplete: (UserEntity) -> Unit,
    onFillLater: (UserEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialUser?.name ?: "") }
    var age by remember { mutableStateOf(initialUser?.age ?: "") }
    var weight by remember { mutableStateOf(initialUser?.weight ?: "") }
    var height by remember { mutableStateOf(initialUser?.height ?: "") }
    var bloodGroup by remember { mutableStateOf(initialUser?.bloodGroup ?: "") }
    var disease by remember { mutableStateOf(initialUser?.disease ?: "") }
    var phone by remember { mutableStateOf(initialUser?.phone ?: "") }
    var emergencyContactName by remember { mutableStateOf(initialUser?.emergencyContactName ?: "") }
    var emergencyContactPhone by remember { mutableStateOf(initialUser?.emergencyContactPhone ?: "") }

    val goalsOptions = listOf("Healthy lifestyle", "Free from diseases", "Weight Management", "Improve Fitness")
    var selectedGoals by remember { 
        mutableStateOf(
            if (initialUser?.expectedGoals?.isNotBlank() == true) {
                initialUser.expectedGoals.split(",").map { it.trim() }.toSet()
            } else {
                setOf<String>()
            }
        ) 
    }

    val isMandatoryFilled = name.isNotBlank() && age.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Complete Your Profile",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (isMandatoryFilled) {
                                onComplete(
                                    UserEntity(
                                        id = initialUser?.id ?: 1,
                                        name = name,
                                        age = age,
                                        weight = weight,
                                        height = height,
                                        bloodGroup = bloodGroup,
                                        disease = disease,
                                        phone = phone,
                                        expectedGoals = selectedGoals.joinToString(", "),
                                        isProfileComplete = true,
                                        emergencyContactName = emergencyContactName,
                                        emergencyContactPhone = emergencyContactPhone,
                                        sosContactPreference = initialUser?.sosContactPreference ?: "Ask",
                                        sosActionPreference = initialUser?.sosActionPreference ?: "Ask",
                                        customSosMessage = initialUser?.customSosMessage ?: ""
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = isMandatoryFilled,
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (isMandatoryFilled) {
                                onFillLater(
                                    UserEntity(
                                        id = initialUser?.id ?: 1,
                                        name = name,
                                        age = age,
                                        weight = weight,
                                        height = height,
                                        bloodGroup = bloodGroup,
                                        disease = disease,
                                        phone = phone,
                                        expectedGoals = selectedGoals.joinToString(", "),
                                        isProfileComplete = true,
                                        emergencyContactName = emergencyContactName,
                                        emergencyContactPhone = emergencyContactPhone,
                                        sosContactPreference = initialUser?.sosContactPreference ?: "Ask",
                                        sosActionPreference = initialUser?.sosActionPreference ?: "Ask",
                                        customSosMessage = initialUser?.customSosMessage ?: ""
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = isMandatoryFilled,
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Fill Later", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            Color(0xFFF7F9FC)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Intro Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Build Your Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "This helps us customize your medical alerts and goals.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            // Card 1: Mandatory Information
            OnboardingCard(
                title = "Mandatory Information",
                icon = Icons.Default.Person,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age *") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Health & Body Metrics
            OnboardingCard(
                title = "Health & Body Metrics",
                icon = Icons.Default.Star,
                iconColor = Color(0xFF009688)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Weight (kg)") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height (cm)") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = { bloodGroup = it },
                        label = { Text("Blood Group (e.g. O+, A-)") },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Medical Background
            OnboardingCard(
                title = "Medical Background",
                icon = Icons.Default.Warning,
                iconColor = MaterialTheme.colorScheme.error
            ) {
                OutlinedTextField(
                    value = disease,
                    onValueChange = { disease = it },
                    label = { Text("Current medical conditions or diseases") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 4: Expectation & Goals
            OnboardingCard(
                title = "App Expectations & Goals",
                icon = Icons.Default.Check,
                iconColor = Color(0xFF673AB7)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalsOptions.forEach { goal ->
                        val isSelected = selectedGoals.contains(goal)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGoals = if (isSelected) selectedGoals - goal else selectedGoals + goal
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedGoals = if (checked == true) selectedGoals + goal else selectedGoals - goal
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = goal,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 5: Emergency Contact
            OnboardingCard(
                title = "Emergency Contact (Optional)",
                icon = Icons.Default.Call,
                iconColor = Color(0xFFE91E63)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emergencyContactName,
                        onValueChange = { emergencyContactName = it },
                        label = { Text("Contact Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = { Text("Contact Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OnboardingCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
