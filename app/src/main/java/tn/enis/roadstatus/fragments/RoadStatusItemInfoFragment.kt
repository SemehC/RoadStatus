package tn.enis.roadstatus.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.fragment_road_status_item_info.*
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler

class RoadStatusItemInfoFragment : Fragment(R.layout.fragment_road_status_item_info) {


    private var labelInput:TextInputLayout?=null
    private var changeBt:Button?=null

    var id:Int?=-1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val changeBt = view?.findViewById<Button>(R.id.change_label_name_bt)
        labelInput = view?.findViewById(R.id.road_status_item_label_layout)
        road_status_item_label_field.addTextChangedListener {
            labelInputChanged()
        }
        changeBt?.setOnClickListener {
            changeLabel()
        }
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
    }






}


