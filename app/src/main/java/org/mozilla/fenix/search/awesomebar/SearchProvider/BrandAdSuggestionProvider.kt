/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar.SearchProvider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.kotlin.sanitizeURL
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A [AwesomeBar.SuggestionProvider] implementation that provides a suggestion containing search engine suggestions (as
 * chips) from the passed in [SearchEngine].
 */
@Suppress("LongParameterList")
class BrandAdSearchSuggestionProvider private constructor(
        @VisibleForTesting internal val client: BrandSearchSuggestionClient,
        private val loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
        @VisibleForTesting internal val engine: Engine? = null,
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
            loadUrlUseCase: SessionUseCases.LoadUrlUseCase,
            fetchClient: Client,
            engine: Engine? = null,
    ) : this(
            BrandSearchSuggestionClient(context, defaultSearchEngineProvider) { url -> fetch(fetchClient, url) },
            loadUrlUseCase,
            engine,
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

    private suspend fun fetchSuggestions(text: String): List<AdSuggestion>? {
        return try {
            client.getAdSuggestions(text)
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

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            BitmapFactory.decodeStream(URL(src).openConnection().getInputStream())
        } catch (e: IOException) {
            null
        }
    }

    @Suppress("ComplexMethod")
    private fun createMultipleSuggestions(text: String, result: List<AdSuggestion>?): List<AwesomeBar.Suggestion> {
        val suggestions = mutableListOf<AwesomeBar.Suggestion>()

        result?.distinct()?.forEachIndexed { index, item ->
            if (item.title != null && item.url != null) {
                suggestions.add(AwesomeBar.Suggestion(
                        provider = this,
                        // We always use the same ID for the entered text so that this suggestion gets replaced "in place".
                        id = if (item.title == text) ID_OF_ENTERED_TEXT else item.title,
                        title = item.title,
                        description = item.description,
                        // Don't show an autocomplete arrow for the entered text
                        editSuggestion = null,
                        icon = getBitmapFromURL(item.imageUrl),
                        score = Int.MIN_VALUE - (index + 1),
                        onSuggestionClicked = {
                            loadUrlUseCase.invoke(item.url)
                        }
                ))
            }
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
