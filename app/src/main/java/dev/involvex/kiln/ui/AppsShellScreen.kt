package dev.involvex.kiln.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.R
import dev.involvex.kiln.feature.apps.AppsRoute
import dev.involvex.kiln.feature.sources.discovery.DiscoveryRoute
import dev.involvex.kiln.feature.sources.discovery.TrendingRoute

private enum class AppsShellSegment {
    LIBRARY,
    DISCOVER,
    TRENDING,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsShellRoute(
    onOpenDetail: (String) -> Unit,
    onOpenCatalogDetail: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var segmentIndex by rememberSaveable { mutableIntStateOf(0) }
    val segment = AppsShellSegment.entries[segmentIndex]
    Column(modifier = modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AppsShellSegment.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = segmentIndex == index,
                    onClick = { segmentIndex = index },
                    shape = SegmentedButtonDefaults.itemShape(index, AppsShellSegment.entries.size),
                ) {
                    Text(
                        when (item) {
                            AppsShellSegment.LIBRARY -> stringResource(R.string.apps_segment_library)
                            AppsShellSegment.DISCOVER -> stringResource(R.string.apps_segment_discover)
                            AppsShellSegment.TRENDING -> stringResource(R.string.apps_segment_trending)
                        },
                    )
                }
            }
        }
        when (segment) {
            AppsShellSegment.LIBRARY -> {
                AppsRoute(
                    onOpenDetail = onOpenDetail,
                    modifier = Modifier.weight(1f),
                )
            }

            AppsShellSegment.DISCOVER -> {
                DiscoveryRoute(
                    onBack = {},
                    embedded = true,
                    showTopBar = false,
                    onOpenCatalogDetail = onOpenCatalogDetail,
                    modifier = Modifier.weight(1f),
                )
            }

            AppsShellSegment.TRENDING -> {
                TrendingRoute(
                    onOpenCatalogDetail = onOpenCatalogDetail,
                    showTopBar = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
