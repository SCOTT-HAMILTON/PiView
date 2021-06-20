package com.sample.piview

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar

class MainPageFragment : Fragment() {
    private var lastUri: Uri? = null
    private lateinit var debugTextView: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_main_page, container, false)
    private fun setCircularProgressIndeterminate(value: Boolean) {
        activity?.runOnUiThread {
            activity?.findViewById<CircularProgressIndicator>(R.id.circularProgressIndicator)
                ?.run {
                visibility = View.GONE
                isIndeterminate = value
                visibility = View.VISIBLE
            }
        }
    }
    private fun updateSelectFileButtonEnabled() {
        if ((activity as? MainActivity)?.allNeededPermissionsGranted == true) {
            println("[log] All needed permissions enabled")
            view?.findViewById<Button>(R.id.selectFileButton)?.run {
                isEnabled = true
            }
        } else {
            println("[log] Not all needed permissions enabled")
            view?.findViewById<Button>(R.id.selectFileButton)?.run {
                isEnabled = false
            }
        }
    }
    private fun appendMessageToDebugView(message: String) {
        activity?.runOnUiThread {
            debugTextView.text = debugTextView.text.toString() + "$message\n"
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) {
            uri: Uri? ->
            println("returned Uri : $uri")
            if (uri != null) {
                lastUri = uri
                (activity as? MainActivity)?.convertUriToPdf(uri)
            }
        }
        debugTextView = view.findViewById(R.id.debugTextView)
        view.findViewById<Button>(R.id.selectFileButton)
            .setOnClickListener {
            openDocumentLauncher.launch(resources.getStringArray(R.array.supported_mime_types))
            setCircularProgressIndeterminate(true)
        }
        (activity as? MainActivity)?.setOnDebugError(::appendMessageToDebugView)
        (activity as? MainActivity)?.setOnNewPermissionsResult {
            updateSelectFileButtonEnabled()
        }
        (activity as? MainActivity)?.setOnConversionResult { succeeded, message ->
            setCircularProgressIndeterminate(false)
            if (succeeded) {
                activity?.runOnUiThread {
                    println("[log-error] Toasting from fragment")
                    (activity?.findViewById(R.id.mainConstraintLayout) as? ConstraintLayout)?.run {
                        Snackbar.make(this,
                            context.getString(R.string.conversion_succeed_info_text),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(context.getString(R.string.open_action_text)) {
                                println("[log] Opening file...")
                                (activity as? MainActivity)?.openPdf()
                            }.show()
                    }
                }
                if (activity == null) {
                    println("[log-error] Couldn't toast from fragment")
                } else {
                    println("[log-error] Should be able to toast from fragment")
                }
            } else {
                appendMessageToDebugView(message)
                activity?.runOnUiThread {
                    (activity?.findViewById(R.id.mainConstraintLayout) as? ConstraintLayout)?.run {
                        Snackbar.make(this,
                            context.getString(R.string.conversion_failed_info_text),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(context.getString(R.string.retry_action_text)) {
                                lastUri?.let {
                                    setCircularProgressIndeterminate(true)
                                    (activity as? MainActivity)?.convertUriToPdf(it)
                                }
                            }
                            .show()
                    }
                }
            }
        }
        updateSelectFileButtonEnabled()
    }
}