package com.example.swasthya.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swasthya.data.ReportEntity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    reports: List<ReportEntity>,
    onAddReport: (ReportEntity) -> Unit,
    onDeleteReport: (ReportEntity) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedReportForPopup by remember { mutableStateOf<ReportEntity?>(null) }
    var pendingReportFile by remember { mutableStateOf<File?>(null) }
    var reportNameInput by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
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

                    pendingReportFile = outFile
                    reportNameInput = ""
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

                    pendingReportFile = outFile
                    reportNameInput = ""
                } catch (e: Exception) {
                    Log.e("Scanner", "Error saving scanned file", e)
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
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
                pendingReportFile = outFile
                reportNameInput = ""
            } catch (e: Exception) {
                Log.e("PDFPicker", "Error saving PDF file", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medical Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            Button(
                onClick = {
                    scanner.getStartScanIntent(context as Activity)
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                        .addOnFailureListener { e -> Log.e("Scanner", "Error starting scanner", e) }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Scan")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan New Prescription")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload PDF")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload PDF File")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn {
                items(reports) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { selectedReportForPopup = report },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.List, contentDescription = "File")
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(report.fileName, fontWeight = FontWeight.Bold)
                                Text("Uploaded: ${report.uploadDate}", fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (reports.isEmpty()) {
                    item {
                        Text(
                            "No reports uploaded yet.",
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
            
        if (isUploading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).pointerInput(Unit) {},
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

if (selectedReportForPopup != null) {
        AlertDialog(
            onDismissRequest = { selectedReportForPopup = null },
            title = { Text(selectedReportForPopup!!.fileName) },
            text = { Text("Uploaded on: ${selectedReportForPopup!!.uploadDate}\nPath: ${selectedReportForPopup!!.localUri}") },
            confirmButton = {
                Button(
                    onClick = { 
                        onDeleteReport(selectedReportForPopup!!)
                        selectedReportForPopup = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        try {
                            val file = File(selectedReportForPopup!!.localUri)
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, if (file.name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/*")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "View Report"))
                        } catch (e: Exception) {
                            Log.e("ViewFile", "Error viewing file", e)
                        }
                    }) {
                        Text("View File")
                    }
                    TextButton(onClick = { selectedReportForPopup = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (pendingReportFile != null) {
        AlertDialog(
            onDismissRequest = { pendingReportFile = null },
            title = { Text("Name Your Report") },
            text = {
                Column {
                    Text("What kind of report is this? (e.g. Blood Test, Prescription, X-Ray)")
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = reportNameInput,
                        onValueChange = { reportNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Report Name") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = pendingReportFile!!
                        val finalName = if (reportNameInput.isNotBlank()) reportNameInput else file.name
                        isUploading = true
                        pendingReportFile = null
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
                                    uploadDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date())
                                )
                            )
                            isUploading = false
                            android.widget.Toast.makeText(context, "Report Uploaded Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReportFile = null }) { Text("Cancel") }
            }
        )
    }
}
