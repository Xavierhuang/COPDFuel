package com.copdhealthtracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.copdhealthtracker.data.model.OxygenReading
import com.copdhealthtracker.databinding.DialogAddOxygenBinding

class AddOxygenDialog(
    private val onSave: (OxygenReading) -> Unit
) : DialogFragment() {
    
    private var _binding: DialogAddOxygenBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddOxygenBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Oxygen Reading")
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
        val levelStr = binding.oxygenLevelEdit.text.toString().trim()
        
        if (TextUtils.isEmpty(levelStr)) {
            Toast.makeText(requireContext(), "Please enter oxygen level", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val level = levelStr.toIntOrNull()
        if (level == null || level < 0 || level > 100) {
            Toast.makeText(requireContext(), "Please enter a valid oxygen level (0-100)", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val reading = OxygenReading(level = level)
        
        onSave(reading)
        Toast.makeText(requireContext(), "Oxygen reading saved successfully", Toast.LENGTH_SHORT).show()
        return true
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
