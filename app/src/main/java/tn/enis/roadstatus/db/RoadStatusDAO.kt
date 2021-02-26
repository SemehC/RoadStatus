package tn.enis.roadstatus.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RoadStatusDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(roadStatus: RoadStatus)

    @Delete
    fun deleteStatus(roadStatus: RoadStatus)


    @Query("DELETE FROM roads_statuses")
    suspend fun removeAllData()

    @Query("SELECT * FROM roads_statuses")
    suspend fun getAllStatuses():List<RoadStatus>

}