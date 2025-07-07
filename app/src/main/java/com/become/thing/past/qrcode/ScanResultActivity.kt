package com.become.thing.past.qrcode

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.become.thing.past.qrcode.databinding.ActivityResultScanBinding

class ScanResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultScanBinding
    private var scanResult: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResultScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan_result_con)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 获取扫描结果
        scanResult = intent.getStringExtra("scan_result") ?: ""

        initViews()
        clickListener()
    }

    private fun initViews() {
        // 显示扫描结果
        binding.tvResult.text = scanResult

        // 根据内容类型更新URL标签
        when {
            scanResult.startsWith("http://") || scanResult.startsWith("https://") -> {
                // 是URL，保持原来的"URL"标签
            }
            scanResult.contains("@") && scanResult.contains(".") -> {
                // 可能是邮箱
                binding.flUrl.findViewById<android.widget.TextView>(android.R.id.text1)?.text = "EMAIL"
            }
            scanResult.startsWith("tel:") || isPhoneNumber(scanResult) -> {
                // 是电话号码
                binding.flUrl.findViewById<android.widget.TextView>(android.R.id.text1)?.text = "PHONE"
            }
            else -> {
                // 其他类型的文本
                binding.flUrl.findViewById<android.widget.TextView>(android.R.id.text1)?.text = "TEXT"
            }
        }
    }

    private fun isPhoneNumber(text: String): Boolean {
        return text.replace(Regex("[\\s\\-\\(\\)\\+]"), "").matches(Regex("\\d{10,}"))
    }

    private fun clickListener() {
        binding.apply {
            // 返回按钮
            imgBack.setOnClickListener {
                finish()
            }

            // 分享按钮
            imgShare.setOnClickListener {
                shareText(scanResult)
            }

            // 复制按钮
            imgCopy.setOnClickListener {
                copyToClipboard(scanResult)
            }

            // 搜索按钮
            imgSearch.setOnClickListener {
                searchInBrowser(scanResult)
            }
        }
    }

    private fun shareText(text: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooser = Intent.createChooser(shareIntent, "分享内容")
        if (shareIntent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            Toast.makeText(this, "没有找到可用的分享应用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("QR scan results", text)
        clipboardManager.setPrimaryClip(clipData)

        Toast.makeText(this, "Copyed to clipboard", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun searchInBrowser(text: String) {
        try {
            val searchIntent = when {
                text.startsWith("http://") || text.startsWith("https://") -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse(text))
                }
                text.contains("@") && text.contains(".") -> {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$text"))
                }
                text.startsWith("tel:") -> {
                    Intent(Intent.ACTION_DIAL, Uri.parse(text))
                }
                isPhoneNumber(text) -> {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$text"))
                }
                else -> {
                    val searchUrl = "https://www.google.com/search?q=${Uri.encode(text)}"
                    Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                }
            }

            startActivity(searchIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Operation failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}