package tn.enis.roadstatus.other

import android.content.Context
import tn.enis.roadstatus.other.Constants.SHARED_PREFS_NAME

class Settings {
    var language:String="en"
    var defaultMode:Int=0
    var units:Int=0
    var distanceBetweenPoints:Int=30
    var context:Context?=null

    fun saveSettings(){
        val sharePrefs = context?.getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE)
        with(sharePrefs?.edit()){
            this?.putInt("units",units)
            this?.putString("language",language)
            this?.putInt("defaultMode",defaultMode)
            this?.putInt("distanceBetweenPoints",distanceBetweenPoints)
            this?.commit()
        }
    }

    fun loadSettings(){
        val sharePrefs = context?.getSharedPreferences(SHARED_PREFS_NAME,Context.MODE_PRIVATE)
        if(sharePrefs!=null){
            language = sharePrefs.getString("language","en").toString()
            defaultMode = sharePrefs.getInt("defaultMode",0)
            units = sharePrefs.getInt("units",0)
            distanceBetweenPoints = sharePrefs.getInt("distanceBetweenPoints",30)
        }
    }

}