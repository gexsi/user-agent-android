/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import mozilla.components.lib.crash.CrashReporter
/* Gexsi begin
import mozilla.components.lib.crash.service.CrashReporterService
import mozilla.components.lib.crash.service.GleanCrashReporterService
import mozilla.components.lib.crash.service.MozillaSocorroService
import mozilla.components.lib.crash.service.SentryService
 Gexsi end */
import mozilla.components.service.nimbus.Nimbus
/* Gexsi begin
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.Config
Gexsi end */
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
/* Gexsi begin
import org.mozilla.fenix.ReleaseChannel
import org.mozilla.fenix.components.metrics.AdjustMetricsService
import org.mozilla.fenix.components.metrics.GleanMetricsService
Gexsi end */
import org.mozilla.fenix.components.metrics.LeanplumMetricsService
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.crashes.DummyCrashReporter
/* Gexsi begin
import org.mozilla.fenix.ext.components
Gexsi end */
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.perf.lazyMonitored
import org.mozilla.fenix.utils.Mockable
/* Gexsi begin
import org.mozilla.geckoview.BuildConfig.MOZ_APP_BUILDID
import org.mozilla.geckoview.BuildConfig.MOZ_APP_VENDOR
import org.mozilla.geckoview.BuildConfig.MOZ_APP_VERSION
import org.mozilla.geckoview.BuildConfig.MOZ_UPDATE_CHANNEL
Gexsi end */
/**
 * Component group for all functionality related to analytics e.g. crash reporting and telemetry.
 */
@Mockable
class Analytics(
    private val context: Context
) {
    val crashReporter: CrashReporter by lazyMonitored {
        /* Gexsi begin: disable the CrashReporter
        val services = mutableListOf<CrashReporterService>()

        if (isSentryEnabled()) {
            val sentryService = SentryService(
                context,
                BuildConfig.SENTRY_TOKEN,
                tags = mapOf("geckoview" to "$MOZ_APP_VERSION-$MOZ_APP_BUILDID"),
                environment = BuildConfig.BUILD_TYPE,
                sendEventForNativeCrashes = false, // Do not send native crashes to Sentry
                sentryProjectUrl = getSentryProjectUrl()
            )

            services.add(sentryService)
        }

        // The name "Fenix" here matches the product name on Socorro and is unrelated to the actual app name:
        // https://bugzilla.mozilla.org/show_bug.cgi?id=1523284
        val socorroService = MozillaSocorroService(context, appName = "Fenix",
            version = MOZ_APP_VERSION, buildId = MOZ_APP_BUILDID, vendor = MOZ_APP_VENDOR,
            releaseChannel = MOZ_UPDATE_CHANNEL)
        services.add(socorroService)
        Gexsi end */
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            0
        )

        CrashReporter(
            context = context,

            // Gexsi begin: disable Glean crash reporter
//            services = services,
//            telemetryServices = listOf(GleanCrashReporterService(context)),
//            shouldPrompt = CrashReporter.Prompt.ALWAYS,
            services = listOf(DummyCrashReporter()),
            telemetryServices = listOf(),
            shouldPrompt = CrashReporter.Prompt.NEVER,
            promptConfiguration = CrashReporter.PromptConfiguration(
                appName = context.getString(R.string.app_name),
//                organizationName = "Mozilla"
                organizationName = "Gexsi"
            ),
            // Gexsi end

            enabled = true,
            nonFatalCrashIntent = pendingIntent
        )
    }

    val leanplumMetricsService by lazyMonitored { LeanplumMetricsService(context as Application) }

    val metrics: MetricController by lazyMonitored {
        MetricController.create(
            listOf(),
            /* Gexsi begin: removing all the Metrics
            listOf(
                GleanMetricsService(context, lazy { context.components.core.store }),
                leanplumMetricsService,
                AdjustMetricsService(context as Application)
            ),
            isDataTelemetryEnabled = { context.settings().isTelemetryEnabled },
            isMarketingDataTelemetryEnabled = { context.settings().isMarketingTelemetryEnabled }
            */
            isDataTelemetryEnabled = { false },
            isMarketingDataTelemetryEnabled = { false }
            /* Gexsi end */
        )
    }

    val experiments by lazyMonitored {
        Nimbus(context, server = null).apply {
            if (FeatureFlags.nimbusExperiments) {
                initialize()
                // Global opt out state is stored in Nimbus, and shouldn't be toggled to `true`
                // from the app unless the user does so from a UI control.
                // However, the user may have opt-ed out of mako experiments already, so
                // we should respect that setting here.
                val enabled = context.settings().isExperimentationEnabled
                if (!enabled) {
                    globalUserParticipation = enabled
                }
                context.settings().isExperimentationEnabled = globalUserParticipation
            }
        }
    }
}

// Gexsi begin: disable sentry
fun isSentryEnabled() = false
//fun isSentryEnabled() = !BuildConfig.SENTRY_TOKEN.isNullOrEmpty()
// Gexsi end

/* Gexsi begin: don't use Sentry
private fun getSentryProjectUrl(): String? {
    val baseUrl = "https://sentry.prod.mozaws.net/operations"
    return when (Config.channel) {
        ReleaseChannel.Nightly -> "$baseUrl/fenix"
        ReleaseChannel.Release -> "$baseUrl/fenix-fennec"
        ReleaseChannel.Beta -> "$baseUrl/fenix-fennec-beta"
        else -> null
    }
}
Gexsi end */