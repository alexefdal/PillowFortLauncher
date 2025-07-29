package org.pillowfort.home.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import org.pillowfort.commons.extensions.beVisibleIf
import org.pillowfort.commons.extensions.normalizeString
import org.pillowfort.commons.extensions.viewBinding
import org.pillowfort.commons.helpers.NavigationIcon
import org.pillowfort.commons.helpers.ensureBackgroundThread
import org.pillowfort.commons.interfaces.RefreshRecyclerViewListener
import org.pillowfort.commons.views.MyGridLayoutManager
import org.pillowfort.home.adapters.HiddenIconsAdapter
import org.pillowfort.home.databinding.ActivityHiddenIconsBinding
import org.pillowfort.home.extensions.config
import org.pillowfort.home.extensions.getDrawableForPackageName
import org.pillowfort.home.extensions.hiddenIconsDB
import org.pillowfort.home.models.HiddenIcon

class HiddenIconsActivity : SimpleActivity(), RefreshRecyclerViewListener {
    private val binding by viewBinding(ActivityHiddenIconsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateIcons()

        updateMaterialActivityViews(
            binding.manageHiddenIconsCoordinator,
            binding.manageHiddenIconsList,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.manageHiddenIconsList, binding.manageHiddenIconsToolbar)

        val layoutManager = binding.manageHiddenIconsList.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = config.drawerColumnCount
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.manageHiddenIconsToolbar, NavigationIcon.Arrow)
    }

    private fun updateIcons() {
        ensureBackgroundThread {
            val hiddenIcons = hiddenIconsDB.getHiddenIcons().sortedWith(
                compareBy({
                    it.title.normalizeString().lowercase()
                }, {
                    it.packageName
                })
            ).toMutableList() as ArrayList<HiddenIcon>

            val hiddenIconsEmpty = hiddenIcons.isEmpty()
            runOnUiThread {
                binding.manageHiddenIconsPlaceholder.beVisibleIf(hiddenIconsEmpty)
            }

            if (hiddenIcons.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)

                val list = packageManager.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED)
                for (info in list) {
                    val componentInfo = info.activityInfo.applicationInfo
                    val packageName = componentInfo.packageName
                    val activityName = info.activityInfo.name
                    hiddenIcons.firstOrNull { it.getIconIdentifier() == "$packageName/$activityName" }?.apply {
                        drawable = info.loadIcon(packageManager) ?: getDrawableForPackageName(packageName)
                    }
                }

                hiddenIcons.firstOrNull { it.packageName == applicationContext.packageName }?.apply {
                    drawable = getDrawableForPackageName(packageName)
                }
            }

            val iconsToRemove = hiddenIcons.filter { it.drawable == null }
            if (iconsToRemove.isNotEmpty()) {
                hiddenIconsDB.removeHiddenIcons(iconsToRemove)
                hiddenIcons.removeAll(iconsToRemove)
            }

            runOnUiThread {
                HiddenIconsAdapter(this, hiddenIcons, this, binding.manageHiddenIconsList) {
                }.apply {
                    binding.manageHiddenIconsList.adapter = this
                }
            }
        }
    }

    override fun refreshItems() {
        updateIcons()
    }
}
