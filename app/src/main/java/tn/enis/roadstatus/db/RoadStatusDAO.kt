package tn.enis.roadstatus.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RoadStatusDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(roadStatus: RoadStatus)

    @Delete
    suspend fun deleteStatus(roadStatus: RoadStatus)

    @Query("SELECT * FROM roads_statuses")
    fun getAllStatuses():LiveData<List<RoadStatus>>

}