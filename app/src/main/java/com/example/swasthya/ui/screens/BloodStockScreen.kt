package com.example.swasthya.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swasthya.data.model.LocationItem
import com.example.swasthya.viewmodel.BloodStockState
import com.example.swasthya.viewmodel.BloodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodStockScreen(
    onBack: () -> Unit,
    viewModel: BloodViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val states by viewModel.states.collectAsState()
    val districts by viewModel.districts.collectAsState()

    var selectedState by remember { mutableStateOf<LocationItem?>(null) }
    var selectedDistrict by remember { mutableStateOf<LocationItem?>(null) }

    val bloodGroups = listOf(
        "all" to "All Blood Groups",
        "18" to "AB-Ve",
        "17" to "AB+Ve",
        "12" to "A-Ve",
        "11" to "A+Ve",
        "14" to "B-Ve",
        "13" to "B+Ve",
        "23" to "Oh-VE",
        "22" to "Oh+VE",
        "16" to "O-Ve",
        "15" to "O+Ve"
    )
    var selectedBloodGroup by remember { mutableStateOf(bloodGroups[0]) }

    val bloodComponents = listOf(
        "11" to "Whole Blood",
        "14" to "Single Donor Platelet",
        "18" to "Single Donor Plasma",
        "28" to "Sagm Packed Red Blood Cells",
        "23" to "Random Donor Platelets",
        "16" to "Platelet Rich Plasma",
        "20" to "Platelet Concentrate",
        "19" to "Plasma",
        "12" to "Packed Red Blood Cells",
        "30" to "Leukoreduced Rbc",
        "29" to "Irradiated RBC",
        "13" to "Fresh Frozen Plasma",
        "17" to "Cryoprecipitate",
        "21" to "Cryo Poor Plasma"
    )
    var selectedComponent by remember { mutableStateOf(bloodComponents[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find Blood", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // State Dropdown
            DropdownMenuField(
                label = "State",
                options = states,
                selectedOption = selectedState,
                onOptionSelected = {
                    selectedState = it
                    selectedDistrict = null
                    if (it != null) viewModel.fetchDistricts(it.value)
                },
                displayText = { it.label }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // District Dropdown
            DropdownMenuField(
                label = "District (Optional)",
                options = listOf(LocationItem("-1", "All Districts")) + districts,
                selectedOption = selectedDistrict,
                onOptionSelected = { selectedDistrict = it },
                displayText = { it.label }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownMenuField(
                    label = "Blood Group",
                    options = bloodGroups,
                    selectedOption = selectedBloodGroup,
                    onOptionSelected = { selectedBloodGroup = it },
                    displayText = { it.second },
                    modifier = Modifier.weight(1f)
                )

                DropdownMenuField(
                    label = "Component",
                    options = bloodComponents,
                    selectedOption = selectedComponent,
                    onOptionSelected = { selectedComponent = it },
                    displayText = { it.second },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedState != null) {
                        viewModel.fetchBloodStock(
                            stateCode = selectedState!!.value,
                            districtCode = selectedDistrict?.value ?: "-1",
                            bloodGroup = selectedBloodGroup.first,
                            bloodComponent = selectedComponent.first
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedState != null
            ) {
                Text("Find Blood")
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is BloodStockState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select criteria and search")
                    }
                }
                is BloodStockState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is BloodStockState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No blood banks found.")
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.data) { item ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = item.bankName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Category: ${item.category}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Availability: ${item.availability}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Last Updated: ${item.lastUpdated}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is BloodStockState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownMenuField(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    displayText: (T) -> String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption?.let { displayText(it) } ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayText(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
