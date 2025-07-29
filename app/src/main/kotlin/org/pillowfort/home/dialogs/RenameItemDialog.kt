package org.pillowfort.home.dialogs

import android.app.Activity
import android.app.AlertDialog
import org.pillowfort.commons.extensions.*
import org.pillowfort.commons.helpers.ensureBackgroundThread
import org.pillowfort.home.databinding.DialogRenameItemBinding
import org.pillowfort.home.extensions.homeScreenGridItemsDB
import org.pillowfort.home.models.HomeScreenGridItem
import org.pillowfort.home.helpers.TagStorage

class RenameItemDialog(val activity: Activity, val item: HomeScreenGridItem, val callback: () -> Unit) {

    init {
        val binding = DialogRenameItemBinding.inflate(activity.layoutInflater)
        val view = binding.root
        binding.renameItemEdittext.setText(item.title)
        val existingTags = TagStorage.getTags(activity, item.packageName)
        binding.editTags.setText(existingTags.joinToString(", "))

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.pillowfort.commons.R.string.ok, null)
            .setNegativeButton(org.pillowfort.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, org.pillowfort.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameItemEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.renameItemEdittext.value
                        val tags = binding.editTags.text.toString()
                            .split(",")
                            .map { it.trim() }

                        if (item.id != null && newTitle.isEmpty()) {
                            activity.toast(org.pillowfort.commons.R.string.value_cannot_be_empty)
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            if (item.id != null) {
                                val result = activity.homeScreenGridItemsDB.updateItemTitle(newTitle, item.id!!)
                                if (result != 1) {
                                    activity.runOnUiThread {
                                        activity.toast(org.pillowfort.commons.R.string.unknown_error_occurred)
                                    }
                                    return@ensureBackgroundThread
                                }
                            }

                            TagStorage.saveTags(activity, item.packageName, tags)
                            activity.runOnUiThread {
                                callback()
                                alertDialog.dismiss()
                            }
                        }
                    }
                }
            }
    }
}
