/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package me.icy.twentyfour.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import me.icy.twentyfour.R
import me.icy.twentyfour.ext.getViewProperty
import me.icy.twentyfour.ext.navigateSafe
import me.icy.twentyfour.models.Provider
import me.icy.twentyfour.ui.recyclerview.SimpleListAdapter
import me.icy.twentyfour.ui.recyclerview.UniqueItemDiffCallback
import me.icy.twentyfour.ui.views.ListItem
import me.icy.twentyfour.viewmodels.ProvidersViewModel

/**
 * Fragment used to select a media provider.
 */
class ProviderSelectorDialogFragment : MaterialDialogFragment(
    R.layout.fragment_provider_selector_dialog
) {
    // View models
    private val viewModel by viewModels<ProvidersViewModel>()

    // Views
    private val addProviderMaterialButton by getViewProperty<MaterialButton>(R.id.addProviderMaterialButton)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)

    // Recyclerview
    private val adapter = object : SimpleListAdapter<Provider, ListItem>(
        UniqueItemDiffCallback(),
        ::ListItem,
    ) {
        override fun ViewHolder.onPrepareView() {
            view.setTrailingView(R.layout.provider_more_button)
        }

        override fun ViewHolder.onBindView(item: Provider) {
            view.setOnClickListener {
                viewModel.setNavigationProvider(item)
                findNavController().navigateUp()
            }

            view.trailingView?.isVisible = item.type.canBeManaged
            view.trailingView?.setOnClickListener {
                findNavController().navigateSafe(
                    R.id.action_providerSelectorDialogFragment_to_fragment_provider_information_bottom_sheet_dialog,
                    ManageProviderFragment.createBundle(providerIdentifier = item),
                    NavOptions.Builder()
                        .setPopUpTo(R.id.mainFragment, false)
                        .build(),
                )
            }

            view.setLeadingIconImage(item.type.iconDrawableResId)
            view.headlineText = item.name
            view.setSupportingText(item.type.nameStringResId)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter

        addProviderMaterialButton.setOnClickListener {
            findNavController().navigateSafe(
                R.id.action_providerSelectorDialogFragment_to_fragment_manage_provider,
                ManageProviderFragment.createBundle()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.providers.collect { providers ->
                    adapter.submitList(providers)
                }
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null

        super.onDestroyView()
    }
}
