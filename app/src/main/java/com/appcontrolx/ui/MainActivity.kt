package com.appcontrolx.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.appcontrolx.BuildConfig
import com.appcontrolx.R
import com.appcontrolx.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d("MainActivity", "MainActivity created")
        setupNavigation()
        showWhatsNewIfNeeded()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
    }
    
    private fun showWhatsNewIfNeeded() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lastVersion = prefs.getInt("last_shown_version", 0)
        val currentVersion = BuildConfig.VERSION_CODE
        
        if (lastVersion < currentVersion) {
            showWhatsNewDialog()
            prefs.edit().putInt("last_shown_version", currentVersion).apply()
        }
    }
    
    private fun showWhatsNewDialog() {
        val whatsNew = """
            |• MVVM Architecture + Hilt DI
            |• Optimized release build (ProGuard)
            |• Beautiful About page with stats
            |• Activity Launcher with expandable groups
            |• Enhanced batch operations
            |• Runtime root/shizuku validation
            |• Enhanced security & input validation
            |• Dark/Light theme toggle
            |• Clean UI improvements
        """.trimMargin()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.whats_new_title, BuildConfig.VERSION_NAME))
            .setMessage(whatsNew)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
