package tn.enis.roadstatus

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bt = findViewById<Button>(R.id.start_scan_bt)
        bt.setOnClickListener {
            val intent = Intent(this,SamplingActivity::class.java)
            startActivity(intent)
        }
    }
}