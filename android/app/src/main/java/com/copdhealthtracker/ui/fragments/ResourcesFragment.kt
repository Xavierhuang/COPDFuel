package com.copdhealthtracker.ui.fragments

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.copdhealthtracker.R
import com.copdhealthtracker.databinding.FragmentResourcesBinding
import com.copdhealthtracker.utils.AppApplication
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ResourcesFragment : Fragment() {

    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!
    private var selectedToolIndex = 0

    private data class ToolInfo(
        val name: String,
        val iconRes: Int
    )

    private val tools = listOf(
        ToolInfo("Severity\nAssessment", R.drawable.ic_tool_severity),
        ToolInfo("Exacerbation\nPlan", R.drawable.ic_tool_exacerbation),
        ToolInfo("Pulmonary\nRehab", R.drawable.ic_tool_pulmonary),
        ToolInfo("Medication\nGuide", R.drawable.ic_tool_medication),
        ToolInfo("Resource\nHub", R.drawable.ic_tool_resources)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolCards()
        showContentForTool(0)
    }

    private fun setupToolCards() {
        binding.toolsChipContainer.removeAllViews()
        
        tools.forEachIndexed { index, tool ->
            val cardView = layoutInflater.inflate(R.layout.item_tool_card, binding.toolsChipContainer, false)
            val container = cardView.findViewById<LinearLayout>(R.id.tool_card_container)
            val iconView = cardView.findViewById<ImageView>(R.id.tool_icon)
            val nameView = cardView.findViewById<TextView>(R.id.tool_name)
            
            val isSelected = index == selectedToolIndex
            
            // Set background
            container.setBackgroundResource(
                if (isSelected) R.drawable.tool_card_background_selected
                else R.drawable.tool_card_background
            )
            
            // Set icon with tint
            val drawable = ContextCompat.getDrawable(requireContext(), tool.iconRes)?.mutate()
            drawable?.let {
                DrawableCompat.setTint(
                    it,
                    ContextCompat.getColor(
                        requireContext(),
                        if (isSelected) R.color.colorPrimary else R.color.textTertiary
                    )
                )
                iconView.setImageDrawable(it)
            }
            
            // Set name
            nameView.text = tool.name
            nameView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.colorPrimary else R.color.textTertiary
                )
            )
            
            // Click handler
            container.setOnClickListener {
                selectedToolIndex = index
                setupToolCards()
                showContentForTool(index)
            }
            
            binding.toolsChipContainer.addView(cardView)
        }
    }

    private fun showContentForTool(index: Int) {
        binding.resourcesContentContainer.removeAllViews()
        when (index) {
            0 -> buildSeverityContent()
            1 -> buildActionPlanContent()
            2 -> buildPulmonaryContent()
            3 -> buildMedicationContent()
            4 -> buildResourceHubContent()
        }
    }

    private fun buildSeverityContent() {
        val ctx = requireContext()
        val primaryDarkColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
        val secondaryTextColor = ContextCompat.getColor(ctx, R.color.textSecondary)

        // Title
        binding.resourcesContentContainer.addView(TextView(ctx).apply {
            text = "COPD Severity Assessment"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Disclaimer
        binding.resourcesContentContainer.addView(TextView(ctx).apply {
            text = "This tool provides an estimate of COPD severity based on your answers. It is not a substitute for professional medical assessment. Always consult with your healthcare provider for an accurate diagnosis and treatment plan."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.cardBackgroundBlue))
            setPadding(24, 16, 24, 16)
            (layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = 24
        })

        val fev1Options = listOf("Select FEV1 percentage", "80% or higher", "50-79%", "30-49%", "Less than 30%", "I don't know")
        val numberOptions = listOf("Select number", "0", "1", "2", "3 or more")
        val oxygenOptions = listOf("Select level", "Yes", "No")

        var fev1Str = "Select FEV1 percentage"
        var hospStr = "Select number"
        var flareStr = "Select number"
        var oxygenStr = "Select level"
        var resultView: TextView? = null

        fun addRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
            val row = layoutInflater.inflate(R.layout.item_guideline, binding.resourcesContentContainer, false)
            val titleView = row.findViewById<TextView>(R.id.guideline_title)
            val textView = row.findViewById<TextView>(R.id.guideline_text)
            titleView.text = label
            textView.text = value
            textView.setOnClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle(label)
                    .setItems(options.toTypedArray()) { _, which ->
                        val selected = options[which]
                        onSelect(selected)
                        textView.text = selected
                    }
                    .show()
            }
            binding.resourcesContentContainer.addView(row)
        }

        addRow("What is your latest FEV1 percentage? (If known)", fev1Str, fev1Options) { fev1Str = it }
        addRow("How many times have you been hospitalized for COPD in the past year?", hospStr, numberOptions) { hospStr = it }
        addRow("How many COPD flare-ups (exacerbations) have you had in the past year?", flareStr, numberOptions) { flareStr = it }
        addRow("Do you use supplemental oxygen?", oxygenStr, oxygenOptions) { oxygenStr = it }

        val calcBtn = Button(ctx).apply {
            text = "Calculate Severity"
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            setOnClickListener {
                val fev1 = when (fev1Str) {
                    "80% or higher" -> 80.0
                    "50-79%" -> 65.0
                    "30-49%" -> 40.0
                    "Less than 30%" -> 15.0
                    else -> null
                }
                val hosp = when (hospStr) { "0" -> 0; "1" -> 1; "2" -> 2; "3 or more" -> 3; else -> null }
                val flares = when (flareStr) { "0" -> 0; "1" -> 1; "2" -> 2; "3 or more" -> 3; else -> null }
                val oxy = oxygenStr == "Yes"

                val severity = when {
                    fev1 != null -> when {
                        fev1 >= 80 -> "Mild COPD (GOLD 1)"
                        fev1 >= 50 -> "Moderate COPD (GOLD 2)"
                        fev1 >= 30 -> "Severe COPD (GOLD 3)"
                        else -> "Very Severe COPD (GOLD 4)"
                    }
                    oxy -> "Severe COPD"
                    hosp != null && hosp >= 1 -> "Severe COPD"
                    flares != null && flares >= 2 -> "Moderate COPD"
                    else -> "Please answer the questions above to calculate severity"
                }
                val desc = when {
                    severity.contains("Mild") -> "FEV1 >= 80% predicted. You may have mild symptoms or be asymptomatic. Regular monitoring and lifestyle modifications are recommended."
                    severity.contains("Moderate") && severity.contains("GOLD 2") -> "FEV1 50-79% predicted. Shortness of breath typically develops on exertion. Bronchodilators and pulmonary rehabilitation may help."
                    severity.contains("Severe") && severity.contains("GOLD 3") -> "FEV1 30-49% predicted. Shortness of breath worsens and may limit daily activities. More intensive treatment is typically needed."
                    severity.contains("Very Severe") -> "FEV1 < 30% predicted. Quality of life is significantly impaired. Comprehensive treatment and close monitoring are essential."
                    severity.contains("Severe") -> "Based on your responses, you may have severe COPD. Please consult with your healthcare provider for proper evaluation."
                    severity.contains("Moderate") -> "Based on your responses, you may have moderate COPD. Please consult with your healthcare provider for proper evaluation."
                    else -> ""
                }
                if (desc.isNotEmpty()) {
                    resultView?.text = "$severity\n\n$desc"
                    resultView?.visibility = View.VISIBLE
                    
                    // Save severity assessment to SharedPreferences for report generation
                    val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
                    val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                    prefs.edit()
                        .putString("severity_fev1", fev1Str)
                        .putString("severity_hospitalizations", hospStr)
                        .putString("severity_exacerbations", flareStr)
                        .putString("severity_oxygen", oxygenStr)
                        .putString("severity_result", severity)
                        .putString("severity_description", desc)
                        .putString("severity_assessment_date", dateFormat.format(java.util.Date()))
                        .apply()
                } else {
                    resultView?.text = severity
                    resultView?.visibility = View.VISIBLE
                }
            }
        }
        binding.resourcesContentContainer.addView(calcBtn)

        resultView = TextView(ctx).apply {
            visibility = View.GONE
            textSize = 16f
            setPadding(24, 24, 24, 24)
            setTextColor(primaryDarkColor)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.cardBackgroundBlue))
            (layoutParams as? LinearLayout.LayoutParams)?.topMargin = 24
        }
        binding.resourcesContentContainer.addView(resultView)
    }

    private fun buildActionPlanContent() {
        val ctx = requireContext()
        val container = binding.resourcesContentContainer
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val repo = (requireActivity().application as AppApplication).repository
        val primaryColor = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val primaryDarkColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
        val secondaryTextColor = ContextCompat.getColor(ctx, R.color.textSecondary)
        val cardBgColor = ContextCompat.getColor(ctx, R.color.cardBackgroundBlue)
        val density = resources.displayMetrics.density

        // Title
        container.addView(TextView(ctx).apply {
            text = "COPD Exacerbation Action Plan"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Disclaimer
        container.addView(TextView(ctx).apply {
            text = "This action plan should be created in partnership with your healthcare provider. Use this template to document your personalized plan for managing COPD flare-ups."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setBackgroundColor(cardBgColor)
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        })

        // Important Contacts Section
        container.addView(TextView(ctx).apply {
            text = "Important Contacts"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Contact input fields
        fun addContactField(label: String, prefKey: String, hint: String): android.widget.EditText {
            container.addView(TextView(ctx).apply {
                text = label
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 4)
            })
            val editText = android.widget.EditText(ctx).apply {
                this.hint = hint
                setText(prefs.getString(prefKey, "") ?: "")
                setBackgroundColor(cardBgColor)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (16 * density).toInt() }
            }
            container.addView(editText)
            return editText
        }

        val doctorNameField = addContactField("Doctor's Name", "doctor_name", "Dr. Smith")
        val doctorPhoneField = addContactField("Doctor's Phone", "doctor_phone", "(555) 123-4567")
        val emergencyNameField = addContactField("Emergency Contact Name", "emergency_contact_name", "Jane Doe")
        val emergencyPhoneField = addContactField("Emergency Contact Phone", "emergency_contact_phone", "(555) 987-6543")

        // Medication Plan Section
        container.addView(TextView(ctx).apply {
            text = "Medication Plan"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 16, 0, 16)
        })

        // Daily Medications
        container.addView(TextView(ctx).apply {
            text = "Daily Medications"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        })
        val dailyList = LinearLayout(ctx).apply { 
            orientation = LinearLayout.VERTICAL 
            setBackgroundColor(cardBgColor)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * density).toInt() }
        }
        container.addView(dailyList)

        // Exacerbation Medications
        container.addView(TextView(ctx).apply {
            text = "Exacerbation Medications"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        })
        val exacerbList = LinearLayout(ctx).apply { 
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBgColor)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        }
        container.addView(exacerbList)

        // Load medications
        lifecycleScope.launch {
            repo.getMedicationsByType("daily").collectLatest { list ->
                if (!isAdded) return@collectLatest
                dailyList.removeAllViews()
                if (list.isEmpty()) {
                    dailyList.addView(TextView(ctx).apply { 
                        text = "No daily medications added yet."
                        setTextColor(secondaryTextColor)
                    })
                } else {
                    list.forEach { med ->
                        dailyList.addView(TextView(ctx).apply {
                            text = "${med.name} - ${med.dosage} (${med.frequency})"
                            setPadding(0, 4, 0, 4)
                        })
                    }
                }
            }
        }
        lifecycleScope.launch {
            repo.getMedicationsByType("exacerbation").collectLatest { list ->
                if (!isAdded) return@collectLatest
                exacerbList.removeAllViews()
                if (list.isEmpty()) {
                    exacerbList.addView(TextView(ctx).apply { 
                        text = "No exacerbation medications added yet."
                        setTextColor(secondaryTextColor)
                    })
                } else {
                    list.forEach { med ->
                        exacerbList.addView(TextView(ctx).apply {
                            text = "${med.name} - ${med.dosage} (${med.frequency})"
                            setPadding(0, 4, 0, 4)
                        })
                    }
                }
            }
        }

        // Action Plan Zones Section
        container.addView(TextView(ctx).apply {
            text = "Action Plan Zones"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 16, 0, 16)
        })

        // Green Zone
        fun addZone(title: String, symptoms: List<String>, action: String, bgColor: Int, textColor: Int) {
            container.addView(TextView(ctx).apply {
                text = title
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(textColor)
                setPadding(0, 8, 0, 8)
            })
            symptoms.forEach { symptom ->
                container.addView(TextView(ctx).apply {
                    text = "  $symptom"
                    textSize = 14f
                    setTextColor(secondaryTextColor)
                    setPadding(0, 2, 0, 2)
                })
            }
            container.addView(TextView(ctx).apply {
                text = "Action: $action"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(textColor)
                setBackgroundColor(bgColor)
                setPadding(16, 12, 16, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { 
                    topMargin = (8 * density).toInt()
                    bottomMargin = (16 * density).toInt() 
                }
            })
        }

        addZone(
            "Green Zone: I'm Doing Well",
            listOf(
                "Usual activity and exercise level",
                "Usual amounts of cough and phlegm/mucus",
                "Sleep well at night",
                "Appetite is good"
            ),
            "Take daily medications as prescribed",
            ContextCompat.getColor(ctx, android.R.color.holo_green_light),
            ContextCompat.getColor(ctx, android.R.color.holo_green_dark)
        )

        addZone(
            "Yellow Zone: I'm Having a Bad Day",
            listOf(
                "More breathless than usual",
                "I have less energy for my daily activities",
                "Increased or thicker phlegm/mucus",
                "Using quick relief inhaler/nebulizer more often",
                "Swelling of ankles more than usual",
                "More coughing than usual",
                "I feel like I have a cold",
                "I'm not sleeping well",
                "My appetite is not good"
            ),
            "Continue daily medication and start exacerbation medications as prescribed",
            ContextCompat.getColor(ctx, android.R.color.holo_orange_light),
            ContextCompat.getColor(ctx, android.R.color.holo_orange_dark)
        )

        addZone(
            "Red Zone: I Need Urgent Medical Care",
            listOf(
                "Severe shortness of breath, even at rest",
                "Not able to do any activity because of breathing",
                "Not able to sleep because of breathing",
                "Fever or shaking chills",
                "Feeling confused or very drowsy",
                "Chest pains",
                "Coughing up blood"
            ),
            "Call 911 or have someone take you to the emergency room",
            ContextCompat.getColor(ctx, android.R.color.holo_red_light),
            ContextCompat.getColor(ctx, android.R.color.holo_red_dark)
        )

        // Additional Instructions Section
        container.addView(TextView(ctx).apply {
            text = "Additional Instructions from Your Doctor"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 16, 0, 8)
        })

        val instructions = android.widget.EditText(ctx).apply {
            hint = "Enter any additional instructions from your doctor here..."
            minLines = 4
            gravity = android.view.Gravity.TOP
            setBackgroundColor(cardBgColor)
            setPadding(16, 16, 16, 16)
            setText(prefs.getString("action_plan_instructions", "") ?: "")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * density).toInt() }
        }
        container.addView(instructions)

        // Save Button
        container.addView(Button(ctx).apply {
            text = "Save Plan"
            setBackgroundColor(primaryColor)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            setOnClickListener {
                prefs.edit()
                    .putString("doctor_name", doctorNameField.text.toString())
                    .putString("doctor_phone", doctorPhoneField.text.toString())
                    .putString("emergency_contact_name", emergencyNameField.text.toString())
                    .putString("emergency_contact_phone", emergencyPhoneField.text.toString())
                    .putString("action_plan_instructions", instructions.text.toString())
                    .apply()
                android.widget.Toast.makeText(ctx, "Action plan saved!", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun buildPulmonaryContent() {
        val container = binding.resourcesContentContainer
        val ctx = requireContext()
        val primaryColor = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val primaryDarkColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
        val secondaryTextColor = ContextCompat.getColor(ctx, R.color.textSecondary)

        // Main Title
        container.addView(TextView(ctx).apply {
            text = "Pulmonary Rehabilitation"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Introduction
        container.addView(TextView(ctx).apply {
            text = "Pulmonary rehabilitation is a comprehensive program that combines exercise, education, and support to help people with COPD breathe better, get stronger, and improve their quality of life. Always consult with your healthcare provider before starting any exercise program."
            textSize = 16f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 32)
        })

        // Benefits Section Title
        container.addView(TextView(ctx).apply {
            text = "Benefits of Pulmonary Rehabilitation"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Benefits
        val benefits = listOf(
            Pair("Improved Exercise Capacity", "Pulmonary rehabilitation can help you walk further and perform daily activities with less breathlessness."),
            Pair("Better Quality of Life", "Many people report feeling better overall and having more energy for the activities they enjoy."),
            Pair("Reduced Hospital Admissions", "Regular participation in pulmonary rehabilitation can reduce your risk of COPD exacerbations requiring hospitalization."),
            Pair("Increased Strength", "Strengthening exercises help counter muscle loss that often occurs with COPD and improve your ability to perform daily tasks."),
            Pair("Better Breathing Control", "Learning proper breathing techniques helps you manage breathlessness during activities and reduce anxiety."),
            Pair("Social Support", "Meeting others with similar conditions provides emotional support and motivation to maintain your exercise program.")
        )

        benefits.forEach { (title, desc) ->
            container.addView(TextView(ctx).apply {
                text = title
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(primaryColor)
                setPadding(0, 0, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = desc
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 0, 0, 16)
            })
        }

        // Finding a Program Section
        container.addView(TextView(ctx).apply {
            text = "Finding a Pulmonary Rehabilitation Program"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 16, 0, 16)
        })

        container.addView(TextView(ctx).apply {
            text = "Pulmonary rehabilitation programs are typically offered at hospitals, outpatient clinics, or community centers. To find a program near you:\n\n" +
                "• Ask your pulmonologist or primary care physician for a referral\n" +
                "• Contact your local hospital or lung health association\n" +
                "• Check with your insurance provider for covered programs\n" +
                "• Visit the American Lung Association website for program directories"
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 24)
        })

        // Find Programs Button
        container.addView(Button(ctx).apply {
            text = "Find Programs Near Me"
            setBackgroundColor(primaryColor)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            setOnClickListener {
                // Navigate to ProgramsNearMeFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProgramsNearMeFragment())
                    .addToBackStack(null)
                    .commit()
            }
        })

        // Home Exercise Program Section
        container.addView(TextView(ctx).apply {
            text = "Home Exercise Program"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 32, 0, 8)
        })

        container.addView(TextView(ctx).apply {
            text = "While a supervised pulmonary rehabilitation program is ideal, these exercises can be performed at home to complement your program or when a formal program isn't available."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 24)
        })

        // Breathing Exercises
        container.addView(TextView(ctx).apply {
            text = "Breathing Exercises"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, 0, 0, 8)
        })

        container.addView(TextView(ctx).apply {
            text = "Techniques to improve breathing efficiency and control"
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 16)
        })

        val breathingExercises = listOf(
            Triple("Pursed-Lip Breathing", "Breathe in through your nose for 2 counts, then breathe out slowly through pursed lips for 4 counts. This helps control breathlessness and slows your breathing rate.", "Recommended frequency: 5-10 minutes, 4-5 times daily"),
            Triple("Diaphragmatic Breathing", "Place one hand on your chest and the other on your abdomen. Breathe in through your nose, feeling your abdomen rise. Breathe out through pursed lips while gently pressing on your abdomen.", "Recommended frequency: 5-10 minutes, 3-4 times daily"),
            Triple("Segmental Breathing", "Focus on directing air to different parts of your lungs by placing hands on specific areas of your chest or sides while breathing deeply.", "Recommended frequency: 5 minutes, 2-3 times daily")
        )

        breathingExercises.forEach { (title, desc, freq) ->
            container.addView(TextView(ctx).apply {
                text = title
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = desc
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 0, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = freq
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.ITALIC)
                setTextColor(primaryColor)
                setPadding(0, 0, 0, 16)
            })
        }

        // Endurance Training
        container.addView(TextView(ctx).apply {
            text = "Endurance Training"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, 16, 0, 8)
        })

        container.addView(TextView(ctx).apply {
            text = "Activities to improve cardiovascular fitness and stamina"
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 16)
        })

        val enduranceExercises = listOf(
            Triple("Walking", "Start with short distances and gradually increase. Use pursed-lip breathing while walking. Stop and rest if you become too breathless.", "Recommended frequency: Start with 5-10 minutes daily, gradually increase to 20-30 minutes"),
            Triple("Stationary Cycling", "Adjust resistance to a comfortable level. Maintain good posture and use pursed-lip breathing.", "Recommended frequency: Start with 5-10 minutes daily, gradually increase to 15-20 minutes"),
            Triple("Swimming/Water Exercises", "The buoyancy of water supports your body, making movement easier. The humidity can also help your breathing.", "Recommended frequency: 20-30 minutes, 2-3 times weekly")
        )

        enduranceExercises.forEach { (title, desc, freq) ->
            container.addView(TextView(ctx).apply {
                text = title
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = desc
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 0, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = freq
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.ITALIC)
                setTextColor(primaryColor)
                setPadding(0, 0, 0, 16)
            })
        }

        // Strength Training
        container.addView(TextView(ctx).apply {
            text = "Strength Training"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, 16, 0, 8)
        })

        container.addView(TextView(ctx).apply {
            text = "Exercises to strengthen respiratory and peripheral muscles"
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 16)
        })

        val strengthExercises = listOf(
            Triple("Upper Body Strengthening", "Use light weights or resistance bands for arm raises, bicep curls, and shoulder presses. Focus on proper breathing throughout.", "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly"),
            Triple("Lower Body Strengthening", "Perform chair stands, leg extensions, and calf raises to strengthen legs. These help with daily activities like standing and walking.", "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly"),
            Triple("Core Strengthening", "Seated abdominal contractions and gentle back extensions help improve posture and breathing mechanics.", "Recommended frequency: 8-12 repetitions, 2-3 sets, 2-3 times weekly")
        )

        strengthExercises.forEach { (title, desc, freq) ->
            container.addView(TextView(ctx).apply {
                text = title
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = desc
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 0, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = freq
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.ITALIC)
                setTextColor(primaryColor)
                setPadding(0, 0, 0, 16)
            })
        }

        // Important Warning
        container.addView(TextView(ctx).apply {
            text = "Important: Always start slowly and progress gradually. Stop any exercise that causes severe shortness of breath, chest pain, or dizziness. Keep your rescue inhaler nearby during exercise."
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark))
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.backgroundGray))
            setPadding(24, 16, 24, 16)
            (layoutParams as? LinearLayout.LayoutParams)?.setMargins(0, 16, 0, 24)
        })

        // Track Your Progress
        container.addView(TextView(ctx).apply {
            text = "Track Your Progress"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 24, 0, 16)
        })

        container.addView(TextView(ctx).apply {
            text = "Keeping track of your exercise sessions helps you see your progress and stay motivated. Consider tracking:\n\n" +
                "• Exercise duration and frequency\n" +
                "• Distance walked or steps taken\n" +
                "• Breathlessness levels before, during, and after exercise\n" +
                "• How you feel overall after each session"
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 32)
        })

        // Start Exercise Journey Button
        container.addView(Button(ctx).apply {
            text = "Start Exercise Journal"
            setBackgroundColor(primaryColor)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            }
            setOnClickListener {
                (activity as? com.copdhealthtracker.MainActivity)?.switchToTracking()
            }
        })

        // Add the pulmonary rehab exercises image at the end - full width
        // Use negative margins to counteract the container's 20dp padding
        val containerPadding = (20 * resources.displayMetrics.density).toInt()
        
        val imageView = android.widget.ImageView(ctx).apply {
            setImageResource(R.drawable.pulmonary_rehab_exercises)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Negative margins to extend to full width
                marginStart = -containerPadding
                marginEnd = -containerPadding
                topMargin = (24 * resources.displayMetrics.density).toInt()
                bottomMargin = (24 * resources.displayMetrics.density).toInt()
            }
        }
        
        container.addView(imageView)
    }

    private var expandedMedicationCategory: String? = null

    private fun buildMedicationContent() {
        val ctx = requireContext()
        val container = binding.resourcesContentContainer
        val primaryColor = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val primaryDarkColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
        val secondaryTextColor = ContextCompat.getColor(ctx, R.color.textSecondary)
        val cardBgColor = ContextCompat.getColor(ctx, R.color.cardBackgroundBlue)
        val density = resources.displayMetrics.density

        // Header Title
        container.addView(TextView(ctx).apply {
            text = "COPD Medication Guide"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Header Description
        container.addView(TextView(ctx).apply {
            text = "This guide provides general information about COPD medications. Your doctor will prescribe medications based on your specific needs. Always follow your healthcare provider's instructions about your medications."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setBackgroundColor(cardBgColor)
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        })

        // Featured COPD Inhalers Section
        container.addView(TextView(ctx).apply {
            text = "Featured COPD Inhalers"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Buttons row for Symbicort and Breztri
        val buttonsRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        }

        // Symbicort Guide Button
        buttonsRow.addView(Button(ctx).apply {
            text = "Symbicort Guide"
            setBackgroundColor(primaryColor)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginEnd = (8 * density).toInt() }
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.symbicort.com/"))
                try { startActivity(intent) } catch (_: Exception) { }
            }
        })

        // Breztri Guide Button
        buttonsRow.addView(Button(ctx).apply {
            text = "Breztri Guide"
            setBackgroundColor(primaryColor)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { marginStart = (8 * density).toInt() }
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.breztri.com/"))
                try { startActivity(intent) } catch (_: Exception) { }
            }
        })
        container.addView(buttonsRow)

        // Medication Types Section Title
        container.addView(TextView(ctx).apply {
            text = "Medication Types"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD or android.graphics.Typeface.ITALIC)
            setTextColor(primaryColor)
            setPadding(0, 0, 0, 16)
        })

        // All 10 medication types with icons
        data class MedType(val title: String, val id: String, val iconRes: Int)
        val medicationTypes = listOf(
            MedType("Bronchodilators", "bronchodilators", R.drawable.ic_med_bronchodilator),
            MedType("Inhaled Corticosteroids", "ics", R.drawable.ic_med_ics),
            MedType("Combination Inhalers", "combination", R.drawable.ic_med_combination),
            MedType("Phosphodiesterase-4 Inhibitors", "pde4", R.drawable.ic_med_pde4),
            MedType("Antibiotics", "antibiotics", R.drawable.ic_med_antibiotics),
            MedType("Systemic Corticosteroids", "systemic", R.drawable.ic_med_systemic),
            MedType("Methylxanthines", "methylxanthines", R.drawable.ic_med_methylxanthines),
            MedType("Mucolytics/Expectorants", "mucolytics", R.drawable.ic_med_mucolytics),
            MedType("Biologics Medications for COPD", "biologics", R.drawable.ic_med_biologics),
            MedType("Nebulizer Medications", "nebulizer", R.drawable.ic_med_nebulizer)
        )

        // Create grid layout - 3 columns per row
        val chunkedTypes = medicationTypes.chunked(3)
        
        chunkedTypes.forEach { rowItems ->
            val typesRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            }

            rowItems.forEach { medType ->
                val typeCard = com.google.android.material.card.MaterialCardView(ctx).apply {
                    radius = 12f * density
                    cardElevation = 2f * density
                    setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.backgroundWhite))
                    strokeWidth = (1 * density).toInt()
                    strokeColor = ContextCompat.getColor(ctx, R.color.borderGray)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        marginEnd = (4 * density).toInt()
                        marginStart = (4 * density).toInt()
                    }
                    isClickable = true
                    isFocusable = true
                }

                val cardContent = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(
                        (8 * density).toInt(),
                        (16 * density).toInt(),
                        (8 * density).toInt(),
                        (16 * density).toInt()
                    )
                }

                // Icon
                cardContent.addView(android.widget.ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        (36 * density).toInt(),
                        (36 * density).toInt()
                    ).apply { bottomMargin = (8 * density).toInt() }
                    setImageResource(medType.iconRes)
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                })

                cardContent.addView(TextView(ctx).apply {
                    text = medType.title
                    textSize = 11f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, R.color.textPrimary))
                    gravity = Gravity.CENTER
                    maxLines = 2
                })

                typeCard.addView(cardContent)
                
                typeCard.setOnClickListener {
                    expandedMedicationCategory = if (expandedMedicationCategory == medType.id) null else medType.id
                    container.removeAllViews()
                    buildMedicationContent()
                }
                
                typesRow.addView(typeCard)
            }

            // Fill remaining space if row has less than 3 items
            val emptySlots = 3 - rowItems.size
            repeat(emptySlots) {
                typesRow.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }

            container.addView(typesRow)
        }

        // Show expanded content if a category is selected
        if (expandedMedicationCategory != null) {
            val expandedContent = getExpandedMedicationContent(expandedMedicationCategory!!)
            if (expandedContent != null) {
                val expandedCard = com.google.android.material.card.MaterialCardView(ctx).apply {
                    radius = 12f * density
                    cardElevation = 2f * density
                    setCardBackgroundColor(cardBgColor)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { 
                        topMargin = (16 * density).toInt()
                        bottomMargin = (16 * density).toInt() 
                    }
                }

                val expandedCardContent = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(
                        (16 * density).toInt(),
                        (16 * density).toInt(),
                        (16 * density).toInt(),
                        (16 * density).toInt()
                    )
                }

                expandedCardContent.addView(TextView(ctx).apply {
                    text = expandedContent.first
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(primaryDarkColor)
                    setPadding(0, 0, 0, (8 * density).toInt())
                })

                expandedContent.second.forEach { item ->
                    expandedCardContent.addView(TextView(ctx).apply {
                        text = "  $item"
                        textSize = 14f
                        setTextColor(secondaryTextColor)
                        setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
                    })
                }

                expandedCard.addView(expandedCardContent)
                container.addView(expandedCard)
            }
        }

        // Watch Inhaler Technique Videos Button
        container.addView(Button(ctx).apply {
            text = "Watch Inhaler Technique Videos"
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.colorSuccess))
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { 
                topMargin = (24 * density).toInt()
                bottomMargin = (16 * density).toInt()
            }
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.copdfoundation.org/Learn-More/Educational-Materials-Resources/Educational-Video-Series.aspx"))
                try { startActivity(intent) } catch (_: Exception) { }
            }
        })

        // Important note at bottom
        container.addView(TextView(ctx).apply {
            text = "Important: Always consult with your healthcare provider before starting, stopping, or changing any medication. This guide is for informational purposes only."
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.textTertiary))
            setTypeface(null, android.graphics.Typeface.ITALIC)
            setPadding(0, 16, 0, 0)
        })
    }

    private fun getExpandedMedicationContent(id: String): Pair<String, List<String>>? {
        return when (id) {
            "bronchodilators" -> Pair("Bronchodilators", listOf(
                "Short-acting (SABAs): Albuterol, Levalbuterol",
                "Short-acting (SAMAs): Ipratropium",
                "Long-acting (LABAs): Salmeterol, Formoterol, Indacaterol",
                "Long-acting (LAMAs): Tiotropium, Aclidinium, Umeclidinium"
            ))
            "ics" -> Pair("Inhaled Corticosteroids", listOf(
                "Fluticasone",
                "Budesonide",
                "Beclomethasone",
                "Mometasone"
            ))
            "combination" -> Pair("Combination Inhalers", listOf(
                "LABA + LAMA: Anoro Ellipta, Stiolto Respimat",
                "LABA + ICS: Advair, Symbicort, Breo Ellipta",
                "Triple Therapy: Trelegy Ellipta, Breztri Aerosphere"
            ))
            "pde4" -> Pair("Phosphodiesterase-4 (PDE4) Inhibitors", listOf(
                "Roflumilast (Daliresp)",
                "Used for severe COPD with chronic bronchitis",
                "Helps reduce exacerbations"
            ))
            "antibiotics" -> Pair("Antibiotics", listOf(
                "Azithromycin (Z-pack)",
                "Amoxicillin-clavulanate (Augmentin)",
                "Doxycycline",
                "Levofloxacin"
            ))
            "systemic" -> Pair("Systemic Corticosteroids", listOf(
                "Prednisone",
                "Methylprednisolone",
                "Dexamethasone",
                "Used short-term during exacerbations"
            ))
            "methylxanthines" -> Pair("Methylxanthines", listOf(
                "Theophylline (Theo-24, Elixophyllin)",
                "Older class of bronchodilators",
                "Used less frequently due to side effects"
            ))
            "mucolytics" -> Pair("Mucolytics/Expectorants", listOf(
                "N-acetylcysteine (NAC)",
                "Carbocysteine",
                "Guaifenesin",
                "Help thin and loosen mucus"
            ))
            "biologics" -> Pair("Biologics Medications for COPD", listOf(
                "Mepolizumab (Nucala) - for eosinophilic COPD",
                "Benralizumab (Fasenra)",
                "Dupilumab (Dupixent)",
                "Newer targeted therapies"
            ))
            "nebulizer" -> Pair("Nebulizer Medications", listOf(
                "Albuterol nebulizer solution",
                "Ipratropium nebulizer solution",
                "Budesonide (Pulmicort Respules)",
                "Combination: Albuterol + Ipratropium (DuoNeb)"
            ))
            else -> null
        }
    }

    private fun buildResourceHubContent() {
        val ctx = requireContext()
        val container = binding.resourcesContentContainer
        val primaryColor = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val primaryDarkColor = ContextCompat.getColor(ctx, R.color.colorPrimaryDark)
        val secondaryTextColor = ContextCompat.getColor(ctx, R.color.textSecondary)
        val cardBgColor = ContextCompat.getColor(ctx, R.color.cardBackgroundBlue)
        val density = resources.displayMetrics.density

        // Title
        container.addView(TextView(ctx).apply {
            text = "COPD Resource Hub"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        // Description
        container.addView(TextView(ctx).apply {
            text = "This resource hub provides links to trusted organizations, educational materials, and support groups to help you better understand and manage your COPD."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setBackgroundColor(cardBgColor)
            setPadding(24, 16, 24, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (24 * density).toInt() }
        })

        // COPD Organizations Section
        container.addView(TextView(ctx).apply {
            text = "COPD Organizations"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 0, 0, 16)
        })

        data class Organization(val name: String, val description: String, val helpline: String?, val url: String)
        val organizations = listOf(
            Organization("American Lung Association", "Provides education, advocacy and research to improve lung health and prevent lung disease.", "1-800-LUNGUSA", "https://www.lung.org"),
            Organization("COPD Foundation", "Dedicated to improving the lives of those affected by COPD through research, education, early diagnosis, and enhanced therapy.", "1-866-316-COPD", "https://www.copdfoundation.org"),
            Organization("Global Initiative for Chronic Obstructive Lung Disease (GOLD)", "Works to improve prevention and treatment of COPD through a global network.", null, "https://goldcopd.org")
        )

        organizations.forEach { org ->
            container.addView(TextView(ctx).apply {
                text = org.name
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(primaryColor)
                setPadding(0, 8, 0, 4)
            })
            container.addView(TextView(ctx).apply {
                text = org.description
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 0, 0, 4)
            })
            if (org.helpline != null) {
                container.addView(TextView(ctx).apply {
                    text = "Helpline: ${org.helpline}"
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(primaryDarkColor)
                    setPadding(0, 0, 0, 4)
                })
            }
            container.addView(Button(ctx).apply {
                text = "Visit Website"
                setBackgroundResource(R.drawable.btn_link_primary)
                setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                textSize = 14f
                setAllCaps(false)
                setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (16 * density).toInt() }
                elevation = 2 * density
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(org.url))
                    try { startActivity(intent) } catch (_: Exception) { }
                }
            })
        }

        // Educational Resources Section
        container.addView(TextView(ctx).apply {
            text = "Educational Resources"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 24, 0, 16)
        })

        val nhlbiVideosUrl = "https://www.nhlbi.nih.gov/health-topics/education-and-awareness/copd-learn-more-breathe-better/copd-videos"
        val videoCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBgColor)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }
        videoCard.addView(TextView(ctx).apply {
            text = "Short, Comprehensive COPD Videos"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
        })
        videoCard.addView(TextView(ctx).apply {
            text = "Videos"
            textSize = 12f
            setTextColor(primaryColor)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4, 0, 4)
        })
        videoCard.addView(TextView(ctx).apply {
            text = "Animations, PSAs, and videos from NHLBI on COPD risk factors, signs and symptoms, treatment options, and more."
            textSize = 14f
            setTextColor(secondaryTextColor)
        })
        videoCard.addView(Button(ctx).apply {
            text = "Access Resource"
            setBackgroundResource(R.drawable.btn_link_primary)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            textSize = 14f
            setAllCaps(false)
            setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * density).toInt() }
            elevation = 2 * density
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nhlbiVideosUrl))
                try { startActivity(intent) } catch (e: Exception) {
                    android.util.Log.e("ResourcesFragment", "Could not open URL: $nhlbiVideosUrl", e)
                }
            }
        })
        container.addView(videoCard)

        val livingWellUrl = "https://www.livingwellwithcopd.com/"
        val livingWellCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBgColor)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }
        livingWellCard.addView(TextView(ctx).apply {
            text = "Living Well with COPD"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
        })
        livingWellCard.addView(TextView(ctx).apply {
            text = "Website"
            textSize = 12f
            setTextColor(primaryColor)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4, 0, 4)
        })
        livingWellCard.addView(TextView(ctx).apply {
            text = "Practical tips for managing daily life with COPD."
            textSize = 14f
            setTextColor(secondaryTextColor)
        })
        livingWellCard.addView(Button(ctx).apply {
            text = "Access Resource"
            setBackgroundResource(R.drawable.btn_link_primary)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            textSize = 14f
            setAllCaps(false)
            setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * density).toInt() }
            elevation = 2 * density
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(livingWellUrl))
                try { startActivity(intent) } catch (e: Exception) {
                    android.util.Log.e("ResourcesFragment", "Could not open URL: $livingWellUrl", e)
                }
            }
        })
        container.addView(livingWellCard)

        val nutritionUrl = "https://www.lung.org/lung-health-diseases/lung-disease-lookup/copd/living-with-copd/nutrition"
        val nutritionCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cardBgColor)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * density).toInt() }
        }
        nutritionCard.addView(TextView(ctx).apply {
            text = "COPD and Nutrition"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
        })
        nutritionCard.addView(TextView(ctx).apply {
            text = "Guide"
            textSize = 12f
            setTextColor(primaryColor)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 4, 0, 4)
        })
        nutritionCard.addView(TextView(ctx).apply {
            text = "How nutrition supports breathing and overall health in COPD."
            textSize = 14f
            setTextColor(secondaryTextColor)
        })
        nutritionCard.addView(Button(ctx).apply {
            text = "Access Resource"
            setBackgroundResource(R.drawable.btn_link_primary)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            textSize = 14f
            setAllCaps(false)
            setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * density).toInt() }
            elevation = 2 * density
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nutritionUrl))
                try { startActivity(intent) } catch (e: Exception) {
                    android.util.Log.e("ResourcesFragment", "Could not open URL: $nutritionUrl", e)
                }
            }
        })
        container.addView(nutritionCard)

        // Support Groups Section
        container.addView(TextView(ctx).apply {
            text = "Support Groups"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 24, 0, 16)
        })

        data class SupportGroup(val name: String, val type: String, val description: String, val url: String)
        val supportGroups = listOf(
            SupportGroup("Right2Breathe", "Online", "The Right2Breathe Pulmonary Chat is a free online chat and live video program where medical experts provide education and answer questions about living with COPD.", "https://right2breathe.org/"),
            SupportGroup("Better Breathers Club", "In-person & Virtual", "In-person and virtual support groups organized by the American Lung Association.", "https://www.lung.org/help-support/better-breathers-club/better-breathers-club-meetings"),
            SupportGroup("COPD360social", "Online", "Online community platform for individuals with COPD and their caregivers.", "https://www.copdfoundation.org/COPD360social/Community/Get-Involved.aspx")
        )

        supportGroups.forEach { group ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(cardBgColor)
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (12 * density).toInt() }
            }
            card.addView(TextView(ctx).apply {
                text = group.name
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(primaryDarkColor)
            })
            card.addView(TextView(ctx).apply {
                text = group.type
                textSize = 12f
                setTextColor(primaryColor)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 4, 0, 4)
            })
            card.addView(TextView(ctx).apply {
                text = group.description
                textSize = 14f
                setTextColor(secondaryTextColor)
            })
            card.addView(Button(ctx).apply {
                text = "Visit Website"
                setBackgroundResource(R.drawable.btn_link_primary)
                setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                textSize = 14f
                setAllCaps(false)
                setPadding((24 * density).toInt(), (14 * density).toInt(), (24 * density).toInt(), (14 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (12 * density).toInt() }
                elevation = 2 * density
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(group.url))
                    try { startActivity(intent) } catch (e: Exception) {
                        android.util.Log.e("ResourcesFragment", "Could not open URL: ${group.url}", e)
                    }
                }
            })
            container.addView(card)
        }

        // COVID-19 and COPD Section
        container.addView(TextView(ctx).apply {
            text = "COVID-19 and COPD"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 24, 0, 16)
        })

        container.addView(TextView(ctx).apply {
            text = "Special Considerations for COPD Patients"
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            setPadding(0, 0, 0, 8)
        })

        container.addView(TextView(ctx).apply {
            text = "People with COPD may be at higher risk for severe illness from COVID-19. It's important to take extra precautions and stay updated with the latest guidance."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 12)
        })

        val covidTips = listOf(
            "Continue taking your COPD medications as prescribed",
            "Maintain at least a 30-day supply of your medications",
            "Follow recommendations for vaccination",
            "Practice physical distancing and wear masks when appropriate",
            "Have an emergency action plan in case you develop COVID-19 symptoms"
        )

        val bulletIndentPx = (18 * density).toInt()
        covidTips.forEach { tip ->
            val bulletText = "  \u2022 $tip"
            val spannable = SpannableString(bulletText).apply {
                setSpan(LeadingMarginSpan.Standard(0, bulletIndentPx), 0, length, 0)
            }
            container.addView(TextView(ctx).apply {
                text = spannable
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 4, 0, 4)
            })
        }

        // Smoking Cessation and COPD Section
        container.addView(TextView(ctx).apply {
            text = "Quit Smoking with COPD"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 24, 0, 16)
        })

        container.addView(TextView(ctx).apply {
            text = "When you have COPD, quitting smoking is more than just a lifestyle change—it's a medical intervention. Because COPD often comes with high nicotine dependence and increased rates of depression, medications are frequently the \"bridge\" needed to make a quit attempt successful."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 16)
        })

        // 1. Smoking Cessation Medications
        container.addView(TextView(ctx).apply {
            text = "1. Smoking Cessation Medications"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 8, 0, 12)
        })

        container.addView(TextView(ctx).apply {
            text = "For 2026, there are three primary paths for medication, plus a promising newcomer."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 12)
        })

        data class MedRow(val medication: String, val howItWorks: String, val sideEffects: String)
        val medTable = listOf(
            MedRow("NRT (Nicotine Replacement)", "Provides nicotine without the toxic smoke. Best used as a \"Combo\": Patch (steady) + Gum/Lozenge (rescue).", "Skin irritation, vivid dreams, or jaw soreness."),
            MedRow("Varenicline (Chantix)", "Blocks the \"pleasure\" receptors in the brain and reduces withdrawal. Currently the most effective monotherapy.", "Nausea (mitigated by food/water) and vivid dreams."),
            MedRow("Bupropion (Zyban)", "Originally an antidepressant, it reduces the urge to smoke. Good for those with co-occurring depression.", "Dry mouth and insomnia."),
            MedRow("Cytisinicline", "New for 2026. A plant-based pill similar to Varenicline but shown in recent trials to be highly effective and well-tolerated in COPD patients.", "Mild nausea or headache.")
        )
        medTable.forEach { row ->
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(cardBgColor)
                setPadding(16, 12, 16, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (8 * density).toInt() }
            }
            card.addView(TextView(ctx).apply {
                text = row.medication
                textSize = 15f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(primaryColor)
            })
            card.addView(TextView(ctx).apply {
                text = "How it works: ${row.howItWorks}"
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 4, 0, 2)
            })
            card.addView(TextView(ctx).apply {
                text = "Common side effects: ${row.sideEffects}"
                textSize = 14f
                setTextColor(secondaryTextColor)
            })
            container.addView(card)
        }

        // 2. Impact on COPD Symptoms
        container.addView(TextView(ctx).apply {
            text = "2. Impact on COPD Symptoms"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 16, 0, 12)
        })

        container.addView(TextView(ctx).apply {
            text = "Medications don't just help you quit; they indirectly improve your COPD management by removing the constant irritation of smoke."
            textSize = 14f
            setTextColor(secondaryTextColor)
            setPadding(0, 0, 0, 8)
        })

        val impactBullets = listOf(
            "Better Inhaler Efficacy: When you stop smoking, the inflammation in your airways begins to subside. This allows your bronchodilators (like Albuterol or Spiriva) to reach deeper into the lungs and work more effectively.",
            "Reduced \"Mucus Plugs\": Smoking paralyzes the cilia (tiny hairs) that clear mucus. Quitting \"wakes them up,\" helping you clear phlegm more easily and reducing the risk of infections.",
            "Stabilized Lung Function: While lung damage from COPD is permanent, medications stop the \"accelerated decline.\" You go from losing lung function at a smoker's pace back to a natural aging pace."
        )
        impactBullets.forEach { bullet ->
            val bulletText = "  \u2022 $bullet"
            val spannable = SpannableString(bulletText).apply {
                setSpan(LeadingMarginSpan.Standard(0, bulletIndentPx), 0, length, 0)
            }
            container.addView(TextView(ctx).apply {
                text = spannable
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 4, 0, 4)
            })
        }

        // 3. Important Considerations for COPD
        container.addView(TextView(ctx).apply {
            text = "3. Important Considerations for COPD"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(primaryDarkColor)
            setPadding(0, 16, 0, 12)
        })

        val considerBullets = listOf(
            "Depression & Anxiety: COPD is physically and mentally taxing. Because some quit-smoking meds (like Bupropion or Varenicline) affect brain chemistry, it's vital to monitor your mood. Recent large-scale studies (including those published in early 2026) have confirmed these are generally safe for COPD patients but should be managed by your doctor.",
            "The \"Quit-Cough\": It sounds counterintuitive, but many COPD patients cough more for the first week after quitting. This is actually a sign of your lungs cleaning themselves out. Don't let it discourage you!",
            "Paced Quitting: For those not ready to stop today, \"Reduce to Quit\" programs using NRT can help you slowly lower your daily cigarette count, making the final quit day less of a shock to the system."
        )
        considerBullets.forEach { bullet ->
            val bulletText = "  \u2022 $bullet"
            val spannable = SpannableString(bulletText).apply {
                setSpan(LeadingMarginSpan.Standard(0, bulletIndentPx), 0, length, 0)
            }
            container.addView(TextView(ctx).apply {
                text = spannable
                textSize = 14f
                setTextColor(secondaryTextColor)
                setPadding(0, 4, 0, 4)
            })
        }

        container.addView(TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (32 * density).toInt()
            )
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
