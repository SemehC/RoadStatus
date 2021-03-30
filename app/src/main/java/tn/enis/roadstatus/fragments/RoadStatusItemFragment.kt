package tn.enis.roadstatus.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_road_status_item.*
import tn.enis.roadstatus.R
import tn.enis.roadstatus.db.DatabaseHandler


class RoadStatusItemFragment : Fragment(R.layout.fragment_road_status_item){

    var id:Int?=-1
    private var roadStatusItemMapFragment = RoadStatusItemMapFragment()
    private var roadStatusItemInfoFragment=RoadStatusItemInfoFragment()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val road=DatabaseHandler().getItemById(view.context,id!!)


        roadStatusItemMapFragment.id= id!!

        roadStatusItemInfoFragment.id=id
        roadStatusItemInfoFragment.road=road

        childFragmentManager.beginTransaction().apply {
            replace(R.id.road_status_item_fragment_container,roadStatusItemInfoFragment).commit()
        }


        tabbed_road_status_menu.addOnTabSelectedListener(object :TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when(tab?.position){
                    0->{
                        childFragmentManager.beginTransaction().apply {
                            replace(R.id.road_status_item_fragment_container,roadStatusItemInfoFragment).commit()
                        }
                    }
                    1 -> {
                        childFragmentManager.beginTransaction().apply {
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