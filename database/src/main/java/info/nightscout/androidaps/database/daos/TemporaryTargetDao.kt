package info.nightscout.androidaps.database.daos

import androidx.room.Dao
import androidx.room.Query
import info.nightscout.androidaps.database.TABLE_TEMPORARY_TARGETS
import info.nightscout.androidaps.database.entities.TemporaryTarget
import io.reactivex.Flowable
import io.reactivex.Single

@Suppress("FunctionName")
@Dao
internal interface TemporaryTargetDao : TraceableDao<TemporaryTarget> {

    @Query("SELECT * FROM $TABLE_TEMPORARY_TARGETS WHERE id = :id")
    override fun findById(id: Long): TemporaryTarget?

    @Query("DELETE FROM $TABLE_TEMPORARY_TARGETS")
    override fun deleteAllEntries()

    @Query("SELECT * FROM $TABLE_TEMPORARY_TARGETS WHERE timestamp >= :start AND timestamp <= :end AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp ASC")
    fun getTemporaryTargetsInTimeRange(start: Long, end: Long): Flowable<List<TemporaryTarget>>

    @Query("SELECT * FROM $TABLE_TEMPORARY_TARGETS WHERE timestamp <= :timestamp AND (timestamp + duration) > :timestamp AND referenceId IS NULL AND isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getTemporaryTargetActiveAt(timestamp: Long): TemporaryTarget?

    @Query("SELECT * FROM $TABLE_TEMPORARY_TARGETS WHERE id >= :id")
    override fun getAllStartingFrom(id: Long): Single<List<TemporaryTarget>>
}