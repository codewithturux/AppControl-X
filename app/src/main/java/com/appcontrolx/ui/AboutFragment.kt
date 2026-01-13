package com.appcontrolx.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appcontrolx.R
import com.appcontrolx.databinding.FragmentAboutBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * About screen displaying app information with Material 3 design.
 * 
 * Displays:
 * - App icon, name, and version prominently in header
 * - App Info section (Version, Build number, Target SDK)
 * - Developer section
 * - Links section (GitHub, Report issue, Star, Share)
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8
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
        setupLinks()
    }
    
    /**
     * Setup app version information in header and App Info card.
     * Requirements: 8.1, 8.4, 8.5
     */
    private fun setupAppInfo() {
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            // Header version display
            binding.tvVersion.text = getString(R.string.about_version_format, versionName, versionCode)
            
            // App Info card values
            binding.tvVersionValue.text = versionName
            binding.tvBuildValue.text = versionCode.toString()
            
            // Get target SDK from application info
            val appInfo = requireContext().packageManager
                .getApplicationInfo(requireContext().packageName, 0)
            binding.tvTargetSdkValue.text = getString(R.string.detail_sdk_format, appInfo.targetSdkVersion)
            
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to get package info", e)
            binding.tvVersion.text = getString(R.string.about_version_format, "Unknown", 0L)
            binding.tvVersionValue.text = "Unknown"
            binding.tvBuildValue.text = "0"
            binding.tvTargetSdkValue.text = "Unknown"
        }
    }
    
    /**
     * Setup link buttons for GitHub, bug report, star, and share.
     * Requirements: 8.6
     */
    private fun setupLinks() {
        binding.btnGithub.setOnClickListener {
            openUrl(GITHUB_URL)
        }
        
        binding.btnBugReport.setOnClickListener {
            openUrl(GITHUB_ISSUES_URL)
        }
        
        binding.btnRate.setOnClickListener {
            openUrl(GITHUB_STARS_URL)
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
