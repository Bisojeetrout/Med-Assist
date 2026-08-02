package com.example.swasthya.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var age by remember { mutableStateOf(initialUser?.age?.toString() ?: "") }
    var weight by remember { mutableStateOf(initialUser?.weight?.toString() ?: "") }
    var height by remember { mutableStateOf(initialUser?.height?.toString() ?: "") }
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
                title = { Text("Complete Your Profile") }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = isMandatoryFilled,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Continue")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = isMandatoryFilled,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fill Later")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Mandatory Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Optional Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = disease,
                onValueChange = { disease = it },
                label = { Text("Current medical conditions / diseases") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("What do you expect from this app?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                goalsOptions.forEach { goal ->
                    val isSelected = selectedGoals.contains(goal)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedGoals = if (checked) {
                                    selectedGoals + goal
                                } else {
                                    selectedGoals - goal
                                }
                            }
                        )
                        Text(text = goal, fontSize = 16.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Emergency Contact (Optional)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = emergencyContactName,
                onValueChange = { emergencyContactName = it },
                label = { Text("Contact Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = emergencyContactPhone,
                onValueChange = { emergencyContactPhone = it },
                label = { Text("Contact Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp)) // Extra space for bottom bar
        }
    }
}
