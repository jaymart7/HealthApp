package ph.mart.healthapp.core.data

import androidx.room3.migration.Migration
import androidx.sqlite.execSQL

/**
 * The upgrade path for [AppDatabase], one entry per version, `n` to `n + 1`.
 *
 * Every step is the diff between two exported schemas in `core/data/schemas/` and nothing else —
 * new tables come from that version's own `createSql`, new columns are an `ADD COLUMN`. Thirty-six
 * `Migration` subclasses would have been thirty-six copies of one loop, so the SQL is the table and
 * [MIGRATIONS] is the plumbing; `MigrationsTest` checks the two against the schemas.
 *
 * A column arriving `NOT NULL` needs a value for the rows already there, and the schema files do
 * not carry one — these entities declare their defaults in Kotlin, not in `@ColumnInfo`. Each
 * `DEFAULT` below is that Kotlin default, so an upgraded row reads the same as a fresh one. Room's
 * own validation ignores a default the entity never declared, which is what makes this safe.
 */
internal val STEPS: Map<Int, List<String>> = mapOf(
    1 to listOf(
        "CREATE TABLE IF NOT EXISTS `measurement_entry` (`part` TEXT NOT NULL, `date` INTEGER NOT NULL, `valueCm` REAL NOT NULL, PRIMARY KEY(`part`, `date`))",
        "CREATE TABLE IF NOT EXISTS `progress_photo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `weightKg` REAL)",
        "CREATE TABLE IF NOT EXISTS `weight_entry` (`date` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`date`))",
    ),
    2 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `mealRemindersOn` INTEGER NOT NULL DEFAULT 1",
        "ALTER TABLE `profile` ADD COLUMN `weighInReminderOn` INTEGER NOT NULL DEFAULT 1",
        "ALTER TABLE `profile` ADD COLUMN `photoReminderOn` INTEGER NOT NULL DEFAULT 0",
    ),
    3 to listOf(
        "CREATE TABLE IF NOT EXISTS `favorite_food` (`name` TEXT NOT NULL, `portionAmount` REAL NOT NULL, `portionUnit` TEXT NOT NULL, `calories` INTEGER NOT NULL, `proteinG` INTEGER NOT NULL, `carbsG` INTEGER NOT NULL, `fatG` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`name`))",
    ),
    4 to listOf(
        "CREATE TABLE IF NOT EXISTS `water_day` (`dateEpochDay` INTEGER NOT NULL, `glasses` INTEGER NOT NULL, PRIMARY KEY(`dateEpochDay`))",
        "ALTER TABLE `profile` ADD COLUMN `waterRemindersOn` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `profile` ADD COLUMN `waterGoalGlasses` INTEGER NOT NULL DEFAULT 8",
    ),
    5 to listOf(
        "CREATE TABLE IF NOT EXISTS `exercise_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `date` INTEGER NOT NULL, `loggedAt` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `burnedKcal` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)",
        "ALTER TABLE `profile` ADD COLUMN `addExerciseToBudget` INTEGER NOT NULL DEFAULT 1",
    ),
    6 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `darkThemeOn` INTEGER",
    ),
    7 to listOf(
        "CREATE TABLE IF NOT EXISTS `saved_meal` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `saved_meal_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mealId` INTEGER NOT NULL, `name` TEXT NOT NULL, `portionAmount` REAL NOT NULL, `portionUnit` TEXT NOT NULL, `calories` INTEGER NOT NULL, `proteinG` INTEGER NOT NULL, `carbsG` INTEGER NOT NULL, `fatG` INTEGER NOT NULL)",
    ),
    8 to listOf(
        "CREATE TABLE IF NOT EXISTS `mood_day` (`dateEpochDay` INTEGER NOT NULL, `mood` INTEGER NOT NULL, `energy` INTEGER NOT NULL, PRIMARY KEY(`dateEpochDay`))",
    ),
    9 to listOf(
        "CREATE TABLE IF NOT EXISTS `health_link` (`remoteName` TEXT NOT NULL, `dataType` TEXT NOT NULL, `localTable` TEXT NOT NULL, `localId` INTEGER NOT NULL, `remoteTimeMillis` INTEGER NOT NULL, `pushed` INTEGER NOT NULL, PRIMARY KEY(`remoteName`))",
    ),
    10 to listOf(
        "CREATE TABLE IF NOT EXISTS `sleep_day` (`date` INTEGER NOT NULL, `minutesAsleep` INTEGER NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL, PRIMARY KEY(`date`))",
    ),
    11 to listOf(
        "CREATE TABLE IF NOT EXISTS `step_day` (`date` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `burnedKcal` INTEGER NOT NULL, PRIMARY KEY(`date`))",
        "ALTER TABLE `exercise_entry` ADD COLUMN `steps` INTEGER NOT NULL DEFAULT 0",
    ),
    12 to listOf(
        "ALTER TABLE `saved_meal` ADD COLUMN `servings` INTEGER",
    ),
    13 to listOf(
        "CREATE TABLE IF NOT EXISTS `fast_session` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER, `goalHours` INTEGER NOT NULL)",
        "ALTER TABLE `profile` ADD COLUMN `fastingGoalHours` INTEGER NOT NULL DEFAULT 16",
        "ALTER TABLE `profile` ADD COLUMN `fastingRemindersOn` INTEGER NOT NULL DEFAULT 0",
    ),
    14 to listOf(
        "ALTER TABLE `favorite_food` ADD COLUMN `fiberG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `favorite_food` ADD COLUMN `sugarG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `favorite_food` ADD COLUMN `sodiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `fiberG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `sugarG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `sodiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `fiberG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `sugarG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `sodiumMg` INTEGER NOT NULL DEFAULT 0",
    ),
    15 to listOf(
        "CREATE TABLE IF NOT EXISTS `heart_day` (`date` INTEGER NOT NULL, `averageBpm` INTEGER NOT NULL, `minBpm` INTEGER NOT NULL, PRIMARY KEY(`date`))",
    ),
    16 to listOf(
        "CREATE TABLE IF NOT EXISTS `supplement` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `dose` TEXT NOT NULL, `timesPerDay` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `supplement_day` (`dateEpochDay` INTEGER NOT NULL, `supplementId` INTEGER NOT NULL, `taken` INTEGER NOT NULL, `dueTimes` INTEGER NOT NULL, PRIMARY KEY(`dateEpochDay`, `supplementId`))",
        "ALTER TABLE `profile` ADD COLUMN `supplementRemindersOn` INTEGER NOT NULL DEFAULT 0",
    ),
    17 to listOf(
        "CREATE TABLE IF NOT EXISTS `blood_pressure_reading` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `takenAtMillis` INTEGER NOT NULL, `systolic` INTEGER NOT NULL, `diastolic` INTEGER NOT NULL, `pulseBpm` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)",
    ),
    18 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `mascotName` TEXT",
    ),
    19 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `stepGoal` INTEGER NOT NULL DEFAULT 10000",
    ),
    20 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `mascotPaletteName` TEXT",
    ),
    21 to listOf(
        "CREATE TABLE IF NOT EXISTS `chat_message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fromUser` INTEGER NOT NULL, `text` TEXT NOT NULL, `sentAtMillis` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)",
    ),
    22 to listOf(
        "CREATE TABLE IF NOT EXISTS `strength_set` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, `exerciseName` TEXT NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_strength_set_entryId` ON `strength_set` (`entryId`)",
    ),
    23 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `homeLayout` TEXT",
    ),
    24 to listOf(
        "CREATE TABLE IF NOT EXISTS `routine` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `routine_lift` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routineId` INTEGER NOT NULL, `exerciseName` TEXT NOT NULL, `sets` INTEGER NOT NULL, `reps` INTEGER NOT NULL)",
    ),
    25 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `workoutRemindersOn` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `routine` ADD COLUMN `days` INTEGER NOT NULL DEFAULT 0",
    ),
    26 to listOf(
        "ALTER TABLE `profile` ADD COLUMN `recapReminderOn` INTEGER NOT NULL DEFAULT 0",
    ),
    27 to listOf(
        "CREATE TABLE IF NOT EXISTS `cycle_day` (`dateEpochDay` INTEGER NOT NULL, `flow` INTEGER NOT NULL, `symptoms` TEXT NOT NULL, PRIMARY KEY(`dateEpochDay`))",
        "ALTER TABLE `profile` ADD COLUMN `cycleTrackingOn` INTEGER",
    ),
    28 to listOf(
        "CREATE TABLE IF NOT EXISTS `scanned_product` (`barcode` TEXT NOT NULL, `name` TEXT NOT NULL, `portionAmount` REAL NOT NULL, `portionUnit` TEXT NOT NULL, `calories` INTEGER NOT NULL, `proteinG` INTEGER NOT NULL, `carbsG` INTEGER NOT NULL, `fatG` INTEGER NOT NULL, `fiberG` INTEGER NOT NULL, `sugarG` INTEGER NOT NULL, `sodiumMg` INTEGER NOT NULL, PRIMARY KEY(`barcode`))",
    ),
    29 to listOf(
        "ALTER TABLE `food_entry` ADD COLUMN `photoPath` TEXT",
    ),
    30 to listOf(
        "ALTER TABLE `favorite_food` ADD COLUMN `vitaminDUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `favorite_food` ADD COLUMN `calciumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `favorite_food` ADD COLUMN `ironUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `favorite_food` ADD COLUMN `potassiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `vitaminDUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `calciumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `ironUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `food_entry` ADD COLUMN `potassiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `vitaminDUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `calciumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `ironUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `saved_meal_item` ADD COLUMN `potassiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `scanned_product` ADD COLUMN `vitaminDUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `scanned_product` ADD COLUMN `calciumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `scanned_product` ADD COLUMN `ironUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `scanned_product` ADD COLUMN `potassiumMg` INTEGER NOT NULL DEFAULT 0",
    ),
    31 to listOf(
        "ALTER TABLE `scanned_product` ADD COLUMN `servingSize` TEXT",
    ),
    32 to listOf(
        "CREATE TABLE IF NOT EXISTS `food_search_query` (`query` TEXT NOT NULL, `lastUsedAt` INTEGER NOT NULL, PRIMARY KEY(`query`))",
    ),
    33 to listOf(
        "CREATE TABLE IF NOT EXISTS `food_search_query_new` (`kind` TEXT NOT NULL, `query` TEXT NOT NULL, `lastUsedAt` INTEGER NOT NULL, PRIMARY KEY(`kind`, `query`))",
        "INSERT INTO `food_search_query_new` (`kind`, `query`, `lastUsedAt`) SELECT 'search', `query`, `lastUsedAt` FROM `food_search_query`",
        "DROP TABLE `food_search_query`",
        "ALTER TABLE `food_search_query_new` RENAME TO `food_search_query`",
    ),
    34 to listOf(
        "ALTER TABLE `cycle_day` ADD COLUMN `minuteOfDay` INTEGER",
        "ALTER TABLE `measurement_entry` ADD COLUMN `minuteOfDay` INTEGER",
        "ALTER TABLE `progress_photo` ADD COLUMN `minuteOfDay` INTEGER",
        "ALTER TABLE `weight_entry` ADD COLUMN `minuteOfDay` INTEGER",
    ),
    35 to listOf(
        "ALTER TABLE `chat_message` ADD COLUMN `receipt` TEXT",
    ),
    36 to listOf(
        "CREATE TABLE IF NOT EXISTS `note_day` (`dateEpochDay` INTEGER NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`dateEpochDay`))",
    ),
    37 to listOf(
        "ALTER TABLE `supplement` ADD COLUMN `fiberG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `sugarG` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `sodiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `vitaminDUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `calciumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `ironUg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `potassiumMg` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `supplement` ADD COLUMN `panel` TEXT NOT NULL DEFAULT ''",
    ),
)

internal val MIGRATIONS: Array<Migration> =
    STEPS.map { (from, statements) ->
        Migration(from, from + 1) { connection ->
            statements.forEach { connection.execSQL(it) }
        }
    }.toTypedArray()
