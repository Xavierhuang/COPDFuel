package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.copdhealthtracker.data.model.ExerciseEntry
import com.copdhealthtracker.databinding.DialogAddExerciseBinding

class AddExerciseDialog(
    private val onSave: (ExerciseEntry) -> Unit
) : DialogFragment() {
    
    private var _binding: DialogAddExerciseBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddExerciseBinding.inflate(layoutInflater)
        
        val exerciseTypes = arrayOf(
            "Walking", "Running", "Cycling", "Swimming", "Yoga", 
            "Strength Training", "Breathing Exercises", "Other"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, exerciseTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.exerciseTypeSpinner.adapter = adapter
        
        binding.customExerciseLayout.visibility = View.GONE
        
        binding.exerciseTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (exerciseTypes[position] == "Other") {
                    binding.customExerciseLayout.visibility = View.VISIBLE
                } else {
                    binding.customExerciseLayout.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Exercise")
            .setView(binding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateAndSave()) {
                    dismiss()
                }
            }
        }
        
        return dialog
    }
    
    private fun validateAndSave(): Boolean {
        val selectedPosition = binding.exerciseTypeSpinner.selectedItemPosition
        val exerciseTypes = arrayOf(
            "Walking", "Running", "Cycling", "Swimming", "Yoga", 
            "Strength Training", "Breathing Exercises", "Other"
        )
        var exerciseType = exerciseTypes[selectedPosition]
        
        if (exerciseType == "Other") {
            val customType = binding.customExerciseEdit.text.toString().trim()
            if (TextUtils.isEmpty(customType)) {
                Toast.makeText(requireContext(), "Please enter exercise type", Toast.LENGTH_SHORT).show()
                return false
            }
            exerciseType = customType
        }
        
        val minutesStr = binding.minutesEdit.text.toString().trim()
        if (TextUtils.isEmpty(minutesStr)) {
            Toast.makeText(requireContext(), "Please enter minutes", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val minutes = minutesStr.toIntOrNull()
        if (minutes == null || minutes <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid number of minutes", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val exerciseEntry = ExerciseEntry(
            type = exerciseType,
            minutes = minutes
        )
        
        onSave(exerciseEntry)
        Toast.makeText(requireContext(), "Exercise saved successfully", Toast.LENGTH_SHORT).show()
        return true
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
