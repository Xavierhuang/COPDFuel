package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.copdhealthtracker.R
import com.copdhealthtracker.data.model.WeightEntry
import com.copdhealthtracker.databinding.DialogAddWeightBinding

class AddWeightDialog(
    private val onSave: (WeightEntry) -> Unit
) : DialogFragment() {
    
    private var _binding: DialogAddWeightBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddWeightBinding.inflate(layoutInflater)
        
        binding.weightTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.weightInputEdit.hint = if (checkedId == R.id.current_weight_radio) {
                "Enter current weight"
            } else {
                "Enter goal weight"
            }
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Weight")
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
        val weightStr = binding.weightInputEdit.text.toString().trim()
        
        if (TextUtils.isEmpty(weightStr)) {
            Toast.makeText(requireContext(), "Please enter weight", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val weight = weightStr.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            Toast.makeText(requireContext(), "Please enter a valid weight", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val isGoal = binding.goalWeightRadio.isChecked
        
        val weightEntry = WeightEntry(
            weight = weight,
            isGoal = isGoal
        )
        
        onSave(weightEntry)
        Toast.makeText(requireContext(), "Weight saved successfully", Toast.LENGTH_SHORT).show()
        return true
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
