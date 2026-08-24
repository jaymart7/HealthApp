package ph.mart.healthapp.core.designsystem.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

data class DualStateIcon(val filled: ImageVector, val outlined: ImageVector)

object AppIcons {
    val Home = DualStateIcon(Icons.Filled.Home, Icons.Outlined.Home)
    val Food = DualStateIcon(Icons.Filled.Restaurant, Icons.Outlined.Restaurant)
    val Progress = DualStateIcon(Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp)
    val Profile = DualStateIcon(Icons.Filled.Person, Icons.Outlined.Person)

    val Add: ImageVector = Icons.Filled.Add
    val AiSparkle: ImageVector = Icons.Filled.AutoAwesome
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val ChevronDown: ImageVector = Icons.Filled.KeyboardArrowDown
    val ChevronLeft: ImageVector = Icons.Filled.KeyboardArrowLeft
    val ChevronRight: ImageVector = Icons.Filled.KeyboardArrowRight
    val Close: ImageVector = Icons.Filled.Close
    val Flash: ImageVector = Icons.Filled.FlashOn
    val Gallery: ImageVector = Icons.Filled.PhotoLibrary
}
