package com.copdhealthtracker.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.FragmentGuidelinesBinding

class GuidelinesFragment : Fragment() {
    private var _binding: FragmentGuidelinesBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuidelinesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }
    
    private fun setupViews() {
        binding.guidelinesTitle.text = "COPD Dietary Guidelines"
        binding.guidelinesIntro.text = "Proper nutrition plays a vital role in managing COPD symptoms and improving overall health."
        
        // Add guideline items
        val guidelines = listOf(
            Pair("Maintain a Healthy Weight", "Being underweight can reduce respiratory muscle strength, while excess weight can make breathing more difficult. Aim for a healthy weight through balanced nutrition."),
            Pair("Stay Hydrated", "Drinking plenty of fluids helps keep mucus thin and easier to clear from the lungs. Aim for 6-8 glasses of water daily unless otherwise advised by your doctor."),
            Pair("Eat Smaller, More Frequent Meals", "Large meals can make breathing uncomfortable by pushing against your diaphragm. Smaller, more frequent meals can help prevent this discomfort."),
            Pair("Monitor Salt Intake", "Excess sodium can cause fluid retention, which may make breathing more difficult. Choose fresh foods and herbs over salt for flavoring."),
            Pair("Include Antioxidant-Rich Foods", "Foods high in antioxidants can help reduce inflammation in the airways. Fresh fruits and vegetables are excellent sources."),
            Pair("Consider Supplements", "Consult with your healthcare provider about supplements like vitamin D, calcium, and omega-3 fatty acids, which may benefit COPD patients.")
        )
        
        binding.guidelinesContainer.removeAllViews()
        guidelines.forEach { (title, text) ->
            val guidelineView = layoutInflater.inflate(R.layout.item_guideline, binding.guidelinesContainer, false)
            val titleView = guidelineView.findViewById<android.widget.TextView>(R.id.guideline_title)
            val textView = guidelineView.findViewById<android.widget.TextView>(R.id.guideline_text)
            titleView.text = title
            textView.text = text
            binding.guidelinesContainer.addView(guidelineView)
        }
        
        binding.consultText.text = "Always consult with your healthcare provider before making significant changes to your diet."
        
        // Strategy items
        val strategies = listOf(
            Pair("Nutrition Plan", "Focus on protein-rich foods like lean meats, fish, eggs, and plant proteins. Adequate protein intake helps maintain respiratory muscle strength."),
            Pair("Breathing Exercises", "Regular breathing exercises like pursed-lip breathing and diaphragmatic breathing can improve lung function and oxygen levels."),
            Pair("Physical Activity", "Regular, moderate exercise improves cardiovascular health and strengthens respiratory muscles. Consult with your healthcare provider for an appropriate exercise plan.")
        )
        
        binding.strategyGrid.removeAllViews()
        strategies.forEach { (title, text) ->
            val strategyView = layoutInflater.inflate(R.layout.item_strategy, binding.strategyGrid, false)
            val titleView = strategyView.findViewById<android.widget.TextView>(R.id.strategy_title)
            val textView = strategyView.findViewById<android.widget.TextView>(R.id.strategy_text)
            titleView.text = title
            textView.text = text
            binding.strategyGrid.addView(strategyView)
        }
        
        val proteinFullText = "Research shows that COPD patients with adequate protein intake have better outcomes. Protein helps maintain respiratory muscle mass and function, which can decline in COPD patients. Aim for 20-30 g protein per meal."
        val boldPart = "Aim for 20-30 g protein per meal."
        val spannableProtein = android.text.SpannableString(proteinFullText)
        val startIndex = proteinFullText.indexOf(boldPart)
        if (startIndex >= 0) {
            spannableProtein.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                startIndex + boldPart.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.proteinText.text = spannableProtein
        
        // Foods to Embrace
        val foodsToEmbrace = listOf(
            Pair("Fresh Fruits and Vegetables", "Rich in antioxidants and fiber, they help reduce inflammation and support immune function."),
            Pair("Lean Proteins", "Fish, poultry, beans, and tofu provide essential amino acids without excess calories."),
            Pair("Whole Grains", "Brown rice, whole wheat bread, and oats provide sustained energy and important nutrients."),
            Pair("Healthy Fats", "Olive oil, avocados, nuts, and fatty fish contain omega-3s that may help reduce inflammation."),
            Pair("Dairy or Fortified Alternatives", "Good sources of calcium and vitamin D for bone health, especially important if taking steroids.")
        )
        
        binding.foodsEmbraceContainer.removeAllViews()
        foodsToEmbrace.forEach { (name, description) ->
            val foodView = layoutInflater.inflate(R.layout.item_food, binding.foodsEmbraceContainer, false)
            val markerView = foodView.findViewById<android.widget.TextView>(R.id.food_marker)
            val nameView = foodView.findViewById<android.widget.TextView>(R.id.food_name)
            val descView = foodView.findViewById<android.widget.TextView>(R.id.food_description)
            markerView.text = "+"
            markerView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            nameView.text = name
            descView.text = description
            binding.foodsEmbraceContainer.addView(foodView)
        }
        
        // Foods to Limit
        val foodsToLimit = listOf(
            Pair("Processed Foods", "Often high in sodium, preservatives, and artificial ingredients that may worsen inflammation."),
            Pair("Gas-Producing Foods", "Beans, cabbage, and carbonated beverages can cause bloating that makes breathing uncomfortable."),
            Pair("Excessive Salt", "Can lead to fluid retention, making it harder to breathe and potentially raising blood pressure."),
            Pair("Cold Foods", "Very cold foods and beverages may trigger coughing or breathing difficulties in some individuals.")
        )
        
        binding.foodsLimitContainer.removeAllViews()
        foodsToLimit.forEach { (name, description) ->
            val foodView = layoutInflater.inflate(R.layout.item_food, binding.foodsLimitContainer, false)
            val markerView = foodView.findViewById<android.widget.TextView>(R.id.food_marker)
            val nameView = foodView.findViewById<android.widget.TextView>(R.id.food_name)
            val descView = foodView.findViewById<android.widget.TextView>(R.id.food_description)
            markerView.text = "-"
            markerView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            nameView.text = name
            descView.text = description
            binding.foodsLimitContainer.addView(foodView)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
