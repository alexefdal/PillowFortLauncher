package org.pillowfort.home.interfaces

import org.pillowfort.home.models.AppWidget

interface WidgetsFragmentListener {
    fun onWidgetLongPressed(appWidget: AppWidget)
}
