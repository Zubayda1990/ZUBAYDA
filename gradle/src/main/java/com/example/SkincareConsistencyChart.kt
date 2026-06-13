package com.example

import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class WeeklyConsistencyData(
    val dateStr: String,
    val label: String,
    val routineCompleted: Int,
    val routineTotal: Int,
    val routinePercent: Int,
    val waterCups: Int,
    val waterTarget: Int,
    val waterPercent: Int
)

@Composable
fun SkincareConsistencyChart(
    allCompletions: List<TaskCompletionLog>,
    allWaterLogs: List<WaterLog>,
    allTasks: List<SkincareTask>,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color
) {
    val context = LocalContext.current

    // Toggle states matching Recharts legends filter
    var showRoutine by remember { mutableStateOf(true) }
    var showWater by remember { mutableStateOf(true) }

    // Hover or Tap index tracker for floating tooltips
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val accentEmerald = Color(0xFF10B981) // Routine line color (Emerald)
    val accentSky = Color(0xFF38BDF8)     // Water intake line color (Sky Blue)

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val labelFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale("ar")) }
    val shortLabelFormat = remember { SimpleDateFormat("E d/M", Locale("ar")) }

    // Generate past 7 days starting from 6 days ago up to today
    val dates = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Process daily statistics
    val chartData = remember(allCompletions, allWaterLogs, allTasks) {
        dates.map { date ->
            val dateStr = sdf.format(date)
            
            // Format dates
            val fullLabel = labelFormat.format(date)
            var shortLabel = shortLabelFormat.format(date)
            // Replace full day names with custom shortened values if needed
            shortLabel = shortLabel
                .replace("الأحد", "أحد")
                .replace("الاثنين", "إثنين")
                .replace("الثلاثاء", "ثلاثاء")
                .replace("الأربعاء", "أربعاء")
                .replace("الخميس", "خميس")
                .replace("الجمعة", "جمعة")
                .replace("السبت", "سبت")

            val logs = allCompletions.filter { it.dateStr == dateStr && it.isCompleted }
            val doneTasks = logs.size
            val totalTasks = allTasks.size.coerceAtLeast(1)
            val routinePercent = ((doneTasks.toFloat() / totalTasks.toFloat()) * 100).toInt().coerceIn(0, 100)

            val water = allWaterLogs.firstOrNull { it.dateStr == dateStr }
            val cups = water?.cupsDrank ?: 0
            val targetCups = water?.targetCups ?: 8
            val waterPercent = ((cups.toFloat() / targetCups.toFloat()) * 100).toInt().coerceIn(0, 100)

            WeeklyConsistencyData(
                dateStr = dateStr,
                label = shortLabel,
                routineCompleted = doneTasks,
                routineTotal = totalTasks,
                routinePercent = routinePercent,
                waterCups = cups,
                waterTarget = targetCups,
                waterPercent = waterPercent
            )
        }
    }

    val totalPoints = chartData.size

    // Auto-select today coordinate on first render
    LaunchedEffect(chartData) {
        if (chartData.isNotEmpty()) {
            selectedIndex = chartData.size - 1
        }
    }

    val activePoint = selectedIndex?.let { if (it in chartData.indices) chartData[it] else null }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Header block resembling premium analytics panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )

                Text(
                    text = "مؤشر الالتزام والترطيب الأسبوعي 📈",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "تحليل مرئي لحساب التزامكِ المائي والأدمي عبر قياس معدلات إكمال روتينكِ اليومي من المهام وكؤوس الماء طوال الأسبوع.",
                color = textMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RECHARTS LEGENDS FILTER (Toggle lines plots dynamically)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Routine Completion Plot Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showRoutine) accentEmerald.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { showRoutine = !showRoutine }
                        .border(
                            1.dp,
                            if (showRoutine) accentEmerald.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إلتزام الروتين % 🧪",
                        color = if (showRoutine) Color.White else textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Water Intake Plot Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showWater) accentSky.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { showWater = !showWater }
                        .border(
                            1.dp,
                            if (showWater) accentSky.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentSky)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الترطيب المائي % 💧",
                        color = if (showWater) Color.White else textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // DYNAMIC FLOATING RECHARTS TOOLTIP INFO CARD
            AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (activePoint != null) {
                    // Calculate clinical consistency rating and custom tips from Dr. Zubayda Ramzi
                    val avgPercent = when {
                        showRoutine && showWater -> (activePoint.routinePercent + activePoint.waterPercent) / 2
                        showRoutine -> activePoint.routinePercent
                        showWater -> activePoint.waterPercent
                        else -> 0
                    }

                    val (ratingText, ratingColor, doctorMessage) = when {
                        avgPercent >= 85 -> Triple(
                            "التزام نخبوي فائق 👑",
                            accentEmerald,
                            "انتظام مذهل يحفز حيوية الخلايا وجاهزية استقبال المغذيات وإعادة ترميم الأدمة من الأعماق!"
                        )
                        avgPercent >= 50 -> Triple(
                            "التزام متوسط ومقبول 🌤️",
                            Color(0xFFFBBF24), // Gold
                            "الالتزام جيد، لكن ترطيب الأدمة العضوي المستدام يتطلب التتابع اليومي دون انقطاع لتنظيم الإفرازات الزهمية."
                        )
                        else -> Triple(
                            "يحتاج لتنشيط الالتزام 🏜️",
                            Color(0xFFEF4444), // Red
                            "البشرة تحتاج للاستقرار؛ تذكري أن حماية الخلايا وبناء السيراميدات تتبدد مع الغياب العلاجي الطويل."
                        )
                    }

                    // Look up long date for title
                    val formattedLongDate = remember(activePoint.dateStr) {
                        try {
                            val parsed = sdf.parse(activePoint.dateStr)
                            labelFormat.format(parsed)
                        } catch (e: Exception) {
                            activePoint.dateStr
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Date and Consistency Level Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ratingColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = ratingText,
                                        color = ratingColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Text(
                                    text = formattedLongDate,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Stats Rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showWater) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${activePoint.waterPercent}%",
                                            color = accentSky,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "(${activePoint.waterCups}/${activePoint.waterTarget} كؤوس)",
                                            color = textMuted,
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "الترطيب المائي:",
                                            color = textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (showRoutine) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${activePoint.routinePercent}%",
                                            color = accentEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "(${activePoint.routineCompleted}/${activePoint.routineTotal} خطوات)",
                                            color = textMuted,
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "إنجاز الروتين:",
                                            color = textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Micro clinical tip from Zubayda Ramzi
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "توصية الأخصائية زبيدة: $doctorMessage",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // RECHARTS CANVAS (Draw gridlines and line series)
            val density = LocalDensity.current
            val basePadding = with(density) { 32.dp.toPx() }
            val textLeftPadding = with(density) { 42.dp.toPx() } // Label grid size
            val bottomPadding = with(density) { 24.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chartData) {
                            detectTapGestures { offset ->
                                if (totalPoints > 1) {
                                    val canvasWidth = size.width
                                    val workableWidth = canvasWidth - basePadding - textLeftPadding
                                    val stepX = workableWidth / (totalPoints - 1)

                                    var closestIndex = 0
                                    var minDistance = Float.MAX_VALUE

                                    for (i in 0 until totalPoints) {
                                        val pointX = textLeftPadding + (i * stepX)
                                        val dist = kotlin.math.abs(offset.x - pointX)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIndex = i
                                        }
                                    }
                                    selectedIndex = closestIndex
                                } else if (totalPoints == 1) {
                                    selectedIndex = 0
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val workableWidth = canvasWidth - basePadding - textLeftPadding
                    val workableHeight = canvasHeight - basePadding - bottomPadding

                    // Map value percent (0..100) to Canvas Offset
                    val getCoordinates = { percentValue: Int, index: Int ->
                        val x = textLeftPadding + if (totalPoints > 1) {
                            index * (workableWidth / (totalPoints - 1))
                        } else {
                            workableWidth / 2f
                        }

                        val percentage = percentValue.toFloat() / 100f
                        val y = basePadding + (workableHeight * (1f - percentage))

                        Offset(x, y)
                    }

                    // DRAW RECHARTS BACKGROUND RULER GRIDLINES
                    val yLevels = listOf(0f, 0.25f, 0.50f, 0.75f, 1f)
                    val paintLabel = Paint().apply {
                        color = android.graphics.Color.parseColor("#475569") // Slate-500
                        textSize = 24f
                        textAlign = Paint.Align.RIGHT
                        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    }

                    val dateLabelPaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8") // Slate-400
                        textSize = 21f
                        textAlign = Paint.Align.CENTER
                    }

                    yLevels.forEach { level ->
                        val y = basePadding + (workableHeight * (1f - level))
                        // Dashed horizontal guidelines
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(textLeftPadding, y),
                            end = Offset(canvasWidth - basePadding, y),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        // Vertical Axis Ruler labels (0%, 25%, 50%, 75%, 100%)
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(level * 100).toInt()}%",
                            textLeftPadding - 12f,
                            y + 8f,
                            paintLabel
                        )
                    }

                    // Tap/Focus Indicator guideline (Vertical dashed line)
                    selectedIndex?.let { index ->
                        if (index in 0 until totalPoints) {
                            val selX = textLeftPadding + if (totalPoints > 1) {
                                index * (workableWidth / (totalPoints - 1))
                            } else {
                                workableWidth / 2f
                            }
                            drawLine(
                                color = accentColor.copy(alpha = 0.4f),
                                start = Offset(selX, basePadding),
                                end = Offset(selX, canvasHeight - bottomPadding),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        }
                    }

                    // 1. DRAW ROUTINE COMPLETION LINE Plot & Area filling
                    if (showRoutine && totalPoints > 0) {
                        val points = chartData.mapIndexed { idx, item -> getCoordinates(item.routinePercent, idx) }

                        // Area Gradient fill underneath
                        val areaPath = Path()
                        if (points.isNotEmpty()) {
                            areaPath.moveTo(points[0].x, canvasHeight - bottomPadding)
                            points.forEach { pt -> areaPath.lineTo(pt.x, pt.y) }
                            areaPath.lineTo(points.last().x, canvasHeight - bottomPadding)
                            areaPath.close()

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(accentEmerald.copy(alpha = 0.22f), Color.Transparent),
                                    startY = basePadding,
                                    endY = canvasHeight - bottomPadding
                                )
                            )
                        }

                        // Curve stroke
                        val strokePath = Path()
                        if (points.isNotEmpty()) {
                            strokePath.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                strokePath.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = strokePath,
                                color = accentEmerald,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }

                        // Bullet nodes
                        points.forEachIndexed { i, pt ->
                            val isSelected = selectedIndex == i
                            drawCircle(
                                color = if (isSelected) Color.White else accentEmerald,
                                radius = if (isSelected) 8.5f else 5f,
                                center = pt
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = accentEmerald,
                                    radius = 14f,
                                    center = pt,
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }

                    // 2. DRAW WATER INTAKE LINE Plot & Area filling
                    if (showWater && totalPoints > 0) {
                        val points = chartData.mapIndexed { idx, item -> getCoordinates(item.waterPercent, idx) }

                        // Area Gradient fill underneath
                        val areaPath = Path()
                        if (points.isNotEmpty()) {
                            areaPath.moveTo(points[0].x, canvasHeight - bottomPadding)
                            points.forEach { pt -> areaPath.lineTo(pt.x, pt.y) }
                            areaPath.lineTo(points.last().x, canvasHeight - bottomPadding)
                            areaPath.close()

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(accentSky.copy(alpha = 0.22f), Color.Transparent),
                                    startY = basePadding,
                                    endY = canvasHeight - bottomPadding
                                )
                            )
                        }

                        // Curve stroke
                        val strokePath = Path()
                        if (points.isNotEmpty()) {
                            strokePath.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                strokePath.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = strokePath,
                                color = accentSky,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }

                        // Bullet nodes
                        points.forEachIndexed { i, pt ->
                            val isSelected = selectedIndex == i
                            drawCircle(
                                color = if (isSelected) Color.White else accentSky,
                                radius = if (isSelected) 8.5f else 5f,
                                center = pt
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = accentSky,
                                    radius = 14f,
                                    center = pt,
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }

                    // Horizontal bottom Axis Labels (Short formatted dates: MM/dd)
                    chartData.forEachIndexed { idx, item ->
                        val ptX = textLeftPadding + if (totalPoints > 1) {
                            idx * (workableWidth / (totalPoints - 1))
                        } else {
                            workableWidth / 2f
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            item.label,
                            ptX,
                            canvasHeight - 6f,
                            dateLabelPaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Informational micro notice block below the consistency chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "اضغطي على أي يوم في المنحنى لعرض تفاصيل إكمال المهام ومعدل الترطيب المائي والتحرك نحو الأهداف العلاجية الموصى بها.",
                    color = textMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
