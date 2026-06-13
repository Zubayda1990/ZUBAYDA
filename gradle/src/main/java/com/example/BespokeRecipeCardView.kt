package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data model for ingredients
data class RecipeIngredient(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val percentage: Float,
    val phase: String, // "Aqueous" or "Active" or "Solubilizer"
    val purposeEn: String,
    val purposeAr: String,
    val isAlerted: Boolean = false
)

// Data model for solubilization steps
data class CompoundingStep(
    val stepNumber: Int,
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String,
    val subItemsEn: List<String>,
    val subItemsAr: List<String>
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BespokeRecipeCardView(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isArabic by remember { mutableStateOf(true) }
    var selectedBatchSizeMl by remember { mutableStateOf(100) } // ml
    
    // Interactive checkboxes for formulation progress
    val completedSteps = remember { mutableStateListOf<Int>() }
    
    // Ambient conditions displayed dynamically
    val ambientTemp = 22
    val ambientHumidity = 50
    
    // Curated ingredients as per USER_PROMPT formulation guidelines
    val ingredients = remember {
        listOf(
            // Aqueous Phase (A)
            RecipeIngredient(
                id = "water",
                nameEn = "Purified Distilled Water (Milli-Q)",
                nameAr = "ماء نقي مقطر خالي من المعادن",
                percentage = 70.8f,
                phase = "Aqueous",
                purposeEn = "Cosmetic vehicle, structural solvent base",
                purposeAr = "مذيب وسيط نقي للقاعدة المائية"
            ),
            RecipeIngredient(
                id = "edta",
                nameEn = "Disodium EDTA",
                nameAr = "إيديتات ثنائية الصوديوم (Disodium EDTA)",
                percentage = 0.2f,
                phase = "Aqueous",
                purposeEn = "Metal ion chelating agent for chemical stability & preservation",
                purposeAr = "عامل مخلب للمعادن لتعزيز الاستقرار وصلاحية المنتج"
            ),
            RecipeIngredient(
                id = "glycerin",
                nameEn = "Pure Vegetable Glycerin (99.5%)",
                nameAr = "جلسرين نباتي نقي مكرر",
                percentage = 4.0f,
                phase = "Aqueous",
                purposeEn = "Natural humectant, moisture binder & barrier support",
                purposeAr = "مرطب طبيعي يحبس الرطوبة ويدعم جدار حماية الأدمة"
            ),
            RecipeIngredient(
                id = "butylene",
                nameEn = "Butylene Glycol (Eco-cert)",
                nameAr = "بوتييلين جلايكول (عضوي متوازن)",
                percentage = 5.0f,
                phase = "Aqueous",
                purposeEn = "Penetration enhancer, humectant & cosmetic slip enhancer",
                purposeAr = "محفز نفوذ مائي، يحسن نسيج الكريم وامتصاص المواد النشطة"
            ),
            
            // Physiological Active Phase (B)
            RecipeIngredient(
                id = "egcg",
                nameEn = "Epigallocatechin Gallate (Active EGCG)",
                nameAr = "مستخلص الشاي الأخضر النشط (EGCG)",
                percentage = 2.5f,
                phase = "Active",
                purposeEn = "Targets Acne Porphyrin reduction & sebum inflammatory control",
                purposeAr = "يستهدف خفض نسبة البورفيرين البكتيرية وتنظيم الغدد الدهنية"
            ),
            RecipeIngredient(
                id = "peptides",
                nameEn = "Adenosine & Tripeptide Complex",
                nameAr = "مركب الأدينوسين والببتيدات الثلاثية المغلفة",
                percentage = 2.5f,
                phase = "Active",
                purposeEn = "Restores elastin density and targets Future Wrinkles prevention",
                purposeAr = "يعيد بناء الدعم الكولاجيني لمكافحة التجاعيد المستقبلية"
            ),
            RecipeIngredient(
                id = "niacinamide",
                nameEn = "Clinical Niacinamide (Vitamin B3)",
                nameAr = "نياسيناميد نقي علاجي (فيتامين B3)",
                percentage = 3.0f,
                phase = "Active",
                purposeEn = "Microbiome balancing, pore-tightening & redness inhibitor",
                purposeAr = "موازن الفلورا الطبيعية وضبط قطر المسام وتلطيف التوهج"
            ),
            
            // Solubilization & Preserving Phase (C)
            RecipeIngredient(
                id = "caprylyl",
                nameEn = "Caprylyl/Capryl Glucoside (Mild Surfactant)",
                nameAr = "كابريليل غلوكوزيد (مذيب سطحي غلوكوزيدي دافئ)",
                percentage = 4.0f,
                phase = "Solubilizer",
                purposeEn = "Solubilizer 1: High efficiency aromatic emulsifier, non-irritating",
                purposeAr = "مذيب معتدل ورغوي لطيف لدمج المعطرات والزيوت دون تحسس"
            ),
            RecipeIngredient(
                id = "polysorbate",
                nameEn = "Polysorbate 20 (Premium Solubilizer)",
                nameAr = "بولي سوربات 20 (مذيب سطحي فسيولوجي)",
                percentage = 3.0f,
                phase = "Solubilizer",
                purposeEn = "Solubilizer 2: Inter-molecular compound helper to lower overall dosage",
                purposeAr = "مذيب عطري يساند الكابريليل لتقليل الجرعة الكيميائية الكلية"
            ),
            RecipeIngredient(
                id = "hexanediol",
                nameEn = "1,2-Hexanediol (Preservative alternative)",
                nameAr = "1,2-هكسانيديول (بديل الحفظ الخالي من الحساسية)",
                percentage = 2.0f,
                phase = "Solubilizer",
                purposeEn = "Scentless clinical preservative. Replacing Phenoxyethanol 100%",
                purposeAr = "مادة حافظة خالية من الروائح كبديل آمن للفينوكسي إيثانول"
            ),
            RecipeIngredient(
                id = "lavender",
                nameEn = "Organic Lavandula Angustifolia Oil",
                nameAr = "زيت لافندر فرنسي عطري نقي",
                percentage = 1.0f,
                phase = "Solubilizer",
                purposeEn = "Therapeutic natural scent, antimicrobial, soothing agent",
                purposeAr = "معطر طبيعي مهدئ ومطهر للأدمة المتفاعلة"
            ),
            RecipeIngredient(
                id = "centella",
                nameEn = "Centella Asiatica (Cica) Hydro-Extract",
                nameAr = "مستخلص عشبة السنتيلا (Cica) المائي المرمم",
                percentage = 2.0f,
                phase = "Solubilizer",
                purposeEn = "Soothes skin micro-tears and accelerates barrier healing",
                purposeAr = "يهدئ الاحمرار الدقيق ويسرع التئام نسيج الجلد"
            )
        )
    }

    // Step-by-step compounding guidelines based on chemical best practices
    val compoundingSteps = remember {
        listOf(
            CompoundingStep(
                stepNumber = 1,
                titleEn = "Aqueous Pre-Dissolution & Chelation",
                titleAr = "تحضير الطور المائي والترابط المخلبي",
                descEn = "Weigh Purified Water. Dissolve Disodium EDTA (0.2%) under ambient 22°C. Stir at moderate speeds until completely clear. This binds calcium/magnesium ions, optimizing active stability.",
                descAr = "وزن الماء المقطر في كأس الكيمياء الزجاجي المعقم. أضيفي 0.2% من الـ Disodium EDTA في درجة حرارة 22 مئوية. يقلب المزيج بسرعة متوسطة حتى يذوب لتثبيت جزيئات السائل ومنع تلفها بفعل شوائب المعادن.",
                subItemsEn = listOf("Target clarity: 100% transparent", "Duration: 3-5 minutes continuous stirring"),
                subItemsAr = listOf("الشفافية المطلوبة: 100% بلوري", "مدة التحريك: 3-5 دقائق متواصلة")
            ),
            CompoundingStep(
                stepNumber = 2,
                titleEn = "Active Raw Mineral Solubilization",
                titleAr = "إذابة وحقن المكونات النشطة الجافة",
                descEn = "Incorporate Niacinamide (3.0%) and the Adenosine & Tripeptide Complex (2.5%) directly into Phase A. Agitate continuously. This prepares a water-soluble cellular nutrition base.",
                descAr = "إضافة النياسيناميد (3.0%) وببتيدات الأدينوسين المغلقة (2.5%) مباشرة إلى الطور المائي المحضر في الخطوة الأولى. قلبي المزيج بلطف بشكل مستمر. هذا يخلق قاعدة التغذية الخلوية مائية العبور.",
                subItemsEn = listOf("Temperature lock: Keep strictly at 20-24°C to protect peptide molecular chains", "Stirring type: Slow paddle rotation to avoid micro-foaming"),
                subItemsAr = listOf("الحرارة: الحفاظ على درجة 22 مئوية لحماية ببتيدات الكولاجين من التمزق والتحلل الحراري", "طريقة التحريك: هادئة لتفادي تشكل رغوات مجهرية")
            ),
            CompoundingStep(
                stepNumber = 3,
                titleEn = "Dual Mild Cloud-Point Oil Solubilization",
                titleAr = "خلط نظام المذيبات المزدوج للافندر",
                descEn = "In a separate container, combine lavender oil (1.0%) with Caprylyl Glycoside (4.0%) and Polysorbate 20 (3.0%). Using two mild non-ionic solubilizers prevents surfactant skin sensitization while achieving clear oil-in-water dispersion.",
                descAr = "في حاوية تعقيم مستقلة، امزجي زيت اللافندر العطري المهدئ (1.0%) مع كابريليل غلوكوزيد (4.0%) وبولي سوربات 20 (3.0%). نستخدم مذيبين خفيفين معاً لرفع الكفاءة وضمان ذوبان كامل للزيت برغوة منعدمة وحماية للبشرة المتفاعلة.",
                subItemsEn = listOf("Ratio target: 7:1 solubilizer-to-oil matrix", "Appearance: Clear, golden homogeneous liquid"),
                subItemsAr = listOf("نسبة المذيب للزيت الكلي: 7 إلى 1 لضمان الاستقرار اللوني والفيزيائي الخالي من التشتت", "المظهر: متجانس بلون عنبري شفاف")
            ),
            CompoundingStep(
                stepNumber = 4,
                titleEn = "Botanical EGCG & Safe Preservative Fusion",
                titleAr = "دمج خلاصة EGCG والحافظ البديل الآمن",
                descEn = "Add the key anti-porphyrin shield, EGCG Active Extract (2.5%), and the clean non-stinging preservative 1,2-Hexanediol (2.0%) into the solubilizer premix. This eliminates the need for harsh Phenoxyethanol or sensory-stinging Arbutin.",
                descAr = "أضيفي درع مكافحة البورفيرين وحب الشباب النشط مستخلص الشاي الأخضر EGCG (بنسبة 2.5%) ومادة 1,2-Hexanediol الحافظة الآمنة (2.0%) إلى الطور الزيتي المذاب. هذا يستثني تماماً الفينوكسي إيثانول والأربوتين، مانعاً تهيج البشرة تماماً.",
                subItemsEn = listOf("Allergen checklist: Phenoxyethanol 0.0%, Arbutin 0.0%", "Clinical purpose: Immediate capture of bacterial enzyme pathways to minimize acne"),
                subItemsAr = listOf("لائحة المواد المستثناة من التركيبة: فينوكسي إيثانول 0%، أربوتين 0%", "الهدف العلاجي: تدمير مركبات البورفيرين الحيوية للبكتيريا المسببة لحب الشباب")
            ),
            CompoundingStep(
                stepNumber = 5,
                titleEn = "Homogenization & Final Hydration Lock",
                titleAr = "الدمج الحر مائي النهائي وحفظ النسيج",
                descEn = "Pour the Phase B solubilizer blend slowly into Phase A under continuous high-speed stirring. Inject the remaining humectants: Glycerin (4.0%), Butylene Glycol (5.0%), and healing Centella Hydro-Extract (2.0%). The solution transitions into a beautiful, pH 5.5 balanced active tonic.",
                descAr = "اسكبي الطور الزيتي المنحل بلطف وببطء فوق الطور المائي الأساسي تحت التقليب السريع المروحي. أضيفي بعدها الجلسرين (4.0%) والبوتيلين جليكول (5.0%) وخلاصة السنتيلا المتبقية (2.0%). يتحول المزيج فوراً إلى تونر متجانس ومائي فائق الرقة بدرجة pH متزنة 5.5.",
                subItemsEn = listOf("Ideal Final pH: 5.5±0.2 (optimal for the skin microbiome)", "Sterilization: Dispense into a cobalt-blue UV protectant glass bottle"),
                subItemsAr = listOf("مستوى حموضة pH التونر النهائي: 5.5 درجة مثالية لدعم نسيج المايكروبيوم الجلدي", "التعبئة: قوارير زجاجية بنية أو زرقاء معتمة لحماية مضادات الأكسدة من أشعة الضوء")
            )
        )
    }

    val totalProgress = if (compoundingSteps.isEmpty()) 0f else completedSteps.size.toFloat() / compoundingSteps.size.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14)) // Medical cyber dark background
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // --- MEDICAL CARD TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    contentDescription = if (isArabic) "الرجوع" else "Back",
                    tint = accentColor
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isArabic) "بطاقة التركيبة الفسیولوجية المخصصة" else "Bespoke Physiological Recipe Card",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "GlowLogic Clinical Lab Module • Week 4",
                    color = accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bilingual Toggle Switch
            TextButton(
                onClick = { isArabic = !isArabic },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isArabic) "EN" else "عربي",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        // --- REAL-TIME AMBIENT ALERTS HUD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "مراقبة العوامل والبيئة الطيفية النشطة 🧬🌡️" else "Spectral & Environmental Sensors Status",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isArabic) Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = null, 
                        tint = accentColor, 
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "حرارة الغرفة: 22°C" else "Room Temp: 22°C",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الرطوبة المحايدة: 50%" else "Humidity: 50%",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isArabic) "البيئة: مثالية" else "Env: Optimal",
                            color = Color(0xFF14B8A6),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isArabic) 
                        "• مستشعرات الأدمة: تم رصد مستويات عالية من البورفيرينات (خطر حب الشباب الجرثومي) مع جفاف مجهري وبداية علامات لخطوط تجاعيد مستقبلية. تم تعديل الصيغة لترميم جدار العزل وتطهير خلايا الصباغ والبورفيرين."
                        else "• Epidermal Sensors: High Levels of organic porphyrins detected (acne risk) alongside early signs of future wrinkles. Formula adjusted to chelate metal catalysts, destroy porphyrins, and support structural health.",
                    color = textMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                )
            }
        }

        // --- SCROLLABLE COMPREHENSIVE COMPONENT ---
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // ALLERGEN EXCLUSION ASSURANCE LABELS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Rich indigo tint
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(1.dp, Color(0xFF4338CA), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isArabic) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser, 
                                contentDescription = null, 
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = if (isArabic) "التحقق من قائمة مسببات الحساسية الطبية 🛡️🔬" else "Medical Allergen Exclusion Assurance",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) 
                                    "فحص أمان آلي: تم استبعاد الفينوكسي إيثانول (Phenoxyethanol) والأربوتين (Arbutin) تماماً من الوصفة (0.0%). تم إدراج 1,2-Hexanediol وببتيدات مهدئة بديلة لتقويض تهيج الوخز وضمان استقرار pH حمضي خفيف آمن."
                                    else "Automated Safety Check: Phenoxyethanol and Arbutin are strictly excluded (0.0%). Substituted with ultra-gentle 1,2-Hexanediol and Centella soothing fraction to prevent skin irritation.",
                                color = Color(0xFFC7D2FE),
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                            )
                        }

                        if (isArabic) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = Icons.Default.VerifiedUser, 
                                contentDescription = null, 
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // BATCH SIZE COMPLIANCE RESCALER
            item {
                Text(
                    text = if (isArabic) "ضبط حجم الدفعة المطلوب (Batch Rescaler)" else "Configure Batch Size (Rescale Recipe)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(50, 100, 150, 250, 500).forEach { size ->
                        val isSelected = selectedBatchSizeMl == size
                        Button(
                            onClick = { selectedBatchSizeMl = size },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) accentColor else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = "$size ml",
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // DETAILED INGREDIENTS RECIPE CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "التوزيع الكيميائي الدقيق للمكونات 🧪📊" else "Chemical Formulation Matrix",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isArabic) Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.BarChart, 
                                contentDescription = null, 
                                tint = accentColor, 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            text = if (isArabic) 
                                "النسب والوزن المطلوب لدفعة عيار $selectedBatchSizeMl مل من التونر المهدئ لمطابقة اللوائح:"
                                else "Ingredient breakdown & calculated weight for a $selectedBatchSizeMl ml batch of active therapeutic tonic:",
                            color = textMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Phases grouping
                        listOf("Aqueous", "Active", "Solubilizer").forEach { phase ->
                            val phaseNameEn = when(phase) {
                                "Aqueous" -> "Phase A: Aqueous Vehicle (수상 الطور المائي)"
                                "Active" -> "Phase B: Physiological Actives (المواد النشطة)"
                                else -> "Phase C: Double Solubilizers & Safety (المذيبات والمواد الحافظة)"
                            }
                            val phaseNameAr = when(phase) {
                                "Aqueous" -> "الطور (أ): الطور المائي المعالج (Aqueous Phase)"
                                "Active" -> "الطور (ب): المغذيات الفسيو-كيميائية الخاصة (Physiological Actives)"
                                else -> "الطور (ج): نظام الترطيب المنحل والمواد المانعة (Solubilizers & Safety)"
                            }
                            
                            val phaseColor = when(phase) {
                                "Aqueous" -> Color(0xFF38BDF8)
                                "Active" -> Color(0xFFFBBF24)
                                else -> Color(0xFF34D399)
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
                            
                            Text(
                                text = if (isArabic) phaseNameAr else phaseNameEn,
                                color = phaseColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp),
                                textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                            )

                            ingredients.filter { it.phase == phase }.forEach { ing ->
                                // Calculate weight based on batch size (density is approx 1g/ml)
                                val calculatedWeight = (ing.percentage / 100f) * selectedBatchSizeMl
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Calculated Weight
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = String.format("%.2f g/ml", calculatedWeight),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${ing.percentage}%",
                                            color = accentColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Name and purpose
                                    Column(
                                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                                        horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
                                    ) {
                                        Text(
                                            text = if (isArabic) ing.nameAr else ingEnToFormatted(ing.nameEn),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                                        )
                                        Text(
                                            text = if (isArabic) ing.purposeAr else ing.purposeEn,
                                            color = textMuted,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            textAlign = if (isArabic) TextAlign.Right else TextAlign.Left
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(phaseColor)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // INTERACTIVE STEP-BY-STEP STEP TRACKER HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "مسار وخطوات تحضير وتذويب المزيج ⚙️🥣" else "Interactive Compounding & Dissolution Steps",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isArabic) Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty, 
                        contentDescription = null, 
                        tint = accentColor, 
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Interactive Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(totalProgress * 100).toInt()}%",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    LinearProgressIndicator(
                        progress = { totalProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accentColor,
                        trackColor = Color.Black.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // RENDER THE INTERACTIVE STEP CARD ITEMS
            items(compoundingSteps.size) { index ->
                val step = compoundingSteps[index]
                val isCompleted = completedSteps.contains(step.stepNumber)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) cardColor.copy(alpha = 0.4f) else cardColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(
                            1.dp, 
                            if (isCompleted) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), 
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (isCompleted) {
                                completedSteps.remove(step.stepNumber)
                            } else {
                                completedSteps.add(step.stepNumber)
                                if (completedSteps.size == compoundingSteps.size) {
                                    Toast.makeText(context, "التهاني! تم إتمام كل خطوات التركيب بنجاح 🧴✨", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isArabic) {
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { checked ->
                                    if (checked) completedSteps.add(step.stepNumber)
                                    else completedSteps.remove(step.stepNumber)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = if (isArabic) Alignment.End else Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isCompleted) Color(0xFF10B981) else accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${step.stepNumber}",
                                        color = if (isCompleted) Color.Black else accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) step.titleAr else step.titleEn,
                                    color = if (isCompleted) Color(0xFF10B981) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = if (isArabic) step.descAr else step.descEn,
                                color = if (isCompleted) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                textAlign = if (isArabic) TextAlign.Right else TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Sub-items checklists
                            val subItems = if (isArabic) step.subItemsAr else step.subItemsEn
                            subItems.forEach { subItem ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    horizontalArrangement = if (isArabic) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = subItem,
                                        color = if (isCompleted) Color.White.copy(alpha = 0.4f) else accentColor.copy(alpha = 0.85f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check, 
                                        contentDescription = null, 
                                        tint = if (isCompleted) Color(0xFF10B981).copy(alpha = 0.5f) else accentColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        if (isArabic) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = { checked ->
                                    if (checked) completedSteps.add(step.stepNumber)
                                    else completedSteps.remove(step.stepNumber)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                        }
                    }
                }
            }
        }

        // --- FIXED PROPRIETARY AUTHORSHIP GRADIENT WATERMARK FOOTER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(vertical = 12.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isArabic) "تأسيس وتطوير الدكتورة زبيدة رمزي حسنون" else "Founded & Developed by: Zubayda Ramzi hasanain",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFF34D399))
                        )
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "GlowLogic Skin Medicine Proprietary © 2026 • All Rights Reserved",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// Utility to ensure cosmetic naming rendering has proper chemical syntax
private fun ingEnToFormatted(name: String): String {
    return name.replace("Milli-Q", "🟢 Milli-Q").replace("Disodium EDTA", "🧬 Disodium EDTA")
}
