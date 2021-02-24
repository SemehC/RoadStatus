package tn.enis.roadstatus.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
        entities = [RoadStatus::class],
        version = 1
)
abstract class RoadStatusDatabase: RoomDatabase() {

    abstract fun getRoadStatusDAO():RoadStatusDAO

}