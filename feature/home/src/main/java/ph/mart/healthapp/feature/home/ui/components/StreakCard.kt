package ph.mart.healthapp.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ph.mart.healthapp.core.data.streak.StreakBadge
import ph.mart.healthapp.core.data.streak.StreakStats
import ph.mart.healthapp.core.data.streak.earnedBadges
import ph.mart.healthapp.core.designsystem.component.BADGE_DOT_SIZE
import ph.mart.healthapp.core.designsystem.component.BadgeDot
import ph.mart.healthapp.core.designsystem.theme.AppTheme
import ph.mart.healthapp.feature.home.R

/** Five badges have to fit inside a paired card's 126dp of content on a 360dp screen. */
private val PairedBadgeSize = 22.dp

/**
 * Consistency, not today's numbers. Every value is derived in `:core:data/streak` — this card only
 * formats.
 *
 * The badges are the whole of the card's second line now. The two sentences it used to carry —
 * "10 days to your 100-day badge." and the weight-badge line — are gone: the unlit badge already
 * says what is next, and the weight line said what the Weight card says one row over. The dot
 * lighting up is still the entire reward, which is why there is no celebration here and never was.
 *
 * The status mark is [StatusMark.OnTrack] only while a streak is actually running; a broken one is
 * neutral rather than [StatusMark.OffTrack], because a day you have not logged *yet* is not a
 * failure the app should be marking in `error` at breakfast.
 */
@Composable
fun StreakCard(streak: StreakStats, wide: Boolean, modifier: Modifier = Modifier) {
    val earned = streak.earnedBadges()
    MetricCard(
        label = stringResource(R.string.home_streak_title),
        value = "${streak.current}",
        unit = pluralStringResource(R.plurals.home_streak_days, streak.current),
        wide = wide,
        status = if (streak.current > 0) StatusMark.OnTrack else StatusMark.None,
        modifier = modifier,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StreakBadge.entries.forEach { badge ->
                BadgeDot(
                    label = badge.days.toString(),
                    earned = badge in earned,
                    description = stringResource(R.string.home_streak_badge_desc, badge.days),
                    size = if (wide) BADGE_DOT_SIZE else PairedBadgeSize,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StreakCardPreview() {
    AppTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                StreakCard(StreakStats(current = 12, best = 31, totalDaysLogged = 74), wide = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StreakCard(
                        StreakStats(current = 90, best = 90, totalDaysLogged = 140),
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                    StreakCard(
                        StreakStats(current = 0, best = 4, totalDaysLogged = 9),
                        wide = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
