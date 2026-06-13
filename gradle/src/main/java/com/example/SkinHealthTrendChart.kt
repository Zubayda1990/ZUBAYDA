package com.example

import android.graphics.PointF
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SkinHealthTrendChart(
    savedReports: List<SavedSkinReport>,
    cardColor: Color,
    accentColor: Color,
    textMuted: Color
) {
    // Interactive toggles for lines (reaches standard of Recharts legend filter)
    var showHydration by remember { mutableStateOf(true) }
    var showBarrierHealth by remember { mutableStateOf(true) }

    // Selected point index for Recharts-style rich hovering details
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val accentSky = Color(0xFF38BDF8)     // Hydration Line Color
    val accentEmerald = Color(0xFF10B981) // Barrier Health Line Color

    // Sort reports chronologically
    val sortedReports = remember(savedReports) {
        savedReports.sortedBy { it.timestamp }
    }

    // Fallback: If user has empty or very few reports, prepend elegant demo baseline data
    // so they see a premium chart right away!
    val chartData = remember(sortedReports) {
        if (sortedReports.size < 2) {
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            val d1 = cal.timeInMillis - (4 * 24 * 3600 * 1000L)
            val d2 = cal.timeInMillis - (2 * 24 * 3600 * 1000L)
            
            val demoList = listOf(
                SavedSkinReport(
                    id = -101,
                    timestamp = d1,
                    dateStr = format.format(Date(d1)),
                    skinType = "جافة (توضيحي)",
                    hydration = 38,
                    barrierHealth = 42,
                    pathology = "جفاف سطحي وضعف خفيف في الغدد الدهنية للوجه.",
                    routineAM = "",
                    routinePM = "",
                    avoid = "",
                    isDemo = true
                ),
                SavedSkinReport(
                    id = -102,
                    timestamp = d2,
                    dateStr = format.format(Date(d2)),
                    skinType = "مختلطة (توضيحي)",
                    hydration = 52,
                    barrierHealth = 48,
                    pathology = "تحسن طفيف في حيوية الأدمة نتيجة للترطيب المنتظم.",
                    routineAM = "",
                    routinePM = "",
                    avoid = "",
                    isDemo = true
                )
            )
            // Combine with whatever real data there is
            demoList + sortedReports
        } else {
            sortedReports
        }
    }

    val totalPoints = chartData.size
    
    // Auto-select latest point on first load
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
            .padding(bottom = 24.dp)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Title block resembling Recharts standard corporate panel
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
                    text = "منحنى تطور حيوية وصحة الجلد 📈",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "تحليل ديناميكي متطور فيزيولوجياً لترطيب الأدمة وحالة الحاجز المناعي من الفحوصات والمسوحات التاريخية.",
                color = textMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // RECHARTS INTERACTIVE LEGEND (Clickable to Toggle Plots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hydration Legend Item
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showHydration) accentSky.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { showHydration = !showHydration }
                        .border(
                            1.dp,
                            if (showHydration) accentSky.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
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
                        text = "ترطيب الأدمة 💧",
                        color = if (showHydration) Color.White else textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Barrier Health Legend Item
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showBarrierHealth) accentEmerald.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { showBarrierHealth = !showBarrierHealth }
                        .border(
                            1.dp,
                            if (showBarrierHealth) accentEmerald.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
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
                        text = "سلامة حاجز البشرة 🛡️",
                        color = if (showBarrierHealth) Color.White else textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // DYNAMIC FLOATING RECHARTS TOOLTIP CARD
            AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (activePoint != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                            // Header: Date & Skin Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activePoint.isDemo) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "نموذج توضيحي",
                                            color = accentColor,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Text(
                                    text = "تاريخ التحليل: " + activePoint.dateStr + " (${activePoint.skinType})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Values row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showBarrierHealth) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${activePoint.barrierHealth}%",
                                            color = accentEmerald,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الحاجز المناعي:",
                                            color = textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                if (showHydration) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${activePoint.hydration}%",
                                            color = accentSky,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ترطيب الأدمة:",
                                            color = textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            if (activePoint.pathology.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "التشخيص: " + activePoint.pathology,
                                    color = Color.White.copy(alpha = 0.8f),
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

            // CHART AREA (Canvas) WITH DETECT TAP GESTURE
            val density = LocalDensity.current
            val basePadding = with(density) { 32.dp.toPx() }
            val textLeftPadding = with(density) { 42.dp.toPx() } // Grid label spacing
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
                                    
                                    // Search for nearest x index
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
                                    
                                    // Update tap selection
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

                    // Helper coordinate generators with explicit Offset return type
                    val getCoordinates = { value: Int, index: Int ->
                        val x = textLeftPadding + if (totalPoints > 1) {
                            index * (workableWidth / (totalPoints - 1))
                        } else {
                            workableWidth / 2f
                        }
                        
                        val percentage = value.toFloat() / 100f
                        val y = basePadding + (workableHeight * (1f - percentage))
                        
                        Offset(x, y)
                    }

                    // DRAW RECHARTS REFERENCE GRIDLINES (Dashed 25%, 50%, 75%, 100%)
                    val yLevels = listOf(0f, 0.25f, 0.50f, 0.75f, 1f)
                    val paintLabel = Paint().asFrameworkPaint().apply {
                        color = android.graphics.Color.parseColor("#475569") // Slate-500
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.RIGHT
                        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                    }

                    val dateLabelPaint = Paint().asFrameworkPaint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8") // Slate-400
                        textSize = 21f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    yLevels.forEach { level ->
                        val y = basePadding + (workableHeight * (1f - level))
                        // Dashed lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(textLeftPadding, y),
                            end = Offset(canvasWidth - basePadding, y),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        // Label text (25%, 50%, 75%, 100%)
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(level * 100).toInt()}%",
                            textLeftPadding - 12f,
                            y + 8f,
                            paintLabel
                        )
                    }

                    // Vertical guideline for selected node
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

                    // 1. DRAW HYDRATION LINE AND GRADIENT AREA
                    if (showHydration && totalPoints > 0) {
                        val points: List<Offset> = chartData.mapIndexed { idx, item -> getCoordinates(item.hydration, idx) }
                        
                        // Path for Gradient Area Fill underneath
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

                        // Path for outer Stroke Curve
                        val strokePath = Path()
                        if (points.isNotEmpty()) {
                            strokePath.moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                // Direct linear representation resembling high-fidelity grid mapping
                                strokePath.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path = strokePath,
                                color = accentSky,
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }

                        // Draw bullet nodes
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

                    // 2. DRAW BARRIER HEALTH LINE AND GRADIENT AREA
                    if (showBarrierHealth && totalPoints > 0) {
                        val points: List<Offset> = chartData.mapIndexed { idx, item -> getCoordinates(item.barrierHealth, idx) }
                        
                        // Area gradient
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

                    // Axis Bottom Labels for point indexes
                    chartData.forEachIndexed { idx, item ->
                        val ptX = textLeftPadding + if (totalPoints > 1) {
                            idx * (workableWidth / (totalPoints - 1))
                        } else {
                            workableWidth / 2f
                        }
                        
                        // Short date label (like MM/dd)
                        val shortDate = try {
                            val incomingFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val outgoingFormat = SimpleDateFormat("M/d", Locale.US)
                            incomingFormat.parse(item.dateStr)?.let { outgoingFormat.format(it) } ?: item.dateStr
                        } catch (e: Exception) {
                            "يوم " + (idx + 1)
                        }

                        shortDate?.let {
                            drawContext.canvas.nativeCanvas.drawText(
                                it,
                                ptX,
                                canvasHeight - 8f,
                                dateLabelPaint
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Informational Tip block below chart
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
                    text = "اضغطي على أي نقطة في المنحنى لعرض التفاصيل الكاملة للفحص السريري ونوع البشرة في ذلك التاريخ.",
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
