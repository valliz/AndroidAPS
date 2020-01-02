package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_PREFERENCE_CHANGES
import info.nightscout.androidaps.database.entities.PreferenceChange
import io.reactivex.Single

@Dao
interface PreferenceChangeDao {

    @Query("""SELECT * FROM $TABLE_PREFERENCE_CHANGES WHERE "key" = :key ORDER BY id DESC LIMIT 1""")
    fun getMostRecentWithKey(key: String): PreferenceChange?

    @Insert
    fun insert(preferenceChange: PreferenceChange)

    @Query("SELECT * FROM $TABLE_PREFERENCE_CHANGES WHERE id >= :id")
    fun getAllStartingFrom(id: Long): Single<List<PreferenceChange>>

}