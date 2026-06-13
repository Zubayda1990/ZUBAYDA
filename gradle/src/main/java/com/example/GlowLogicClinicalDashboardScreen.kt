package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlowLogicClinicalDashboardScreen(
    currentTab: Int,
    onTabChange: (Int) -> Unit,
    onLaunchScan: () -> Unit,
    onLaunchTelemetry: () -> Unit,
    onLaunchFormulation: () -> Unit
) {
    var isArabic by remember { mutableStateOf(true) }
    
    // Theme palette definition as per strict requirements
    val primeNavy = Color(0xFF0A192F) // Deep clinical trust navy
    val actionAzure = Color(0xFF007BFF) // Clinical azure blue
    val sterileWhite = Color(0xFFFFFFFF)
    val softGray = Color(0xFFF8F9FA)
    val cardDark = Color(0xFF1E293B)
    val textMuted = Color(0xFF64748B)

    // Environmental metrics (simulated live data)
    var currentCityIndex by remember { mutableStateOf(0) }
    val citiesEn = listOf("Baghdad Medical Complex, IQ", "Amman Clinic District, JO", "Seoul Cosmetic Lab, KR", "London Research Centre, UK")
    val citiesAr = listOf("مجمع بغداد الطبي، العراق", "حي عيادات عمان، الأردن", "مختبر سيول للمستحضرات، كوريا", "مركز لندن لأبحاث الأدمة، بريطانيا")
    
    val temps = listOf(28, 16, 21, 11)
    val humidities = listOf(45, 65, 50, 75)

    val currentCity = if (isArabic) citiesAr[currentCityIndex] else citiesEn[currentCityIndex]
    val tempVal = temps[currentCityIndex]
    val humidityVal = humidities[currentCityIndex]

    // AI dynamic feedback statements based on weather indices
    val aiInsightEn = when (currentCityIndex) {
        0 -> "High humidity detected: Modifying pH algorithms for lightweight routine."
        1 -> "Dry, wind-prone climate: Accelerating lipid/ceramide accumulation rates."
        2 -> "Mild conditions: Balanced microbiome nourishment routine stabilized."
        else -> "Damp cool index: Adjusting non-comedogenic water levels for high skin slip."
    }

    val aiInsightAr = when (currentCityIndex) {
        0 -> "تم رصد رطوبة عالية: يجري تعديل خوارزميات الـ pH لروتين مائي خفاف خفيف خالي من التلصق."
        1 -> "مناخ جاف وعاصف: يجري تسريع دمج سيراميد حاجز الأدمة لحفظ الرطوبة الدقيقة."
        2 -> "البيئة معتدلة متكاملة: تتبع الصيغة العلاجية القياسية لدعم المايكروبيوم الجلدي."
        else -> "رطوبة مائية مرتفعة مع برودة: تعديل نسب المواد المانعة لتعظيم كفاءة النفوذ المسامي."
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(softGray),
        containerColor = softGray,
        topBar = {
            // Requirement 2: Dynamic Header & Bilingual Architecture (LTR/RTL shifts automatically via content alignment)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primeNavy)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isArabic) {
                        // Language Toggle switch (Left when Arabic)
                        BilingualToggleBtn(isArabic) { isArabic = !isArabic }
                        
                        // Logo (Right when Arabic)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "غلو-لوجيك",
                                color = sterileWhite,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = actionAzure,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Logo (Left when English)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalHospital,
                                contentDescription = null,
                                tint = actionAzure,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                    text = "GlowLogic",
                                color = sterileWhite,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Language Toggle switch (Right when English)
                        BilingualToggleBtn(isArabic) { isArabic = !isArabic }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Intro text
                Text(
                    text = if (isArabic) "بوابة التحليل البيولوجي والتشخيص السريري" else "Clinical Biological Diagnostic Hub",
                    color = primeNavy,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                // Requirement 3: Geo-Dermatological Widget (Hero Section)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .border(
                            BorderStroke(1.dp, actionAzure.copy(alpha = 0.15f)),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            // Cycle city
                            currentCityIndex = (currentCityIndex + 1) % citiesEn.size
                        },
                    colors = CardDefaults.cardColors(containerColor = sterileWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isArabic) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = actionAzure,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CURRENT LOCATION HUD",
                                        color = primeNavy,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10b981).copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AUTO-GPS",
                                        color = Color(0xFF059669),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10b981).copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "تتبع فوري تلقائي",
                                        color = Color(0xFF059669),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "بيانات الموقع والطقس العام",
                                        color = primeNavy,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = actionAzure,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = currentCity,
                            color = textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Environmental Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Temp block
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(softGray, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isArabic) "حرارة الجو" else "TEMP INDICES",
                                        color = textMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$tempVal°C / ${(tempVal * 1.8 + 32).toInt()}°F",
                                        color = primeNavy,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            // Humidity block
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(softGray, RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Opacity,
                                    contentDescription = null,
                                    tint = actionAzure,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isArabic) "رطوبة المناخ" else "HUMIDITY INDEX",
                                        color = textMuted,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$humidityVal% RH",
                                        color = primeNavy,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // AI Clinical Insight
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(actionAzure.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .border(BorderStroke(0.5.dp, actionAzure.copy(alpha = 0.12f)), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isArabic) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = actionAzure,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "استشراف الذكاء الاصطناعي الأدمي:" else "AI Actionable Insight:",
                                    color = actionAzure,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isArabic) aiInsightAr else aiInsightEn,
                                    color = primeNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 13.sp,
                                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (isArabic) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = actionAzure,
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Grid Title
                Text(
                    text = if (isArabic) "بوابة التوجيهات الطبية والتقييم" else "Clinical Control Grid",
                    color = primeNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                )

                // Requirement 4: Core Clinical Navigation (Grid Layout)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Action 1: AI Clinical Skin Scan
                        GridItemCard(
                            title = if (isArabic) "فحص عزل الخلايا بالذكاء 🔬" else "AI Clinical Skin Scan",
                            desc = if (isArabic) "بدء المراقبة البصرية لأنسجة الجلد" else "Real-time computer vision scan",
                            icon = Icons.Default.Expand,
                            accentColor = actionAzure,
                            primeNavy = primeNavy,
                            isArabic = isArabic,
                            modifier = Modifier.weight(1f).testTag("clinical_scan_card"),
                            onClick = onLaunchScan
                        )

                        // Action 2: pH & Microbiome Telemetry
                        GridItemCard(
                            title = if (isArabic) "مقياس درجة الحموضة والحاجز 🧬" else "pH & Microbiome Telemetry",
                            desc = if (isArabic) "تحليل مستويات كيمياء الطبقة القرنية" else "Analyze molecular epidermis pH",
                            icon = Icons.Default.Science,
                            accentColor = actionAzure,
                            primeNavy = primeNavy,
                            isArabic = isArabic,
                            modifier = Modifier.weight(1f).testTag("clinical_telemetry_card"),
                            onClick = onLaunchTelemetry
                        )
                    }

                    // Action 3 Custom Banner Layout: "Bespoke Dermatological Routine"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.5.dp, actionAzure), RoundedCornerShape(16.dp))
                            .clickable(onClick = onLaunchFormulation)
                            .testTag("action_formulation_bar"),
                        colors = CardDefaults.cardColors(containerColor = primeNavy),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isArabic) {
                                Icon(
                                    imageVector = Icons.Default.MedicalInformation,
                                    contentDescription = null,
                                    tint = actionAzure,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = if (isArabic) "التركيبة العلاجية المخصصة (Bespoke Recipe)" else "Bespoke Dermatological Routine",
                                    color = sterileWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isArabic) "الحل الكيميائي المطور للأسبوع الرابع خالي من المخرشات" else "Excluding allergens, calculated precisely in active matrix V4",
                                    color = sterileWhite.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isArabic) {
                                Spacer(modifier = Modifier.width(14.dp))
                                Icon(
                                    imageVector = Icons.Default.MedicalInformation,
                                    contentDescription = null,
                                    tint = actionAzure,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Requirement 5: Authorship & Watermark (Strict proprietary requirement)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sterileWhite)
                    .border(width = 0.5.dp, color = Color(0xFFE2E8F0))
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isArabic) "تأسيس وتطوير: زبيدة رمزي حسنون" else "Founded & Developed by: Zubayda Ramzi hasanain",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(primeNavy, actionAzure)
                            )
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "GlowLogic Clinical Specification System Model • V4",
                        color = textMuted,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BilingualToggleBtn(
    isArabic: Boolean,
    onLangToggle: () -> Unit
) {
    TextButton(
        onClick = onLangToggle,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isArabic) "English EN" else "العربية AR",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun GridItemCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    primeNavy: Color,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = primeNavy,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
