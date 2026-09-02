package ph.mart.healthapp.today

import kotlinx.coroutines.flow.first
import ph.mart.healthapp.core.data.profile.ProfileRepository
import ph.mart.healthapp.core.data.water.DEFAULT_WATER_GOAL_GLASSES
import ph.mart.healthapp.core.data.water.WaterRepository

/**
 * One glass, from whichever surface asked — the widget's button or the watch's. Both the count
 * and the goal are re-read here rather than carried in by the caller: a widget that has been
 * sitting on the home screen since breakfast, or a watch showing a snapshot from an hour ago,
 * would otherwise add a glass to a stale count.
 *
 * Returns false when the day is already at its goal, which is also where both surfaces stop
 * offering the control — the cap should never actually bite.
 */
suspend fun addGlass(
    waterRepository: WaterRepository,
    profileRepository: ProfileRepository,
): Boolean {
    val goal = profileRepository.observeProfile().first()?.waterGoalGlasses
        ?: DEFAULT_WATER_GOAL_GLASSES
    val glasses = waterRepository.observeToday().first()
    if (glasses >= goal) return false
    waterRepository.setToday(glasses + 1)
    return true
}
