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
import tn.enis.roadstatus.fragments.HomeFragment
import tn.enis.roadstatus.other.RoadStatusItem
import java.io.File

class RoadStatusItemAdapter(private val arrayList:ArrayList<RoadStatusItem>, val context: Context, private val mainActivity: MainActivity, private val homeFragment: HomeFragment):
    RecyclerView.Adapter<RoadStatusItemAdapter.ViewHolder>() {

    class ViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        fun bindItems(roadStatusItem: RoadStatusItem){
            itemView.road_status_item_label.text = roadStatusItem.title
            itemView.road_status_item_time.text = roadStatusItem.time
            itemView.road_status_item_image.setImageBitmap(roadStatusItem.img)
        }

    }
    //function that adds the arraylist's items, defined in the constructor, in the view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.road_status_item,parent,false)
        return ViewHolder(v)
    }

    //function that configures how the items are displayed
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.itemView.road_status_item_image.animation = AnimationUtils.loadAnimation(context,R.anim.fade_transition_animation)
        holder.itemView.road_status_item.animation = AnimationUtils.loadAnimation(context,R.anim.fade_scale_animation)
        holder.bindItems(arrayList[position])
        holder.itemView.setOnClickListener {
            itemClicked(arrayList[position].id)
        }
        //when long clicked on an item , shows a dialog that asks if u want to delete it
        holder.itemView.setOnLongClickListener {
            deleteItem(arrayList[position].id,arrayList[position].folder)
            true
        }

    }

    //deletes an item from the list
    private fun deleteItem(id:Int,folder:String){


        val builder = AlertDialog.Builder(context)

        // Set the alert dialog title
        builder.setTitle("Confirm Deletion")

        // Display a message on alert dialog
        builder.setMessage("Are you sure you want to delete this ?")

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton("YES"){ _, _ ->
            DatabaseHandler().removeItemById(context,id)
            homeFragment.refresh()
            val dir =  context.getExternalFilesDir(null)?.absolutePath
            val appFolder = File(dir, "PFA")
            val filesFolder = File(appFolder.absolutePath, folder)
            filesFolder.deleteRecursively()
            filesFolder.delete()
        }


        // Display a negative button on alert dialog
        builder.setNegativeButton("No"){ _, _ ->
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

    //when an item is clicked , load another fragment and show its info
    private fun itemClicked(id:Int){
        mainActivity.openRoadStatusItem(id)
    }


}