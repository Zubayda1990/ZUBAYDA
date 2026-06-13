package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "skincare_tasks")
data class SkincareTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isAm: Boolean,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "task_completion_logs",
    indices = [Index(value = ["dateStr", "taskId"], unique = true)]
)
data class TaskCompletionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateStr: String, // "yyyy-MM-dd"
    val taskId: Int,
    val isCompleted: Boolean
)

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["dateStr"], unique = true)]
)
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateStr: String, // "yyyy-MM-dd"
    val cupsDrank: Int,
    val targetCups: Int = 8
)

@Entity(tableName = "saved_skin_reports")
data class SavedSkinReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String,
    val skinType: String,
    val hydration: Int,
    val barrierHealth: Int,
    val pathology: String,
    val routineAM: String,
    val routinePM: String,
    val avoid: String,
    val isDemo: Boolean = false
)

@Entity(tableName = "recommended_products")
data class SkincareProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String,
    val category: String, // "غسول", "مرطب", "سيروم", "واقي شمس", "مقشر"
    val skinType: String, // "جافة" or "دهنية" or "مختلطة" or "جميع الأنواع"
    val concern: String, // "حب الشباب" or "الجفاف" or "التصبغات" or "الحساسية والاحمرار" or "الدهون والمسام" or "التجاعيد والخطوط"
    val description: String,
    val activeIngredients: String,
    val isCustom: Boolean = false
)

@Dao
interface SkincareDao {
    @Query("SELECT * FROM skincare_tasks ORDER BY isCustom ASC, id ASC")
    fun getAllTasksFlow(): Flow<List<SkincareTask>>

    @Query("SELECT * FROM skincare_tasks ORDER BY isCustom ASC, id ASC")
    suspend fun getAllTasksDirect(): List<SkincareTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: SkincareTask): Long

    @Delete
    suspend fun deleteTask(task: SkincareTask)

    @Query("DELETE FROM skincare_tasks WHERE isCustom = 1")
    suspend fun deleteCustomTasks()

    @Query("SELECT * FROM task_completion_logs WHERE dateStr = :dateStr")
    fun getCompletionsForDateFlow(dateStr: String): Flow<List<TaskCompletionLog>>

    @Query("SELECT * FROM task_completion_logs WHERE dateStr = :dateStr")
    suspend fun getCompletionsForDateDirect(dateStr: String): List<TaskCompletionLog>

    @Query("SELECT * FROM task_completion_logs")
    fun getAllCompletionsFlow(): Flow<List<TaskCompletionLog>>

    @Query("SELECT * FROM task_completion_logs")
    suspend fun getAllCompletionsDirect(): List<TaskCompletionLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletion(completion: TaskCompletionLog)

    @Query("DELETE FROM task_completion_logs WHERE dateStr = :dateStr AND taskId = :taskId")
    suspend fun removeCompletion(dateStr: String, taskId: Int)

    @Query("SELECT * FROM water_logs WHERE dateStr = :dateStr")
    fun getWaterLogForDateFlow(dateStr: String): Flow<WaterLog?>

    @Query("SELECT * FROM water_logs")
    fun getAllWaterLogsFlow(): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWaterLog(waterLog: WaterLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedReport(report: SavedSkinReport): Long

    @Query("SELECT * FROM saved_skin_reports ORDER BY timestamp DESC")
    fun getAllSavedReportsFlow(): Flow<List<SavedSkinReport>>

    @Query("SELECT * FROM saved_skin_reports ORDER BY timestamp DESC")
    suspend fun getAllSavedReportsDirect(): List<SavedSkinReport>

    @Delete
    suspend fun deleteSavedReport(report: SavedSkinReport)

    @Query("SELECT * FROM recommended_products ORDER BY id ASC")
    fun getAllProductsFlow(): Flow<List<SkincareProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: SkincareProduct): Long

    @Delete
    suspend fun deleteProduct(product: SkincareProduct)
}

@Database(entities = [SkincareTask::class, TaskCompletionLog::class, WaterLog::class, SavedSkinReport::class, SkincareProduct::class], version = 4, exportSchema = false)
abstract class SkincareDatabase : RoomDatabase() {
    abstract fun dao(): SkincareDao

    companion object {
        @Volatile
        private var INSTANCE: SkincareDatabase? = null

        fun getDatabase(context: Context): SkincareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkincareDatabase::class.java,
                    "glowlogic_skincare_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Use background thread to insert pre-populated values
                CoroutineScope(Dispatchers.IO).launch {
                    val defaultTasks = listOf(
                        // Morning tasks (AM)
                        SkincareTask(name = "غسول لطيف يوازن حموضة البشرة 🧴", isAm = true, isCustom = false),
                        SkincareTask(name = "سيروم الهيالورنيك على بشرة ندية 💧", isAm = true, isCustom = false),
                        SkincareTask(name = "مرطب حاجز الدهن وعوامل الترطيب 🛡️", isAm = true, isCustom = false),
                        SkincareTask(name = "واقي شمس فيزيائي واسع النطاق ☀️", isAm = true, isCustom = false),
                        
                        // Evening tasks (PM)
                        SkincareTask(name = "غسول مائي لتنظيف عميق مزدوج 🧼", isAm = false, isCustom = false),
                        SkincareTask(name = "سيروم علاجي مهدئ ومنظم للافرازات ✨", isAm = false, isCustom = false),
                        SkincareTask(name = "كريم السيراميد المكثف لترميم البشرة 🌙", isAm = false, isCustom = false)
                    )
                    INSTANCE?.let { database ->
                        for (task in defaultTasks) {
                            database.dao().insertTask(task)
                        }

                        // Seeding skin reports
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val cal = java.util.Calendar.getInstance()
                        
                        val dateToday = sdf.format(cal.time)
                        
                        cal.add(java.util.Calendar.DAY_OF_YEAR, -5)
                        val dateMinus5 = sdf.format(cal.time)
                        
                        val initialReports = listOf(
                            SavedSkinReport(
                                timestamp = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L,
                                dateStr = dateMinus5,
                                skinType = "جافة ومتحسسة 🏜️",
                                hydration = 32,
                                barrierHealth = 40,
                                pathology = "تراجع ملحوظ في معامل الرطوبة الداخلية مع التهاب وتسلخ خفيف في حاجز البشرة بسبب الأجواء الحارة والجافة.",
                                routineAM = "١. غسول مرطب غير رغوي.\n٢. سيروم حمض الهيالورونيك.\n٣. مرطب حاجز دهني غني بالسيراميد.\n٤. واقي شمس مهدئ ومقاوم.",
                                routinePM = "١. منظف لطيف مائي.\n٢. سيروم خلاصة السيكا والزنك المهدئ.\n٣. كريم سيراميد ليلي مكثف للترميم.",
                                avoid = "الصابون الخشن، أحماض التقشير عالية التركيز لثلاث أيام، والتعرض للشمس دون واقي."
                            ),
                            SavedSkinReport(
                                timestamp = System.currentTimeMillis(),
                                dateStr = dateToday,
                                skinType = "مختلطة دهنية (الحالة الحالية) 🧪",
                                hydration = 54,
                                barrierHealth = 65,
                                pathology = "تحسن ممتاز في جدار الحماية الخلوي مع نشاط معتدل في الغدد الزهمية بالمنطقة T.",
                                routineAM = "١. غسول لطيف يوازن حموضة البشرة.\n٢. سيروم نياسيناميد ١٠٪ لتقليل الاحمرار.\n٣. مرطب عامل ترطيب خفيف جداً.\n٤. واقي شمس فيزيائي واسع النطاق.",
                                routinePM = "١. غسول رغوي عميق للتطهير المائي.\n٢. سيروم حمض الأزيليك لتثبيط الحبوب.\n٣. كريم السيراميد المكثف لترميم البشرة.",
                                avoid = "المنتجات الزيتية الثقيلة، المستحضرات المقشرة الفيزيائية الخشنة والسفر بدون ترطيب."
                            )
                        )
                        for (rep in initialReports) {
                            database.dao().insertSavedReport(rep)
                        }

                        // Seeding some water logs over the last 7 days to populate consistency chart
                        val wCal = java.util.Calendar.getInstance()
                        val waterLogs = listOf(
                            WaterLog(dateStr = sdf.format(wCal.time), cupsDrank = 5, targetCups = 8), // Today
                            WaterLog(dateStr = { wCal.add(java.util.Calendar.DAY_OF_YEAR, -1); sdf.format(wCal.time) }(), cupsDrank = 8, targetCups = 8), // Yesterday
                            WaterLog(dateStr = { wCal.add(java.util.Calendar.DAY_OF_YEAR, -1); sdf.format(wCal.time) }(), cupsDrank = 6, targetCups = 8), // 2 days ago
                            WaterLog(dateStr = { wCal.add(java.util.Calendar.DAY_OF_YEAR, -1); sdf.format(wCal.time) }(), cupsDrank = 7, targetCups = 8), // 3 days ago
                            WaterLog(dateStr = { wCal.add(java.util.Calendar.DAY_OF_YEAR, -1); sdf.format(wCal.time) }(), cupsDrank = 4, targetCups = 8), // 4 days ago
                            WaterLog(dateStr = { wCal.add(java.util.Calendar.DAY_OF_YEAR, -1); sdf.format(wCal.time) }(), cupsDrank = 8, targetCups = 8), // 5 days ago
                            WaterLog(dateStr = { wCal.add(java.util.Calendar.DAY_OF_YEAR, -1); sdf.format(wCal.time) }(), cupsDrank = 3, targetCups = 8)  // 6 days ago
                        )
                        for (wl in waterLogs) {
                            database.dao().insertOrUpdateWaterLog(wl)
                        }
                        
                        // Seeding some completion logs to populate routine consistency chart
                        val cCal = java.util.Calendar.getInstance()
                        val completionLogs = mutableListOf<TaskCompletionLog>()
                        
                        // Today: tasks 1, 2 completed
                        val d0 = sdf.format(cCal.time)
                        completionLogs.add(TaskCompletionLog(dateStr = d0, taskId = 1, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d0, taskId = 2, isCompleted = true))
                        
                        // Yesterday: tasks 1, 2, 3, 5, 6 completed
                        cCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val d1 = sdf.format(cCal.time)
                        completionLogs.add(TaskCompletionLog(dateStr = d1, taskId = 1, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d1, taskId = 2, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d1, taskId = 3, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d1, taskId = 5, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d1, taskId = 6, isCompleted = true))
                        
                        // 2 days ago: tasks 1, 5 completed
                        cCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val d2 = sdf.format(cCal.time)
                        completionLogs.add(TaskCompletionLog(dateStr = d2, taskId = 1, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d2, taskId = 5, isCompleted = true))

                        // 3 days ago: tasks 1, 2, 6 completed
                        cCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                        val d3 = sdf.format(cCal.time)
                        completionLogs.add(TaskCompletionLog(dateStr = d3, taskId = 1, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d3, taskId = 2, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d3, taskId = 6, isCompleted = true))

                        // 5 days ago: tasks 1, 3 completed
                        cCal.add(java.util.Calendar.DAY_OF_YEAR, -2) // Skip 4 days ago to have a gap
                        val d5 = sdf.format(cCal.time)
                        completionLogs.add(TaskCompletionLog(dateStr = d5, taskId = 1, isCompleted = true))
                        completionLogs.add(TaskCompletionLog(dateStr = d5, taskId = 3, isCompleted = true))

                        for (cl in completionLogs) {
                            database.dao().insertOrUpdateCompletion(cl)
                        }

                        val defaultProducts = listOf(
                            SkincareProduct(
                                name = "Effaclar Purifying Foaming Gel",
                                brand = "La Roche-Posay",
                                category = "غسول",
                                skinType = "دهنية",
                                concern = "حب الشباب",
                                description = "غسول رغوي يطهر البشرة بلطف ويقلل الإفرازات الدهنية الزائدة ويمنع سد المسام دون إحداث جفاف للبشرة.",
                                activeIngredients = "الزنك المقاوم للميكروبات، المياه الحرارية المهدئة"
                            ),
                            SkincareProduct(
                                name = "Hydrating Facial Cleanser",
                                brand = "CeraVe",
                                category = "غسول",
                                skinType = "جافة",
                                concern = "الجفاف",
                                description = "منظف غني ومطهر كريمي غير رغوي يعمل على تنظيف خلايا البشرة الجافة مع الحفاظ الكامل على الرطوبة الطبيعية ومستويات السيراميدات الأساسية.",
                                activeIngredients = "كريم السيراميدات الثلاثية المتكاملة، حمض الهيالورونيك"
                            ),
                            SkincareProduct(
                                name = "Niacinamide 10% + Zinc 1%",
                                brand = "The Ordinary",
                                category = "سيروم",
                                skinType = "مختلطة",
                                concern = "الدهون والمسام",
                                description = "سيروم مائي عالي التركيز ينظم إفراز الدهون المسامية، يقلل من ظهور التوهج، ويشد جدران المسام تجميلياً.",
                                activeIngredients = "النياسيناميد 10%، زنك PCA 1%"
                            ),
                            SkincareProduct(
                                name = "Cicaplast Baume B5+",
                                brand = "La Roche-Posay",
                                category = "مرطب",
                                skinType = "جميع الأنواع",
                                concern = "الحساسية والاحمرار",
                                description = "مرمم حاجز فائق تبريدي وعلاجي سريع للجلد الملتهب والمهيج والمتشقق، ومقاوم للندبات وتلف الخلايا.",
                                activeIngredients = "البانثينول 5% (فیتامین B5)، الماديكاسوسايد (مستخلص السيكا)، الزنك والنحاس"
                            ),
                            SkincareProduct(
                                name = "Pigment Control Sun Fluid SPF 50+",
                                brand = "Eucerin",
                                category = "واقي شمس",
                                skinType = "جميع الأنواع",
                                concern = "التصبغات",
                                description = "واقي شمس طبي وعلاجي واسع النطاق يقي من الأشعة فوق البنفسجية UVA/UVB ويعمل على تخفيض وتثبيط تصبغات الكلف والبقع الموضعية بفعالية تامة.",
                                activeIngredients = "مادة التياميدول الحصرية الحاصلة على براءة ابتكار لتثبيط الميلانين"
                            ),
                            SkincareProduct(
                                name = "Skin Perfecting 2% BHA Liquid Exfoliant",
                                brand = "Paula's Choice",
                                category = "مقشر",
                                skinType = "دهنية",
                                concern = "حب الشباب",
                                description = "مقشر سائل سريع النفاذ ينظف الحويصلات الدهنية من الداخل بعمق ويزيل الخلايا القرنية الميتة لتحسين نضارة وملمس الجلد وتصغير المسام المتمددة.",
                                activeIngredients = "حمض الساليسيليك (BHA) 2%، خلاصة الشاي الأخضر"
                            ),
                            SkincareProduct(
                                name = "Sensibio H2O Micellar Water",
                                brand = "Bioderma",
                                category = "غسول",
                                skinType = "جميع الأنواع",
                                concern = "الحساسية والاحمرار",
                                description = "محلول مائي لتنظيف الوجه وإزالة المكياج بلطف تام بدون غسل، يحترم تماماً توازن حاجز البشرة الحساسة والنواقل المائية العصبية.",
                                activeIngredients = "مذيلات مرطبة متوافقة حيوياً مع بايلوجيا البشرة الطبيعية"
                            ),
                            SkincareProduct(
                                name = "Advanced Snail 96 Mucin Power Essence",
                                brand = "COSRX",
                                category = "سيروم",
                                skinType = "جميع الأنواع",
                                concern = "الجفاف",
                                description = "مستخلص غني جداً يغذي خلايا الجلد رطوبة ومرونة، ويسرع إصلاح التلف السطحي، ويمنح نضارة زجاجية طبيعية غاية في اللمعان.",
                                activeIngredients = "ترشيح إفراز البزاق 96%، آلانتوين مهدئ للأنسجة"
                            ),
                            SkincareProduct(
                                name = "Retinol Correxion Deep Wrinkle Cream",
                                brand = "RoC",
                                category = "مرطب",
                                skinType = "جميع الأنواع",
                                concern = "التجاعيد والخطوط",
                                description = "كريم ليلي مكثف يحارب التجاعيد الشديدة والخطوط الدقيقة التعبيرية عبر تحفيز تسريع التجدد الخلوي وإنتاج كولاجين الأدمة.",
                                activeIngredients = "ريتينول نقي عالي النفاذ، مركب معادن مغذٍّ ومقاوم للأكسدة"
                            )
                        )
                        for (prod in defaultProducts) {
                            database.dao().insertProduct(prod)
                        }
                    }
                }
            }
        }
    }
}
