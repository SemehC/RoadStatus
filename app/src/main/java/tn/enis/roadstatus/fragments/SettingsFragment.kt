package tn.enis.roadstatus.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_settings.*
import tn.enis.roadstatus.BaseApplication
import tn.enis.roadstatus.MainActivity
import tn.enis.roadstatus.R
import java.util.*
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities


class SettingsFragment : Fragment(R.layout.fragment_settings), AdapterView.OnItemSelectedListener {


    private val languages=arrayOf("Choose language","English","French")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageAdapter = ArrayAdapter<String>(view?.context,android.R.layout.simple_spinner_item,languages)

        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        language_spinner.adapter = languageAdapter

        language_spinner.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            when(position){
                1 ->{
                    setLocale("en")
                }
                2->{
                    setLocale("fr")
                }
            }
    }

    private fun setLocale(localeName: String) {
            val locale = Locale(localeName)
            val res = resources
            val dm = res.displayMetrics
            val conf = res.configuration
            conf.locale = locale
            res.updateConfiguration(conf, dm)
            startActivity(Intent(view?.context, MainActivity::class.java))

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

}