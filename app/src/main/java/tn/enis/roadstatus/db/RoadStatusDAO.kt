package tn.enis.roadstatus.db

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

    @Query("SELECT id FROM roads_statuses ORDER BY date DESC")
    suspend fun getLastInsertedId():Int

    @Query("DELETE FROM roads_statuses WHERE id=:data_id")
    suspend fun removeDataById(data_id:Int)

    @Query("SELECT * FROM roads_statuses WHERE id=:data_id")
    suspend fun getDataById(data_id: Int):RoadStatus

    @Query("UPDATE roads_statuses SET label=:data_label WHERE id=:data_id")
    suspend fun updateLabelById(data_label:String,data_id:Int)

}