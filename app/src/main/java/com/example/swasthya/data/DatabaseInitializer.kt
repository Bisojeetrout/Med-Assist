package com.example.swasthya.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object DatabaseInitializer {

    private const val PREFS_NAME = "swasthya_db_prefs"
    private const val KEY_IS_INITIALIZED = "is_db_initialized"

    suspend fun initializeDatabaseIfNeed(context: Context, dao: SwasthyaDao, onProgress: (Float) -> Unit = {}) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IS_INITIALIZED, false)) {
            onProgress(1f)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                Log.d("DatabaseInitializer", "Starting database initialization from CSVs...")
                
                // 1. Jan Aushadhi
                onProgress(0.1f)
                val janAushadhiList = mutableListOf<JanAushadhiEntity>()
                context.assets.open("medicine_data/jan_aushadhi_medicines.csv").use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var isFirstLine = true
                    var srNo = 1
                    reader.forEachLine { line ->
                        if (isFirstLine) { isFirstLine = false; return@forEachLine }
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 6) {
                            janAushadhiList.add(JanAushadhiEntity(
                                srNo = srNo++,
                                drugCode = tokens[1],
                                genericName = tokens[2],
                                unitSize = tokens[3],
                                mrp = tokens[4],
                                groupName = tokens[5]
                            ))
                        }
                    }
                }
                dao.insertJanAushadhi(janAushadhiList)

                // 2. Indian Medicines
                onProgress(0.4f)
                val indianMedicinesList = mutableListOf<IndianMedicineEntity>()
                context.assets.open("medicine_data/indian_medicines.csv").use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var isFirstLine = true
                    reader.forEachLine { line ->
                        if (isFirstLine) { isFirstLine = false; return@forEachLine }
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 24) {
                            try {
                                indianMedicinesList.add(IndianMedicineEntity(
                                    id = tokens[0].toIntOrNull() ?: 0,
                                    name = tokens[1],
                                    price = tokens[2],
                                    isDiscontinued = tokens[3].toBooleanStrictOrNull() ?: false,
                                    manufacturerName = tokens[4],
                                    type = tokens[5],
                                    packSizeLabel = tokens[6],
                                    shortComposition1 = tokens[7],
                                    shortComposition2 = tokens[8],
                                    substitute0 = tokens[9],
                                    substitute1 = tokens[10],
                                    substitute2 = tokens[11],
                                    substitute3 = tokens[12],
                                    substitute4 = tokens[13],
                                    sideEffects = tokens[14],
                                    use0 = tokens[15],
                                    use1 = tokens[16],
                                    use2 = tokens[17],
                                    use3 = tokens[18],
                                    use4 = tokens[19],
                                    chemicalClass = tokens[20],
                                    habitForming = tokens[21],
                                    therapeuticClass = tokens[22],
                                    actionClass = tokens[23]
                                ))
                            } catch (e: Exception) {
                                Log.e("DatabaseInitializer", "Error parsing indian_medicines row: $line", e)
                            }
                        }
                    }
                }
                // Batch insert chunked to avoid SQL limits
                indianMedicinesList.chunked(1000).forEach { dao.insertIndianMedicines(it) }

                // 3. Indian Products
                onProgress(0.7f)
                val indianProductsList = mutableListOf<IndianProductEntity>()
                context.assets.open("medicine_data/indian_products.csv").use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var isFirstLine = true
                    reader.forEachLine { line ->
                        if (isFirstLine) { isFirstLine = false; return@forEachLine }
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 15) {
                            try {
                                indianProductsList.add(IndianProductEntity(
                                    productId = tokens[0].toIntOrNull() ?: 0,
                                    brandName = tokens[1],
                                    manufacturer = tokens[2],
                                    priceInr = tokens[3],
                                    isDiscontinued = tokens[4],
                                    dosageForm = tokens[5],
                                    packSize = tokens[6],
                                    packUnit = tokens[7],
                                    numActiveIngredients = tokens[8],
                                    primaryIngredient = tokens[9],
                                    primaryStrength = tokens[10],
                                    activeIngredients = tokens[11],
                                    therapeuticClass = tokens[12],
                                    packagingRaw = tokens[13],
                                    manufacturerRaw = tokens[14]
                                ))
                            } catch (e: Exception) {
                                Log.e("DatabaseInitializer", "Error parsing indian_products row: $line", e)
                            }
                        }
                    }
                }
                indianProductsList.chunked(1000).forEach { dao.insertIndianProducts(it) }

                onProgress(1f)
                prefs.edit().putBoolean(KEY_IS_INITIALIZED, true).apply()
                Log.d("DatabaseInitializer", "Database initialization complete.")
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Failed to initialize database", e)
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var currentToken = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val char = line[i]
            if (char == '\"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                tokens.add(currentToken.toString().trim())
                currentToken = StringBuilder()
            } else {
                currentToken.append(char)
            }
        }
        tokens.add(currentToken.toString().trim())
        return tokens
    }
}
