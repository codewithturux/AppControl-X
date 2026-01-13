package com.appcontrolx.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appcontrolx.R
import com.appcontrolx.databinding.ActivityMainBinding
import com.appcontrolx.domain.manager.ModeLossAction
import com.appcontrolx.domain.manager.ModeStatus
import com.appcontrolx.domain.manager.ModeWatcher
import com.appcontrolx.ui.components.ModeLossDialog
import com.appcontrolx.ui.history.ActionHistoryBottomSheet
import com.appcontrolx.ui.setup.SetupActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the app after setup is complete.
 * 
 * Contains TabLayout with ViewPager2 for Dashboard and Apps tabs.
 * Overflow menu provides access to Settings, Action Logs, and About.
 * 
 * Requirement 1.1: Header with app icon, title, and overflow menu
 * Requirement 1.3: TabLayout with Dashboard and Apps tabs
 * Requirement 1.4, 1.5: Tab switching via tap and swipe
 * Requirement 1.6: Active tab indicator
 * Requirement 1.7: No bottom navigation bar
 * Requirements 10.1.1, 10.1.2, 10.1.3: Observe mode status on resume and show dialog when mode lost.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    @Inject
    lateinit var modeWatcher: ModeWatcher
    
    // Track if we're currently showing the mode loss dialog to prevent duplicates
    private var isModeLossDialogShowing = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Defensive check: Redirect to SetupActivity if setup is not complete
        if (!SetupActivity.isSetupComplete(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewPagerWithTabs()
        observeModeStatus()
    }
    
    override fun onResume() {
        super.onResume()
        // Verify mode availability on resume (Requirement 10.1.1)
        verifyModeOnResume()
    }
    
    /**
     * Setup toolbar with overflow menu.
     * Requirement 1.1: Header with app icon, title, and overflow menu
     * Requirement 2.1-2.5: Overflow menu items
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            handleMenuItemClick(menuItem)
        }
    }
    
    /**
     * Setup ViewPager2 with TabLayout using TabLayoutMediator.
     * Requirements 1.3, 1.4, 1.5, 1.6
     */
    private fun setupViewPagerWithTabs() {
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                MainPagerAdapter.POSITION_DASHBOARD -> getString(R.string.nav_dashboard)
                MainPagerAdapter.POSITION_APPS -> getString(R.string.nav_apps)
                else -> ""
            }
        }.attach()
    }
    
    /**
     * Handle overflow menu item clicks.
     * Requirements 2.3, 2.4, 2.5
     */
    private fun handleMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_settings -> {
                navigateToSettings()
                true
            }
            R.id.action_logs -> {
                showActionHistoryBottomSheet()
                true
            }
            R.id.action_about -> {
                navigateToAbout()
                true
            }
            else -> false
        }
    }
    
    /**
     * Navigate to Settings screen.
     * Requirement 2.3
     */
    private fun navigateToSettings() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(android.R.id.content, com.appcontrolx.ui.settings.SettingsFragment())
            .addToBackStack("settings")
            .commit()
    }
    
    /**
     * Show Action History bottom sheet.
     * Requirement 2.4
     */
    private fun showActionHistoryBottomSheet() {
        ActionHistoryBottomSheet.newInstance()
            .show(supportFragmentManager, ActionHistoryBottomSheet.TAG)
    }
    
    /**
     * Navigate to About screen.
     * Requirement 2.5
     */
    private fun navigateToAbout() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(android.R.id.content, AboutFragment())
            .addToBackStack("about")
            .commit()
    }
    
    /**
     * Observe mode status changes and show dialog when mode is lost.
     * Requirements 10.1.2, 10.1.3
     */
    private fun observeModeStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                modeWatcher.observeModeStatus().collect { status ->
                    when (status) {
                        is ModeStatus.Lost -> {
                            showModeLossDialog(status)
                        }
                        is ModeStatus.Available -> {
                            // Mode is available, nothing to do
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Verify current mode availability when app resumes from background.
     * Requirement 10.1.1
     */
    private fun verifyModeOnResume() {
        lifecycleScope.launch {
            val status = modeWatcher.verifyCurrentMode()
            if (status is ModeStatus.Lost) {
                showModeLossDialog(status)
            }
        }
    }
    
    /**
     * Show the mode loss dialog with options to retry, switch mode, or continue in view-only.
     * Requirements 10.1.4, 10.1.5, 10.1.6, 10.1.7
     */
    private fun showModeLossDialog(status: ModeStatus.Lost) {
        // Prevent showing multiple dialogs
        if (isModeLossDialogShowing) return
        
        // Check if dialog is already showing
        val existingDialog = supportFragmentManager.findFragmentByTag(ModeLossDialog.TAG)
        if (existingDialog != null) return
        
        isModeLossDialogShowing = true
        
        val dialog = ModeLossDialog.newInstance(
            previousMode = status.previousMode,
            reason = status.reason
        )
        
        dialog.setOnActionSelectedListener { action ->
            handleModeLossAction(action)
        }
        
        dialog.setOnDismissedListener {
            isModeLossDialogShowing = false
        }
        
        dialog.show(supportFragmentManager, ModeLossDialog.TAG)
    }
    
    /**
     * Handle user's selected action from the mode loss dialog.
     * Requirements 10.1.5, 10.1.6, 10.1.7
     */
    private fun handleModeLossAction(action: ModeLossAction) {
        lifecycleScope.launch {
            when (action) {
                ModeLossAction.RETRY -> {
                    // Requirement 10.1.5: Re-check the current mode availability
                    val result = modeWatcher.handleModeLoss(action)
                    if (result.isFailure) {
                        // Mode is still lost, show dialog again
                        val status = modeWatcher.verifyCurrentMode()
                        if (status is ModeStatus.Lost) {
                            showModeLossDialog(status)
                        }
                    } else {
                        showSnackbar(getString(R.string.status_available))
                    }
                }
                
                ModeLossAction.SWITCH_MODE -> {
                    // Requirement 10.1.6: Navigate to mode selection screen
                    SetupActivity.resetSetup(this@MainActivity)
                    startActivity(Intent(this@MainActivity, SetupActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                
                ModeLossAction.CONTINUE_VIEW_ONLY -> {
                    // Requirement 10.1.7: Switch to None mode and disable action buttons
                    val result = modeWatcher.handleModeLoss(action)
                    if (result.isSuccess) {
                        showSnackbar(getString(R.string.mode_view_only))
                    }
                }
            }
        }
    }
    
    /**
     * Show a snackbar message.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
