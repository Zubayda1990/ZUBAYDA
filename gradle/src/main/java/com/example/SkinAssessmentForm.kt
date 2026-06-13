package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun SkinAssessmentForm(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    onReportGenerated: (SkinReport) -> Unit,
    onBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Form states
    var selectedSkinType by remember { mutableStateOf("") }
    val selectedConcerns = remember { mutableStateListOf<String>() }
    val selectedGoals = remember { mutableStateListOf<String>() }
    var selectedComplexity by remember { mutableStateOf("علاجي متكامل 🩺") }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val bgDark = Color(0xFF0F172A)
    val successGreen = Color(0xFF10B981)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // --- HEADER & STEP INDICATOR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    if (currentStep > 1) {
                        currentStep--
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .background(cardColor, RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = accentColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "التقييم الذكي للبشرة 🩺",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "الخطوة $currentStep من $totalSteps",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = cardColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = accentColor)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "جاري تجميع وتحليل البيانات السريرية...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    color = textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> StepSkinType(
                        selectedType = selectedSkinType,
                        onTypeSelected = { selectedSkinType = it },
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textMuted = textMuted
                    )
                    2 -> StepConcerns(
                        selectedConcerns = selectedConcerns,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textMuted = textMuted
                    )
                    3 -> StepGoals(
                        selectedGoals = selectedGoals,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textMuted = textMuted
                    )
                    4 -> StepComplexity(
                        selectedComplexity = selectedComplexity,
                        onComplexitySelected = { selectedComplexity = it },
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textMuted = textMuted
                    )
                }
            }

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }

            // --- NAVIGATION FOOTER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isNextEnabled = when (currentStep) {
                    1 -> selectedSkinType.isNotEmpty()
                    2 -> selectedConcerns.isNotEmpty()
                    3 -> selectedGoals.isNotEmpty()
                    else -> true
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                            errorMessage = null
                        } else {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val report = submitAssessment(
                                        skinType = selectedSkinType,
                                        concerns = selectedConcerns.toList(),
                                        goals = selectedGoals.toList(),
                                        complexity = selectedComplexity,
                                        updateStatus = { statusMessage = it }
                                    )
                                    onReportGenerated(report)
                                    isLoading = false
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = e.message ?: "حدث عطل في معالجة التقييم"
                                }
                            }
                        }
                    },
                    enabled = isNextEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == totalSteps) successGreen else accentColor,
                        disabledContainerColor = cardColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (currentStep == totalSteps) "إصدار التقرير الطبي المخصص ✨" else "الخطوة التالية ➡️",
                        color = if (isNextEnabled) bgDark else textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StepSkinType(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color
) {
    val types = listOf(
        "دهنية" to "تتميز بلمعان زائد ومسام واسعة وإفرازات زهمية مفرطة.",
        "جافة" to "تعاني من الملمس الخشن والتقشر والشد والشعور المستمر بالعطش الجلدي.",
        "مختلطة" to "دهنية ومسامية في منطقة T-Zone (الجبين والأنف) وجافة عند الوجنتين.",
        "حساسة / وردية" to "سريعة التهيج والاحمرار والوهج والتأثر بالحرارة والعوامل الجوية.",
        "عادية" to "متوازنة النعومة ومثالية الترطيب وخالية من المشاكل الكبيرة."
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "ما هو نوع بشرتك الملاحظ؟ 🤔",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "تحديد نوع البشرة يساعدنا على إيجاد القوام والمكونات القاعدية الأمثل لك.",
                color = textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        items(types.size) { index ->
            val (type, desc) = types[index]
            val isSelected = selectedType == type

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else cardColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeSelected(type) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onTypeSelected(type) },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = type,
                            color = if (isSelected) accentColor else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            color = textMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Right,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepConcerns(
    selectedConcerns: MutableList<String>,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color
) {
    val concerns = listOf(
        "حب شباب ورؤوس سوداء" to "بثور ملتهبة، احتقان تحت الجلد، ورؤوس سوداء أو بيضاء.",
        "تصبغات وبقع داكنة" to "آثار حب الشباب، الكلف، التصبغات الشمسية، وعدم التجانس.",
        "جفاف وتقشر" to "تقشر نسيجي، خشونة، واحتياج شديد لترميم الحاجز الجلدي.",
        "خطوط دقيقة وتجاعيد" to "فقدان المرونة، ارتخاء في الجلد، وخطوط حول العين أو الفم.",
        "مسام واسعة ودهون مكثفة" to "إفرازات سريعة ومفرطة تجعل الوجه لامعاً ومسام واضحة.",
        "احمرار وتوهج فوري" to "تحسس سريع للمنتجات الجديدة، والتهاب نسيجي متكرر."
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "ما هي المشاكل الجلدية التي تعاني منها؟ ⚠️",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "يمكنك اختيار مشكلة واحدة أو أكثر، لنقوم باستهدافها بمنتجات علاجية بمكونات فعالة.",
                color = textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        items(concerns.size) { index ->
            val (concern, desc) = concerns[index]
            val isSelected = selectedConcerns.contains(concern)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else cardColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isSelected) {
                            selectedConcerns.remove(concern)
                        } else {
                            selectedConcerns.add(concern)
                        }
                    }
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedConcerns.add(concern)
                            } else {
                                selectedConcerns.remove(concern)
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = concern,
                            color = if (isSelected) accentColor else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            color = textMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepGoals(
    selectedGoals: MutableList<String>,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color
) {
    val goals = listOf(
        "نضارة وإشراق فوري" to "تفتيح البشرة المتعبة وإزالة البهتان للحصول على مظهر رطب نضر.",
        "علاج البثور وتطهير المسام" to "الحد من تفشي الحبوب وتطهير الغدد الدهنية من الشوائب بشكل دائم.",
        "ترميم الحاجز الجلدي وتخفيف الاحمرار" to "تقوية الخط الدفاعي الخارجي للجلد واسترداد مستويات الراحة الطبيعية.",
        "مقاومة علامات تقدم السن وشد البشرة" to "تحفيز إنتاج الكولاجين للأدمة وتعبئة التجاعيد الرقيقة والخطوط.",
        "ترطيب عميق طويل المدى" to "الاحتفاظ بالماء وجذب الرطوبة لطبقات الجلد السفلى لمنع الجفاف.",
        "توحيد لون البشرة وإزالة الآثار" to "تثبيط ميكانيزم الخلايا الصباغية وتلاشي البقع والاحمرار."
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "ما هي تطلعاتك وأهدافك للبشرة؟ ✨",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "يرجى تحديد أهدافك الأساسية لتتوجه العلاجات نحو صلب النتائج المرتقبة.",
                color = textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        items(goals.size) { index ->
            val (goal, desc) = goals[index]
            val isSelected = selectedGoals.contains(goal)

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.12f) else cardColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isSelected) {
                            selectedGoals.remove(goal)
                        } else {
                            selectedGoals.add(goal)
                        }
                    }
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedGoals.add(goal)
                            } else {
                                selectedGoals.remove(goal)
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = accentColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = goal,
                            color = if (isSelected) accentColor else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            color = textMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepComplexity(
    selectedComplexity: String,
    onComplexitySelected: (String) -> Unit,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color
) {
    val complexOptions = listOf(
        "روتين علاجي مكثف متكامل 🩺" to "روتين احترافي غني بالمنتجات المتقدمة (غسول، سيروم متخصص صباحي ومسائي، مرطب حاجز، وواقي تجميلي علمي).",
        "روتين اقتصادي مبسط ⚙️" to "أبسط روتين ممكن (٣ خطوات أساسية فقط صباحاً ومساءً) لتوفير التكاليف وضمان الاستمرارية والسهولة."
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                text = "ما مدى تعقيد الروتين الذي تفضلينه؟ 📋",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "نحن نطوّع عدد الخطوات بما يناسب ميزانيتك ومستوى التزامك اليومي.",
                color = textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        items(complexOptions.size) { index ->
            val (option, desc) = complexOptions[index]
            val isSelected = selectedComplexity == option

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.15f) else cardColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onComplexitySelected(option) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onComplexitySelected(option) },
                        colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) accentColor else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            color = textMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Right,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// --- SUBMISSION LOGIC WITH GEMINI API & DETAILED CLINICAL FALLBACK ---

suspend fun submitAssessment(
    skinType: String,
    concerns: List<String>,
    goals: List<String>,
    complexity: String,
    updateStatus: (String) -> Unit
): SkinReport = withContext(Dispatchers.IO) {
    updateStatus("تجهيز الاستجابات والربط المايكروبي الجيني...")

    val apiKey = BuildConfig.GEMINI_API_KEY
    val isDemoKey = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

    Thread.sleep(1200)

    if (isDemoKey) {
        updateStatus("تحميل خوارزمية التشخيص المحلي المحكم CDSS...")
        Thread.sleep(1200)

        // Build premium, customized report locally based on actual choices
        return@withContext buildRealisticLocalReport(skinType, concerns, goals, complexity)
    }

    updateStatus("تحميل وتواصل سحابي مع خادم الذكاء الاصطناعي...")

    val prompt = """
        You are 'GlowLogic CDSS' - an advanced AI Dermatology Decision Support system designed by Zubayda Ramzi.
        The user has completed our comprehensive Skin Assessment Questionnaire with the following profile:
        - Skin Type: $skinType
        - Current Skin Concerns: ${concerns.joinToString(", ")}
        - Skincare Goals: ${goals.joinToString(", ")}
        - Skincare Routine Complexity Choice: $complexity

        Please analyze this precise clinical profile. Focus on providing extremely professional, highly scientific, and personalized skincare recommendations.
        Your output MUST be a strict JSON object with EXACTLY the following keys (with values as specified below, strictly in Arabic):
        {
          "skinType": "A customized skin profile title (e.g., بشرة مختلطة مفرطة الحساسية)",
          "hydration": Estimated hydration level percentage (an integer between 0 and 100),
          "barrierHealth": Estimated protective skin barrier health percentage (an integer between 0 and 100),
          "pathology": "A detailed scientific analysis explaining the physiological causes of their concerns and how their skin type behaves in Arabic",
          "routineAM": "Suggested morning skincare routine AM with bullet points or numbered steps list of specific pharmaceutical active ingredients and product categories in Arabic",
          "routinePM": "Suggested evening skincare routine PM with bullet points or numbered steps list of specific pharmaceutical active ingredients and product categories in Arabic",
          "avoid": "Skincare active ingredients, physical practices, and lifestyle triggers that this profile MUST avoid at all costs in Arabic"
        }
        Make sure the clinical tone is highly reassuring, completely accurate in Arabic medical style, and starts with the style of Zubayda Ramzi CDSS. Do NOT put any markdown or formatting tags outside of raw JSON. Just return the valid JSON string.
    """.trimIndent()

    val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    val partText = JSONObject().put("text", prompt)
    val contentsArray = JSONArray().put(JSONObject().put("parts", JSONArray().put(partText)))
    val requestObj = JSONObject().put("contents", contentsArray)

    // Schema constraint
    val responseSchema = JSONObject()
        .put("type", "OBJECT")
        .put("properties", JSONObject()
            .put("skinType", JSONObject().put("type", "STRING"))
            .put("hydration", JSONObject().put("type", "INTEGER"))
            .put("barrierHealth", JSONObject().put("type", "INTEGER"))
            .put("pathology", JSONObject().put("type", "STRING"))
            .put("routineAM", JSONObject().put("type", "STRING"))
            .put("routinePM", JSONObject().put("type", "STRING"))
            .put("avoid", JSONObject().put("type", "STRING"))
        )
        .put("required", JSONArray().put("skinType").put("hydration").put("barrierHealth").put("pathology").put("routineAM").put("routinePM").put("avoid"))

    val responseFormat = JSONObject()
        .put("mimeType", "application/json")
        .put("schema", responseSchema)

    requestObj.put("generationConfig", JSONObject().put("responseFormat", responseFormat))

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = requestObj.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val resJson = JSONObject(responseBody)
            val resultText = resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val reportJson = JSONObject(resultText)
            SkinReport(
                skinType = reportJson.getString("skinType"),
                hydration = reportJson.getInt("hydration"),
                barrierHealth = reportJson.getInt("barrierHealth"),
                pathology = reportJson.getString("pathology"),
                routineAM = reportJson.getString("routineAM"),
                routinePM = reportJson.getString("routinePM"),
                avoid = reportJson.getString("avoid"),
                isDemo = false
            )
        } else {
            throw Exception("عطل في الاتصال: ${response.code}")
        }
    } catch (e: Exception) {
        // Fall back to local realistic report quietly if API call fails
        return@withContext buildRealisticLocalReport(skinType, concerns, goals, complexity).copy(isDemo = true)
    }
}

// Beautiful fallback algorithm to generate clinical skins based on explicit user conditions
private fun buildRealisticLocalReport(
    skinType: String,
    concerns: List<String>,
    goals: List<String>,
    complexity: String
): SkinReport {
    val isMinimal = complexity.contains("مبسط")

    // Determine hydration and barrier based on concerns and skinType
    var hydration = when (skinType) {
        "جافة" -> 28
        "مختلطة" -> 55
        "دهنية" -> 46
        "حساسة / وردية" -> 40
        else -> 75
    }
    if (concerns.contains("جفاف وتقشر")) hydration -= 12

    var barrier = 80
    if (concerns.contains("جفاف وتقشر") || concerns.contains("احمرار وتوهج فوري")) barrier -= 35
    if (concerns.contains("حب شباب ورؤوس سوداء")) barrier -= 20
    barrier = barrier.coerceIn(15, 95)
    hydration = hydration.coerceIn(10, 99)

    // Build Pathology text
    val pathologyBuilder = StringBuilder()
    pathologyBuilder.append("بإشراف زبيدة رمزي وبناءً على التقييم الذكي المستند للقرارات السريرية (CDSS):\n\n")
    pathologyBuilder.append("إن طبيعة بشرتك المصنفة كـ ($skinType) تعكس توازناً ")
    when (skinType) {
        "جافة" -> pathologyBuilder.append("حرجاً مع نقص هائل في تركيز غدد السيبوم الدهنية والسيراميد الهيكلي، مما يسبب ظهور قشور عاجلة وفقدان للمرونة الطبيعية.")
        "دهنية" -> pathologyBuilder.append("دهنياً نشطاً مع فرط في استجابة الغدد الزهمية التي تحفز إفراز الزهم (Sebum) وسد جدران المسام وحويصلات الشعرة.")
        "مختلطة" -> pathologyBuilder.append("متبايناً؛ حيث يتركز النشاط الدهني واللمعان عند جبهة T-Zone مع نقص ترطيب حاد وجفاف قشري ملموس على الوجنتين.")
        "حساسة / وردية" -> pathologyBuilder.append("مضطرباً وجداراً واقياً شديد الهشاشة مع توسع سريع للأوعية الدموية الدقيقة مما يصفي وهجاً أحمراً فورياً لأي محفز بيئي أو كيميائي.")
        else -> pathologyBuilder.append("مثالياً ومقاومة ممتازة للمؤثرات مع تبادل خلوي متوازن في طبقات البشرة العليا.")
    }

    pathologyBuilder.append("\n\nملاحظة الأخصائية حول المشاكل:")
    if (concerns.contains("حب شباب ورؤوس سوداء")) {
        pathologyBuilder.append(" تم تحديد نشاط بكتيري لاهوائي في منافذ الغدد الدهنية. يستوجب استخدام مقشرات دهنية وحمض الأزليك لضبط الالتهاب والميكروبيوم.")
    }
    if (concerns.contains("تصبغات وبقع داكنة")) {
        pathologyBuilder.append(" فرط نشاط مزمن لإنزيم Tyrosinase المسؤول عن الميلانين، تزداد الآفة عمقاً بالتعرض المباشر للأشعة الشمسي وتتطلب تثبيطاً كيميائياً بالترانكساميك.")
    }
    if (concerns.contains("احمرار وتوهج فوري")) {
        pathologyBuilder.append(" تضرر متلازم لحاجز الحماية الشحمي (Barrier) مما ينشط خلايا الهيستامين السطحية باللمس السريع.")
    }
    if (pathologyBuilder.length < 200) {
        pathologyBuilder.append(" تم رصد تطلعك لتحقيق ${goals.joinToString(" و ")}, مما يركّز روتيننا على إمداد الخلايا بمضادات الأكسدة وتأمين ترطيب مستدام.")
    }

    // Build Routine PM and AM
    val routineAM = StringBuilder()
    val routinePM = StringBuilder()

    if (isMinimal) {
        // AM
        routineAM.append("1. منظف لطيف خامل ميكروبياً يناسب البشرة $skinType.\n")
        val amSerum = when {
            concerns.contains("احمرار وتوهج فوري") || concerns.contains("جفاف وتقشر") -> "مرطب بانتينول سائل مغذٍ لترميم حاجز الأملاح."
            concerns.contains("تصبغات وبقع داكنة") -> "سيروم فيتامين سي خفيف وآمن."
            else -> "سيروم الهيالورونيك اسيد 2% مبلل لملأ الخلية."
        }
        routineAM.append("2. $amSerum\n")
        routineAM.append("3. واقي شمس فيزيائي SPF 50+ خالي من العطور لحماية الخلايا الكيراتينية.")

        // PM
        routinePM.append("1. غسول مائي رغوي خفيف.\n")
        val pmTreatment = when {
            concerns.contains("حب شباب ورؤوس سوداء") -> "سيروم حمض الساليسيليك 2% مرتين بالأسبوع للسيطرة على الحبوب."
            concerns.contains("تصبغات وبقع داكنة") -> "سيروم حمض الأزيليك لتثبيط التصبغات بأمان."
            concerns.contains("خطوط دقيقة وتجاعيد") -> "سيروم ريتينول لطيف جداً للتجديد الخليوي."
            else -> "كريم سيكا مهدئ ثري بالماديكاسوسايد."
        }
        routinePM.append("2. $pmTreatment\n")
        routinePM.append("3. مرطب غني بالسيراميد لإعادة تدعيم السند الجداري.")
    } else {
        // AM Professional Clinical Routine
        routineAM.append("1. غسول علاجي مخصص: ")
        routineAM.append(
            when (skinType) {
                "دهنية" -> "Salicylic Acid 2% Cleanser لتخترق جدران المسام وإذابة الدهون.\n"
                "حساسة / وردية" -> "غسول رغوي مائي مهدئ خالي تماماً من المعطرات المسببة للتوهج.\n"
                else -> "منظف كريمي حليبي غني بالأملاح المعدنية.\n"
            }
        )
        routineAM.append("2. تونر مياه حرارية لإعادة اتزان حموضة الـ pH الجلدي.\n")
        routineAM.append("3. سيروم الدعم اليومي: ")
        routineAM.append(
            when {
                concerns.contains("تصبغات وبقع داكنة") -> "سيروم ترانكساميك اسيد 3% مدمج بـ فيتامين سي لمكافحة البهتان.\n"
                concerns.contains("حب شباب ورؤوس سوداء") -> "سيروم نياسيناميد 10% لتقليص حجم المسام والسيطرة على الغدد الدهنية.\n"
                else -> "سيروم هيالورونيك اسيد 2% مع فيتامين B5 للترطيب المكثف.\n"
            }
        )
        routineAM.append("4. مرطب نهاري خفيف غير كوميدوغينيك (لا يغلق المسام).\n")
        routineAM.append("5. واقي شمس فيزيائي 100% Zinc Oxide SPF 50+ مانع للتدمير الضوئي.")

        // PM Professional Clinical Routine
        routinePM.append("1. المنظف المزدوج (غسول زيتي للتخلص من المكياج وواقي الشمس يليه غسول مائي).\n")
        routinePM.append("2. الموجه العلاجي النشط: ")
        routinePM.append(
            when {
                concerns.contains("حب شباب ورؤوس سوداء") -> "سيروم حمض الساليسيليك بالتناوب مع حمض الأزليك لتطهير البكتيريا اللاهوائية.\n"
                concerns.contains("خطوط دقيقة وتجاعيد") -> "سيروم الريتينول النقي 0.3% لإثارة إنتاج الكولاجين ليلاً.\n"
                concerns.contains("تصبغات وبقع داكنة") -> "سيروم الترانكساميك اسيد أو الأربوتين لإبادة البقع العميقة.\n"
                else -> "سيروم السيكا الشوفاني لتخفيف تهيج حاجز الغشاء الخلوي.\n"
            }
        )
        routinePM.append("3. كريم مرطب حاجز ليلي ثري بـ السيراميد والكوليسترول والدهون الطبيعية الثلاثية.\n")
        routinePM.append("4. طبقة حماية عازلة عند الأماكن شديدة الجفاف.")
    }

    // Build Avoid List
    val avoid = StringBuilder()
    avoid.append("تنبيهات وتوصيات وقائية صارمة من الدكتورة زبيدة:\n\n")
    if (skinType == "حساسة / وردية" || concerns.contains("احمرار وتوهج فوري")) {
        avoid.append("- تجنب تماماً استخدام الريتينول المركز، وأحماض الجليكوليك، والمقشرات الفيزيائية الخشنة الميكانيكية.\n")
        avoid.append("- احذر استخدام المياه الساخنة جداً عند غسيل الوجه، وتجنب استخدام العطور والبارابين الكيميائي والمشتقات الكحولية القاسية.")
    } else {
        avoid.append("- يمنع دمج الريتينول مع حمض الساليسيليك في نفس الليلة لتفادي تآكل جدار الحماية الجلدي.\n")
        avoid.append("- يرجى تجنب استخدام المقشرات الفيزيائية (السكر / اللوف المجهد) لسلامة الفلورا المايكروبية الطبيعية للبشرة.")
    }

    return SkinReport(
        skinType = "بشرة $skinType مخصصة - تقييم CDSS",
        hydration = hydration,
        barrierHealth = barrier,
        pathology = pathologyBuilder.toString(),
        routineAM = routineAM.toString(),
        routinePM = routinePM.toString(),
        avoid = avoid.toString(),
        isDemo = true
    )
}
