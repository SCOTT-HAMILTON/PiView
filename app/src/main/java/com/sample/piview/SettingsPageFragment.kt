package com.sample.piview

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsPageFragment : PreferenceFragmentCompat() {
    private val DIALOG_FRAGMENT_TAG = "NumberPickerDialog"
    private lateinit var myPreference: NumberPickerPreference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentFragmentManager.setFragmentResultListener("requestKey", this)
        { key, bundle ->
            if (key == "requestKey") {
                val result = bundle.getInt("result")
                println("Got result $result")
                myPreference.doPersistInt(result)
            } else println("Wrong key : $key")
        }
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (parentFragmentManager.findFragmentByTag(DIALOG_FRAGMENT_TAG) == null) {
            if (preference is NumberPickerPreference) {
                myPreference = preference
                val dialog = NumberPickerPreferenceDialog.newInstance(preference.key)
                dialog.setTargetFragment(this, 0)
                dialog.show(parentFragmentManager, DIALOG_FRAGMENT_TAG)
            } else super.onDisplayPreferenceDialog(preference)
        }
    }
}