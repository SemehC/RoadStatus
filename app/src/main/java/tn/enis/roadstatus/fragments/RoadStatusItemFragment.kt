package tn.enis.roadstatus.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.ArrayMap
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.collection.arrayMapOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.android.material.tabs.TabItem
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_road_status_item.*
import org.json.JSONObject
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler
import tn.enis.roadstatus.db.RoadStatus
import java.io.File


class RoadStatusItemFragment : Fragment(R.layout.fragment_road_status_item){

    var id:Int?=-1
    var roadStatusItemMapFragment = RoadStatusItemMapFragment()
    var roadStatusItemInfoFragment=RoadStatusItemInfoFragment()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roadStatusItemMapFragment.id=id
        roadStatusItemInfoFragment.id=id

        childFragmentManager.beginTransaction()?.apply {
            replace(R.id.road_status_item_fragment_container,roadStatusItemInfoFragment).commit()
        }


        tabbed_road_status_menu.addOnTabSelectedListener(object :TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position){
                    0->{
                        childFragmentManager.beginTransaction()?.apply {
                            replace(R.id.road_status_item_fragment_container,roadStatusItemInfoFragment).commit()
                        }
                    }
                    1 -> {
                        childFragmentManager.beginTransaction()?.apply {
                            replace(R.id.road_status_item_fragment_container,roadStatusItemMapFragment).commit()
                        }
                    }
                }

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })


    }







}