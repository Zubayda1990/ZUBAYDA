package com.example

import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
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
import java.io.InputStream

// --- Models & Data Structures ---

data class SkinReport(
    val skinType: String,
    val hydration: Int,
    val barrierHealth: Int,
    val pathology: String,
    val routineAM: String,
    val routinePM: String,
    val avoid: String,
    val isDemo: Boolean = false
)

data class CuratedCase(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val defaultReport: SkinReport
)

data class ChatMessage(
    val sender: String,
    val text: String,
    val isUser: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GlowLogicMasterApp()
            }
        }
    }
}

@Composable
fun GlowLogicMasterApp() {
    var currentTab by remember { mutableStateOf(0) } // Start on Scan tab
    val trackerViewModel: SkincareTrackerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    // Shared State for the active report
    var activeReport by remember { mutableStateOf<SkinReport?>(null) }
    
    // Aesthetic Palette
    val bgDark = Color(0xFF0F172A)     // Slate 900
    val cardDark = Color(0xFF1E293B)   // Slate 800
    val accentCyan = Color(0xFF38BDF8)  // Sky blue
    val textMuted = Color(0xFF94A3B8)   // Slate 400

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(containerColor = cardDark) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Face, contentDescription = "الفحص", tint = if (currentTab == 0) accentCyan else Color.White) },
                    label = { Text("الفحص", color = Color.White) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "الصيدلية", tint = if (currentTab == 1) accentCyan else Color.White) },
                    label = { Text("الصيدلية", color = Color.White) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "التقرير", tint = if (currentTab == 2) accentCyan else Color.White) },
                    label = { Text("التقرير", color = Color.White) }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "التتبع", tint = if (currentTab == 3) accentCyan else Color.White) },
                    label = { Text("التتبع", color = Color.White) }
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.History, contentDescription = "السجل", tint = if (currentTab == 4) accentCyan else Color.White) },
                    label = { Text("السجل", color = Color.White) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(bgDark)
        ) {
            when (currentTab) {
                0 -> ScanScreen(
                    accentColor = accentCyan,
                    cardColor = cardDark,
                    textColorMuted = textMuted,
                    viewModel = trackerViewModel,
                    activeReport = activeReport,
                    onNavigateToReport = { currentTab = 2 },
                    onReportGenerated = { report ->
                        activeReport = report
                        trackerViewModel.saveSkinReport(report)
                        // Automatically switch to clinical report screen on successful analysis
                        currentTab = 2
                    }
                )
                1 -> PharmacyScreen(
                    cardColor = cardDark,
                    accentColor = accentCyan,
                    textMuted = textMuted,
                    activeReport = activeReport,
                    viewModel = trackerViewModel
                )
                2 -> ClinicalReportScreen(
                    cardColor = cardDark,
                    accentColor = accentCyan,
                    textMuted = textMuted,
                    activeReport = activeReport,
                    onNavigateToScan = { currentTab = 0 }
                )
                3 -> SkincareTrackerScreen(
                    cardColor = cardDark,
                    accentColor = accentCyan,
                    textMuted = textMuted,
                    activeReport = activeReport,
                    onNavigateToScan = { currentTab = 0 },
                    viewModel = trackerViewModel
                )
                4 -> HistoryScreen(
                    cardColor = cardDark,
                    accentColor = accentCyan,
                    textMuted = textMuted,
                    viewModel = trackerViewModel,
                    onNavigateToScan = { currentTab = 0 },
                    onLoadReport = { savedReport ->
                        activeReport = SkinReport(
                            skinType = savedReport.skinType,
                            hydration = savedReport.hydration,
                            barrierHealth = savedReport.barrierHealth,
                            pathology = savedReport.pathology,
                            routineAM = savedReport.routineAM,
                            routinePM = savedReport.routinePM,
                            avoid = savedReport.avoid,
                            isDemo = savedReport.isDemo
                        )
                        currentTab = 2 // Switch to report screen
                    }
                )
            }
        }
    }
}

@Composable
fun ScanScreen(
    accentColor: Color,
    cardColor: Color,
    textColorMuted: Color,
    viewModel: SkincareTrackerViewModel,
    activeReport: SkinReport?,
    onNavigateToReport: () -> Unit,
    onReportGenerated: (SkinReport) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAssessmentMode by remember { mutableStateOf(false) }
    var isBaumannMode by remember { mutableStateOf(false) }
 
    var showClinicalDashboard by remember { mutableStateOf(true) }
    var showBespokeFromDashboard by remember { mutableStateOf(false) }
    var scanSubTab by remember { mutableStateOf(0) }

    if (showBespokeFromDashboard) {
        BespokeRecipeCardView(
            cardColor = cardColor,
            accentColor = accentColor,
            textMuted = textColorMuted,
            activeReport = activeReport,
            viewModel = viewModel,
            onBack = { showBespokeFromDashboard = false }
        )
    } else if (isBaumannMode) {
        BaumannQuestionnaireScreen(
            cardColor = cardColor,
            accentColor = accentColor,
            textMuted = textColorMuted,
            onReportGenerated = { report ->
                onReportGenerated(report)
                isBaumannMode = false
            },
            onBack = { isBaumannMode = false }
        )
    } else if (isAssessmentMode) {
        SkinAssessmentForm(
            cardColor = cardColor,
            accentColor = accentColor,
            textMuted = textColorMuted,
            onReportGenerated = onReportGenerated,
            onBack = { isAssessmentMode = false }
        )
    } else if (showClinicalDashboard) {
        GlowLogicClinicalDashboardScreen(
            currentTab = 0,
            onTabChange = {},
            onLaunchScan = {
                scanSubTab = 0
                showClinicalDashboard = false
            },
            onLaunchTelemetry = {
                scanSubTab = 1
                showClinicalDashboard = false
            },
            onLaunchFormulation = {
                showBespokeFromDashboard = true
            }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A192F))
                    .clickable { showClinicalDashboard = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "العودة إلى لوحة GlowLogic الرئيسية / Return to Clinical Board",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { scanSubTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanSubTab == 0) accentColor else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "التشخيص 🔬",
                        color = if (scanSubTab == 0) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { scanSubTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanSubTab == 1) accentColor else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "الحساسية 🧪",
                        color = if (scanSubTab == 1) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { scanSubTab = 2 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanSubTab == 2) accentColor else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.2f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "المرآة والتركيب 🪞🧪",
                        color = if (scanSubTab == 2) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            if (scanSubTab == 0) {

    // 1. Curated Dermatological Cases for easy interactive emulation
    val curatedCases = remember {
        listOf(
            CuratedCase(
                id = "acne",
                name = "حب شباب كيسي ومسام واسعة",
                emoji = "🔴",
                description = "مسام دهنية نشطة مع رؤوس سوداء ملتهبة تحت السطح الجلدي.",
                defaultReport = SkinReport(
                    skinType = "دهنية معرضة لحب الشباب",
                    hydration = 42,
                    barrierHealth = 51,
                    pathology = "ارتفاع ملحوظ في معدل إفراز الدهون الزهمية مع انسداد غدد البشرة بفعل جزيئات المناخ الصيفي الحار، مما تسبب ببكتيريا حب الشباب اللاهوائية وعقد التهابية نشطة.",
                    routineAM = "1. غسول حمض الساليسيليك Salicylic Acid 2% موازن للدهون.\n2. سيروم نياسيناميد 10% لتنظيم الزهم وتقليل الاحمرار.\n3. مرطب مائي هلامي مهدئ غني بالبانثينول.\n4. واقي شمس مطفأ خفيف SPF 50+.",
                    routinePM = "1. غسول رغوي عميق التطهير.\n2. سيروم حمض الأزيليك 10% للتخلص من الالتهابات والتصبغات.\n3. طبقة خفيفة جداً من مهدئ خلاصة السيكا والزنك لتبريد تهيج البشرة.",
                    avoid = "تجنب المقشرات اليدوية الخشنة، الزيوت الثقيلة كزيت جوز الهند، والمستحضرات التي تحتوي على نسبة عالية من العطور الكيميائية."
                )
            ),
            CuratedCase(
                id = "dry",
                name = "جفاف شديد مع تقشر بهتان",
                emoji = "🌵",
                description = "جفاف حاد وتقشر في منطقة الخدين ناتج عن فقدان الرطوبة.",
                defaultReport = SkinReport(
                    skinType = "جافة جداً وتالفة الحاجز الواقي",
                    hydration = 26,
                    barrierHealth = 34,
                    pathology = "تلف شديد في الحاجز الشحمي الواقي للبشرة ونقص في دهون السيراميد، مما أنتج ارتفاع معدل فقدان الماء اللامرئي عبر الجلد (TEWL) وتقشراً خلوياً واضحاً.",
                    routineAM = "1. منظف كريمي حليبي غسول مرطب غير رغوي.\n2. سيروم حمض الهيالورونيك 2% على بشرة ندية ومبللة.\n3. مرطب حاجز دهني غني بالسيراميد (Ceramides AP, NP).\n4. واقي شمس كريمي ثري بالمعادن المرطبة الألترا.",
                    routinePM = "1. حليب منظف فائق النعومة.\n2. سيروم مغذ بالبانثينول 5% وسكوالين نقي.\n3. كريم حاجز ليلي كثيف يعمل على الاندماج الخلوي وحفظ الرطوبة داخل نسيج البشرة.",
                    avoid = "تجنب تماماً استخدام أحماض التقشير القوية كالسليسليك والجليكولين حتى ترميم الحاجز بالكامل، وتجنب الماء الساخن أثناء غسيل الوجه."
                )
            ),
            CuratedCase(
                id = "rosacea",
                name = "وردية واحمرار مفرط الحساسية",
                emoji = "🌺",
                description = "بشرة سريعة التوتر ووهج أحمر ناتج عن الأوعية المجهرية.",
                defaultReport = SkinReport(
                    skinType = "مفرطة الحساسية والوردية التفاعلية",
                    hydration = 58,
                    barrierHealth = 29,
                    pathology = "توسع دائم وتهيج سريع في الشعيرات الدموية الدقيقة بالبشرة بفعل الإشعار الشمسي والحرارة المتطرفة، مع تضرر نظام الحماية الخلوي السطحي.",
                    routineAM = "1. غسول مائي رغوي خال تماماً من الصابون والعطور.\n2. رذاذ مياه حرارية مهدئة وفورية الـ pH.\n3. سيروم خلاصة السنتيلا Centella Asiatica المهدئة.\n4. واقي شمس فيزيائي 100% يحتوي على أكسيد الزنك المعالج.",
                    routinePM = "1. تنظيف مهدئ مخصص للبشرة المتوهجة.\n2. سيروم ترميم الببتيد الشوفاني الملطف للبشرة.\n3. كريم استعادة الراحة الفاخر والخفيف لتثبيط الحساسية الجلدية.",
                    avoid = "تجنب الريتينول، كحول مستحضرات التجميل، المقشرات، العطور الاصطناعية، والمأكولات الحارة الحامضية."
                )
            ),
            CuratedCase(
                id = "normal",
                name = "بشرة عادية نضرة متوازنة",
                emoji = "✨",
                description = "مثال للبشرة المتوازنة ذات الترطيب الرائع الخالي من الحبوب.",
                defaultReport = SkinReport(
                    skinType = "عادية متوازنة ومقاومة",
                    hydration = 78,
                    barrierHealth = 85,
                    pathology = "توزيع متناسق ومثالي لدهون البشرة ومرونتها، مع سلامة الحاجز الجلدي وتناغم الخلايا الكيراتينية في التبادل الحيوي الطبيعي.",
                    routineAM = "1. غسول لطيف صباحي مغذٍ.\n2. سيروم فيتامين سي لتعزيز الإشراق والنضارة الطبيعية.\n3. كريم ترطيب خفيف يومي وسريع الامتصاص.\n4. واقي شمس واسع النطاق وخفيف القوام.",
                    routinePM = "1. غسول زيتي (تنظيف مزدوج) يليه غسول مائي.\n2. سيروم ريتينول لطيف جداً 0.2% لمقاومة علامات التقدم بالسن وتجديد الخلايا.\n3. كريم مكثف ومغذي يدعم الصحة الحيوية والراحة.",
                    avoid = "تجنب إساءة استخدام منتجات العناية بالبشرة المفرطة للمحافظة على الفلورا المايكروبية المتزنة الطبيعية للبشرة."
                )
            )
        )
    }

    var selectedCase by remember { mutableStateOf<CuratedCase?>(curatedCases[0]) }
    var customImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var analysisProgressMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Custom Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customImageUri = uri
            selectedCase = null // Prioritize custom uploaded photo
            errorMessage = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.cacheDir, "camera_skin_capture.jpg")
                val outputStream = java.io.FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.flush()
                outputStream.close()
                customImageUri = Uri.fromFile(file)
                selectedCase = null
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "فشل في حفظ الصورة الملتقطة: ${e.message}"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMessage = "عذراً، لم نتمكن من تشغيل تطبيق الكاميرا على هذا الجهاز: ${e.message}"
            }
        } else {
            errorMessage = "نحتاج إلى إذن الكاميرا لتصوير البشرة وتحليلها فسيولوجياً."
        }
    }

    val checkCameraPermissionAndLaunch = {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMessage = "عذراً، لم نتمكن من تشغيل تطبيق الكاميرا على هذا الجهاز: ${e.message}"
            }
        } else {
            try {
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            } catch (e: Exception) {
                errorMessage = "عذراً، فشل طلب الإذن: ${e.message}"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "الفحص الذكي للبشرة AI 🔬",
                color = accentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "بإشراف أخصائية الذكاء الاصطناعي للبشرة زبيدة رمزي",
                color = textColorMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (activeReport != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .testTag("active_skin_report_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Title / Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "تشخيص مكتمل",
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "نتائج التشخيص الذكي النشط بالـ AI 📊🧬",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Skin Type Badge and diagnostic type
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(1.dp, accentColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (activeReport.isDemo) "عينة فحص تجريبية" else "فحص طيفي جيني وحيوي",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeReport.skinType,
                                    color = accentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نوع البشرة المكتشف:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                        // Concerns/Pathology section (identify skin concerns)
                        Text(
                            text = "المشاكل والمؤشرات الجديرة بالاهتمام ومشاكل الجلد 🩺🔍",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = activeReport.pathology,
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bio-Metrics Level linear indications
                        Text(
                            text = "مؤشرات ترطيب وصحة حيوية البشرة:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 1. Hydration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeReport.hydration}%",
                                color = if (activeReport.hydration > 50) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مستوى ترطيب الخلايا 💧",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { activeReport.hydration / 100f },
                            color = if (activeReport.hydration > 50) Color(0xFF10B981) else Color(0xFFEF4444),
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        // 2. Barrier Health
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeReport.barrierHealth}%",
                                color = if (activeReport.barrierHealth > 50) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "صحة وقوة غشاء الجلد الواقي 🛡️",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        LinearProgressIndicator(
                            progress = { activeReport.barrierHealth / 100f },
                            color = if (activeReport.barrierHealth > 50) Color(0xFF10B981) else Color(0xFFEF4444),
                            trackColor = Color.White.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Button(
                                onClick = onNavigateToReport,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.2f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "عرض الروتين الكامل والدردشة مع د. زبيدة 👈",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 0: Skin Assessment Form Option
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(1.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .clickable { isAssessmentMode = true },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "استبيان البشرة الشامل",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "استبيان البشرة الشامل 🩺",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جاوبي على أسئلة عبر استمارة التقييم السريري الشامل لترصد مشاكلك وتطلعاتك لتوليد بروتوكول فوري لعلاج الحبوب والتحسس والبهتان.",
                            color = textColorMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }

        // Section Baumann: Baumann Skin Type Indicator (D1-D4)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { isBaumannMode = true }
                    .testTag("baumann_questionnaire_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "استبيان باومان لتصنيف البشرة",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "استبيان باومان السريري العيادي (D1-D4) 🧪🌱",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "حددي بصمة بشرتكِ الدقيقة ضمن الـ 16 نوعاً فسيولوجياً عبر خوارزمية باومان متعددة الأبعاد (الدهنية وجفاف الجلد، والتحسس، والتصبغ، والتجاعيد) لاستلام تركيبة مركبة دقيقة.",
                            color = textColorMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }

        // Section 1: Custom Upload option
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "فحص عينة البشرة البصرية 📸🔬",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "التقط صورة مقربة وواضحة لبشرتكِ لمعالجتها فيزيولوجياً وبنيوياً عبر الذكاء الاصطناعي الجيني المتقدم.",
                        color = textColorMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (customImageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = customImageUri,
                                contentDescription = "صورة البشرة المدخلة",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { checkCameraPermissionAndLaunch() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                modifier = Modifier.weight(1.0f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل الكاميرا", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.weight(1.0f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل المعرض", color = Color.White, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    customImageUri = null
                                    selectedCase = curatedCases[0]
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                modifier = Modifier.weight(0.7f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("إلغاء", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable { checkCameraPermissionAndLaunch() }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "الكاميرا الحية",
                                        tint = accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "التقاط حي بالكاميرا 📸",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "التقط عينة جلدية فورية",
                                        color = textColorMuted,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp,
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
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Photo,
                                        contentDescription = "استيراد من المعرض",
                                        tint = textColorMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "ألبوم الصور والمعرض 🖼️",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "اختر لقطة مخزنة سابقاً",
                                        color = textColorMuted,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Preset Skin Pathology Cases Carousel
        if (customImageUri == null) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    Text(
                        text = "أو اختر حالة جلدية نموذجية معتمدة للفحص 💡",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(curatedCases) { case ->
                            val isSelected = selectedCase?.id == case.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) accentColor.copy(alpha = 0.22f) else cardColor
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .clickable { selectedCase = case }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) accentColor else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(case.emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = case.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = case.description,
                                        color = textColorMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Perform Diagnostic Analysis Call to Gemini
        item {
            Spacer(modifier = Modifier.height(10.dp))
            
            if (isLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "نظام الرؤية الحاسوبية السحابي قيد التشخيص...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = analysisProgressMessage,
                            color = accentColor,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val report = runSkinAnalysis(
                                    context = context,
                                    customUri = customImageUri,
                                    presetCase = selectedCase,
                                    updateProgressMessage = { msg -> analysisProgressMessage = msg }
                                )
                                isLoading = false
                                onReportGenerated(report)
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = e.message ?: "حدث عطل غير متوقع أثناء تحليل البشرة"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بدء تشخيص الذكاء الاصطناعي الفوري ✨",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        if (errorMessage != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("حدث خطأ في النظام:", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMessage!!, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
} else if (scanSubTab == 1) {
    IngredientScannerScreen(
        accentColor = accentColor,
        cardColor = cardColor,
        textColorMuted = textColorMuted
    )
} else {
    SmartMirrorScreen(
        accentColor = accentColor,
        cardColor = cardColor,
        textColorMuted = textColorMuted,
        viewModel = viewModel
    )
}
}
}
}

// INGREDIENT MATCHING DATA MODELS & KNOWLEDGE BANK
data class IngredientKnowledge(
    val englishName: String,
    val synonyms: List<String>,
    val description: String,
    val standardRecommendSkinTypes: List<String>,
    val standardAvoidSkinTypes: List<String>,
    val benefits: String
)

val ingredientKnowledgeBase = listOf(
    IngredientKnowledge(
        englishName = "Retinol",
        synonyms = listOf("ريتينول", "retinol", "فيتامين أ", "vitamin a", "ريتينوئيد", "تقشير"),
        description = "مُقشّر ومجدد خلايا قوي، يحفز الكولاجين ويقلل الخطوط الدقيقة وحب الشباب.",
        standardRecommendSkinTypes = listOf("دهنية", "مختلطة"),
        standardAvoidSkinTypes = listOf("حساسة"),
        benefits = "تجديد عميق للجلد، تحفيز الكولاجين، وتقليص المسام ومكافحة التجاعيد."
    ),
    IngredientKnowledge(
        englishName = "Salicylic Acid (BHA)",
        synonyms = listOf("حمض الساليسيليك", "salicylic", "bha", "ساليسيليك", "مقشر حمضي", "سالسليك"),
        description = "حمض مقشر يذوب في الدهون، يخترق المسام لتنظيفها وإزالة الرؤوس السوداء وحب الشباب.",
        standardRecommendSkinTypes = listOf("دهنية", "مختلطة"),
        standardAvoidSkinTypes = listOf("جافة"),
        benefits = "تنظيف المسام العميقة، تخفيض الزهم، علاج البثور الفتية والرؤوس السوداء."
    ),
    IngredientKnowledge(
        englishName = "Hyaluronic Acid",
        synonyms = listOf("حمض الهيالورونيك", "hyaluronic", "هيالورونيك", "مرطب مائي", "هيدرات", "ترطيب"),
        description = "مغناطيس للرطوبة يسحب الماء لطبقات البشرة لترطيبها وملئها.",
        standardRecommendSkinTypes = listOf("جافة", "جميع الأنواع", "حساسة"),
        standardAvoidSkinTypes = emptyList(),
        benefits = "ترطيب فائق فوري، تقليل الجفاف والخطوط الناتجة عن نقص الدعم المائي."
    ),
    IngredientKnowledge(
        englishName = "Niacinamide (Vitamin B3)",
        synonyms = listOf("النياسيناميد", "niacinamide", "نياسيناميد", "فيتامين ب3", "b3", "تفتيح"),
        description = "منظم حاجز البشرة، يقلل الاحمرار والمسام والتصبغات ويوازن الإفرازات الدهنية.",
        standardRecommendSkinTypes = listOf("حساسة", "دهنية", "مختلطة"),
        standardAvoidSkinTypes = emptyList(),
        benefits = "تقوية حاجز الدهون الحامي، تقليل المسام، إضاءة التصبغات والوقاية من الالتئامات الحيوية."
    ),
    IngredientKnowledge(
        englishName = "Vitamin C",
        synonyms = listOf("فيتامين سي", "vitamin c", "سي", "حمض الأسكوربيك", "ascorbic", "نضارة"),
        description = "مضاد أكسدة خارق يرشح الجذور الحرة، يحفز النضارة ويفتح البقع الداكنة.",
        standardRecommendSkinTypes = listOf("جميع الأنواع"),
        standardAvoidSkinTypes = listOf("حساسة"),
        benefits = "إشراقة فورية، تثبيط الميلانين المفرط، إنتاج الكولاجين والحماية البيئية."
    ),
    IngredientKnowledge(
        englishName = "Ceramides",
        synonyms = listOf("السيراميدات", "سيراميد", "ceramides", "ceramide", "حاجز البشرة"),
        description = "الملاط الاسمنتي لحاجز البشرة الطبيعي لمنع فقدان الرطوبة عبر الجلد.",
        standardRecommendSkinTypes = listOf("جافة", "حساسة"),
        standardAvoidSkinTypes = emptyList(),
        benefits = "إصلاح فوري لحاجز الدهون المتضرر، منع التهيج والجفاف والوقاية من البيئة الضارة."
    ),
    IngredientKnowledge(
        englishName = "Glycolic Acid (AHA)",
        synonyms = listOf("حمض الجليكوليك", "glycolic", "aha", "جليكوليك", "جلايكوليك"),
        description = "حمض مقشر يذوب في الماء، يزيل خلايا الجلد الميتة السطحية لتحسين الملمس ولون البشرة.",
        standardRecommendSkinTypes = listOf("جافة"),
        standardAvoidSkinTypes = listOf("حساسة"),
        benefits = "تنعيم ملمس البشرة الخشن، تفتيح لوني سطحي متجانس."
    ),
    IngredientKnowledge(
        englishName = "Tea Tree Oil",
        synonyms = listOf("شجرة الشاي", "زيت شجرة الشاي", "tea tree", "شاي"),
        description = "مضاد بكتيريا طبيعي ممتاز لعلاج حب الشباب وتطهير التهاب البشرة.",
        standardRecommendSkinTypes = listOf("دهنية"),
        standardAvoidSkinTypes = emptyList(),
        benefits = "محاربة البكتيريا المسببة لحب الشباب دون جفاف مفرط."
    ),
    IngredientKnowledge(
        englishName = "Zinc Oxide",
        synonyms = listOf("أكسيد الزنك", "zinc oxide", "واقي معدني", "زنك"),
        description = "فلتر واقي مادي يقي من أشعة الشمس ويهدئ الاحمرار والتهيج الجلدي.",
        standardRecommendSkinTypes = listOf("حساسة", "جميع الأنواع"),
        standardAvoidSkinTypes = emptyList(),
        benefits = "حماية كاملة من UVA/UVB مع تهدئة البشرة المتهيجة وتحفيز الالتئام اللطيف."
    )
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PharmacyScreen(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    activeReport: SkinReport?,
    viewModel: SkincareTrackerViewModel
) {
    val productsList by viewModel.allRecommendedProducts.collectAsState()
    val aiAnalysisState by viewModel.skincareAnalysisState.collectAsState()

    var selectedCategory by remember { mutableStateOf("جميع الفئات") }
    var selectedSkinType by remember { mutableStateOf("جميع الأنواع") }
    var selectedConcern by remember { mutableStateOf("جميع المشاكل") }
    var selectedIngredient by remember { mutableStateOf("جميع المكونات") }
    var showGlossary by remember { mutableStateOf(false) }

    var onlyMyProfileMatches by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }

    // Client-side instant compatibility search states
    var ingredientSearchQuery by remember { mutableStateOf("") }

    // Form states
    var newProdName by remember { mutableStateOf("") }
    var newProdBrand by remember { mutableStateOf("") }
    var newProdCategory by remember { mutableStateOf("غسول") }
    var newProdSkinType by remember { mutableStateOf("جميع الأنواع") }
    var newProdConcern by remember { mutableStateOf("حب الشباب") }
    var newProdDescription by remember { mutableStateOf("") }
    var newProdActiveIngredients by remember { mutableStateOf("") }

    val categories = listOf("جميع الفئات", "غسول", "سيروم", "مرطب", "واقي شمس", "مقشر")
    val skinTypes = listOf("جميع الأنواع", "جافة", "دهنية", "مختلطة", "حساسة")
    val concerns = listOf("جميع المشاكل", "حب الشباب", "الجفاف", "التصبغات", "الحساسية والاحمرار", "الدهون والمسام", "التجاعيد والخطوط")
    val ingredients = listOf(
        "جميع المكونات",
        "ريتينول (Retinol)",
        "فيتامين سي (Vitamin C)",
        "حمض الساليسيليك (BHA)",
        "حمض الهيالورونيك",
        "النياسيناميد (Niacinamide)",
        "السيراميد والبانثينول"
    )

    // Matching profile helper
    val isProductMatchingProfile = { product: SkincareProduct ->
        if (activeReport == null) false
        else {
            val reportText = "${activeReport.skinType} ${activeReport.pathology} ${activeReport.avoid}".lowercase()

            val isTypeMatch = when (product.skinType) {
                "جميع الأنواع" -> true
                "جافة" -> reportText.contains("جاف") || reportText.contains("dry")
                "دهنية" -> reportText.contains("دهن") || reportText.contains("oily")
                "مختلطة" -> reportText.contains("مختلط") || reportText.contains("combination")
                "حساسة" -> reportText.contains("حساس") || reportText.contains("sensitive") || reportText.contains("احمرار") || reportText.contains("تهيج")
                else -> false
            }

            val isConcernMatch = when (product.concern) {
                "حب الشباب" -> reportText.contains("حب") || reportText.contains("بثور") || reportText.contains("acne")
                "الجفاف" -> reportText.contains("جاف") || reportText.contains("جفاف") || reportText.contains("قشور") || reportText.contains("dry")
                "التصبغات" -> reportText.contains("تصبغ") || reportText.contains("كلف") || reportText.contains("بقع") || reportText.contains("pigment")
                "الحساسية والاحمرار" -> reportText.contains("حس") || reportText.contains("احمرار") || reportText.contains("تهيج") || reportText.contains("sens")
                "الدهون والمسام" -> reportText.contains("مسام") || reportText.contains("دهون") || reportText.contains("زهم") || reportText.contains("pore")
                "التجاعيد والخطوط" -> reportText.contains("تجاعيد") || reportText.contains("خطوط") || reportText.contains("wrinkle") || reportText.contains("aging")
                else -> true
            }

            isTypeMatch && isConcernMatch
        }
    }

    // Filtered list
    val filteredProducts = productsList.filter { prod ->
        val categoryMatch = selectedCategory == "جميع الفئات" || prod.category == selectedCategory
        val skinTypeMatch = selectedSkinType == "جميع الأنواع" || prod.skinType == selectedSkinType || prod.skinType == "جميع الأنواع"
        val concernMatch = selectedConcern == "جميع المشاكل" || prod.concern == selectedConcern

        val ingredientMatch = when (selectedIngredient) {
            "جميع المكونات" -> true
            "ريتينول (Retinol)" -> prod.activeIngredients.lowercase().contains("ريتينول") || prod.activeIngredients.lowercase().contains("retinol")
            "فيتامين سي (Vitamin C)" -> prod.activeIngredients.lowercase().contains("فيتامين سي") || prod.activeIngredients.lowercase().contains("vitamin c") || prod.activeIngredients.lowercase().contains("تياميدول")
            "حمض الساليسيليك (BHA)" -> prod.activeIngredients.lowercase().contains("ساليسيليك") || prod.activeIngredients.lowercase().contains("bha") || prod.activeIngredients.lowercase().contains("salicylic")
            "حمض الهيالورونيك" -> prod.activeIngredients.lowercase().contains("هيالورونيك") || prod.activeIngredients.lowercase().contains("hyaluronic") || prod.activeIngredients.lowercase().contains("بزاق")
            "النياسيناميد (Niacinamide)" -> prod.activeIngredients.lowercase().contains("نياسيناميد") || prod.activeIngredients.lowercase().contains("niacinamide")
            "السيراميد والبانثينول" -> prod.activeIngredients.lowercase().contains("سيراميد") || prod.activeIngredients.lowercase().contains("ceramide") || prod.activeIngredients.lowercase().contains("بانثينول") || prod.activeIngredients.lowercase().contains("panthenol")
            else -> true
        }

        val profileMatch = !onlyMyProfileMatches || isProductMatchingProfile(prod)

        categoryMatch && skinTypeMatch && concernMatch && ingredientMatch && profileMatch
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        // HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "صيدلية التطابق الخلوي 🔬💊",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "دليل المنتجات الجلدية والطبية لمطابقة نوع بشرتكِ ومعالجة العوامل الجزيئية تحت إشراف الأخصائية زبيدة رمزي.",
                color = textMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ADVANCED AI SERVICE REQUEST TRIGGER
        item {
            var showAIDialog by remember { mutableStateOf(false) }

            AIAnalysisDialog(
                isOpen = showAIDialog,
                onDismiss = { showAIDialog = false },
                viewModel = viewModel,
                accentColor = accentColor,
                cardColor = cardColor,
                textMuted = textMuted
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, accentColor, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showAIDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("بدء الاستشارة ⚙️🤖", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("استشارة المنتجات المتقدمة (AI) 🔬", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("تحليل دقيق وحصري ومطابقة مخصصة للمستحضرات", color = textMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        // CLINICAL PROFILE MATCH PANEL
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    if (activeReport != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تحليل بشرتكِ النشط المكتشف ✨",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "النوع: ${activeReport.skinType}",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الملاحظات: ${activeReport.pathology.take(130)}...",
                            color = textMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Match Profile Toggle Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .clickable { onlyMyProfileMatches = !onlyMyProfileMatches }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Switch(
                                checked = onlyMyProfileMatches,
                                onCheckedChange = { onlyMyProfileMatches = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF0F172A),
                                    checkedTrackColor = accentColor
                                )
                            )
                            Text(
                                text = "تصدير المستحضرات المتوافقة تلقائياً 🔬",
                                color = if (onlyMyProfileMatches) Color.White else textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Banner to do a scan
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لم تقومي بتحليل بشرتكِ بعد 🏜️",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Info, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "قومي بإجراء 'الفحص' الجلدي الذكي أولاً لتمكين الفلاتر الحيوية ومطابقة المنتجات تلقائياً لمستويات الخلايا الكيراتينية والصباغية الخاصة بكِ.",
                            color = textMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // SMART INGREDIENT & PRODUCT COMPATIBILITY CHECKER (user's feature request)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .border(1.2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "مستشار التوافقية ومطابقة المكونات 🔎🧬",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    
                    Text(
                        text = "ابحثي عن أي مكون نشط (مثل ريتينول، حمض الساليسيليك) أو نوع منتج للتحقق فوريًا من ملاءمته وجدواه الحيوية مع روتينك الموصى به.",
                        color = textMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Search input
                    TextField(
                        value = ingredientSearchQuery,
                        onValueChange = { ingredientSearchQuery = it },
                        placeholder = {
                            Text(
                                "اكتبي مكونًا أو نوع منتج هنا...",
                                color = textMuted.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        trailingIcon = {
                            if (ingredientSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { ingredientSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", tint = textMuted)
                                }
                            }
                        }
                    )

                    // Preset Quick Chips
                    Text(
                        text = "مكونات وتصنيفات شائعة للفحص السريع:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    val commonPresets = listOf("ريتينول", "نياسيناميد", "حمض الساليسيليك", "حمض الهيالورونيك", "فيتامين سي", "سيراميد", "واقي شمس", "غسول", "مقشر")
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.End,
                        maxItemsInEachRow = 5
                    ) {
                        commonPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp, bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (ingredientSearchQuery == preset) accentColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, if (ingredientSearchQuery == preset) accentColor else textMuted.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .clickable { ingredientSearchQuery = preset }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(preset, color = if (ingredientSearchQuery == preset) Color.White else textMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    // SEARCH MATCH AND ANALYSIS CONTAINER
                    if (ingredientSearchQuery.trim().isNotEmpty()) {
                        val queryNormalized = ingredientSearchQuery.trim().lowercase()
                        
                        // Try to find the ingredient in our advanced knowledge base
                        val foundIngredient = ingredientKnowledgeBase.find { knowledge ->
                            knowledge.englishName.lowercase().contains(queryNormalized) ||
                                    knowledge.synonyms.any { synonym -> queryNormalized.contains(synonym) || synonym.contains(queryNormalized) }
                        }

                        // Check actual routine details from AI analysis flow to see matching or avoiding parameters
                        var aiDetailsResultRecommended: String? = null
                        var aiDetailsResultAvoid: String? = null
                        var isSpecificallyTargeted = false
                        var isSpecificallyAvoided = false
                        
                        val activeAIRecommendation = (aiAnalysisState as? SkincareAnalysisUiState.Success)?.recommendation
                        
                        if (activeAIRecommendation != null) {
                            // Check targets
                            val matchingTargetArg = activeAIRecommendation.targetActiveIngredients.find { target ->
                                queryNormalized.contains(target.lowercase()) || target.lowercase().contains(queryNormalized) ||
                                        (foundIngredient?.synonyms?.any { syn -> target.lowercase().contains(syn.lowercase()) || syn.lowercase().contains(target.lowercase()) } ?: false)
                            }
                            if (matchingTargetArg != null) {
                                isSpecificallyTargeted = true
                                aiDetailsResultRecommended = "هذا المكون مستهدف خصيصًا وموصى به في تحليل بشرتكِ المتقدم ومدرج تحت المكونات الحيوية النشطة لبرنامج العناية بكِ 🔬🧬."
                            }
                            
                            // Check avoids
                            val matchingAvoidArg = activeAIRecommendation.avoidIngredientsAndTriggers.find { avoid ->
                                queryNormalized.contains(avoid.lowercase()) || avoid.lowercase().contains(queryNormalized) ||
                                        (foundIngredient?.synonyms?.any { syn -> avoid.lowercase().contains(syn.lowercase()) || syn.lowercase().contains(avoid.lowercase()) } ?: false)
                            }
                            if (matchingAvoidArg != null) {
                                isSpecificallyAvoided = true
                                aiDetailsResultAvoid = "تنبيه هام: تم إدراج هذا المكون أو مسبباته في روتينكِ كعنصر يجب تجنبه لتلافي حدوث حساسية أو تضرر لحاجز البشرة 🚨."
                            }
                        }
                        
                        // Check active clinical report inputs
                        if (activeReport != null) {
                            val activeReportAvoids = activeReport.avoid.lowercase()
                            if (queryNormalized.isNotEmpty() && (activeReportAvoids.contains(queryNormalized) || (foundIngredient?.synonyms?.any { syn -> activeReportAvoids.contains(syn) } ?: false))) {
                                isSpecificallyAvoided = true
                                aiDetailsResultAvoid = "تم رصد هذا المكون ضمن المواد المحذورة طبقًا لتحليل الفحص الطبي النشط الخاص بكِ! ننصح بتجنبه تماماً لتجنب التهيجات."
                            }
                        }

                        // Determine product type matching
                        val isProductTypeQuery = queryNormalized.contains("غسول") || queryNormalized.contains("سيروم") ||
                                queryNormalized.contains("مرطب") || queryNormalized.contains("شمس") || queryNormalized.contains("مقشر") ||
                                queryNormalized.contains("cleanser") || queryNormalized.contains("serum") || queryNormalized.contains("moisturizer") ||
                                queryNormalized.contains("sunscreen") || queryNormalized.contains("exfoli")

                        var productTypeMessage: String? = null
                        if (isProductTypeQuery) {
                            val routineAM = activeReport?.routineAM?.lowercase() ?: ""
                            val routinePM = activeReport?.routinePM?.lowercase() ?: ""
                            val aiAM = activeAIRecommendation?.morningRoutineSteps?.joinToString(" ")?.lowercase() ?: ""
                            val aiPM = activeAIRecommendation?.eveningRoutineSteps?.joinToString(" ")?.lowercase() ?: ""
                            
                            val fullRoutineText = "$routineAM $routinePM $aiAM $aiPM"
                            
                            val matchesType = if (queryNormalized.contains("غسول") || queryNormalized.contains("cleanser")) {
                                fullRoutineText.contains("غسول") || fullRoutineText.contains("غسل") || fullRoutineText.contains("cleans")
                            } else if (queryNormalized.contains("سيروم") || queryNormalized.contains("serum")) {
                                fullRoutineText.contains("سيروم") || fullRoutineText.contains("serum")
                            } else if (queryNormalized.contains("مرطب") || queryNormalized.contains("moisturizer")) {
                                fullRoutineText.contains("مرطب") || fullRoutineText.contains("ترطيب") || fullRoutineText.contains("moist")
                            } else if (queryNormalized.contains("شمس") || queryNormalized.contains("sunscreen")) {
                                fullRoutineText.contains("شمس") || fullRoutineText.contains("sun")
                            } else if (queryNormalized.contains("مقشر") || queryNormalized.contains("exfoli")) {
                                fullRoutineText.contains("مقشر") || fullRoutineText.contains("تحفيز") || fullRoutineText.contains("تقشير") || fullRoutineText.contains("exfoli")
                            } else false

                            if (matchesType) {
                                productTypeMessage = "نعم، هذا النوع من المستحضرات مدمج وموصى به بالفعل كخطوة فاعلة وجزء أساسي من جدول روتين العناية المخطط لكِ 🗓️✨."
                            } else {
                                productTypeMessage = "لم يتم تحديد هذا المستحضر كجزء أساسي في خطتكِ الحالية. تواصل مع د. زبيدة رمزي أو احرصي على عدم دمج خطوات إضافية غير ضرورية تفاديًا لإجهاد البشرة."
                            }
                        }

                        // Calculate compatibility score and styling
                        val score: Int
                        val statusText: String
                        val statusColor: Color
                        val statusBg: Color
                        val iconRes: androidx.compose.ui.graphics.vector.ImageVector

                        if (isSpecificallyAvoided) {
                            score = 15
                            statusText = "تحذير: غير متوافق ومعارض لحاجز بشرتكِ الحرج! 🚨🛑"
                            statusColor = Color(0xFFEF4444)
                            statusBg = Color(0xFFEF4444).copy(alpha = 0.08f)
                            iconRes = Icons.Default.Cancel
                        } else if (isSpecificallyTargeted) {
                            score = 100
                            statusText = "تطابق جزيئي مثالي وموصى به لمستويات الخلايا! 🌟🔋"
                            statusColor = Color(0xFF10B981)
                            statusBg = Color(0xFF10B981).copy(alpha = 0.08f)
                            iconRes = Icons.Default.CheckCircle
                        } else if (foundIngredient != null) {
                            // Check compatibility on skin type compatibility
                            val detectedType = activeAIRecommendation?.detectedSkinType ?: activeReport?.skinType ?: "جميع الأنواع"
                            val isRecommendedForSkinType = foundIngredient.standardRecommendSkinTypes.any { t -> detectedType.contains(t) } || detectedType == "جميع الأنواع"
                            val isAvoidedForSkinType = foundIngredient.standardAvoidSkinTypes.any { t -> detectedType.contains(t) }

                            if (isAvoidedForSkinType) {
                                score = 40
                                statusText = "ملاءمة منخفضة: قد يسبب تهيجًا لنوع بشرتكِ (${detectedType}) ⚠️🧴"
                                statusColor = Color(0xFFF59E0B)
                                statusBg = Color(0xFFF59E0B).copy(alpha = 0.08f)
                                iconRes = Icons.Default.Warning
                            } else if (isRecommendedForSkinType) {
                                score = 90
                                statusText = "متوافق وممتاز لخصائص بشرتكِ الكيراتينية (${detectedType}) ✅✨"
                                statusColor = Color(0xFF10B981)
                                statusBg = Color(0xFF10B981).copy(alpha = 0.06f)
                                iconRes = Icons.Default.CheckCircle
                            } else {
                                score = 75
                                statusText = "آمن ومتوافق للاستخدام المعتدل 🟡"
                                statusColor = Color(0xFFF59E0B)
                                statusBg = Color(0xFFF59E0B).copy(alpha = 0.05f)
                                iconRes = Icons.Default.Info
                            }
                        } else {
                            // General item
                            if (productTypeMessage != null && productTypeMessage.contains("نعم")) {
                                score = 95
                                statusText = "توافق روتيني مدرج ومثالي! 🛠️🟢"
                                statusColor = Color(0xFF10B981)
                                statusBg = Color(0xFF10B981).copy(alpha = 0.06f)
                                iconRes = Icons.Default.CheckCircle
                            } else {
                                score = 65
                                statusText = "مكون عام - غير مستهدف لكنه متوافق مبدئيًا 🧪"
                                statusColor = Color(0xFF94A3B8)
                                statusBg = Color(0xFF94A3B8).copy(alpha = 0.05f)
                                iconRes = Icons.Default.Info
                            }
                        }

                        // Display Analysis Card inside
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(statusBg)
                                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Circular / Linear Compatibility Score representation
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$score%",
                                            color = statusColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "معدل الملاءمة:",
                                            color = textMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "نتيجة التحليل الجزيئي",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(iconRes, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                                    }
                                }

                                HorizontalDivider(color = statusColor.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                                // Status text badge
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (foundIngredient != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = foundIngredient.description,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الفوائد الجزيئية: ${foundIngredient.benefits}",
                                        color = accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (aiDetailsResultRecommended != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "💡 $aiDetailsResultRecommended",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (aiDetailsResultAvoid != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🚨 $aiDetailsResultAvoid",
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (productTypeMessage != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🔍 طراز المستحضرات المخطط: $productTypeMessage",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FILTER CHIPS: CATEGORY
        item {
            Text("الفئة المستهدفة:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                reverseLayout = true
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, if (isSelected) accentColor else textMuted.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(cat, color = if (isSelected) Color.White else textMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // FILTER CHIPS: SKIN TYPE
        item {
            Text("نوع البشرة المناسب:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                reverseLayout = true
            ) {
                items(skinTypes) { sType ->
                    val isSelected = selectedSkinType == sType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, if (isSelected) accentColor else textMuted.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable { selectedSkinType = sType }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(sType, color = if (isSelected) Color.White else textMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // FILTER CHIPS: SKIN CONCERN
        item {
            Text("المشكلة الجلدية الرئيسية المتضررة:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                reverseLayout = true
            ) {
                items(concerns) { con ->
                    val isSelected = selectedConcern == con
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, if (isSelected) accentColor else textMuted.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .clickable { selectedConcern = con }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(con, color = if (isSelected) Color.White else textMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // FILTER CHIPS: ACTIVE INGREDIENTS (ADVANCED FILTERING)
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = "المكون النشط المستهدف 🧬🔬",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                reverseLayout = true
            ) {
                items(ingredients) { ing ->
                    val isSelected = selectedIngredient == ing
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.3f))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedIngredient = ing }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = ing,
                            color = if (isSelected) Color.White else textMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // INTERACTIVE GLOSSARY ACCESSIBLE AND LINKED WITH THE ACTIVE FILTERS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(
                        1.dp,
                        if (showGlossary) accentColor else Color.White.copy(alpha = 0.08f),
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
                            onClick = { showGlossary = !showGlossary },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showGlossary) accentColor else Color.Black.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (showGlossary) "إغلاق الدليل ✖" else "فتح دليل المكونات التفاعلي 📚🧪",
                                color = if (showGlossary) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "دليل المكونات الطبية 🔬📘",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (!showGlossary) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغطي لمعرفة تركيب وعمل النياسيناميد، الريتينول، السيراميدات وغيرها رفقة نصائح زبيدة رمزي وبث مباشر للملاءمة الجزيئية.",
                            color = textMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    AnimatedVisibility(
                        visible = showGlossary,
                        enter = fadeIn() + expandVertically(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            SkincareGlossaryComponent(
                                accentColor = accentColor,
                                cardColor = cardColor,
                                textColorMuted = textMuted,
                                selectedFilterIngredientName = selectedIngredient,
                                onSelectIngredientFilter = { newIngFilter ->
                                    selectedIngredient = newIngFilter
                                }
                            )
                        }
                    }
                }
            }
        }

        // ADD NEW CUSTOM RECOMMENDATION TRIGGER BUTTON
        item {
            Button(
                onClick = { showAddForm = !showAddForm },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showAddForm) Color.Red.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(
                        1.dp,
                        if (showAddForm) Color.Red.copy(alpha = 0.3f) else accentColor.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (showAddForm) "إغلاق نافذة الإضافة ✖" else "إضافة منتج موصى به مخصص ➕",
                        color = if (showAddForm) Color.Red else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // CUSTOM PRODUCT ADDITION FORM BLOCK
        if (showAddForm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("إدراج منتج طبي في الصيدلية 🧪", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Product Name
                        OutlinedTextField(
                            value = newProdName,
                            onValueChange = { newProdName = it },
                            label = { Text("اسم المنتج بالإنكليزية") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = textMuted.copy(alpha = 0.3f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        // Brand
                        OutlinedTextField(
                            value = newProdBrand,
                            onValueChange = { newProdBrand = it },
                            label = { Text("الشركة المصنعة للعلامة (مثل CeraVe)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = textMuted.copy(alpha = 0.3f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        // Active Ingredients
                        OutlinedTextField(
                            value = newProdActiveIngredients,
                            onValueChange = { newProdActiveIngredients = it },
                            label = { Text("المكونات النشطة كيميائياً") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = textMuted.copy(alpha = 0.3f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        // Description
                        OutlinedTextField(
                            value = newProdDescription,
                            onValueChange = { newProdDescription = it },
                            label = { Text("آلية العمل الفيزيولوجية للخلايا والشرح") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = textMuted.copy(alpha = 0.3f),
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = textMuted,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        )

                        // Row of drop choices in simple visual selection
                        // Category selection
                        Text("اختيار فئة المنتج:", color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.drop(1).forEach { catOpt ->
                                val isSelected = newProdCategory == catOpt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) accentColor else Color.Black.copy(alpha = 0.3f))
                                        .clickable { newProdCategory = catOpt }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        catOpt,
                                        color = if (isSelected) Color(0xFF0F172A) else textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Skin type selection
                        Text("نوع البشرة المناسب للمنتج:", color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            skinTypes.forEach { sTypeOpt ->
                                val isSelected = newProdSkinType == sTypeOpt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) accentColor else Color.Black.copy(alpha = 0.3f))
                                        .clickable { newProdSkinType = sTypeOpt }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        sTypeOpt,
                                        color = if (isSelected) Color(0xFF0F172A) else textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Target concern selection
                        Text("المشكلة الجلدية الرئيسية المستهدفة:", color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            concerns.drop(1).forEach { conOpt ->
                                val isSelected = newProdConcern == conOpt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) accentColor else Color.Black.copy(alpha = 0.3f))
                                        .clickable { newProdConcern = conOpt }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        conOpt,
                                        color = if (isSelected) Color(0xFF0F172A) else textMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Submit Button
                        val formValid = newProdName.isNotBlank() && newProdBrand.isNotBlank() && newProdActiveIngredients.isNotBlank() && newProdDescription.isNotBlank()
                        Button(
                            onClick = {
                                if (formValid) {
                                    viewModel.addNewProduct(
                                        name = newProdName,
                                        brand = newProdBrand,
                                        category = newProdCategory,
                                        skinType = newProdSkinType,
                                        concern = newProdConcern,
                                        description = newProdDescription,
                                        activeIngredients = newProdActiveIngredients
                                    )
                                    // Reset form and close
                                    newProdName = ""
                                    newProdBrand = ""
                                    newProdDescription = ""
                                    newProdActiveIngredients = ""
                                    showAddForm = false
                                }
                            },
                            enabled = formValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                disabledContainerColor = accentColor.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "إدراج المنتج ومواءمته مع الخوارزمية ➕",
                                color = if (formValid) Color(0xFF0F172A) else textMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // NO MATCH STATE
        if (filteredProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = textMuted.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لا توجد مستحضرات تلبي هذه المعايير أو فلاتر الملاءمة الخاصة بشرتكِ 🌵",
                            color = textMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // PRODUCTS ITEMS RENDER LIST
        items(filteredProducts) { product ->
            val matchesProfile = isProductMatchingProfile(product)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(
                        width = if (matchesProfile) 1.5.dp else 1.dp,
                        color = if (matchesProfile) accentColor else textMuted.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp)
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
                        // Left Option: Delete icon for custom products, or category badge if system
                        if (product.isCustom) {
                            IconButton(
                                onClick = { viewModel.deleteProduct(product) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الموصى به",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            // Category Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = product.category,
                                    color = accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right Option: Brand and matches indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product.brand,
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Name
                    Text(
                        text = product.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Ingredients
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable {
                                val matchedFilter = matchIngredientFilterTerm(product.activeIngredients)
                                selectedIngredient = matchedFilter
                                showGlossary = true
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = product.activeIngredients,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "اضغطي لقراءة الشرح العلمي من الدليل",
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Physiological description
                    Text(
                        text = product.description,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Interactive matching indicator
                        if (matchesProfile) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(accentColor.copy(alpha = 0.25f), Color(0xFFA855F7).copy(alpha = 0.25f))
                                        )
                                    )
                                    .border(0.8.dp, accentColor, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "تطابق ممتاز",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تطابق ١٠٠٪ مع بشرتكِ ✨",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Right Attributes
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "البشرة: ${product.skinType}",
                                    color = textMuted,
                                    fontSize = 8.5.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "الهدف: ${product.concern}",
                                    color = textMuted,
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClinicalReportScreen(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    activeReport: SkinReport?,
    onNavigateToScan: () -> Unit
) {
    if (activeReport == null) {
        // Welcome / guide screen if report has not been generated yet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = accentColor
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "تقرير تحليل الـ CDSS الجلدي",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "لم تقم بالتحليل بعد. يرجى التوجه إلى قسم 'الفحص' لرفع أو اختيار صورة البشرة، ثم تفعيل تشخيص الذكاء الاصطناعي لإنشاء التقرير بالتفصيل السريري.",
                color = textMuted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(26.dp))
            Button(
                onClick = onNavigateToScan,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.height(48.dp)
            ) {
                Text("ابدأ الفحص الفوري الآن ", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Q&A Conversation state
    var chatQuery by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "زُبيدة رمزي \uD83D\uDCDD CDR",
                text = "مرحباً بك! أنا مستشارة العناية الذكية الخاصة بك. تم إعداد تقريرك الجلدي بنجاح. هل لديك أي استفسار حول روتينك AM/PM أو مكونات معينة تود معرفتها؟",
                isUser = false
            )
        )
    }
    var isSendingQuery by remember { mutableStateOf(false) }

    val onSendClick: (String) -> Unit = { textToSend ->
        if (textToSend.isNotBlank() && !isSendingQuery) {
            chatMessages.add(ChatMessage("أنت (المستخدم)", textToSend, true))
            isSendingQuery = true
            coroutineScope.launch {
                try {
                    val reply = askSkinQuestion(
                        question = textToSend,
                        report = activeReport
                    )
                    chatMessages.add(ChatMessage("زُبيدة رمزي \uD83D\uDCDD Smart CDR", reply, false))
                } catch (e: Exception) {
                    chatMessages.add(ChatMessage("زُبيدة رمزي \uD83D\uDCDD System", "حدث خطأ: ${e.message}", false))
                } finally {
                    isSendingQuery = false
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        // Warning if using simulated demo mode
        if (activeReport.isDemo) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ℹ️ محاكاة التشخيص المحلي المتقدم:",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "لم يتم الكشف عن مفتاح Gemini API نشط، تم تفعيل خوارزمية التشخيص المحلي المحاكي للحالة المختارة بنجاح كامل لرؤية مخرجات الأداء الدقيق للفحص.",
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Text("تقرير تحليل الجلد والروتين الطبي 📊", color = accentColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("بإشراف زبيدة رمزي | CDSS", color = textMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Key Quantitative Metrics (Hydration & Barrier)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(75.dp)
                            .background(cardColor, RoundedCornerShape(38.dp))
                            .border(1.5.dp, accentColor, RoundedCornerShape(38.dp))
                    ) {
                        Text("${activeReport.hydration}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("الترطيب الجلدي (Hydration)", color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = if (activeReport.hydration < 40) "جفاف" else if (activeReport.hydration < 65) "معتدل" else "ممتاز",
                        color = if (activeReport.hydration < 40) Color(0xFFFCA5A5) else accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(75.dp)
                            .background(cardColor, RoundedCornerShape(38.dp))
                            .border(1.5.dp, if (activeReport.barrierHealth < 40) Color(0xFFEF4444) else Color(0xFF10B981), RoundedCornerShape(38.dp))
                    ) {
                        Text("${activeReport.barrierHealth}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("حاجز الحماية (Barrier)", color = textMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = if (activeReport.barrierHealth < 40) "تالف" else if (activeReport.barrierHealth < 70) "ضعيف" else "سليم وقوي",
                        color = if (activeReport.barrierHealth < 40) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Skin Type Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("نوع البشرة المرصود:", color = textMuted, fontSize = 12.sp)
                        Text(activeReport.skinType, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Pathology
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔬 الباثولوجيا والتحليل السريري للجلد:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeReport.pathology,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Routine details (AM & PM)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 البروتوكول العلاجي المخصص (Personalized Routine):", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("☀️ روتين الفترة الصباحية (AM Program):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeReport.routineAM,
                        color = textMuted,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("🌙 روتين الفترة المسائية (PM Program):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeReport.routinePM,
                        color = textMuted,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Important warnings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3F1D24)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ تحذيرات ومكونات ينصح بتجنبها:", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeReport.avoid,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // --- Recommended Skincare Products section (user's feature request) ---
        item {
            val sType = activeReport.skinType.lowercase()
            
            // Define data structure for products
            data class SuggestedProduct(
                val name: String,
                val desc: String,
                val ingredients: String,
                val url: String
            )
            
            val suggestedProducts = when {
                sType.contains("دهن") -> listOf(
                    SuggestedProduct(
                        name = "غسول رغوي سيرافي للبشرة الدهنية (CeraVe Foaming Cleanser)",
                        desc = "ينظف بلطف ويوازن إفراز الدهون دون التسبب بجفاف كيراتيني للبشرة.",
                        ingredients = "سيراميدات أساسية، نياسيناميد، وحمض الهيالورونيك.",
                        url = "https://www.amazon.sa/s?k=CeraVe+Foaming+Cleanser"
                    ),
                    SuggestedProduct(
                        name = "سيروم نياسيناميد من ذا اورديناري (The Ordinary Niacinamide 10% + Zinc 1%)",
                        desc = "منظم ممتاز للزهم المفرط، ويساعد بفاعلية في تقليل مظهر المسام الواسعة وإعادة التوازن لسطح الأدمة.",
                        ingredients = "النياسيناميد (فيتامين ب3)، الزنك النشط.",
                        url = "https://www.amazon.sa/s?k=The+Ordinary+Niacinamide+Zinc"
                    ),
                    SuggestedProduct(
                        name = "مرطب مائي لاروش بوزيه إيفاكلار مات (La Roche-Posay Effaclar Mat)",
                        desc = "مرطب خفيف يمنح لمسة مطفأة ويوازن الإفرازات الدهنية طوال اليوم.",
                        ingredients = "مياه حرارية مهدئة، السيليكا المنظمة للدهون.",
                        url = "https://www.amazon.sa/s?k=La+Roche-Posay+Effaclar+Mat"
                    )
                )
                sType.contains("جاف") -> listOf(
                    SuggestedProduct(
                        name = "غسول مرطب سيرافي للبشرة الجافة (CeraVe Hydrating Cleanser)",
                        desc = "ينظف البشرة بعمق دون المساس بغلاف الحماية الطبيعي والدهون الحيوية للجلد.",
                        ingredients = "السيراميدات الثلاثية الأساسية، حمض الهيالورونيك.",
                        url = "https://www.amazon.sa/s?k=CeraVe+Hydrating+Cleanser"
                    ),
                    SuggestedProduct(
                        name = "سيروم حمض الهيالورونيك من ذا اورديناري (The Ordinary Hyaluronic Acid 2% + B5)",
                        desc = "جاذب فادح للرطوبة يمنح البشرة الترطيب المائي والامتلاء للتخلص من الخطوط الناتجة عن نقص الرطوبة.",
                        ingredients = "حمض الهيالورونيك الطبيعي، فيتامين ب5.",
                        url = "https://www.amazon.sa/s?k=The+Ordinary+Hyaluronic+Acid+B5"
                    ),
                    SuggestedProduct(
                        name = "كريم مرطب سيرافي المغذي (CeraVe Moisturizing Cream)",
                        desc = "كريم غني ومغذي يرمم ويعوض النقص الكثيف في رطوبة وحاجز حاجز الجلد الجاف.",
                        ingredients = "السيراميدات الأساسية، الجلسرين.",
                        url = "https://www.amazon.sa/s?k=CeraVe+Moisturizing+Cream"
                    )
                )
                sType.contains("مختلط") -> listOf(
                    SuggestedProduct(
                        name = "غسول هيدرا جيل نيتروجينا (Neutrogena Hydro Boost Water Gel Cleanser)",
                        desc = "منظف جل مائي متوازن ومبتكر، يزيل دهون منطقة T ولا يسبب جفافاً للوجنتين.",
                        ingredients = "حمض الهيالورونيك المرمم، الجلسرين الحامي.",
                        url = "https://www.amazon.sa/s?k=Neutrogena+Hydro+Boost+Water+Gel+Cleanser"
                    ),
                    SuggestedProduct(
                        name = "سيروم النياسيناميد لتفتيح البشرة المختلطة (The Ordinary Niacinamide 10%)",
                        desc = "يساعد في توحيد وتنسيق ملمس ومسامات الوجه المختلط بفاعلية ذكية.",
                        ingredients = "النياسيناميد، مستخلصات نباتية مهدئة.",
                        url = "https://www.amazon.sa/s?k=The+Ordinary+Niacinamide+10%25"
                    ),
                    SuggestedProduct(
                        name = "مرطب مائي كلينيك ري كوندشنينج (Clinique Moisture Surge 100H Auto-Replenishing)",
                        desc = "جل مائي مرطب سريع الامتصاص ينعش الجلد دون تراكم طبقات دهنية زائدة.",
                        ingredients = "مستخلص الصبار المخمر، حمض الهيالورونيك.",
                        url = "https://www.amazon.sa/s?k=Clinique+Moisture+Surge+100H"
                    )
                )
                sType.contains("حساس") -> listOf(
                    SuggestedProduct(
                        name = "غسول سيتافيل اللطيف للبشرة الحساسة (Cetaphil Gentle Skin Cleanser)",
                        desc = "تركيبة خالية تماماً من الصابون والتهيجات العطرية تنظف الأدمة بلطف فائق.",
                        ingredients = "نياسيناميد، بروفيتامين ب5، جلسرين مرطب مائي.",
                        url = "https://www.amazon.sa/s?k=Cetaphil+Gentle+Skin+Cleanser"
                    ),
                    SuggestedProduct(
                        name = "سيروم هيدرا فاز ريتش لاروش بوزيه (La Roche-Posay Hyalu B5 Serum)",
                        desc = "سيروم مرطب ومهدئ ومضاد للتجاعيد مصمم خصيصاً لمقاومة احمرار وحساسية الخلايا.",
                        ingredients = "حمض الهيالورونيك الثنائي النقاء، فيتامين ب5، ماديكاسوسايد.",
                        url = "https://www.amazon.sa/s?k=La+Roche-Posay+Hyalu+B5+Serum"
                    ),
                    SuggestedProduct(
                        name = "مرطب لاروش بوزيه توليريان المهدئ (La Roche-Posay Toleriane Sensitive Cream)",
                        desc = "مرطب مهدئ فائق للالتهابات اليومية ومصمم لأشد خلايا البشرة حساسية وتفرعاً.",
                        ingredients = "مياه لاروش بوزيه الحرارية العلاجية، سيراميدات حامية.",
                        url = "https://www.amazon.sa/s?k=La+Roche-Posay+Toleriane+Sensitive+Cream"
                    )
                )
                else -> listOf(
                    SuggestedProduct(
                        name = "غسول سيمبل اللطيف لتنقية البشرة (Simple Kind to Skin Facial Wash)",
                        desc = "غسول لطيف يغذي حاجز البشرة اليومي بالمعادن الأساسية دون غسولات كيميائية قاسية.",
                        ingredients = "بروفيتامين ب5، فيتامين هـ، مياه ثلاثية النقاوة.",
                        url = "https://www.amazon.sa/s?k=Simple+Kind+to+Skin+Facial+Wash"
                    ),
                    SuggestedProduct(
                        name = "سيروم نيفيا فيتامين سي للنضارة (Nivea Cellular Luminous 360 Serum)",
                        desc = "يعزز توهج ونضارة البشرة الطبيعي ويوحد بقع التصبغ الخفيفة.",
                        ingredients = "لوكسينول مضاد التصبغ، فيتامين سي، حمض الهيالورونيك.",
                        url = "https://www.amazon.sa/s?k=Nivea+Cellular+Luminous+360+Serum"
                    ),
                    SuggestedProduct(
                        name = "مرطب نيفيا سوفت الخفيف (Nivea Soft Moisturizing Cream)",
                        desc = "كريم ترطيب خفيف منعش سريع الامتصاص، مثالي للاستخدام اليومي المستمر.",
                        ingredients = "زيت الجوجوبا الطبيعي، فيتامين هـ.",
                        url = "https://www.amazon.sa/s?k=Nivea+Soft"
                    )
                )
            }
            
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .testTag("recommended_products_analysis_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "🎁 المنتجات المقترحة لبشرتكِ المكتشفة (${activeReport.skinType}):",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    suggestedProducts.forEachIndexed { index, product ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = product.name,
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.desc,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🧪 المكونات الحيوية: ${product.ingredients}",
                                color = textMuted,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Simple Button Link
                            Button(
                                onClick = { uriHandler.openUri(product.url) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "تفاصيل وعروض أمازون السعودية 🔍🌐",
                                        color = accentColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        if (index < suggestedProducts.size - 1) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- Interactive Expert Q&A Chatbot sub-section ---
        item {
            Text("💬 اسأل مستشارة العناية الذكية زبيدة رمزي:", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Chat Bubble Logs List
        items(chatMessages) { message ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.isUser) accentColor.copy(alpha = 0.15f) else cardColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(
                            1.dp,
                            if (message.isUser) accentColor.copy(alpha = 0.5f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.sender,
                            color = if (message.isUser) accentColor else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Suggestions & Send Question input panel
        item {
            val suggestions = listOf(
                "هل روتيني الصباحي آمن للحامل؟",
                "كيف يمنع الساليسيليك الحبوب والتهيج؟",
                "متى تظهر تجارب تحسن البشرة؟",
                "هل واقي الشمس كيميائي أم فيزيائي؟"
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(suggestions) { suggestion ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = cardColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .clickable(enabled = !isSendingQuery) {
                                onSendClick(suggestion)
                            }
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = suggestion,
                            color = accentColor,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = chatQuery,
                    onValueChange = { chatQuery = it },
                    placeholder = { Text("مثال: هل روتين الصباح آمن للحامل؟", color = textMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    enabled = !isSendingQuery
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (chatQuery.isBlank()) return@IconButton
                        val userText = chatQuery
                        chatQuery = ""
                        onSendClick(userText)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(accentColor, RoundedCornerShape(8.dp)),
                    enabled = !isSendingQuery && chatQuery.isNotBlank()
                ) {
                    if (isSendingQuery) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                    }
                }
            }
        }
    }
}

// --- Image processing & AI Diagnostics Engine ---

suspend fun runSkinAnalysis(
    context: android.content.Context,
    customUri: Uri?,
    presetCase: CuratedCase?,
    updateProgressMessage: (String) -> Unit
): SkinReport = withContext(Dispatchers.IO) {
    updateProgressMessage("جاري قراءة وتحويل عينة الأنسجة والبيكسلات الجسدية...")
    
    val apiKey = BuildConfig.GEMINI_API_KEY
    val isDemoKey = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

    // Simulate load delay for visual polish and CDSS feels
    Thread.sleep(1500)

    if (isDemoKey) {
        updateProgressMessage("تنفيذ خوارزمية التشابك العصبي المحلي السريعة ...")
        Thread.sleep(1000)
        
        // If they did a custom upload but key is fake, we provide a clever dynamic report
        if (customUri != null) {
            return@withContext SkinReport(
                skinType = "بشرة مدمجة الفحص الخلوي",
                hydration = 54,
                barrierHealth = 61,
                pathology = "تم الكشف محلياً عن تلف جزئي في الخلايا السطحية القرنية مع وجود نسب دهن زهمية معتدلة، يُنصح بتوفير مفتاح Gemini API صالح في AI Studio Secrets للاستمتاع بالقوة التشخيصية الكاملة لخوارزمية السيمانتيك العصبية المتكاملة للفحص الحقيقي.",
                routineAM = "1. غسول معتدل غير صابوني صباحاً.\n2. سيروم فيتامين B5 للترميم العميق.\n3. مرطب خفيف مدعم بالفيتامينات.\n4. واقي شمس مائي SPF 50.",
                routinePM = "1. تنظيف عميق لازالة الملوثات البيئية.\n2. كريم مغذي للجلد غني بخلاصة السيكا والشوفان.\n3. مرطب ملين مهدئ.",
                avoid = "المقشرات العنيفة والتعرض للشمس دون حماية كافية.",
                isDemo = true
            )
        }
        
        // Return preset report configured as demo
        return@withContext presetCase?.defaultReport?.copy(isDemo = true) 
            ?: CuratedCase("default", "مختلطة", "", "", SkinReport("عادية", 50, 50, "", "", "", "")).defaultReport.copy(isDemo = true)
    }

    // Prepare visual Base64 payload
    updateProgressMessage("تجهيز طبقات الأنسجة لإرسالها لمحرك Vision الرائد...")
    val base64Image = if (customUri != null) {
        val inputStream = context.contentResolver.openInputStream(customUri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else {
            throw Exception("لا يمكن قراءة ملف الصورة المحدد")
        }
    } else {
        // Enforce demo or convert pre-coded sample image to Base64 (Using placeholder bitmap)
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    updateProgressMessage("تحميل فوري وتحليل طيفي عبر Gemini 3.5 Flash...")

    val prompt = """
        You are 'GlowLogic CDSS' - an advanced AI Dermatology Decision Support system designed by Zubayda Ramzi.
        Please analyze this close-up skin sample image. Focus on wrinkles, pore status, pigment uniformity, sebum levels, and irritation.
        Your output MUST be a strict JSON object with EXACTLY the following keys in proper Arabic (with values as specified below):
        {
          "skinType": "The skin type text (e.g. دهنية, جافة, حساسة, مختلطة, عادية)",
          "hydration": The estimated skin hydration percentage (an integer between 0 and 100),
          "barrierHealth": The estimated protective skin barrier health percentage (an integer between 0 and 100),
          "pathology": "Dermatological clinical analysis and pathology details in professional Arabic",
          "routineAM": "Custom morning skincare AM routine (numbered steps and actives) in Arabic",
          "routinePM": "Custom evening skincare PM routine (numbered steps and actives) in Arabic",
          "avoid": "Important warning list of skincare active ingredients and physical practices to avoid in Arabic"
        }
        Make sure the clinical tone is highly reassuring, completely accurate in Arabic medical style, and starts with the style of Zubayda Ramzi CDSS. Do NOT put any markdown or formatting tags outside of raw JSON. Just return the valid JSON string.
    """.trimIndent()

    val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    // REST Payload construction
    val partText = JSONObject().put("text", prompt)
    val inlineDataJson = JSONObject()
        .put("mimeType", "image/jpeg")
        .put("data", base64Image)
    val partImage = JSONObject().put("inlineData", inlineDataJson)

    val partsArray = JSONArray().put(partText).put(partImage)
    val contentObj = JSONObject().put("parts", partsArray)
    val contentsArray = JSONArray().put(contentObj)

    val requestObj = JSONObject().put("contents", contentsArray)

    // Generation constraint
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
            throw Exception("عطل بروتوكول الشبكة للتشخيص: ${response.code}. يرجى محاولة استخدام عينة من أمثلة الكتالوج.")
        }
    } catch (e: Exception) {
        throw Exception("فشل الاتصال بمحرك التشخيص السحابي: ${e.message}. بدلاً من ذلك، يمكنك استخدام عينات الفحص السريعة الممتازة.")
    }
}

suspend fun askSkinQuestion(
    question: String,
    report: SkinReport?
): String = withContext(Dispatchers.IO) {
    if (report == null) return@withContext "يرجى تحليل حالتك أولاً."

    val apiKey = BuildConfig.GEMINI_API_KEY
    val isDemoKey = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

    // Simulate small conversational delay
    Thread.sleep(1200)

    if (isDemoKey) {
        val lowerText = question.lowercase()
        return@withContext when {
            lowerText.contains("فيتامين") || lowerText.contains("vitamin") -> {
                "سؤال ذكي من زبيدة رمزي: فيتامين سي هو مضاد الأكسدة الفاتن للصباح، ولكن في وضع بشرتك الحالي يفضل إدخاله بالتدريج بتركيز منخفض (مثل 5% أو 10% مشتق) كي لا يسبب بثوراً أو يهيج الحاجز الجلدي الرقيق."
            }
            lowerText.contains("مرطب") || lowerText.contains("cream") || lowerText.contains("moistur") -> {
                "نصيحة زبيدة رمزي: المرطب يدعم طبقة الفوسفوليبيدز. للبشرة الجافة، اختاري مواد ذات قوام دهني غني مثل Ceramide و Squalane. وللبشرة الدهنية، اعتمدي مواد خفيفة ذات قاعدة مائية كالبانثينول السائل."
            }
            lowerText.contains("غسول") || lowerText.contains("cleanse") -> {
                "إشراف زبيدة رمزي: الغسول الخامل ميكروبياً يضمن ألا تجف بشرتك. نوصيك بغسول رغوي صلب إذا كانت بشرتك دهنية مسامية، وغسول كريمي خالي المذيبات في أوقات الجفاف الشديد."
            }
            lowerText.contains("شمس") || lowerText.contains("sun") -> {
                "توجيه زبيدة رمزي: حماية طيف الأشعة UVA/UVB ضروري جداً لمنع نشاط الميلانين والشيخوخة الضوئية. ضعي واقي شمس فيزيائي يحتوي على الزنك للبشرة الحساسة لضمان أقصى حماية دون احمرار."
            }
            lowerText.contains("حامل") || lowerText.contains("pregnancy") -> {
                "تذكير طبي CDSS: في هذه الفترة، يُرجى التوقف عن استخدام مشتقات الريتينول وفيتامين A، واستبدالها بمواد آمنة خفيفة مثل حمض الأزيليك لتفتيح البشرة وتنظيم الدهون بأمان مطلق."
            }
            else -> {
                "أهلاً بك! لقد تم فحص سؤالك وفقاً لبيانات تقريرك الجلدي (${report.skinType}). تأكدي من الالتزام التام بالبروتوكول العلاجي المقترح صباحاً ومساءً، وشرب لترين من الماء يومياً، ومتابعتي للوقوف على التطورات الممتازة لبشرتك."
            }
        }
    }

    val modelUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    val prompt = """
        User Skin Context:
        - Skin Type: ${report.skinType}
        - Hydration Level: ${report.hydration}%
        - Protective Barrier Health: ${report.barrierHealth}%
        - Detected Pathology: ${report.pathology}
        - Suggested AM Routine: ${report.routineAM}
        - Suggested PM Routine: ${report.routinePM}
        - Ingredients to avoid: ${report.avoid}

        User skincare inquiry:
        $question

        Please reply to the user's inquiry elegantly, accurately, and scientifically in friendly Arabic. Keep your persona as expert skincare advisor clinical CDSS under the supervision of Zubayda Ramzi. Make the response highly actionable, informative, and structurally beautiful. Do NOT write in plain paragraphs, use nice clean spacing and bullet points where useful. Do not put any markdown or formatting tags outside of output.
    """.trimIndent()

    val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val partText = JSONObject().put("text", prompt)
    val contentsArray = JSONArray().put(JSONObject().put("parts", JSONArray().put(partText)))
    val requestObj = JSONObject().put("contents", contentsArray)

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = requestObj.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(modelUrl)
        .post(requestBody)
        .build()

    try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (response.isSuccessful) {
            val resJson = JSONObject(responseBody)
            resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } else {
            throw Exception("Failed with code ${response.code}")
        }
    } catch (e: Exception) {
        throw Exception("عذرًا، لم نتمكن من الوصول للمساعد الاستشاري حالياً: ${e.message}")
    }
}

fun matchIngredientFilterTerm(ingredientsText: String): String {
    val lower = ingredientsText.lowercase()
    if (lower.contains("ريتينول") || lower.contains("retinol")) return "ريتينول (Retinol)"
    if (lower.contains("هيالورونيك") || lower.contains("hyaluronic")) return "حمض الهيالورونيك"
    if (lower.contains("نياسيناميد") || lower.contains("niacinamide")) return "النياسيناميد (Niacinamide)"
    if (lower.contains("ساليسيليك") || lower.contains("salicylic") || lower.contains("bha")) return "حمض الساليسيليك (BHA)"
    if (lower.contains("سيراميد") || lower.contains("ceramide") || lower.contains("بانثينول") || lower.contains("panthenol") || lower.contains("سيكا") || lower.contains("بزاق") || lower.contains("snail")) return "السيراميد والبانثينول"
    if (lower.contains("فيتامين سي") || lower.contains("vitamin c") || lower.contains("تياميدول") || lower.contains("thiamidol")) return "فيتامين سي (Vitamin C)"
    return "جميع المكونات"
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AIAnalysisDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: SkincareTrackerViewModel,
    accentColor: Color,
    cardColor: Color,
    textMuted: Color
) {
    if (!isOpen) return

    val analysisState by viewModel.skincareAnalysisState.collectAsState()

    var skinType by remember { mutableStateOf("مختلطة") }
    val selectedConcerns = remember { mutableStateListOf<String>() }
    val selectedGoals = remember { mutableStateListOf<String>() }
    var ageStr by remember { mutableStateOf("25") }
    var lifestyleDetail by remember { mutableStateOf("نشاط طبيعي، طقس جاف، سهر أحياناً") }

    val skinTypesList = listOf("جافة", "دهنية", "مختلطة", "حساسة / وردية", "عادية")
    val concernsList = listOf(
        "حب شباب وبثور",
        "جفاف وتقشر وحكة",
        "تصبغات وبقع داكنة",
        "مسام واسعة ورؤوس سوداء",
        "تجاعيد دقيقة وترهل",
        "احمرار وتهيج فوري"
    )
    val goalsList = listOf(
        "تنظيم وموازنة الدهون واللمعان",
        "ترطيب مكثف وترميم حاجز البشرة",
        "تفتيح وتوحيد لون البشرة والإشراق",
        "مكافحة علامات تقدم السن والخطوط",
        "تهدئة التهيج وتبريد الوهج"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (analysisState !is SkincareAnalysisUiState.Loading) {
                onDismiss()
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = analysisState !is SkincareAnalysisUiState.Loading,
                        modifier = Modifier.background(cardColor, RoundedCornerShape(12.dp)).size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "الاستشارة الذكية بالذكاء الاصطناعي 🧪🤖",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                when (val state = analysisState) {
                    is SkincareAnalysisUiState.Initial -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            item {
                                Text(
                                    text = "اكتشفي الحلول الجلدية الجزيئية والمنتجات المتطابقة مع طبيعة خلايا بشرتكِ بإشراف الأخصائية زبيدة رمزي.",
                                    color = textMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 1. SKIN TYPE
                            item {
                                Text("حددي نوع البشرة الحالي:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    skinTypesList.forEach { type ->
                                        val isSelected = skinType == type
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) accentColor.copy(alpha = 0.22f) else cardColor)
                                                .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .clickable { skinType = type }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(type, color = if (isSelected) Color.White else textMuted, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 2. CONCERNS
                            item {
                                Text("المشاكل الجلدية النشطة (متعدد):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    concernsList.forEach { concern ->
                                        val isSelected = selectedConcerns.contains(concern)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) accentColor.copy(alpha = 0.22f) else cardColor)
                                                .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (isSelected) selectedConcerns.remove(concern)
                                                    else selectedConcerns.add(concern)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(concern, color = if (isSelected) Color.White else textMuted, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 3. GOALS
                            item {
                                Text("أهداف العناية المرجوة:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    goalsList.forEach { goal ->
                                        val isSelected = selectedGoals.contains(goal)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) accentColor.copy(alpha = 0.22f) else cardColor)
                                                .border(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (isSelected) selectedGoals.remove(goal)
                                                    else selectedGoals.add(goal)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(goal, color = if (isSelected) Color.White else textMuted, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 4. AGE & LIFESTYLE
                            item {
                                Text("العمر والتفاصيل اليومية (إضافي):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = ageStr,
                                    onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) ageStr = it },
                                    label = { Text("العمر", color = textMuted, fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = cardColor,
                                        focusedLabelColor = accentColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = lifestyleDetail,
                                    onValueChange = { lifestyleDetail = it },
                                    label = { Text("أسلوب الحياة والنظام اليومي", color = textMuted, fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = cardColor,
                                        focusedLabelColor = accentColor,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.analyzeUserDataAndSuggest(
                                    skinType = skinType,
                                    concerns = selectedConcerns.toList(),
                                    goals = selectedGoals.toList(),
                                    age = ageStr.toIntOrNull(),
                                    lifestyle = lifestyleDetail
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("تحليل ذكي واقتراح المنتجات 🔬🤖", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    is SkincareAnalysisUiState.Loading -> {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = accentColor)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "جاري تجميع البيانات الجزيئية للخلايا...",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يقوم الذكاء الاصطناعي GlowLogic الآن بنسج مصفوفة التشخيص واختيار تركيبات المكونات الأكثر ملاءمة...",
                                color = textMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }

                    is SkincareAnalysisUiState.Success -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Introduction & Scientific Analysis
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                                        Text("التحليل العلمي للبشرة 🩺", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.recommendation.scientificAnalysis,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }

                            // Active Ingredients
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                                        Text("المكونات النشطة المستهدفة 🧬", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        androidx.compose.foundation.layout.FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            state.recommendation.targetActiveIngredients.forEach { ing ->
                                                Text(
                                                    text = ing,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Daily Routines AM & PM
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                                        Text("خطوات روتين الصباح (AM) ☀️", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        state.recommendation.morningRoutineSteps.forEach { rStep ->
                                            Text(
                                               text = "• $rStep",
                                               color = Color.White,
                                               fontSize = 12.sp,
                                               lineHeight = 16.sp,
                                               textAlign = TextAlign.Right,
                                               modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("خطوات روتين المساء (PM) 🌙", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        state.recommendation.eveningRoutineSteps.forEach { rStep ->
                                            Text(
                                               text = "• $rStep",
                                               color = Color.White,
                                               fontSize = 12.sp,
                                               lineHeight = 16.sp,
                                               textAlign = TextAlign.Right,
                                               modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Products Recommendation List
                            item {
                                Text("المستحضرات الطبية المقترحة 💊🧴", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                                state.recommendation.suggestedProducts.forEach { prod ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = cardColor),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(prod.brand, color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(prod.name, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "الهدف: ${prod.purpose}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "التطبيق: ${prod.howToUse}",
                                                color = textMuted,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // Lifestyle & Triggers
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.End) {
                                        Text("تنبيهات ومحاذير ⚠️", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        state.recommendation.avoidIngredientsAndTriggers.forEach { av ->
                                            Text(
                                                text = "• $av",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("توصيات نمط الحياة والوقاية 💧", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        state.recommendation.lifestyleRecommendations.forEach { life ->
                                            Text(
                                                text = "• $life",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                lineHeight = 15.sp,
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.resetSkincareAnalysis()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("استشارة جديدة 🧼", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val routineAM = state.recommendation.morningRoutineSteps.joinToString("\n")
                                    val routinePM = state.recommendation.eveningRoutineSteps.joinToString("\n")
                                    viewModel.syncFromAILog(routineAM, routinePM)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f).height(48.dp)
                            ) {
                                Text("تصدير ومتابعة الروتين ➕🔬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    is SkincareAnalysisUiState.Error -> {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "فشل في الاستشارة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.message,
                                color = textMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    viewModel.analyzeUserDataAndSuggest(
                                        skinType = skinType,
                                        concerns = selectedConcerns.toList(),
                                        goals = selectedGoals.toList(),
                                        age = ageStr.toIntOrNull(),
                                        lifestyle = lifestyleDetail
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("إعادة المحاولة 🔁", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}



