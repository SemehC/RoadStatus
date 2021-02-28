package tn.enis.roadstatus

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.road_status_item.view.*
import tn.enis.roadstatus.db.DatabaseHandler
import java.io.File

class RoadStatusItemAdapter(val arrayList:ArrayList<RoadStatusItem>, val context: Context):
    RecyclerView.Adapter<RoadStatusItemAdapter.ViewHolder>() {


    class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        fun bindItems(roadStatusItem: RoadStatusItem){
            itemView.road_status_item_label.text = roadStatusItem.title
            itemView.road_status_item_time.text = roadStatusItem.time
            itemView.road_status_item_image.setImageBitmap(roadStatusItem.img)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.road_status_item,parent,false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.road_status_item_image.animation = AnimationUtils.loadAnimation(context,R.anim.fade_transition_animation)
        holder.itemView.road_status_item.animation = AnimationUtils.loadAnimation(context,R.anim.fade_scale_animation)
        holder.bindItems(arrayList[position])
        holder.itemView.setOnClickListener {
            Toast.makeText(context,arrayList[position].id.toString(),Toast.LENGTH_SHORT).show()
        }

        holder.itemView.setOnLongClickListener {
            deleteItem(arrayList[position].id,arrayList[position].folder)
            true
        }

    }

    fun deleteItem(id:Int,folder:String){


        val builder = AlertDialog.Builder(context)

        // Set the alert dialog title
        builder.setTitle("Confirm Deletion")

        // Display a message on alert dialog
        builder.setMessage("Are you sure you want to delete this ?")

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton("YES"){dialog, which ->
            DatabaseHandler().removeItemById(context,id)
            //to fix , [REMOVE ACTUAL FOLDER AND FILES ]
        }


        // Display a negative button on alert dialog
        builder.setNegativeButton("No"){dialog,which ->
            Toast.makeText(context,"Canceled.",Toast.LENGTH_SHORT).show()
        }


        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }




}