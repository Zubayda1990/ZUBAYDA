package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonalizedSkincareTip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailySkincareTipComponent(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    activeReport: SkinReport?,
    viewModel: SkincareTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val tipState by viewModel.skincareTipState.collectAsState()
    
    val concerns = listOf(
        "حب الشباب والبثور" to "🧪",
        "الجفاف والقشور" to "🏜️",
        "التصبغات والبقع" to "✨",
        "حساسية واحمرار" to "🛡️",
        "المسام والدهون" to "🔍",
        "التجاعيد والخطوط" to "⏳"
    )

    var selectedConcern by remember { mutableStateOf("حب الشباب والبثور") }

    // Prepopulate based on active report if available
    LaunchedEffect(activeReport) {
        if (activeReport != null) {
            val reportText = "${activeReport.skinType} ${activeReport.pathology} ${activeReport.avoid}"
            selectedConcern = when {
                reportText.contains("حب الشباب") || reportText.contains("بثور") -> "حب الشباب والبثور"
                reportText.contains("جاف") || reportText.contains("جفاف") || reportText.contains("قشور") -> "الجفاف والقشور"
                reportText.contains("تصبغ") || reportText.contains("كلف") || reportText.contains("بقع") -> "التصبغات والبقع"
                reportText.contains("حساس") || reportText.contains("احمرار") || reportText.contains("تهيج") -> "حساسية واحمرار"
                reportText.contains("مسام") || reportText.contains("دهون") || reportText.contains("زهم") -> "المسام والدهون"
                else -> "التجاعيد والخطوط"
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("skincare_tip_card")
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Title Header with Gemini Glowing Effect
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "مستشاركِ الذكي لجمال خلايا البشرة ✦",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accentColor, Color(0xFFA855F7), Color(0xFFEC4899))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "استشارة ذكية",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "حددي المشكلة الجلدية التي تودين الحصول على نصيحة تركيبية وسلوكية مخصصة لها مباشرة عبر خلايا Gemini العصبية المتكاملة:",
                color = textMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Concerns Chips Flow
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                concerns.forEach { (concern, emoji) ->
                    val isSelected = selectedConcern == concern
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) accentColor.copy(alpha = 0.2f)
                                else Color.Black.copy(alpha = 0.25f)
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isSelected) accentColor else textMuted.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedConcern = concern }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("concern_chip_${concern.replace(" ", "_")}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = concern,
                                color = if (isSelected) Color.White else textMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = emoji,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Generate Button
            Button(
                onClick = { viewModel.generateSkincareTip(selectedConcern) },
                enabled = tipState !is SkincareTipUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = accentColor.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("gemini_generate_tip_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "توليد نصيحة مخصصة بذكاء Gemini ✦",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "برق",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Output Content based on state
            AnimatedContent(
                targetState = tipState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tip_state_transition"
            ) { state ->
                when (state) {
                    is SkincareTipUiState.Initial -> {
                        // Prompt to click generate
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "اضغطي على الزر بالأعلى لدعوة GlowLogic AI في الحصول على تركيبتكِ الدقيقة السحرية ✨",
                                color = textMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is SkincareTipUiState.Loading -> {
                        // Beautiful glowing shimmery loading block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "أخصائية الجلدية زبيدة رمزي تعاير خوارزميات الذكاء السريري لـ Gemini... 🧠🧬",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    is SkincareTipUiState.Success -> {
                        val tip = state.tip
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .border(
                                    width = 1.dp,
                                    color = accentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Section 1: Molecular Cause
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = tip.molecular_cause,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tip_result_molecular_text")
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = "علمي",
                                            tint = accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "التشخيص الجزيئي",
                                        color = accentColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = textMuted.copy(alpha = 0.1f))

                            // Section 2: Chemical Advice
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = tip.chemical_advice,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tip_result_chemical_text")
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterVintage,
                                            contentDescription = "نشط",
                                            tint = Color(0xFFA855F7),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "العمود الكيميائي",
                                        color = Color(0xFFA855F7),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = textMuted.copy(alpha = 0.1f))

                            // Section 3: Daily Habit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = tip.daily_habit,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tip_result_habit_text")
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = "سلوك",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "العادة السلوكية",
                                        color = Color(0xFF10B981),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = textMuted.copy(alpha = 0.1f))

                            // Section 4: Critical Note
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = tip.critical_note,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tip_result_critical_text")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "تنبيه طبي",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    is SkincareTipUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (state as SkincareTipUiState.Error).message,
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
