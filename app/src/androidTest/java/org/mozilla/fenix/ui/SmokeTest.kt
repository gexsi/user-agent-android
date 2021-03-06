/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.view.View
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.RecyclerViewIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.ViewVisibilityIdlingResource
import org.mozilla.fenix.ui.robots.clickUrlbar
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 * Test Suite that contains tests defined as part of the Smoke and Sanity check defined in Test rail.
 * These tests will verify different functionalities of the app as a way to quickly detect regressions in main areas
 */

class SmokeTest {
    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var mockWebServer: MockWebServer
    private var awesomeBar: ViewVisibilityIdlingResource? = null
    private var searchSuggestionsIdlingResource: RecyclerViewIdlingResource? = null

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // copied over from HomeScreenTest
    @Test
    fun firstRunScreenTest() {
        homeScreen {
            verifyHomeScreen()
            verifyNavigationToolbar()
            verifyHomePrivateBrowsingButton()
            verifyHomeMenu()
            verifyHomeWordmark()

            verifyWelcomeHeader()
            // Sign in to Firefox
            verifyStartSyncHeader()
            verifyAccountsSignInButton()

            // Intro to other sections
            verifyGetToKnowHeader()

            // Automatic privacy
            scrollToElementByText("Automatic privacy")
            verifyAutomaticPrivacyHeader()
            verifyTrackingProtectionToggle()
            verifyAutomaticPrivacyText()

            // Choose your theme
            verifyChooseThemeHeader()
            verifyChooseThemeText()
            verifyDarkThemeDescription()
            verifyDarkThemeToggle()
            verifyLightThemeDescription()
            verifyLightThemeToggle()

            // Browse privately
            verifyBrowsePrivatelyHeader()
            verifyBrowsePrivatelyText()

            // Take a position
            verifyTakePositionHeader()
            verifyTakePositionElements()

            // Your privacy
            verifyYourPrivacyHeader()
            verifyYourPrivacyText()
            verifyPrivacyNoticeButton()

            // Start Browsing
            verifyStartBrowsingButton()
        }
    }

    @Test
    fun verifyBasicNavigationToolbarFunctionality() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                mDevice.waitForIdle()
                verifyNavURLBarItems()
            }.openNavigationToolbar {
            }.goBackToWebsite {
            }.openTabDrawer {
                verifyExistingTabList()
            }.openNewTab {
            }.dismissSearchBar {
                verifyHomeScreen()
            }
        }
    }

    @Test
    fun verifyPageMainMenuItemsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        // Add this to check openInApp and youtube is a default app available in every Android emulator/device
        val youtubeUrl = "www.youtube.com"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            verifyThreeDotMainMenuItems()
        }.openHistory {
            verifyHistoryMenuView()
        }.goBackToBrowser {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarksMenuView()
        }.goBackToBrowser {
        }.openThreeDotMenu {
        }.openSyncedTabs {
            verifySyncedTabsMenuHeader()
        }.goBack {
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsView()
        }.goBackToBrowser {
        }.openThreeDotMenu {
        }.openFindInPage {
            verifyFindInPageSearchBarItems()
        }.closeFindInPage {
        }.openThreeDotMenu {
        }.addToFirefoxHome {
            verifySnackBarText("Added to top sites!")
        }.openTabDrawer {
        }.openNewTab {
        }.dismissSearchBar {
            verifyExistingTopSitesTabs(defaultWebPage.title)
        }.openTabDrawer {
        }.openTab(defaultWebPage.title) {
        }.openThreeDotMenu {
        }.openAddToHomeScreen {
            verifyShortcutNameField(defaultWebPage.title)
            clickAddShortcutButton()
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(defaultWebPage.title) {
        }.openThreeDotMenu {
        }.openSaveToCollection {
            verifyCollectionNameTextField()
        }.exitSaveCollection {
        }.openThreeDotMenu {
        }.bookmarkPage {
            verifySnackBarText("Bookmark saved!")
        }.openThreeDotMenu {
        }.sharePage {
            verifyShareAppsLayout()
        }.closeShareDialogReturnToPage {
        }.openThreeDotMenu {
        }.refreshPage {
            verifyUrl(defaultWebPage.url.toString())
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(youtubeUrl.toUri()) {
        }.openThreeDotMenu {
            verifyOpenInAppButton()
        }
    }

    @Test
    fun verifyETPShieldNotDisplayedIfOFFGlobally() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            clickEnhancedTrackingProtectionDefaults()
            verifyEnhancedTrackingProtectionOptionsGrayedOut()
        }.goBackToHomeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                verifyEnhancedTrackingProtectionPanelNotVisible()
            }.openThreeDotMenu {
            }.openSettings {
            }.openEnhancedTrackingProtectionSubMenu {
                clickEnhancedTrackingProtectionDefaults()
            }.goBack {
            }.goBackToBrowser {
                clickEnhancedTrackingProtectionPanel()
                verifyEnhancedTrackingProtectionSwitch()
                // Turning off TP Switch results in adding the WebPage to exception list
                clickEnhancedTrackingProtectionSwitchOffOn()
            }
        }
    }

    @Test
    fun verifySearchEngineCanBeChangedTemporarilyUsingShortcuts() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openSearch {
            verifyKeyboardVisibility()
            clickSearchEngineShortcutButton()
            verifySearchEngineList()
            changeDefaultSearchEngine("Amazon.com")
            verifySearchEngineIcon("Amazon.com")
        }.goToSearchEngine {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer {
        }.openNewTab {
            clickSearchEngineShortcutButton()
            mDevice.waitForIdle()
            changeDefaultSearchEngine("Bing")
            verifySearchEngineIcon("Bing")
        }.goToSearchEngine {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer {
        }.openNewTab {
            clickSearchEngineShortcutButton()
            mDevice.waitForIdle()
            changeDefaultSearchEngine("DuckDuckGo")
            verifySearchEngineIcon("DuckDuckGo")
        }.goToSearchEngine {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer {
        }.openNewTab {
            clickSearchEngineShortcutButton()
            changeDefaultSearchEngine("Wikipedia")
            verifySearchEngineIcon("Wikipedia")
        }.goToSearchEngine {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer {
            // Checking whether the next search will be with default or not
        }.openNewTab {
        }.goToSearchEngine {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openNavigationToolbar {
            clickUrlbar {
                verifyDefaultSearchEngine("Google")
            }
        }
    }

    @Test
    fun addPredefinedSearchEngineTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openSearchSubMenu {
            openAddSearchEngineMenu()
            verifyAddSearchEngineList()
            addNewSearchEngine("YouTube")
            verifyEngineListContains("YouTube")
        }.goBack {
        }.goBack {
        }.openSearch {
            verifyKeyboardVisibility()
            clickSearchEngineShortcutButton()
            verifyEnginesListShortcutContains("YouTube")
        }
    }

    @Test
    fun toggleSearchSuggestions() {
        // Goes through the settings and changes the search suggestion toggle, then verifies it changes.
        homeScreen {
        }.openNavigationToolbar {
            typeSearchTerm("mozilla")
            val awesomeBarView = getAwesomebarView()
            awesomeBarView?.let {
                awesomeBar = ViewVisibilityIdlingResource(it, View.VISIBLE)
            }
            IdlingRegistry.getInstance().register(awesomeBar!!)
            searchSuggestionsIdlingResource =
                RecyclerViewIdlingResource(awesomeBarView as RecyclerView, 1)
            IdlingRegistry.getInstance().register(searchSuggestionsIdlingResource!!)
            verifySearchSuggestionsAreMoreThan(0)
            IdlingRegistry.getInstance().unregister(searchSuggestionsIdlingResource!!)
        }.goBack {
        }.openThreeDotMenu {
        }.openSettings {
        }.openSearchSubMenu {
            disableShowSearchSuggestions()
        }.goBack {
        }.goBack {
        }.openNavigationToolbar {
            typeSearchTerm("mozilla")
            searchSuggestionsIdlingResource =
                RecyclerViewIdlingResource(getAwesomebarView() as RecyclerView)
            IdlingRegistry.getInstance().register(searchSuggestionsIdlingResource!!)
            verifySearchSuggestionsAreEqualTo(0)
            IdlingRegistry.getInstance().unregister(searchSuggestionsIdlingResource!!)
        }
    }

    // This finds the dialog fragment child of the homeFragment, otherwise the awesomeBar would return null
    private fun getAwesomebarView(): View? {
        val homeFragment = activityTestRule.activity.supportFragmentManager.primaryNavigationFragment
        val searchDialogFragment = homeFragment?.childFragmentManager?.fragments?.first {
            it.javaClass.simpleName == "SearchDialogFragment"
        }
        return searchDialogFragment?.view?.findViewById(R.id.awesome_bar)
    }
}
