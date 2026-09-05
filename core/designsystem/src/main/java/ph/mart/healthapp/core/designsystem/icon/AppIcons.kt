package ph.mart.healthapp.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector

data class DualStateIcon(val filled: ImageVector, val outlined: ImageVector)

object AppIcons {
    val Home = DualStateIcon(Icons.Filled.Home, Icons.Outlined.Home)
    val Food = DualStateIcon(Icons.Filled.Restaurant, Icons.Outlined.Restaurant)
    val Progress = DualStateIcon(Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
    val Profile = DualStateIcon(Icons.Filled.Person, Icons.Outlined.Person)
    val Favorite = DualStateIcon(Icons.Filled.Star, Icons.Outlined.StarBorder)

    val Add: ImageVector = Icons.Filled.Add
    val Barcode: ImageVector = Icons.Filled.QrCodeScanner
    val Bookmark: ImageVector = Icons.Filled.BookmarkAdd
    val Camera: ImageVector = Icons.Filled.PhotoCamera
    val AiSparkle: ImageVector = Icons.Filled.AutoAwesome
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val ChevronDown: ImageVector = Icons.Filled.KeyboardArrowDown
    val ChevronLeft: ImageVector = Icons.Filled.KeyboardArrowLeft
    val ChevronRight: ImageVector = Icons.Filled.KeyboardArrowRight
    val Close: ImageVector = Icons.Filled.Close
    val Compare: ImageVector = Icons.Filled.SwapHoriz
    val Delete: ImageVector = Icons.Outlined.Delete
    val DragHandle: ImageVector = Icons.Filled.DragHandle
    val Edit: ImageVector = Icons.Outlined.Edit
    val Filter: ImageVector = Icons.Filled.FilterList
    val Flash: ImageVector = Icons.Filled.FlashOn
    val Gallery: ImageVector = Icons.Filled.PhotoLibrary
    val Mic: ImageVector = Icons.Filled.Mic
    val Pause: ImageVector = Icons.Filled.Pause
    val Play: ImageVector = Icons.Filled.PlayArrow
    /** Exercise, wherever it has to read as its own kind of thing rather than a fifth meal. */
    val Run: ImageVector = Icons.Filled.DirectionsRun
    val Send: ImageVector = Icons.AutoMirrored.Filled.Send
    val Share: ImageVector = Icons.Filled.Share
    val Streak: ImageVector = Icons.Filled.LocalFireDepartment
    val TrendDown: ImageVector = Icons.Filled.ArrowDownward
    /** The third trend glyph — a movement too small to call, or too few readings to call one.
     * Colour alone never carries the verdict, so a neutral trend needs a glyph of its own. */
    val TrendFlat: ImageVector = Icons.Filled.Remove
    val TrendUp: ImageVector = Icons.Filled.ArrowUpward
}
