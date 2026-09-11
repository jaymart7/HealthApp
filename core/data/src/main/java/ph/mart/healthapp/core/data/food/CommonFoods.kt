package ph.mart.healthapp.core.data.food

import kotlin.math.roundToInt

/**
 * The food "database" the search panel offers: one hand-written list shipped in the APK, no
 * table, no repository and no query — the shape [localMealIdeas] and
 * [goalProjection][ph.mart.healthapp.core.data.progress.goalProjection] already use.
 *
 * It replaced a USDA FoodData Central call, and the three reasons are the whole design: it
 * answers instantly with no debounce, it answers offline, and it spends nothing from the
 * app-wide key budget the barcode scanner still lives on. What it gives up is branded packages
 * — a scan resolves those, and anything neither knows is still typed in by hand.
 *
 * Every row is **per 100 g** ([PORTION_G]), the convention [ScannedProduct] already carries, so a
 * pick is repriced by `withPortionAmount()` exactly as an FDC hit is. Drinks and oils are per
 * 100 ml at the same figure — near enough at this precision, and the alternative is a second unit
 * on a list whose whole point is that it is one shape.
 *
 * The names stay in Kotlin, and must: [mergeSuggestions] and [searchFoods] treat the trimmed
 * lowercase name as a food's identity, and a food the user authors *replaces* a built-in row of
 * the same name. Two names for one food is exactly what that dedup exists to prevent.
 *
 * Figures are USDA reference values, rounded to the Int fields [Nutrients] holds. Every nutrient
 * is filled **where the food is a real source** and left at 0 elsewhere — `0` already reads as
 * unknown-or-none everywhere else in the app, and a fabricated 1 mg on every row would be worse
 * than an honest blank. That is exactly why a graded panel reports its coverage: see
 * [DiaryTotals.foodsWithMicronutrients][ph.mart.healthapp.core.data.food.DiaryTotals].
 *
 * ponytail: cooked-weight staples in one flat list, no categories and no synonyms, so "aubergine"
 * finds nothing. A tags column on the row is the upgrade path if that gets reported.
 */
val COMMON_FOODS: List<ScannedProduct> = listOf(
    // Poultry and meat, cooked.
    food("Chicken breast, cooked", 165, 31, 0, 4, sodium = 74, calcium = 15, iron = 1.0, potassium = 256),
    food("Chicken thigh, cooked", 209, 26, 0, 11, sodium = 88, calcium = 12, iron = 1.3, potassium = 230),
    food("Chicken, roasted with skin", 239, 27, 0, 14, sodium = 82, calcium = 15, iron = 1.3, potassium = 223),
    food("Turkey breast, cooked", 135, 30, 0, 1, sodium = 99, calcium = 12, iron = 1.4, potassium = 302),
    food("Ground beef, cooked", 250, 26, 0, 15, sodium = 75, calcium = 24, iron = 2.6, potassium = 318),
    food("Beef steak, cooked", 206, 31, 0, 8, sodium = 55, calcium = 18, iron = 2.6, potassium = 353),
    food("Pork chop, cooked", 231, 26, 0, 13, sodium = 62, calcium = 25, iron = 0.9, potassium = 423),
    food("Pork belly, cooked", 518, 9, 0, 53, sodium = 32, iron = 0.6, potassium = 185),
    food("Bacon, cooked", 541, 37, 1, 42, sodium = 1717, iron = 1.4, potassium = 565),
    food("Ham, sliced", 145, 17, 2, 7, sodium = 1203, iron = 1.0, potassium = 287),
    food("Sausage, pork, cooked", 301, 18, 1, 25, sodium = 800, iron = 1.2, potassium = 268),
    food("Lamb, cooked", 258, 25, 0, 17, sodium = 72, calcium = 17, iron = 1.9, potassium = 310),

    // Fish and seafood.
    food("Salmon, cooked", 208, 20, 0, 13, sodium = 59, vitD = 13, calcium = 15, iron = 0.5, potassium = 384),
    food("Tuna, canned in water", 116, 26, 0, 1, sodium = 320, vitD = 2, calcium = 17, iron = 1.0, potassium = 237),
    food("Tilapia, cooked", 128, 26, 0, 3, sodium = 56, vitD = 3, calcium = 14, iron = 0.7, potassium = 380),
    food("Cod, cooked", 105, 23, 0, 1, sodium = 78, vitD = 1, calcium = 18, iron = 0.3, potassium = 244),
    food("Shrimp, cooked", 99, 24, 0, 0, sodium = 111, calcium = 70, iron = 0.5, potassium = 264),
    food("Sardines, canned", 208, 25, 0, 11, sodium = 505, vitD = 5, calcium = 382, iron = 2.9, potassium = 397),
    food("Mackerel, cooked", 262, 24, 0, 18, sodium = 83, vitD = 14, calcium = 15, iron = 1.6, potassium = 401),
    food("Squid, cooked", 175, 18, 8, 7, sodium = 260, calcium = 37, iron = 1.0, potassium = 279),

    // Eggs and dairy.
    food("Egg, whole, boiled", 155, 13, 1, 11, sugar = 1, sodium = 124, vitD = 2, calcium = 50, iron = 1.2, potassium = 126),
    food("Egg white", 52, 11, 1, 0, sodium = 166, calcium = 7, potassium = 163),
    food("Milk, whole", 61, 3, 5, 3, sugar = 5, sodium = 43, vitD = 1, calcium = 113, potassium = 132),
    food("Milk, skim", 34, 3, 5, 0, sugar = 5, sodium = 42, vitD = 1, calcium = 122, potassium = 156),
    food("Greek yogurt, plain nonfat", 59, 10, 4, 0, sugar = 3, sodium = 36, calcium = 110, potassium = 141),
    food("Yogurt, plain whole milk", 61, 4, 5, 3, sugar = 5, sodium = 46, calcium = 121, potassium = 155),
    food("Cheddar cheese", 403, 25, 1, 33, sodium = 653, calcium = 721, iron = 0.7, potassium = 98),
    food("Mozzarella cheese", 300, 22, 2, 22, sodium = 627, calcium = 505, potassium = 76),
    food("Cottage cheese", 98, 11, 3, 4, sugar = 3, sodium = 364, calcium = 83, potassium = 104),
    food("Cream cheese", 342, 6, 4, 34, sugar = 3, sodium = 321, calcium = 97, potassium = 138),
    food("Butter", 717, 1, 0, 81, sodium = 576, vitD = 1, calcium = 24),

    // Grains, bread, pasta and starches.
    food("White rice, cooked", 130, 3, 28, 0, fiber = 0, iron = 0.2, potassium = 35),
    food("Brown rice, cooked", 123, 3, 26, 1, fiber = 2, calcium = 10, iron = 0.6, potassium = 86),
    food("Pasta, cooked", 158, 6, 31, 1, fiber = 2, iron = 0.5, potassium = 44),
    food("Whole wheat pasta, cooked", 124, 5, 27, 1, fiber = 4, iron = 1.1, potassium = 62),
    food("Bread, white", 265, 9, 49, 3, fiber = 3, sugar = 5, sodium = 491, calcium = 144, iron = 3.6, potassium = 115),
    food("Bread, whole wheat", 247, 13, 41, 3, fiber = 7, sugar = 6, sodium = 450, calcium = 107, iron = 2.5, potassium = 254),
    food("Oats, dry", 389, 17, 66, 7, fiber = 11, calcium = 54, iron = 4.7, potassium = 429),
    food("Oatmeal, cooked", 71, 3, 12, 2, fiber = 2, iron = 0.9, potassium = 61),
    food("Cornflakes", 357, 7, 84, 0, sugar = 8, sodium = 729, calcium = 6, iron = 8.1, potassium = 95),
    food("Granola", 471, 10, 64, 20, fiber = 7, sugar = 21, sodium = 26, calcium = 60, iron = 2.6, potassium = 350),
    food("Quinoa, cooked", 120, 4, 21, 2, fiber = 3, calcium = 17, iron = 1.5, potassium = 172),
    food("Couscous, cooked", 112, 4, 23, 0, fiber = 1, iron = 0.4, potassium = 58),
    food("Tortilla, flour", 306, 8, 51, 7, fiber = 3, sodium = 600, calcium = 130, iron = 3.2, potassium = 130),
    food("Bagel", 250, 10, 49, 2, fiber = 2, sugar = 5, sodium = 490, calcium = 51, iron = 3.8, potassium = 96),
    food("Instant noodles, cooked", 138, 3, 20, 5, sodium = 700, iron = 1.0, potassium = 30),
    food("Rice cakes", 387, 8, 82, 3, fiber = 4, sodium = 30, iron = 1.0, potassium = 110),
    food("Crackers, saltine", 418, 9, 72, 10, fiber = 3, sodium = 941, calcium = 110, iron = 4.6, potassium = 130),
    food("Pancake", 227, 6, 28, 10, sugar = 6, sodium = 439, calcium = 219, iron = 1.8, potassium = 132),
    food("Potato, boiled", 87, 2, 20, 0, fiber = 2, iron = 0.3, potassium = 379),
    food("Potato, baked", 93, 3, 21, 0, fiber = 2, iron = 1.1, potassium = 535),
    food("French fries", 312, 3, 41, 15, fiber = 4, sodium = 210, iron = 0.8, potassium = 579),
    food("Sweet potato, cooked", 90, 2, 21, 0, fiber = 3, sugar = 7, calcium = 38, iron = 0.7, potassium = 475),
    food("Sweet corn, cooked", 96, 3, 21, 2, fiber = 2, sugar = 5, iron = 0.5, potassium = 218),

    // Legumes and soy.
    food("Black beans, cooked", 132, 9, 24, 1, fiber = 9, calcium = 27, iron = 2.1, potassium = 355),
    food("Chickpeas, cooked", 164, 9, 27, 3, fiber = 8, calcium = 49, iron = 2.9, potassium = 291),
    food("Lentils, cooked", 116, 9, 20, 0, fiber = 8, calcium = 19, iron = 3.3, potassium = 369),
    food("Kidney beans, cooked", 127, 9, 23, 1, fiber = 6, calcium = 28, iron = 2.9, potassium = 403),
    food("Green peas, cooked", 84, 5, 16, 0, fiber = 6, sugar = 6, calcium = 27, iron = 1.5, potassium = 271),
    food("Tofu, firm", 144, 17, 3, 9, fiber = 2, sodium = 14, calcium = 350, iron = 2.7, potassium = 121),
    food("Tempeh", 192, 20, 8, 11, sodium = 9, calcium = 111, iron = 2.7, potassium = 412),
    food("Edamame, cooked", 121, 12, 9, 5, fiber = 5, sodium = 6, calcium = 63, iron = 2.3, potassium = 436),
    food("Peanut butter", 588, 25, 20, 50, fiber = 6, sugar = 9, sodium = 429, calcium = 43, iron = 1.9, potassium = 649),
    food("Soy milk", 54, 3, 6, 2, sugar = 4, sodium = 51, vitD = 1, calcium = 123, potassium = 118),

    // Vegetables.
    food("Broccoli, cooked", 35, 2, 7, 0, fiber = 3, calcium = 40, iron = 0.7, potassium = 293),
    food("Spinach, raw", 23, 3, 4, 0, fiber = 2, sodium = 79, calcium = 99, iron = 2.7, potassium = 558),
    food("Carrot, raw", 41, 1, 10, 0, fiber = 3, sugar = 5, sodium = 69, calcium = 33, iron = 0.3, potassium = 320),
    food("Tomato, raw", 18, 1, 4, 0, fiber = 1, sugar = 3, calcium = 10, potassium = 237),
    food("Cucumber, raw", 15, 1, 4, 0, fiber = 1, sugar = 2, calcium = 16, potassium = 147),
    food("Lettuce, romaine", 17, 1, 3, 0, fiber = 2, calcium = 33, iron = 1.0, potassium = 247),
    food("Cabbage, raw", 25, 1, 6, 0, fiber = 3, sugar = 3, calcium = 40, potassium = 170),
    food("Cauliflower, cooked", 23, 2, 4, 0, fiber = 2, calcium = 16, potassium = 142),
    food("Bell pepper, raw", 31, 1, 6, 0, fiber = 2, sugar = 4, calcium = 7, potassium = 211),
    food("Onion, raw", 40, 1, 9, 0, fiber = 2, sugar = 4, calcium = 23, potassium = 146),
    food("Garlic, raw", 149, 6, 33, 1, fiber = 2, sugar = 1, calcium = 181, iron = 1.7, potassium = 401),
    food("Mushrooms, raw", 22, 3, 3, 0, fiber = 1, potassium = 318),
    food("Zucchini, cooked", 17, 1, 3, 0, fiber = 1, calcium = 18, potassium = 264),
    food("Green beans, cooked", 35, 2, 8, 0, fiber = 3, calcium = 44, iron = 0.7, potassium = 146),
    food("Eggplant, cooked", 35, 1, 9, 0, fiber = 3, calcium = 6, potassium = 123),
    food("Asparagus, cooked", 22, 2, 4, 0, fiber = 2, calcium = 23, iron = 0.9, potassium = 224),
    food("Kale, raw", 49, 4, 9, 1, fiber = 4, calcium = 150, iron = 1.5, potassium = 491),
    food("Avocado", 160, 2, 9, 15, fiber = 7, calcium = 12, iron = 0.6, potassium = 485),

    // Fruit.
    food("Banana", 89, 1, 23, 0, fiber = 3, sugar = 12, calcium = 5, potassium = 358),
    food("Apple", 52, 0, 14, 0, fiber = 2, sugar = 10, calcium = 6, potassium = 107),
    food("Orange", 47, 1, 12, 0, fiber = 2, sugar = 9, calcium = 40, potassium = 181),
    food("Grapes", 69, 1, 18, 0, fiber = 1, sugar = 16, calcium = 10, potassium = 191),
    food("Strawberries", 32, 1, 8, 0, fiber = 2, sugar = 5, calcium = 16, potassium = 153),
    food("Blueberries", 57, 1, 14, 0, fiber = 2, sugar = 10, calcium = 6, potassium = 77),
    food("Mango", 60, 1, 15, 0, fiber = 2, sugar = 14, calcium = 11, potassium = 168),
    food("Pineapple", 50, 1, 13, 0, fiber = 1, sugar = 10, calcium = 13, potassium = 109),
    food("Watermelon", 30, 1, 8, 0, sugar = 6, calcium = 7, potassium = 112),
    food("Papaya", 43, 1, 11, 0, fiber = 2, sugar = 8, calcium = 20, potassium = 182),
    food("Pear", 57, 0, 15, 0, fiber = 3, sugar = 10, calcium = 9, potassium = 116),
    food("Peach", 39, 1, 10, 0, fiber = 2, sugar = 8, calcium = 6, potassium = 190),
    food("Grapefruit", 42, 1, 11, 0, fiber = 2, sugar = 7, calcium = 22, potassium = 135),
    food("Kiwi", 61, 1, 15, 1, fiber = 3, sugar = 9, calcium = 34, potassium = 312),
    food("Raisins", 299, 3, 79, 1, fiber = 4, sugar = 59, calcium = 50, iron = 1.9, potassium = 749),
    food("Dates", 282, 3, 75, 0, fiber = 8, sugar = 63, calcium = 39, iron = 1.0, potassium = 656),

    // Nuts, seeds and fats.
    food("Almonds", 579, 21, 22, 50, fiber = 13, sugar = 4, calcium = 269, iron = 3.7, potassium = 733),
    food("Peanuts", 567, 26, 16, 49, fiber = 9, calcium = 92, iron = 4.6, potassium = 705),
    food("Walnuts", 654, 15, 14, 65, fiber = 7, sugar = 3, calcium = 98, iron = 2.9, potassium = 441),
    food("Cashews", 553, 18, 30, 44, fiber = 3, sugar = 6, calcium = 37, iron = 6.7, potassium = 660),
    food("Chia seeds", 486, 17, 42, 31, fiber = 34, calcium = 631, iron = 7.7, potassium = 407),
    food("Sunflower seeds", 584, 21, 20, 51, fiber = 9, sugar = 3, calcium = 78, iron = 5.2, potassium = 645),
    food("Olive oil", 884, 0, 0, 100),
    food("Coconut oil", 862, 0, 0, 100),
    food("Vegetable oil", 884, 0, 0, 100),
    food("Mayonnaise", 680, 1, 1, 75, sodium = 635, potassium = 20),

    // Snacks, sweets, drinks and condiments.
    food("Dark chocolate", 546, 5, 61, 31, fiber = 7, sugar = 48, sodium = 24, calcium = 73, iron = 11.9, potassium = 715),
    food("Milk chocolate", 535, 8, 59, 30, fiber = 3, sugar = 52, sodium = 79, calcium = 189, iron = 2.4, potassium = 372),
    food("Potato chips", 536, 7, 53, 35, fiber = 4, sodium = 525, calcium = 24, iron = 1.6, potassium = 1275),
    food("Ice cream, vanilla", 207, 4, 24, 11, sugar = 21, sodium = 80, calcium = 128, potassium = 199),
    food("Doughnut", 452, 5, 51, 25, fiber = 2, sugar = 23, sodium = 373, calcium = 60, iron = 2.0, potassium = 100),
    food("Cookie, chocolate chip", 488, 6, 64, 24, fiber = 2, sugar = 36, sodium = 350, calcium = 30, iron = 2.6, potassium = 150),
    food("Honey", 304, 0, 82, 0, sugar = 82, calcium = 6, potassium = 52),
    food("Sugar, white", 387, 0, 100, 0, sugar = 100),
    food("Cola", 37, 0, 10, 0, sugar = 10, sodium = 4),
    food("Orange juice", 45, 1, 10, 0, sugar = 8, calcium = 11, potassium = 200),
    food("Beer", 43, 1, 4, 0, potassium = 27),
    food("Coffee, black", 1, 0, 0, 0, potassium = 49),
    food("Tea, unsweetened", 1, 0, 0, 0, potassium = 21),
    food("Ketchup", 101, 1, 26, 0, sugar = 22, sodium = 907, calcium = 15, iron = 0.4, potassium = 281),
    food("Soy sauce", 53, 8, 5, 0, sodium = 5493, calcium = 33, iron = 1.9, potassium = 435),
)

/**
 * The whole search: a blank field answers with every food — the panel pages through it — and
 * anything else is a case-insensitive substring on the name, declaration order kept so related
 * foods stay together.
 */
fun searchCommonFoods(query: String): List<ScannedProduct> {
    val term = query.trim()
    if (term.isEmpty()) return COMMON_FOODS
    return COMMON_FOODS.filter { it.name.contains(term, ignoreCase = true) }
}

/** [vitD] is micrograms and [iron] milligrams *as written here* — the units a reference table and
 * a package label both print — while [Nutrients] stores iron in micrograms, so the conversion
 * happens once, here, rather than in 123 hand-typed rows. Every nutrient defaults to 0, which is
 * what leaves an untouched row's source line untouched. */
private fun food(
    name: String,
    kcal: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    fiber: Int = 0,
    sugar: Int = 0,
    sodium: Int = 0,
    vitD: Int = 0,
    calcium: Int = 0,
    iron: Double = 0.0,
    potassium: Int = 0,
) = ScannedProduct(
    name = name,
    portionAmount = PORTION_G,
    portionUnit = "g",
    calories = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    nutrients = Nutrients(
        fiberG = fiber,
        sugarG = sugar,
        sodiumMg = sodium,
        vitaminDUg = vitD,
        calciumMg = calcium,
        ironUg = (iron * 1000).roundToInt(),
        potassiumMg = potassium,
    ),
)

/**
 * The whole food search: **the user's own foods lead**, then [COMMON_FOODS], then [online] — the
 * Open Food Facts tier, which arrives late and is the least certain of the three, so it goes at the
 * back where it cannot move a page someone is already reading.
 *
 * All three are deduped by the same case-insensitive name key [mergeSuggestions] treats as
 * identity — a custom "Chicken breast, cooked" *replaces* the built-in row rather than sitting
 * beside it, so the search can never offer two answers for one food.
 *
 * The two local halves take the same substring filter, so a query narrows the user's foods exactly
 * as it narrows the built-in list, and neither touches the network. [online] arrives already
 * matched by the server and is not re-filtered: OFF matches brands, categories and labels, so a
 * substring pass over the name would throw away most of what was asked for.
 */
fun searchFoods(
    query: String,
    myFoods: List<ScannedProduct>,
    online: List<ScannedProduct> = emptyList(),
): List<ScannedProduct> {
    val local = if (myFoods.isEmpty()) {
        searchCommonFoods(query)
    } else {
        val term = query.trim()
        val mine = if (term.isEmpty()) myFoods else myFoods.filter { it.name.contains(term, ignoreCase = true) }
        val claimed = mine.mapTo(mutableSetOf()) { it.nameKey() }
        mine + searchCommonFoods(query).filterNot { it.nameKey() in claimed }
    }
    if (online.isEmpty()) return local

    // add() both dedupes against the local tiers and stops OFF listing one product twice, which it
    // does whenever the same package is entered under two codes.
    val seen = local.mapTo(mutableSetOf()) { it.nameKey() }
    return local + online.filter { seen.add(it.nameKey()) }
}

/** Name is the identity of a food the user owns — it is `favorite_food`'s primary key — and it is
 * matched the way [mergeSuggestions] matches one: trimmed and case-insensitively. */
private fun ScannedProduct.nameKey(): String = name.trim().lowercase()
