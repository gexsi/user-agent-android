package org.mozilla.fenix.search.awesomebar.SearchProvider

import org.json.JSONObject

class AdSuggestion(json: String) : JSONObject(json) {
    val title: String? = this.optString("title")
    val description: String? = this.optString("q").replace("!", "")
    val url: String? = this.optString("url")
    var imageUrl: String? = this.optString("image")
    var impressionURL: String? = this.optString("impression")
}