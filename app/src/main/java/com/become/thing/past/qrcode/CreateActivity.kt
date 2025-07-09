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
        binding.tvText.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true

            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (this is android.widget.EditText) {
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
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
            val qrCodeBitmap = generateQRCodeBitmap(text, 512, 512)

            if (qrCodeBitmap != null) {
                val intent = Intent(this, CreateResultActivity::class.java)
                intent.putExtra("qr_text", text)

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
