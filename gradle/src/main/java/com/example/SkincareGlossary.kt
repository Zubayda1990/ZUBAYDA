package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

data class GlossaryIngredient(
    val name: String,
    val scientificName: String,
    val arabicName: String,
    val category: String, // e.g. "مقاومة التجاعيد والخطوط", "ترطيب عميق وحبس ماء", "تفتيح وتنظيم ملانين", "ترميم حاجز البشرة والتهيج", "تقشير وتطهير مسام"
    val benefits: List<String>,
    val description: String,
    val suitability: String,
    val riskLevel: String, // "آمن ومغذي جداً", "معتدل (يتطلب ضبط الجرعة)", "نشط (يتطلب حذر واستخدام واقي)"
    val riskColor: Color,
    val iconEmoji: String,
    val clinicalTip: String,
    val matchingFilterTerm: String // Term that maps to the selectedIngredient filter
)

object SkincareGlossaryData {
    val ingredients = listOf(
        GlossaryIngredient(
            name = "Retinol",
            scientificName = "Retinol / Vitamin A derivatives",
            arabicName = "الريتينول (مشتق فيتامين أ)",
            category = "مقاومة التجاعيد والخطوط",
            benefits = listOf(
                "يسرع من تجديد الخلايا الميتة في الطبقة القرنية السطحية.",
                "يحفز الخلايا الليفية لإنتاج كولاجين جديد يعزز متانة الأدمة.",
                "يقلل تدريجياً البقع الموضعية وتصبغات الكلف السطحي."
            ),
            description = "مُرمم ومعالج جزيئي ذهبي مكافح لعلامات شيخوخة البشرة والخطوط الدقيقة، يعمل عن طريق اختراق طبقات الجلد السفلية لتسريع عجلة استبدال الخلايا الهرمة بجديدة بالغة الحيوية والنضارة.",
            suitability = "البشرة العادية، والدهنية، والمختلطة (ويُتجنب تماماً في الحمل).",
            riskLevel = "نشط (يتطلب حذر وتدريج بالجرعة وواقي شمس)",
            riskColor = Color(0xFFFCA5A5), // Reddish light
            iconEmoji = "🧪",
            clinicalTip = "يُوضع ليلاً حصراً على بشرة تامة الجفاف والوقار طبيعي لها، بجرعة لا تتعدى حجم حبة البازلاء، مع الترطيب الفوري بكريم غني بالسيراميدات لتفادي الجفاف القشري الملازم للأسابيع الأولى.",
            matchingFilterTerm = "ريتينول (Retinol)"
        ),
        GlossaryIngredient(
            name = "Hyaluronic Acid",
            scientificName = "Sodium Hyaluronate",
            arabicName = "حمض الهيالورونيك (المغناطيس المائي)",
            category = "ترطيب عميق وحبس ماء",
            benefits = listOf(
                "يجذب ويرتكز بجزيئات المياه حتى 1000 ضعف وزنه الجاف.",
                "يملأ الفراغات البينية للخلايا ليعيد المظهر الممتلئ (Plump) والنضر.",
                "يرطب الأدمة ترطيباً مائياً فورياً مريحاً للغاية للبشرة الجافة."
            ),
            description = "مكثف وجاذب رطوبة فسيولوجي فائق الدقة، يقع بصورة حيوية في خلايا نسيج الجلد البشري. يزيل الخطوط الدقيقة المؤقتة الناتجة عن شح الماء ونقص الرطوبة البيئية.",
            suitability = "مثالي لجميع فئات البشرة دون استثناء وخاصة الجافة والمتهيجة.",
            riskLevel = "آمن ومغذي ومريح للغاية للبشرة",
            riskColor = Color(0xFFA7F3D0), // Greenish light
            iconEmoji = "💧",
            clinicalTip = "للحصول على المفعول المغناطيسي المضاعف، ضعي السيروم على بشرتك وهي منداة بالماء رطبة قليلاً، ثم قومي فوراً بوضع كريم مرطب قفل فوقه لحبس جزئيات الماء داخل مسامات الأدمة.",
            matchingFilterTerm = "حمض الهيالورونيك"
        ),
        GlossaryIngredient(
            name = "Niacinamide",
            scientificName = "Niacinamide / Vitamin B3",
            arabicName = "النياسيناميد (فيتامين B3 متعدد الوظائف)",
            category = "ترميم حاجز البشرة والتهيج",
            benefits = listOf(
                "ينظم ويقنن إفرازات الغدد الدهنية في عمق المسام لمنع اللمعان.",
                "يعزز تخليق السيراميدات والبروتينات داخل حاجز الغلاف الجلدي الخارجي.",
                "يمنع انتقال ميلانوسومات الخلايا الصباغية وبذلك يوحد لون البشرة."
            ),
            description = "مقاوم ممتاز وشبه معجزة للالتهاب والمسام المتوسعة، يقوي حاجز البشرة الوقائي بشكل تصاعدي، مما يجعله خط دفاع استثنائي ضد الجفاف والوهج البيئي الخارجي والوردية.",
            suitability = "البشرة الدهنية، والمختلطة، والمصابة بالمسام والوردية الحساسة.",
            riskLevel = "آمن وملطف ومقاوم تهيج رائع للبشرة",
            riskColor = Color(0xFFA7F3D0),
            iconEmoji = "🛡️",
            clinicalTip = "يمكن استخدامه في الروتين الصباحي والمسائي بمرونة مطلقة، وهو شريك فسيولوجي رائع لتقليل تهيج المقشرات والريتينول ومحبذ للدمج الكيميائي مع حمض الساليسيليك.",
            matchingFilterTerm = "النياسيناميد (Niacinamide)"
        ),
        GlossaryIngredient(
            name = "Salicylic Acid",
            scientificName = "Salicylic Acid / BHA",
            arabicName = "حمض الساليسيليك (مذيب دهون المسام)",
            category = "تقشير وتطهير مسام",
            benefits = listOf(
                "يتخلل ويذوب الدهون في أعماق المسامات لتفريق الرؤوس المغلقة والمفتوحة.",
                "يمتلك خصائص طبيعية مضادة للالتهابات تحد من بؤر حب الشباب الحمراء.",
                "يزيل بشكل فوري الخلايا القرنية الميتة المسببة لبهتان البشرة."
            ),
            description = "حمض بيتا هيدروكسي (BHA) محب للدهون وقابل للنفاذ الفريد عبر الجدران الزهمية للمسامات، مما يجعله السلاح الذهبي السريري لمنع انسداد المسام والقضاء على حب الشباب تدريجياً.",
            suitability = "البشرة الدهنية، والمختلطة المعرضة لتكون الحبوب واللمعان المفرط.",
            riskLevel = "معتدل (يستلزم الترطيب والاعتدال)",
            riskColor = Color(0xFFFDE047), // Yellowish light
            iconEmoji = "🍋",
            clinicalTip = "ابدئي باستعماله مرتين في الأسبوع مساءً بلمسات خفيفة، وتجنبي وضعه قرب منطقة العين، وضعي دائماً مرطباً لطيفاً بعده لاستعادة التوازن الهيدروليبيدي للجلد.",
            matchingFilterTerm = "حمض الساليسيليك (BHA)"
        ),
        GlossaryIngredient(
            name = "Ceramides",
            scientificName = "Ceramides 1, 3, 6-II",
            arabicName = "السيراميدات الطبيعية (شحم البناء وحامي الحاجز)",
            category = "ترميم حاجز البشرة والتهيج",
            benefits = listOf(
                "تمثل أكثر من 50% من دهون حاجز البشرة الواقي الخارجي.",
                "تمنع الفقدان المستمر غير المحسوس للماء عبر الجلد (TEWL).",
                "ترأب وترمم الصدوع والتشققات السطحية الناتجة عن الجفاف المزمن."
            ),
            description = "اللبنات والدهون الشحمية الأساسية المحتضنة بين خلايا الطبقة القرنية. تعمل مثل الإسمنت الذي يربط خطوط الطوب معاً ليصمد جدار الحماية ضد غزو الميكروبات والمواد المثيرة للاحمرار والتحسس.",
            suitability = "البشرة الحساسة جداً والملتهبة، والجافة كلياً، ومصابي الأكزيما والوردية.",
            riskLevel = "آمن جداً ويعتبر مكملاً فسيولوجياً ممتازاً",
            riskColor = Color(0xFFA7F3D0),
            iconEmoji = "🧱",
            clinicalTip = "لا غنى عن مركب يتضمن السيراميدات في حقيبتكِ العلاجية إذا كانت هناك بوادر احمرار أو حكة أو وخز بسبب المقشرات القاسية أو بعد رحلات الصيف والشمس الحارقة.",
            matchingFilterTerm = "السيراميد والبانثينول"
        ),
        GlossaryIngredient(
            name = "Panthenol",
            scientificName = "D-Panthenol / Pro-Vitamin B5",
            arabicName = "البانثينول (بروفيتامين B5 المجدد لطبقات الأدمة)",
            category = "ترميم حاجز البشرة والتهيج",
            benefits = listOf(
                "يتحول داخل خلايا البشرة إلى حمض البانتوثينيك الضروري لترميم الأنسجة.",
                "يسرع من تكاثر الخلايا الظهارية والتئام الجروح والشقوق السطحية.",
                "يهدئ التحسس، والتهيج، والاحمرار الجلدي فور التطبيق بنفاذ مريح."
            ),
            description = "مرطب جاذب للرطوبة ومعالج كيميائي معزز لخلايا الجلد المتهيجة. يعمل على المساعدة في ترميم الروابط النسيجية وحماية أنوية الخلايا الجلدية من التخريب والالتهاب المفرط.",
            suitability = "الجلد المتهيج، والبشرة الحساسة والجافة جداً المعرضة للاحمرار.",
            riskLevel = "آمن وصالح للاستعمال اليومي لجميع البشرات دون استثناء",
            riskColor = Color(0xFFA7F3D0),
            iconEmoji = "🌱",
            clinicalTip = "البانثينول يعتبر الخيار الأول بعد حروق الليزر أو حروق الشمس، كما أنه مهدئ ممتاز ويخفف حدة علاجات حب الشباب المجففة جداً مثل كريمات البنزويل بيروكسيد.",
            matchingFilterTerm = "السيراميد والبانثينول"
        ),
        GlossaryIngredient(
            name = "Vitamin C",
            scientificName = "L-Ascorbic Acid / Ascorbyl Phosphate",
            arabicName = "فيتامين سي (مضاد الأكسدة الجبار والمنير الخلوي)",
            category = "تفتيح وتنظيم ملانين",
            benefits = listOf(
                "يحيد الجذور الحرة الضارة الناتجة عن التعرض للشمس وتلوث البيئة.",
                "يعيق بشكل سريري نشاط صبغة الميلانين المصنعة بفرط للبقع.",
                "مشارك أساسي يحفز مصانع خلايا الأدمة لإنتاج بروتين الكولاجين."
            ),
            description = "مضاد الأكسدة الأول عالمياً، يعزز توهج الخلايا ونضارتها الزجاجية الفاتحة. يحمي خلايا الجلد من التلف الضوئي المعجل للشيخوخة الباكرة ويدعم تلاشي كلف المساء والبقع الناتجة عن البثور السابقة.",
            suitability = "جميع أنواع البشرة للتفتيح والنضارة (ويفضل تركيزات خفيفة للحساسة).",
            riskLevel = "معتدل (قد يتأكسد بالضوء والأكسجين ويحتاج زجاجة معتمة)",
            riskColor = Color(0xFFFDE047),
            iconEmoji = "🍊",
            clinicalTip = "استخدميه نهاراً تحت كريم واقي الشمس لتغطية دفاعية متكاملة تفتت أشعة الشمس الضارة وتزود حماية مضاعفة للجلد لن تندمي عليها أبداً.",
            matchingFilterTerm = "فيتامين سي (Vitamin C)"
        ),
        GlossaryIngredient(
            name = "Thiamidol",
            scientificName = "Isobutylamido Thiazolyl Resorcinol",
            arabicName = "التياميدول (قاهر التصبغات المعقدة المجهري)",
            category = "تفتيح وتنظيم ملانين",
            benefits = listOf(
                "المركب الحاصل على براءة اختراع طبية كأقوى مثبط لإنتاج التيروزيناز البشري.",
                "يقلل بوضوح مسافات التصبغات في الجلد وتناظر البقع الداكنة.",
                "يمنع ظهور خلايا بؤر الميلانين النشطة حديثاً في الجلد."
            ),
            description = "مبتكر جزيئي فائق الأداء والخصوصية في معالجة البقع العنيدة والكلف وتصبغات الحوامل التراكمية، يتفوق بفعالية سريعة تبدأ بالظهور المخملي خلال أسبوعين من المواظبة السريرية المستقيمة.",
            suitability = "البشرة ذات التصبغات الكثيفة، وكلف الحوامل، والنمش المتصاعد.",
            riskLevel = "آمن وفعال بدقة طبية معملية عالية ومثبتة",
            riskColor = Color(0xFFA7F3D0),
            iconEmoji = "✨",
            clinicalTip = "يُطبق مباشرة على موضع البقع الداكنة مع توزيعه بلطف مرتين في اليوم، ويُعتبر البديل الأكثر أماناً للهيدروكينون بدون أي آثار جانبية مبيضة طارئة.",
            matchingFilterTerm = "فيتامين سي (Vitamin C)" // Thiamidol is matched under active Vitamin C/Melanin Inhibitors filter
        ),
        GlossaryIngredient(
            name = "Snail Mucin",
            scientificName = "Snail Secretion Filtrate 96%",
            arabicName = "ترشيح إفراز البزاق (سر النضارة المائية الكورية)",
            category = "ترطيب عميق وحبس ماء",
            benefits = listOf(
                "مغذي فائق وسريع يرفع مستويات المرونة الهيكلية لألياف الإيلاستين.",
                "يجدد التلف الخلوي بفضل محتواه الطبيعي من النبتة والآلانتوين وحمض الجليكوليك.",
                "يمنح البشرة مظهر زجاجي لامع فائق الترطيب والانعكاس الطبيعي للضوء."
            ),
            description = "إفراز طبيعي مصفى يتميز بلزوجة هيدروجينية حيوية مدهشة، يغذي نسيج الجلد بمزيج معقد من الببتيدات والمعادن وحمض الهيالورونيك، مما يجعله المحرك الأول للبشرة الكورية اللامعة النضرة وخفيفة التلف السطحي.",
            suitability = "البشرة الباهتة، المطفية والفاقدة للمرونة والرطوبة ومحبي النضارة المائية.",
            riskLevel = "آمن ويحتوي على تلاؤم حيوي استثنائي وسلس",
            riskColor = Color(0xFFA7F3D0),
            iconEmoji = "🐌",
            clinicalTip = "يُوضع على بشرة رطبة بعد الغسول وقبل استعمال الكريمات المغذية الثقيلة. امسحي وربتي بيديك بخفة لكي تتشرب جزيئاته اللزجة داخل عمق الخلايا بكفاءة وسرعة.",
            matchingFilterTerm = "السيراميد والبانثينول" // Snail/Allantoin matched in Sere/Cera/Panthenol filter
        ),
        GlossaryIngredient(
            name = "Zinc PCA / Zinc",
            scientificName = "Zinc Pyrrolidone Carboxylic Acid",
            arabicName = "الزنك المقاوم للميكروبات (مهدئ ومزيل زيوت)",
            category = "تقشير وتطهير مسام",
            benefits = listOf(
                "مضاد قوي للالتهاب يكبح تكاثر بكتيريا حب الشباب (C. acnes).",
                "يقلل إفراز الدهون وينعم سطح البشرة للحد من اللمعان والزهم.",
                "يدعم عمليات التئام وتجديد النسيج التالف جراء الندوب والبثور."
            ),
            description = "معدن حيوي فسيولوجي متميز بخصائص مهدئة ومعقمة للمسام. يساعد على المواءمة والتخلص من لمعة الجبين والأنف وتجفيف الغدد الزهمية المفرطة في الإفراز.",
            suitability = "البشرة الدهنية، شديدة الزيتية، والوردية والمليئة بالحبوب.",
            riskLevel = "آمن ومطهر ويعزر صفاء خلايا البشرة الجذعي",
            riskColor = Color(0xFFA7F3D0),
            iconEmoji = "🔮",
            clinicalTip = "متوافق ومحبذ للغاية استخدامه رفقة النياسيناميد، حيث يعملان معاً كدرع منظم دهون مسامي رائع ومقاوم فائق للبثور والرؤوس الحمراء الملتهبة.",
            matchingFilterTerm = "النياسيناميد (Niacinamide)" // Zinc PCA matched with Niacinamide
        )
    )
}

@Composable
fun SkincareGlossaryComponent(
    accentColor: Color,
    cardColor: Color,
    textColorMuted: Color,
    selectedFilterIngredientName: String = "جميع المكونات",
    onSelectIngredientFilter: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabCategory by remember { mutableStateOf("الكل") }

    val categories = listOf(
        "الكل" to "جميع الفئات 🧬",
        "مقاومة التجاعيد والخطوط" to "مكافحة التجاعيد 🌸",
        "ترطيب عميق وحبس ماء" to "ترطيب وحبس ماء 💧",
        "ترميم حاجز البشرة والتهيج" to "ترميم وحماية 🧱",
        "تقشير وتطهير مسام" to "تقشير ومسام 🍋",
        "تفتيح وتنظيم ملانين" to "تفتيح وبقع ✨"
    )

    // Highlight the active ingredient from product filtering if passed in!
    var highlightedIngredientName by remember(selectedFilterIngredientName) {
        val matchedName = if (selectedFilterIngredientName != "جميع المكونات") {
            SkincareGlossaryData.ingredients.firstOrNull {
                it.matchingFilterTerm.lowercase().contains(selectedFilterIngredientName.lowercase()) ||
                selectedFilterIngredientName.lowercase().contains(it.name.lowercase())
            }?.name
        } else null
        mutableStateOf(matchedName)
    }

    // Filter ingredients based on Search query and Category Tab
    val filteredGlossary = remember(searchQuery, selectedTabCategory, selectedFilterIngredientName) {
        SkincareGlossaryData.ingredients.filter { ing ->
            val matchCategory = selectedTabCategory == "الكل" || ing.category == selectedTabCategory
            val matchSearch = searchQuery.isBlank() ||
                    ing.name.contains(searchQuery, ignoreCase = true) ||
                    ing.arabicName.contains(searchQuery) ||
                    ing.scientificName.contains(searchQuery, ignoreCase = true) ||
                    ing.description.contains(searchQuery) ||
                    ing.benefits.any { it.contains(searchQuery) }

            matchCategory && matchSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("skincare_interactive_glossary")
            .background(cardColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // --- GLOSSARY HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    searchQuery = ""
                    selectedTabCategory = "الكل"
                    highlightedIngredientName = null
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "استعادة التعيين",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "دليل المكونات والمواد الطبية 📚🧪",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("🔬", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "تصفحي القاموس التفاعلي لفهم المكونات النشطة كيميائياً، تأثيرها الخلوي، وكيف تختارين تركيبتك بدقة مع ميزة الفلترة الفورية.",
            color = textColorMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- SEARCH INPUT ---
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isNotBlank()) highlightedIngredientName = null
            },
            placeholder = {
                Text(
                    text = "ابحثي بالاسم (مثال: ريتينول، Hyaluronic)... 🔍",
                    color = textColorMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            leadingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            },
            trailingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.35f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- CATEGORIES HORIZONTAL NAVIGATION ROW ---
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            reverseLayout = true
        ) {
            items(categories) { (categoryKey, categoryLabel) ->
                val isSelected = selectedTabCategory == categoryKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) accentColor else Color.Black.copy(alpha = 0.25f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedTabCategory = categoryKey
                            highlightedIngredientName = null
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Showing notification if deep linked from filters
        if (selectedFilterIngredientName != "جميع المكونات" && highlightedIngredientName != null) {
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "مكون نشط مصفى حالياً بالأعلى ✨",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تم جلب وإبراز تفاصيل $selectedFilterIngredientName سريرياً بالأسفل لمعرفة خصائصه.",
                            color = Color.White,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("💡", fontSize = 16.sp)
                }
            }
        }

        // --- INGREDIENTS EXPANDABLE CARDS LIST ---
        if (filteredGlossary.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد مكونات تطابق البحث أو التصنيف المختار حالياً 🌵",
                    color = textColorMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Render maximum 5 to keep listing inside the parent LazyColumn balanced, but expand all if user filters
            val itemsToShow = filteredGlossary

            itemsToShow.forEach { ingredient ->
                val isHighlighted = highlightedIngredientName == ingredient.name
                var isExpanded by remember { mutableStateOf(isHighlighted) }

                // Automatically expand if it became highlighted via deep link selection
                LaunchedEffect(highlightedIngredientName) {
                    if (highlightedIngredientName == ingredient.name) {
                        isExpanded = true
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHighlighted) accentColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .border(
                            width = if (isHighlighted) 1.5.dp else 1.dp,
                            color = if (isHighlighted) accentColor else Color.White.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(12.dp)
                    ) {
                        // Card Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "تقليص" else "توسيع",
                                tint = if (isHighlighted) accentColor else textColorMuted,
                                modifier = Modifier.size(18.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = ingredient.arabicName,
                                        color = if (isHighlighted) accentColor else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = ingredient.scientificName,
                                        color = textColorMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ingredient.iconEmoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                        .wrapContentSize(Alignment.Center)
                                )
                            }
                        }

                        // Expanded Content with Animations!
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Category Badge & Risk Level
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(ingredient.riskColor)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ingredient.riskLevel,
                                            color = ingredient.riskColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(accentColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = ingredient.category,
                                            color = accentColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Description
                                Text(
                                    text = ingredient.description,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Key Benefits Bullets
                                Text(
                                    text = "فوائد جزيئية رئيسية الخلايا المستهدفة: 🔬📝",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                ingredient.benefits.forEach { benefit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = benefit,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Suitability
                                Text(
                                    text = "الأنواع المناسبة: ${ingredient.suitability}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Clinical Tip from Zubayda Ramzi
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor.copy(alpha = 0.05f))
                                        .border(1.dp, accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "نصيحة الأخصائية زبيدة رمزي 🔬👩‍⚕️",
                                                color = accentColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = ingredient.clinicalTip,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Interlink Action button inside detail results
                                val isSelectedAsActiveFilter = selectedFilterIngredientName == ingredient.matchingFilterTerm
                                Button(
                                    onClick = {
                                        if (isSelectedAsActiveFilter) {
                                            onSelectIngredientFilter("جميع المكونات")
                                        } else {
                                            onSelectIngredientFilter(ingredient.matchingFilterTerm)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelectedAsActiveFilter) Color.White.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.14f)
                                    ),
                                    border = BorderStroke(1.dp, if (isSelectedAsActiveFilter) Color.White.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelectedAsActiveFilter) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                                            contentDescription = null,
                                            tint = if (isSelectedAsActiveFilter) Color.White else accentColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSelectedAsActiveFilter) "إلغاء فحص هذا المكون وتصفية الكل ✖" else "تصفية مستحضرات الصيدلية بهذا المكون 🔬🛒",
                                            color = if (isSelectedAsActiveFilter) Color.White else Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
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
}
