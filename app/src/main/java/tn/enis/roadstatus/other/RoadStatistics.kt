package tn.enis.roadstatus.other

import com.google.android.gms.maps.model.LatLng

class RoadStatistics(val position:LatLng,val accelerometerData:Array<Double>,val gyroscopeData:Array<Double>,val speed:Double) {
}