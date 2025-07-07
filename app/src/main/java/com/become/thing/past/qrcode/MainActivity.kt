package com.become.thing.past.qrcode

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.become.thing.past.qrcode.databinding.ActivityGuideBinding
import com.become.thing.past.qrcode.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        clickListener()
    }

    private fun clickListener() {
        binding.apply {
            aivSetting.setOnClickListener {
                startActivity(Intent(this@MainActivity, NetActivity::class.java))
            }
            tvScan.setOnClickListener {
                startActivity(Intent(this@MainActivity, ScanActivity::class.java))
            }
            tvCreate.setOnClickListener {
                startActivity(Intent(this@MainActivity, CreateActivity::class.java))
            }
            tvFlashlight.setOnClickListener {
                startActivity(Intent(this@MainActivity, FlashActivity::class.java))
            }
        }
    }
}