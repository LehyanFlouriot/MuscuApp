package com.lehyanflouriot.muscuapp.core.domain

import com.lehyanflouriot.muscuapp.core.model.Nutrition
import java.time.DayOfWeek
import java.time.LocalDate

/** Aisles of a supermarket, in the order the shopping list shows them. */
enum class Aisle { PRODUCE, MEAT_FISH, DAIRY, STARCHES, GROCERY, SUPPLEMENTS, OTHER }

/** How a quantity is counted. Weights are in grams and volumes in millilitres. */
enum class AmountUnit { GRAM, MILLILITER, PIECE, TABLESPOON, TEASPOON, OTHER }

/** A quantity read from the text of a food: "150 g", "1,5 c. à s.", "2"... [label] names an [AmountUnit.OTHER] unit. */
data class Amount(val value: Double, val unit: AmountUnit, val label: String = "")

private val leadingNumber = Regex("^(\\d+(?:[.,]\\d+)?)(?:\\s*/\\s*(\\d+))?\\s*(.*)$")
private val tablespoon = Regex("^(c\\.?a\\.?s|cs|cuilleres?asoupe)$")
private val teaspoon = Regex("^(c\\.?a\\.?c|cc|cuilleres?acafe)$")

/** Reads "150 g", "25 cl", "1,5 c. à s." or "1/2". Null when the text does not start with a number. */
fun parseQuantity(text: String): Amount? {
    val match = leadingNumber.matchEntire(text.trim()) ?: return null
    var value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    match.groupValues[2].toDoubleOrNull()?.takeIf { it > 0 }?.let { value /= it }
    val unitText = match.groupValues[3].trim()
    val unit = normalizeFood(unitText).replace(" ", "").removeSuffix(".")
    return when {
        unit.isEmpty() -> Amount(value, AmountUnit.PIECE)
        unit == "g" || unit == "gr" || unit == "gramme" || unit == "grammes" -> Amount(value, AmountUnit.GRAM)
        unit == "kg" -> Amount(value * 1_000, AmountUnit.GRAM)
        unit == "ml" -> Amount(value, AmountUnit.MILLILITER)
        unit == "cl" -> Amount(value * 10, AmountUnit.MILLILITER)
        unit == "dl" -> Amount(value * 100, AmountUnit.MILLILITER)
        unit == "l" -> Amount(value * 1_000, AmountUnit.MILLILITER)
        tablespoon.matches(unit) -> Amount(value, AmountUnit.TABLESPOON)
        teaspoon.matches(unit) -> Amount(value, AmountUnit.TEASPOON)
        else -> Amount(value, AmountUnit.OTHER, unitText)
    }
}

/** Volume of spoons, in ml: a tablespoon is 15 ml and a teaspoon 5 ml. */
fun spoonsVolume(amounts: List<Amount>): Double = amounts.sumOf {
    when (it.unit) {
        AmountUnit.TABLESPOON -> it.value * 15
        AmountUnit.TEASPOON -> it.value * 5
        else -> 0.0
    }
}

/** The same food written in the singular or the plural, with or without accents, is bought once. */
fun shoppingKey(name: String): String = normalizeFood(name).replace(Regex("\\s+"), " ").removeSuffix("s")

// The longest word found in the name of a food decides its aisle: "beurre de cacahuète" goes
// with the peanut butter, not with the butter. A final "s" or "x" is allowed after each word.
private val aisleWords: List<Pair<String, Aisle>> = listOf(
    Aisle.PRODUCE to listOf(
        "banane", "pomme", "poire", "mangue", "fruit", "fruits rouges", "citron", "orange", "clementine", "kiwi",
        "fraise", "framboise", "myrtille", "ananas", "peche", "abricot", "raisin", "avocat", "tomate", "brocoli",
        "courgette", "haricots verts", "haricot vert", "poivron", "epinard", "salade", "ratatouille", "legume",
        "carotte", "oignon", "ail", "echalote", "champignon", "concombre", "aubergine", "patate douce",
        "pomme de terre", "pommes de terre", "chou", "chou-fleur", "poireau", "petits pois", "mais", "herbe",
        "legumes couscous",
    ),
    Aisle.MEAT_FISH to listOf(
        "poulet", "dinde", "boeuf", "steak", "hache", "veau", "porc", "jambon", "viande", "thon", "saumon",
        "cabillaud", "colin", "merlu", "lieu", "sardine", "maquereau", "crevette", "poisson", "oeuf",
    ),
    Aisle.DAIRY to listOf(
        "lait", "fromage", "fromage blanc", "fromage frais", "skyr", "yaourt", "yogourt", "parmesan", "emmental",
        "mozzarella", "feta", "comte", "creme", "beurre", "cottage",
    ),
    Aisle.STARCHES to listOf(
        "riz", "pates", "semoule", "quinoa", "boulgour", "flocons", "avoine", "muesli", "granola", "cereale",
        "pain", "wrap", "tortilla", "galette", "lentille", "pois chiche", "pois chiches", "haricots rouges",
        "farine", "couscous",
    ),
    Aisle.GROCERY to listOf(
        "huile", "miel", "confiture", "beurre de cacahuete", "puree d'amande", "puree de cacahuete", "cacao",
        "chocolat", "sauce", "sauce tomate", "cannelle", "epice", "sel", "poivre", "vinaigre", "moutarde",
        "amande", "noix", "noisette", "noix de cajou", "cajou", "pistache", "raisins secs", "fruits secs",
        "graine", "the", "cafe", "sucre", "bouillon", "conserve",
    ),
    Aisle.SUPPLEMENTS to listOf("creatine", "whey", "proteine", "vitamine", "omega", "complement"),
).flatMap { (aisle, words) -> words.map { it to aisle } }.sortedByDescending { it.first.length }

private fun String.containsWordOrPlural(word: String): Boolean =
    Regex("(^|[^a-z0-9])" + Regex.escape(word) + "[sx]?($|[^a-z0-9])").containsMatchIn(this)

fun aisleOf(name: String): Aisle {
    val food = normalizeFood(name)
    return aisleWords.firstOrNull { (word, _) -> food.containsWordOrPlural(word) }?.second ?: Aisle.OTHER
}

/** A food to buy, with everything the meals of the period need. */
data class ShoppingItem(
    val key: String,
    val name: String,
    val aisle: Aisle,
    /** One per unit, in a stable order: grams, then millilitres, then pieces, then spoons. */
    val amounts: List<Amount>,
    /** Meals where the food comes without a quantity ("brocolis"). */
    val unmeasured: Int,
    val days: List<DayOfWeek>,
)

data class ShoppingList(val from: LocalDate, val to: LocalDate, val items: List<ShoppingItem>) {
    /** The aisles that have something to buy, in the order of the supermarket. */
    val aisles: List<Pair<Aisle, List<ShoppingItem>>>
        get() = items.groupBy { it.aisle }.toSortedMap().map { (aisle, foods) -> aisle to foods }
}

/**
 * Everything the meals from [from] to [to] (included) need: each food once, with its quantities
 * added up. The plan followed each day is used, with the foods replaced for a day.
 */
fun buildShoppingList(nutrition: Nutrition, from: LocalDate, to: LocalDate): ShoppingList {
    class Needs(val name: String) {
        val amounts = LinkedHashMap<Pair<AmountUnit, String>, Double>()
        var unmeasured = 0
        val days = sortedSetOf<DayOfWeek>()
    }
    val needs = LinkedHashMap<String, Needs>()
    var date = from
    while (date <= to) {
        buildNutritionDay(nutrition, date)?.meals?.forEach { meal ->
            meal.foods.forEach foods@{ food ->
                val name = food.name.trim()
                if (name.isEmpty()) return@foods
                val need = needs.getOrPut(shoppingKey(name)) { Needs(name) }
                need.days += date.dayOfWeek
                val amount = parseQuantity(food.quantity)
                if (amount == null) {
                    need.unmeasured++
                } else {
                    need.amounts.merge(amount.unit to amount.label, amount.value, Double::plus)
                }
            }
        }
        date = date.plusDays(1)
    }
    val items = needs.map { (key, need) ->
        ShoppingItem(
            key = key,
            name = need.name,
            aisle = aisleOf(need.name),
            amounts = need.amounts.entries
                .map { (unit, value) -> Amount(value, unit.first, unit.second) }
                .sortedBy { it.unit.ordinal },
            unmeasured = need.unmeasured,
            days = need.days.toList(),
        )
    }.sortedWith(compareBy({ it.aisle.ordinal }, { normalizeFood(it.name) }))
    return ShoppingList(from, to, items)
}

/** The week a shopping list is first shown for: from Saturday on, people shop for the next one. */
fun defaultShoppingWeek(today: LocalDate): LocalDate = today.plusDays(2).startOfWeek()
