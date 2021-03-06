/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar

import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.core.graphics.drawable.toBitmap
import mozilla.components.browser.awesomebar.BrowserAwesomeBar
import mozilla.components.browser.search.DefaultSearchEngineProvider
import mozilla.components.browser.session.Session
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.awesomebar.provider.BookmarksStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchActionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.search.ext.legacy
import mozilla.components.feature.search.ext.toDefaultSearchEngineProvider
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.syncedtabs.DeviceIndicators
import mozilla.components.feature.syncedtabs.SyncedTabsStorageSuggestionProvider
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.search.SearchEngineSource
import org.mozilla.fenix.search.SearchFragmentState
import org.mozilla.fenix.search.awesomebar.SearchProvider.BrandAdSearchSuggestionProvider
import org.mozilla.fenix.search.awesomebar.SearchProvider.BrandSearchSuggestionProvider
import mozilla.components.browser.search.SearchEngine as LegacySearchEngine

/**
 * View that contains and configures the BrowserAwesomeBar
 */
@Suppress("LargeClass")
class AwesomeBarView(
    private val activity: HomeActivity,
    val interactor: AwesomeBarInteractor,
    val view: BrowserAwesomeBar
) {
    private val sessionProvider: SessionSuggestionProvider
    /* Gexsi begin: removing suggstions
    private val historyStorageProvider: HistoryStorageSuggestionProvider
    private val shortcutsEnginePickerProvider: ShortcutsSuggestionProvider
    private val bookmarksStorageSuggestionProvider: BookmarksStorageSuggestionProvider
    private val syncedTabsStorageSuggestionProvider: SyncedTabsStorageSuggestionProvider
    private val defaultSearchSuggestionProvider: SearchSuggestionProvider
    private val defaultSearchActionProvider: SearchActionProvider
    */
    private val brandSearchSuggestionProvider: BrandSearchSuggestionProvider
    private var brandAdSearchSuggestionProvider: BrandAdSearchSuggestionProvider
    // Gexsi end
    private val searchSuggestionProviderMap: MutableMap<SearchEngine, List<AwesomeBar.SuggestionProvider>>
    private var providersInUse = mutableSetOf<AwesomeBar.SuggestionProvider>()

    private val loadUrlUseCase = object : SessionUseCases.LoadUrlUseCase {
        override fun invoke(
            url: String,
            flags: EngineSession.LoadUrlFlags,
            additionalHeaders: Map<String, String>?
        ) {
            interactor.onUrlTapped(url)
        }
    }

    private val searchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(
            searchTerms: String,
            searchEngine: mozilla.components.browser.search.SearchEngine?,
            parentSession: Session?
        ) {
            interactor.onSearchTermsTapped(searchTerms)
        }
    }

    private val shortcutSearchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(
            searchTerms: String,
            searchEngine: mozilla.components.browser.search.SearchEngine?,
            parentSession: Session?
        ) {
            interactor.onSearchTermsTapped(searchTerms)
        }
    }

    private val selectTabUseCase = object : TabsUseCases.SelectTabUseCase {
        override fun invoke(session: Session) {
            interactor.onExistingSessionSelected(session)
        }

        override fun invoke(tabId: String) {
            interactor.onExistingSessionSelected(tabId)
        }
    }

    init {
        view.itemAnimator = null
        val components = activity.components
        val primaryTextColor = activity.getColorFromAttr(R.attr.primaryText)

        val engineForSpeculativeConnects = when (activity.browsingModeManager.mode) {
            BrowsingMode.Normal -> components.core.engine
            BrowsingMode.Private -> null
        }
        sessionProvider =
            SessionSuggestionProvider(
                activity.resources,
                components.core.store,
                selectTabUseCase,
                components.core.icons,
                getDrawable(activity, R.drawable.ic_search_results_tab),
                excludeSelectedSession = true
            )
        /* Gexsi begin: removing suggstions
        historyStorageProvider =
            HistoryStorageSuggestionProvider(
                components.core.historyStorage,
                loadUrlUseCase,
                components.core.icons,
                engineForSpeculativeConnects
            )

        bookmarksStorageSuggestionProvider =
            BookmarksStorageSuggestionProvider(
                bookmarksStorage = components.core.bookmarksStorage,
                loadUrlUseCase = loadUrlUseCase,
                icons = components.core.icons,
                indicatorIcon = getDrawable(activity, R.drawable.ic_search_results_bookmarks),
                engine = engineForSpeculativeConnects
            )

        syncedTabsStorageSuggestionProvider =
            SyncedTabsStorageSuggestionProvider(
                components.backgroundServices.syncedTabsStorage,
                loadUrlUseCase,
                components.core.icons,
                DeviceIndicators(
                    getDrawable(activity, R.drawable.ic_search_results_device_desktop),
                    getDrawable(activity, R.drawable.ic_search_results_device_mobile),
                    getDrawable(activity, R.drawable.ic_search_results_device_tablet)
                )
            )
        Gexsi end */
        val searchBitmap = getDrawable(activity, R.drawable.ic_search)!!.apply {
            colorFilter = createBlendModeColorFilterCompat(primaryTextColor, SRC_IN)
        }.toBitmap()

        /* Gexsi begin: replacing with brand suggestion provider
        defaultSearchSuggestionProvider =
            SearchSuggestionProvider(
                context = activity,
                defaultSearchEngineProvider = components.core.store.toDefaultSearchEngineProvider(),
                searchUseCase = searchUseCase,
                fetchClient = components.core.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                limit = 3,
                icon = searchBitmap,
                showDescription = false,
                engine = engineForSpeculativeConnects,
                filterExactMatch = true
            )

        defaultSearchActionProvider =
            SearchActionProvider(
                defaultSearchEngineProvider = components.core.store.toDefaultSearchEngineProvider(),
                searchUseCase = searchUseCase,
                icon = searchBitmap,
                showDescription = false
            )
        */
        brandSearchSuggestionProvider =
                BrandSearchSuggestionProvider(
                        context = activity,
                        defaultSearchEngineProvider = components.core.store.toDefaultSearchEngineProvider(),
                        searchUseCase = searchUseCase,
                        fetchClient = components.core.client,
                        icon = searchBitmap,
                        engine = engineForSpeculativeConnects,
                )
        brandAdSearchSuggestionProvider =
                BrandAdSearchSuggestionProvider(
                        context = activity,
                        defaultSearchEngineProvider = components.core.store.toDefaultSearchEngineProvider(),
                        loadUrlUseCase = loadUrlUseCase,
                        fetchClient = components.core.client,
                        engine = engineForSpeculativeConnects,
                )
        // Gexsi end

        /* Gexsi begin: Frecency suggestions provider
        shortcutsEnginePickerProvider =
            ShortcutsSuggestionProvider(
                store = components.core.store,
                context = activity,
                selectShortcutEngine = interactor::onSearchShortcutEngineSelected,
                selectShortcutEngineSettings = interactor::onClickSearchEngineSettings
            )
        Gexsi end */

        searchSuggestionProviderMap = HashMap()
    }

    fun update(state: SearchFragmentState) {
        updateSuggestionProvidersVisibility(state)

        // Do not make suggestions based on user's current URL unless it's a search shortcut
        if (state.query.isNotEmpty() && state.query == state.url && !state.showSearchShortcuts) {
            return
        }

        view.onInputChanged(state.query)
    }

    private fun updateSuggestionProvidersVisibility(state: SearchFragmentState) {
        if (state.showSearchShortcuts) {
            handleDisplayShortcutsProviders()
            return
        }

        val providersToAdd = getProvidersToAdd(state)
        val providersToRemove = getProvidersToRemove(state)

        performProviderListChanges(providersToAdd, providersToRemove)
    }

    private fun performProviderListChanges(
        providersToAdd: MutableSet<AwesomeBar.SuggestionProvider>,
        providersToRemove: MutableSet<AwesomeBar.SuggestionProvider>
    ) {
        for (provider in providersToAdd) {
            if (providersInUse.find { it.id == provider.id } == null) {
                providersInUse.add(provider)
                view.addProviders(provider)
            }
        }

        for (provider in providersToRemove) {
            if (providersInUse.find { it.id == provider.id } != null) {
                providersInUse.remove(provider)
                view.removeProviders(provider)
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun getProvidersToAdd(state: SearchFragmentState): MutableSet<AwesomeBar.SuggestionProvider> {
        val providersToAdd = mutableSetOf<AwesomeBar.SuggestionProvider>()
        /* Gexsi begin: disable history and bookmarks suggestions
        if (state.showHistorySuggestions) {
            providersToAdd.add(historyStorageProvider)
        }

        if (state.showBookmarkSuggestions) {
            providersToAdd.add(bookmarksStorageSuggestionProvider)
        }
        Gexsi end */
        if (state.showSearchSuggestions) {
            providersToAdd.addAll(getSelectedSearchSuggestionProvider(state))
        }
        /* Gexsi begin: disable synced tabs
        if (state.showSyncedTabsSuggestions) {
            providersToAdd.add(syncedTabsStorageSuggestionProvider)
        }
        Gexsi end */
        if (activity.browsingModeManager.mode == BrowsingMode.Normal) {
            providersToAdd.add(sessionProvider)
        }

        return providersToAdd
    }

    private fun getProvidersToRemove(state: SearchFragmentState): MutableSet<AwesomeBar.SuggestionProvider> {
        val providersToRemove = mutableSetOf<AwesomeBar.SuggestionProvider>()

        /* Gexsi begin: removing suggestions
         providersToRemove.add(shortcutsEnginePickerProvider)

        if (!state.showHistorySuggestions) {
            providersToRemove.add(historyStorageProvider)
        }

        if (!state.showBookmarkSuggestions) {
            providersToRemove.add(bookmarksStorageSuggestionProvider)
        }

        if (!state.showSyncedTabsSuggestions) {
            providersToRemove.add(syncedTabsStorageSuggestionProvider)
        }
        Gexsi end */

        if (!state.showSearchSuggestions) {
            providersToRemove.addAll(getSelectedSearchSuggestionProvider(state))
        }

        if (activity.browsingModeManager.mode == BrowsingMode.Private) {
            providersToRemove.add(sessionProvider)
        }

        return providersToRemove
    }

    private fun getSelectedSearchSuggestionProvider(state: SearchFragmentState): List<AwesomeBar.SuggestionProvider> {
        return when (state.searchEngineSource) {
            is SearchEngineSource.Default -> listOf(
                /* Gexsi begin: replacing with brand suggestion provider
                defaultSearchActionProvider,
                defaultSearchSuggestionProvider
                */
                brandAdSearchSuggestionProvider,
                brandSearchSuggestionProvider
                // Gexsi end
            )
            is SearchEngineSource.Shortcut -> getSuggestionProviderForEngine(
                state.searchEngineSource.searchEngine
            )
            is SearchEngineSource.None -> emptyList()
        }
    }

    private fun handleDisplayShortcutsProviders() {
        view.removeAllProviders()
        providersInUse.clear()

        /* Gexsi begin: removing shortcust
         providersInUse.add(shortcutsEnginePickerProvider)
         view.addProviders(shortcutsEnginePickerProvider)
        Gexsi end */
    }

    private fun getSuggestionProviderForEngine(engine: SearchEngine): List<AwesomeBar.SuggestionProvider> {
        return searchSuggestionProviderMap.getOrPut(engine) {
            val components = activity.components
            val primaryTextColor = activity.getColorFromAttr(R.attr.primaryText)

            val searchBitmap = getDrawable(activity, R.drawable.ic_search)!!.apply {
                colorFilter = createBlendModeColorFilterCompat(primaryTextColor, SRC_IN)
            }.toBitmap()

            val engineForSpeculativeConnects = when (activity.browsingModeManager.mode) {
                BrowsingMode.Normal -> components.core.engine
                BrowsingMode.Private -> null
            }

            listOf(
                SearchActionProvider(
                    defaultSearchEngineProvider = object : DefaultSearchEngineProvider {
                        override fun getDefaultSearchEngine(): LegacySearchEngine? =
                            engine.legacy()
                        override suspend fun retrieveDefaultSearchEngine(): LegacySearchEngine? =
                            engine.legacy()
                    },
                    searchUseCase = shortcutSearchUseCase,
                    icon = searchBitmap
                ),
                SearchSuggestionProvider(
                    engine.legacy(),
                    shortcutSearchUseCase,
                    components.core.client,
                    limit = 3,
                    mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                    icon = searchBitmap,
                    engine = engineForSpeculativeConnects,
                    filterExactMatch = true
                )
            )
        }
    }
}