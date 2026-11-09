package dev.involvex.kiln.feature.sources.discovery

import androidx.annotation.StringRes
import dev.involvex.kiln.feature.sources.R

object AntiFeatureLabels {
    @StringRes
    fun labelRes(id: String): Int? = when (id) {
        "Ads" -> R.string.anti_feature_ads
        "Tracking" -> R.string.anti_feature_tracking
        "NonFreeNet" -> R.string.anti_feature_non_free_net
        "NonFreeAdd" -> R.string.anti_feature_non_free_add
        "NonFreeDep" -> R.string.anti_feature_non_free_dep
        "NonFreeAssets" -> R.string.anti_feature_non_free_assets
        "UpstreamNonFree" -> R.string.anti_feature_upstream_non_free
        "DisabledAlgorithm" -> R.string.anti_feature_disabled_algorithm
        "KnownVuln" -> R.string.anti_feature_known_vuln
        "NoSourceSince" -> R.string.anti_feature_no_source_since
        "NSFW" -> R.string.anti_feature_nsfw
        "TetheredNet" -> R.string.anti_feature_tethered_net
        else -> null
    }
}
