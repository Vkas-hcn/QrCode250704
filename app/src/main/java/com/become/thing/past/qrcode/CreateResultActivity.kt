package com.become.thing.past.qrcode

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.become.thing.past.qrcode.databinding.ActivityResultCreateBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultCreateBinding
    private var qrCodeBitmap: Bitmap? = null
    private var qrText: String = ""
    private val STORAGE_PERMISSION_REQUEST = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResultCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_result)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 获取数据
        qrText = intent.getStringExtra("qr_text") ?: ""
        qrCodeBitmap = (application as? QRCodeApplication)?.getTempQRCodeBitmap()

        initViews()
        clickListener()
    }

    private fun initViews() {
        // 显示二维码图片
        qrCodeBitmap?.let { bitmap ->
            binding.imgCreate.setImageBitmap(bitmap)
        }
    }

    private fun clickListener() {
        binding.apply {
            // 返回按钮
            imgBack.setOnClickListener {
                finish()
            }

            // 分享按钮
            imgShare.setOnClickListener {
                shareQRCodeImage()
            }

            // 下载/保存按钮
            imgDowload.setOnClickListener {
                checkStoragePermissionAndSave()
            }
        }
    }

    private fun shareQRCodeImage() {
        qrCodeBitmap?.let { bitmap ->
            try {
                // 创建临时文件
                val fileName = "qr_code_${System.currentTimeMillis()}.png"
                val cacheDir = File(cacheDir, "shared_images")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val file = File(cacheDir, fileName)
                val fos = FileOutputStream(file)

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()

                // 获取文件URI
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                // 创建分享Intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "QR code content: $qrText")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "Share the QR code")
                startActivity(chooser)

            } catch (e: Exception) {
                Toast.makeText(
                    this@CreateResultActivity,
                    "Sharing failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } ?: run {
            Toast.makeText(this, "There is no QR code picture to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermissionAndSave() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ 使用 Scoped Storage，不需要权限
                saveImageToGallery()
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-9 需要申请存储权限
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    saveImageToGallery()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION_REQUEST
                    )
                }
            }

            else -> {
                // Android 5 及以下
                saveImageToGallery()
            }
        }
    }

    private fun saveImageToGallery() {
        qrCodeBitmap?.let { bitmap ->
            try {
                val fileName = "QRCode_${
                    SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                    ).format(Date())
                }.png"

                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveImageToGalleryQ(bitmap, fileName)
                } else {
                    saveImageToGalleryLegacy(bitmap, fileName)
                }

                if (success) {
                    Toast.makeText(this, "QR code has been saved to photo album", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Saving failed", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this, "Saving failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "There is no QR code picture to save", Toast.LENGTH_SHORT).show()
        }
    }

    // Android 10+ 保存方法
    private fun saveImageToGalleryQ(bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/QRCode"
                )
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                true
            } ?: false

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Android 9 及以下保存方法
    private fun saveImageToGalleryLegacy(bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val qrCodeDir = File(picturesDir, "QRCode")
            if (!qrCodeDir.exists()) {
                qrCodeDir.mkdirs()
            }

            val file = File(qrCodeDir, fileName)
            val fos = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            // 通知系统扫描新文件
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            STORAGE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImageToGallery()
                } else {
                    showStoragePermissionDeniedDialog()
                }
            }
        }
    }

    private fun showStoragePermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Requires storage permissions")
            .setMessage("Saving QR code to the album requires storage permissions. Please enable storage permissions in settings")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除临时bitmap
        (application as? QRCodeApplication)?.clearTempQRCodeBitmap()
    }
}