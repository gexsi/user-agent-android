/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar.SearchProvider

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import mozilla.components.browser.search.DefaultSearchEngineProvider
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.suggestions.SearchSuggestionClient
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.isSuccess
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlin.sanitizeURL
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides a suggestion containing search engine suggestions (as
 * chips) from the passed in [SearchEngine].
 */
@Suppress("LongParameterList")
class BrandSearchSuggestionProvider private constructor(
        @VisibleForTesting internal val client: BrandSearchSuggestionClient,
        private val searchUseCase: SearchUseCases.SearchUseCase,
        @VisibleForTesting internal val engine: Engine? = null,
        private val icon: Bitmap? = null,
) : AwesomeBar.SuggestionProvider {
    override val id: String = UUID.randomUUID().toString()

    /**
     * Creates a [SearchSuggestionProvider] using the default engine as returned by the provided
     *
     * @param context the activity or application context, required to load search engines.
     * @param searchUseCase The use case to invoke for searches.
     * @param fetchClient The HTTP client for requesting suggestions from the search engine.
     * @param engine optional [Engine] instance to call [Engine.speculativeConnect] for the
     * highest scored search suggestion URL.
     * @param icon The image to display next to the result. If not specified, the engine icon is used.
     */
    constructor(
            context: Context,
            defaultSearchEngineProvider: DefaultSearchEngineProvider,
            searchUseCase: SearchUseCases.SearchUseCase,
            fetchClient: Client,
            engine: Engine? = null,
            icon: Bitmap? = null,
    ) : this (
            BrandSearchSuggestionClient(context, defaultSearchEngineProvider) { url -> fetch(fetchClient, url) },
            searchUseCase,
            engine,
            icon,
    )

    @Suppress("ReturnCount")
    override suspend fun onInputChanged(text: String): List<AwesomeBar.Suggestion> {
        if (text.isEmpty()) {
            return emptyList()
        }
        val suggestions = fetchSuggestions(text)

        return createMultipleSuggestions(text, suggestions).also {
            // Call speculativeConnect for URL of first (highest scored) suggestion
            it.firstOrNull()?.title?.let { searchTerms -> maybeCallSpeculativeConnect(searchTerms) }
        }
    }

    private fun maybeCallSpeculativeConnect(searchTerms: String) {
        client.searchEngine?.let { searchEngine ->
            engine?.speculativeConnect(searchEngine.buildSearchUrl(searchTerms))
        }
    }

    private suspend fun fetchSuggestions(text: String): List<String>? {
        return try {
            client.getSuggestions(text)
        } catch (e: SearchSuggestionClient.FetchException) {
            Logger.info("Could not fetch search suggestions from search engine", e)
            // If we can't fetch search suggestions then just continue with a single suggestion for the entered text
            emptyList()
        } catch (e: SearchSuggestionClient.ResponseParserException) {
            Logger.warn("Could not parse search suggestions from search engine", e)
            // If parsing failed then just continue with a single suggestion for the entered text
            emptyList()
        }
    }

    @Suppress("ComplexMethod")
    private fun createMultipleSuggestions(text: String, result: List<String>?): List<AwesomeBar.Suggestion> {
        val suggestions = mutableListOf<AwesomeBar.Suggestion>()
        val id = UUID.randomUUID().toString()
        result?.distinct()?.forEachIndexed { index, item ->
            suggestions.add(AwesomeBar.Suggestion(
                    provider = this,
                    // We always use the same ID for the entered text so that this suggestion gets replaced "in place".
                    id = id + "_" + index.toString(), // if (item == text) ID_OF_ENTERED_TEXT else item,
                    title = item,
                    description = null,
                    // Don't show an autocomplete arrow for the entered text
                    editSuggestion = if (item == text) null else item,
                    icon = icon,
                    score = Int.MAX_VALUE - (index + 1),
                    onSuggestionClicked = {
                        searchUseCase.invoke(item)
                    }
            ))
        }

        return suggestions
    }

    override val shouldClearSuggestions: Boolean
        // We do not want the suggestion of this provider to disappear and re-appear when text changes.
        get() = false

    companion object {
        private const val READ_TIMEOUT_IN_MS = 2000L
        private const val CONNECT_TIMEOUT_IN_MS = 1000L
        private const val ID_OF_ENTERED_TEXT = "<@@@entered_text_id@@@>"

        @Suppress("ReturnCount", "TooGenericExceptionCaught")
        private fun fetch(fetchClient: Client, url: String): String? {
            try {
                val request = Request(
                        url = url.sanitizeURL(),
                        readTimeout = Pair(READ_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS),
                        connectTimeout = Pair(CONNECT_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
                )

                val response = fetchClient.fetch(request)
                if (!response.isSuccess) {
                    return null
                }

                return response.use { it.body.string() }
            } catch (e: IOException) {
                return null
            } catch (e: ArrayIndexOutOfBoundsException) {
                // On some devices we are seeing an ArrayIndexOutOfBoundsException being thrown
                // somewhere inside AOSP/okhttp.
                // See: https://github.com/mozilla-mobile/android-components/issues/964
                return null
            }
        }
    }
}
