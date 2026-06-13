package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PersonalizedSkincareTip
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed interface SkincareTipUiState {
    object Initial : SkincareTipUiState
    object Loading : SkincareTipUiState
    data class Success(val tip: PersonalizedSkincareTip) : SkincareTipUiState
    data class Error(val message: String) : SkincareTipUiState
}

sealed interface SkincareAnalysisUiState {
    object Initial : SkincareAnalysisUiState
    object Loading : SkincareAnalysisUiState
    data class Success(val recommendation: com.example.data.model.SkinAnalysisAndProductRecommendation) : SkincareAnalysisUiState
    data class Error(val message: String) : SkincareAnalysisUiState
}

class SkincareTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SkincareDatabase.getDatabase(application)
    private val repository = SkincareRepository(database.dao())

    // SimpleDateFormat for database storage
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Current selected date for view/tracking
    private val _selectedDateStr = MutableStateFlow(sdf.format(Date()))
    val selectedDateStr: StateFlow<String> = _selectedDateStr.asStateFlow()

    // Combined UI task list for the selected date
    val taskItemsForSelectedDate: StateFlow<List<TrackableTaskItem>> = combine(
        repository.allTasks,
        _selectedDateStr.flatMapLatest { date -> repository.getCompletionsForDate(date) }
    ) { tasks, completions ->
        val completedIds = completions.map { it.taskId }.toSet()
        tasks.map { task ->
            TrackableTaskItem(
                task = task,
                isCompleted = completedIds.contains(task.id)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Water intake log for the selected date
    val waterLogForSelectedDate: StateFlow<WaterLog?> = _selectedDateStr
        .flatMapLatest { date -> repository.getWaterLog(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allSavedReports: StateFlow<List<SavedSkinReport>> = repository.allSavedReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allWaterLogs: StateFlow<List<WaterLog>> = repository.allWaterLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCompletions: StateFlow<List<TaskCompletionLog>> = repository.allCompletions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTasks: StateFlow<List<SkincareTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allRecommendedProducts: StateFlow<List<SkincareProduct>> = repository.allRecommendedProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveSkinReport(report: SkinReport) {
        viewModelScope.launch {
            repository.saveSkinReport(
                SavedSkinReport(
                    dateStr = sdf.format(Date()),
                    skinType = report.skinType,
                    hydration = report.hydration,
                    barrierHealth = report.barrierHealth,
                    pathology = report.pathology,
                    routineAM = report.routineAM,
                    routinePM = report.routinePM,
                    avoid = report.avoid,
                    isDemo = report.isDemo
                )
            )
        }
    }

    fun saveSkinReport(savedReport: SavedSkinReport) {
        viewModelScope.launch {
            repository.saveSkinReport(savedReport)
        }
    }

    fun deleteSkinReport(report: SavedSkinReport) {
        viewModelScope.launch {
            repository.deleteSkinReport(report)
        }
    }

    // Daily Skincare Tip state
    private val _skincareTipState = MutableStateFlow<SkincareTipUiState>(SkincareTipUiState.Initial)
    val skincareTipState: StateFlow<SkincareTipUiState> = _skincareTipState.asStateFlow()

    // User Skincare Analysis state
    private val _skincareAnalysisState = MutableStateFlow<SkincareAnalysisUiState>(SkincareAnalysisUiState.Initial)
    val skincareAnalysisState: StateFlow<SkincareAnalysisUiState> = _skincareAnalysisState.asStateFlow()

    fun analyzeUserDataAndSuggest(
        skinType: String,
        concerns: List<String>,
        goals: List<String>,
        age: Int? = null,
        lifestyle: String? = null
    ) {
        viewModelScope.launch {
            _skincareAnalysisState.value = SkincareAnalysisUiState.Loading
            try {
                val recommendation = repository.analyzeUserDataAndRecommend(
                    skinType = skinType,
                    concerns = concerns,
                    goals = goals,
                    age = age,
                    lifestyle = lifestyle
                )
                _skincareAnalysisState.value = SkincareAnalysisUiState.Success(recommendation)
                saveSkincareAnalysisToLocal(recommendation)
            } catch (e: Exception) {
                e.printStackTrace()
                _skincareAnalysisState.value = SkincareAnalysisUiState.Error(e.localizedMessage ?: "حدث خطأ أثناء تحليل البيانات")
            }
        }
    }

    fun generateSkincareTip(concern: String) {
        viewModelScope.launch {
            _skincareTipState.value = SkincareTipUiState.Loading
            
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
                val fallback = getFallbackTip(concern)
                _skincareTipState.value = SkincareTipUiState.Success(fallback)
                return@launch
            }
            
            val systemPrompt = """
                You are an expert dermatological AI called GlowLogic, created by Zubayda Ramzi.
                Your role is to generate a personalized skincare tip based on the user's specific skin concern: '$concern'.
                You MUST respond with a valid JSON in the following format. Ensure all text values are in Arabic:
                {
                  "concern": "$concern",
                  "molecular_cause": "شرح علمي جزيئي مبسط لسبب حدوث المشكلة على مستوى خلايا البشرة والغدد الدهنية",
                  "chemical_advice": "نصيحة دقيقة تهم المكونات الكيميائية والنشطة (مثل أحماض AHA/BHA، نياسيناميد، ريتينول أو فيتامينات) وكيفية تطبيقها أو دمجها في هذه الحالة",
                  "daily_habit": "عادة سلوكية يومية مجربة ومهمة لمنع تفاقم المشكلة وتحسين مظهر البشرة الخارجي والداخلي",
                  "critical_note": "تنبيه محذر أو نصيحة طبية حرجة لا بد من الانتباه إليها لتفادي المضاعفات (بموجبه من الأخصائية زبيدة رمزي)"
                }
            """.trimIndent()

            val request = com.example.data.api.GenerateContentRequest(
                contents = listOf(
                    com.example.data.api.Content(
                        parts = listOf(com.example.data.api.Part(text = "Generate a personalized skincare tip for the concern: $concern. Response in Arabic in strict JSON format."))
                    )
                ),
                generationConfig = com.example.data.api.GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.7f
                ),
                systemInstruction = com.example.data.api.Content(parts = listOf(com.example.data.api.Part(text = systemPrompt)))
            )

            try {
                val response = com.example.data.api.RetrofitClient.service.generateContent(apiKey, request)
                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Empty response from AI")
                
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(com.example.data.model.PersonalizedSkincareTip::class.java)
                val tip = adapter.fromJson(jsonText) ?: throw Exception("JSON conversion error")
                
                _skincareTipState.value = SkincareTipUiState.Success(tip)
            } catch (e: Exception) {
                e.printStackTrace()
                val fallback = getFallbackTip(concern)
                _skincareTipState.value = SkincareTipUiState.Success(fallback)
            }
        }
    }

    private fun getFallbackTip(concern: String): com.example.data.model.PersonalizedSkincareTip {
        return when {
            concern.contains("حب") || concern.contains("بثور") || concern.contains("حب الشباب") || concern.contains("Acne") -> {
                com.example.data.model.PersonalizedSkincareTip(
                    concern = "حب الشباب والبثور 🧪",
                    molecular_cause = "تراكم الزهم الزائد وموت الخلايا القرنية على فوهة الغدة الدهنية يؤدي إلى تسرطن لاهوائي لبكتيريا الـ C. acnes النشطة، مما يحفز التورم والالتهاب الموضعي.",
                    chemical_advice = "ادمجي حمض الساليسيليك (BHA) بتركيز 2% لتقشير جدار المسام الداخلي، مع ضرورة تجنب خلط الريتينول في نفس الليلة مع أحماض التقشير لعدم تدمير حاجز البشرة الواقي.",
                    daily_habit = "استخدمي مناديل استخدام واحد ناعمة لتجفيف الوجه بدلاً من المناشف القطنية المشتركة، وقومي بتعقيم شاشة هاتفك المحمول يوميًا بمسحة كحولية.",
                    critical_note = "تجنبي تماماً فقع أو عصر البثور الملتهبة؛ لأن ذلك يدفع الصديد والعدوى لطبقات الأدمة العميقة مسبباً تصبغات مستعصية وندبات حفر صعبة العلاج!"
                )
            }
            concern.contains("جاف") || concern.contains("جفاف") || concern.contains("قشور") || concern.contains("Dryness") -> {
                com.example.data.model.PersonalizedSkincareTip(
                    concern = "الجفاف والقشور 🏜️",
                    molecular_cause = "نقص السيراميد والأحماض الدهنية الحرة في الطبقة الخارجية للبشرة يتسبب في تبخر الماء السريع (TEWL)، مما يسبب جفاف الخلايا وظهور تشققات طفيفة.",
                    chemical_advice = "طبقي سيروم حمض الهيالورونيك دائماً على بشرة رطبة (منداة بالماء) ثم اغلقيه مباشرة باستخدام مرطب دهني سميك يحتوي على السيراميد لحجز الرطوبة داخل الأدمة.",
                    daily_habit = "تجنبي غسل وجهكِ بالماء الحار جداً، وحافظي على شرب ما لا يقل عن 8 أكواب من الماء يومياً لتعزيز المرونة المائية للخلايا من الداخل.",
                    critical_note = "استخدام أحماض التقشير القوية أو مقشرات الوجه الفيزيائية (السكر / الحبيبات) على بشرة جافة ومقشرة سيفكك حاجزك المناعي تماماً؛ ركزي على الترميم أولاً!"
                )
            }
            concern.contains("تصبغ") || concern.contains("كلف") || concern.contains("بقع") || concern.contains("التصبغات") || concern.contains("Pigmentation") -> {
                com.example.data.model.PersonalizedSkincareTip(
                    concern = "التصبغات والبقع الداكنة ✨",
                    molecular_cause = "إشارة الأكسدة الناتجة عن الأشعة فوق البنفسجية أو الالتهابات (PIH) تنشط الخلايا الصباغية (Melanocytes) لإفراز كميات فائضة من الميلانين لحماية نواة الخلية.",
                    chemical_advice = "امزجي بين فيتامين C صباحاً لحماية مضاعفة من الأكسدة، وحمض الترانيكساميك أو ألفا أربوتين مساءً لتثبيط إنزيم النشوء الصباغي المسؤول عن تلون الخلايا.",
                    daily_habit = "تطبيق واقي شمس واسع النطاق (SPF 50) وتجديده كل ساعتين خارج المنزل هو السد المنيع الوحيد لمنع إثارة الخلايا الصباغية من جديد.",
                    critical_note = "التعرض لأشعة الشمس أو حرارة الأفران المباشرة بدون حماية كافية بعد استخدام المقشرات العلاجية سيقلب النتيجة إلى تصبغات أعمق وأكثر عناداً!"
                )
            }
            concern.contains("حس") || concern.contains("احمرار") || concern.contains("تهيج") || concern.contains("الحساسية") || concern.contains("Sens") -> {
                com.example.data.model.PersonalizedSkincareTip(
                    concern = "حساسية واحمرار الأدمة 🛡️",
                    molecular_cause = "تصدع الخلايا السطحية القرنية وتلاشي الطبقة الدهنية الواقية يؤدي لتعرية النهايات العصبية ومستقبلات المناعة، مما ينتج عنه تدفق دم موضعي غزير (الاحمرار).",
                    chemical_advice = "ابحثي عن مرطبات غنية بخلاصة السيكا (Centella Asiatica) مع مادة البانثينول (فيتامين B5) والزنك لتبريد تهيج البشرة وإعادة بناء النسيج التالف.",
                    daily_habit = "توقفي تماماً عن استخدام أي عطور كحولية أو زيوت عطرية مباشرة في روتينك، واعتمدي المنتجات المكتوب عليها 'خالٍ من العطور' و'مخصص للبشرة المتحسسة'.",
                    critical_note = "في فترات التهيج الحاد, أوقفي جميع سيرومات التقشير العلاجية والريتينول فوراً واقتصري على غسول مائي لطيف ومرطب ريفي مهدئ حتى يستقر الحاجز تماماً!"
                )
            }
            concern.contains("مسام") || concern.contains("دهون") || concern.contains("زهم") || concern.contains("المسام") || concern.contains("Pores") -> {
                com.example.data.model.PersonalizedSkincareTip(
                    concern = "المسام الواسعة والدهون 🔍",
                    molecular_cause = "تجاوب الخلايا الدهنية للنشاط الهرموني يزيد من إفراز الزهم الذي يتراكم ويهتك الأربطة المرنة المحيطة بفتحة المسام، مستدعياً تمددها الظاهري.",
                    chemical_advice = "يعد النياسيناميد (بتركيز 10%) هو المعيار الذهبي لتنظيم إفراز الغدد الدهنية وشد المسامات، إضافة إلى ماسك طين الكاولين مرة أسبوعياً لامتصاص الدهون الزائدة.",
                    daily_habit = "استخدمي دائماً مستحضرات ذات قواعد هلامية (Gel-based) أو مائية لتفادي غلق المسام بالشموع الثقيلة والزيوت المعدنية الكثيفة.",
                    critical_note = "تنظيف البشرة بعنف أو استخدام الكحول المنشف رغبة في التخلص من اللمعان سينعكس سلبياً؛ حيث سترسل البشرة إشارة طوارئ لإفراز دهون مضاعفة حمايةً لنفسها!"
                )
            }
            else -> {
                com.example.data.model.PersonalizedSkincareTip(
                    concern = "التجاعيد والخطوط الدقيقة ⏳",
                    molecular_cause = "تناقص إنتاج بروتينات الكولاجين والإيلاستين في طبقة الأدمة بمعدل 1% سنوياً بعد سن العشرين، وتصلب هذه الروابط بسبب الأكسدة والجفاف.",
                    chemical_advice = "الريتينويدات هي المقاتل الشرس للتجاعيد؛ ابدئي بسيروم الريتينول بتركيز منخفض (0.2%) لزيادة سرعة تجدد الخلايا، مع إقرانه بمركبات ببتيدية داعمة.",
                    daily_habit = "احرصي على النوم على وسادة من الحرير أو الساتان لتقليل الاحتكاك الميكانيكي المستمر بجلد الوجه، والحد من تجاعيد النوم التعبيرية.",
                    critical_note = "الريتينول سلاح ذو حدين؛ لا يطبق مطلقاً حول جفن العين دون غطاء عازل، ويمنع تماماً من قبل السيدات الحوامل والمرضعات لأسباب طبية قاطعة!"
                )
            }
        }
    }

    // Streak and history statistics
    private val _streakData = MutableStateFlow(StreakData(0, 0, emptySet()))
    val streakData: StateFlow<StreakData> = _streakData.asStateFlow()

    init {
        // Automatically calculate streak anytime completions update
        viewModelScope.launch {
            repository.allCompletions.collect {
                refreshStreaks()
            }
        }
        // Load the saved skincare analysis if any exists
        val savedAnalysis = loadSkincareAnalysisFromLocal()
        if (savedAnalysis != null) {
            _skincareAnalysisState.value = SkincareAnalysisUiState.Success(savedAnalysis)
        }
    }

    private fun saveSkincareAnalysisToLocal(recommendation: com.example.data.model.SkinAnalysisAndProductRecommendation) {
        try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("skincare_tracker_prefs", Context.MODE_PRIVATE)
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(com.example.data.model.SkinAnalysisAndProductRecommendation::class.java)
            val jsonText = adapter.toJson(recommendation)
            sharedPrefs.edit().putString("saved_skincare_analysis", jsonText).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSkincareAnalysisFromLocal(): com.example.data.model.SkinAnalysisAndProductRecommendation? {
        return try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("skincare_tracker_prefs", Context.MODE_PRIVATE)
            val jsonText = sharedPrefs.getString("saved_skincare_analysis", null) ?: return null
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(com.example.data.model.SkinAnalysisAndProductRecommendation::class.java)
            adapter.fromJson(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetSkincareAnalysis() {
        _skincareAnalysisState.value = SkincareAnalysisUiState.Initial
        try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences("skincare_tracker_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("saved_skincare_analysis").apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectDate(date: Date) {
        _selectedDateStr.value = sdf.format(date)
    }

    fun addNewTask(name: String, isAm: Boolean) {
        viewModelScope.launch {
            repository.addTask(name, isAm, isCustom = true)
        }
    }

    fun addNewProduct(name: String, brand: String, category: String, skinType: String, concern: String, description: String, activeIngredients: String) {
        viewModelScope.launch {
            repository.addProduct(name, brand, category, skinType, concern, description, activeIngredients)
        }
    }

    fun deleteProduct(product: SkincareProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun deleteTask(task: SkincareTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(taskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleCompletion(_selectedDateStr.value, taskId, isCompleted)
            refreshStreaks()
        }
    }

    fun syncFromAILog(routineAM: String, routinePM: String) {
        viewModelScope.launch {
            repository.syncClinicalRoutine(routineAM, routinePM)
            refreshStreaks()
        }
    }

    fun incrementWaterIntake() {
        viewModelScope.launch {
            val date = _selectedDateStr.value
            val currentLog = waterLogForSelectedDate.value
            if (currentLog == null) {
                repository.updateWaterLog(WaterLog(dateStr = date, cupsDrank = 1))
            } else {
                repository.updateWaterLog(currentLog.copy(cupsDrank = currentLog.cupsDrank + 1))
            }
        }
    }

    fun decrementWaterIntake() {
        viewModelScope.launch {
            val date = _selectedDateStr.value
            val currentLog = waterLogForSelectedDate.value
            if (currentLog != null && currentLog.cupsDrank > 0) {
                repository.updateWaterLog(currentLog.copy(cupsDrank = currentLog.cupsDrank - 1))
            }
        }
    }

    fun setWaterTarget(target: Int) {
        viewModelScope.launch {
            val date = _selectedDateStr.value
            val currentLog = waterLogForSelectedDate.value
            if (currentLog == null) {
                repository.updateWaterLog(WaterLog(dateStr = date, cupsDrank = 0, targetCups = target))
            } else {
                repository.updateWaterLog(currentLog.copy(targetCups = target))
            }
        }
    }

    private fun refreshStreaks() {
        viewModelScope.launch {
            val stats = repository.calculateStreaks(sdf.format(Date()))
            _streakData.value = stats
        }
    }
}

data class TrackableTaskItem(
    val task: SkincareTask,
    val isCompleted: Boolean
)
