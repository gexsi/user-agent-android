package org.mozilla.fenix.search.awesomebar.SearchProvider

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.content.Context
import mozilla.components.browser.search.DefaultSearchEngineProvider
import mozilla.components.browser.search.SearchEngine
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import java.io.IOException

/**
 * Async function responsible for taking a URL and returning the results
 */
typealias SearchSuggestionFetcher = suspend (url: String) -> String?

/**
 *  Provides an interface to get search suggestions from a given SearchEngine.
 */
class BrandSearchSuggestionClient {
    private val context: Context?
    private val fetcher: SearchSuggestionFetcher
    private val logger = Logger("SearchSuggestionClient")

    private val defaultSearchEngineProvider: DefaultSearchEngineProvider?
    var searchEngine: SearchEngine? = null
        private set

    internal constructor(
            context: Context?,
            defaultSearchEngineProvider: DefaultSearchEngineProvider?,
            searchEngine: SearchEngine?,
            fetcher: SearchSuggestionFetcher
    ) {
        this.context = context
        this.defaultSearchEngineProvider = defaultSearchEngineProvider
        this.searchEngine = searchEngine
        this.fetcher = fetcher
    }

    constructor(
            context: Context,
            defaultSearchEngineProvider: DefaultSearchEngineProvider,
            fetcher: SearchSuggestionFetcher
    ) : this (context, defaultSearchEngineProvider, null, fetcher)

    /**
     * Exception types for errors caught while getting a list of suggestions
     */
    class FetchException : Exception("There was a problem fetching suggestions")

    /**
     * Gets search suggestions for a given query
     */
    suspend fun getSuggestions(query: String): List<String>? {
        val searchEngine = searchEngine ?: run {
            requireNotNull(defaultSearchEngineProvider)
            requireNotNull(context)

            val searchEngine = defaultSearchEngineProvider.retrieveDefaultSearchEngine()
            if (searchEngine == null) {
                logger.warn("No default search engine for fetching suggestions")
                return emptyList()
            } else {
                this.searchEngine = searchEngine
                searchEngine
            }
        }

        if (!searchEngine.canProvideSearchSuggestions) {
            // This search engine doesn't support suggestions. Let's only return a default suggestion
            // for the entered text.
            return emptyList()
        }

        val suggestionsURL = searchEngine.buildSuggestionsURL(query)

        val suggestionResults = try {
            suggestionsURL?.let { fetcher(it) }
        } catch (_: IOException) {
            throw FetchException()
        }

        return parseJSON(suggestionResults)
    }

    private fun parseJSON(input: String?): List<String>? {
        if (input == null) {
            return emptyList()
        }
        return JSONObject(input).optJSONArray("results")
                ?.let { 0.until(it.length()).map { i -> it.optJSONObject(i) } }
                ?.filter { it.getString("type") == "QUERY" }
                ?.map { it.getString("q") }
    }

    /**
     * Gets search suggestions for a given query
     */
    suspend fun getAdSuggestions(query: String): List<AdSuggestion>? {
        val searchEngine = searchEngine ?: run {
            requireNotNull(defaultSearchEngineProvider)
            requireNotNull(context)

            val searchEngine = defaultSearchEngineProvider.retrieveDefaultSearchEngine()
            if (searchEngine == null) {
                logger.warn("No default search engine for fetching suggestions")
                return emptyList()
            } else {
                this.searchEngine = searchEngine
                searchEngine
            }
        }

        if (!searchEngine.canProvideSearchSuggestions) {
            // This search engine doesn't support suggestions. Let's only return a default suggestion
            // for the entered text.
            return emptyList()
        }

        val suggestionsURL = searchEngine.buildSuggestionsURL(query)

        val suggestionResults = try {
            suggestionsURL?.let { fetcher(it) }
        } catch (_: IOException) {
            throw FetchException()
        }

        return parseAdJSON(suggestionResults)
    }

    private fun parseAdJSON(input: String?): List<AdSuggestion>? {
        if (input == null) {
            return emptyList()
        }
        return JSONObject(input).optJSONArray("results")
                ?.let { 0.until(it.length()).map { i -> it.optJSONObject(i) } }
                ?.filter { it.getString("type") == "NAVIGATION" }
                ?.map { AdSuggestion(it.toString()) }
    }
}