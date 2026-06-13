package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BaumannQuestion(
    val id: String,
    val textAr: String,
    val options: List<Pair<Int, String>> // Value to text
)

data class BaumannDimension(
    val id: String,
    val titleAr: String,
    val tagAr: String,
    val emoji: String,
    val questions: List<BaumannQuestion>,
    val letterLow: String,
    val letterHigh: String,
    val threshold: Float = 2.5f
)

val BAUMANN_KOTLIN_DIMENSIONS = listOf(
    BaumannDimension(
        id = "D1",
        titleAr = "البعد الأول: الدهنية والجفاف (D vs O)",
        tagAr = "قياس نشاط الغدد الدهنية وقوة ترطيب الحاجز الطبيعي.",
        emoji = "💧",
        letterLow = "D",
        letterHigh = "O",
        questions = listOf(
            BaumannQuestion(
                id = "D1_Q1",
                textAr = "بعد غسل وجهك بالمنظف ورغوة التطهير، كيف تشعر بشرتك بعد مرور 20-30 دقيقة دون كريم؟",
                options = listOf(
                    1 to "شد قوي للغاية متبوعاً بقشرية وخشونة حادة",
                    2 to "شد خفيف ولكن النسيج جاف وغير ريان",
                    3 to "مريحة ومتوازنة النعومة دون دهون أو لمعان",
                    4 to "لامعة وتفرز دهوناً زيتية واضحة وملموسة"
                )
            ),
            BaumannQuestion(
                id = "D1_Q2",
                textAr = "كيف يبدو بريق ولمعان وجهك في الصور الفوتوغرافية المقربة كلياً؟",
                options = listOf(
                    1 to "باهتة، جافة ومفتقدة لأي لمعان نضارة",
                    2 to "مات (مطفأة) وناعمة وسلسة كلياً",
                    3 to "لمعان لطيف ونضر يتركز بوضوح في الجبين والأنف",
                    4 to "لمعان دهني زيتي كامل ومستمر يعكس الضوء"
                )
            ),
            BaumannQuestion(
                id = "D1_Q3",
                textAr = "كم مرة تلاحظ تقشراً دقيقاً أو قشوراً جافة على سطح وجهك؟",
                options = listOf(
                    1 to "دائماً ובصورة يومية تقريباً",
                    2 to "أحياناً في الفصول الباردة والجافة",
                    3 to "نادراً جداً وفي نطاق ضيق للغاية",
                    4 to "مستحيل، بشرتي رطبة وممتلئة بالدهون دائماً"
                )
            ),
            BaumannQuestion(
                id = "D1_Q4",
                textAr = "بعد ساعتين من تطبيق كريم مرطب عادي، كيف تبدو بشرتك؟",
                options = listOf(
                    1 to "تشربته تماماً وعادت تشعر بالعطش والشد",
                    2 to "ناعمة كالحرير ومحمية ومثالية الراحة",
                    3 to "لمعان طفيف وبداية خروج إفرازات زهمية",
                    4 to "دهنية ومغطاة بطبقة زبدة ثقيلة ومزعجة"
                )
            )
        )
    ),
    BaumannDimension(
        id = "D2",
        titleAr = "البعد الثاني: الحساسية والمقاومة (S vs R)",
        tagAr = "قياس تفاعلية الجلد والوردية والتهاب خلايا البشرة.",
        emoji = "🛡️",
        letterLow = "R",
        letterHigh = "S",
        questions = listOf(
            BaumannQuestion(
                id = "D2_Q1",
                textAr = "هل يسبب لك تجريب منتج عناية جديد حرقاناً أو احمراراً أو حكة؟",
                options = listOf(
                    1 to "لا يحدث أبداً، بشرتي تحتمل أي منتج كيميائي",
                    2 to "نادراً مع مقشرات أو تراكيز قوية للغاية",
                    3 to "أحياناً، لاسيما عند وجود كحول أو معطرات",
                    4 to "غالباً ودائماً، بشرتي مفرطة الاحتقان والتفاعل"
                )
            ),
            BaumannQuestion(
                id = "D2_Q2",
                textAr = "هل سبق أن شخصك أخصائي أمراض جلدية بالوردية أو الأكزيما أو حب الشباب المفرط؟",
                options = listOf(
                    1 to "لا، بشرتي سليمة ومقاومة تماماً",
                    2 to "أعتقد ذلك في السابق ولكن خفيف وزال",
                    3 to "نعم، أعاني من حالة خفيفة إلى متوسطة",
                    4 to "نعم، حالة مزمنة نشطة ومتهجة طوال العام"
                )
            ),
            BaumannQuestion(
                id = "D2_Q3",
                textAr = "عند خروجك للشمس أو تعرضك للحرارة أو عند المجهر، هل يتورم وجهك باللون الأحمر؟",
                options = listOf(
                    1 to "أبداً، لوني يحافظ على اتزانه الطبيعي",
                    2 to "نادراً ويرتاح الاحمرار فور الهدوء",
                    3 to "أحياناً يورد لبعض الوقت ويحتاج لتهدئة",
                    4 to "دائماً، يتوهج كجمرات ملتهبة ويستمر لساعات"
                )
            ),
            BaumannQuestion(
                id = "D2_Q4",
                textAr = "كم عدد البثور أو الحبوب الحمراء المتفاعلة التي تلاحظها شهرياً؟",
                options = listOf(
                    1 to "حبة واحدة على الأكثر أو لا شيء",
                    2 to "من حبتين إلى 4 حبات خفيفة",
                    3 to "من 5 إلى 10 حبات ملتهبة ومزعجة",
                    4 to "أكثر من 10 حبات ومعظمها كيسية غائرة"
                )
            )
        )
    ),
    BaumannDimension(
        id = "D3",
        titleAr = "البعد الثالث: النزعة التصبغية (P vs N)",
        tagAr = "قياس وتيرة نشاط الميلانوما وقابلية ظهور البقع والكلف.",
        emoji = "☀️",
        letterLow = "N",
        letterHigh = "P",
        questions = listOf(
            BaumannQuestion(
                id = "D3_Q1",
                textAr = "بعد شفاء حبة شباب أو جرح، هل تترك علامة داكنة أو بقعة بنية ثابتة؟",
                options = listOf(
                    1 to "أبداً، تتعافى وتعود البشرة للونها المتجانس",
                    2 to "نادراً ما تترك علامة زهرية تتلاشى سريعاً",
                    3 to "غالباً تترك بقعا بنية تظل لأسابيع وأشهر",
                    4 to "دائماً وبشكل ثابت، أي خدش بسيط يترك تصبغاً عميقاً"
                )
            ),
            BaumannQuestion(
                id = "D3_Q2",
                textAr = "هل تلاحظ وجود كلف متكاثف أو نمش شمسي داكن على وجهك؟",
                options = listOf(
                    1 to "لا يوجد لدي أي نمش أو تصبغات إطلاقاً",
                    2 to "عدد قليل خفيف للغاية وغير ملاحظ",
                    3 to "نمش وتناثر لوني واضح يتركز بالوسط والوجنات",
                    4 to "بقع كلف داكنة جسيمة ومنتشرة بشكل معقد"
                )
            ),
            BaumannQuestion(
                id = "D3_Q3",
                textAr = "عند جلوسك تحت أشعة الشمس المباشرة لفترة، كيف تسمر بشرتك؟",
                options = listOf(
                    1 to "أحترق بشدة وأتقشر ولا أكتسب لوناً أسمراً إطلاقاً",
                    2 to "أحترق قليلاً ثم يظهر غسق أسمر باهت",
                    3 to "أسمر وأكتسب لون مبهج وغني بنسبة جيدة مع احتراق طفيف",
                    4 to "أسمر بعمق مباشر وتتحرك خلايا الصباغ دون أي احتراق"
                )
            ),
            BaumannQuestion(
                id = "D3_Q4",
                textAr = "هل تعرضت لدهان كلف هرموني غامق التصبغ (مثل قناع كلف الحمل والولادة)؟",
                options = listOf(
                    1 to "لا، لم يحدث لي ذلك قط",
                    2 to "بقع خفيفة للغاية تظهر في فترات متباعدة ثم تزول",
                    3 to "نعم، توجد مناطق محددة فوق الفم والخدين تظل داكنة",
                    4 to "نعم، كلف عام واسع الانتشار مائل للسواد ومزمن"
                )
            )
        )
    ),
    BaumannDimension(
        id = "D4",
        titleAr = "البعد الرابع: التجاعيد ومقاومة السن (W vs T)",
        tagAr = "قياس مستويات انهيار ألياف الإيلاستين المجهرية والأكسدة الجلدية.",
        emoji = "⏳",
        letterLow = "T",
        letterHigh = "W",
        questions = listOf(
            BaumannQuestion(
                id = "D4_Q1",
                textAr = "في وضعية الاسترخاء التام للمرآة، هل ترى خطوطاً دقيقة واضحة بالبشرة؟",
                options = listOf(
                    1 to "لا يوجد أي أثر، ناعمة ومشدودة وجلد ممتلئ",
                    2 to "خطوط مجهرية حول العين لا ترى إلا تحت تركيز ضوئي",
                    3 to "توجد خطوط تعبيرية خفيفة تظل ثابتة على الجبهة",
                    4 to "نعم، تجاعيد واضحة وملموسة تمنح الوجه ترهلاً خفيفاً"
                )
            ),
            BaumannQuestion(
                id = "D4_Q2",
                textAr = "ما هو تاريخ ومعدل تعرضك لأشعة الشمس دون استخدام واقي SPF؟",
                options = listOf(
                    1 to "ضئيل جداً، معظم حياتي بالداخل وألتزم بالواقي يومياً",
                    2 to "معتدل، أخرج بتعقل وغالباً أتفادى شمس الظهيرة الحارة",
                    3 to "مرتفع، أمارس أعمالاً وأنشطة بالخارج طويلاً دون حماية",
                    4 to "شديد للغاية، أسمر وأقضي ساعات متراكمة طوال العام"
                )
            ),
            BaumannQuestion(
                id = "D4_Q3",
                textAr = "بالتأمل بوالديك وأقاربك، هل الشيخوخة الجلدية سريعة في جيناتكم؟",
                options = listOf(
                    1 to "يبدون أصغر بكثير من عمرهم ومقاومتهم للتجاعيد جينية فائقة",
                    2 to "متوافقين بشكل طبيعي وأنيق مع تقدم سنهم المتوقع",
                    3 to "تظهر لديهم تجاعيد تعبيرية وارتخاء جلد مبكر بأعمار الأربعين",
                    4 to "يبدون أكبر سناً بشكل لافت وتجاعيد غائرة واسعة الترهل"
                )
            ),
            BaumannQuestion(
                id = "D4_Q4",
                textAr = "هل تدخن أو تعيش بانتظام مع مدخن نشط في غرف مغلقة مسببة للأكسدة؟",
                options = listOf(
                    1 to "لا إطلاقاً، أعيش في بيئة متوازنة ونقية وخالية من السموم برئتي",
                    2 to "دخنت قليلاً في سنوات سابقة وتوقفت كلياً",
                    3 to "مدخن سلبي بالمنزل أو العمل بانتظام",
                    4 to "نعم، مدخن نشط وبصورة يومية كثيفة وشبه مستمرة"
                )
            )
        )
    )
)

data class KotlinSkinTypeRec(
    val code: String,
    val nameAr: String,
    val descAr: String,
    val prosAr: String,
    val consAr: String,
    val cleanser: String,
    val active: String,
    val moisturizer: String
)

val KOTLIN_SKIN_TYPE_RECS = mapOf(
    "DRNT" to KotlinSkinTypeRec(
        "DRNT", "الجافة، المقاومة، غير التصبغية، المشدودة",
        "تتمتع بشرتك بمناعة نسيجية عالية وخلو من التصبغات والتجاعيد، لكنها تفتقر للغشاء الدهني المغذي للبشرة. عائقك الأكبر هو الجفاف والحد من فقدان الماء المجهري.",
        "نادرة التحسس، نقية لونيًا ومشدودة وممتلئة النسيج.", "عطش جلدي، عروق وقشور مجهرية وخشونة بالملمس الخارجي.",
        "منظف حليبي غير رغوي غني بالسكوالين الشوفاني.", "سيروم حمض الهيالورونيك 2% لربط الماء في الخلايا.", "كريم حاجز غني بالسيراميد Lipids الثلاثي الفاخر."
    ),
    "DRNW" to KotlinSkinTypeRec(
        "DRNW", "الجافة، المقاومة، غير التصبغية، المعرضة للتجاعيد",
        "تفتقر بشرتك للدهون الطبيعية مما يزيد من سرعة تبخر الرطوبة، وهو ما يمهد مع الزمن لانهيار الكولاجين وظهور التجاعيد التعبيرية المبكرة.",
        "نقية من النمش والتصبغات، وتتلقى المكونات المضادة للتجاعيد بيسر عالي وجدار مقاوم.", "خطوط مبكرة حول العين وفقدان رونق رطوبة الخلايا.",
        "غسول زيتي مغذي للبشرة.", "ريتينول 0.5% مع باكوتشيول لتحفيز الفيبروبلاست والكولاجين.", "مرطب مغذي عميق غني بالببتيدات المركبة وزبدة الشيا."
    ),
    "DRPT" to KotlinSkinTypeRec(
        "DRPT", "الجافة، المقاومة، التصبغية، المشدودة",
        "تتميز بشرتك بالسلامة من الخطوط والشيخوخة المبكرة، ولكن خلل توزيع الميلانين ينتج بقعاً غامقة وكلفاً تبدو أكثر وضوحاً بسبب الجفاف وبهتان السطح.",
        "مقاومة لمشاكل وأحماض وخطوط الشيخوخة والترهل وجدر ممتلئ.", "بهتان لوني وكلف منتشر وقشور جافة تفقد البشرة بريقها.",
        "منظف لطيف بالمرطبات الكولاجينية لتهيئة سطح النسيج.", "سيروم فيتامين C النقي بالتآزر مع ألفا أربوتين لتفتيح الخلايا.", "مرطب مائي غني بخصائص التفتيح والنياسيناميد والانتشار."
    ),
    "DRPW" to KotlinSkinTypeRec(
        "DRPW", "الجافة، المقاومة، التصبغية، المعرضة للتجاعيد",
        "بشرة جافة وتظهر عليها البقع والتجاعيد معاً بفعل التدمير الضوئي وأشعة الشمس والتعرض البيئي. تتقبل بشرتك المكونات النشطة القوية بكفاءة عالية لتجديد الخلايا.",
        "تتحمل المواد الفعالة المركزة والأحماض والتقشير دون نوبات تحسس.", "جفاف دائم، كلف داكن وتجاعيد رفيعة غائرة في الجبين.",
        "منظف زيتي غني بمضادات الأكسدة والسكوالين.", "سيروم النياسيناميد 10% مدمجاً مع بروتوكول الريتينول السلس.", "مرطب ليلي دهني غني بالسيراميد وحمض اللاكتيك الملطف."
    ),
    "DSNT" to KotlinSkinTypeRec(
        "DSNT", "الجافة، الحساسة، غير التصبغية، المشدودة",
        "تعاني بشرتك من جدار حماية تالف، مما يجعلها جافة على الدوام وفي حالة تهيج واحمرار مستمر لأقل مؤثر. لحسن الحظ تظل مشدودة ونقية من البقع الداكنة.",
        "نقية من الكلف والتصبغات المزعجة جراء البثور وتجاعيد غائبة جينياً.", "احمرار نسيجي مستمر، حرقان من المنتجات وقشور تهيج متكررة.",
        "منظف حليبي حمضي pH 5.5 خالي تماماً من العطور والصابون.", "سيروم خلاصة السيكا (Centella) والشوفان والبانثينول المرمم للجلد.", "مرطب طبي ثقيل خالي من المواد الحافظة المهيجة لترميم الحاجز."
    ),
    "DSNW" to KotlinSkinTypeRec(
        "DSNW", "الجافة، الحساسة، غير التصبغية، المعرضة للتجاعيد",
        "يتآمر الجفاف والحساسية لتسريع تلف الخلايا السطحية والألياف العميقة، مما يعرض بشرتك للخطوط ومظهر الشيخوخة المبكرة مع نوبات احمرار ونخز.",
        "بشرة نقية لونيًا ومحمية من البقع الصبغية السوداء.", "تجاعيد رفيعة، جفاف حاد وجلد هش يتأثر بالملامسة السريعة.",
        "منظف لطيف بالبابونج ومستخلص الألوفيرا الطبيعي.", "ببتيدات النحاس المخففة مع ريزفيراترول مضاد للأكسدة.", "كريم سيراميد ثري ومطعم بمهدئات الشحوم والحماية الفائقة."
    ),
    "DSPT" to KotlinSkinTypeRec(
        "DSPT", "الجافة، الحساسة، التصبغية، المشدودة",
        "تجمع بشرتك بين التحسس العالي والنزعة التصبغية القوية؛ أي تهيج أو بثور حمراء تتحول تلقائياً لبقع داكنة عسيرة. لا يمكنك استخدام مقشرات فيزيائية.",
        "مشدودة ومتماسكة النسيج الكولاجيني وحيوية جيدة ضد الخطوط.", "تصبغات عنيدة ناتجة عن التهاب الجلد واحمرار متكرر وجفاف.",
        "غسول كريمي مهدئ بالشوفان الغروي المنعم.", "سيروم حمض الترانيكساميك المخفف مع نياسيناميد 5% لتفادي التبقع.", "كريم مرطب بالهيالورونيك وخلاصة نبات العرقسوس البارد المهدئ."
    ),
    "DSPW" to KotlinSkinTypeRec(
        "DSPW", "الجافة، الحساسة, التصبغية، المعرضة للتجاعيد",
        "أحد أكثر أنواع البشرة رقة وتطلباً. تتنافس عوامل التهيج والتلف البقعي وتكسر الكولاجين السريع تحت وطأة جفاف تام. البدء بجدار الترطيب والمهدئات وواقي شمس فيزيائي ضرورة قصوى.",
        "بشرة بالغة الحساسية تستجيب سريعاً للعلاجات الهادئة والتغذوية الصارمة.", "جفاف شامل، احمرار وتوهج متواصل، كلف داكن وتجاعيد.",
        "زيت منظف غني بمستخلص الصبار الطبيعي لتهدئة النسيج.", "ببتيدات مغلفة مع نياسيناميد لطيف لضبط جميع المحاور السطحية.", "مرطب غني عميق للبشرة الحساسة خال تماماً من المعطرات الكحولية."
    ),
    "ORNT" to KotlinSkinTypeRec(
        "ORNT", "الدهنية، المقاومة، غير التصبغية، المشدودة",
        "البشرة الأقرب وراثياً لتوازن الفلورا والاستقرار. بريق وفير وافرازات حمائية تحميكِ من التجاعيد والجفاف، وجدار صلب وخال من البقع. العناية تقتصر على تنظيف المسامات.",
        "إنتاج دهني ناضر، شباب دائم وبشرة لا تتحسس ولا تتبقع إطلاقاً.", "لمعان مستمر ورؤوس دهنية سوداء ومسامات واسعة واضحة بوسط الوجه.",
        "غسول رغوي عميق بحمض الساليسيليك Salicylic Acid 2%.", "سيروم نياسيناميد 10% لضبط الزهم وتضييق القطر المسامي.", "مرطب هلامي مائي جل خفيف للغاية غني بالزنك وموازنات الجفاف."
    ),
    "ORNW" to KotlinSkinTypeRec(
        "ORNW", "الدهنية، المقاومة، غير التصبغية، المعرضة للتجاعيد",
        "بشرة دهنية رطبة تحارب علامات الجفاف والخطوط الرفيعة، ولكن تلف الإيلاستين بفعل العمر يتطلب السيطرة. جدارك الصلب يقابل الريتينول القوي بكفاءة خرافية.",
        "تتحمل أحماض الـ AHA المقشرة والريتينول المركز بكفاءة ودون أي مشاكل.", "لمعان زيتي مزعج مع خطوط تعبيرية غائرة وارتخاء حول العين.",
        "منظف رغوي منشط لتبادل الخلايا وإزالة الزهم الزائد.", "سيروم الريتينول السريري 1% متبوعاً بمضادات الأكسدة.", "لوشن ليلي خفيف ومائي غني بحمض الجليكوليك لتوحيد الساق الفايبري."
    ),
    "ORPT" to KotlinSkinTypeRec(
        "ORPT", "الدهنية، المقاومة، التصبغية، المشدودة",
        "بشرة شابة مشبعة بالترطيب الكولاجيني الطبيعي ومقاومة للشيخوخة والتهل، ولكنها مستهدفة ببقع الشمس والنمش وتناثر الميلانين. المقشرات والأربوتين خيار ممتاز.",
        "متماسكة الأدمة وريانة بالشد ولا تتحسس من مقشرات التفتيح الفعالة.", "تناسق لوني ضعيف ودهون نشطة طوال اليوم تطفو على السطح.",
        "غسول رغوي بحمض الساليسيليك والجليكوليد المقشر.", "سيروم فيتامين C النقي 15% متبوعاً بألفا أربوتين لتأمين النقاء.", "مرطب لوشن تفتيحي خفيف القوام وخال من الزيوت المانعة للمسام."
    ),
    "ORPW" to KotlinSkinTypeRec(
        "ORPW", "الدهنية، المقاومة، التصبغية، المعرضة للتجاعيد",
        "بشرة ممتلئة بالدهون ومقاومة للحساسية ولكنها مستهدفة بالتصبغات والخطوط التعبيرية معاً. الجدار الطبيعي السميك يتيح لك استخدام التجديد الخلوي والتقشير العنيف.",
        "تستجيب بكفاءة لأقوى صيحات التقشير وإعادة الرونق اللوني دون تهيج بالجلد.", "مسام واسعة، بريق زيتي كالح، كلف مبعثر وخطوط جبهية واضحة.",
        "غسول رغوي علاجي يومي مقشر مزدوج لتنظيف السطح وعمقه.", "سيروم ريتينول 0.5% بالتناوب مع سيروم الفيتامين سي والأربوتين.", "مرطب مائي من موازن الدهون الدهني الخفيف جداً."
    ),
    "OSNT" to KotlinSkinTypeRec(
        "OSNT", "الدهنية، الحساسة، غير التصبغية، المشدودة",
        "تجمع بشرتك بين فرط إنتاج الدهون وحساسية الخلايا المتكررة. تظهر البثور الحمراء بآلية تهيج حب الشباب، ولكن نقاء الجينات يحميكِ من ذكريات البقع البنية الدائمة.",
        "شباب ناضر وجينات مرونة ممتازة، والحبوب تصفي لوناً عادياً بعد الشفاء.", "حب شباب نشط، احمرار وتوهج فوري بالحرارة، مسامات ملتهبة ممتلئة بالزهم.",
        "غسول رغوي لطيف للغاية pH 5.5 بحمض الساليسيليك المخفف.", "سيروم نياسيناميد 10% بخصائصه المقوضة لإفرازات حب الشباب.", "جل مائي فوري الامتصاص خلاصة السيكا (Centella) والشاي الأخضر."
    ),
    "OSNW" to KotlinSkinTypeRec(
        "OSNW", "الدهنية، الحساسة، غير التصبغية، المعرضة للتجاعيد",
        "بشرة دهنية وحساسة ومعرضة للتجاعيد في ذات اللحظة. يساهم الالتهاب المستمر للبثور والدهون الفائضة في تسريع ارتخاء الجلد وظهور علامات العمر المبكرة.",
        "بشرة نقية ومحمية من التصبغات والكلف واللطخات الداكنة.", "لمعان دهني، بثور دورية، خطوط تعبيرية رفيعة حول الجبين والعين.",
        "غسول جل مهدئ بـ مياه البابونج والألوفيرا الطبيعية.", "سيروم ببتيدات مجتمعة لإعادة بناء اللياقة الكولاجينية بلطف.", "مرطب هيدروجيني جل مائي مبرد خال تماماً من الزيوت الثقيلة."
    ),
    "OSPT" to KotlinSkinTypeRec(
        "OSPT", "الدهنية، الحساسة، التصبغية، المشدودة",
        "النوع الكلاسيكي المعرض لحب الشباب المصحوب بالبقع البنية الدائمة. تفرز البشرة دهوناً وتصطدم باحمرار، وأي بثرة ملتهبة تترك أثراً بنياً داكناً (PIH).",
        "جينات مشدودة ومظهر شبابي كثيف ومقاومة ممتازة للتجاعيد العميقة.", "مسامات واسعة دائرية، بقع داكنة مبعثرة للحبوب ودهون مستمرة.",
        "غسول رغوي مهدئ مطهر بالزنك والشوفان لضبط البكتيريا بلطف.", "سيروم نياسيناميد 10% مع ألفا أربوتين وسيروم ساليسيليك 1% اللطيفة.", "جل ترطيب غني بالبانثينول مائي غير زيتي لضمان تبريد النسيج."
    ),
    "OSPW" to KotlinSkinTypeRec(
        "OSPW", "الدهنية، الحساسة، التصبغية، المعرضة للتجاعيد",
        "تجمع بشرتك كل التحديات الجلدية الأساسية: دهون مفرطة وبثور، حساسية واحمرار حار، تصبغات تالية لحب الشباب، وخطوط تعبيرية مبكرة بفعل الأكسدة الدائمة.",
        "تستجيب بشكل مذهل عند ارساء بروتوكول متزن خال من التهيج الكيميائي.", "تصبغات معقدة، بثور نشطة ملتهبة، وهج ولمعان زيتي مع خطوط وشيخوخة مبكرة.",
        "غسول جل مهدئ بالبابونج والشوفان وخلاصة الشاي الأخضر.", "سيروم نياسيناميد المخفف بالتناوب مع لوشن الببتيدات لشد البشرة بلطف.", "لوشن ترطيب حاجز خفيف للغاية خال من الزيوت ومطهر."
    )
)

@Composable
fun BaumannQuestionnaireScreen(
    cardColor: Color,
    accentColor: Color,
    textMuted: Color,
    onReportGenerated: (SkinReport) -> Unit,
    onBack: () -> Unit
) {
    val bgDark = Color(0xFF0F172A)
    var currentDimensionIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, Int>() }
    var showResult by remember { mutableStateOf(false) }

    val currentDimension = BAUMANN_KOTLIN_DIMENSIONS[currentDimensionIndex]

    // Navigation and state calculators
    val isCurrentDimensionComplete = currentDimension.questions.all { q -> answers[q.id] != null }

    fun calculateBaumannType(): Pair<String, Map<String, Float>> {
        var code = ""
        val scores = mutableMapOf<String, Float>()
        BAUMANN_KOTLIN_DIMENSIONS.forEach { dim ->
            var sum = 0
            dim.questions.forEach { q ->
                sum += (answers[q.id] ?: 2)
            }
            val avg = sum.toFloat() / dim.questions.size.toFloat()
            scores[dim.id] = avg
            code += if (avg < dim.threshold) dim.letterLow else dim.letterHigh
        }
        return Pair(code, scores)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
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
                onClick = {
                    if (showResult) {
                        showResult = false
                    } else if (currentDimensionIndex > 0) {
                        currentDimensionIndex--
                    } else {
                        onBack()
                    }
                },
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
                    text = "مؤشر باومان لتصنيف البشرة 🩺🧪",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (showResult) "رصد النتائج السريرية" else "البعد ${currentDimensionIndex + 1} من 4",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- PROGRESS BAR ---
        LinearProgressIndicator(
            progress = { if (showResult) 1.0f else (currentDimensionIndex + 1).toFloat() / 4.0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accentColor,
            trackColor = cardColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!showResult) {
            // --- QUESTIONNAIRE FLOW ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dimension Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentDimension.emoji,
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = currentDimension.titleAr,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentDimension.tagAr,
                                    color = textMuted,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }

                // Questions items
                items(currentDimension.questions.size) { index ->
                    val question = currentDimension.questions[index]
                    val selectedValue = answers[question.id]

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardColor)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${index + 1}. ${question.textAr}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Render options stacked vertically
                        question.options.forEach { (value, text) ->
                            val isSelected = selectedValue == value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.15f))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { answers[question.id] = value }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { answers[question.id] = value },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Text(
                                    text = text,
                                    color = if (isSelected) accentColor else Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM NAVIGATION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (currentDimensionIndex > 0) {
                            currentDimensionIndex--
                        }
                    },
                    enabled = currentDimensionIndex > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardColor,
                        disabledContainerColor = cardColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("السابق ⬅️", color = Color.White, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (currentDimensionIndex < 3) {
                            currentDimensionIndex++
                        } else {
                            showResult = true
                        }
                    },
                    enabled = isCurrentDimensionComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentDimensionIndex == 3) Color(0xFF10B981) else accentColor,
                        disabledContainerColor = cardColor.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .padding(start = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentDimensionIndex == 3) "احصل على التصنيف السريري ✨" else "البعد التالي ➡️",
                        color = if (isCurrentDimensionComplete) bgDark else textMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // --- QUESTIONNAIRE RESULTS & RECOMMENDATION ---
            val (calculatedCode, dimensionScores) = calculateBaumannType()
            val rec = KOTLIN_SKIN_TYPE_RECS[calculatedCode] ?: KOTLIN_SKIN_TYPE_RECS["ORNT"]!!

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calculated Code Medal card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, accentColor, RoundedCornerShape(20.dp))
                            .testTag("baumann_result_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(40.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .border(2.dp, accentColor, RoundedCornerShape(40.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = calculatedCode,
                                    color = accentColor,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "نوع بشرتِك المكتشف رسمياً بمؤشر باومان:",
                                color = textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = rec.nameAr,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Grid of Dimension Scores
                item {
                    Text(
                        text = "معدل نتائج أبعاد باومان الأربعة (D1-D4):",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BAUMANN_KOTLIN_DIMENSIONS.forEach { dim ->
                            val score = dimensionScores[dim.id] ?: 2.5f
                            val isHigh = score >= dim.threshold
                            val letter = if (isHigh) dim.letterHigh else dim.letterLow
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = dim.id, color = textMuted, fontSize = 10.sp)
                                    Text(text = String.format("%.2f", score), color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(accentColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "$letter", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Detailed physiological feedback
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "🔬 التشريح السلوكي والحيوي لنوع بشرتكِ:",
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rec.descAr,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Strengths and weaknesses
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("💪 نقاط القوة والميزات", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rec.prosAr, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp, textAlign = TextAlign.Right, lineHeight = 14.sp)
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("⚠️ التحديات والأخطار الجزيئية", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rec.consAr, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp, textAlign = TextAlign.Right, lineHeight = 14.sp)
                            }
                        }
                    }
                }

                // Recommended customized compounding formulary prescription list
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF059669).copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "🧴 بروتوكول التركيب المعملي المقترح من GlowLogic:",
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Cleanser
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = rec.cleanser, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "1. الغسول الموصى به:", color = textMuted, fontSize = 11.sp)
                            }

                            // 2. Active component
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = rec.active, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "2. المادة الفعالة المصححة:", color = textMuted, fontSize = 11.sp)
                            }

                            // 3. Moisturizer base carrier
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = rec.moisturizer, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "3. قاعدة مرطب الحفظ والترميم:", color = textMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Bottom action buttons to save or go back
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val finalReport = SkinReport(
                            skinType = "باومان ${calculatedCode} - ${rec.nameAr}",
                            hydration = when (calculatedCode.first().toString()) {
                                "D" -> 30
                                else -> 68
                            },
                            barrierHealth = when (calculatedCode[1].toString()) {
                                "S" -> 35
                                else -> 82
                            },
                            pathology = "بناءً على تصنيف باومان الذاتي الدقيق (D1-D4)، فإن بشرتك تندرج فسيولوجياً تحت فئة ${rec.nameAr} (${calculatedCode}).\n\nأبحاث ونواتج د. زبيدة تخلص عيوب النسيج: ${rec.descAr}",
                            routineAM = "1. غسول معتمد لباومان: ${rec.cleanser}\n2. سيروم التوحيد السطحي: ${rec.active}\n3. واقي شمس فيزيائي 100% Zinc Oxide SPF 50+.",
                            routinePM = "1. الغسول المزدوج الطبي المعتمد.\n2. علاج التركيب الفعال: ${rec.active}\n3. طبقة ترطيب الحفظ بالسيراميد: ${rec.moisturizer}",
                            avoid = "تجنب أي منتج كيميائي يسبب تحسس لـ ${calculatedCode[1]} وتفادي الشمس لـ ${calculatedCode[3]}.\n\nملاحظة الأخصائية: ${rec.consAr}",
                            isDemo = false
                        )
                        onReportGenerated(finalReport)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ التقرير الطبي وتعديل الروتين 💾", color = bgDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        // Reset
                        answers.clear()
                        currentDimensionIndex = 0
                        showResult = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إعادة الاختبار 🔄", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
