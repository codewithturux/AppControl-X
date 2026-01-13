package com.appcontrolx.ui.applist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.appcontrolx.R
import com.appcontrolx.data.model.AppListFilter
import com.appcontrolx.data.model.FilterType
import com.appcontrolx.data.model.SortType
import com.appcontrolx.databinding.BottomSheetFilterSortBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Callback interface for filter/sort selection changes.
 * Requirements: 3.3, 3.6, 3.7
 */
interface FilterSortCallback {
    fun onFilterSortChanged(filter: AppListFilter)
}

/**
 * Bottom sheet for selecting filter and sort options for the app list.
 * Requirements: 3.3, 3.4, 3.5, 3.6, 3.7
 */
class FilterSortBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFilterSortBinding? = null
    private val binding get() = _binding!!

    private var currentFilter: AppListFilter = AppListFilter()
    private var callback: FilterSortCallback? = null

    companion object {
        const val TAG = "FilterSortBottomSheet"
        private const val ARG_FILTER_TYPE = "filter_type"
        private const val ARG_SORT_TYPE = "sort_type"

        /**
         * Show the filter/sort bottom sheet.
         * @param fragmentManager FragmentManager to show the dialog
         * @param currentFilter Current filter configuration
         * @param callback Callback for filter/sort changes
         */
        fun show(
            fragmentManager: FragmentManager,
            currentFilter: AppListFilter,
            callback: FilterSortCallback
        ) {
            val fragment = FilterSortBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILTER_TYPE, currentFilter.filterType.name)
                    putString(ARG_SORT_TYPE, currentFilter.sortType.name)
                }
                this.callback = callback
            }
            fragment.show(fragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            val filterTypeName = args.getString(ARG_FILTER_TYPE, FilterType.ALL.name)
            val sortTypeName = args.getString(ARG_SORT_TYPE, SortType.NAME_ASC.name)
            currentFilter = AppListFilter(
                filterType = FilterType.valueOf(filterTypeName),
                sortType = SortType.valueOf(sortTypeName)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFilterSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFilterRadioButtons()
        setupSortRadioButtons()
        setupApplyButton()
        restoreSelection()
    }

    /**
     * Setup filter radio button listeners.
     */
    private fun setupFilterRadioButtons() {
        binding.rgFilter.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = currentFilter.copy(
                filterType = when (checkedId) {
                    R.id.rbFilterAll -> FilterType.ALL
                    R.id.rbFilterRunning -> FilterType.RUNNING
                    R.id.rbFilterFrozen -> FilterType.FROZEN
                    R.id.rbFilterRestricted -> FilterType.RESTRICTED
                    else -> FilterType.ALL
                }
            )
        }
    }

    /**
     * Setup sort radio button listeners.
     */
    private fun setupSortRadioButtons() {
        binding.rgSort.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = currentFilter.copy(
                sortType = when (checkedId) {
                    R.id.rbSortNameAsc -> SortType.NAME_ASC
                    R.id.rbSortNameDesc -> SortType.NAME_DESC
                    R.id.rbSortSize -> SortType.SIZE_DESC
                    R.id.rbSortUpdated -> SortType.UPDATED_DESC
                    else -> SortType.NAME_ASC
                }
            )
        }
    }

    /**
     * Setup apply button click handler.
     */
    private fun setupApplyButton() {
        binding.btnApply.setOnClickListener {
            callback?.onFilterSortChanged(currentFilter)
            dismiss()
        }
    }

    /**
     * Restore the current filter/sort selection in the UI.
     */
    private fun restoreSelection() {
        // Restore filter selection
        val filterRadioId = when (currentFilter.filterType) {
            FilterType.ALL -> R.id.rbFilterAll
            FilterType.RUNNING -> R.id.rbFilterRunning
            FilterType.FROZEN -> R.id.rbFilterFrozen
            FilterType.RESTRICTED -> R.id.rbFilterRestricted
        }
        binding.rgFilter.check(filterRadioId)

        // Restore sort selection
        val sortRadioId = when (currentFilter.sortType) {
            SortType.NAME_ASC -> R.id.rbSortNameAsc
            SortType.NAME_DESC -> R.id.rbSortNameDesc
            SortType.SIZE_DESC -> R.id.rbSortSize
            SortType.UPDATED_DESC -> R.id.rbSortUpdated
        }
        binding.rgSort.check(sortRadioId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
