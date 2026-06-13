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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkincareTrackerScreen(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    activeReport: SkinReport?,
    onNavigateToScan: () -> Unit,
    viewModel: SkincareTrackerViewModel
) {
    val selectedDateStr by viewModel.selectedDateStr.collectAsState()
    val taskItems by viewModel.taskItemsForSelectedDate.collectAsState()
    val waterLog by viewModel.waterLogForSelectedDate.collectAsState()
    val streakData by viewModel.streakData.collectAsState()

    val allCompletions by viewModel.allCompletions.collectAsState()
    val allWaterLogs by viewModel.allWaterLogs.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val allSavedReports by viewModel.allSavedReports.collectAsState()

    // Form inputs for simple inline custom task adder
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskName by remember { mutableStateOf("") }
    var newTaskIsAm by remember { mutableStateOf(true) }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayNameFormat = remember { SimpleDateFormat("E", Locale("ar")) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale.US) }

    // Generate past 3 days + today + next 3 days
    val daysOfWeek = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..6) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("skincare_tracker_screen")
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title Header
        item {
            Text(
                text = "مفكرة وتتبع وتكامل البشرة اليومية 🗓️🔬",
                color = accentColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "بإشراف أخصائية الجلدية والذكاء الاصطناعي زبيدة رمزي",
                color = textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // LATE DAY REMINDER CARD
        item {
            val context = LocalContext.current
            val todayStr = remember { sdf.format(Date()) }
            
            // Check if user has dismissed the reminder for today
            var reminderDismissedDate by remember {
                mutableStateOf(
                    context.getSharedPreferences("skincare_tracker_prefs", android.content.Context.MODE_PRIVATE)
                        .getString("dismissed_tracker_reminder_date", "") ?: ""
                )
            }
            
            val isTodayActive = (selectedDateStr == todayStr)
            val hasActiveTasks = taskItems.isNotEmpty()
            val anyTaskCompletedTodayByNow = taskItems.any { it.isCompleted }
            
            // Conditions to show the reminder:
            // 1. We are looking at "today"
            // 2. We have tasks scheduled to be done
            // 3. None of them are checked yet
            // 4. We didn't dismiss it today
            val shouldShowReminder = isTodayActive && hasActiveTasks && !anyTaskCompletedTodayByNow && (reminderDismissedDate != todayStr)
            
            AnimatedVisibility(
                visible = shouldShowReminder,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .border(
                            width = 1.2.dp,
                            color = accentColor.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val sharedPrefs = context.getSharedPreferences("skincare_tracker_prefs", android.content.Context.MODE_PRIVATE)
                                    sharedPrefs.edit().putString("dismissed_tracker_reminder_date", todayStr).apply()
                                    reminderDismissedDate = todayStr
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق التذكير",
                                    tint = textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "تذكير العناية بالبشرة اليومي 🔔",
                                    color = accentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "جرس",
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "عزيزتي، لم تقومي بتسجيل أيٍّ من خطوات روتينك اليومي بعد! حافظي على استمرارية التزامكِ حتى لا تفقدي تقدم بشرتكِ وصحة الأدمة المتوازنة 🧬✨",
                            color = Color.White,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = {
                                    // Highlights attention downwards
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = accentColor)
                            ) {
                                Text(
                                    text = "ابدئي بتسجيل روتينكِ الآن 👈",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Date Selector Carousel
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(daysOfWeek) { date ->
                        val dateStr = sdf.format(date)
                        val isSelected = dateStr == selectedDateStr
                        val dayName = dayNameFormat.format(date)
                        val dayNum = dayNumFormat.format(date)

                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) accentColor else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.selectDate(date)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayName,
                                    color = if (isSelected) Color.Black else textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNum,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Streak Status Board
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "أطول سلسلة 🏆",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${streakData.longestStreak} يوم",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "سلسلة الالتزام 🔥",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${streakData.currentStreak} يوم",
                                color = accentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "سلسلة النشاط",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Water Hydration Card
        item {
            val cupsDrank = waterLog?.cupsDrank ?: 0
            val targetCups = waterLog?.targetCups ?: 8
            val progressPercent = (cupsDrank.toFloat() / targetCups.toFloat()).coerceIn(0f, 1f)

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معدل الترطيب المائي 💧",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ترطيب خلايا البشرة داخلياً لتعزيز التبادل الحيوي وصيانة مظهر المسام ونضارة الوجه.",
                        color = textMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress slider/bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Plus/Minus Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.decrementWaterIntake() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "كوب أقل", tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { viewModel.incrementWaterIntake() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "كوب إضافي", tint = accentColor, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Display Value
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$targetCups",
                                    color = textMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = " / $cupsDrank",
                                    color = accentColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                text = "كؤوس ماء (250 مل لكل كوب)",
                                color = textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accentColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // AM Route Checklist
        item {
            val amTasks = taskItems.filter { it.task.isAm }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "صباحي",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "روتين الصباح السريري ☀️",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (amTasks.isEmpty()) {
                        Text(
                            text = "لا توجد خطوات مضافة في روتينك الصباحي اليوم.",
                            color = textMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        amTasks.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .clickable {
                                        viewModel.toggleTaskCompletion(item.task.id, !item.isCompleted)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delete icon for custom tasks
                                if (item.task.isCustom) {
                                    IconButton(
                                        onClick = { viewModel.deleteTask(item.task) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف خطوة",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = item.task.name,
                                        color = if (item.isCompleted) textMuted else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleTaskCompletion(item.task.id, isChecked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentColor,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // PM Route Checklist
        item {
            val pmTasks = taskItems.filter { !it.task.isAm }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = "مسائي",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "روتين المساء السريري 🌙",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pmTasks.isEmpty()) {
                        Text(
                            text = "لا توجد خطوات مضافة في روتينك المسائي اليوم.",
                            color = textMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        pmTasks.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .clickable {
                                        viewModel.toggleTaskCompletion(item.task.id, !item.isCompleted)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delete icon for custom tasks
                                if (item.task.isCustom) {
                                    IconButton(
                                        onClick = { viewModel.deleteTask(item.task) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف خطوة",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(24.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = item.task.name,
                                        color = if (item.isCompleted) textMuted else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )

                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleTaskCompletion(item.task.id, isChecked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentColor,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Clinical Recommendation Sync or Dynamic Action Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button to trigger Add custom task slider
                Button(
                    onClick = { showAddTaskDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "إضافة خطوة مخصصة للروتين ➕",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // If task list is totally empty or user needs sync from AI
                Button(
                    onClick = {
                        if (activeReport != null) {
                            viewModel.syncFromAILog(activeReport.routineAM, activeReport.routinePM)
                        } else {
                            // Sync standard template routine
                            viewModel.syncFromAILog(
                                "1. غسول ساليسيليك موازن\n2. سيروم نياسيناميد 10%\n3. مائي هلامي بانثينول\n4. واقي شمس خفيف +SPF 50",
                                "1. غسول رغوي عميق\n2. سيروم خلاصة سيكا\n3. مهدئ حاجز دهني"
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "استيراد الروتين السريري المقترح من التقرير 🩺🧪",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Include Premium Consistency Analytics Dashboard Chart
        item {
            Text(
                text = "تحليل لوحة الالتزام والنتائج 📊",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp)
            )

            SkincareConsistencyChart(
                allCompletions = allCompletions,
                allWaterLogs = allWaterLogs,
                allTasks = allTasks,
                cardColor = cardColor,
                accentColor = accentColor,
                textMuted = textMuted
            )
        }

        // Embed the Skin Health evolution trend chart
        item {
            SkinHealthTrendChart(
                savedReports = allSavedReports,
                cardColor = cardColor,
                accentColor = accentColor,
                textMuted = textMuted
            )
        }

        // --- Daily Notification Reminders Panel ---
        item {
            val context = LocalContext.current
            
            // Shared Preferences for reminders
            val reminderPrefs = remember {
                context.getSharedPreferences("skincare_reminder_prefs", android.content.Context.MODE_PRIVATE)
            }
            
            // State for Morning Reminder
            var amEnabled by remember { mutableStateOf(reminderPrefs.getBoolean("am_enabled", false)) }
            var amHour by remember { mutableStateOf(reminderPrefs.getInt("am_hour", 8)) }
            var amMinute by remember { mutableStateOf(reminderPrefs.getInt("am_minute", 0)) }
            
            // State for Evening Reminder
            var pmEnabled by remember { mutableStateOf(reminderPrefs.getBoolean("pm_enabled", false)) }
            var pmHour by remember { mutableStateOf(reminderPrefs.getInt("pm_hour", 20)) }
            var pmMinute by remember { mutableStateOf(reminderPrefs.getInt("pm_minute", 0)) }

            // State for feedback log messages
            var feedbackMessage by remember { mutableStateOf("") }

            // Permission Launcher for Android 13+ (POST_NOTIFICATIONS)
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    feedbackMessage = "تم تفعيل صلاحية الإشعارات بنجاح! 🎉"
                } else {
                    feedbackMessage = "تنبيه: تم رفض صلاحية الإشعارات. يرجى تفعيلها من إعدادات النظام لتلقي التنبيهات ⚠️"
                }
            }

            // Function to schedule morning reminder
            fun saveMorningReminder(enabled: Boolean, hour: Int, minute: Int) {
                reminderPrefs.edit()
                    .putBoolean("am_enabled", enabled)
                    .putInt("am_hour", hour)
                    .putInt("am_minute", minute)
                    .apply()
                
                if (enabled) {
                    SkincareReminderReceiver.scheduleAlarm(context, true, hour, minute)
                    feedbackMessage = "تم جدولة التذكير الصباحي عند الساعة ${String.format("%02d:%02d", hour, minute)} ⏰☀️"
                } else {
                    SkincareReminderReceiver.cancelAlarm(context, true)
                    feedbackMessage = "تم إلغاء تفعيل التذكير الصباحي ⚠️"
                }
            }

            // Function to schedule evening reminder
            fun saveEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
                reminderPrefs.edit()
                    .putBoolean("pm_enabled", enabled)
                    .putInt("pm_hour", hour)
                    .putInt("pm_minute", minute)
                    .apply()
                
                if (enabled) {
                    SkincareReminderReceiver.scheduleAlarm(context, false, hour, minute)
                    feedbackMessage = "تم جدولة التذكير المسائي عند الساعة ${String.format("%02d:%02d", hour, minute)} ⏰🌙"
                } else {
                    SkincareReminderReceiver.cancelAlarm(context, false)
                    feedbackMessage = "تم إلغاء تفعيل التذكير المسائي ⚠️"
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .testTag("skincare_notifications_configuration_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "التذكيرات",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "منبه تذكيرات العناية بالبشرة اليومية 🔔🕰️",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "حافظي على صحة وقوة حاجز الجلد من خلال تنبيهات يومية مجدولة لتطبيق روتينك الصباحي والمسائي بانتظام.",
                        color = textMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Morning Reminder Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = amEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    amEnabled = checked
                                    saveMorningReminder(checked, amHour, amMinute)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تنشيط التذكير الصباحي ☀️",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (amEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Minutes controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                amMinute = (amMinute + 15) % 60
                                                saveMorningReminder(amEnabled, amHour, amMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+", color = accentColor, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = String.format("%02d", amMinute),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        TextButton(
                                            onClick = {
                                                amMinute = (amMinute + 45) % 60
                                                saveMorningReminder(amEnabled, amHour, amMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-", color = accentColor, fontSize = 14.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(":", color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Hours controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                amHour = (amHour + 1) % 24
                                                saveMorningReminder(amEnabled, amHour, amMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+", color = accentColor, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = String.format("%02d", amHour),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        TextButton(
                                            onClick = {
                                                amHour = (amHour + 23) % 24
                                                saveMorningReminder(amEnabled, amHour, amMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-", color = accentColor, fontSize = 14.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = "تحديد وقت التذكير:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Evening Reminder Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = pmEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    pmEnabled = checked
                                    saveEveningReminder(checked, pmHour, pmMinute)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = accentColor,
                                    uncheckedThumbColor = textMuted,
                                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تنشيط التذكير المسائي 🌙",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (pmEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Minutes controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                pmMinute = (pmMinute + 15) % 60
                                                saveEveningReminder(pmEnabled, pmHour, pmMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+", color = accentColor, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = String.format("%02d", pmMinute),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        TextButton(
                                            onClick = {
                                                pmMinute = (pmMinute + 45) % 60
                                                saveEveningReminder(pmEnabled, pmHour, pmMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-", color = accentColor, fontSize = 14.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(":", color = Color.White, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Hours controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                pmHour = (pmHour + 1) % 24
                                                saveEveningReminder(pmEnabled, pmHour, pmMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+", color = accentColor, fontSize = 14.sp)
                                        }
                                        Text(
                                            text = String.format("%02d", pmHour),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        TextButton(
                                            onClick = {
                                                pmHour = (pmHour + 23) % 24
                                                saveEveningReminder(pmEnabled, pmHour, pmMinute)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-", color = accentColor, fontSize = 14.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = "تحديد وقت التذكير:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (feedbackMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = feedbackMessage,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                SkincareReminderReceiver.triggerImmediateTestNotification(context, false)
                                feedbackMessage = "تم إرسال إشعار تجريبي فوري للروتين المسائي 🌙📲"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("تجربة تنبيه المساء 🧪🌙", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                SkincareReminderReceiver.triggerImmediateTestNotification(context, true)
                                feedbackMessage = "تم إرسال إشعار تجريبي فوري للروتين الصباحي ☀️📲"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("تجربة تنبيه الصباح 🧪☀️", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Include the Daily Skincare Advice Box from Gemini API
        item {
            Spacer(modifier = Modifier.height(12.dp))
            DailySkincareTipComponent(
                cardColor = cardColor,
                accentColor = accentColor,
                textMuted = textMuted,
                activeReport = activeReport,
                viewModel = viewModel
            )
        }
    }

    // Modal dialog to add a new task cleanly, complying to "No Composable in onClick" rule
    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = {
                Text(
                    text = "إضافة خطوة مخصصة للروتين 🔬",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "أدخلي مسمى خطوة العناية الكيميائية أو السلوكية وتحديد فترة الروتين:",
                        color = textMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTaskName,
                        onValueChange = { newTaskName = it },
                        placeholder = { Text("مثال: سيروم ريتينول 0.2%، واقي شمس", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = textMuted.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { newTaskIsAm = false }
                        ) {
                            Text("روتين مساء 🌙", fontSize = 12.sp, color = if (!newTaskIsAm) Color.White else textMuted)
                            RadioButton(
                                selected = !newTaskIsAm,
                                onClick = { newTaskIsAm = false },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { newTaskIsAm = true }
                        ) {
                            Text("روتين صباح ☀️", fontSize = 12.sp, color = if (newTaskIsAm) Color.White else textMuted)
                            RadioButton(
                                selected = newTaskIsAm,
                                onClick = { newTaskIsAm = true },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskName.isNotBlank()) {
                            viewModel.addNewTask(newTaskName, newTaskIsAm)
                            newTaskName = ""
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("حفظ الخطوة", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = cardColor
        )
    }
}
