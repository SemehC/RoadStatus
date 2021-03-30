package tn.enis.roadstatus.fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import tn.enis.roadstatus.R


class ProfileFragment : Fragment(R.layout.fragment_profile), AdapterView.OnItemSelectedListener {
    private val cars=arrayOf("car1", "car2", "car3")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val spin=view.findViewById<Spinner>(R.id.cars_spinner)

        val aa=ArrayAdapter(view.context,android.R.layout.simple_spinner_item,cars)

        aa.setDropDownViewResource(android.R.layout.simple_spinner_item)

        spin.adapter = aa


    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Toast.makeText(view?.context,cars[position],Toast.LENGTH_SHORT).show()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }


}