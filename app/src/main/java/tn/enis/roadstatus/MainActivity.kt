package tn.enis.roadstatus

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import tn.enis.roadstatus.fragments.HomeFragment
import tn.enis.roadstatus.fragments.ProfileFragment
import tn.enis.roadstatus.other.Constants
import tn.enis.roadstatus.other.Utilities


class MainActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks {





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val homeFragment = HomeFragment()
        val profileFragment = ProfileFragment()

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
                    val intent = Intent(this, SamplingActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.navigation_stats -> {
                }
            }
            true
        }





        requestPermissions()
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