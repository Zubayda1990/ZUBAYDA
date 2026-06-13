import React, { useState } from 'react';

// ============================================================================
// Baumann Skin Type Indicator (D1 - D4) Interactive Questionnaire
// Implemented as a polished, interactive React component for GlowLogic AI Lab
// ============================================================================

interface Question {
  id: string;
  textAr: string;
  textEn: string;
  options: {
    value: number;
    textAr: string;
    textEn: string;
  }[];
}

interface Dimension {
  id: 'D1' | 'D2' | 'D3' | 'D4';
  titleAr: string;
  titleEn: string;
  taglineAr: string;
  taglineEn: string;
  emoji: string;
  questions: Question[];
  letterLow: string; // e.g., 'D' (Dry)
  letterHigh: string; // e.g., 'O' (Oily)
  threshold: number; // average score threshold (typically 2.5)
}

const BAUMANN_DIMENSIONS: Dimension[] = [
  {
    id: 'D1',
    titleAr: "البعد الأول: الجافة مقابل الدهنية (Dry vs. Oily)",
    titleEn: "Dimension 1: Dry vs. Oily",
    taglineAr: "يقيس هذا البعد إنتاج الغدد الدهنية وقدرة الحاجز المائي للجلد على حفظ الترطيب.",
    taglineEn: "Measures sebum production and the skin's moisture barrier capacity.",
    emoji: "💧",
    letterLow: "D", // Dry
    letterHigh: "O", // Oily
    threshold: 2.5,
    questions: [
      {
        id: 'D1_Q1',
        textAr: "بعد غسل وجهك بالمنظف (صابون أو غسول رغوي)، كيف تشعر بشرتك بعد 15-30 دقيقة؟",
        textEn: "After washing your face, how does your skin feel after 15-30 minutes if you don't apply any product?",
        options: [
          { value: 1, textAr: "شد شديد، خشنة للغاية وجافة", textEn: "Very tight, extremely dry, or flaky" },
          { value: 2, textAr: "شد خفيف ولكن ملمسها غير رطب", textEn: "Slightly tight but not completely dry" },
          { value: 3, textAr: "مريحة ومتوازنة دون دهون فائضة", textEn: "Comfortable and balanced, with no excess oil" },
          { value: 4, textAr: "لامعة وملمسها دهني وزيتي بوضوح", textEn: "Shiny, slippery, and clearly oily" }
        ]
      },
      {
        id: 'D1_Q2',
        textAr: "كيف تبدو بشرتك في الصور الفوتوغرافية العادية دون استخدام فلاتر؟",
        textEn: "How does your skin look in close-up photographs without filters?",
        options: [
          { value: 1, textAr: "باهتة، جافة وقشرية أحياناً", textEn: "Dull, dry, or with visible dry patches" },
          { value: 2, textAr: "مات (مطفأة) ولكنها ناعمة ومتناسقة", textEn: "Matte, normal and uniform" },
          { value: 3, textAr: "لمعان خفيف ومنعش في منطقة الجبين والأنف", textEn: "Slight dewiness especially in the T-Zone" },
          { value: 4, textAr: "لامعة بريقاً زيتيًا قويًا في كامل الوجه", textEn: "Very shiny, oily-looking over the entire face" }
        ]
      },
      {
        id: 'D1_Q3',
        textAr: "ما هو معدل تعرضك لتقشر الجلد الخلوي أو الجزيئات البيضاء المتطايرة على وجهك؟",
        textEn: "How often do you notice skin flaking or small white scales on your face?",
        options: [
          { value: 1, textAr: "دائماً وبصورة يومية تقريباً", textEn: "Always or almost daily" },
          { value: 2, textAr: "أحياناً في الطقس البارد أو الجاف", textEn: "Sometimes in dry or cold weather" },
          { value: 3, textAr: "نادراً جداً وفي مناطق ضيقة للغاية", textEn: "Rarely and in very localized areas" },
          { value: 4, textAr: "مستحيل، بشرتي دهنية تماماً ولا تتقشر أبداً", textEn: "Never; my skin is oily and doesn't flake" }
        ]
      },
      {
        id: 'D1_Q4',
        textAr: "بعد مرور ساعتين من وضع كريم مرطب عادي، كيف تبدو بشرتك؟",
        textEn: "Two hours after applying a standard moisturizer, how does your skin feel?",
        options: [
          { value: 1, textAr: "تتشربه تماماً وتعود تشعر بالجفاف والعطش", textEn: "Sucked it all up; feels dry and tight again" },
          { value: 2, textAr: "ناعمة ومريحة ومحمية جيداً", textEn: "Smooth, comfortable, and well protected" },
          { value: 3, textAr: "لمعان طفيف وبداية تكون إفرازات زهمية", textEn: "Slightly shiny and showing some sebum" },
          { value: 4, textAr: "زيتية للغاية وثقيلة مع رغبة في غسل الوجه", textEn: "Very oily, heavy, or sticky" }
        ]
      }
    ]
  },
  {
    id: 'D2',
    titleAr: "البعد الثاني: الحساسة مقابل المقاومة (Sensitive vs. Resistant)",
    titleEn: "Dimension 2: Sensitive vs. Resistant",
    taglineAr: "يقيس هذا البعد التهاب الجلد المجهري، الاستجابة التحسسية وقوة مناعة الجلد.",
    taglineEn: "Measures subclinical inflammation, redness, allergic response and skin defense.",
    emoji: "🛡️",
    letterLow: "R", // Resistant
    letterHigh: "S", // Sensitive
    threshold: 2.5,
    questions: [
      {
        id: 'D2_Q1',
        textAr: "ما مدى تكرار إصابتك بالاحمرار، الحكة أو الوخز عند تجربة منتج skincare جديد للوجه؟",
        textEn: "How often do you experience redness, itching, or stinging when trying a new skincare product?",
        options: [
          { value: 1, textAr: "لا يحدث أبداً، بشرتي تحتمل أي منتج", textEn: "Never; my skin tolerates anything" },
          { value: 2, textAr: "نادراً جداً ومع منتجات مخصصة قوية", textEn: "Rarely, only with highly active formulas" },
          { value: 3, textAr: "أحياناً، لاسيما مع العطور أو الكحول الكيميائي", textEn: "Sometimes, especially with fragrance or alcohol" },
          { value: 4, textAr: "غالباً ودائماً، بشرتي مفرطة التهيج", textEn: "Frequently; my skin is extremely reactive" }
        ]
      },
      {
        id: 'D2_Q2',
        textAr: "هل تم تشخيصك سريرياً من قبل بأمراض الوردية، الأكزيما، أو التهاب الجلد التماسي؟",
        textEn: "Have you ever been clinically diagnosed with rosacea, eczema, or contact dermatitis?",
        options: [
          { value: 1, textAr: "لا، إطلاقاً وبشرتي صحية للغاية", textEn: "No, never" },
          { value: 2, textAr: "أعتقد ذلك ولكن ليس تشخيصاً مؤكداً", textEn: "I suspects so, but not confirmed" },
          { value: 3, textAr: "نعم، حالة خفيفة تتم السيطرة عليها", textEn: "Yes, a mild controlled case" },
          { value: 4, textAr: "نعم وبصورة مزمنة ومزعجة ومستمرة", textEn: "Yes, a chronic active medical condition" }
        ]
      },
      {
        id: 'D2_Q3',
        textAr: "عند ممارسة الرياضة، التعرض للحرارة، أو أثناء المواقف الحرجة، هل يتوهج وجهك باللون الأحمر؟",
        textEn: "During exercise, exposure to heat/sun, or stressful moments, does your face flush red?",
        options: [
          { value: 1, textAr: "أبداً، يحافظ على لونه العادي والطبيعي", textEn: "Never; stays normal tone" },
          { value: 2, textAr: "نادراً ما يتوهج وتزول زهرته بسرعة", textEn: "Rarely; calms down very quickly" },
          { value: 3, textAr: "أحياناً يورد ويتوهج لبعض الوقت", textEn: "Sometimes flushes and stays warm" },
          { value: 4, textAr: "دائماً وبقوة، وجهي يتحول لكتلة من اللهب والأحمر", textEn: "Always; turns bright red and burns" }
        ]
      },
      {
        id: 'D2_Q4',
        textAr: "كم عدد البثور أو الحبوب الحمراء الملتهبة التي تظهر على وجهك شهرياً؟",
        textEn: "How many inflammatory pimples or red bumps do you get in an average month?",
        options: [
          { value: 1, textAr: "صفر إلى حبة واحدة على الأكثر", textEn: "0-1 minor spots" },
          { value: 2, textAr: "من 2 إلى 5 حبات صغيرة", textEn: "2-5 mild spots" },
          { value: 3, textAr: "من 6 إلى 10 حبات ملتهبة ومؤلمة", textEn: "6-10 active bumps" },
          { value: 4, textAr: "أكثر من 10 حبات ومعظمها كيسي عميق", textEn: "More than 10 or cystic lesions" }
        ]
      }
    ]
  },
  {
    id: 'D3',
    titleAr: "البعد الثالث: التصبغية مقابل غير التصبغية (Pigmented vs. Non-pigmented)",
    titleEn: "Dimension 3: Pigmented vs. Non-pigmented",
    taglineAr: "يقيس هذا البعد نشاط الخلايا الميلانينية ومستوى القابلية لظهور البقع الداكنة والكلف.",
    taglineEn: "Measures melanocyte activity and susceptibility to hyperpigmentation and dark spots.",
    emoji: "☀️",
    letterLow: "N", // Non-pigmented
    letterHigh: "P", // Pigmented
    threshold: 2.5,
    questions: [
      {
        id: 'D3_Q1',
        textAr: "عند إصابتك بحب الشباب أو الجروح، هل تترك أثراً داكناً أو بقعة بنية بعد التعافي؟",
        textEn: "When you get a pimple or cut, does it leave a lingering dark brown or black spot after healing?",
        options: [
          { value: 1, textAr: "لا تترك أي أثر إطلاقاً، تتعافى وتختفي", textEn: "Never; skin heals clean" },
          { value: 2, textAr: "نادراً وتترك علامة زهرية تزول بسرعة", textEn: "Rarely; leaves a faint pink spot" },
          { value: 3, textAr: "غالباً تترك بقعاً داكنة تظل لعدة أسابيع", textEn: "Often; leaves brown spots that last weeks" },
          { value: 4, textAr: "دائماً، أي خدش بسيط يترك تصبغاً دائماً", textEn: "Always; leaves strong dark hyperpigmentation" }
        ]
      },
      {
        id: 'D3_Q2',
        textAr: "هل تعاني من النمش أو بقع الشمس الداكنة على وجهك أو كتفيك؟",
        textEn: "Do you have freckles or dark sunspots on your face and shoulders?",
        options: [
          { value: 1, textAr: "لا يوجد لدي أي نمش أو كلف إطلاقاً", textEn: "None at all" },
          { value: 2, textAr: "حبات قليلة مجهرية وغير واضحة", textEn: "A few tiny, unnoticeable ones" },
          { value: 3, textAr: "نمش بني فاتح متوزع بوضوح على الخدين", textEn: "Moderately visible freckles on cheeks" },
          { value: 4, textAr: "كثير جداً، وبقع شمسية داكنة وجسمية واضحة", textEn: "Many deep spots, sunburn freckles, or patchiness" }
        ]
      },
      {
        id: 'D3_Q3',
        textAr: "كيف تصف استجابة بشرتك للتسمير وصعود صبغة الميلانين عند التعرض المباشر للشمس؟",
        textEn: "How does your skin react when exposed directly to the sun for an hour?",
        options: [
          { value: 1, textAr: "تحترق وتتقشر فوراً ولا يتغير لونها للأسمر", textEn: "Always burns and peels, never tans" },
          { value: 2, textAr: "تحترق قليلاً ثم تكتسب بعض التسمير الخفيف", textEn: "Burns slightly first, then tans mildly" },
          { value: 3, textAr: "نادراً ما تحترق وتسمر بسهولة", textEn: "Rarely burns, tans very easily" },
          { value: 4, textAr: "تسمر وتغمق بشكل فوري وعميق دون أي احتراق", textEn: "Never burns, gets deeply bronzed/dark immediately" }
        ]
      },
      {
        id: 'D3_Q4',
        textAr: "هل سبق لك المعاناة من الكلف البني (بقع الحمل أو التغيرات الجرمية العميقة)؟",
        textEn: "Have you ever suffered from melasma, dark hormonal patches, or large brown patches?",
        options: [
          { value: 1, textAr: "أبداً، ولم أعاني من هذا طيلة حياتي", textEn: "Never, not even slightly" },
          { value: 2, textAr: "تصبغ خفيف جداً يظهر في الصيف ويختفي", textEn: "Very mild, comes in summer and fades" },
          { value: 3, textAr: "نعم، توجد بقع كلف واضحة فوق الشفاه أو على الخدين", textEn: "Yes, prominent patches on cheeks or upper lip" },
          { value: 4, textAr: "نعم، كلف شديد وعام وتصبغات منتشرة وعصية على العلاج", textEn: "Severe, persistent melasma affecting large areas" }
        ]
      }
    ]
  },
  {
    id: 'D4',
    titleAr: "البعد الرابع: التجاعيد مقابل شد البشرة (Wrinkle-prone vs. Tight)",
    titleEn: "Dimension 4: Wrinkle-prone vs. Tight",
    taglineAr: "يقيس هذا البعد استهلاك الإيلاستين والكولاجين وقابلية شيخوخة الخلايا وعوامل الأكسدة.",
    taglineEn: "Measures collagen loss, predisposition to premature aging, and oxidative stress.",
    emoji: "⏳",
    letterLow: "T", // Tight
    letterHigh: "W", // Wrinkle-prone
    threshold: 2.5,
    questions: [
      {
        id: 'D4_Q1',
        textAr: "عند النظر إلى وجهك في المرآة بدون إثارة تعابير، هل ترى خطوطاً دقيقة أو تجاعيد؟",
        textEn: "When your face is completely relaxed, do you see fine lines or wrinkles?",
        options: [
          { value: 1, textAr: "لا يوجد أي خط إطلاقاً، ناعمة ومشدودة كلياً", textEn: "No lines at all, perfectly tight" },
          { value: 2, textAr: "خطوط خفيفة مجهرية حول العين تحت ضوء ساطع", textEn: "Very faint fine lines visible only under bright light" },
          { value: 3, textAr: "توجد تجاعيد تعبيرية لطيفة تظل ثابتة في الهدوء", textEn: "Visible smile lines or faint forehead expression lines" },
          { value: 4, textAr: "نعم، تجاعيد واضحة وعميقة مرئية دون تعابير", textEn: "Yes, deep static wrinkles visible without expressions" }
        ]
      },
      {
        id: 'D4_Q2',
        textAr: "ما هو معدل تعرضك المباشر لأشعة الشمس أو التردد على أسرة التسمير الاصطناعي؟",
        textEn: "How much sun exposure or tanning bed use have you had in your life?",
        options: [
          { value: 1, textAr: "ضئيل للغاية، أعيش بالداخل وألتزم بواقي الشمس", textEn: "Very little; I stay indoors & use daily sunscreen" },
          { value: 2, textAr: "معتدل، أخرج للشمس بعقلانية وأتجنب أوقات الذروة", textEn: "Moderate; limited outdoors and occasionally sunscreen" },
          { value: 3, textAr: "مرتفع، أمارس نشاطات خارجية يومية وغالباً أنسى SPF", textEn: "High; outdoors frequently, rarely use sunscreen" },
          { value: 4, textAr: "شديد للغاية، أسافر وأسمر والتعرض للشمس جزء من نمط حياتي", textEn: "Extremely high; sunburns are common or regular sunbather" }
        ]
      },
      {
        id: 'D4_Q3',
        textAr: "بالتطلع إلى والديك وأشقائك، هل تبدو بشرتهم أصغر أم أكبر من عمرهم الفعلي؟",
        textEn: "Looking closely at your biological parents and siblings, do they look older/wrinklier?",
        options: [
          { value: 1, textAr: "يبدون أصغر بـ 5 إلى 10 سنوات (جينات ممتازة ومشدودة)", textEn: "Look 5-10 years younger (great density/minimal lines)" },
          { value: 2, textAr: "يبدون متوافقين تماماً مع عمرهم الحقيقي بلياقة", textEn: "Look exactly their age with balanced aging" },
          { value: 3, textAr: "يبدون أكبر قليلاً ولديهم تجاعيد مبكرة تعبيرية", textEn: "Look slightly older, with early sagging or wrinkling" },
          { value: 4, textAr: "يبدون أكبر بكثير وتجاعيد غائرة وجينات متهدلة سريعة", textEn: "Look significantly older with severe sagging & wrinkles" }
        ]
      },
      {
        id: 'D4_Q4',
        textAr: "هل تدخن السجائر، الشيشة، أو تعيش بانتظام مع مدخن في مكان مغلق؟",
        textEn: "Do you smoke, vape, or regularly live/work with active smokers?",
        options: [
          { value: 1, textAr: "أبداً، أعيش في بيئة نقية ومغلقة وصحية 100%", textEn: "Never; smoke-free environment" },
          { value: 2, textAr: "دخنت لفترة قصيرة في الماضي وتوقفت تماماً", textEn: "Did in the past, but quit long ago" },
          { value: 3, textAr: "مدخنة سلبية (أعيش مع مدخن أو حولي بالعمل)", textEn: "Passive smoker (live or work with active smokers)" },
          { value: 4, textAr: "نعم، مدخنة نشطة وبصورة يومية منتظمة", textEn: "Yes, active daily smoker" }
        ]
      }
    ]
  }
];

// Definition of 16 Baumann Skin Types and Recommendations
interface SkinTypeDetail {
  code: string;
  nameAr: string;
  nameEn: string;
  descAr: string;
  descEn: string;
  prosAr: string;
  consAr: string;
  cleanser: string;
  active: string;
  moisturizer: string;
}

const SKIN_TYPE_RECS: Record<string, SkinTypeDetail> = {
  DRNT: {
    code: "DRNT",
    nameAr: "الجافة، المقاومة، غير التصبغية، المشدودة",
    nameEn: "Dry, Resistant, Non-pigmented, Tight",
    descAr: "تتمتع بشرتك بمناعة نسيجية عالية وخلو من التصبغات والتجاعيد، لكنها تفتقر للغشاء الدهني المغذي للطبقة القرنية (Stratum Corneum). عائقك الأكبر هو الجفاف والحد من فقدان الماء المجهري (TEWL).",
    descEn: "Your skin enjoys resilient barrier defenses, no spots, and no wrinkles, but lacks natural sebum oil to protect the stratum corneum. The key issue is hydration.",
    prosAr: "نادرة التحسس، نقية بيئياً ومشدودة للغاية.",
    consAr: "عطش جلدي، تشققات مجهرية وخشونة بالملمس.",
    cleanser: "منظف حليبي غير رغوي غني بالسكوالين الشوفاني.",
    active: "سيروم حمض الهيالورونيك 2% لربط الهيدروجين.",
    moisturizer: "كريم حاجز غني بالسيراميد Lipids الثلاثي الفاخر."
  },
  DRNW: {
    code: "DRNW",
    nameAr: "الجافة، المقاومة، غير التصبغية، المعرضة للتجاعيد",
    nameEn: "Dry, Resistant, Non-pigmented, Wrinkle-prone",
    descAr: "تفتقر بشرتك للدهون الطبيعية مما يزيد من سرعة تبخر الماء وجفاف الملاط الخلوي، وهو ما يمهد مع الزمن لانهيار الكولاجين وظهور الخطوط التعبيرية المبكرة. الحاجة تكمن في مضادات الأكسدة المكثفة.",
    descEn: "Dryness accelerates collagen depletion. Highly resistant to inflammation but prone to expression fine lines. Requires rich emollients and retinoids.",
    prosAr: "نقية من البقع والتصبغات، وتتحمل المكونات المقاومة للتجاعيد بيسر.",
    consAr: "خطوط حول العين وجفاف مطبق نهاراً.",
    cleanser: "غسول زيتي مغذي للبشرة.",
    active: "ريتينول 0.5% مع باكوتشيول لتحفيز الفيبروبلاست.",
    moisturizer: "مرطب مغذي عميق غني بزبدة الشيا والببتيدات المغلفة."
  },
  DRPT: {
    code: "DRPT",
    nameAr: "الجافة، المقاومة، التصبغية، المشدودة",
    nameEn: "Dry, Resistant, Pigment-prone, Tight",
    descAr: "تتميز بشرتك بالسلامة من الخطوط والشيخوخة المبكرة، ولكن الخلل في توزيع صبغة الميلانين يسبب بقع كلف داكنة وبهتان، تزداد وضوحاً بسبب الجفاف المزمن الذي يفقد السطح بريقه الطبيعي.",
    descEn: "Wrinkle-free but prone to dark spots and melasma. Dryness prevents natural skin radiance by slowing down healthy cell turnover.",
    prosAr: "مقاومة للمواد الكيميائية الفعالة، مشدودة ومحمية الكولاجين.",
    consAr: "بقع كلف داكنة وبهتان مع قوام جاف.",
    cleanser: "منظف لطيف جداً بالمرطبات الكولاجينية.",
    active: "حمض الكوجيك مع فيتامين C لتعطيل التيروزيناز.",
    moisturizer: "مرطب تفتيح مائي غني بألفا أربوتين ونياسيناميد مهدئ."
  },
  DRPW: {
    code: "DRPW",
    nameAr: "الجافة، المقاومة، التصبغية، المعرضة للتجاعيد",
    nameEn: "Dry, Resistant, Pigment-prone, Wrinkle-prone",
    descAr: "أحد أكثر الأنواع تطلباً للبروتوكولات الشاملة. بشرتك جافة وتظهر عليها البقع والتجاعيد معاً بفعل أشعة الشمس والتلف الخلوي المتراكم. تحتاجي جدار ترطيب متكامل مع مبيضات آمنة ومحفزات كولاجينية.",
    descEn: "A high-demand skin type needing integrated protocols. Prone to dry patches, sun pigmentation, and fine wrinkles. Needs dual-function hydration & cell renewal.",
    prosAr: "تتقبل المكونات النشطة القوية (أحماض وتقشير) دون تهيج كبير.",
    consAr: "بهتان لوني، كلف متعدد وتجاعيد مبكرة واضحة.",
    cleanser: "منظف زيتي غني بمضادات الأكسدة.",
    active: "سيروم النياسيناميد 10% مدمجاً مع الريتينول السلس.",
    moisturizer: "كريم ليلي دهني بالسيراميد وفيتامين E وحمض اللاكتيك."
  },
  DSNT: {
    code: "DSNT",
    nameAr: "الجافة، الحساسة، غير التصبغية، المشدودة",
    nameEn: "Dry, Sensitive, Non-pigmented, Tight",
    descAr: "تعاني بشرتك من ضعف شديد في جدار الحماية الطبيعي، مما يجعلها جافة على الدوام وفي حالة تهيج واحمرار مستمر لأقل ملامسة بيئية. لحسن الحظ تظل مشدودة ونقية من البقع الداكنة. الأولوية للتهدئة والترميم.",
    descEn: "Compromised skin barrier with severe dryness and reactivity. Easily flushes but remains free of spots and wrinkles. Focus on calming and barrier reconstruction.",
    prosAr: "نقاء تام من التصبغات ومشدودة وجينات كولاجين قوية.",
    consAr: "حرقان وخلفية وردية مستمرة مع قشور تهيج.",
    cleanser: "منظف حليبي حمضي متوازن pH 5.5 خالي من العطور.",
    active: "سيروم خلاصة السيكا والزنك والبانثينول المرمم.",
    moisturizer: "مرطب طبي ثقيل خال من المواد الحافظة المهيجة."
  },
  DSNW: {
    code: "DSNW",
    nameAr: "الجافة، الحساسة، غير التصبغية، المعرضة للتجاعيد",
    nameEn: "Dry, Sensitive, Non-pigmented, Wrinkle-prone",
    descAr: "يتآمر الجفاف والحساسية لتسريع تلف الخلايا السطحية والألياف العميقة، مما يعرض بشرتك للخطوط ومظهر الشيخوخة المبكرة مع نوبات من الاحمرار والوخز. العناية تتطلب تلطيفاً أولاً ثم تدعيم الكولاجين.",
    descEn: "A delicate balance of rebuilding barrier strength while applying gentle non-irritating anti-aging actives (like peptides) to delay wrinkles.",
    prosAr: "خالية من الكلف والبقع الملونة العنيدة.",
    consAr: "خطوط رفيعة، جفاف مطبق وحرقان متكرر.",
    cleanser: "منظف لطيف جداً بالبابونج والصبار الطبيعي.",
    active: "ببتيدات النحاس المخففة مع ريزفيراترول مضاد أكسدة.",
    moisturizer: "كريم سيراميد ثري ومطعم بالصبار وحمض الخليك اللطيف."
  },
  DSPT: {
    code: "DSPT",
    nameAr: "الجافة، الحساسة، التصبغية، المشدودة",
    nameEn: "Dry, Sensitive, Pigment-prone, Tight",
    descAr: "تجمع بشرتك بين الحساسية العالية والنزوع التصبغي القوي؛ أي تهيج أو احمرار بسيط يثير فوراً الخلايا الصباغية ويتحول لبقع داكنة وكلف. لا يمكنك استخدام مقشرات عنيفة وتتطلب مبيضات مهدئة.",
    descEn: "Skin sensitivity directly triggers pigmentation (PIH - post-inflammatory hyperpigmentation). Harsh bleaching agents are forbidden. Calm to prevent spots.",
    prosAr: "مشدودة وغير معرضة للترهل مبكراً.",
    consAr: "تصبغات تالية للالتهاب، احمرار، بهتان وجفاف متعب.",
    cleanser: "غسول كريمي مهدئ غني بالشوفان الغروي.",
    active: "سيروم نياسيناميد 5% وسيروم مائي بحمض الترانيكساميك.",
    moisturizer: "كريم مرطب بالهيالورونيك وخلاصة نبات العرقسوس المهدئ البارد."
  },
  DSPW: {
    code: "DSPW",
    nameAr: "الجافة، الحساسة، التصبغية، المعرضة للتجاعيد",
    nameEn: "Dry, Sensitive, Pigment-prone, Wrinkle-prone",
    descAr: "أكثر أنواع البشرة رقة وتطلباً للبرمجيات السريرية عالية الاتساق. تتنافس عوامل التهيج والتلف البقعي وتكسر الكولاجين، تحت سطوة جفاف مطلق. البروتوكول يبدأ بالترطيب والمهدئات وواقي شمس معدني صارم.",
    descEn: "The most delicate Baumann skin type. Experiencing dryness, hyperreactivity, dark marks, and skin thinning. Prioritize barrier recovery, mineral SPF 50, and safe botanicals.",
    prosAr: "تستجيب سريعاً للبروتوكولات الشاملة والمصممة بدقة.",
    consAr: "احمرار، بقع كلف غائرة، فقدان مرونة وجفاف شامل.",
    cleanser: "زيت منظف غني بمستخلص الصبار والشاي الأخضر.",
    active: "ببتيدات مغلفة ومثبتة مع النياسيناميد والأربوتين اللطيف.",
    moisturizer: "مرطب غني كالحرير مصنع بتقنية الدهون المستحلبة الطبيعية."
  },
  ORNT: {
    code: "ORNT",
    nameAr: "الدهنية، المقاومة، غير التصبغية، المشدودة",
    nameEn: "Oily, Resistant, Non-pigmented, Tight",
    descAr: "البشرة المثالية أو الأقرب وراثياً للمثالية الاستقرارية. إفراز دهني وفير يحميك كليا من التجاعيد والجفاف، وجدار حاد يحميك من الحساسية والمشاكل، ونقاء لوني رائع. التركيز يقتصر على تنظيف المسام.",
    descEn: "The 'bulletproof' skin type. Highly hydrated and naturally protected from signs of aging. Prone only to shiny T-Zones and enlarged pores. Focus on sebum modulation.",
    prosAr: "شباب دائم، مقاومة تامة ولا تتهيج ولا تتبقع أبداً.",
    consAr: "مسام واسعة قليلاً، لمعان زيتي متكرر ونادراً رؤوس سوداء.",
    cleanser: "غسول رغوي بحمض الساليسيليك Salicylic Acid 2%.",
    active: "نياسيناميد 10% لتنظيم الزهم وتقليص الأقطار المسامية.",
    moisturizer: "لوشن هلامي (جل كهرومغناطيسي) خفيف جداً غني بالزنك PCA."
  },
  ORNW: {
    code: "ORNW",
    nameAr: "الدهنية، المقاومة، غير التصبغية، المعرضة للتجاعيد",
    nameEn: "Oily, Resistant, Non-pigmented, Wrinkle-prone",
    descAr: "تتمتع بشرتك بإفرازات دهنية تحارب الجفاف، ولكن مع زوال الكولاجين بسبب الشمس أو الجينات يبدأ ظهور التجاعيد. تتقبل بشرتك المكونات المقاومة للتجاعيد القوية جداً بكفاءة عالية ومن دون قلق.",
    descEn: "Oily but wrinkle-prone. Highly robust skin barrier that easily tolerates powerful anti-aging treatments. Needs high-strength retinoids and exfoliators.",
    prosAr: "تتحمل أحماض الـ AHA والريتينول العالي بدون أي احمرار أو حكة.",
    consAr: "لمعان دهني مع خطوط جبهية واضحة ومسامات واسعة.",
    cleanser: "غسول جل برغوة منشطة ومجددة لخلايا الوجه.",
    active: "ريتينول سريري بتركيز 1% متبوعاً بمضادات الأكسدة.",
    moisturizer: "لوشن ليلي خفيف ومائي غني بحمض الجليكوليك ومركب الببتيد."
  },
  ORPT: {
    code: "ORPT",
    nameAr: "الدهنية، المقاومة، التصبغية، المشدودة",
    nameEn: "Oily, Resistant, Pigment-prone, Tight",
    descAr: "بشرة شابة مقاومة للخطوط والشيخوخة بفعل الإفرازات الشحمية، لكنها تعاني من بقع الشمس، النمش أو تلون خلايا الجلد في مناطق معينة. تتقابل المبيضات القوية لتقشير الميلانين وتوحيد السطح.",
    descEn: "Youthful and fully hydrated with high sebum output, but prone to localized sun patches and uneven tone. Robust barrier tolerates advanced brightening serums.",
    prosAr: "مشدودة، ريان بالشباب، ومحمية من الترهلات والتلف الهيكلي.",
    consAr: "علامات داكنة واضحة ومسامات دهنية نشطة طوال النهار.",
    cleanser: "غسول مقشر لطيف بحمض الساليسيليك والجليكوليد.",
    active: "سيروم فيتامين سي النقي 15% بالتآزر مع حمض الترانيكساميك.",
    moisturizer: "مرطب غير كوميدوغينيك (لوشن خفيف للغاية) تفتيحي."
  },
  ORPW: {
    code: "ORPW",
    nameAr: "الدهنية، المقاومة، التصبغية، المعرضة للتجاعيد",
    nameEn: "Oily, Resistant, Pigment-prone, Wrinkle-prone",
    descAr: "بشرتك دهنية ومقاومة للتحسس ولكنها مستهدفة بالتصبغات والخطوط التعبيرية معاً. الجدار الطبيعي السميك يتيح لك استخدام أقوى صيحات التجديد الخلوي والتقشير الكيميائي لتسوية اللون وتعزيز الساق الفايبري.",
    descEn: "Oily but aging and pigmented. Thick skin barrier allows the combined use of high-strength exfoliators, retinol, and tyrosinase inhibitors safely.",
    prosAr: "تتحمل التركيبات الطبية المركزة والمعقدة دون تهيج يذكر.",
    consAr: "مسام واسعة، بهتان دهني كالح، نتوءات صبغية خطية وتجاعيد.",
    cleanser: "منظف منشط رغوي يومي مقشر مزدوج.",
    active: "سيروم الريتينول السريري بالترافق مع سيروم الأربوتين وفيتامين سي.",
    moisturizer: "كريم مائي لتنظيم خلايا وتفتيح النسيج (Oil-free Lotion)."
  },
  OSNT: {
    code: "OSNT",
    nameAr: "الدهنية، الحساسة، غير التصبغية، المشدودة",
    nameEn: "Oily, Sensitive, Non-pigmented, Tight",
    descAr: "تجمع بشرتك بين إنتاج الدهون المرتفع والتهيج السطحي المتكرر. تظهر الحبوب والتهابات حب الشباب والمسام لاسيما في البيئات الحارة، ولكن خلو جيناتك من نزعة التصبغ والكلف يحميكِ من الآثار البنية الدائمة.",
    descEn: "Oily and reactive. Prone to shiny flare-ups, redness, and acne breakouts. Good news: inflammation rarely leaves permanent brown spots, and wrinkles are non-existent.",
    prosAr: "بشرة شابة وجينات كولاجين ممتازة، لا بقع داكنة بعد زوال الحبوب.",
    consAr: "حبوب ملتهبة، احمرار شديد عند تفعيل الإنزيمات وظهور زهمي مزعج.",
    cleanser: "غسول رغوي لطيف للغاية حموضة 5.5 يحتوي على حمض الساليسيليك المخفف.",
    active: "سيروم نياسيناميد 10% بخصائصه المضادة للالتهاب والمقوضة لزهم الحبوب.",
    moisturizer: "هلام مائي سريع الامتصاص غني بخلاصة السيكا والشاي الأخضر المطفأ."
  },
  OSNW: {
    code: "OSNW",
    nameAr: "الدهنية، الحساسة، غير التصبغية، المعرضة للتجاعيد",
    nameEn: "Oily, Sensitive, Non-pigmented, Wrinkle-prone",
    descAr: "بشرة دهنية وحساسة ومعرضة للتجاعيد في آن واحد. يساهم التهيج والالتهاب المستمر للبثور والدهون النشطة في تسريع تلف الإيلاستين المجهري وظهور علامات العمر. يجب معالجة الالتهاب بلطف أولاً.",
    descEn: "Continuous inflammation from skin sensitivity and acne accelerates early sagging and dynamic wrinkles around the eyes and forehead. Balance first.",
    prosAr: "محمية من الكلف المزمن بنسبة كبيرة.",
    consAr: "لمعان مفرط، بثور حمراء دورية وتجاعيد رقيقة بفعل جفاف الطبقة السطحية.",
    cleanser: "منظف جل هلامي مهدئ غني بمستخلص الصبار والألوفيرا.",
    active: "سيروم ببتيدات مجدد للبشرة مع لوشن النياسيناميد اللطيف.",
    moisturizer: "مرطب هيدروجيني مائي وخفيف خال تماماً من الزيوت الثقيلة."
  },
  OSPT: {
    code: "OSPT",
    nameAr: "الدهنية، الحساسة، التصبغية، المشدودة",
    nameEn: "Oily, Sensitive, Pigment-prone, Tight",
    descAr: "النوع الكلاسيكي المعرض لحب الشباب المصحوب بالبقع البنية. تفرز بشرتك دهوناً زائدة وتصطدم باحمرار وحساسية، مما ينتج بثوراً حمراء تتحول تلقائياً بعد الشفاء لبقع بنية غامقة (تصبغات PIH). المشاكل بحاجة لعلاج هادئ.",
    descEn: "The typical 'acne combined with hyperpigmentation' type. Any inflamed spot turns into a severe dark brown scar. Clear skin carefully and keep inflammation at zero.",
    prosAr: "مشدودة النسيج وتدوم طويلاً بمظهر العشرينيات ومقاومة للترهل.",
    consAr: "بقع سوداء مبعثرة، آثار حبوب وتوهج دهني مرن.",
    cleanser: "منظف رغوي مهدئ ومطهر بالزنك والشوفان.",
    active: "سيروم نياسيناميد 10% متبوعاً بألفا أربوتين وسيروم حمض الساليسيليك 1% اللطيفة.",
    moisturizer: "جل ترطيب غامق بالبانثينول مائي غير زيتي على الإطلاق."
  },
  OSPW: {
    code: "OSPW",
    nameAr: "الدهنية، الحساسة، التصبغية، المعرضة للتجاعيد",
    nameEn: "Oily, Sensitive, Pigment-prone, Wrinkle-prone",
    descAr: "بشرة تتميز بكل التحديات الجلدية: زيادة في الدهون والبثور، التهابات واحمرار متواصل، بقع داكنة تالية لحب الشباب، وخطوط تعبيرية غائرة مبكرة بفعل الأكسدة. يتطلب بروتوكولاً مخصصاً للغاية يوازن التهدئة مع التفتيح والدعم العصبي.",
    descEn: "Experiencing all major skin complaints: sebum overproduction, hyper-reactivity, post-inflammatory scarring, and fine lines. Calming, brightening, and sunscreen are absolute keys.",
    prosAr: "تستفيد جداً عندما يتم إرساء نظام متوازن وخالٍ من المغامرات التجميلية.",
    consAr: "تصبغات عنيدة، حبوب مستثارة، وهج دهني، وتجاعيد في نفس اللحظة.",
    cleanser: "غسول جل معقم ومهدئ بمستخلص البابونج والشاي الأخضر.",
    active: "سيروم النياسيناميد بالتناوب مع مركب الببتيد اللطيف، وتجنب الريتينول العالي.",
    moisturizer: "لوشن مرطب حاجز خفيف وخالي من الدهون والعطور والبارابين."
  }
};

export const BaumannSkinTypeQuestionnaire: React.FC = () => {
  const [lang, setLang] = useState<'ar' | 'en'>('ar');
  const [currentDimensionIndex, setCurrentDimensionIndex] = useState<number>(0);
  const [answers, setAnswers] = useState<Record<string, number>>({});
  const [showResult, setShowResult] = useState<boolean>(false);

  const currentDimension = BAUMANN_DIMENSIONS[currentDimensionIndex];

  const handleSelectOption = (questionId: string, value: number) => {
    setAnswers(prev => ({
      ...prev,
      [questionId]: value
    }));
  };

  const isDimensionAnswered = () => {
    return currentDimension.questions.every(q => answers[q.id] !== undefined);
  };

  const handleNext = () => {
    if (currentDimensionIndex < BAUMANN_DIMENSIONS.length - 1) {
      setCurrentDimensionIndex(prev => prev + 1);
    } else {
      setShowResult(true);
    }
  };

  const handlePrevious = () => {
    if (currentDimensionIndex > 0) {
      setCurrentDimensionIndex(prev => prev - 1);
    }
  };

  const calculateBaumannType = (): { code: string; scores: Record<string, number> } => {
    const scores: Record<string, number> = {};
    let code = "";

    BAUMANN_DIMENSIONS.forEach(dim => {
      let sum = 0;
      dim.questions.forEach(q => {
        sum += answers[q.id] || 2.5;
      });
      const avg = sum / dim.questions.length;
      scores[dim.id] = avg;

      if (avg < dim.threshold) {
        code += dim.letterLow;
      } else {
        code += dim.letterHigh;
      }
    });

    return { code, scores };
  };

  const restart = () => {
    setAnswers({});
    setCurrentDimensionIndex(0);
    setShowResult(false);
  };

  const { code: calculatedCode, scores: dimensionScores } = showResult
    ? calculateBaumannType()
    : { code: "", scores: {} };
  
  const resultDetail = SKIN_TYPE_RECS[calculatedCode] || SKIN_TYPE_RECS.ORNT;

  return (
    <div className="min-h-screen bg-[#0F172A] text-white p-4 md:p-8 flex flex-col justify-start items-center font-sans select-none" dir={lang === 'ar' ? 'rtl' : 'ltr'}>
      {/* Top Header */}
      <header className="w-full max-w-3xl flex justify-between items-center mb-8 pb-4 border-b border-slate-800">
        <div className="flex items-center gap-3">
          <span className="text-3xl">🔬</span>
          <div>
            <h1 className="text-xl font-extrabold tracking-tight text-emerald-400">GlowLogic AI Lab</h1>
            <p className="text-xs text-slate-400">
              {lang === 'ar' ? 'مؤشر باومان لتصنيف البشرة السريري (D1-D4)' : 'Baumann Skin Type Indicator (D1-D4)'}
            </p>
          </div>
        </div>
        <button
          onClick={() => setLang(prev => prev === 'ar' ? 'en' : 'ar')}
          className="bg-slate-800 hover:bg-slate-700 text-emerald-400 font-bold px-3 py-1.5 rounded-lg text-sm transition-all duration-200 border border-emerald-500/30"
        >
          {lang === 'ar' ? 'English 🇺🇸' : 'العربية 🇸🇦'}
        </button>
      </header>

      {/* Main Container */}
      <main className="w-full max-w-3xl bg-slate-900/80 backdrop-blur-md rounded-2xl p-6 md:p-8 border border-slate-800 shadow-2xl relative overflow-hidden">
        {/* Decorative corner accent */}
        <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 rounded-full blur-2xl pointer-events-none" />
        
        {!showResult ? (
          <div>
            {/* Steps & Progress */}
            <div className="flex justify-between items-center mb-6">
              <span className="text-sm font-bold text-emerald-400">
                {lang === 'ar' ? `البعد ${currentDimensionIndex + 1} من 4` : `Dimension ${currentDimensionIndex + 1} of 4`}
              </span>
              <span className="text-2xl">{currentDimension.emoji}</span>
            </div>

            {/* Progress Bar */}
            <div className="w-full h-2 bg-slate-800 rounded-full mb-8 overflow-hidden">
              <div 
                className="h-full bg-emerald-500 transition-all duration-300"
                style={{ width: `${((currentDimensionIndex + 1) / 4) * 100}%` }}
              />
            </div>

            {/* Dimension Info */}
            <div className="mb-8">
              <h2 className="text-2xl font-bold mb-2 text-slate-100">
                {lang === 'ar' ? currentDimension.titleAr : currentDimension.titleEn}
              </h2>
              <p className="text-sm text-slate-400 leading-relaxed">
                {lang === 'ar' ? currentDimension.taglineAr : currentDimension.taglineEn}
              </p>
            </div>

            {/* Questions List */}
            <div className="space-y-8 mb-8">
              {currentDimension.questions.map((question, qIdx) => (
                <div key={question.id} className="p-5 bg-slate-950/40 rounded-xl border border-slate-800/60 hover:border-emerald-500/20 transition-all duration-200">
                  <h3 className="text-base font-bold mb-4 text-slate-200">
                    <span className="text-emerald-400 mr-1 ml-1">{qIdx + 1}.</span>
                    {lang === 'ar' ? question.textAr : question.textEn}
                  </h3>
                  
                  {/* Options List */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {question.options.map(option => {
                      const isSelected = answers[question.id] === option.value;
                      return (
                        <button
                          key={option.value}
                          onClick={() => handleSelectOption(question.id, option.value)}
                          className={`p-4 rounded-xl text-right text-sm transition-all duration-200 border flex items-center justify-between ${
                            isSelected
                              ? 'bg-emerald-500/15 border-emerald-500 text-white font-semibold'
                              : 'bg-slate-900 border-slate-800 text-slate-300 hover:bg-slate-800/50 hover:border-slate-700'
                          }`}
                          dir={lang === 'ar' ? 'rtl' : 'ltr'}
                        >
                          <span>{lang === 'ar' ? option.textAr : option.textEn}</span>
                          {isSelected && <span className="text-emerald-400 text-lg mr-2 ml-2">✓</span>}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>

            {/* Navigation Footer */}
            <div className="flex justify-between items-center pt-4 border-t border-slate-800">
              <button
                onClick={handlePrevious}
                disabled={currentDimensionIndex === 0}
                className="bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:hover:bg-slate-800 text-white font-bold py-3 px-6 rounded-xl text-sm transition-all duration-200"
              >
                {lang === 'ar' ? '← السابق' : '← Back'}
              </button>

              <button
                onClick={handleNext}
                disabled={!isDimensionAnswered()}
                className="bg-emerald-500 hover:bg-emerald-600 disabled:opacity-40 disabled:hover:bg-emerald-500 text-slate-950 font-bold py-3 px-8 rounded-xl text-sm transition-all duration-200 shadow-lg shadow-emerald-500/10"
              >
                {currentDimensionIndex === BAUMANN_DIMENSIONS.length - 1
                  ? (lang === 'ar' ? 'احصل على النتيجة السريرية ✨' : 'Generate Clinical Type ✨')
                  : (lang === 'ar' ? 'التالي ←' : 'Next →')}
              </button>
            </div>
          </div>
        ) : (
          /* Result Summary Screen */
          <div className="animate-fadeIn">
            {/* Medal / Badge badge */}
            <div className="flex flex-col items-center justify-center text-center mb-8">
              <div className="w-24 h-24 rounded-full bg-emerald-500/10 flex items-center justify-center border-2 border-emerald-400 mb-4 animate-bounce">
                <span className="text-4xl text-emerald-400">{calculatedCode}</span>
              </div>
              <h2 className="text-xs font-bold uppercase tracking-widest text-emerald-400 mb-1">
                {lang === 'ar' ? 'نوع بشرتك المكتشف رسمياً' : 'Your Official Baumann Skin Type'}
              </h2>
              <h3 className="text-2xl font-extrabold text-white">
                {lang === 'ar' ? resultDetail.nameAr : resultDetail.nameEn}
              </h3>
            </div>

            {/* Dimension Breakdown Details */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
              {BAUMANN_DIMENSIONS.map(dim => {
                const score = dimensionScores[dim.id];
                const isHigh = score >= dim.threshold;
                const activeLetter = isHigh ? dim.letterHigh : dim.letterLow;
                return (
                  <div key={dim.id} className="p-3.5 bg-slate-950/55 rounded-xl border border-slate-800 flex flex-col items-center justify-center text-center">
                    <span className="text-xs text-slate-400">{dim.id} Score</span>
                    <span className="text-lg font-black text-emerald-300">{score.toFixed(2)}</span>
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-800 border border-slate-700 text-white mt-2">
                      {activeLetter} ({isHigh ? (lang === 'ar' ? 'مرتفع' : 'High') : (lang === 'ar' ? 'منخفض' : 'Low')})
                    </span>
                  </div>
                );
              })}
            </div>

            {/* Narrative Box */}
            <div className="p-5 bg-slate-950/40 rounded-xl border border-slate-800 mb-6 leading-relaxed text-slate-300 text-sm">
              <h4 className="font-extrabold text-emerald-400 mb-2 flex items-center gap-2">
                <span>📋</span>
                {lang === 'ar' ? 'التشريح الفسيولوجي للبشرة:' : 'Physiological Breakdown:'}
              </h4>
              <p>{lang === 'ar' ? resultDetail.descAr : resultDetail.descEn}</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
              <div className="p-4 bg-slate-950/30 rounded-xl border border-slate-800/80">
                <h5 className="font-bold text-emerald-400 mb-2 flex items-center gap-2 text-sm">
                  <span>💚</span>
                  {lang === 'ar' ? 'نقاط القوة والمميزات:' : 'Strengths & Pros:'}
                </h5>
                <p className="text-slate-300 text-xs leading-relaxed">{lang === 'ar' ? resultDetail.prosAr : 'High cell density & collagen longevity.'}</p>
              </div>

              <div className="p-4 bg-slate-950/30 rounded-xl border border-slate-800/80">
                <h5 className="font-bold text-emerald-400 mb-2 flex items-center gap-2 text-sm">
                  <span>⚠️</span>
                  {lang === 'ar' ? 'الأخطار والتحديات الطبيعية:' : 'Weaknesses & Cons:'}
                </h5>
                <p className="text-slate-300 text-xs leading-relaxed">{lang === 'ar' ? resultDetail.consAr : 'Dehydration, dryness & scaling.'}</p>
              </div>
            </div>

            {/* Skincare Formula Recipes section */}
            <div className="p-5 bg-[#059669]/10 rounded-xl border border-emerald-500/30 mb-8">
              <h4 className="font-extrabold text-emerald-300 mb-4 flex items-center gap-2 text-base">
                <span>🧴</span>
                {lang === 'ar' ? 'التركيبة والبروتوكول السريري الموصى به:' : 'Recommended Skincare Formulation Protocol:'}
              </h4>
              
              <div className="space-y-3.5">
                <div className="flex justify-between items-center border-b border-slate-800/50 pb-2">
                  <span className="text-xs text-slate-400 font-bold">{lang === 'ar' ? '1. الغسول الموصى به:' : '1. Cleanser:'}</span>
                  <span className="text-xs font-semibold text-emerald-400">{resultDetail.cleanser}</span>
                </div>
                <div className="flex justify-between items-center border-b border-slate-800/50 pb-2">
                  <span className="text-xs text-slate-400 font-bold">{lang === 'ar' ? '2. المكون الفعال للتحسين:' : '2. Key Active Ingredient:'}</span>
                  <span className="text-xs font-semibold text-emerald-400">{resultDetail.active}</span>
                </div>
                <div className="flex justify-between items-center pb-1">
                  <span className="text-xs text-slate-400 font-bold">{lang === 'ar' ? '3. قاعدة مرطب الحفظ:' : '3. Base Carrier Moisturizer:'}</span>
                  <span className="text-xs font-semibold text-emerald-400">{resultDetail.moisturizer}</span>
                </div>
              </div>
            </div>

            {/* Action buttons */}
            <div className="flex gap-3 justify-center">
              <button
                onClick={restart}
                className="bg-slate-800 hover:bg-slate-700 text-white font-bold py-3.5 px-8 rounded-xl text-sm transition-all duration-200 border border-slate-700"
              >
                {lang === 'ar' ? 'إعادة الاستبيان 🔄' : 'Restart Questionnaire 🔄'}
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

export default BaumannSkinTypeQuestionnaire;
