package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    viewModel: SkincareTrackerViewModel,
    onNavigateToScan: () -> Unit,
    onLoadReport: (SavedSkinReport) -> Unit
) {
    val savedReports by viewModel.allSavedReports.collectAsState()
    val waterLogs by viewModel.allWaterLogs.collectAsState()
    val completions by viewModel.allCompletions.collectAsState()
    val activeTasks by viewModel.allTasks.collectAsState()

    val bgDark = Color(0xFF0F172A)
    val accentOrange = Color(0xFFF97316) // consistency/streak accent
    val accentSky = Color(0xFF38BDF8) // water accent
    val accentEmerald = Color(0xFF10B981) // recovery accent

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayFormat = remember { SimpleDateFormat("E", Locale("ar")) }
    val dayFullFormat = remember { SimpleDateFormat("EEEE d MMMM", Locale("ar")) }

    var viewMode by remember { mutableStateOf("list") } // "list" or "calendar"
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedReportInCalendar by remember { mutableStateOf<SavedSkinReport?>(null) }

    // Sync selectedReportInCalendar when list updates
    LaunchedEffect(savedReports) {
        if (selectedReportInCalendar != null && !savedReports.any { it.id == selectedReportInCalendar!!.id }) {
            selectedReportInCalendar = savedReports.firstOrNull()
        }
    }

    // Calculate dates for the last 7 days (oldest to newest, left to right chronologically)
    val last7Days = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list.reverse()
        list
    }

    // Process daily metrics for the consistency chart
    val processedTrends = remember(last7Days, completions, waterLogs, activeTasks) {
        last7Days.map { date ->
            val dateStr = sdf.format(date)
            
            // completions
            val dateComps = completions.filter { it.dateStr == dateStr && it.isCompleted }
            val completedCount = dateComps.map { it.taskId }.distinct().size
            val totalActiveCount = activeTasks.size.coerceAtLeast(1)
            val routineProgress = (completedCount.toFloat() / totalActiveCount.toFloat()).coerceIn(0f, 1f)

            // water
            val wLog = waterLogs.find { it.dateStr == dateStr }
            val cupsDrank = wLog?.cupsDrank ?: 0
            val targetCups = wLog?.targetCups ?: 8
            val waterProgress = if (targetCups > 0) (cupsDrank.toFloat() / targetCups.toFloat()).coerceIn(0f, 1f) else 0f

            DailyMetricPoint(
                date = date,
                dateStr = dateStr,
                label = dayFormat.format(date),
                routineProgress = routineProgress,
                cupsDrank = cupsDrank,
                targetCups = targetCups,
                waterProgress = waterProgress
            )
        }
    }

    // Stats calculations
    val (consistencyIndex, avgHydrationCups, completedDays) = remember(processedTrends) {
        val completedDaysCount = processedTrends.count { it.routineProgress > 0f || it.cupsDrank > 0 }
        val consistencyPercentage = ((completedDaysCount.toFloat() / 7f) * 100).toInt()
        
        val avgWater = if (processedTrends.isNotEmpty()) {
            processedTrends.map { it.cupsDrank }.average()
        } else {
            0.0
        }
        
        Triple(consistencyPercentage, avgWater, completedDaysCount)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER ---
        item {
            Text(
                text = "سجل العناية والالتزام 📜",
                color = accentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تابعي رحلة توهج حاجز البشرة مع الأخصائية زبيدة رمزي",
                color = textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- 7-DAY CONSISTENCY DASHBOARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "مؤشرات الالتزام لآخر 7 أيام 📊",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sparkline/Column visual layout for last 7 days
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        processedTrends.forEach { metric ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // Double Bar Stack (Skincare progress and Water progress)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Water intake bar (Blue)
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .fillMaxHeight(metric.waterProgress)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(accentSky)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    // Skincare tasks completion bar (Green or Orange depending on progress)
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .fillMaxHeight(metric.routineProgress)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (metric.routineProgress >= 1f) accentEmerald else accentOrange
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = metric.label,
                                    color = if (metric.dateStr == sdf.format(Date())) accentColor else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (metric.dateStr == sdf.format(Date())) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legends / Key
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentSky)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الترطيب والماء", color = textMuted, fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اكتمال الروتين", color = textMuted, fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentOrange)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("روتين جزئي", color = textMuted, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary statistics boxes (Consistency % & Avg Hydration)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Consistency Index
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$consistencyIndex%",
                                    color = if (consistencyIndex > 60) accentEmerald else accentOrange,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "معدل الالتزام الكلي",
                                    color = textMuted,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Card 2: Average Hydration Cups
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f كوب", avgHydrationCups),
                                    color = accentSky,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "متوسط شرب الماء اليومي",
                                    color = textMuted,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Clinical Guideline text from Zubaida Ramzi
                    val clinicalInsight = when {
                        consistencyIndex == 0 -> "البداية تبدأ بخطوة صغيرة! قومي بتفعيل روتين اليوم لتبدأ عيادتنا تتبع نشاط تجديد خلايا الأدمة 🧬"
                        consistencyIndex < 45 -> "معدل الالتزام أقل من المتوسط. تذكري أن المكونات النشطة مثل الهيالورونيك والريتينول لا تعطي نتائجها الإعجازية إلا بالاستمرارية اليومية 🧪"
                        consistencyIndex < 80 -> "التزام رائع! بشرتكِ تسير في الاتجاه الصحيح. استمري هكذا لتتجاوزي مشاكل المسام والاحمرار بثقة ✨"
                        else -> "أداء استثنائي فائق! الخلايا في أعلى مستويات النشاط المناعي والتغذوي الداخلي، وحاجز بشرتك مقاوم تماماً للظروف الخارجية 🏆"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.05f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = clinicalInsight,
                            color = Color(0xFFA5F3FC),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Clinician Guideline",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- SKIN HEALTH HISTORICAL TREND CHART (Recharts Style) ---
        item {
            SkinHealthTrendChart(
                savedReports = savedReports,
                cardColor = cardColor,
                accentColor = accentColor,
                textMuted = textMuted
            )
        }

        // --- SUBTITLE LOG SECTION ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Number of stored reports badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${savedReports.size} فحص محفوظ",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "سجل الفحوصات والتقارير السريرية 🩺",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- VIEW MODE TOGGLE ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // List representation button
                Button(
                    onClick = { viewMode = "list" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewMode == "list") accentColor else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = if (viewMode == "list") Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "طريقة القائمة 📜",
                            color = if (viewMode == "list") Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Calendar representation button
                Button(
                    onClick = { viewMode = "calendar" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewMode == "calendar") accentColor else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (viewMode == "calendar") Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "طريقة التقويم 📅",
                            color = if (viewMode == "calendar") Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- CONTENT BASED ON VIEW MODE ---
        if (viewMode == "list") {
            if (savedReports.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "لا يوجد فحوصات",
                                tint = textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد فحوصات سابقة مخزنة",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ابدئي بالفحص الفوري بالمرآة السحابية عن طريق الضغط على زر الفحص لتوليد أول ملف تحليل متكامل.",
                                color = textMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToScan,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("تشخيص البشرة الآن ✨", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(savedReports) { report ->
                    SavedReportCard(
                        report = report,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textMuted = textMuted,
                        accentSky = accentSky,
                        accentEmerald = accentEmerald,
                        accentOrange = accentOrange,
                        dayFullFormat = dayFullFormat,
                        viewModel = viewModel,
                        onLoadReport = onLoadReport
                    )
                }
            }
        } else { // Calendar mode
            item {
                LaunchedEffect(savedReports, calendarMonth) {
                    if (selectedReportInCalendar == null && savedReports.isNotEmpty()) {
                        val yr = calendarMonth.get(Calendar.YEAR)
                        val mth = calendarMonth.get(Calendar.MONTH)
                        val matching = savedReports.filter { r ->
                            val rCal = Calendar.getInstance().apply { timeInMillis = r.timestamp }
                            rCal.get(Calendar.YEAR) == yr && rCal.get(Calendar.MONTH) == mth
                        }
                        if (matching.isNotEmpty()) {
                            selectedReportInCalendar = matching.first()
                        }
                    }
                }

                SkincareCalendarView(
                    savedReports = savedReports,
                    cardColor = cardColor,
                    accentColor = accentColor,
                    textMuted = textMuted,
                    accentSky = accentSky,
                    accentEmerald = accentEmerald,
                    accentOrange = accentOrange,
                    selectedReport = selectedReportInCalendar,
                    onSelectReport = { selectedReportInCalendar = it },
                    calendarMonth = calendarMonth,
                    onMonthChanged = { calendarMonth = it },
                    sdf = sdf
                )
            }

            if (selectedReportInCalendar != null) {
                item {
                    val reportDate = remember(selectedReportInCalendar!!.timestamp) {
                        try {
                            dayFullFormat.format(Date(selectedReportInCalendar!!.timestamp))
                        } catch (e: Exception) {
                            selectedReportInCalendar!!.dateStr
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تقرير العناية اليومية المحدّد: $reportDate 🔬",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }
                    SavedReportCard(
                        report = selectedReportInCalendar!!,
                        cardColor = cardColor,
                        accentColor = accentColor,
                        textMuted = textMuted,
                        accentSky = accentSky,
                        accentEmerald = accentEmerald,
                        accentOrange = accentOrange,
                        dayFullFormat = dayFullFormat,
                        viewModel = viewModel,
                        onLoadReport = onLoadReport
                    )
                }
            } else if (savedReports.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "اضغطي على يوم مروّس ومميز بنقطة 🩺 في التقويم أعلاه لعرض التقرير الطبي المفصّل والروتين العلاجي الخاص بذلك اليوم.",
                            color = textMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "لا توجد فحوصات محفوظة لعرضها في التقويم. ابدئي بالفحص الفوري بالمرآة السحابية أولاً لتوليد أول ملف تحليل.",
                            color = textMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedReportCard(
    report: SavedSkinReport,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    accentSky: Color,
    accentEmerald: Color,
    accentOrange: Color,
    dayFullFormat: SimpleDateFormat,
    viewModel: SkincareTrackerViewModel,
    onLoadReport: (SavedSkinReport) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayDate = remember(report.timestamp) {
        try {
            dayFullFormat.format(Date(report.timestamp))
        } catch (e: Exception) {
            report.dateStr
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(
                1.dp, 
                if (isExpanded) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), 
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title / Header of the Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.deleteSkinReport(report) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف ملف الفحص",
                        tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = report.skinType,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = displayDate,
                            color = textMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Double Gauges (Hydration & Barrier Health)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hydration Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("ترطيب الأدمة 💧", color = textMuted, fontSize = 9.sp)
                        Text("${report.hydration}%", color = accentSky, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { report.hydration.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = accentSky,
                            trackColor = accentSky.copy(alpha = 0.1f)
                        )
                    }
                }

                // Barrier Health Badge
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("حاجز البشرة 🛡️", color = textMuted, fontSize = 9.sp)
                        Text("${report.barrierHealth}%", color = accentEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { report.barrierHealth.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = accentEmerald,
                            trackColor = accentEmerald.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Clinical snippet
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.pathology,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = if (isExpanded) 10 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Expanded Area (Routines & Avoidances)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // AM routine
                    Text("روتين الصباح المعتمد ☀️", color = accentSky, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text(report.routineAM, color = textMuted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), textAlign = TextAlign.Right)

                    // PM routine
                    Text("روتين المساء المعتمد 🌙", color = accentOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text(report.routinePM, color = textMuted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), textAlign = TextAlign.Right)

                    // Avoid limits
                    Text("تجنبي تماماً ❌", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Text(report.avoid, color = textMuted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Panel (Expand & Switch clinical active state)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Load as Active Report
                Button(
                    onClick = { onLoadReport(report) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "تفعيل وتحميل التقرير 🔬",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Toggle Expansion
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (isExpanded) "عرض أقل" else "تفاصيل البروتوكول 🔍",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SkincareCalendarView(
    savedReports: List<SavedSkinReport>,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    accentSky: Color,
    accentEmerald: Color,
    accentOrange: Color,
    selectedReport: SavedSkinReport?,
    onSelectReport: (SavedSkinReport?) -> Unit,
    calendarMonth: Calendar,
    onMonthChanged: (Calendar) -> Unit,
    sdf: SimpleDateFormat
) {
    val monthYearFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("ar")) }
    val monthYearStr = remember(calendarMonth) { monthYearFormatter.format(calendarMonth.time) }

    // Sunday through Saturday headers in Arabic standard
    val weekDaysHeaders = listOf("ح", "ن", "ث", "ر", "خ", "ج", "س")

    // We start calculating first day of month and days count
    val daysCount = remember(calendarMonth) { calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val firstDayCal = remember(calendarMonth) {
        val f = calendarMonth.clone() as Calendar
        f.set(Calendar.DAY_OF_MONTH, 1)
        f
    }
    val startDayOfWeek = remember(firstDayCal) { 
        // Sunday = 1, Monday = 2 ... Saturday = 7
        firstDayCal.get(Calendar.DAY_OF_WEEK) 
    }

    val year = calendarMonth.get(Calendar.YEAR)
    val month = calendarMonth.get(Calendar.MONTH)

    val cells = remember(calendarMonth, startDayOfWeek, daysCount) {
        val list = mutableListOf<Int?>()
        // empty space for days before the first
        for (i in 1 until startDayOfWeek) {
            list.add(null)
        }
        for (d in 1..daysCount) {
            list.add(d)
        }
        list
    }

    val chunkedWeeks = remember(cells) { cells.chunked(7) }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val temp = calendarMonth.clone() as Calendar
                        temp.add(Calendar.MONTH, -1)
                        onMonthChanged(temp)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "الشهر السابق",
                        tint = accentColor
                    )
                }

                Text(
                    text = monthYearStr,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = {
                        val temp = calendarMonth.clone() as Calendar
                        temp.add(Calendar.MONTH, 1)
                        onMonthChanged(temp)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "الشهر التالي",
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekdays Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDaysHeaders.forEach { dayName ->
                    Text(
                        text = dayName,
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Divider
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            chunkedWeeks.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { day ->
                        if (day == null) {
                            Box(modifier = Modifier.weight(1f))
                        } else {
                            val cellCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }
                            val cellDateStr = sdf.format(cellCal.time)
                            val dayReports = savedReports.filter { 
                                it.dateStr == cellDateStr || sdf.format(Date(it.timestamp)) == cellDateStr
                            }
                            val hasReport = dayReports.isNotEmpty()
                            val isSelected = selectedReport != null && (
                                selectedReport.dateStr == cellDateStr || sdf.format(Date(selectedReport.timestamp)) == cellDateStr
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> accentColor.copy(alpha = 0.3f)
                                            hasReport -> accentColor.copy(alpha = 0.12f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else if (hasReport) 1.dp else 0.dp,
                                        color = if (isSelected) accentColor else if (hasReport) accentColor.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = true) {
                                        if (hasReport) {
                                            onSelectReport(dayReports.first())
                                        } else {
                                            onSelectReport(null)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$day",
                                        color = if (isSelected) accentColor else if (hasReport) Color.White else Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || hasReport) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (hasReport) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(accentColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (week.size < 7) {
                        for (i in 0 until (7 - week.size)) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

data class DailyMetricPoint(
    val date: Date,
    val dateStr: String,
    val label: String,
    val routineProgress: Float,
    val cupsDrank: Int,
    val targetCups: Int,
    val waterProgress: Float
)
