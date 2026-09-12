package ph.mart.healthapp.core.navigation.route

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

/**
 * A flattened multi-back-stack navigator: one stack per top-level tab, all kept live so switching
 * tabs preserves each tab's history. Adapted from the Nav3 "Common UI" recipe.
 *
 * [backStack] is handed in rather than created here so the caller can supply a *saveable* list —
 * this whole navigator used to sit behind a plain `remember`, so a rotation, a font-scale change,
 * a locale change or a process-death restore dropped the user back on the start tab, with any
 * open sheet (which is `rememberSaveable`) still floating over it.
 *
 * The per-tab grouping is **derived** from that flat list rather than saved alongside it: every
 * group begins with its own [TopLevelDestination] route, so a restore reconstructs the map exactly
 * and the two can never disagree about where a tab's history starts.
 */
class TopLevelBackStack(val backStack: MutableList<NavKey>) {

    private val topLevelStacks = regroup()

    /** Always the most recently visited tab, which is the last key in insertion order. */
    var topLevelKey by mutableStateOf(topLevelStacks.keys.last())
        private set

    /**
     * The flat list back into one stack per tab: a key that names a tab opens a new group, and
     * everything after it belongs to that group until the next one.
     *
     * A restored list holding no tab root at all is not a stack this class can navigate — that
     * can only come from a mangled `Bundle`, and starting over on Home beats throwing on the
     * empty map that [topLevelKey] would otherwise read.
     */
    private fun regroup(): LinkedHashMap<NavKey, SnapshotStateList<NavKey>> {
        val roots = TopLevelDestination.entries.mapTo(mutableSetOf()) { it.route }
        val stacks = linkedMapOf<NavKey, SnapshotStateList<NavKey>>()
        var current: SnapshotStateList<NavKey>? = null
        backStack.forEach { key ->
            if (key in roots) {
                current = mutableStateListOf(key)
                stacks[key] = current
            } else {
                current?.add(key)
            }
        }
        if (stacks.isEmpty()) {
            val home = TopLevelDestination.Home.route
            stacks[home] = mutableStateListOf(home)
            backStack.clear()
            backStack.add(home)
        }
        return stacks
    }

    private fun updateBackStack() = backStack.apply {
        clear()
        addAll(topLevelStacks.flatMap { it.value })
    }

    fun addTopLevel(key: NavKey) {
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            topLevelStacks.apply { remove(key)?.let { put(key, it) } }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: NavKey) {
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        topLevelStacks.remove(removedKey)
        // `lastOrNull`, because the line above can empty the map: popping a tab's own root removes
        // its group, and if that was the only group there is no previous tab to fall back to.
        // `NavDisplay` does not dispatch back at a stack of one, so today nothing reaches it — but
        // the alternative to this fallback is a NoSuchElementException, and [regroup] already
        // answers the same question the same way.
        topLevelKey = topLevelStacks.keys.lastOrNull() ?: TopLevelDestination.Home.route.also {
            topLevelStacks[it] = mutableStateListOf(it)
        }
        updateBackStack()
    }
}
