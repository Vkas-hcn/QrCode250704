package com.become.thing.past.qrcode

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.become.thing.past.qrcode.databinding.ActivityCreateBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*

class CreateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        clickListener()
    }

    private fun initViews() {
        // 如果XML中使用的是TextView，需要在运行时替换为EditText
        // 或者直接在XML中将tv_text改为EditText

        // 这里假设XML已经使用EditText，如果是TextView需要进行以下设置：
        binding.tvText.apply {
            // 如果是AppCompatTextView并支持输入
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            // 设置输入类型和其他属性
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            // 添加文本变化监听器（如果支持）
            if (this is android.widget.EditText) {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        // 可以在这里实时预览二维码，但为了性能考虑，我们在点击创建时生成
                    }
                })
            }
        }
    }

    private fun clickListener() {
        binding.apply {
            // 返回按钮
            imgBack.setOnClickListener {
                finish()
            }

            // 创建二维码按钮
            llCreate.setOnClickListener {
                createQRCode()
            }

            // 点击文本区域获取焦点
            tvText.setOnClickListener {
                tvText.requestFocus()
            }
        }
    }

    private fun createQRCode() {
        val text = binding.tvText.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter the content to generate the QR code", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 生成二维码
            val qrCodeBitmap = generateQRCodeBitmap(text, 512, 512)

            if (qrCodeBitmap != null) {
                // 跳转到结果页面
                val intent = Intent(this, CreateResultActivity::class.java)
                intent.putExtra("qr_text", text)

                // 将bitmap保存到临时文件或通过Application传递
                // 这里我们通过Application类传递bitmap
                (application as? QRCodeApplication)?.setTempQRCodeBitmap(qrCodeBitmap)

                startActivity(intent)
            } else {
                Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to generate QR code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQRCodeBitmap(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 2

            val bitMatrix: BitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}

// 简单的Application类用于传递Bitmap
