package com.become.thing.past.qrcode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.become.thing.past.qrcode.databinding.ActivityNetBinding

class NetActivity : AppCompatActivity() {
    private val binding by lazy { ActivityNetBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setting)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        clickFun()
    }

    fun clickFun(){
        binding.imgBack.setOnClickListener {
            finish()
        }
        binding.atvPp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/beathome/home"))
            startActivity(intent)
        }
        binding.atvShare.setOnClickListener {
            //分享Google商店链接
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=${this.packageName}")
            startActivity(Intent.createChooser(intent, "share"))

        }
    }
}