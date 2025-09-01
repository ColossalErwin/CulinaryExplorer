package com.luutran.mycookingapp.ui.searchresults

// --- SORTING ---
enum class RecipeSortOption(
    val displayName: String,
    val spoonacularSortValue: String?, // The value Spoonacular API expects for 'sort'
    val defaultDirectionAsc: Boolean = true // Most sorts are asc by default if not specified, but some are desc
) {
    RELEVANCE("Relevance", "meta-score"), // Spoonacular default
    POPULARITY("Popularity", "popularity"),
    HEALTHINESS("Healthiness", "healthiness", defaultDirectionAsc = false), // Higher is better
    TIME_QUICKEST("Time: Quickest", "time", defaultDirectionAsc = true),
    CALORIES_LOWEST("Calories: Lowest", "calories", defaultDirectionAsc = true),
    PROTEIN_HIGHEST("Protein: Highest", "protein", defaultDirectionAsc = false),
}

enum class DietOption(val displayName: String, val spoonacularValue: String) {
    NONE("Any", ""), // Special case for no diet filter
    GLUTEN_FREE("Gluten Free", "gluten free"),
    KETOGENIC("Ketogenic", "ketogenic"),
    VEGETARIAN("Vegetarian", "vegetarian"),
    LACTO_VEGETARIAN("Lacto-Vegetarian", "lacto-vegetarian"),
    OVO_VEGETARIAN("Ovo-Vegetarian", "ovo-vegetarian"),
    VEGAN("Vegan", "vegan"),
    PESCETARIAN("Pescetarian", "pescetarian"),
    PALEO("Paleo", "paleo"),
    PRIMAL("Primal", "primal"),
    LOW_FODMAP("Low FODMAP", "low fodmap"),
    WHOLE30("Whole30", "whole30");

    companion object {
        fun fromSpoonacularValue(value: String?): DietOption? = entries.find { it.spoonacularValue.equals(value, ignoreCase = true) }
        fun default() = NONE
    }
}

enum class IntoleranceOption(val displayName: String, val spoonacularValue: String) {
    DAIRY("Dairy", "dairy"),
    EGG("Egg", "egg"),
    GLUTEN("Gluten", "gluten"),
    GRAIN("Grain", "grain"),
    PEANUT("Peanut", "peanut"),
    SEAFOOD("Seafood", "seafood"),
    SESAME("Sesame", "sesame"),
    SHELLFISH("Shellfish", "shellfish"),
    SOY("Soy", "soy"),
    SULFITE("Sulfite", "sulfite"),
    TREE_NUT("Tree Nut", "tree nut"),
    WHEAT("Wheat", "wheat");

    companion object {
        fun fromSpoonacularValue(value: String): IntoleranceOption? = entries.find { it.spoonacularValue.equals(value, ignoreCase = true) }
    }
}

enum class DishTypeOption(val displayName: String, val spoonacularValue: String) {
    MAIN_COURSE("Main Course", "main course"),
    SIDE_DISH("Side Dish", "side dish"),
    DESSERT("Dessert", "dessert"),
    APPETIZER("Appetizer", "appetizer"),
    SALAD("Salad", "salad"),
    BREAD("Bread", "bread"),
    BREAKFAST("Breakfast", "breakfast"),
    SOUP("Soup", "soup"),
    BEVERAGE("Beverage", "beverage"),
    SAUCE("Sauce", "sauce"),
    MARINADE("Marinade", "marinade"),
    FINGERFOOD("Fingerfood", "fingerfood"),
    SNACK("Snack", "snack"),
    DRINK("Drink", "drink");

    companion object {
        fun fromSpoonacularValue(value: String): DishTypeOption? = entries.find { it.spoonacularValue.equals(value, ignoreCase = true) }
    }
}

enum class CuisineOption(val displayName: String, val spoonacularValue: String) {
    AFRICAN("African", "african"),
    ASIAN("Asian", "asian"),
    AMERICAN("American", "american"),
    BRITISH("British", "british"),
    CAJUN("Cajun", "cajun"),
    CARIBBEAN("Caribbean", "caribbean"),
    CHINESE("Chinese", "chinese"),
    EUROPEAN("European", "european"),
    FRENCH("French", "french"),
    GERMAN("German", "german"),
    GREEK("Greek", "greek"),
    INDIAN("Indian", "indian"),
    IRISH("Irish", "irish"),
    ITALIAN("Italian", "italian"),
    JAPANESE("Japanese", "japanese"),
    JEWISH("Jewish", "jewish"),
    KOREAN("Korean", "korean"),
    LATIN_AMERICAN("Latin American", "latin american"),
    MEDITERRANEAN("Mediterranean", "mediterranean"),
    MEXICAN("Mexican", "mexican"),
    MIDDLE_EASTERN("Middle Eastern", "middle eastern"),
    NORDIC("Nordic", "nordic"),
    SOUTHERN("Southern", "southern"),
    SPANISH("Spanish", "spanish"),
    THAI("Thai", "thai"),
    VIETNAMESE("Vietnamese", "vietnamese");

    companion object {
        fun fromSpoonacularValue(value: String): CuisineOption? = entries.find { it.spoonacularValue.equals(value, ignoreCase = true) }
    }
}

data class AppliedSearchFilters(
    val diet: DietOption = DietOption.NONE,
    val intolerances: Set<IntoleranceOption> = emptySet(),
    val cuisines: Set<CuisineOption> = emptySet(),
    val dishTypes: Set<DishTypeOption> = emptySet(),
    val includeIngredients: String = "",
    val excludeIngredients: String = "",
    val maxReadyTime: Int? = null, // in minutes
) {
    fun hasActiveFilters(): Boolean {
        return diet != DietOption.NONE ||
                intolerances.isNotEmpty() ||
                cuisines.isNotEmpty() ||
                dishTypes.isNotEmpty() ||
                includeIngredients.isNotBlank() ||
                excludeIngredients.isNotBlank() ||
                maxReadyTime != null
    }
}