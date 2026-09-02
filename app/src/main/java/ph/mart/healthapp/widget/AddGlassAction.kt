package ph.mart.healthapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.WaterRepository
import ph.mart.healthapp.today.addGlass

/**
 * The widget's only write, and the same one the watch sends — both go through
 * [addGlass], so neither can cap at a different goal or read a staler count than the other.
 */
class AddGlassAction : ActionCallback, KoinComponent {

    private val waterRepository: WaterRepository by inject()
    private val profileRepository: ProfileRepository by inject()

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        addGlass(waterRepository, profileRepository)
        // The Application's collector will fire too, but only once Room emits; pushing here keeps
        // the tap feeling immediate even on a cold-started process.
        TodayWidget().update(context, glanceId)
    }
}
