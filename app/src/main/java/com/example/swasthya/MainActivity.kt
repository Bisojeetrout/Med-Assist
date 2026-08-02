package com.example.swasthya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.swasthya.data.SwasthyaDatabase
import com.example.swasthya.data.FoodEntity
import com.example.swasthya.ui.screens.AuthScreen
import com.example.swasthya.ui.screens.DashboardScreen
import com.example.swasthya.ui.screens.OnboardingScreen
import com.example.swasthya.ui.theme.SwasthyaTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.google.firebase.FirebaseApp.initializeApp(this)
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
        )
        
        val database = SwasthyaDatabase.getDatabase(this)
        val dao = database.swasthyaDao()

        val startWithFoodLog = intent.getBooleanExtra("OPEN_FOOD_LOG", false)

        setContent {
            SwasthyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SwasthyaApp(dao, startWithFoodLog)
                }
            }
        }
    }
}

@Composable
fun SwasthyaApp(dao: com.example.swasthya.data.SwasthyaDao, startWithFoodLog: Boolean = false) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    
    val vitals by dao.getAllVitals().collectAsState(initial = emptyList())
    val medicines by dao.getAllMedicines().collectAsState(initial = emptyList())
    val reports by dao.getAllReports().collectAsState(initial = emptyList())
    val foods by dao.getAllFoods().collectAsState(initial = emptyList())
    val user by dao.getUser().collectAsState(initial = null)

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(
                onNavigateToDashboard = {
                    coroutineScope.launch {
                        val currentUser = dao.getUser().firstOrNull()
                        if (currentUser?.isProfileComplete == true) {
                            navController.navigate("dashboard") {
                                popUpTo("auth") { inclusive = true }
                            }
                        } else {
                            navController.navigate("onboarding") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
        composable("onboarding") {
            com.example.swasthya.ui.screens.OnboardingScreen(
                onComplete = { userEntity ->
                    coroutineScope.launch {
                        dao.insertUser(userEntity)
                        FirestoreSync.syncUser(userEntity)
                    }
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onFillLater = { userEntity ->
                    coroutineScope.launch {
                        dao.insertUser(userEntity)
                        FirestoreSync.syncUser(userEntity)
                    }
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("profile_edit") {
            OnboardingScreen(
                initialUser = user,
                onComplete = { userEntity ->
                    coroutineScope.launch {
                        dao.insertUser(userEntity)
                        FirestoreSync.syncUser(userEntity)
                    }
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                onFillLater = {
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                user = user,
                onUpdateUser = { updatedUser ->
                    coroutineScope.launch {
                        dao.insertUser(updatedUser)
                        FirestoreSync.syncUser(updatedUser)
                    }
                },
                vitals = vitals,
                medicines = medicines,
                onAddVitals = { vital ->
                    coroutineScope.launch {
                        dao.insertVitals(vital)
                        FirestoreSync.syncVitals(user?.phone ?: "anonymous", vital)
                    }
                },
                onAddMedicine = { med ->
                    coroutineScope.launch {
                        dao.insertMedicine(med)
                        FirestoreSync.syncMedicine(user?.phone ?: "anonymous", med)
                    }
                },
                onUpdateMedicine = { med ->
                    coroutineScope.launch {
                        dao.updateMedicine(med)
                        FirestoreSync.syncMedicine(user?.phone ?: "anonymous", med)
                    }
                },
                onDeleteMedicine = { med ->
                    coroutineScope.launch {
                        dao.deleteMedicine(med)
                    }
                },
                reports = reports,
                onAddReport = { report ->
                    coroutineScope.launch {
                        dao.insertReport(report)
                        FirestoreSync.syncReport(user?.phone ?: "anonymous", report)
                    }
                },
                foods = foods,
                onAddFood = { food: FoodEntity ->
                    coroutineScope.launch {
                        dao.insertFood(food)
                        FirestoreSync.syncFood(user?.phone ?: "anonymous", food)
                    }
                },
                onNavigateToReports = {
                    navController.navigate("reports")
                },
                onNavigateToProfile = {
                    navController.navigate("profile_edit")
                },
                onNavigateToInsights = { steps, hr, calories ->
                    navController.navigate("insights/$steps/$hr/$calories")
                },
                onSignOut = {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                startWithFoodLog = startWithFoodLog,
                onShareWithPhysician = {
                    val reportText = buildString {
                        append("Patient Profile\n")
                        append("Name: ${user?.name ?: "Unknown"}\n")
                        append("Age: ${user?.age ?: "Unknown"}\n")
                        append("Blood Group: ${user?.bloodGroup ?: "Unknown"}\n")
                        append("Weight/Height: ${user?.weight ?: "-"} kg / ${user?.height ?: "-"} cm\n")
                        append("Medical Conditions: ${user?.disease ?: "None"}\n\n")

                        append("Recent Vitals (Last 7 days)\n")
                        vitals.take(7).forEach { v ->
                            append("${v.date}: Mood: ${v.mood}, Sleep: ${v.sleepDuration}h, Symptoms: ${v.symptoms}\n")
                        }
                        append("\n")

                        append("Recent Diet (Last 3 days)\n")
                        val threeDaysAgo = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000
                        foods.filter { it.timestamp >= threeDaysAgo }.forEach { f ->
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date(f.timestamp))
                            append("$dateStr: ${f.description} (${f.calories ?: "Unknown"} kcal)\n")
                        }
                        append("\n")

                        append("Current Medications\n")
                        medicines.forEach { m ->
                            append("${m.name}: ${m.dosage} - ${m.schedule}\n")
                        }
                        append("\n")

                        append("Recent Reports\n")
                        reports.take(5).forEach { r ->
                            append("${r.fileName} (Uploaded: ${r.uploadDate})\n")
                            if (!r.reportSummary.isNullOrBlank()) {
                                append("Summary: ${r.reportSummary}\n")
                            }
                        }
                    }

                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, reportText)
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Share with Physician")
                    navController.context.startActivity(shareIntent)
                }
            )
        }
        composable(
            "insights/{steps}/{hr}/{calories}",
            enterTransition = { slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left, androidx.compose.animation.core.tween(300)) },
            exitTransition = { slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right, androidx.compose.animation.core.tween(300)) }
        ) { backStackEntry ->
            val steps = backStackEntry.arguments?.getString("steps") ?: "0"
            val hr = backStackEntry.arguments?.getString("hr") ?: "0"
            val calories = backStackEntry.arguments?.getString("calories") ?: "0"
            
            com.example.swasthya.ui.screens.InsightsScreen(
                user = user,
                vitals = vitals,
                medicines = medicines,
                reports = reports,
                foods = foods,
                steps = steps,
                hr = hr,
                calories = calories,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "reports",
            enterTransition = { slideIntoContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Left, androidx.compose.animation.core.tween(300)) },
            exitTransition = { slideOutOfContainer(androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Right, androidx.compose.animation.core.tween(300)) }
        ) {
            com.example.swasthya.ui.screens.ReportsScreen(
                reports = reports,
                onAddReport = { report ->
                    coroutineScope.launch {
                        dao.insertReport(report)
                        FirestoreSync.syncReport(user?.phone ?: "anonymous", report)
                    }
                },
                onDeleteReport = { report ->
                    coroutineScope.launch {
                        dao.deleteReport(report)
                        FirestoreSync.deleteReport(user?.phone ?: "anonymous", report)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
