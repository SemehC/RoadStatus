package tn.enis.roadstatus.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_road_status_item_info.*
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.round

class RoadStatusItemInfoFragment : Fragment(R.layout.fragment_road_status_item_info) {


    private var labelInput:TextInputLayout?=null
    private var changeBt:Button?=null

    var road:RoadStatus?=null
    var id:Int?=-1

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val changeBt = view.findViewById<Button>(R.id.change_label_name_bt)

        labelInput = view.findViewById(R.id.road_status_item_label_layout)
        road_status_item_label_field.addTextChangedListener {
            labelInputChanged()
        }
        changeBt?.setOnClickListener {
            changeLabel()
            road = DatabaseHandler().getItemById(view.context,id!!)
            updateTitle()
        }

        road_status_item_title.text = road?.label
        road_status_item_label_field.setText(road?.label)
        val date = Date(road?.date!!)
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
        road_status_item_time.text = format.format(date).toString()
        road_status_item_distance.text = round(road?.total_distance!!).toString()+" meteres"

        val s = String.format("%d min, %d sec",
            TimeUnit.MILLISECONDS.toMinutes(road?.total_time!!),
            TimeUnit.MILLISECONDS.toSeconds(road?.total_time!!) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(road?.total_time!!))
        )

        road_status_item_total_time.text=s

    }
    private fun labelInputChanged(){
        println("Text : "+labelInput?.editText?.text)
        if(labelInput?.editText?.text?.isBlank()!!){
            labelInput?.error="Cannot be empty"
            changeBt?.isEnabled=false
        }else{
            labelInput?.error=null
            changeBt?.isEnabled=true
        }
    }

    private fun changeLabel(){
        val label = labelInput?.editText?.text.toString()
        DatabaseHandler().changeLabelName(view?.context!!,label,id!!)
        Toast.makeText(view?.context,"Updated title",Toast.LENGTH_SHORT).show()
    }

    private fun updateTitle(){
        road_status_item_title.text = road?.label
    }






}


