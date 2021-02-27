package tn.enis.roadstatus.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class Converters {


    fun toBitmap(bytes:String):Bitmap{
        val b=bytes.toByteArray()
        return BitmapFactory.decodeByteArray(b,0,b.size)
    }

    fun fromBitmap(bmp:Bitmap):String{
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG,100,outputStream)
        return outputStream.toByteArray().toString()
    }

}