package org.fossify.home.dialogs

import android.app.Activity
import android.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.home.databinding.DialogRenameItemBinding
import org.fossify.home.extensions.homeScreenGridItemsDB
import org.fossify.home.models.HomeScreenGridItem
import org.fossify.home.helpers.TagStorage

class RenameItemDialog(val activity: Activity, val item: HomeScreenGridItem, val callback: () -> Unit) {

    init {
        val binding = DialogRenameItemBinding.inflate(activity.layoutInflater)
        val view = binding.root
        binding.renameItemEdittext.setText(item.title)
        val existingTags = TagStorage.getTags(activity, item.packageName)
        binding.editTags.setText(existingTags.joinToString(", "))

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, org.fossify.commons.R.string.rename) { alertDialog ->
                    alertDialog.showKeyboard(binding.renameItemEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newTitle = binding.renameItemEdittext.value
                        val tags = binding.editTags.text.toString()
                            .split(",")
                            .map { it.trim() }

                        if (item.id != null && newTitle.isEmpty()) {
                            activity.toast(org.fossify.commons.R.string.value_cannot_be_empty)
                            return@setOnClickListener
                        }

                        ensureBackgroundThread {
                            if (item.id != null) {
                                val result = activity.homeScreenGridItemsDB.updateItemTitle(newTitle, item.id!!)
                                if (result != 1) {
                                    activity.runOnUiThread {
                                        activity.toast(org.fossify.commons.R.string.unknown_error_occurred)
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
