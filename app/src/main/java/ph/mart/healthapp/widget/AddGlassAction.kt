package ph.mart.healthapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * The widget's only write. Both the count and the goal are re-read here rather than carried in
 * as action parameters: parameters are baked into the RemoteViews at composition time, so a widget
 * that has been sitting on the home screen since breakfast would add a glass to a stale count.
 *
 * Capped at the goal, matching [TodayWidgetState.glassesAfterAdd] — and the widget stops offering
 * the button at that point, so the cap should never actually bite.
 */
class AddGlassAction : ActionCallback, KoinComponent {

    private val waterRepository: WaterRepository by inject()
    private val profileRepository: ProfileRepository by inject()

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val goal = profileRepository.observeProfile().first()?.waterGoalGlasses
            ?: DEFAULT_WATER_GOAL_GLASSES
        val glasses = waterRepository.observeToday().first()
        if (glasses >= goal) return
        waterRepository.setToday(glasses + 1)
        // The Application's collector will fire too, but only once Room emits; pushing here keeps
        // the tap feeling immediate even on a cold-started process.
        TodayWidget().update(context, glanceId)
    }
}
