/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.Context
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuHighlight
import mozilla.components.browser.menu.ext.getHighlight
import mozilla.components.browser.menu.item.BrowserMenuDivider
import mozilla.components.browser.menu.item.BrowserMenuHighlightableItem
import mozilla.components.browser.menu.item.BrowserMenuImageText
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.whatsnew.WhatsNew

class HomeMenu(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {},
    private val onMenuBuilderChanged: (BrowserMenuBuilder) -> Unit = {},
    private val onHighlightPresent: (BrowserMenuHighlight) -> Unit = {}
) {
    sealed class Item {
        object WhatsNew : Item()
        object Help : Item()
        object AddonsManager : Item()
        object Settings : Item()

        // Gexsi begin: disable authentication
//        object SyncedTabs : Item()
        // Gexsi end

        object History : Item()
        object Bookmarks : Item()
        object Downloads : Item()
        object Quit : Item()
        object Sync : Item()
    }

    private val primaryTextColor =
        ThemeManager.resolveAttribute(R.attr.primaryText, context)
    private val syncDisconnectedColor = ThemeManager.resolveAttribute(R.attr.syncDisconnected, context)
    private val syncDisconnectedBackgroundColor = context.getColorFromAttr(R.attr.syncDisconnectedBackground)

    private val menuCategoryTextColor =
        ThemeManager.resolveAttribute(R.attr.menuCategoryText, context)
    private val shouldUseBottomToolbar = context.settings().shouldUseBottomToolbar

    // 'Reconnect' and 'Quit' items aren't needed most of the time, so we'll only create the if necessary.
    private val reconnectToSyncItem by lazy {
        BrowserMenuHighlightableItem(
            context.getString(R.string.sync_reconnect),
            R.drawable.ic_sync_disconnected,
            iconTintColorResource = syncDisconnectedColor,
            textColorResource = primaryTextColor,
            highlight = BrowserMenuHighlight.HighPriority(
                backgroundTint = syncDisconnectedBackgroundColor,
                canPropagate = false
            ),
            isHighlighted = { true }
        ) {
            onItemTapped.invoke(Item.Sync)
        }
    }

    private val quitItem by lazy {
        BrowserMenuImageText(
            context.getString(R.string.delete_browsing_data_on_quit_action),
            R.drawable.ic_exit,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Quit)
        }
    }

    private val coreMenuItems by lazy {
        // Gexsi begin:
        val appName = context.getString(R.string.app_name)
        // Gexsi end
        val whatsNewItem = BrowserMenuHighlightableItem(
            // Gexsi begin:
//            context.getString(R.string.browser_menu_whats_new),
            context.getString(R.string.browser_menu_about, appName),
            // Gexsi end
            R.drawable.ic_whats_new,
            iconTintColorResource = primaryTextColor,
            highlight = BrowserMenuHighlight.LowPriority(
                notificationTint = getColor(context, R.color.whats_new_notification_color)
            ),
            isHighlighted = { WhatsNew.shouldHighlightWhatsNew(context) }
        ) {
            onItemTapped.invoke(Item.WhatsNew)
        }

        val bookmarksItem = BrowserMenuImageText(
            context.getString(R.string.library_bookmarks),
            R.drawable.ic_bookmark_filled,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Bookmarks)
        }

        val historyItem = BrowserMenuImageText(
            context.getString(R.string.library_history),
            R.drawable.ic_history,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.History)
        }

        /* Gexsi begin: disable addons
        val addons = BrowserMenuImageText(
            context.getString(R.string.browser_menu_add_ons),
            R.drawable.ic_addons_extensions,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.AddonsManager)
        }
        Gexsi end */

        val settingsItem = BrowserMenuImageText(
            context.getString(R.string.browser_menu_settings),
            R.drawable.ic_settings,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Settings)
        }

        /* Gexsi begin: disable authentication
        val syncedTabsItem = BrowserMenuImageText(
            context.getString(R.string.library_synced_tabs),
            R.drawable.ic_synced_tabs,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.SyncedTabs)
        }
        Gexsi end */

        /* Gexsi begin: disable help
        val helpItem = BrowserMenuImageText(
            context.getString(R.string.browser_menu_help),
            R.drawable.ic_help,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Help)
        }
        Gexsi end */

        val downloadsItem = BrowserMenuImageText(
            context.getString(R.string.library_downloads),
            R.drawable.ic_download,
            primaryTextColor
        ) {
            onItemTapped.invoke(Item.Downloads)
        }

        /* Gexsi begin: disable account authentication
        // Only query account manager if it has been initialized.
        // We don't want to cause its initialization just for this check.
        val accountAuthItem = if (context.components.backgroundServices.accountManagerAvailableQueue.isReady()) {
            if (context.components.backgroundServices.accountManager.accountNeedsReauth()) reconnectToSyncItem else null
        } else {
            null
        }
        Gexsi end */

        val settings = context.components.settings

        val menuItems = listOfNotNull(
            if (settings.shouldDeleteBrowsingDataOnQuit) quitItem else null,
            settingsItem,
            BrowserMenuDivider(),

            // Gexsi begin: disable sync tabs
//            if (settings.syncedTabsInTabsTray) null else syncedTabsItem,
            // Gexsi end

            bookmarksItem,
            historyItem,
            downloadsItem,
            BrowserMenuDivider(),

            // Gexsi begin: disable addons
//            addons,
//            BrowserMenuDivider(),
            // Gexsi end
            whatsNewItem,
            // Gexsi begin: disable help and account
//            helpItem,
//            accountAuthItem
            // Gexsi end
        ).also { items ->
            items.getHighlight()?.let { onHighlightPresent(it) }
        }

        if (shouldUseBottomToolbar) {
            menuItems.reversed()
        } else {
            menuItems
        }
    }

    init {
        // Report initial state.
        onMenuBuilderChanged(BrowserMenuBuilder(coreMenuItems))

        // Observe account state changes, and update menu item builder with a new set of items.
        context.components.backgroundServices.accountManagerAvailableQueue.runIfReadyOrQueue {
            // This task isn't relevant if our parent fragment isn't around anymore.
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@runIfReadyOrQueue
            }
            context.components.backgroundServices.accountManager.register(object : AccountObserver {
                override fun onAuthenticationProblems() {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(BrowserMenuBuilder(
                            listOf(reconnectToSyncItem) + coreMenuItems
                        ))
                    }
                }

                override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(
                            BrowserMenuBuilder(
                                coreMenuItems
                            )
                        )
                    }
                }

                override fun onLoggedOut() {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(
                            BrowserMenuBuilder(
                                coreMenuItems
                            )
                        )
                    }
                }
            }, lifecycleOwner)
        }
    }
}
