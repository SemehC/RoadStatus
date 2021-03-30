package tn.enis.roadstatus.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.fragment_settings.*
import tn.enis.roadstatus.MainActivity
import tn.enis.roadstatus.R
import tn.enis.roadstatus.other.Settings
import java.util.*


class SettingsFragment : Fragment(R.layout.fragment_settings), AdapterView.OnItemSelectedListener,SeekBar.OnSeekBarChangeListener {


    private val languages=arrayOf("Choose language","English","French")
    private val settings=Settings()
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageAdapter = ArrayAdapter<String>(view.context,android.R.layout.simple_spinner_item,languages)

        settings.context = view.context

        settings.loadSettings()
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        language_spinner.adapter = languageAdapter

        language_spinner.onItemSelectedListener = this

        distanceSeekBar.progress=settings.distanceBetweenPoints
        distanceTextView.text = "${(distanceSeekBar.progress.toFloat()/10)} M"
        
        val unitsGroup = view.findViewById<ChipGroup>(R.id.units_chip_group)
        val modegroup = view.findViewById<ChipGroup>(R.id.mode_select_chips_group)

        distanceSeekBar.setOnSeekBarChangeListener(this)

        when(settings.units){
            0->{
                unitsGroup.check(R.id.units_select_chip_1)
            }
            1->{
                unitsGroup.check(R.id.units_select_chip_2)
            }
        }

        when(settings.defaultMode){
            0->{
                modegroup.check(R.id.mode_select_chip_1)
            }
            1->{
                modegroup.check(R.id.mode_select_chip_2)
            }
            2->{
                modegroup.check(R.id.mode_select_chip_3)
            }
        }

        units_chip_group.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.units_select_chip_1->{
                    Toast.makeText(view.context,"Checked 1",Toast.LENGTH_SHORT).show()
                    settings.units=0

                }
                R.id.units_select_chip_2->{
                    Toast.makeText(view.context,"Checked 2",Toast.LENGTH_SHORT).show()
                    settings.units=1
                }

            }
            settings.saveSettings()
        }

        mode_select_chips_group.setOnCheckedChangeListener{_,checkedId->
            when(checkedId){
                R.id.mode_select_chip_1->{
                    settings.defaultMode=0
                }
                R.id.mode_select_chip_2->{
                    settings.defaultMode=1
                }
                R.id.mode_select_chip_3->{
                    settings.defaultMode=2
                }
            }
            settings.saveSettings()
        }


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
        if(settings.language==localeName){
            Toast.makeText(view?.context,"Language already selected",Toast.LENGTH_SHORT).show()
        }else{
            settings.language=localeName
            settings.saveSettings()
            val locale = Locale(localeName)
            val res = resources
            val dm = res.displayMetrics
            val conf = res.configuration
            conf.locale = locale
            res.updateConfiguration(conf, dm)
            startActivity(Intent(view?.context, MainActivity::class.java))
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    @SuppressLint("SetTextI18n")
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        distanceTextView.text = "${(distanceSeekBar.progress.toFloat()/10)} M"
        settings.distanceBetweenPoints = distanceSeekBar.progress
        settings.saveSettings()

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

}