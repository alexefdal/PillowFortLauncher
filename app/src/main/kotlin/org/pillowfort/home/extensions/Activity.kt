package org.pillowfort.home.extensions

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.MenuCompat
import androidx.core.view.forEach
import com.google.android.material.color.MaterialColors
import org.pillowfort.commons.extensions.getPopupMenuTheme
import org.pillowfort.commons.extensions.getProperTextColor
import org.pillowfort.commons.extensions.isDynamicTheme
import org.pillowfort.commons.extensions.showErrorToast
import org.pillowfort.commons.helpers.isQPlus
import org.pillowfort.commons.helpers.isSPlus
import org.pillowfort.home.R
import org.pillowfort.home.activities.SettingsActivity
import org.pillowfort.home.helpers.ITEM_TYPE_FOLDER
import org.pillowfort.home.helpers.ITEM_TYPE_ICON
import org.pillowfort.home.helpers.ITEM_TYPE_WIDGET
import org.pillowfort.home.helpers.UNINSTALL_APP_REQUEST_CODE
import org.pillowfort.home.interfaces.ItemMenuListener
import org.pillowfort.home.models.HomeScreenGridItem

fun Activity.launchApp(packageName: String, activityName: String) {
    try {
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            `package` = packageName
            component = ComponentName.unflattenFromString("$packageName/$activityName")
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            startActivity(this)
        }
    } catch (e: Exception) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            startActivity(launchIntent)
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

fun Activity.launchAppInfo(packageName: String) {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        startActivity(this)
    }
}

fun Activity.canAppBeUninstalled(packageName: String): Boolean {
    return try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    } catch (ignored: Exception) {
        false
    }
}

fun Activity.uninstallApp(packageName: String) {
    Intent(Intent.ACTION_DELETE).apply {
        data = Uri.fromParts("package", packageName, null)
        startActivityForResult(this, UNINSTALL_APP_REQUEST_CODE)
    }
}

fun Activity.handleGridItemPopupMenu(
    anchorView: View,
    gridItem: HomeScreenGridItem,
    isOnAllAppsFragment: Boolean,
    listener: ItemMenuListener,
): PopupMenu {
    val contextTheme = ContextThemeWrapper(this, getPopupMenuTheme())
    return PopupMenu(contextTheme, anchorView, Gravity.TOP or Gravity.END).apply {
        if (isQPlus()) {
            setForceShowIcon(true)
        }

        inflate(R.menu.menu_app_icon)
        menu.forEach {
            val default = getProperTextColor()
            val color = if (isSPlus() && isDynamicTheme()) {
                default
            } else {
                MaterialColors.getColor(contextTheme, android.R.attr.actionMenuTextColor, default)
            }
            it.iconTintList = ColorStateList.valueOf(color)
        }
        val renameItem = menu.findItem(R.id.rename)
        renameItem.isVisible =
            (gridItem.type == ITEM_TYPE_ICON || gridItem.type == ITEM_TYPE_FOLDER)
        if (isOnAllAppsFragment) {
            renameItem.title = getString(R.string.edit_tags)
        }

        menu.findItem(R.id.hide_icon).isVisible =
            gridItem.type == ITEM_TYPE_ICON && isOnAllAppsFragment
        menu.findItem(R.id.resize).isVisible = gridItem.type == ITEM_TYPE_WIDGET
        menu.findItem(R.id.app_info).isVisible = gridItem.type == ITEM_TYPE_ICON
        menu.findItem(R.id.uninstall).isVisible = gridItem.type == ITEM_TYPE_ICON
                && canAppBeUninstalled(gridItem.packageName)
                && gridItem.packageName != packageName
        menu.findItem(R.id.remove).isVisible = !isOnAllAppsFragment

        val launcherApps =
            applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcuts = if (launcherApps.hasShortcutHostPermission()) {
            try {
                val query = LauncherApps.ShortcutQuery().setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                ).setPackage(gridItem.packageName)
                launcherApps.getShortcuts(query, Process.myUserHandle())
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val hasShortcuts = !shortcuts.isNullOrEmpty()
        MenuCompat.setGroupDividerEnabled(menu, hasShortcuts)
        menu.setGroupVisible(R.id.group_shortcuts, hasShortcuts)
        if (hasShortcuts) {
            val iconSize = resources.getDimensionPixelSize(R.dimen.menu_icon_size)
            shortcuts?.forEach { shortcutInfo ->
                val iconDrawable = launcherApps.getShortcutIconDrawable(
                    shortcutInfo, resources.displayMetrics.densityDpi
                )

                menu.add(R.id.group_shortcuts, Menu.NONE, Menu.NONE, shortcutInfo.getLabel())
                    .setIcon(
                        (iconDrawable ?: Color.TRANSPARENT.toDrawable())
                            .toBitmap(width = iconSize, height = iconSize)
                            .toDrawable(resources)
                    )
                    .setOnMenuItemClickListener { _ ->
                        listener.onAnyClick()
                        val id = shortcutInfo.id
                        val packageName = shortcutInfo.`package`
                        val userHandle = Process.myUserHandle()
                        launcherApps.startShortcut(packageName, id, Rect(), null, userHandle)
                        true
                    }
            }
        }

        setOnMenuItemClickListener { item ->
            listener.onAnyClick()
            when (item.itemId) {
                R.id.hide_icon -> listener.hide(gridItem)
                R.id.rename -> listener.rename(gridItem)
                R.id.resize -> listener.resize(gridItem)
                R.id.app_info -> listener.appInfo(gridItem)
                R.id.remove -> listener.remove(gridItem)
                R.id.uninstall -> listener.uninstall(gridItem)
            }
            true
        }

        setOnDismissListener {
            listener.onDismiss()
        }

        listener.beforeShow(menu)

        show()
    }
}
