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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.BuildConfig
import com.example.data.api.*

@Composable
fun BespokeRecipeCardView(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    activeReport: SkinReport?,
    viewModel: SkincareTrackerViewModel,
    onBack: () -> Unit
) {
    var generatedReport by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
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
                    text = "التركيبة العلاجية المخصصة 🧬",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (generatedReport != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = generatedReport ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (isGenerating) {
            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(50.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "جاري قراءة البيانات وتحليل المناخ لتكوين التقرير المعملي عبر GlowLogic AI...",
                color = textMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        } else {
            Button(
                onClick = {
                    isGenerating = true
                    coroutineScope.launch {
                        try {
                            val contextData = if (activeReport != null) {
                                "SkinType: ${activeReport.skinType}, Hydration: ${activeReport.hydration}%, Barrier: ${activeReport.barrierHealth}%"
                            } else {
                                "Unknown skin type profile."
                            }
                            
                            val prompt = "أنت طبيب الأمراض الجلدية والذكاء الاصطناعي السريري. اقرأ البيانات المجمعة التالية: $contextData ودرجة الـ pH للمريض والمناخ المحيط. أنشئ تقرير طبي وصفة طبية (Bespoke Recipe) مخصصة جداً باللغة العربية لعلاج مشاكل بشرته بناءً على أحدث البروتوكولات المعملية. لا تكتب مقدمات، اذكر الوصفة مباشرة وضع النصائح."

                            val apiKey = BuildConfig.GEMINI_API_KEY
                            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder")) {
                                generatedReport = "يجب إضافة مفتاح Gemini API في إعدادات المنصة لتوليد الوصفة السريرية المخصصة."
                                isGenerating = false
                                return@launch
                            }

                            val request = GenerateContentRequest(
                                contents = listOf(Content(parts = listOf(Part(text = prompt))))
                            )
                            val response = RetrofitClient.service.generateContent(
                                apiKey = apiKey,
                                request = request
                            )

                            generatedReport = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "عذراً، فشل توليد التقرير."
                        } catch (e: Exception) {
                            generatedReport = "خطأ في الاتصال بالذكاء الاصطناعي: ${e.message}"
                        }
                        isGenerating = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007BFF)), // Blue "Bespoke Recipe" button as requested
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = "إصدار التركيبة المخصصة (Bespoke Recipe)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "يقرأ هذا الزر جميع إجاباتك وبياناتك الحيوية ومستوى حموضة البشرة، لإنشاء مستحضر طبي مخصوص لك.",
                color = textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
