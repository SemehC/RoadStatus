package tn.enis.roadstatus.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "roads_statuses")
data class RoadStatus(
        val label:String,
        var img:Bitmap,
        var date: Long = 0L,
        var total_time: Long=0L,
        var total_distance:Float,
        var file_name: String?=null

) {
        @PrimaryKey(autoGenerate = true)
        var id:Int = 0

}