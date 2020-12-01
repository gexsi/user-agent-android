/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.about

sealed class AboutItem {
    data class ExternalLink(val type: AboutItemType, val url: String) : AboutItem()
    object Libraries : AboutItem()
    object Crashes : AboutItem()
}

enum class AboutItemType {
    // Gexsi begin:
//    WHATS_NEW, SUPPORT, PRIVACY_NOTICE, RIGHTS, LICENSING_INFO
    WHATS_NEW, FAQ, CONTACT, TERMS_OF_USE, PRIVACY_POLICY, LICENSING_INFO
    // Gexsi end
}

data class AboutPageItem(val type: AboutItem, val title: String)
