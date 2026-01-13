package com.appcontrolx.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.appcontrolx.R
import com.appcontrolx.databinding.ActivitySetupBinding
import com.appcontrolx.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Setup wizard activity with ViewPager2 for wizard steps.
 * 
 * Steps:
 * 1. Welcome - Introduction to the app
 * 2. Mode Selection - Select Root/Shizuku/View-Only mode
 * 3. Complete - Setup complete confirmation
 * 
 * Requirements: 0.1, 0.2, 0.8
 */
@AndroidEntryPoint
class SetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySetupBinding
    
    private val setupPrefs by lazy { 
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE) 
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if setup is already complete (Requirement 0.8)
        // When app is launched after setup complete, skip Setup_Wizard and show Dashboard directly
        if (setupPrefs.getBoolean(PREFS_SETUP_COMPLETE, false)) {
            startMainActivity()
            return
        }
        
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
        setupBackPressHandler()
    }
    
    /**
     * Handle back press to navigate between wizard steps.
     * On welcome screen, exit the app.
     * On other screens, go back to previous step.
     * On complete screen, prevent going back (user must tap "Go to Dashboard").
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentItem = binding.viewPager.currentItem
                when (currentItem) {
                    STEP_WELCOME -> {
                        // Exit the app on welcome screen
                        finish()
                    }
                    STEP_COMPLETE -> {
                        // On complete screen, don't allow going back
                        // User must tap "Go to Dashboard" to proceed
                        // This prevents users from getting stuck in an incomplete state
                    }
                    else -> {
                        // Go back to previous step
                        binding.viewPager.currentItem = currentItem - 1
                    }
                }
            }
        })
    }
    
    private fun setupViewPager() {
        // Setup wizard steps: Welcome -> Mode Selection -> Complete
        val fragments = listOf<Fragment>(
            SetupWelcomeFragment(),
            SetupModeFragment(),
            SetupCompleteFragment()
        )
        
        binding.viewPager.adapter = SetupPagerAdapter(this, fragments)
        binding.viewPager.isUserInputEnabled = false // Disable swipe navigation
        
        setupDotsIndicator(fragments.size)
        binding.dotsIndicator.visibility = View.VISIBLE
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDotsIndicator(position)
            }
        })
    }
    
    private fun setupDotsIndicator(count: Int) {
        binding.dotsIndicator.removeAllViews()
        for (i in 0 until count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.dot_size),
                    resources.getDimensionPixelSize(R.dimen.dot_size)
                ).apply {
                    marginStart = resources.getDimensionPixelSize(R.dimen.dot_margin)
                    marginEnd = resources.getDimensionPixelSize(R.dimen.dot_margin)
                }
                setBackgroundResource(R.drawable.dot_inactive)
            }
            binding.dotsIndicator.addView(dot)
        }
        updateDotsIndicator(0)
    }
    
    private fun updateDotsIndicator(position: Int) {
        for (i in 0 until binding.dotsIndicator.childCount) {
            binding.dotsIndicator.getChildAt(i).setBackgroundResource(
                if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }
    
    /**
     * Navigate to the next step in the wizard.
     */
    fun nextStep() {
        val currentItem = binding.viewPager.currentItem
        val itemCount = binding.viewPager.adapter?.itemCount ?: 0
        
        if (currentItem < itemCount - 1) {
            binding.viewPager.currentItem = currentItem + 1
        }
    }
    
    /**
     * Navigate to a specific step in the wizard.
     */
    fun goToStep(step: Int) {
        val itemCount = binding.viewPager.adapter?.itemCount ?: 0
        if (step in 0 until itemCount) {
            binding.viewPager.currentItem = step
        }
    }
    
    /**
     * Complete the setup wizard and navigate to main activity (Dashboard).
     * Saves setup_complete flag to preferences.
     * 
     * Requirement 0.7: Save setup_complete flag and navigate to Dashboard
     * Requirement 0.8: After this, subsequent app launches will skip Setup_Wizard
     */
    fun completeSetup() {
        setupPrefs.edit().putBoolean(PREFS_SETUP_COMPLETE, true).apply()
        startMainActivity()
    }
    
    private fun startMainActivity() {
        // Navigate to Dashboard (MainActivity)
        startActivity(Intent(this, MainActivity::class.java).apply {
            // Clear the back stack so user can't go back to setup
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
    
    /**
     * ViewPager2 adapter for setup wizard fragments.
     */
    private inner class SetupPagerAdapter(
        activity: AppCompatActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
    
    companion object {
        const val PREFS_NAME = "appcontrolx_prefs"
        const val PREFS_SETUP_COMPLETE = "setup_complete"
        
        // Step constants for navigation
        private const val STEP_WELCOME = 0
        private const val STEP_MODE_SELECTION = 1
        private const val STEP_COMPLETE = 2
        
        /**
         * Check if setup is complete.
         * 
         * Requirement 0.8: Used to determine if Setup_Wizard should be shown
         * or if the app should navigate directly to Dashboard.
         */
        fun isSetupComplete(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(PREFS_SETUP_COMPLETE, false)
        }
        
        /**
         * Reset setup state (for testing or settings reset).
         */
        fun resetSetup(context: Context) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(PREFS_SETUP_COMPLETE, false)
                .apply()
        }
    }
}
