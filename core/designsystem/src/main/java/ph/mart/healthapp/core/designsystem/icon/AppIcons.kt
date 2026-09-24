package ph.mart.healthapp.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EggAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class DualStateIcon(val filled: ImageVector, val outlined: ImageVector)

object AppIcons {
    val Home = DualStateIcon(Icons.Filled.Home, Icons.Outlined.Home)
    val Food = DualStateIcon(Icons.Filled.Restaurant, Icons.Outlined.Restaurant)
    val Progress = DualStateIcon(Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
    val Profile = DualStateIcon(Icons.Filled.Person, Icons.Outlined.Person)
    val Favorite = DualStateIcon(Icons.Filled.Star, Icons.Outlined.StarBorder)

    val Add: ImageVector = Icons.Filled.Add
    /** Onboarding's "maintain" goal — a pair of scales, not the bathroom kind. */
    val Balance: ImageVector = Icons.Filled.Balance
    val Bedtime: ImageVector = Icons.Outlined.Bedtime
    /** The progress photo, wherever it has to read as a body shot rather than a plate —
     * [Camera] is the plate. */
    val AddPhoto: ImageVector = Icons.Outlined.AddAPhoto
    /** Onward to the place a thing landed — the coach's door into the meal it just wrote to.
     * Auto-mirrored, because "forward" is the reading direction and not a compass point. */
    val ArrowForward: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
    /** The composer's send glyph. Same vector as [TrendUp] and deliberately its own name: one
     * of them means "send this" and the other means "this went up". */
    val ArrowUp: ImageVector = Icons.Filled.ArrowUpward
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Barcode: ImageVector = Icons.Filled.QrCodeScanner
    /** Notifications, wherever reminders are the subject rather than a single alert. */
    val Bell: ImageVector = Icons.Outlined.Notifications
    val Book: ImageVector = Icons.AutoMirrored.Outlined.MenuBook
    val Bookmark: ImageVector = Icons.Filled.BookmarkAdd
    val Camera: ImageVector = Icons.Filled.PhotoCamera
    val Chair: ImageVector = Icons.Outlined.Chair
    val Check: ImageVector = Icons.Filled.Check
    /** The answered state of a selectable card — [Check] is the bare mark, this is the mark that
     * says a choice has landed. */
    val CheckCircle: ImageVector = Icons.Filled.CheckCircle
    /** [CheckCircle]'s unticked half — a checklist row nobody has tapped yet. The pair is what
     * Home's supplements card and the Progress page's catch-up checklist both draw. */
    val Circle: ImageVector = Icons.Outlined.Circle
    /** The packaged-food lookup could not be reached. Never the offline state: a local list
     * answering with no network is the feature, and only a *failed* ask gets a glyph. */
    val CloudOff: ImageVector = Icons.Outlined.CloudOff
    val AiSparkle: ImageVector = Icons.Filled.AutoAwesome
    val ChevronDown: ImageVector = Icons.Filled.KeyboardArrowDown
    val ChevronUp: ImageVector = Icons.Filled.KeyboardArrowUp
    val ChevronLeft: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft
    val ChevronRight: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight
    val Close: ImageVector = Icons.Filled.Close
    val Compare: ImageVector = Icons.Filled.SwapHoriz
    val Copy: ImageVector = Icons.Filled.ContentCopy
    val Delete: ImageVector = Icons.Outlined.Delete
    /** Clear a whole conversation at once, as against [Delete]'s one row. */
    val DeleteSweep: ImageVector = Icons.Outlined.DeleteSweep
    val DragHandle: ImageVector = Icons.Filled.DragHandle
    val Dumbbell: ImageVector = Icons.Outlined.FitnessCenter
    val Edit: ImageVector = Icons.Outlined.Edit
    val Egg: ImageVector = Icons.Outlined.EggAlt
    val Filter: ImageVector = Icons.Filled.FilterList
    /** A figure the app worked out rather than one the user typed — onboarding's derivation line
     * is the only place a calculation shows its own arithmetic. */
    val Formula: ImageVector = Icons.Filled.Functions
    val Flash: ImageVector = Icons.Filled.FlashOn
    val Heart: ImageVector = Icons.Outlined.FavoriteBorder
    /** Nothing logged yet — the recents list's own empty state. */
    val History: ImageVector = Icons.Outlined.History
    val Gallery: ImageVector = Icons.Filled.PhotoLibrary
    val Info: ImageVector = Icons.Outlined.Info
    /** An outbound connection to another service — Google Health, and whatever follows it. */
    val Link: ImageVector = Icons.Filled.Link
    /** The connection that was offered and declined. Never an error glyph: not connecting is an
     * answer, not a fault. */
    val LinkOff: ImageVector = Icons.Filled.LinkOff
    val Lock: ImageVector = Icons.Outlined.Lock
    val Mic: ImageVector = Icons.Filled.Mic
    val More: ImageVector = Icons.Filled.MoreHoriz
    /** The overflow in a top bar, where [More] is the one that sits in a row of content. */
    val MoreVert: ImageVector = Icons.Filled.MoreVert
    /** The minus of a stepper. Same vector as [TrendFlat] and deliberately its own name: one of
     * them means "take some away" and the other means "this has not moved". */
    val Minus: ImageVector = Icons.Filled.Remove
    val Pause: ImageVector = Icons.Filled.Pause
    val Play: ImageVector = Icons.Filled.PlayArrow
    val Plant: ImageVector = Icons.Outlined.Grass
    /** Ask again for something that didn't come back. Never a sync glyph — see [SyncProblem]. */
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Search: ImageVector = Icons.Filled.Search
    /** Nothing matched — the search's own empty state, distinct from [CloudOff]'s could-not-ask. */
    val SearchOff: ImageVector = Icons.Outlined.SearchOff
    val Settings: ImageVector = Icons.Outlined.Settings
    val Smartphone: ImageVector = Icons.Outlined.Smartphone
    val Steps: ImageVector = Icons.AutoMirrored.Outlined.DirectionsWalk
    val Supplement: ImageVector = Icons.Outlined.Medication
    /** A round trip that failed — the coach's answer that never arrived. Not [CloudOff], which
     * is the connection being gone rather than the call going wrong. */
    val SyncProblem: ImageVector = Icons.Outlined.SyncProblem
    val Timer: ImageVector = Icons.Outlined.Timer
    val Undo: ImageVector = Icons.AutoMirrored.Filled.Undo
    /** Exercise, wherever it has to read as its own kind of thing rather than a fifth meal. */
    val Run: ImageVector = Icons.AutoMirrored.Filled.DirectionsRun
    val Send: ImageVector = Icons.AutoMirrored.Filled.Send
    val Share: ImageVector = Icons.Filled.Share
    /** Abandon something already running — the coach's answer mid-stream. Not [Close], which
     * dismisses a surface the user opened. */
    val Stop: ImageVector = Icons.Filled.Stop
    /** A turn the user stopped, marked in the transcript afterwards. [Stop] is the control;
     * this is the record of having pressed it. */
    val StopCircle: ImageVector = Icons.Filled.StopCircle
    val Streak: ImageVector = Icons.Filled.LocalFireDepartment
    val TrendDown: ImageVector = Icons.Filled.ArrowDownward
    /** The third trend glyph — a movement too small to call, or too few readings to call one.
     * Colour alone never carries the verdict, so a neutral trend needs a glyph of its own. */
    val TrendFlat: ImageVector = Icons.Filled.Remove
    val TrendUp: ImageVector = Icons.Filled.ArrowUpward
    /** A downward *trend*, not a downward step: [TrendDown] is the arrow beside a weight delta,
     * this is the shape of losing weight over time, which is what onboarding's goal card means. */
    val TrendingDown: ImageVector = Icons.AutoMirrored.Filled.TrendingDown
    /** A caution the user may proceed past — the calorie floor. Never paired with an
     * `errorContainer` surface; see the floor warning's own comment. */
    val Warning: ImageVector = Icons.Outlined.WarningAmber
    val Water: ImageVector = Icons.Outlined.WaterDrop
    /** Onboarding's "very active" — the hardest of the four, which [Run] and [Steps] already
     * spend on the two below it. */
    val Workout: ImageVector = Icons.Outlined.SportsGymnastics
    val Weight: ImageVector = Icons.Outlined.MonitorWeight
}
