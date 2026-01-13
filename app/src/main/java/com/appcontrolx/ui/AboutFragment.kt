package com.appcontrolx.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appcontrolx.R
import com.appcontrolx.databinding.FragmentAboutBinding
import com.appcontrolx.domain.executor.PermissionBridge
import com.appcontrolx.domain.manager.ActionLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * About screen displaying app information, version, and links.
 * 
 * Displays:
 * - App name, version, and icon
 * - Current execution mode
 * - Device statistics (user apps, system apps, actions count)
 * - Device info (brand, model, Android version)
 * - Links to GitHub, bug reports, and share functionality
 * - Open source license information
 * - No personal branding prominently displayed
 * 
 * Requirements: 10.2.1, 10.2.2, 10.2.3, 10.2.4, 10.2.5
 */
@AndroidEntryPoint
class AboutFragment : Fragment() {
    
    companion object {
        private const val TAG = "AboutFragment"
        private const val GITHUB_URL = "https://github.com/risunCode/AppControl-X"
        private const val GITHUB_STARS_URL = "https://github.com/risunCode/AppControl-X/stargazers"
        private const val GITHUB_ISSUES_URL = "https://github.com/risunCode/AppControl-X/issues/new"
    }
    
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var permissionBridge: PermissionBridge
    
    @Inject
    lateinit var actionLogger: ActionLogger
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAppInfo()
        setupSystemInfo()
        setupStats()
        setupLinks()
    }
    
    /**
     * Setup app name, version, and current execution mode.
     * Requirements: 10.2.1, 10.2.2
     */
    private fun setupAppInfo() {
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            binding.tvVersion.text = getString(
                R.string.about_version_format,
                packageInfo.versionName,
                versionCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get package info", e)
            binding.tvVersion.text = getString(R.string.about_version_format, "Unknown", 0L)
        }
        
        // Display current execution mode
        binding.tvCurrentMode.text = permissionBridge.mode.displayName
    }
    
    /**
     * Setup device information section.
     */
    private fun setupSystemInfo() {
        // Device brand and model
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
        binding.tvDeviceInfo.text = getString(
            R.string.about_device_format,
            manufacturer,
            Build.MODEL
        )
        
        // Android version and API level
        binding.tvAndroidVersion.text = getString(
            R.string.about_android_format,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT
        )
    }
    
    /**
     * Setup statistics section (user apps, system apps, actions count).
     */
    private fun setupStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (userApps, systemApps, actionsCount) = withContext(Dispatchers.IO) {
                val pm = requireContext().packageManager
                val packages = pm.getInstalledPackages(0)
                
                var user = 0
                var system = 0
                
                packages.forEach { pkg ->
                    val isSystem = (pkg.applicationInfo?.flags 
                        ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                    if (isSystem) {
                        system++
                    } else {
                        user++
                    }
                }
                
                // Get action count from ActionLogger
                val actions = actionLogger.getActionHistory().size
                
                Triple(user, system, actions)
            }
            
            binding.tvUserAppsCount.text = userApps.toString()
            binding.tvSystemAppsCount.text = systemApps.toString()
            binding.tvActionsCount.text = actionsCount.toString()
        }
    }
    
    /**
     * Setup link buttons for GitHub, star, bug report, and share.
     * Requirements: 10.2.3
     */
    private fun setupLinks() {
        binding.btnGithub.setOnClickListener {
            openUrl(GITHUB_URL)
        }
        
        binding.btnRate.setOnClickListener {
            openUrl(GITHUB_STARS_URL)
        }
        
        binding.btnBugReport.setOnClickListener {
            openUrl(GITHUB_ISSUES_URL)
        }
        
        binding.btnShare.setOnClickListener {
            shareApp()
        }
    }
    
    /**
     * Open a URL in the default browser.
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
        }
    }
    
    /**
     * Share the app via Android share sheet.
     */
    private fun shareApp() {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.about_share_text))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.about_share_via)))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share app", e)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
