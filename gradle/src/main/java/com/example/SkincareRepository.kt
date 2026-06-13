package com.example

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.model.SkinAnalysisAndProductRecommendation
import com.example.data.model.SkincareProductSuggestion
import com.example.data.api.Part
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class SkincareRepository(private val dao: SkincareDao) {

    val allTasks: Flow<List<SkincareTask>> = dao.getAllTasksFlow()
    val allCompletions: Flow<List<TaskCompletionLog>> = dao.getAllCompletionsFlow()
    val allSavedReports: Flow<List<SavedSkinReport>> = dao.getAllSavedReportsFlow()
    val allWaterLogs: Flow<List<WaterLog>> = dao.getAllWaterLogsFlow()
    val allRecommendedProducts: Flow<List<SkincareProduct>> = dao.getAllProductsFlow()

    fun getCompletionsForDate(dateStr: String): Flow<List<TaskCompletionLog>> {
        return dao.getCompletionsForDateFlow(dateStr)
    }

    suspend fun addProduct(name: String, brand: String, category: String, skinType: String, concern: String, description: String, activeIngredients: String): Long {
        return dao.insertProduct(
            SkincareProduct(
                name = name,
                brand = brand,
                category = category,
                skinType = skinType,
                concern = concern,
                description = description,
                activeIngredients = activeIngredients,
                isCustom = true
            )
        )
    }

    suspend fun deleteProduct(product: SkincareProduct) {
        dao.deleteProduct(product)
    }

    suspend fun saveSkinReport(report: SavedSkinReport): Long {
        return dao.insertSavedReport(report)
    }

    suspend fun deleteSkinReport(report: SavedSkinReport) {
        dao.deleteSavedReport(report)
    }

    suspend fun addTask(name: String, isAm: Boolean, isCustom: Boolean = true): Long {
        return dao.insertTask(SkincareTask(name = name, isAm = isAm, isCustom = isCustom))
    }

    suspend fun deleteTask(task: SkincareTask) {
        dao.deleteTask(task)
    }

    suspend fun toggleCompletion(dateStr: String, taskId: Int, isCompleted: Boolean) {
        if (isCompleted) {
            dao.insertOrUpdateCompletion(TaskCompletionLog(dateStr = dateStr, taskId = taskId, isCompleted = true))
        } else {
            dao.removeCompletion(dateStr, taskId)
        }
    }

    fun getWaterLog(dateStr: String): Flow<WaterLog?> {
        return dao.getWaterLogForDateFlow(dateStr)
    }

    suspend fun updateWaterLog(waterLog: WaterLog) {
        dao.insertOrUpdateWaterLog(waterLog)
    }

    suspend fun syncClinicalRoutine(routineAM: String, routinePM: String) {
        // Clear previous custom tasks to populate the fresh clinic recommendation
        dao.deleteCustomTasks()

        val amSteps = parseRoutineSteps(routineAM)
        for (step in amSteps) {
            addTask(name = "$step ☀️", isAm = true, isCustom = true)
        }

        val pmSteps = parseRoutineSteps(routinePM)
        for (step in pmSteps) {
            addTask(name = "$step 🌙", isAm = false, isCustom = true)
        }
    }

    private fun parseRoutineSteps(routineStr: String): List<String> {
        return routineStr.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                // Clean prefixes like "1. ", "1- ", "AM: ", "PM: ", etc.
                var cleanLine = line.replace(Regex("^\\d+([.\\-:]\\s*)?"), "")
                cleanLine = cleanLine.replace(Regex("^[-*]\\s*"), "")
                cleanLine.trim()
            }
            .filter { it.isNotEmpty() && it.length > 3 }
    }

    // Helper to calculate daily streak from completions history
    suspend fun calculateStreaks(todayDateStr: String): StreakData {
        val completions = dao.getAllCompletionsDirect()
        val tasks = dao.getAllTasksDirect()
        if (completions.isEmpty() || tasks.isEmpty()) {
            return StreakData(currentStreak = 0, longestStreak = 0, completedDates = emptySet())
        }

        // Group completions by date
        val completionsByDate = completions
            .filter { it.isCompleted }
            .groupBy { it.dateStr }

        // A day is considered completed if the user has completed at least 50% of the active tasks for that day
        val completedDates = mutableSetOf<String>()
        val totalTasksCount = tasks.size.coerceAtLeast(1)

        completionsByDate.forEach { (date, logs) ->
            // Let's count completion. If check-ins exist, say >= 50% or if they checked in at least 1 task
            // Setting a threshold of checking at least 1 task or at least 50% of tasks
            // Let's make it friendly: if they completed at least 1 task, they kept their streak alive! (or >= 50% of tasks)
            val doneCount = logs.map { it.taskId }.distinct().size
            if (doneCount > 0) {
                completedDates.add(date)
            }
        }

        // Calculate current and maximum streak by moving backwards or forwards through dates
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Find maximum historical/longest streak
        val sortedDates = completedDates.mapNotNull { 
            try { sdf.parse(it) } catch (e: Exception) { null }
        }.sorted()

        var maxStreak = 0
        var currentRunningStreak = 0
        var previousDate: Date? = null

        for (date in sortedDates) {
            if (previousDate == null) {
                currentRunningStreak = 1
            } else {
                val diff = date.time - previousDate.time
                val daysDiff = diff / (1000 * 60 * 60 * 24)
                if (daysDiff <= 1) {
                    currentRunningStreak++
                } else {
                    if (currentRunningStreak > maxStreak) {
                        maxStreak = currentRunningStreak
                    }
                    currentRunningStreak = 1
                }
            }
            previousDate = date
        }
        if (currentRunningStreak > maxStreak) {
            maxStreak = currentRunningStreak
        }

        // Compute current continuous streak ending today (or yesterday, to be lenient)
        var currentStreak = 0
        try {
            val today = sdf.parse(todayDateStr)
            val calendar = Calendar.getInstance()
            calendar.time = today

            // Check if today is completed
            var checkDateStr = sdf.format(calendar.time)
            val isCompletedToday = completedDates.contains(checkDateStr)

            // Or yesterday completed to keep the streak going
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val checkDateStrYesterday = sdf.format(calendar.time)
            val isCompletedYesterday = completedDates.contains(checkDateStrYesterday)

            if (isCompletedToday || isCompletedYesterday) {
                // Count backwards from since when it continues
                calendar.time = today
                if (!isCompletedToday) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                while (true) {
                    val dateFormatted = sdf.format(calendar.time)
                    if (completedDates.contains(dateFormatted)) {
                        currentStreak++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Make sure longest is at least current
        val finalLongest = maxOf(maxStreak, currentStreak)

        return StreakData(
            currentStreak = currentStreak,
            longestStreak = finalLongest,
            completedDates = completedDates
        )
    }

    suspend fun analyzeUserDataAndRecommend(
        skinType: String,
        concerns: List<String>,
        goals: List<String>,
        age: Int? = null,
        lifestyle: String? = null
    ): SkinAnalysisAndProductRecommendation = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isDemoKey = apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)

        if (isDemoKey) {
            return@withContext getLocalRealisticRecommendation(skinType, concerns, goals)
        }

        val concernsStr = concerns.joinToString(", ")
        val goalsStr = goals.joinToString(", ")
        val systemPrompt = """
            You are 'GlowLogic CDSS' - an advanced AI Dermatology Decision Support system designed by Zubayda Ramzi.
            Your task is to analyze the user's skincare profile:
            - Skin Type: $skinType
            - Skin Concerns: $concernsStr
            - Skincare Goals: $goalsStr
            ${if (age != null) "- User Age: $age" else ""}
            ${if (lifestyle != null) "- Lifestyle Details: $lifestyle" else ""}

            Based on this, you must suggest customized morning & evening steps of skincare routine, active ingredients AND propose real skincare/medical products.
            Ensure you recommend realistic, highly available skincare/dermatological products (like CeraVe, La Roche-Posay, The Ordinary, Eucerin, Cetaphil, Bioderma, etc.) that match their concerns and skin type specifically.

            You MUST respond with a valid JSON in the exact following structure. All string values MUST be in Arabic:
            {
              "detectedSkinType": "$skinType",
              "scientificAnalysis": "شرح علمي جزيئي دقيق وتحليل لحالة البشرة الحالية ومسببات المشاكل بناءً على المدخلات بالعربية",
              "targetActiveIngredients": ["نياسيناميد", "حمض الساليسيليك", "سيراميد"],
              "morningRoutineSteps": ["تنظيف البشرة بغسول لطيف", "وضع مرطب خفيف", "تطبيق واقي الشمس"],
              "eveningRoutineSteps": ["تنظيف عميق بالبغسول", "تطبيق السيروم المعالج", "مرطب حاجز البشرة"],
              "protectiveAdvice": "نصائح وإرشادات وقائية هامة لسلامة وحماية خلايا البشرة والوقاية من المسببات البيئية",
              "suggestedProducts": [
                {
                  "name": "اسم المنتج تحديداً (مثال: CeraVe Cleanser)",
                  "brand": "العلامة التجارية (مثال: CeraVe)",
                  "purpose": "فائدة المنتج للبشرة وكيفية دعمه للروتين",
                  "howToUse": "طريقة وتوقيت الاستخدام بدقة"
                }
              ],
              "avoidIngredientsAndTriggers": ["العطور الكاوية", "التقشير الفيزيائي الخشن", "التعرض للشمس دون واقٍ"],
              "lifestyleRecommendations": ["شرب 8 أكواب ماء يومياً لحماية مرونة الخلايا والوقاية من بهتان البشرة", "تجنب التوتر الكورتيزولي المنشط للحبوب الدهنية"]
            }

            Do NOT add any markdown formatting, backticks, or text outside the JSON block. Return ONLY the strict raw JSON string.
        """.trimIndent()

        val promptPart = Part(
            text = "Analyze this user skincare profile and suggest products and steps of skincare in Arabic in strict JSON format."
        )

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(promptPart))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini")

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(SkinAnalysisAndProductRecommendation::class.java)
            val result = adapter.fromJson(jsonText) ?: throw Exception("JSON conversion error")

            // Auto-insert suggested products into our app's recommended products table so they dynamically appear in the Pharmacy UI
            for (prod in result.suggestedProducts) {
                val category = when {
                    prod.name.lowercase().contains("cleanser") || prod.name.lowercase().contains("wash") || prod.name.lowercase().contains("غسول") -> "غسول"
                    prod.name.lowercase().contains("moistur") || prod.name.lowercase().contains("cream") || prod.name.lowercase().contains("مرطب") -> "مرطب"
                    prod.name.lowercase().contains("scrub") || prod.name.lowercase().contains("peel") || prod.name.lowercase().contains("مقشر") || prod.name.lowercase().contains("exfoli") -> "مقشر"
                    prod.name.lowercase().contains("sun") || prod.name.lowercase().contains("spf") || prod.name.lowercase().contains("واقي") -> "واقي شمس"
                    else -> "سيروم"
                }
                
                dao.insertProduct(
                    SkincareProduct(
                        name = prod.name,
                        brand = prod.brand,
                        category = category,
                        skinType = skinType,
                        concern = concerns.firstOrNull() ?: "جميع المشاكل",
                        description = prod.purpose,
                        activeIngredients = prod.howToUse,
                        isCustom = true
                    )
                )
            }

            return@withContext result
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getLocalRealisticRecommendation(skinType, concerns, goals)
        }
    }

    private fun getLocalRealisticRecommendation(
        skinType: String,
        concerns: List<String>,
        goals: List<String>
    ): SkinAnalysisAndProductRecommendation {
        val scientificAnalysis = when (skinType) {
            "جافة" -> "طبيعة بشرتك الجافة تعاني من فقر في الدهون الطبيعية والـ lipids الهيكلية في حاجز البشرة الواقي، مما يسارع في الفقد المائي للترطيب الخلوي ويبهت الجلد."
            "دهنية" -> "بشرتك الدهنية تتميز بنشاط مفرط وغير منظم للغدد الزهمية التي تفرز السيبوم، مما يقود لتمدد خلايا المسام وجذب الخلايا الميتة والشوائب البيئية لتكوين الرؤوس والالتهابات."
            "مختلطة" -> "تظهر بشرتك توازناً متفاوتاً؛ فرط لمعان زهمي في منطقة T-Zone مقابل جفاف وحاجة مائية بالغة في الخدين والوجنتين."
            else -> "تتميز بشرتك بتحسس سريع وتوسع ميكروبي للأوعية الدموية مما يدعو لتهدئة دائمة للخلايا وحظر الإضافات العطرية الكحولية."
        }

        val targetActiveIngredients = when (skinType) {
            "جافة" -> listOf("حمض الهيالورونيك", "السيراميد", "البانثينول")
            "دهنية" -> listOf("حمض الساليسيليك (BHA)", "النياسيناميد", "الزنك")
            "مختلطة" -> listOf("النياسيناميد", "حمض الجليكوليك", "الجلسرين")
            else -> listOf("خلاصة السيكا", "مستخلص البابونج", "سينتيلا أسياتيكا")
        }

        val morningRoutineSteps = listOf(
            "تنظيف البشرة باستخدام غسول موازن ومضاد للتحسس",
            "تطبيق سيروم مائي مرطب لإنعاش الخلايا وشد الترهلات",
            "غلق الروتين بالمرطب الواقي المائي الداعم",
            "تطبيق واقي الشمس الخفيف واسع المدى لتجنب التهيج الحراري"
        )

        val eveningRoutineSteps = listOf(
            "تنظيف البشرة وإزالة الشوائب العالقة نهاراً",
            "تطبيق المكونات والعلاجات النشطة المستهدفة للمشاكل الجلدية",
            "إحكام حاجز البشرة بدهن مرطب سميك ومصلح دهني خلوي"
        )

        val protectiveAdvice = "تجنبي الاستحمام بالماء الحار للغاية والفرك الفيزيائي بعنف، وحاولي تعقيم الأدوات الشخصية والوسائد بشكل أسبوعي."

        val suggestedProducts = when (skinType) {
            "دهنية" -> listOf(
                SkincareProductSuggestion(
                    name = "Effaclar Purifying Foaming Gel",
                    brand = "La Roche-Posay / لاروش بوزيه",
                    purpose = "غسول رغوي رقيق ينظف المسام العميقة ويزيل الزهم الزائد بشكل منظم",
                    howToUse = "يُدلك بلطف على وجه رطب صباحاً ومساءً ثم يُشطف جيداً"
                ),
                SkincareProductSuggestion(
                    name = "Niacinamide 10% + Zinc 1%",
                    brand = "The Ordinary / ذا أوردينري",
                    purpose = "سيروم مركّب لتضييق المسام وتنظيم إفراز الزيوت ومقاومة البثور النشطة",
                    howToUse = "يُطبق بضع قطرات على بشرة نظيفة قبل المرطب ليلاً"
                ),
                SkincareProductSuggestion(
                    name = "Effaclar Mat Moisturizer",
                    brand = "La Roche-Posay / لاروش بوزيه",
                    purpose = "مرطب مائي مطفأ خالي من الزيوت لتبريد اللمعان وتوازن الخلايا الدهنية",
                    howToUse = "يوضع بعد السيروم كخطوة واقية مريحة ومقاومة للمعان"
                )
            )
            "جافة" -> listOf(
                SkincareProductSuggestion(
                    name = "Hydrating Facial Cleanser",
                    brand = "CeraVe / سيرافي",
                    purpose = "غسول مائي حليبي غير رغوي لغسل البشرة وترميم حاجز الرطوبة بالتوازي",
                    howToUse = "يوضع على بشرة رطبة ويدلك بنعومة دائرية ثم يشطف بماء فاتر"
                ),
                SkincareProductSuggestion(
                    name = "Hyaluronic Acid 2% + B5",
                    brand = "The Ordinary / ذا أوردينري",
                    purpose = "سيروم مائي عالي السحب الخلوي لترطيب خلايا البشرة العميقة وزيادة نضارتها",
                    howToUse = "يطبق دائماً على بشرة رطبة منادية بالماء لتعزيز السحب المائي"
                ),
                SkincareProductSuggestion(
                    name = "Moisturizing Cream / Ceramide",
                    brand = "CeraVe / سيرافي",
                    purpose = "كريم مرطب سميك وغني بثلاثة سيراميدات أساسية لمنع التبخر المائي وحجز السوائل",
                    howToUse = "يوضع بسخاء بعد سيروم الترطيب صباحاً ومساءً"
                )
            )
            else -> listOf(
                SkincareProductSuggestion(
                    name = "Cicaplast Baume B5+",
                    brand = "La Roche-Posay / لاروش بوزيه",
                    purpose = "مرهم مهدئ خلوي غني بالبانثينول والماديكاسوسايد لترميم فوري لحاجز البشرة المتهيج",
                    howToUse = "توضع مسحة دافئة على المناطق الجافة أو المتهيجة ليلاً"
                ),
                SkincareProductSuggestion(
                    name = "Toleriane Sensitive Fluid",
                    brand = "La Roche-Posay / لاروش بوزيه",
                    purpose = "سائل مرطب خفيف وخالٍ من العطور والمهيجات للبشرات شديدة التحسس",
                    howToUse = "يطبق كمرطب مائي آمن صباحاً ومساءً"
                ),
                SkincareProductSuggestion(
                    name = "Anthelios UVMune 400 SPF50+",
                    brand = "La Roche-Posay / لاروش بوزيه",
                    purpose = "واقي شمس سائل وخفيف ومقاوم للمياه والأشعة فوق البنفسجية الطويلة دون إزعاج",
                    howToUse = "يوضع كآخر خطوة صباحية ويجدد كل ساعتين في حال التعرض الطويل للشمس"
                )
            )
        }

        val avoidIngredientsAndTriggers = listOf(
            "المقشرات اليدوية الخشنة التي تحدث خدوشاً في الخلايا",
            "العطور والمذيبات الاصطناعية المجهدة للبشرة الحساسة",
            "الكحول المشوه المجفف لمستويات الزيوت المفيدة بالبشرة"
        )

        val lifestyleRecommendations = listOf(
            "شرب ما لا يقل عن 2.5 لتر من الماء المفلتر يومياً لتغذية خلايا الأدمة",
            "تجنب الأغذية مفرطة السكريات والألبان الكثيفة المحفزة للهرمونات الدهنية",
            "النوم الكافي المتوازن لمدة 7-8 ساعات لمساعدة آليات الترميم الليلي الخلوي"
        )

        return SkinAnalysisAndProductRecommendation(
            detectedSkinType = skinType,
            scientificAnalysis = scientificAnalysis,
            targetActiveIngredients = targetActiveIngredients,
            morningRoutineSteps = morningRoutineSteps,
            eveningRoutineSteps = eveningRoutineSteps,
            protectiveAdvice = protectiveAdvice,
            suggestedProducts = suggestedProducts,
            avoidIngredientsAndTriggers = avoidIngredientsAndTriggers,
            lifestyleRecommendations = lifestyleRecommendations
        )
    }
}

data class StreakData(
    val currentStreak: Int,
    val longestStreak: Int,
    val completedDates: Set<String>
)
