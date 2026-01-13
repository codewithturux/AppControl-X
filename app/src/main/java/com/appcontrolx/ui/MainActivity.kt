package com.appcontrolx.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.appcontrolx.R
import com.appcontrolx.databinding.ActivityMainBinding
import com.appcontrolx.domain.manager.ModeLossAction
import com.appcontrolx.domain.manager.ModeStatus
import com.appcontrolx.domain.manager.ModeWatcher
import com.appcontrolx.ui.components.ModeLossDialog
import com.appcontrolx.ui.setup.SetupActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the app after setup is complete.
 * 
 * Contains the Dashboard and navigation to other screens.
 * Integrates ModeWatcher to detect and handle execution mode loss.
 * 
 * Requirement 0.8: Skip Setup_Wizard and show Dashboard directly when setup is complete.
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
        // This handles edge cases like deep links or direct activity launches
        if (!SetupActivity.isSetupComplete(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        observeModeStatus()
    }
    
    override fun onResume() {
        super.onResume()
        // Verify mode availability on resume (Requirement 10.1.1)
        verifyModeOnResume()
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNavigation.setupWithNavController(navController)
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
                    // Reset setup and restart SetupActivity to allow mode re-selection
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
