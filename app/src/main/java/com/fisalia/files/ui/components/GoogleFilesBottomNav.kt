package com.fisalia.files.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fisalia.files.R
import com.fisalia.files.ui.theme.GoogleBlueContainer
import com.fisalia.files.ui.theme.GoogleBlueOnContainer
import com.fisalia.files.ui.theme.GoogleBluePrimary

enum class NavTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CLEAN(R.string.tab_clean, Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices),
    BROWSE(R.string.tab_browse, Icons.Filled.Folder, Icons.Outlined.Folder),
    NETWORK(R.string.tab_network, Icons.Filled.Dns, Icons.Outlined.Dns)
}

@Composable
fun GoogleFilesBottomNav(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = stringResource(tab.titleRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GoogleBlueOnContainer,
                    selectedTextColor = GoogleBluePrimary,
                    indicatorColor = GoogleBlueContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
