package tn.enis.roadstatus

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import tn.enis.roadstatus.other.Settings
import tn.enis.roadstatus.other.Utilities


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val itemFragment = RoadStatusItemFragment()
    private val homeFragment = HomeFragment()

    private var enteredFragment = false
    private var sureToClose = false
    private val settings = Settings()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Load data
        settings.context = this
        settings.loadSettings()
        //fragments to each navigation tab
        val profileFragment = ProfileFragment()
        val settingsFragment = SettingsFragment()

        homeFragment.mainActivity = this
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, homeFragment).commit()
        }


        //when clicked on navigation item , load corresponding fragment ( view)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigationView2)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {

                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainer, homeFragment).commit()
                    }
                }
                R.id.navigation_profile -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainer, profileFragment).commit()
                    }
                }
                R.id.start_scanning -> {
                    settings.loadSettings()
                    when (settings.defaultMode) {
                        0 -> {
                            val builder = AlertDialog.Builder(this)
                            var intent1: Intent?

                            // Set the alert dialog title
                            builder.setTitle("Sélectionner le mode d'utilisation : ")
                                .setItems(
                                    arrayOf("Collecte de données", "Exploration")
                                ) { _, which ->
                                    if (which == 0) {
                                        intent1 = Intent(this, SamplingActivity::class.java)
                                        chooseTypes(intent1!!)
                                    } else {
                                        intent1 = Intent(this, Exploring::class.java)
                                        startActivity(intent1)
                                    }
                                }
                            // Finally, make the alert dialog using builder
                            val dialog: AlertDialog = builder.create()

                            // Display the alert dialog on app interface
                            dialog.show()
                        }
                        1 -> {
                            intent = Intent(this, SamplingActivity::class.java)
                            chooseTypes(intent)
                        }
                        2 -> {
                            startActivity(Intent(this, Exploring::class.java))

                        }
                    }


                }
                R.id.navigation_stats -> {
                }
                R.id.navigation_settings -> {
                    supportFragmentManager.beginTransaction().apply {
                        replace(R.id.fragmentContainer, settingsFragment).commit()
                    }
                }
            }
            true
        }

        requestPermissions()
    }

    //Open selected road item that's in the list
    fun openRoadStatusItem(id: Int) {
        enteredFragment = true
        itemFragment.id = id
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, itemFragment).commit()
        }
    }


    private fun returnHome() {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainer, homeFragment).commit()
        }
    }

    //when back button pressed , return to the last fragment , else quit the app
    override fun onBackPressed() {
        if (enteredFragment) {
            returnHome()
            enteredFragment = false
        } else {
            if (sureToClose) {
                super.onBackPressed()
            } else {
                Toast.makeText(this, "Press again to close", Toast.LENGTH_SHORT).show()
                GlobalScope.launch(Dispatchers.Default) {
                    delay(2000)
                    sureToClose = true
                }
            }
        }
    }

    //get the permissions to use the camera , access storage , get device location
    private fun requestPermissions() {
        if (Utilities.hasAllPermissions(this)) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "Il faut accepter les permissions pour utiliser cette application",
                Constants.REQUEST_CODE_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "Il faut accepter les permissions pour utiliser cette application",
                Constants.REQUEST_CODE_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
            )
        }


    }

    private fun chooseTypes(intent: Intent) {
        val builder2 = AlertDialog.Builder(this)

        val types = arrayOf(
            "Earthen",
            "Gravel",
            "Kankar",
            "WBM",
            "Bituminous(normal)",
            "Concrete(highway)"
        )
        builder2.setTitle("Select road type : ").setItems(types)
        { _, which1 ->
            intent.putExtra("road type", types[which1])
            val builder3 = AlertDialog.Builder(this)
            val quality = arrayOf("Good", "Average", "Bad")
            builder3.setTitle("Select road quality")
                .setItems(quality) { _, which2 ->
                    intent.putExtra(
                        "road quality",
                        quality[which2]
                    )
                    startActivity(intent)
                }
            // Finally, make the alert dialog using builder
            val dialog3: AlertDialog = builder3.create()

            // Display the alert dialog on app interface
            dialog3.show()
        }
        // Finally, make the alert dialog using builder
        val dialog2: AlertDialog = builder2.create()

        // Display the alert dialog on app interface
        dialog2.show()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).setRationale(
                "Il faut accepter tout les permissions " +
                        "pour utiliser cette application, merci de les accepter manuellement !"
            ).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}