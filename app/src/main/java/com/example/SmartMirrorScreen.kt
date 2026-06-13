package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import androidx.compose.ui.window.Dialog
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.FileProvider

// -------------------------------------------------------------
// AI Smart Mirror & Bespoke Cosmetic Formulation Module
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SmartMirrorScreen(
    accentColor: Color,
    cardColor: Color,
    textColorMuted: Color,
    viewModel: SkincareTrackerViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showBespokeRecipeCard by remember { mutableStateOf(false) }

    if (showBespokeRecipeCard) {
        BespokeRecipeCardView(
            cardColor = cardColor,
            accentColor = accentColor,
            textMuted = textColorMuted,
            activeReport = null,
            viewModel = viewModel,
            onBack = { showBespokeRecipeCard = false }
        )
        return
    }

    // --------------------------------------------------
    // Workflow Step Control
    // --------------------------------------------------
    var currentWorkflowStep by remember { mutableStateOf(1) }
    // Step 1: Questionnaire & Patient Diagnostics (Pre-Check)
    // Step 2: Live AI Smart Mirror Facial Scan
    // Step 3: Bespoke Formulation Blending & Compliance
    // Step 4: Intelligent Automated Dispensing (Smart Factory)
    // Step 5: Recipe Finalized & Added to Pharmacy database

    // --------------------------------------------------
    // Pre-Questionnaire States
    // --------------------------------------------------
    var gender by remember { mutableStateOf("أنثى") }
    var ageGroup by remember { mutableStateOf("20 - 29") }
    var expectedSkinType by remember { mutableStateOf("مختلطة") }
    var pastIssues by remember { mutableStateOf("حب شباب نشط") }
    var allergyOptions by remember { mutableStateOf("لا توجد حساسية") }
    var primaryGoal by remember { mutableStateOf("تفتيح وتصفية") }
    var envFactor by remember { mutableStateOf("التعرض الشديد للشمس") }

    // --------------------------------------------------
    // Mirror Scanner Simulation States
    // --------------------------------------------------
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var scanningStatusMessage by remember { mutableStateOf("جاري تشغيل المستشعرات الطيفية...") }
    var scanCompleted by remember { mutableStateOf(false) }

    // Smart Mirror 8 Measured Metrics (%)
    var wrinkleScore by remember { mutableStateOf(85) } // High score means healthy (less wrinkles)
    var poreScore by remember { mutableStateOf(65) }
    var rednessScore by remember { mutableStateOf(70) }
    var pigmentScore by remember { mutableStateOf(60) }
    var moistureScore by remember { mutableStateOf(50) }
    var sebumScore by remember { mutableStateOf(80) } // lower is less oily
    var acneScore by remember { mutableStateOf(55) }
    var darkCircleScore by remember { mutableStateOf(68) }

    // --------------------------------------------------
    // Bespoke Formulation States
    // --------------------------------------------------
    var selectedBaseType by remember { mutableStateOf("سيروم هيدرو-جل مائي خفيف (Hydro-Gel Serum Base)") }
    val activeIngredientsList = listOf(
        "Retinol" to "الريتينول (مشتق فيتامين أ) 🧪",
        "Hyaluronic Acid" to "حمض الهيالورونيك 💧",
        "Niacinamide" to "النياسيناميد (فيتامين B3) 🛡️",
        "Salicylic Acid" to "حمض الساليسيليك (BHA) 🍋",
        "Ceramides" to "السيراميدات الطبيعية 🧱",
        "Panthenol" to "البانثينول (برو فيتامين B5) 🌱",
        "Vitamin C" to "فيتامين سي 🍊",
        "Zinc PCA" to "الزنك المقاوم للميكروبات 🔮",
        "Snail Mucin" to "ترشيح إفراز البزاق الكوري 🐌"
    )
    val selectedIngredients = remember { mutableStateListOf("Hyaluronic Acid", "Niacinamide") }
    var formulaCustomName by remember { mutableStateOf("سيروم التخليق الجزيئي المخصص") }
    var formulationScent by remember { mutableStateOf("لافندر طبيعي مهدئ 🪻") }
    var showExportDialog by remember { mutableStateOf(false) }

    // --------------------------------------------------
    // Smart Factory Dispensing Animation States
    // --------------------------------------------------
    var isDispensing by remember { mutableStateOf(false) }
    var dispenseProgress by remember { mutableStateOf(0f) }
    var dispenseMessage by remember { mutableStateOf("تعبئة المذيب العضوي المائي...") }
    var dispenseCompleted by remember { mutableStateOf(false) }

    // --------------------------------------------------
    // Informational Center Tab Control
    // --------------------------------------------------
    var infoCenterTab by remember { mutableStateOf(0) } // 0: Tech Stack, 1: traditional vs AI

    // Formulate a realistic system recommendation message based on questionnaire & scanned results
    val analysisDermatologySummary = remember(scanCompleted, expectedSkinType, acneScore, moistureScore) {
        if (scanCompleted) {
            "بناءً على الفحص المجهري المباشر للمرآة الذكية ومعدل رطوبة الأدمة البالغ $moistureScore% ونسب إفراز الغدد الزهمية ومؤشر الحبوب ($acneScore%)، تم الكشف عن حاجة خلايا الجلد لتقوية الروابط الخارجية وتظهير المسام بصورة منقحة."
        } else {
            "بانتظار إجراء التحليل الطيفي للمرآة الذكية لتوليد المشورة السريرية الدقيقة."
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("smart_mirror_bespoke_workspace"),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // MODULE TITLE BANNER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "عيادة التخليق الذاتي والتركيب الذكي 🪞🪄",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "منصة متكاملة متجانسة تحاكي مرآة التشخيص ثلاثية الأبعاد (Smart Mirror) وتقوم بتركيب مستحضرات العناية المخصصة (Bespoke Cosmetics) مجهرياً وفورياً وفق اللوائح الصحية المعتمدة.",
                        color = textColorMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showBespokeRecipeCard = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, accentColor),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .testTag("open_recipe_card_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مستند التركيبة النشطة للتونر (USER_PROMPT) 🔬📋",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // WORKFLOW PROGRESS HUD Indicator
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState(), reverseScrolling = true),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    5 to "المنتج 🌟",
                    4 to "التصنيع ⚙️",
                    3 to "التركيب 🧪",
                    2 to "الفحص 🪞",
                    1 to "الاستبيان 🩺"
                ).forEach { (step, name) ->
                    val isPassed = currentWorkflowStep >= step
                    val isActive = currentWorkflowStep == step
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActive) accentColor else if (isPassed) accentColor.copy(alpha = 0.2f) else Color.Black.copy(
                                    alpha = 0.3f
                                )
                            )
                            .border(
                                1.dp,
                                if (isActive) accentColor else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            color = if (isActive) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // -------------------------------------------------------------
        // WORKFLOW SCREEN 1: PRE-QUESTIONNAIRE (THE ARABIC QUESTIONS)
        // -------------------------------------------------------------
        if (currentWorkflowStep == 1) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "استبيان وتحليل ما قبل الفحص 🩺📋",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = accentColor)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "الرجاء الإجابة عن هذه الأسئلة لضبط وتوجيه خوارزمية المسح البصري والتشخيص الجيني للمرآة الذكية بدقة متناهية.",
                            color = textColorMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Question 1: Gender
                        Text(
                            text = "Q1: ما هو الجنس الاجتماعي؟ (Gender)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("أنثى", "ذكر", "يفضل عدم الذكر").forEach { item ->
                                val selected = gender == item
                                Button(
                                    onClick = { gender = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(horizontal = 3.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Question 2: Age Group
                        Text(
                            text = "Q2: ما هي الفئة العمرية المحددة للخلية؟ (Age Group)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("أقل من 20", "20 - 29", "30 - 39", "40+").forEach { item ->
                                val selected = ageGroup == item
                                Button(
                                    onClick = { ageGroup = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(horizontal = 3.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Question 3: Expected Skin Type
                        Text(
                            text = "Q3: كيف تقيمين تصنيف بشرتك الأولي؟ (Expected Skin Type)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("جافة 🌵", "دهنية 💧", "مختلطة 🌸", "حساسة 🛡️").forEach { item ->
                                val cleanedItem = item.substringBefore(" ").trim()
                                val selected = expectedSkinType == cleanedItem
                                Button(
                                    onClick = { expectedSkinType = cleanedItem },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Question 4: Past Issues
                        Text(
                            text = "Q4: هل تواجهين عوارض جلدية نشطة مؤخراً؟ (Past Issues)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("حب شباب نشط", "تصبغات وكلف", "احمرار وحساسية", "جفاف وقشور").forEach { item ->
                                val selected = pastIssues == item
                                Button(
                                    onClick = { pastIssues = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Question 5: Allergies
                        Text(
                            text = "Q5: وجود حساسية تجاه مركبات معينة؟ (Allergies)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("العطور الكيميائية", "الريتينول بتركيز عال", "لا توجد حساسية").forEach { item ->
                                val selected = allergyOptions == item
                                Button(
                                    onClick = { allergyOptions = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Question 6: Skincare Goal
                        Text(
                            text = "Q6: ما هو الهدف البنيوي الأبرز للبروتوكول مجهرياً؟ (Goals)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("تفتيح وتصفية", "مقاومة التجاعيد والخطوط", "ترطيب عميق وحبس مائي", "إزالة الحبوب والدهون").forEach { item ->
                                val selected = primaryGoal == item
                                Button(
                                    onClick = { primaryGoal = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Question 7: Environment
                        Text(
                            text = "Q7: ما هي العوامل المناخية الأكثر تحفيزاً للبشرة؟ (Environment)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            listOf("التعرض الشديد للشمس", "جفاف الجو الداخلي", "التلوث والأتربة").forEach { item ->
                                val selected = envFactor == item
                                Button(
                                    onClick = { envFactor = item },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) accentColor else Color.Black.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = item,
                                        fontSize = 10.sp,
                                        color = if (selected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // SUBMIT QUESTIONNAIRE & GO TO SCAN SENSOR
                        Button(
                            onClick = {
                                // Initialize mock values based on expectedSkinType & goal to simulate genuine scan
                                when (expectedSkinType) {
                                    "جافة" -> {
                                        moistureScore = 28
                                        sebumScore = 35
                                        wrinkleScore = 62
                                    }
                                    "دهنية" -> {
                                        moistureScore = 52
                                        sebumScore = 88
                                        acneScore = 42
                                    }
                                    "مختلطة" -> {
                                        moistureScore = 48
                                        sebumScore = 67
                                        acneScore = 68
                                    }
                                    "حساسة" -> {
                                        moistureScore = 58
                                        sebumScore = 48
                                        rednessScore = 39
                                    }
                                }
                                currentWorkflowStep = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "حفظ الاستبيان والانتقال إلى فحص المرآة الذكية 🪞✦",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // WORKFLOW SCREEN 2: LIVE AI SMART MIRROR FACIAL SCAN
        // -------------------------------------------------------------
        if (currentWorkflowStep == 2) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "مرآة الأدمة وتحليل الخلايا المباشر 🪞🔬",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🫧", fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تحليل طيفي عميق يفحص بنية وأعماق الجلد حتى وهي مغطاة بالمكياج بفضل مستشعرات الطيف الضوئي الفسفوري ونماذج التعلم المعملية العميقة.",
                            color = textColorMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Smart Mirror Visual Scan Area
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(3.dp, if (isScanning) accentColor else Color.White.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated sweeping scanning overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (isScanning) {
                                    val lineY = (size.height * scanProgress)
                                    // Sweep line representation
                                    drawLine(
                                        color = accentColor,
                                        start = Offset(0f, lineY),
                                        end = Offset(size.width, lineY),
                                        strokeWidth = 4f
                                    )
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                                            center = Offset(size.width/2, lineY),
                                            radius = 120f
                                        ),
                                        center = Offset(size.width/2, lineY),
                                        radius = 120f
                                    )
                                }

                                // Interactive face scanner mockup grid
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.05f),
                                    style = Stroke(width = 1f)
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = Offset(size.width/2, 0f),
                                    end = Offset(size.width/2, size.height)
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.05f),
                                    start = Offset(0f, size.height/2),
                                    end = Offset(size.width, size.height/2)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isScanning) {
                                    CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "${(scanProgress * 100).toInt()}%",
                                        color = accentColor,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (scanCompleted) {
                                    Text("🪞✨", fontSize = 42.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "اكتمل تحليل المرآة",
                                        color = accentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null,
                                        tint = textColorMuted.copy(alpha = 0.5f),
                                        modifier = Modifier.size(74.dp)
                                    )
                                }
                            }
                        }

                        if (isScanning) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = scanningStatusMessage,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Trigger Button for scanner with coroutine loop
                        if (!isScanning) {
                            Button(
                                onClick = {
                                    isScanning = true
                                    scanCompleted = false
                                    scanProgress = 0f
                                    coroutineScope.launch {
                                        val messages = listOf(
                                            "جاري قراءة مستويات رطوبة الطبقة الخارجية للجلد...",
                                            "تفقد توسع حجم ومسامية الـ Pores مجهرياً...",
                                            "قياس معامل الاحمرار Erythema واستجابة الاحتقان...",
                                            "التحقق من توزيع بقع الميلانين والنمش...",
                                            "معايرة معامل التجاعيد والخطوط التعبيرية للجلد...",
                                            "تشغيل الخوارزميات الحيوية وتوليد المخرجات النهائية..."
                                        )
                                        while (scanProgress < 1.0f) {
                                            delay(500)
                                            scanProgress += 0.16f
                                            if (scanProgress > 1.0f) scanProgress = 1.0f
                                            val index = ((scanProgress * (messages.size - 1)).toInt()).coerceIn(0, messages.size - 1)
                                            scanningStatusMessage = messages[index]
                                        }
                                        isScanning = false
                                        scanCompleted = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (scanCompleted) Icons.Default.Refresh else Icons.Default.Camera,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (scanCompleted) "إعادة إجراء فحص المرآة الذكية 🔁" else "بدء المسح الضوئي للمرآة الذكية 🪞📡",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Display measurement clinical metrics once scan completes!
                        if (scanCompleted) {
                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "مخرجات الفحص الطيفي للمرآة الذكية (8 مؤشرات رئيسية) 🔬📈",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            )

                            // Grid style presentation for the 8 metrics
                            val metrics = listOf(
                                Triple("Wrinkles", "العمر الخلوي والخطوط", wrinkleScore),
                                Triple("Pore size", "حجم ومسامية المسام", poreScore),
                                Triple("Redness / Erythema", "الاحمرار والالتهاب", rednessScore),
                                Triple("Pigmentation / Freckles", "توزيع الميلانين والبقع", pigmentScore),
                                Triple("Moisture levels", "معدل رطوبة الأدمة", moistureScore),
                                Triple("Sebum levels", "إفراز الزهم والدهون", sebumScore),
                                Triple("Acne / Troubles", "بثور وحب شباب نشط", acneScore),
                                Triple("Dark circles", "محيط العين والهالات", darkCircleScore)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                metrics.forEach { (tag, name, score) ->
                                    val isGood = if (tag == "Moisture levels") score > 60 else score > 70
                                    val scoreColor = if (isGood) accentColor else Color(0xFFFCA5A5)

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$score%",
                                                color = scoreColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = name,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = when (tag) {
                                                        "Wrinkles" -> "⏳"
                                                        "Pore size" -> "🕳️"
                                                        "Redness / Erythema" -> "🔴"
                                                        "Pigmentation / Freckles" -> "✨"
                                                        "Moisture levels" -> "💧"
                                                        "Sebum levels" -> "🧴"
                                                        "Acne / Troubles" -> "🩹"
                                                        else -> "👁️"
                                                    },
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        LinearProgressIndicator(
                                            progress = { score / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp)),
                                            color = scoreColor,
                                            trackColor = Color.White.copy(alpha = 0.05f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action button to step 3 formulation
                            Button(
                                onClick = { currentWorkflowStep = 3 },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "بدء تركيب المستحضر المخصص للنتائج 🧪✦",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // WORKFLOW SCREEN 3: BESPOKE FORMULATION BLENDING & COMPLIANCE
        // -------------------------------------------------------------
        if (currentWorkflowStep == 3) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "معمل توليد وخلط المواد النشطة مجهرياً 🧪🔬",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Build, contentDescription = null, tint = accentColor)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "امزجي المكونات النشطة للحصول على مصل معقم بالكامل. يقوم محاكي اللوائح بالتحقق التلقائي للتأكد من مطابقة التركيبة لمعايير الجودة.",
                            color = textColorMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Select Cosmetic Base
                        Text(
                            text = "أولا: اختيار قاعدة المستحضر العضوية (Cosmetic Base)",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        listOf(
                            "سيروم هيدرو-جل مائي خفيف (Hydro-Gel Serum Base)",
                            "كريم استحلابي مغذي حريري (Silky Embellished Cream Base)"
                        ).forEach { baseItem ->
                            val isSelected = selectedBaseType == baseItem
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.08f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else Color.White.copy(alpha = 0.04f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedBaseType = baseItem }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedBaseType = baseItem },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Text(
                                    text = baseItem,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Select Active Ingredients (Max 3 as per standard cosmetic compounding to protect formulation potency)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "المختار: ${selectedIngredients.size}/3",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "ثانياً: تحديد المكونات الفسيولوجية النشطة 🧬🧪",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeIngredientsList.chunked(3).forEach { chunk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                                ) {
                                    chunk.forEach { (key, label) ->
                                        val isSelected = selectedIngredients.contains(key)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) accentColor else Color.Black.copy(alpha = 0.25f))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) accentColor else Color.White.copy(alpha = 0.04f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedIngredients.remove(key)
                                                    } else {
                                                        if (selectedIngredients.size < 3) {
                                                            selectedIngredients.add(key)
                                                        } else {
                                                            Toast
                                                                .makeText(
                                                                    context,
                                                                    "الحد الأقصى هو 3 مكونات نشطة لتفادي تضارب الفعالية والجرعة!",
                                                                    Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Name Form
                        Text(
                            text = "ثالثاً: تسمية المستحضر المخصص وعطر التركيبة",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        TextField(
                            value = formulaCustomName,
                            onValueChange = { formulaCustomName = it },
                            placeholder = { Text("أدخل اسماً مخصصاً للمنتج...", color = textColorMuted, fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("لافندر طبيعي mهدئ 🪻", "زهور ياسمين الدمشقية 🌸", "خالي من العطور للبشرة الحساسة 🫧").forEach { scentValue ->
                                val selected = formulationScent == scentValue
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) accentColor else Color.Black.copy(alpha = 0.2f))
                                        .border(
                                            1.dp,
                                            if (selected) accentColor else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { formulationScent = scentValue }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = scentValue.substringBefore(" ").trim(),
                                        color = if (selected) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // COMPLIANCE POLICY BOX (CROSS-REFERENCING BESPOKE_COSMETICS_POLICY)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .border(1.dp, accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "رقابة ومطابقة المركبات واللوائح الصحية 🛡️⚖️",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Active compliance warnings
                                var hasChemicalConflict = false
                                var conflictExplanation = ""

                                if (selectedIngredients.contains("Retinol") && selectedIngredients.contains("Salicylic Acid")) {
                                    hasChemicalConflict = true
                                    conflictExplanation = "تنبيه سريري: خلط الريتينول (Retinol) وحمض الساليسيليك (BHA) يسبب إجهاداً جدارياً وقشوراً خلوية حادة. يوصى بفصلهما نهاراً ومساءً."
                                } else if (selectedIngredients.contains("Retinol") && selectedIngredients.contains("Vitamin C")) {
                                    hasChemicalConflict = true
                                    conflictExplanation = "تنبيه سريري: خلط الريتينول وفيتامين سي معاً في نفس السائل يقلل من فاعلية فيتامين سي بسبب تضارب الـ pH الهيدروجيني."
                                } else if (selectedIngredients.contains("Salicylic Acid") && selectedIngredients.contains("Vitamin C")) {
                                    hasChemicalConflict = true
                                    conflictExplanation = "تنبيه سريري: قد تسبب هذه التوليفة حموضة نسبية مجهرية للبشرات الحساسة؛ يرجى مراقبة التهيج."
                                }

                                if (hasChemicalConflict) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF451C1C))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            text = conflictExplanation,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "التركيب الجزيئي آمن ومتجانس مع مستشعرات الأدمة.",
                                            color = accentColor,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Right
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(10.dp))
                                    }
                                }

                                // General Bespoke cosmetics regulation points
                                Text(
                                    text = "• خلط ومزج المكونات النشطة بالجرعة الطبية المجهرية مسموح ✅",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                                Text(
                                    text = "• تقسيم وتعبئة الحجم لحبسات الرطوبة والزجاجات معتمد ✅",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                                Text(
                                    text = "• يمنع استخدام مواد مصنعة للبيع التجزئي المسبق أو العينات الترويجية ❌",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PROCEED TO DISPERSING SMART FACTORY
                        Button(
                            onClick = { currentWorkflowStep = 4 },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "الانتقال إلى وحدة التركيب وضخ المكونات ⚙️🪄",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // WORKFLOW SCREEN 4: INTELLIGENT AUTOMATED DISPENSING BAR
        // -------------------------------------------------------------
        if (currentWorkflowStep == 4) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "وحدة التخليق الكيميائي الفوري (Smart Factory) ⚙️🧪",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡", fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تحاكي عملية تصنيع وتعبئة المستحضرات بنظام المصنع الذكي مع توجيه الصمامات الرقمية لصب المكونات بالنسب المجهرية بدقة لحماية حيوية المستحضر.",
                            color = textColorMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Beautiful Digital Bottle Dispensing Visualizer
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(2.dp, if (isDispensing) accentColor else Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Fluid level rising
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(dispenseProgress)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.8f), accentColor.copy(alpha = 0.3f))
                                        )
                                    )
                            )

                            // Display current percentage
                            Text(
                                text = "${(dispenseProgress * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = dispenseMessage,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (!isDispensing && !dispenseCompleted) {
                            Button(
                                onClick = {
                                    isDispensing = true
                                    dispenseProgress = 0f
                                    coroutineScope.launch {
                                        val steps = listOf(
                                            "إعداد قاعدة المستحضر: $selectedBaseType (85.5%)...",
                                            "إضافة جزيئات مكملات السيراميد والبانثينول المرممة...",
                                            "تعبئة الجرعة الدقيقة للمكونات النشطة: ${selectedIngredients.joinToString(", ")} (10%)...",
                                            "حقن المادة الحافظة الطبية لتعقيم الأدمة (1.5%)...",
                                            "دمج عطر الحمام العطري: $formulationScent...",
                                            "ضبط النانوتكنولوجي واستقرار الـ pH الكيميائي وتدقيق الصيغة...",
                                            "إغلاق ميكانيكي معقم لبلورة المستحضر جزيئياً بنجاح!"
                                        )
                                        while (dispenseProgress < 1.0f) {
                                            delay(700)
                                            dispenseProgress += 0.15f
                                            if (dispenseProgress > 1.0f) dispenseProgress = 1.0f
                                            val idx = ((dispenseProgress * (steps.size - 1)).toInt()).coerceIn(0, steps.size - 1)
                                            dispenseMessage = steps[idx]
                                        }
                                        isDispensing = false
                                        dispenseCompleted = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Text(
                                    text = "بدء تركيب وتخليق المستحضر فوراً ⚙️🧪",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (dispenseCompleted) {
                            Button(
                                onClick = { currentWorkflowStep = 5 },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "استعراض وجلب المستحضر النهائي 🛒✦",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // WORKFLOW SCREEN 5: RECIPE FINALIZED & ADDED TO PHARMACY DATABASE
        // -------------------------------------------------------------
        if (currentWorkflowStep == 5) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.2.dp, accentColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 تم تخليق مستحضر التجميل بنجاح تام!",
                            color = accentColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧪", fontSize = 34.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = formulaCustomName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "بواسطة الأخصائية زبيدة رمزي وبراءة اختراع AI Mirror",
                            color = textColorMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Details Sheet
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "سيروم مخصص", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "تصنيف المستحضر", color = textColorMuted, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = selectedIngredients.joinToString(", "), color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "المكونات النشطة المدمجة", color = textColorMuted, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = formulationScent, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "النوتة العطرية المضافة", color = textColorMuted, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "مطابق للائحة Bespoke 🛡️", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "حالة المطابقة الطبية", color = textColorMuted, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        var isSavedDirectly by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                if (!isSavedDirectly) {
                                    // Save bespoke product to database through the ViewModel
                                    viewModel.addNewProduct(
                                        name = formulaCustomName,
                                        brand = "Bespoke (خياطة وتخليق صيدلي)",
                                        category = "سيروم",
                                        skinType = expectedSkinType,
                                        concern = when (primaryGoal) {
                                            "إزالة الحبوب والدهون" -> "حب الشباب"
                                            "تفتيح وتصفية" -> "التصبغات"
                                            "ترطيب عميق وحبس مائي" -> "الجفاف"
                                            else -> "التجاعيد والخطوط"
                                        },
                                        description = "سيروم مخصص صممه مرآة الذكاء الاصطناعي بدقة تناظرية بناء على استبيان العوارض والتحليل الضوئي للخلايا. يحتوي على خلاصة عطرية: $formulationScent",
                                        activeIngredients = selectedIngredients.joinToString(", ")
                                    )
                                    isSavedDirectly = true
                                    Toast.makeText(context, "تم حفظ المستحضر وإيداعه في صيدليتكِ بنجاح! 🔬🛒", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSavedDirectly) Color.Gray else accentColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSavedDirectly) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSavedDirectly) "تم الحفظ بنجاح مسبقاً! ✓" else "حفظ المستحضر في صيدلية ومستحضرات التطبيق 🛒💾",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // EXPORT BUTTON
                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // Emerald highlight
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تصدير وطباعة بطاقة الوصفة / PDF 📤📋",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // EXPORT DIGITAL CARD DIALOG
                        if (showExportDialog) {
                            Dialog(onDismissRequest = { showExportDialog = false }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Header
                                        item {
                                            Text(
                                                text = "🔬 بِطَاقَة تَرْكِيبَة البشرة الطِّبِّيَّة",
                                                color = accentColor,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = "صيدلية ومستحضرات التجميل الجزيئي الشخصي",
                                                color = textColorMuted,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }

                                        // Styled Formula Card representation (Digital Receipt look)
                                        item {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "GlowLogic Bespoke Series",
                                                    color = accentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = formulaCustomName,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(Color.White.copy(alpha = 0.1f))
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Left aligned arabic receipt details
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text(
                                                        text = "👤 نوع البشرة: $expectedSkinType",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )

                                                    Text(
                                                        text = "🎯 الهدف الرئيسي: $primaryGoal",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "🧪 المكونات والنسب الدقيقة لتبخير النسيج:",
                                                        color = accentColor,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )

                                                    selectedIngredients.forEach { ing ->
                                                        val desc = when (ing) {
                                                            "Hyaluronic Acid" -> "حمض الهيالورونيك (ترطيب مائي)"
                                                            "Niacinamide" -> "نياسيناميد (تفتيح وشد المسام)"
                                                            "Retinol" -> "ريتينول (تجديد ومكافحة التصبغات)"
                                                            "Salicylic Acid" -> "حمض الساليسيليك (علاج الدهون والحبوب)"
                                                            "Vitamin C" -> "فيتامين سي (مضاد للأكسدة والنضارة)"
                                                            else -> ing
                                                        }
                                                        Text(
                                                            text = "• ${desc} بنسبة تركيبية 3.3%",
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "🪻 القاعدة العطرية: $formulationScent",
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "🛡️ المطابقة والترخيص: مطهر وملقّح بالكامل وفق معايير الجودة الطبية للائحة Bespoke بنجاح.",
                                                        color = accentColor.copy(alpha = 0.85f),
                                                        fontSize = 10.sp,
                                                        lineHeight = 13.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "توقيع الأخصائية الحصري: زبيدة رمزي 🩺✍️",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 9.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }

                                        // Interaction controls
                                        item {
                                            Spacer(modifier = Modifier.height(20.dp))
                                            
                                            // Share Text Button
                                            Button(
                                                onClick = {
                                                    shareFormulationText(
                                                        context,
                                                        formulaCustomName,
                                                        expectedSkinType,
                                                        primaryGoal,
                                                        selectedIngredients,
                                                        formulationScent
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(42.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("مشاركة النص رقمياً (واتساب/رسائل) 🔗", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Download PDF Button
                                            Button(
                                                onClick = {
                                                    exportFormulationPdf(
                                                        context,
                                                        formulaCustomName,
                                                        expectedSkinType,
                                                        primaryGoal,
                                                        selectedIngredients,
                                                        formulationScent,
                                                        onComplete = { file ->
                                                            Toast.makeText(context, "تم تخليق وتصدير تقرير الروتين PDF بنجاح! 📄", Toast.LENGTH_LONG).show()
                                                            shareFormulationPdf(context, file)
                                                        },
                                                        onError = { _ ->
                                                            Toast.makeText(context, "فشل تصدير ملف PDF", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(42.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("تصدير وطباعة بطاقة PDF الرسمية 📄", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Dismiss dismiss button
                                            TextButton(onClick = { showExportDialog = false }) {
                                                Text("إغلاق العرض المؤقت ✖", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // RESET WORKFLOW BUTTON
                        OutlinedButton(
                            onClick = {
                                currentWorkflowStep = 1
                                scanCompleted = false
                                dispenseCompleted = false
                                isSavedDirectly = false
                                selectedIngredients.clear()
                                selectedIngredients.add("Hyaluronic Acid")
                                selectedIngredients.add("Niacinamide")
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = "تصميم تركيبة جديدة مخصصة 🔁",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PERSISTED THE INDUSTRIAL / TECH KNOWLEDGE COMPONENT SECTION
        // -------------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "الركن المعرفي والتقني للأدمة 🔬📙",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغطي لقراءة البنية التكنولوجية للمصانع الذكية والمرآة، أو اطلعي على الفارق السريري بين الفحص التقليدي والفحص الذكي بنظم البيانات.",
                        color = textColorMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab bar for info center
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { infoCenterTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (infoCenterTab == 1) accentColor else Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(
                                text = "مقارنة الفرز والمقاييس 📊",
                                color = if (infoCenterTab == 1) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { infoCenterTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (infoCenterTab == 0) accentColor else Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(
                                text = "البنية التكنولوجية 🧠⚙️",
                                color = if (infoCenterTab == 0) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (infoCenterTab == 0) {
                        // Core Tech Stack list
                        val techs = listOf(
                            "Computer Vision & Sensors" to "لتشخيص وتحليل عوارض الأدمة بدقة متناهية تخترق جدار الخلايا السطحية والقرنية.",
                            "Big Data & AI Algorithms" to "توقع وحساب استجابة البشرة للمواد النشطة بمقارنة مصفوفة من ٢ مليون نقطة ومقاييس سابقة.",
                            "Formulation Technology" to "معالجة ومعايرة كيميائية لتحديد تركيزات الأحماض ومضادات الأكسدة دون إحداث تهيج.",
                            "Nano Delivery Systems" to "تقنية استحلاب بالغة الدقة (Micro-capsules) لتوصيل جزيئيات السيراميد والريتينول لأعمق الخلايا.",
                            "Smart Factory & Robots" to "تصنيع فوري ميكانيكي معقم ومؤتمت يحقن المواد الكيميائية بدقة ليزرية متناهية."
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            techs.forEach { (title, desc) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.02f))
                                        .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = title, color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = desc, color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp, textAlign = TextAlign.Right, lineHeight = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Method comparison
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Traditional
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "طرق الفرز التقليدية (Traditional Assessment)",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• الأدوات المستخدمة: Corneometer (قياس الرطوبة الخلوية)، Sebumeter (قياس الدهون الحرة).\n• الإيجابيات والمزايا: الاعتماد المباشر على الحدس البشري للأخصائي المعالج والاتصال السلوكي المباشر مع المريض.\n• السلبيات: نتائج تقديرية، تخضع للمؤثرات الخارجية والضوء المشتت، وتحتاج لزيارات عيادية متباعدة وطويلة.",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }

                            // AI powered
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.04f))
                                    .border(1.dp, accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "الفرز بالذكاء الاصطناعي (AI-Powered Mirror Technology)",
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• الأدوات المستخدمة: 3D Skin Scanning (المسح المجسم ثلاثي الأبعاد)، Spectrometers (مستشعرات المدى الطيفي للترشيح).\n• الإيجابيات والمزايا: دقة مذهلة وموضوعية تبلغ >95% في رصد المشاكل الجلدية والتجاعيد حتى أثناء ارتداء مواد الزينة والمكياج بفضل الاستقرائي العصبي.\n• السلبيات: الاعتماد التام على جودة المدخلات البصرية لعدسة الكاميرا والمستشعرات الطيفية المحسوسة.",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Core PDF & Text Recipe Export Helpers
// -------------------------------------------------------------

fun exportFormulationPdf(
    context: Context,
    formulaCustomName: String,
    skinType: String,
    primaryGoal: String,
    ingredients: List<String>,
    scent: String,
    onComplete: (File) -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // Setup paints
        val linePaint = Paint().apply {
            color = AndroidColor.parseColor("#CCCCCC")
            strokeWidth = 1f
        }
        
        // Draw elegant border/frame
        val borderPaint = Paint().apply {
            color = AndroidColor.parseColor("#10B981") // Emerald accent
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(Rect(20, 20, 575, 822), borderPaint)
        
        val innerBorderPaint = Paint().apply {
            color = AndroidColor.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas.drawRect(Rect(25, 25, 570, 817), innerBorderPaint)
        
        // Header
        val titlePaint = Paint().apply {
            color = AndroidColor.parseColor("#111827")
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("GLOWLOGIC AI SKINCARE LAB 🔬", 130f, 70f, titlePaint)
        
        val subtitlePaint = Paint().apply {
            color = AndroidColor.parseColor("#059669")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("Official Clinical Compounding Recipe Formulation", 155f, 90f, subtitlePaint)
        
        // Divider
        canvas.drawLine(40f, 110f, 555f, 110f, Paint().apply { color = AndroidColor.parseColor("#059669"); strokeWidth = 1.5f })
        
        // Metadata fields
        val boldPaint = Paint().apply {
            color = AndroidColor.parseColor("#1F2937")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val valuePaint = Paint().apply {
            color = AndroidColor.parseColor("#4B5563")
            textSize = 12f
            isAntiAlias = true
        }
        
        var yPos = 150f
        
        val serialId = "GL-${(1000..9999).random()}-${(26..30).random()}"
        
        // Serial
        canvas.drawText("Formulation Serial ID:", 50f, yPos, boldPaint)
        canvas.drawText(serialId, 240f, yPos, valuePaint)
        
        yPos += 25f
        // Date
        canvas.drawText("Date of Compounding:", 50f, yPos, boldPaint)
        canvas.drawText(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.ENGLISH).format(java.util.Date()), 240f, yPos, valuePaint)
        
        yPos += 25f
        // Formulation Name
        canvas.drawText("Custom Formulation Name:", 50f, yPos, boldPaint)
        canvas.drawText(formulaCustomName, 240f, yPos, Paint().apply {
            color = AndroidColor.parseColor("#111827")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        })
        
        yPos += 25f
        // Skin Target
        canvas.drawText("Target Skin Type:", 50f, yPos, boldPaint)
        canvas.drawText(skinType, 240f, yPos, valuePaint)
        
        yPos += 25f
        // Concern
        canvas.drawText("Primary Skin Concern:", 50f, yPos, boldPaint)
        canvas.drawText(primaryGoal, 240f, yPos, valuePaint)
        
        yPos += 35f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        
        yPos += 30f
        // Title 2
        val header2Paint = Paint().apply {
            color = AndroidColor.parseColor("#111827")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("CLINICALLY INFUSED ACTIVE INGREDIENTS RECIPE", 50f, yPos, header2Paint)
        
        yPos += 25f
        ingredients.forEachIndexed { idx, ing ->
            canvas.drawText("${idx + 1}. $ing", 70f, yPos, boldPaint)
            val desc = when (ing) {
                "Hyaluronic Acid" -> "Deep cellular hydration & trans-epidermal water retention"
                "Niacinamide" -> "Barrier reinforcement, sebum modulation & redness reduction"
                "Retinol" -> "Accelerated cell renewal & collagen production stimulation"
                "Salicylic Acid" -> "Follicular lipid exfoliation & acne comedone dissolution"
                "Vitamin C" -> "Melanogenesis inhibitor, powerful antioxidant & tissue glow"
                else -> "Active compound targeted to clinical pathology"
            }
            canvas.drawText("Purpose: $desc", 85f, yPos + 15f, Paint().apply {
                color = AndroidColor.parseColor("#6B7280")
                textSize = 10f
                isAntiAlias = true
            })
            yPos += 35f
        }
        
        yPos += 10f
        canvas.drawText("Vehicle Base Formulation Carrier: Pure Organic Aloe Vera Gel Solution (90%)", 50f, yPos, valuePaint)
        yPos += 20f
        canvas.drawText("Ethereal Botanical Fragrance Scent: $scent", 50f, yPos, valuePaint)
        
        yPos += 40f
        canvas.drawLine(40f, yPos, 555f, yPos, linePaint)
        
        yPos += 30f
        canvas.drawText("COMPLIANCE CERTIFICATION & MEDICAL REGULATION 🛡️", 50f, yPos, subtitlePaint)
        
        yPos += 20f
        val compliancePaint = Paint().apply {
            color = AndroidColor.parseColor("#4B5563")
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("This customized molecular formulation recipe complies with European Cosmetic Safety Regulation (EC) 1223/2009.", 50f, yPos, compliancePaint)
        yPos += 14f
        canvas.drawText("Designed under supervision of cosmetic lead Zubayda Ramzi. Keep refrigerated for premium potency preservation.", 50f, yPos, compliancePaint)
        
        yPos += 55f
        canvas.drawLine(380f, yPos, 530f, yPos, Paint().apply { color = AndroidColor.parseColor("#9CA3AF") })
        
        yPos += 15f
        canvas.drawText("Clinical Lead Signature", 390f, yPos, Paint().apply {
            color = AndroidColor.BLACK
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        })
        canvas.drawText("Zubayda Ramzi, GlowLogic", 390f, yPos + 12f, Paint().apply {
            color = AndroidColor.parseColor("#4B5563")
            textSize = 9f
            isAntiAlias = true
        })
        
        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "GlowLogic_Bespoke_Recipe_${serialId}.pdf")
        val os = FileOutputStream(file)
        pdfDocument.writeTo(os)
        os.close()
        pdfDocument.close()
        
        onComplete(file)
    } catch (e: Exception) {
        onError(e)
    }
}

fun shareFormulationText(
    context: Context,
    formulaCustomName: String,
    skinType: String,
    primaryGoal: String,
    ingredients: List<String>,
    scent: String
) {
    try {
        val sharingBody = """
            🧪 وِصْفَة الطَّرْكِيْبَة المخصَّصة من GlowLogic 🌿
            ------------------------------------------
            🔬 اسم المستحضر: $formulaCustomName
            🩺 إشراف الأخصائية: زبيدة رمزي
            👤 نوع البشرة المستهدف: بشرة $skinType
            🎯 هدف البروتوكول: $primaryGoal
            
            🧪 المكونات النشطة المدمجة بدقة (10%):
            ${ingredients.mapIndexed { idx, ing -> "  ${idx + 1}. $ing" }.joinToString("\n")}
            
            🪻 النوتة العطرية الأساسية: $scent
            🛡️ حالة المطابقة: مطابق تماماً للمواصفات الطبية Bespoke
            ------------------------------------------
            🔐 تم تخليقها وصياغتها رقمياً بكفاءة بواسطة براءة اختراع AI Mirror للمرآة السحابية الذكية.
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "وصفة المستحضر المخصص من GlowLogic")
            putExtra(Intent.EXTRA_TEXT, sharingBody)
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة وصفة التركيبة عبر"))
    } catch (e: Exception) {
        Toast.makeText(context, "حدث خطأ أثناء رصد ومشاركة النص", Toast.LENGTH_SHORT).show()
    }
}

fun shareFormulationPdf(context: Context, pdfFile: File) {
    try {
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            pdfFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "تصدير وطباعة بطاقة الوصفة الطبية PDF 📥"))
    } catch (e: Exception) {
        Toast.makeText(context, "خطأ في مشاركة ملف PDF: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
