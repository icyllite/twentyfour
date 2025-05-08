/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package git.icyllite.twentyfour.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import git.icyllite.twentyfour.R
import git.icyllite.twentyfour.ext.getParcelable
import git.icyllite.twentyfour.ext.getViewProperty
import git.icyllite.twentyfour.ext.navigateSafe
import git.icyllite.twentyfour.models.DataSourceInformation
import git.icyllite.twentyfour.models.FlowResult
import git.icyllite.twentyfour.models.FlowResult.Companion.getOrNull
import git.icyllite.twentyfour.models.ProviderIdentifier
import git.icyllite.twentyfour.ui.recyclerview.SimpleListAdapter
import git.icyllite.twentyfour.ui.recyclerview.UniqueItemDiffCallback
import git.icyllite.twentyfour.ui.views.FullscreenLoadingProgressBar
import git.icyllite.twentyfour.ui.views.ListItem
import git.icyllite.twentyfour.viewmodels.ProviderViewModel

/**
 * Fragment used to show useful information regarding a provider.
 */
class ProviderInformationBottomSheetDialogFragment : BottomSheetDialogFragment(
    R.layout.fragment_provider_information_bottom_sheet_dialog
) {
    // View models
    private val viewModel by viewModels<ProviderViewModel>()

    // Views
    private val deleteProviderMaterialButton by getViewProperty<MaterialButton>(R.id.deleteProviderMaterialButton)
    private val fullscreenLoadingProgressBar by getViewProperty<FullscreenLoadingProgressBar>(R.id.fullscreenLoadingProgressBar)
    private val manageButtonsHorizontalScrollView by getViewProperty<HorizontalScrollView>(R.id.manageButtonsHorizontalScrollView)
    private val manageProviderMaterialButton by getViewProperty<MaterialButton>(R.id.manageProviderMaterialButton)
    private val providerIconImageView by getViewProperty<ImageView>(R.id.providerIconImageView)
    private val providerTypeTextView by getViewProperty<TextView>(R.id.providerTypeTextView)
    private val statusMaterialDivider by getViewProperty<MaterialDivider>(R.id.statusMaterialDivider)
    private val statusRecyclerView by getViewProperty<RecyclerView>(R.id.statusRecyclerView)
    private val titleTextView by getViewProperty<TextView>(R.id.titleTextView)

    // RecyclerView
    private val statusAdapter by lazy {
        object : SimpleListAdapter<DataSourceInformation, ListItem>(
            UniqueItemDiffCallback(),
            { context -> ListItem(context) }
        ) {
            override fun ViewHolder.onBindView(item: DataSourceInformation) {
                view.headlineText = item.keyLocalizedString.getString(view.resources)
                view.supportingText = item.value.getString(view.resources)
            }
        }
    }

    // Arguments
    private val providerIdentifier: ProviderIdentifier
        get() = requireArguments().getParcelable(
            ARG_PROVIDER_IDENTIFIER, ProviderIdentifier::class
        )!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        manageProviderMaterialButton.setOnClickListener {
            viewModel.provider.value.getOrNull()?.let {
                findNavController().navigateSafe(
                    R.id.action_providerInformationBottomSheetDialogFragment_to_fragment_manage_provider,
                    ManageProviderFragment.createBundle(
                        providerIdentifier = providerIdentifier
                    ),
                )
            }
        }

        deleteProviderMaterialButton.setOnClickListener {
            showDeleteDialog()
        }

        statusRecyclerView.adapter = statusAdapter

        viewModel.setProviderIdentifier(providerIdentifier)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loadData()
            }
        }
    }

    override fun onDestroyView() {
        statusRecyclerView.adapter = null

        super.onDestroyView()
    }

    private fun CoroutineScope.loadData() {
        launch {
            viewModel.provider.collectLatest {
                when (it) {
                    is FlowResult.Loading -> {
                        // Do nothing
                    }

                    is FlowResult.Success -> {
                        val provider = it.data

                        titleTextView.text = provider.name
                        providerTypeTextView.setText(provider.type.nameStringResId)
                        providerIconImageView.setImageResource(provider.type.iconDrawableResId)
                    }

                    is FlowResult.Error -> {
                        Log.e(LOG_TAG, "Failed to load data, error: ${it.error}", it.throwable)

                        titleTextView.text = ""
                        providerTypeTextView.text = ""
                        providerIconImageView.setImageResource(R.drawable.ic_warning)
                    }
                }
            }
        }

        launch {
            viewModel.canBeManaged.collectLatest {
                manageButtonsHorizontalScrollView.isVisible = it
            }
        }

        launch {
            viewModel.status.collectLatest {
                when (it) {
                    is FlowResult.Loading -> {
                        // Do nothing
                    }

                    is FlowResult.Success -> {
                        val data = it.data

                        statusAdapter.submitList(data)

                        val isEmpty = data.isEmpty()

                        statusRecyclerView.isVisible = !isEmpty
                        statusMaterialDivider.isVisible = !isEmpty
                    }

                    is FlowResult.Error -> {
                        Log.e(LOG_TAG, "Failed to load data, error: ${it.error}", it.throwable)

                        statusAdapter.submitList(emptyList())

                        statusRecyclerView.isVisible = false
                        statusMaterialDivider.isVisible = false
                    }
                }
            }
        }
    }

    private fun showDeleteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_provider_confirmation)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    fullscreenLoadingProgressBar.withProgress {
                        viewModel.deleteProvider()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                // Do nothing
            }
            .show()
    }

    companion object {
        private val LOG_TAG = ProviderInformationBottomSheetDialogFragment::class.simpleName!!

        private const val ARG_PROVIDER_IDENTIFIER = "provider_identifier"

        /**
         * Create a [Bundle] to use as the arguments for this fragment.
         * @param providerIdentifier The [ProviderIdentifier] of the provider to manage
         */
        fun createBundle(
            providerIdentifier: ProviderIdentifier,
        ) = bundleOf(
            ARG_PROVIDER_IDENTIFIER to providerIdentifier,
        )
    }
}
