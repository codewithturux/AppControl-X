package com.appcontrolx.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.appcontrolx.R
import com.appcontrolx.databinding.FragmentAboutBinding
import com.appcontrolx.service.PermissionBridge
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class AboutFragment : Fragment() {
    
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return _binding?.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAppInfo()
        setupSystemInfo()
        setupStats()
        setupLinks()
    }
    
    private fun setupAppInfo() {
        val b = binding ?: return
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            
            b.tvVersion.text = getString(R.string.about_version_format, 
                packageInfo.versionName, packageInfo.longVersionCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get package info", e)
        }
        
        // Current mode
        val mode = PermissionBridge(requireContext()).detectMode()
        b.tvCurrentMode.text = mode.displayName()
    }
    
    private fun setupSystemInfo() {
        val b = binding ?: return
        b.tvDeviceInfo.text = getString(R.string.about_device_format,
            Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Build.MODEL)
        b.tvAndroidVersion.text = getString(R.string.about_android_format,
            Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
    }
    
    private fun setupStats() {
        val b = binding ?: return
        
        lifecycleScope.launch {
            val (userApps, systemApps) = withContext(Dispatchers.IO) {
                val pm = requireContext().packageManager
                val packages = pm.getInstalledPackages(0)
                
                var user = 0
                var system = 0
                
                packages.forEach { pkg ->
                    if ((pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                        system++
                    } else {
                        user++
                    }
                }
                
                Pair(user, system)
            }
            
            b.tvUserAppsCount.text = userApps.toString()
            b.tvSystemAppsCount.text = systemApps.toString()
            
            // Actions count from prefs (placeholder for now)
            b.tvActionsCount.text = "0"
        }
    }
    
    private fun setupLinks() {
        val b = binding ?: return
        
        b.btnGithub.setOnClickListener {
            openUrl("https://github.com/risunCode/AppControl-X")
        }
        
        b.btnShare.setOnClickListener {
            shareApp()
        }
        
        b.btnRate.setOnClickListener {
            openUrl("https://github.com/risunCode/AppControl-X/stargazers")
        }
        
        b.btnBugReport.setOnClickListener {
            openUrl("https://github.com/risunCode/AppControl-X/issues/new")
        }
    }
    
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
        }
    }
    
    companion object {
        private const val TAG = "AboutFragment"
    }
    
    private fun shareApp() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.about_share_text))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.about_share_via)))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
