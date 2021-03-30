package tn.enis.roadstatus.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.io.ByteArrayOutputStream

@TypeConverters
class Converters {

    @TypeConverter
    fun toBitmap(bytes:ByteArray):Bitmap{
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size)
    }

    @TypeConverter
    fun fromBitmap(bmp:Bitmap):ByteArray{
        val outputStream = ByteArrayOutputStream()
        var b = Bitmap.createScaledBitmap(bmp,bmp.width/2,bmp.height/2,false)
        b.compress(Bitmap.CompressFormat.PNG,100,outputStream)
        return outputStream.toByteArray()
    }

}