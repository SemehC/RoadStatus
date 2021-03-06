package tn.enis.roadstatus

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import tn.enis.roadstatus.fragments.HomeFragment
import tn.enis.roadstatus.fragments.ProfileFragment
import tn.enis.roadstatus.fragments.RoadStatusItemFragment
import tn.enis.roadstatus.fragments.SettingsFragment
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Utilities
import java.util.*


class MainActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks {




    private val itemFragment = RoadStatusItemFragment()
    private val homeFragment = HomeFragment()

    private var enteredFragment=false
    private var sureToClose=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val profileFragment = ProfileFragment()
        val settingsFragment = SettingsFragment()

        homeFragment.mainActivity=this
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer,homeFragment).commit()
        }



        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView2)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {

                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainer,homeFragment).commit()
                    }
                }
                R.id.navigation_profile -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainer,profileFragment).commit()
                    }
                }
                R.id.start_scanning ->{
                    val builder = AlertDialog.Builder(this)


                    // Set the alert dialog title
                    builder.setTitle("Sélectionner le mode d'utilisation : ")
                            .setItems(arrayOf("Collecte de donnees","Exploration",)
                            ) { _, which ->
                                var intent: Intent?
                                if(which==0)
                                {
                                    intent = Intent(this, SamplingActivity::class.java)
                                }
                                else
                                {
                                    intent = Intent(this, Exploring::class.java)
                                }
                                startActivity(intent)
                                
                            }
                    // Finally, make the alert dialog using builder
                    val dialog: AlertDialog = builder.create()

                    // Display the alert dialog on app interface
                    dialog.show()

                }
                R.id.navigation_stats -> {
                }
                R.id.navigation_settings -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainer,settingsFragment).commit()
                    }
                }
            }
            true
        }

        requestPermissions()
    }

    fun openRoadStatusItem(id:Int){
        enteredFragment=true
        itemFragment.id=id
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer,itemFragment).commit()
        }
    }


    fun returnHome(){
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer,homeFragment).commit()
        }
    }

    override fun onBackPressed() {
        if(enteredFragment){
            returnHome()
            enteredFragment=false
        }else{
            if(sureToClose){
                super.onBackPressed()
            }else{
                Toast.makeText(this,"Press again to close",Toast.LENGTH_SHORT).show()
                GlobalScope.launch(Dispatchers.Default) {
                    delay(2000)
                    sureToClose=true
                }
            }
        }
    }


    private fun requestPermissions()
    {
        if(Utilities.hasAllPermissions(this))
        {
            return
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            EasyPermissions.requestPermissions(this,"Il faut accepter les permissions pour utiliser cette application",
                Constants.REQUEST_CODE_PERMISSION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO,Manifest.permission.INTERNET)
        }
        else
        {
            EasyPermissions.requestPermissions(this,"Il faut accepter les permissions pour utiliser cette application",
                Constants.REQUEST_CODE_PERMISSION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_BACKGROUND_LOCATION,Manifest.permission.RECORD_AUDIO,Manifest.permission.INTERNET)
        }



    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms))
        {
            AppSettingsDialog.Builder(this).setRationale("Il faut accepter tout les permissions " +
                    "pour utiliser cette application, merci de les accepter manuellement !").build().show()
        }
        else
        {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,this)
    }

}