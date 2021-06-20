// Credits go to https://stackoverflow.com/a/61340341
package com.sample.piview

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import androidx.core.os.bundleOf
import androidx.preference.PreferenceDialogFragmentCompat

class NumberPickerPreferenceDialog : PreferenceDialogFragmentCompat() {
    lateinit var numberPicker: NumberPicker
    override fun onCreateDialogView(context: Context?): View {
        numberPicker = NumberPicker(context)
        numberPicker.minValue = NumberPickerPreference.MIN_VALUE
        numberPicker.maxValue = NumberPickerPreference.MAX_VALUE
        return numberPicker
    }
    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        numberPicker.value = (preference as NumberPickerPreference).getPersistedInt()
    }
    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            numberPicker.clearFocus()
            val newValue: Int = numberPicker.value
            parentFragmentManager.setFragmentResult("requestKey",
                bundleOf("result" to newValue))
        }
    }
    companion object {
        fun newInstance(key: String): NumberPickerPreferenceDialog {
            val fragment = NumberPickerPreferenceDialog()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
           return fragment
        }
    }
}
