package org.pillowfort.home.interfaces

import androidx.room.*
import org.pillowfort.home.models.HiddenIcon

@Dao
interface HiddenIconsDao {
    @Query("SELECT * FROM hidden_icons")
    fun getHiddenIcons(): List<HiddenIcon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hiddenIcon: HiddenIcon): Long

    @Delete
    fun removeHiddenIcons(icons: List<HiddenIcon>)
}
