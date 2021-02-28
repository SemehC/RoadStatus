package tn.enis.roadstatus.db

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import tn.enis.roadstatus.other.Constants.DATABASE_NAME

class DatabaseHandler {



    fun saveRoadStatus(roadStatus: RoadStatus, ctx:Context){


        val job = GlobalScope.launch(Dispatchers.IO) {
            RoadStatusDatabase(ctx).getRoadStatusDAO().insertStatus(roadStatus)
        }
        runBlocking {
            job.join()
        }

    }

    fun getAllRoadStatus(ctx: Context):List<RoadStatus>?{
        var roadsStatuses:List<RoadStatus>?=null
        val job = GlobalScope.launch(Dispatchers.IO){
            roadsStatuses = RoadStatusDatabase(ctx).getRoadStatusDAO().getAllStatuses()
        }
        runBlocking {
            job.join()
        }
        return roadsStatuses
    }



    fun getLastInsertedId(ctx:Context):Int{
        var id:Int=-1
        val job = GlobalScope.launch(Dispatchers.IO){
            id = RoadStatusDatabase(ctx).getRoadStatusDAO().getLastInsertedId()

        }
        runBlocking {
            job.join()
        }
        return id
    }

    fun removeItemById(ctx:Context,id:Int){

        val job = GlobalScope.launch(Dispatchers.IO){
            RoadStatusDatabase(ctx).getRoadStatusDAO().removeDataById(id)
        }
        runBlocking {
            job.join()
        }
    }

    fun getItemById(ctx:Context,id:Int):RoadStatus?{
        var res:RoadStatus?=null
        val job = GlobalScope.launch(Dispatchers.IO) {
            res = RoadStatusDatabase(ctx).getRoadStatusDAO().getDataById(id)
        }
        runBlocking {
            job.join()
        }
        return res
    }



}