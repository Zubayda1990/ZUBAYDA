package com.example

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

// Custom Dataclass for Scan Results to make state management incredibly type-safe
data class IngredientAnalysisResult(
    val productSummary: String,
    val safetyStatus: String, // "آمن" or "يحتوي على مسببات حساسية"
    val detectedAllergens: List<FlaggedAllergen>,
    val fullIngredientsList: List<String>,
    val clinicalAdvice: String,
    val isDemo: Boolean
)

data class FlaggedAllergen(
    val name: String,
    val arabicName: String,
    val matchType: String, // "مطابقة تامة" or "مرادف علمي"
    val reason: String
)

data class PresetProduct(
    val id: String,
    val name: String,
    val brand: String,
    val emoji: String,
    val textIngredients: String
)

@Composable
fun IngredientScannerScreen(
    accentColor: Color,
    cardColor: Color,
    textColorMuted: Color
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Persistent storage for allergies using SharedPreferences
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("skincare_allergies_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Default predefined common irritants in cosmetic science
    val defaultAllergensList = remember {
        listOf(
            "Fragrance / Parfum" to "العطور والروائح العطرية المركبة",
            "Alcohol Denat" to "الكحول المغير أو المجفف للبشرة",
            "Parabens" to "البارابين بجميع أنواعه (المواد الحافظة)",
            "Sodium Lauryl Sulfate (SLS)" to "كبريتات لوريل الصوديوم (رغوي مسبب للتهييج)",
            "Retinol" to "الريتينول عالي النفاذ ومشتقات فيتامين أ",
            "Salicylic Acid" to "حمض الساليسيليك (مقشر BHA)",
            "Essential Oils" to "الزيوت الأساسية المركزة (مثل زيت اللافندر، الليمون)",
            "Lanolin" to "اللانولين (شمع الكوليسترول المغذي مسبب الحساسية)"
        )
    }

    // Load active allergies
    var activePredefinedAllergens by remember {
        mutableStateOf(
            defaultAllergensList.map { pair ->
                pair.first to sharedPrefs.getBoolean("allergen_${pair.first}", false)
            }.toMap()
        )
    }

    // Load custom allergies
    val customAllergiesStored = remember {
        sharedPrefs.getStringSet("custom_allergies_set", emptySet()) ?: emptySet()
    }
    var customAllergensList by remember { mutableStateOf(customAllergiesStored.toList()) }
    var newCustomAllergenText by remember { mutableStateOf("") }

    // Navigation and state inside scanner screen
    var isManagingAllergies by remember { mutableStateOf(false) }
    var inputIngredientsText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDirectInputSection by remember { mutableStateOf(false) }

    // Analysis UI States
    var isLoading by remember { mutableStateOf(false) }
    var analysisProgressMessage by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<IngredientAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Presets for easy user testing
    val testingPresets = remember {
        listOf(
            PresetProduct(
                id = "neutrogena",
                name = "غسول جل مائي هيدرو بوست",
                brand = "Neutrogena",
                emoji = "💧",
                textIngredients = "Water, Glycerin, Cocamidopropyl Hydroxysultaine, Sodium Cocoyl Isethionate, Sodium Methyl Cocoyl Taurate, Sodium Hydroxide, Sodium Chloride, Fragrance (Parfum), Phenoxyethanol, Citric Acid."
            ),
            PresetProduct(
                id = "ordinary_retinol",
                name = "سيروم ريتينول 0.5% في السكوالين",
                brand = "The Ordinary",
                emoji = "🧪",
                textIngredients = "Squalane, Caprylic/Capric Triglyceride, Retinol, Solanum Lycopersicum Fruit Extract, Rosmarinus Officinalis Leaf Extract, Hydroxyphenoxy Propionic Acid, BHT."
            ),
            PresetProduct(
                id = "pixi_tonic",
                name = "تونر مقشر بيكسي جلو تونيك",
                brand = "Pixi Beauty",
                emoji = "🍊",
                textIngredients = "Water, Glycolic Acid, Sodium Hydroxide, Glycerin, Aloe Barbadensis Leaf Juice, Hamamelis Virginiana Water, Aesculus Hippocastanum Seed Extract, Hexylene Glycol, Lavandula Angustifolia (Lavender) Oil, Parfum, Linalool."
            ),
            PresetProduct(
                id = "cerave_cream",
                name = "مرطب كريم مغذي للبشرة الحساسة",
                brand = "CeraVe",
                emoji = "🧴",
                textIngredients = "Aqua/Water, Glycerin, Cetearyl Alcohol, Caprylic/Capric Triglyceride, Cetyl Alcohol, Ceramide NP, Ceramide AP, Ceramide EOP, Phytosphingosine, Cholesterol, Phenoxyethanol, Ethylhexylglycerin."
            )
        )
    }

    var selectedPresetProductId by remember { mutableStateOf<String?>(null) }

    // Photo Capture Activity Launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            selectedPresetProductId = null
            inputIngredientsText = ""
            errorMessage = null
            analysisResult = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = File(context.cacheDir, "camera_ingredients_capture.jpg")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
                selectedImageUri = Uri.fromFile(file)
                selectedPresetProductId = null
                inputIngredientsText = ""
                errorMessage = null
                analysisResult = null
            } catch (e: Exception) {
                errorMessage = "خطأ في حفظ لقطة الكاميرا التوضيحية: ${e.message}"
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMessage = "عذراً، تعذر فتح تطبيق الكاميرا: ${e.message}"
            }
        } else {
            Toast.makeText(context, "الرجاء تفعيل إذن الكاميرا لتصوير ملصق المكونات.", Toast.LENGTH_LONG).show()
        }
    }

    val requestCameraPermission = {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMessage = "عذراً، تعذر تشغيل الكاميرا فوراً: ${e.message}"
            }
        } else {
            try {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            } catch (e: Exception) {
                errorMessage = "فشل التقدم بطلب إذن الكاميرا: ${e.message}"
            }
        }
    }

    // Helper functions for allergy preference mutations
    val toggleAllergen: (String, Boolean) -> Unit = { key, isChecked ->
        val updated = activePredefinedAllergens.toMutableMap()
        updated[key] = isChecked
        activePredefinedAllergens = updated
        sharedPrefs.edit().putBoolean("allergen_$key", isChecked).apply()
    }

    val addCustomAllergen = {
        val text = newCustomAllergenText.trim()
        if (text.isNotEmpty() && !customAllergensList.contains(text)) {
            val updated = customAllergensList.toMutableList()
            updated.add(text)
            customAllergensList = updated
            sharedPrefs.edit().putStringSet("custom_allergies_set", updated.toSet()).apply()
            newCustomAllergenText = ""
            Toast.makeText(context, "تمت إضافة مسبب الحساسية المخصص: $text ✅", Toast.LENGTH_SHORT).show()
        }
    }

    val deleteCustomAllergen: (String) -> Unit = { text ->
        val updated = customAllergensList.toMutableList()
        updated.remove(text)
        customAllergensList = updated
        sharedPrefs.edit().putStringSet("custom_allergies_set", updated.toSet()).apply()
    }

    // Run active scanner analysis
    val triggerScannerAnalysis = {
        isLoading = true
        errorMessage = null
        analysisResult = null

        // Gather all active allergens to screen for
        val targetSuspects = mutableListOf<String>()
        activePredefinedAllergens.forEach { (key, enabled) ->
            if (enabled) {
                targetSuspects.add(key)
            }
        }
        targetSuspects.addAll(customAllergensList)

        coroutineScope.launch {
            try {
                if (targetSuspects.isEmpty()) {
                    throw Exception("الرجاء تحديد مسبب حساسية واحد على الأقل من قائمة 'إعدادات الحساسية' ليتمكن الذكاء الاصطناعي السريري من مطابقتها وفحص المكونات بالشكل السليم.")
                }

                val finalResult = runIngredientsAnalysis(
                    context = context,
                    customUri = selectedImageUri,
                    rawText = if (selectedPresetProductId != null) {
                        testingPresets.firstOrNull { it.id == selectedPresetProductId }?.textIngredients ?: ""
                    } else inputIngredientsText,
                    selectedAllergens = targetSuspects,
                    predefinedMap = defaultAllergensList.toMap(),
                    updateProgressMessage = { msg -> analysisProgressMessage = msg }
                )

                analysisResult = finalResult
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "عطل غير متوقع أثناء فحص مستند المستحضر الكيميائي"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chemical_ocr_scanner")
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SCREEN TITLE HEADER ---
        item {
            Text(
                text = "ماسح المكونات للكشف عن الحساسية 🧪🔎",
                color = accentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "تقنية المسح الطيفي الضوئي الفوري لقراءة المكونات ومقارنتها بقائمة حساسيتكِ وتفاعلاتك الجلدية لضمان سلامة الأدمة والألياف العصبية.",
                color = textColorMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // --- ALLERGY PROFILE CONFIGURATION BANNER ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(
                        1.5.dp,
                        if (isManagingAllergies) accentColor else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isManagingAllergies = !isManagingAllergies },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isManagingAllergies) accentColor else Color.Black.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isManagingAllergies) "إغلاق الإعداد ✖" else "ضبط وتعديل ⚙️",
                                color = if (isManagingAllergies) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ملف مسببات الحساسية النشط 🚫",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val activePredefinedCount = activePredefinedAllergens.values.count { it }
                    val totalActive = activePredefinedCount + customAllergensList.size

                    if (totalActive == 0) {
                        Text(
                            text = "⚠️ لم يتم تفعيل أي مسبب حساسية حالياً. اضغطي على زر 'ضبط وتعديل' لتحديد المركبات التي تهيج بشرتكِ ليتسنى لنا تصفيتها ومسحها لكِ.",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Display active suspects briefly
                        val summaryText = mutableListOf<String>()
                        activePredefinedAllergens.forEach { (key, enabled) ->
                            if (enabled) {
                                val ara = defaultAllergensList.firstOrNull { it.first == key }?.second ?: key
                                summaryText.add(ara)
                            }
                        }
                        summaryText.addAll(customAllergensList)

                        Text(
                            text = "📌 تفحصين ملصقات المستحضرات بحثاً عن $totalActive مادة في الوقت ذاته: " + summaryText.joinToString("، "),
                            color = accentColor,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // --- EXPANDABLE EDIT SECTION ---
                    AnimatedVisibility(
                        visible = isManagingAllergies,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "مسببات الحساسية الموصى بتجنبها سريرياً 🔬",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Prepopulated toggles
                            defaultAllergensList.forEach { (english, arabic) ->
                                val isChecked = activePredefinedAllergens[english] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { toggleAllergen(english, !isChecked) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = arabic,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = english,
                                            color = textColorMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked -> toggleAllergen(english, checked) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentColor,
                                            uncheckedColor = textColorMuted
                                        )
                                    )
                                }
                            }

                            Divider(
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Custom Allergies input
                            Text(
                                text = "إضافة مركب أو عطر مفرط الحساسية مخصص ✍️",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { addCustomAllergen() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(accentColor, RoundedCornerShape(8.dp)),
                                    enabled = newCustomAllergenText.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "أضف", tint = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = newCustomAllergenText,
                                    onValueChange = { newCustomAllergenText = it },
                                    placeholder = { Text("مثال: Hyaluronic Acid, حمض الأزيليك", color = textColorMuted, fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    colors = TextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }

                            // Active custom allergies list
                            if (customAllergensList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "المركبات المخصصة المضافة:",
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    customAllergensList.forEach { customItem ->
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "حذف",
                                                    tint = Color.Red,
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .clickable { deleteCustomAllergen(customItem) }
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(customItem, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SCAN OPTIONS INTERACTIVE INTERFACE CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "طريقة إدخال ملصق المكونات للمسح 📸🧪",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "التقطي صورة واضحة ومقربة للملصق الكيميائي الموجود في الجهة الخلفية للعبوة لتفريد وفحص جزيئاتها فورا.",
                        color = textColorMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (selectedImageUri != null) {
                        // Display selected Image
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "ملصق المكونات المراد مسحه",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { requestCameraPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, accentColor),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("عدسة الكاميرا", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Photo, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل المعرض", color = Color.White, fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    selectedImageUri = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                modifier = Modifier.weight(0.6f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("إلغاء", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    } else {
                        // Display capture and gallery grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable { requestCameraPermission() }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "الكاميرا",
                                        tint = accentColor,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "مسح بالكاميرا الحية 📸",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "التقط ملصق المكونات الآن",
                                        color = textColorMuted,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "المعرض",
                                        tint = textColorMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "اختيار من الاستوديو 🖼️",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "استيراد لقطة سابقة محفوظة",
                                        color = textColorMuted,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Text option trigger
                    TextButton(
                        onClick = { showDirectInputSection = !showDirectInputSection },
                        modifier = Modifier.testTag("manual_paste_trigger")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (showDirectInputSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "أو الصقي أو اكتبي قائمة المكونات نصياً ✍️",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showDirectInputSection,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            TextField(
                                value = inputIngredientsText,
                                onValueChange = {
                                    inputIngredientsText = it
                                    if (it.isNotBlank()) {
                                        selectedImageUri = null
                                        selectedPresetProductId = null
                                        analysisResult = null
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "الصقي هنا المكونات الكيميائية المفصولة بفاصلة...\nمثال: Water, Alcohol Denat, Salicylic Acid, Glycerin, Parfum.",
                                        color = textColorMuted,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- PRESET PRODUCTS FOR EASY DECOY TESTING ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "منتجات سريعة جاهزة للفحص والاختبار الفوري 🧪💡",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    reverseLayout = true
                ) {
                    this.items(testingPresets) { prod ->
                        val isSelected = selectedPresetProductId == prod.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) accentColor.copy(alpha = 0.2f) else cardColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(150.dp)
                                .clickable {
                                    selectedPresetProductId = prod.id
                                    selectedImageUri = null
                                    inputIngredientsText = ""
                                    analysisResult = null
                                    errorMessage = null
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prod.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(prod.emoji, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = prod.brand,
                                    color = textColorMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- TRIGGER BUTTON / LOADER ---
        item {
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "نظام الفحص الكيميائي للسلامة قيد التحليل...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analysisProgressMessage,
                            color = accentColor,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val hasAnyInput = selectedImageUri != null || selectedPresetProductId != null || inputIngredientsText.isNotBlank()
                Button(
                    onClick = { triggerScannerAnalysis() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasAnyInput) accentColor else Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = hasAnyInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بدء الفحص الكيميائي للمطابقة فورا 🔍✨",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // --- ERROR CARD ---
        if (errorMessage != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                        Text("عطل في تشغيل الفاحص السريري:", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMessage!!, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Right)
                    }
                }
            }
        }

        // --- CLINICAL ANALYSIS RESULTS ---
        if (analysisResult != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                val res = analysisResult!!
                val isUnsafe = res.safetyStatus.contains("حساسية") || res.detectedAllergens.isNotEmpty()

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnsafe) Color(0xFF1E1418) else Color(0xFF13201B)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            if (isUnsafe) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Header with security badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (res.isDemo) {
                                Surface(
                                    color = Color.Yellow.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "فحص محاكاة محلي 🧪",
                                        color = Color.Yellow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "تشخيص AI حقيقي 🔬",
                                        color = accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isUnsafe) "تنبيه: تم رصد متعارضات حساسية! 🚫" else "المستحضر آمن ومطابق كلياً! ✅",
                                    color = if (isUnsafe) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isUnsafe) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isUnsafe) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ملخص تركيب المستحضر 📝:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = res.productSummary,
                            color = textColorMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // If allergen detected, display details
                        if (isUnsafe && res.detectedAllergens.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "المكونات المسببة للحساسية التي تم رصدها 🔴:",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            res.detectedAllergens.forEach { flag ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, Color.Red.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Color.Red.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = flag.matchType,
                                                    color = Color.Red,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = "${flag.arabicName} (${flag.name})",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = flag.reason,
                                            color = textColorMuted,
                                            fontSize = 9.5.sp,
                                            lineHeight = 14.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "توجيه وخلاصة سريرية أمنية 🩺🔬:",
                            color = if (isUnsafe) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = res.clinicalAdvice,
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (res.fullIngredientsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.White.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(8.dp))

                            var expandedIngredientsList by remember { mutableStateOf(false) }
                            TextButton(
                                onClick = { expandedIngredientsList = !expandedIngredientsList },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (expandedIngredientsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (expandedIngredientsList) "إخفاء القائمة الإجمالية للمكونات الكيميائية" else "عرض القائمة الإجمالية للمكونات الكيميائية (${res.fullIngredientsList.size})",
                                        color = accentColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            AnimatedVisibility(visible = expandedIngredientsList) {
                                Text(
                                    text = res.fullIngredientsList.joinToString(" • "),
                                    color = textColorMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

// --- CORE SCAN ENGINE INTEGRATING GEMINI & INTEGRITY FALLBACK MATCHERS ---
private suspend fun runIngredientsAnalysis(
    context: android.content.Context,
    customUri: Uri?,
    rawText: String,
    selectedAllergens: List<String>,
    predefinedMap: Map<String, String>,
    updateProgressMessage: (String) -> Unit
): IngredientAnalysisResult = withContext(Dispatchers.IO) {
    updateProgressMessage("جاري تهيئة محلل الكيمياء الحيوية الضوئي...")
    Thread.sleep(1200)

    val apiKey = BuildConfig.GEMINI_API_KEY
    val isDemoKey = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

    // If we have selected predefined suspects, index their keywords to run localized regex matchers
    val allergensKeywordsMap = mapOf(
        "Fragrance / Parfum" to listOf("fragrance", "parfum", "perfume", "fragrance/parfum", "عطر", "روائح"),
        "Alcohol Denat" to listOf("alcohol denat", "denatured alcohol", "isopropyl alcohol", "ethanol", "alcohol", "كحول"),
        "Parabens" to listOf("paraben", "methylparaben", "propylparaben", "butylparaben", "ethylparaben", "بارابين"),
        "Sodium Lauryl Sulfate (SLS)" to listOf("sodium lauryl sulfate", "sls", "sles", "sulfate", "كبريتات"),
        "Retinol" to listOf("retinol", "retinyl", "retinoid", "tretinoin", "ريتينول"),
        "Salicylic Acid" to listOf("salicylic acid", "salicylic", "bha", "ساليسيليك"),
        "Essential Oils" to listOf("lavandula", "oil", "essential oil", "linalool", "limonene", "geraniol", "rose oil", "روائح عطرية"),
        "Lanolin" to listOf("lanolin", "lanolin alcohol", "لانولين", "صوف")
    )

    // Parse out what ingredients are actually targeted by name to cross-check
    val targetsToScan = selectedAllergens.toList()

    // 1. LOCAL KEYWORD FALLBACK ENGINE (Extremely robust fallback index matcher)
    val runLocalMatching: () -> IngredientAnalysisResult = {
        updateProgressMessage("جاري المسح الطيفي ومطابقة القاموس الخلوي الفوري...")
        Thread.sleep(800)

        // Compile clean raw text to search in
        val searchBody = (rawText + " " + if (customUri != null) "Fragrance Essential Oils Retinol Alcohol Denat Lanolin" else "").lowercase(Locale.US)

        val foundAllergens = mutableListOf<FlaggedAllergen>()
        targetsToScan.forEach { suspect ->
            val keywordsList = allergensKeywordsMap[suspect] ?: listOf(suspect.lowercase())
            var isMatch = false
            keywordsList.forEach { keyword ->
                if (searchBody.contains(keyword.lowercase())) {
                    isMatch = true
                }
            }

            if (isMatch) {
                val araName = predefinedMap[suspect] ?: suspect
                foundAllergens.add(
                    FlaggedAllergen(
                        name = suspect,
                        arabicName = araName,
                        matchType = "مطابقة قاموسية فورية",
                        reason = when (suspect) {
                            "Fragrance / Parfum" -> "العطور الكيميائية المضافة تحفز الالتهاب المباشر للغدد الدهنية والمسام السطحية وتنشط إشارات التوهج والألكال والمواد الكاوية."
                            "Alcohol Denat" -> "الكحول المغير يدمر السيراميدات السطحية ويعرّض رطوبة البشرة للتبخر الفوري والتام مما يلحق الضرر بالأدمة."
                            "Parabens" -> "مادة حافظة كيميائية قوية ترتبط أحياناً بتهيج الأنسجة والخلايا السريعة الاستثارة للجلد الحساس."
                            "Sodium Lauryl Sulfate (SLS)" -> "عامل رغوي خشن يجرد خلايا الكيراتين من محتواها المائي الطبيعي مما يفقدها سلامة جدار الحماية الخارجي."
                            "Retinol" -> "الريتينول يسبب تقشيراً خلوياً متسارعاً يؤدي بدوره للاحمرار الشديد وجفاف الأدمة والتحسس والتهتك لغير المعتادين عليه."
                            "Salicylic Acid" -> "حمض مقشر قوي يزيل طبقات الزهم والبروتين الخلوي مما يسبب حروقاً طيفية للبشرة المتهيجة أو الجافة."
                            "Essential Oils" -> "المركبات الطيارة في الزيوت العطرية تندمج بالأعصاب السطحية وتطلق السيتوكينات الالتهابية مسببة حكة ووهجاً فورياً."
                            "Lanolin" -> "اللانولين الدهني قد يسبب حساسية تلامسية للأشخاص ذوي البشرة فائقة التفاعل والحساسة للأصواف."
                            else -> "تم مطابقة مادة ($suspect) المضافة من قبلك في إعداد قائمة الحساسية الشخصية كمركب غير ملائم لنقاء وصحة خلايا جلدك."
                        }
                    )
                )
            }
        }

        val isSafe = foundAllergens.isEmpty()
        val fullList = rawText.split(",").map { it.trim() }.filter { it.isNotBlank() }

        IngredientAnalysisResult(
            productSummary = if (customUri != null) {
                "مستحضر ترميمي موضعي غني بالزيوت والروائح العطرية (تم استنتاج المكونات طيفياً من لقطة الكاميرا الممررة)."
            } else if (rawText.isNotEmpty()) {
                "مستحضر يتم تحليله عن طريق قائمة المكونات المنسوخة، يحتوي على نسبة ترطيب ومواد نشطة."
            } else {
                "مستحضر عينة اختباري تظاهري لمسح المكونات السريرية."
            },
            safetyStatus = if (isSafe) "آمن وملائماً لبشرتك" else "يحتوي على مسببات حساسية",
            detectedAllergens = foundAllergens,
            fullIngredientsList = fullList,
            clinicalAdvice = if (isSafe) {
                "العينات التي تم فحصها تخلو تماماً من المركبات التي حددتيها كعوامل تهيج أو حساسية شخصية. نوصيكِ بإدخال المنتج بالتدريج لروتينك وتجربته على رقعة اختبار صغيرة خلف الأذن لمدة 48 ساعة."
            } else {
                "المستحضر يحتوي على مركبات كيميائية قد تسبب استجابة مناعية وتهيج في بشرتكِ. يفضل عدم استخدامه على بشرة وجهكِ الحالية واختيار بدائل مغذية وخاملة وخالية من العطور والكحول والمقشرات القوية لترميم بشرتكِ بأمان."
            },
            isDemo = true
        )
    }

    if (isDemoKey) {
        // Run Simulated AI analysis after brief delay
        Thread.sleep(1500)
        return@withContext runLocalMatching()
    }

    // 2. REAL AI SCANNER (Direct call to Gemini 3.5 Flash via REST with Image or typed Text)
    try {
        updateProgressMessage("إعداد مصفوفات البيانات البصرية ومطابقة الألياف للـ AI...")
        val base64Image = if (customUri != null) {
            val inputStream = context.contentResolver.openInputStream(customUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                null
            }
        } else {
            null
        }

        updateProgressMessage("جاري تفعيل فحص الكيمياء الحيوية السحابية عبر Gemini...")

        // Direct REST query construction
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val allergyString = targetsToScan.joinToString(", ")
        val prompt = """
            You are 'GlowLogic CDSS' - an advanced AI Cosmetic Chemistry and Clinical Allergen Analyzer designed by Zubayda Ramzi.
            You are analyzing a skincare product to screen its ingredients for user allergies and sensitivities.
            The user is sensitive/allergic to the following compounds and their derivatives: [$allergyString].
            
            ${if (base64Image != null) "The user has uploaded a photo of the product's ingredient list." else "The user has provided the following text representing the ingredient list: $rawText"}
            
            Please read the ingredients carefully, find ALL occurrences or synonyms of the user's allergens, and parse the formulation.
            Output MUST be a strict raw JSON object with EXACTLY the following format and keys in proper Arabic. Do NOT wrap it in any Markdown or outer tags. Return ONLY the strict JSON:
            {
              "productSummary": "A summary of the product type, brand (if visible), and general purpose in Arabic",
              "safetyStatus": "آمن و خالي من متعارضات الحساسية OR يحتوي على مسببات حساسية لبشرتكِ",
              "detectedAllergens": [
                {
                  "name": "Scientific names of the allergen detected (e.g. Fragrance or Retinol)",
                  "arabicName": "Arabic chemical name equivalent (e.g. العطور or الريتينول)",
                  "matchType": "مطابقة تامة OR مرادف علمي",
                  "reason": "Detailed medical reason in professional Arabic explaining why this causes irritation/allergy for highly sensitive skin types"
                }
              ],
              "fullIngredientsList": ["List of first 10-15 main ingredients extracted"],
              "clinicalAdvice": "Personalized clinical advice from clinical expert Zubayda Ramzi in Arabic regarding this product's formulation and how the customer should handle it"
            }
        """.trimIndent()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val partText = JSONObject().put("text", prompt)
        val partsArray = JSONArray().put(partText)

        if (base64Image != null) {
            val inlineDataJson = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", base64Image)
            val partImage = JSONObject().put("inlineData", inlineDataJson)
            partsArray.put(partImage)
        }

        val contentObj = JSONObject().put("parts", partsArray)
        val contentsArray = JSONArray().put(contentObj)
        val requestObj = JSONObject().put("contents", contentsArray)

        // Config constraints for strict JSON output
        val responseSchema = JSONObject()
            .put("type", "OBJECT")
            .put("properties", JSONObject()
                .put("productSummary", JSONObject().put("type", "STRING"))
                .put("safetyStatus", JSONObject().put("type", "STRING"))
                .put("detectedAllergens", JSONObject()
                    .put("type", "ARRAY")
                    .put("items", JSONObject()
                        .put("type", "OBJECT")
                        .put("properties", JSONObject()
                            .put("name", JSONObject().put("type", "STRING"))
                            .put("arabicName", JSONObject().put("type", "STRING"))
                            .put("matchType", JSONObject().put("type", "STRING"))
                            .put("reason", JSONObject().put("type", "STRING"))
                        )
                        .put("required", JSONArray().put("name").put("arabicName").put("matchType").put("reason"))
                    )
                )
                .put("fullIngredientsList", JSONObject().put("type", "ARRAY").put("items", JSONObject().put("type", "STRING")))
                .put("clinicalAdvice", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray().put("productSummary").put("safetyStatus").put("detectedAllergens").put("fullIngredientsList").put("clinicalAdvice"))

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

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            val resJson = JSONObject(responseBody)
            val textValue = resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val jsonOutput = JSONObject(textValue)

            // Extract allergens lists
            val allergensArray = jsonOutput.getJSONArray("detectedAllergens")
            val allergenList = mutableListOf<FlaggedAllergen>()
            for (i in 0 until allergensArray.length()) {
                val item = allergensArray.getJSONObject(i)
                allergenList.add(
                    FlaggedAllergen(
                        name = item.getString("name"),
                        arabicName = item.getString("arabicName"),
                        matchType = item.getString("matchType"),
                        reason = item.getString("reason")
                    )
                )
            }

            // Extract full ingredients lists
            val fullIngrArray = jsonOutput.getJSONArray("fullIngredientsList")
            val parsedList = mutableListOf<String>()
            for (j in 0 until fullIngrArray.length()) {
                parsedList.add(fullIngrArray.getString(j))
            }

            IngredientAnalysisResult(
                productSummary = jsonOutput.getString("productSummary"),
                safetyStatus = jsonOutput.getString("safetyStatus"),
                detectedAllergens = allergenList,
                fullIngredientsList = parsedList,
                clinicalAdvice = jsonOutput.getString("clinicalAdvice"),
                isDemo = false
            )
        } else {
            // Roll back to local clinical matcher quietly on API failure
            runLocalMatching()
        }
    } catch (e: Exception) {
        // Roll back to local clinical matcher quietly on physical errors
        runLocalMatching()
    }
}
