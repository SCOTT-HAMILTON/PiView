package com.sample.piview

import android.Manifest.permission.INTERNET
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.vmadalin.easypermissions.EasyPermissions

/**
 * The number of pages (wizard steps) to show in this demo.
 */
private const val NUM_PAGES = 2

class MainActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks {
    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private lateinit var viewPager: ViewPager2
    private var converted_pdf_file: String? = null
    private val REQUEST_CODE_ALL_PERMISSIONS = 0
    private val neededPermissions = arrayOf(INTERNET, WRITE_EXTERNAL_STORAGE)
    val allNeededPermissionsGranted: Boolean
        get() = EasyPermissions.hasPermissions(this, *neededPermissions)
    private var onDebugErrorCallback: ((message: String)->Unit)? = null
    private var onNewPermissionsResultCallback: (()->Unit)? = null
    private var onConversionResultCallback: ((succeeded: Boolean, message: String)->Unit)? = null
    fun setOnDebugError(callback: (String)->Unit) {
        onDebugErrorCallback = callback
    }
    fun setOnNewPermissionsResult(callback: ()->Unit) {
        onNewPermissionsResultCallback = callback
    }
    fun setOnConversionResult(callback: (Boolean, String)->Unit) {
        onConversionResultCallback = callback
    }
    fun onDebugError(message: String) {
        println("[log-error] $message")
        onDebugErrorCallback?.invoke(message)
    }
    fun onConversionFailure(message: String) {
        println("[log-error] Failed Conversion : $message")
        onConversionResultCallback?.invoke(false, message)
    }
    fun onFileConverted(output_file_path: String) {
        println("[log] File Converted to $output_file_path!")
        converted_pdf_file = output_file_path
        onConversionResultCallback?.invoke(true, "")
    }
    fun convertUriToPdf(uri: Uri) {
        val web_address_key = getString(R.string.web_service_address_key)
        val web_port_key = getString(R.string.web_service_port_key)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val saved_pref_web_address = preferences.getString(web_address_key,
            getString(R.string.default_web_service_address))
        val saved_pref_web_port = preferences.getInt(web_port_key, 80)
        println("[log] Saved Web Address : $saved_pref_web_address,"
                +" saved web port : $saved_pref_web_port")
        runOnUiThread(WebServiceFileConvertRunnable(this, uri,
            saved_pref_web_address,saved_pref_web_port))
    }
    fun openPdf() {
        converted_pdf_file?.let {
            runOnUiThread(OpenPdfRunnable(this, it))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        // Instantiate a ViewPager2 and a PagerAdapter.
        viewPager = findViewById(R.id.viewPager)
        // The pager adapter, which provides the pages to the view pager widget.
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter
        // Interface TabLayout with the ViewPager2
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.settingsPageTab_text)
                else -> getString(R.string.mainPageTab_text)
            }
        }.attach()
        if (!allNeededPermissionsGranted) {
            EasyPermissions.requestPermissions(
                host = this,
                rationale = getString(R.string.permission_request_rationale),
                requestCode = REQUEST_CODE_ALL_PERMISSIONS,
                perms = neededPermissions
            )
        }
    }
    override fun onBackPressed() {
        if (viewPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            viewPager.currentItem = viewPager.currentItem - 1
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions,
            grantResults, this)
    }
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        println("RC CODE $requestCode, Permissions granted : $perms")
        onNewPermissionsResultCallback?.invoke()
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        println("RC CODE $requestCode, Permissions denied : $perms")
        onNewPermissionsResultCallback?.invoke()
    }
    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> MainPageFragment()
            1 -> SettingsPageFragment()
            else -> MainPageFragment()
        }
    }
}

