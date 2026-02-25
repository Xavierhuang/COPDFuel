package com.copdhealthtracker.ui.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.FragmentRecipesBinding

class RecipesFragment : Fragment() {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = "COPD-Friendly Recipes"
        binding.subtitle.text = "Nutritious and delicious recipes that are easy to prepare and gentle on your respiratory system."
        addMealIdeasContent()
    }

    private fun addMealIdeasContent() {
        val ctx = requireContext()
        val container = binding.recipesContentContainer
        val density = resources.displayMetrics.density
        val primaryColor = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val primaryDark = ContextCompat.getColor(ctx, R.color.textPrimary)
        val secondaryColor = ContextCompat.getColor(ctx, R.color.textSecondary)
        val bulletIndentPx = (18 * density).toInt()
        val pad = (16 * density).toInt()
        val padSmall = (12 * density).toInt()
        val marginBottom = (16 * density).toInt()
        val itemMargin = (8 * density).toInt()

        fun sectionTitle(text: String): TextView {
            return TextView(ctx).apply {
                this.text = text
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(primaryColor)
                setPadding(pad, padSmall, pad, padSmall)
            }
        }

        fun numberedItem(number: Int, title: String, bullets: List<String>, imageRes: Int? = null): LinearLayout {
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.recipe_item_bg)
                setPadding(pad, padSmall, pad, padSmall)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = itemMargin }
            }
            imageRes?.let { res ->
                val imageHeight = (160 * density).toInt()
                card.addView(ImageView(ctx).apply {
                    setImageResource(res)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        imageHeight
                    ).apply { bottomMargin = padSmall }
                })
            }
            card.addView(TextView(ctx).apply {
                this.text = "$number. $title"
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(primaryDark)
                setPadding(0, 0, 0, (4 * density).toInt())
            })
            bullets.forEachIndexed { index, bullet ->
                val bulletText = "  \u2022 $bullet"
                val spannable = SpannableString(bulletText).apply {
                    setSpan(LeadingMarginSpan.Standard(0, bulletIndentPx), 0, length, 0)
                }
                card.addView(TextView(ctx).apply {
                    text = spannable
                    textSize = 14f
                    setTextColor(secondaryColor)
                    setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
                })
            }
            return card
        }

        data class RecipeItem(val number: Int, val title: String, val bullets: List<String>, val imageRes: Int? = null)

        fun addSection(titleText: String, items: List<RecipeItem>) {
            val sectionCard = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.recipe_section_header_bg)
                setPadding(pad, padSmall, pad, pad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = marginBottom }
            }
            sectionCard.addView(sectionTitle(titleText))
            items.forEach { item ->
                sectionCard.addView(numberedItem(item.number, item.title, item.bullets, item.imageRes))
            }
            container.addView(sectionCard)
        }

        addSection("Breakfast Ideas", listOf(
            RecipeItem(1, "Greek Yogurt + Berries + Walnuts", listOf("Protein", "Omega-3 fats"), R.drawable.greek_yogurt_berries_walnuts),
            RecipeItem(2, "Scrambled Eggs + Spinach + Whole-grain Toast", listOf("Protein", "Vitamin A"), R.drawable.scrambled_eggs_spinach_whole_grain_toast),
            RecipeItem(3, "Oatmeal + Ground Flaxseed + Blueberries", listOf("Omega-3 fats", "Fiber"), R.drawable.flaxseed_oatmeal),
            RecipeItem(4, "Cottage Cheese + Peaches + Chia Seeds", listOf("Protein", "Calcium"), R.drawable.cottage_cheese_peaches_chia_seeds),
            RecipeItem(5, "Smoothie: Milk + Berries + Spinach + Greek Yogurt + Ground Flaxseed", listOf("Protein", "Antioxidants"), R.drawable.smoothie_berries_spinach)
        ))

        addSection("Lunch & Dinner Ideas", listOf(
            RecipeItem(1, "Baked Salmon + Sweet Potatoes + Asparagus", listOf("Omega-3 fats", "Protein"), R.drawable.salmon_asparagus_sweet_potato),
            RecipeItem(2, "Chicken & Vegetable Stir-Fry + Brown Rice", listOf("Protein", "Vitamin C"), R.drawable.chicken_vegetable_stir_fry),
            RecipeItem(3, "Turkey & Avocado Sandwich on Whole-grain Bread", listOf("Protein", "Healthy fats"), R.drawable.turkey_avocado_sandwich),
            RecipeItem(4, "Rotisserie Chicken + Brown Rice + Roasted Mixed Vegetables", listOf("Protein", "Antioxidants"), R.drawable.rotisserie_chicken_rice_vegetables),
            RecipeItem(5, "Shrimp Tacos with Cabbage Slaw", listOf("Protein", "Omega-3 fats"), R.drawable.shrimp_tacos),
            RecipeItem(6, "Chili with Lean Ground Beef, Beans & Tomatoes", listOf("Protein", "Iron"), R.drawable.chili_beef_beans_tomatoes),
            RecipeItem(7, "Baked Chicken Breast + Quinoa + Roasted Brussels Sprouts", listOf("Protein", "Magnesium"), R.drawable.chicken_breast_quinoa_brussels)
        ))

        addSection("Snack Ideas", listOf(
            RecipeItem(1, "Apple Slices + Peanut Butter", listOf("Healthy fats", "Fiber"), R.drawable.apple_slices_peanut_butter),
            RecipeItem(2, "Hummus + Baby Carrots or Bell Peppers", listOf("Plant protein", "Vitamin A"), R.drawable.hummus_carrots_peppers),
            RecipeItem(3, "Cheese Stick + Whole-grain Crackers", listOf("Protein", "Calcium"), R.drawable.cheese_stick_crackers),
            RecipeItem(4, "Hard-boiled Egg", listOf("Protein", "Vitamin D"), R.drawable.hard_boiled_egg),
            RecipeItem(5, "Trail Mix (Nuts + Seeds + Dried Fruit)", listOf("Healthy fats", "Magnesium"), R.drawable.trail_mix)
        ))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
