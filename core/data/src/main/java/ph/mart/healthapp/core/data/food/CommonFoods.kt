package ph.mart.healthapp.core.data.food

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
 * Figures are USDA reference values, rounded to the Int fields [ScannedProduct] holds. Fiber,
 * sugar and sodium are filled where they matter and left at 0 elsewhere — `0` already reads as
 * unknown-or-none everywhere else in the app.
 *
 * ponytail: cooked-weight staples in one flat list, no categories and no synonyms, so "aubergine"
 * finds nothing. A tags column on the row is the upgrade path if that gets reported.
 */
val COMMON_FOODS: List<ScannedProduct> = listOf(
    // Poultry and meat, cooked.
    food("Chicken breast, cooked", 165, 31, 0, 4, sodium = 74),
    food("Chicken thigh, cooked", 209, 26, 0, 11, sodium = 88),
    food("Chicken, roasted with skin", 239, 27, 0, 14, sodium = 82),
    food("Turkey breast, cooked", 135, 30, 0, 1, sodium = 99),
    food("Ground beef, cooked", 250, 26, 0, 15, sodium = 75),
    food("Beef steak, cooked", 206, 31, 0, 8, sodium = 55),
    food("Pork chop, cooked", 231, 26, 0, 13, sodium = 62),
    food("Pork belly, cooked", 518, 9, 0, 53, sodium = 32),
    food("Bacon, cooked", 541, 37, 1, 42, sodium = 1717),
    food("Ham, sliced", 145, 17, 2, 7, sodium = 1203),
    food("Sausage, pork, cooked", 301, 18, 1, 25, sodium = 800),
    food("Lamb, cooked", 258, 25, 0, 17, sodium = 72),

    // Fish and seafood.
    food("Salmon, cooked", 208, 20, 0, 13, sodium = 59),
    food("Tuna, canned in water", 116, 26, 0, 1, sodium = 320),
    food("Tilapia, cooked", 128, 26, 0, 3, sodium = 56),
    food("Cod, cooked", 105, 23, 0, 1, sodium = 78),
    food("Shrimp, cooked", 99, 24, 0, 0, sodium = 111),
    food("Sardines, canned", 208, 25, 0, 11, sodium = 505),
    food("Mackerel, cooked", 262, 24, 0, 18, sodium = 83),
    food("Squid, cooked", 175, 18, 8, 7, sodium = 260),

    // Eggs and dairy.
    food("Egg, whole, boiled", 155, 13, 1, 11, sugar = 1, sodium = 124),
    food("Egg white", 52, 11, 1, 0, sodium = 166),
    food("Milk, whole", 61, 3, 5, 3, sugar = 5, sodium = 43),
    food("Milk, skim", 34, 3, 5, 0, sugar = 5, sodium = 42),
    food("Greek yogurt, plain nonfat", 59, 10, 4, 0, sugar = 3, sodium = 36),
    food("Yogurt, plain whole milk", 61, 4, 5, 3, sugar = 5, sodium = 46),
    food("Cheddar cheese", 403, 25, 1, 33, sodium = 653),
    food("Mozzarella cheese", 300, 22, 2, 22, sodium = 627),
    food("Cottage cheese", 98, 11, 3, 4, sugar = 3, sodium = 364),
    food("Cream cheese", 342, 6, 4, 34, sugar = 3, sodium = 321),
    food("Butter", 717, 1, 0, 81, sodium = 576),

    // Grains, bread, pasta and starches.
    food("White rice, cooked", 130, 3, 28, 0, fiber = 0),
    food("Brown rice, cooked", 123, 3, 26, 1, fiber = 2),
    food("Pasta, cooked", 158, 6, 31, 1, fiber = 2),
    food("Whole wheat pasta, cooked", 124, 5, 27, 1, fiber = 4),
    food("Bread, white", 265, 9, 49, 3, fiber = 3, sugar = 5, sodium = 491),
    food("Bread, whole wheat", 247, 13, 41, 3, fiber = 7, sugar = 6, sodium = 450),
    food("Oats, dry", 389, 17, 66, 7, fiber = 11),
    food("Oatmeal, cooked", 71, 3, 12, 2, fiber = 2),
    food("Cornflakes", 357, 7, 84, 0, sugar = 8, sodium = 729),
    food("Granola", 471, 10, 64, 20, fiber = 7, sugar = 21, sodium = 26),
    food("Quinoa, cooked", 120, 4, 21, 2, fiber = 3),
    food("Couscous, cooked", 112, 4, 23, 0, fiber = 1),
    food("Tortilla, flour", 306, 8, 51, 7, fiber = 3, sodium = 600),
    food("Bagel", 250, 10, 49, 2, fiber = 2, sugar = 5, sodium = 490),
    food("Instant noodles, cooked", 138, 3, 20, 5, sodium = 700),
    food("Rice cakes", 387, 8, 82, 3, fiber = 4, sodium = 30),
    food("Crackers, saltine", 418, 9, 72, 10, fiber = 3, sodium = 941),
    food("Pancake", 227, 6, 28, 10, sugar = 6, sodium = 439),
    food("Potato, boiled", 87, 2, 20, 0, fiber = 2),
    food("Potato, baked", 93, 3, 21, 0, fiber = 2),
    food("French fries", 312, 3, 41, 15, fiber = 4, sodium = 210),
    food("Sweet potato, cooked", 90, 2, 21, 0, fiber = 3, sugar = 7),
    food("Sweet corn, cooked", 96, 3, 21, 2, fiber = 2, sugar = 5),

    // Legumes and soy.
    food("Black beans, cooked", 132, 9, 24, 1, fiber = 9),
    food("Chickpeas, cooked", 164, 9, 27, 3, fiber = 8),
    food("Lentils, cooked", 116, 9, 20, 0, fiber = 8),
    food("Kidney beans, cooked", 127, 9, 23, 1, fiber = 6),
    food("Green peas, cooked", 84, 5, 16, 0, fiber = 6, sugar = 6),
    food("Tofu, firm", 144, 17, 3, 9, fiber = 2, sodium = 14),
    food("Tempeh", 192, 20, 8, 11, sodium = 9),
    food("Edamame, cooked", 121, 12, 9, 5, fiber = 5, sodium = 6),
    food("Peanut butter", 588, 25, 20, 50, fiber = 6, sugar = 9, sodium = 429),
    food("Soy milk", 54, 3, 6, 2, sugar = 4, sodium = 51),

    // Vegetables.
    food("Broccoli, cooked", 35, 2, 7, 0, fiber = 3),
    food("Spinach, raw", 23, 3, 4, 0, fiber = 2, sodium = 79),
    food("Carrot, raw", 41, 1, 10, 0, fiber = 3, sugar = 5, sodium = 69),
    food("Tomato, raw", 18, 1, 4, 0, fiber = 1, sugar = 3),
    food("Cucumber, raw", 15, 1, 4, 0, fiber = 1, sugar = 2),
    food("Lettuce, romaine", 17, 1, 3, 0, fiber = 2),
    food("Cabbage, raw", 25, 1, 6, 0, fiber = 3, sugar = 3),
    food("Cauliflower, cooked", 23, 2, 4, 0, fiber = 2),
    food("Bell pepper, raw", 31, 1, 6, 0, fiber = 2, sugar = 4),
    food("Onion, raw", 40, 1, 9, 0, fiber = 2, sugar = 4),
    food("Garlic, raw", 149, 6, 33, 1, fiber = 2, sugar = 1),
    food("Mushrooms, raw", 22, 3, 3, 0, fiber = 1),
    food("Zucchini, cooked", 17, 1, 3, 0, fiber = 1),
    food("Green beans, cooked", 35, 2, 8, 0, fiber = 3),
    food("Eggplant, cooked", 35, 1, 9, 0, fiber = 3),
    food("Asparagus, cooked", 22, 2, 4, 0, fiber = 2),
    food("Kale, raw", 49, 4, 9, 1, fiber = 4),
    food("Avocado", 160, 2, 9, 15, fiber = 7),

    // Fruit.
    food("Banana", 89, 1, 23, 0, fiber = 3, sugar = 12),
    food("Apple", 52, 0, 14, 0, fiber = 2, sugar = 10),
    food("Orange", 47, 1, 12, 0, fiber = 2, sugar = 9),
    food("Grapes", 69, 1, 18, 0, fiber = 1, sugar = 16),
    food("Strawberries", 32, 1, 8, 0, fiber = 2, sugar = 5),
    food("Blueberries", 57, 1, 14, 0, fiber = 2, sugar = 10),
    food("Mango", 60, 1, 15, 0, fiber = 2, sugar = 14),
    food("Pineapple", 50, 1, 13, 0, fiber = 1, sugar = 10),
    food("Watermelon", 30, 1, 8, 0, sugar = 6),
    food("Papaya", 43, 1, 11, 0, fiber = 2, sugar = 8),
    food("Pear", 57, 0, 15, 0, fiber = 3, sugar = 10),
    food("Peach", 39, 1, 10, 0, fiber = 2, sugar = 8),
    food("Grapefruit", 42, 1, 11, 0, fiber = 2, sugar = 7),
    food("Kiwi", 61, 1, 15, 1, fiber = 3, sugar = 9),
    food("Raisins", 299, 3, 79, 1, fiber = 4, sugar = 59),
    food("Dates", 282, 3, 75, 0, fiber = 8, sugar = 63),

    // Nuts, seeds and fats.
    food("Almonds", 579, 21, 22, 50, fiber = 13, sugar = 4),
    food("Peanuts", 567, 26, 16, 49, fiber = 9),
    food("Walnuts", 654, 15, 14, 65, fiber = 7, sugar = 3),
    food("Cashews", 553, 18, 30, 44, fiber = 3, sugar = 6),
    food("Chia seeds", 486, 17, 42, 31, fiber = 34),
    food("Sunflower seeds", 584, 21, 20, 51, fiber = 9, sugar = 3),
    food("Olive oil", 884, 0, 0, 100),
    food("Coconut oil", 862, 0, 0, 100),
    food("Vegetable oil", 884, 0, 0, 100),
    food("Mayonnaise", 680, 1, 1, 75, sodium = 635),

    // Snacks, sweets, drinks and condiments.
    food("Dark chocolate", 546, 5, 61, 31, fiber = 7, sugar = 48, sodium = 24),
    food("Milk chocolate", 535, 8, 59, 30, fiber = 3, sugar = 52, sodium = 79),
    food("Potato chips", 536, 7, 53, 35, fiber = 4, sodium = 525),
    food("Ice cream, vanilla", 207, 4, 24, 11, sugar = 21, sodium = 80),
    food("Doughnut", 452, 5, 51, 25, fiber = 2, sugar = 23, sodium = 373),
    food("Cookie, chocolate chip", 488, 6, 64, 24, fiber = 2, sugar = 36, sodium = 350),
    food("Honey", 304, 0, 82, 0, sugar = 82),
    food("Sugar, white", 387, 0, 100, 0, sugar = 100),
    food("Cola", 37, 0, 10, 0, sugar = 10, sodium = 4),
    food("Orange juice", 45, 1, 10, 0, sugar = 8),
    food("Beer", 43, 1, 4, 0),
    food("Coffee, black", 1, 0, 0, 0),
    food("Tea, unsweetened", 1, 0, 0, 0),
    food("Ketchup", 101, 1, 26, 0, sugar = 22, sodium = 907),
    food("Soy sauce", 53, 8, 5, 0, sodium = 5493),
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

private fun food(
    name: String,
    kcal: Int,
    proteinG: Int,
    carbsG: Int,
    fatG: Int,
    fiber: Int = 0,
    sugar: Int = 0,
    sodium: Int = 0,
) = ScannedProduct(
    name = name,
    portionAmount = PORTION_G,
    portionUnit = "g",
    calories = kcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    fiberG = fiber,
    sugarG = sugar,
    sodiumMg = sodium,
)
