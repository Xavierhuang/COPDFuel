package com.copdhealthtracker.labelscan

/**
 * Nutrients beyond the four macros that a scan can fill in. Each one already has a field on
 * FoodEntry, in [unit]. [usdaIds] are FoodData Central nutrient ids, most preferred first;
 * [keywords] are how the row is printed on a Nutrition Facts panel, most specific first.
 */
enum class ExtraNutrient(val unit: String, val usdaIds: List<Int>, val keywords: List<String>) {
    FIBER("g", listOf(1079), listOf("dietary fiber", "fiber")),
    ADDED_SUGARS("g", listOf(1235), listOf("added sugars", "added sugar")),
    SATURATED_FAT("g", listOf(1258), listOf("saturated fat", "sat. fat", "sat fat")),
    CHOLESTEROL("mg", listOf(1253), listOf("cholesterol", "cholest.")),
    SODIUM("mg", listOf(1093), listOf("sodium")),
    POTASSIUM("mg", listOf(1092), listOf("potassium", "potas.")),
    CALCIUM("mg", listOf(1087), listOf("calcium")),
    IRON("mg", listOf(1089), listOf("iron")),

    // 1114 is vitamin D in mcg; 1110 is the same nutrient reported in IU.
    VITAMIN_D("mcg", listOf(1114, 1110), listOf("vitamin d", "vit. d", "vit d"));

    companion object {
        const val USDA_VITAMIN_D_IU = 1110
        const val MCG_PER_IU_VITAMIN_D = 0.025
    }
}
