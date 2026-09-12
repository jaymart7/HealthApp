package ph.mart.healthapp.feature.food.ui.search.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.designsystem.component.TextButton
import ph.mart.healthapp.core.designsystem.icon.AppIcons
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.food.R
import ph.mart.healthapp.feature.food.ui.search.OnlineSearch

/** How many placeholder rows stand in for the answer still coming. Two, because the point is to
 * say "there is more below", not to guess how much. */
private const val SKELETON_ROWS = 2

/**
 * What the online tier has to say, at the **end of the list** rather than pinned anywhere.
 *
 * That placement is the whole design. The local tiers have already answered and their rows are
 * pickable; the packaged-food tier is a thing happening *behind* them, and behind is where the
 * list already puts it — see `searchFoods`, which folds it in last. A banner would interrupt an
 * answer that is not waiting on anything, and a spinner over the list would suggest the rows under
 * it are provisional. They are not.
 *
 * [OnlineSearch.Failed] is an inline row, never a screen: the on-device results stay exactly where
 * they were and stay pickable, and retry re-asks only the leg that failed.
 */
@Composable
internal fun ListTailStatus(
    status: OnlineSearch,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        OnlineSearch.Idle -> Unit

        OnlineSearch.Searching -> Column(modifier = modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.food_search_online_searching),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            repeat(SKELETON_ROWS) { ResultRowSkeleton() }
        }

        OnlineSearch.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Icon(
                imageVector = AppIcons.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.food_search_online_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            TextButton(label = stringResource(R.string.food_search_retry), onClick = onRetry)
        }
    }
}

/**
 * A row-shaped placeholder, sized to the [FoodItemRowVariant.Result][ph.mart.healthapp.core.designsystem.component.FoodItemRowVariant]
 * rows above it so the list does not jump when the real ones land.
 *
 * ponytail: an alpha pulse, not a gradient sweep — one `rememberInfiniteTransition`, the idiom
 * `MascotAvatar` already breathes on, and no shimmer dependency. Upgrade to a moving highlight only
 * if it reads flat on a device.
 */
@Composable
private fun ResultRowSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    // A preview capture would freeze at whatever phase it sampled; hold the rest state instead.
    val alpha = if (LocalInspectionMode.current) 1f else pulse

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Bar(widthFraction = 0.62f, height = 16.dp, radius = 8.dp, tone = MaterialTheme.colorScheme.surfaceContainerHigh)
            Bar(widthFraction = 0.44f, height = 12.dp, radius = 4.dp, tone = MaterialTheme.colorScheme.surfaceContainer)
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .width(56.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

@Composable
private fun Bar(
    widthFraction: Float,
    height: Dp,
    radius: Dp,
    tone: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(tone),
    )
}

@PreviewLightDark
@Composable
private fun ListTailSearchingPreview() {
    AppTheme {
        Surface {
            ListTailStatus(status = OnlineSearch.Searching, onRetry = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun ListTailFailedPreview() {
    AppTheme {
        Surface {
            ListTailStatus(status = OnlineSearch.Failed, onRetry = {})
        }
    }
}
