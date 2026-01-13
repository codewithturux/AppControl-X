package com.appcontrolx.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.appcontrolx.ui.applist.AppListFragment
import com.appcontrolx.ui.dashboard.DashboardFragment

/**
 * Adapter for ViewPager2 that manages Dashboard and Apps fragments.
 * 
 * Position 0: DashboardFragment
 * Position 1: AppListFragment
 * 
 * Requirements: 1.4, 1.5
 */
class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val TAB_COUNT = 2
        const val POSITION_DASHBOARD = 0
        const val POSITION_APPS = 1
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            POSITION_DASHBOARD -> DashboardFragment()
            POSITION_APPS -> AppListFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}
