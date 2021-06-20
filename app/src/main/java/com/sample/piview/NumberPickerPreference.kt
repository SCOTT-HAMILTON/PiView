// Credits go to https://stackoverflow.com/a/61340341
package com.sample.piview

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

class NumberPickerPreference(context: Context?, attrs: AttributeSet?) :
    DialogPreference(context, attrs) {
    override fun getSummary(): CharSequence {
        return getPersistedInt(INITIAL_VALUE).toString()
    }
    fun getPersistedInt() = super.getPersistedInt(INITIAL_VALUE)
    fun doPersistInt(value: Int) {
        super.persistInt(value)
        notifyChanged()
    }
    companion object {
        // allowed range
        const val INITIAL_VALUE = 80
        const val MIN_VALUE = 0
        const val MAX_VALUE = 100_000
    }
}