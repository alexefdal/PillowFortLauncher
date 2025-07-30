package org.pillowfort.home.interfaces

import org.pillowfort.home.models.AppLauncher

interface AllAppsListener {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher)
}
