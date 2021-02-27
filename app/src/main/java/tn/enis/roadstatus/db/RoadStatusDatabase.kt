package tn.enis.roadstatus.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tn.enis.roadstatus.other.Constants.DATABASE_NAME
import javax.inject.Singleton

@Database(
        entities = [RoadStatus::class],
        version = 1
)
@TypeConverters(Converters::class)
abstract class RoadStatusDatabase: RoomDatabase() {

    @Singleton
    abstract fun getRoadStatusDAO():RoadStatusDAO

    companion object {

       @Volatile private var instance : RoadStatusDatabase? = null
        private val LOCK = Any()

        operator fun invoke(context:Context) = instance ?: synchronized(LOCK){
            instance ?: buildDatabase(context).also {
                instance=it
            }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
                context.applicationContext,
                RoadStatusDatabase::class.java,
                DATABASE_NAME
        ).build()

    }

}