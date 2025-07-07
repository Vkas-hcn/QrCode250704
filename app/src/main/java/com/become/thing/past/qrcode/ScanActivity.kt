package com.become.thing.past.qrcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.become.thing.past.qrcode.databinding.ActivityScanBinding
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private lateinit var barcodeView: DecoratedBarcodeView
    private var isFlashOn = false
    private val CAMERA_PERMISSION_REQUEST = 1001

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1002
        private const val STORAGE_PERMISSION_REQUEST = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scan)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkCameraPermission()
    }

    private fun initBarcodeView() {
        Log.d("ScanActivity", "Initializing barcode view")

        // 动态创建扫描视图
        barcodeView = DecoratedBarcodeView(this)

        // 配置扫描视图的外观
        barcodeView.apply {
            // 隐藏底部状态文本
            statusView.visibility = android.view.View.GONE
            statusView.text = "" // 清空文本内容

            // 通过反射修改ViewFinder的颜色
            viewFinder?.let { finder ->
                setViewFinderColors(finder)
            }
        }

        // 将扫描视图添加到布局中(在FrameLayout上方)
        val layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.topToTop =
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.bottomToBottom =
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID

        // 添加到根布局的第一层，确保在其他视图下方
        binding.root.addView(barcodeView, 0, layoutParams)

        // 设置扫描回调
        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                Log.d("ScanActivity", "Barcode result: ${result?.text}")
                result?.let {
                    // 停止扫描
                    barcodeView.pause()

                    // 跳转到结果页面
                    val intent = Intent(this@ScanActivity, ScanResultActivity::class.java)
                    intent.putExtra("scan_result", it.text)
                    startActivity(intent)
                    finish()
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {
                Log.d("ScanActivity", "Possible result points: ${resultPoints?.size}")
            }
        })

        // 开始扫描
        barcodeView.resume()

        clickListener()
    }

    private fun setViewFinderColors(viewFinder: com.journeyapps.barcodescanner.ViewfinderView) {
        try {
            val targetColor = android.graphics.Color.parseColor("#8CFDFF")

            // 尝试设置扫描线颜色的多种可能字段名
            val possibleLaserFields = arrayOf("laserColor", "mLaserColor", "LASER_COLOR")
            val possibleFrameFields = arrayOf("frameColor", "mFrameColor", "FRAME_COLOR")

            val viewFinderClass = viewFinder.javaClass

            // 设置扫描线颜色
            for (fieldName in possibleLaserFields) {
                try {
                    val field = viewFinderClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    field.setInt(viewFinder, targetColor)
                    Log.d("ScanActivity", "Successfully set laser color using field: $fieldName")
                    break
                } catch (e: NoSuchFieldException) {
                    // 继续尝试下一个字段名
                }
            }

            // 设置扫描框颜色
            for (fieldName in possibleFrameFields) {
                try {
                    val field = viewFinderClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    field.setInt(viewFinder, targetColor)
                    Log.d("ScanActivity", "Successfully set frame color using field: $fieldName")
                    break
                } catch (e: NoSuchFieldException) {
                    // 继续尝试下一个字段名
                }
            }

            // 强制重绘ViewFinder
            viewFinder.invalidate()

        } catch (e: Exception) {
            Log.w("ScanActivity", "Failed to set ViewFinder colors: ${e.message}")

            // 备用方案：尝试通过Paint对象设置颜色
            setViewFinderColorsByPaint(viewFinder)
        }
    }

    private fun setViewFinderColorsByPaint(viewFinder: com.journeyapps.barcodescanner.ViewfinderView) {
        try {
            val targetColor = android.graphics.Color.parseColor("#8CFDFF")
            val viewFinderClass = viewFinder.javaClass

            // 尝试获取Paint对象并设置颜色
            val possiblePaintFields = arrayOf("paint", "mPaint", "laserPaint", "framePaint")

            for (fieldName in possiblePaintFields) {
                try {
                    val field = viewFinderClass.getDeclaredField(fieldName)
                    field.isAccessible = true
                    val paint = field.get(viewFinder) as? android.graphics.Paint
                    paint?.color = targetColor
                    Log.d("ScanActivity", "Successfully set paint color using field: $fieldName")
                } catch (e: Exception) {
                    // 继续尝试下一个字段
                }
            }

            viewFinder.invalidate()

        } catch (e: Exception) {
            Log.w("ScanActivity", "Failed to set ViewFinder colors by Paint: ${e.message}")
        }
    }

    private fun clickListener() {
        binding.apply {
            // 返回按钮
            imgBack.setOnClickListener {
                onBackPressed()
            }

            // 闪光灯开关
            imgFlash.setOnClickListener {
                toggleFlashlight()
            }
            flChoose.setOnClickListener {
                checkStoragePermission()
            }
        }
    }

    private fun checkStoragePermission() {
        openImagePicker()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
    }

    private fun toggleFlashlight() {
        isFlashOn = !isFlashOn
        if (isFlashOn) {
            barcodeView.setTorchOn()
            binding.imgFlash.setImageResource(R.drawable.icon_on)
        } else {
            barcodeView.setTorchOff()
            binding.imgFlash.setImageResource(R.drawable.icon_off)
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            initBarcodeView()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initBarcodeView()
                } else {
                    showPermissionDeniedDialog()
                }
            }

            STORAGE_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                } else {
                    Toast.makeText(
                        this,
                        "Need storage permission to select images",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                decodeFromImage(uri)
            }
        }
    }

    private fun decodeFromImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            if (bitmap == null) {
                Toast.makeText(this, "Unable to read the picture", Toast.LENGTH_SHORT).show()
                return
            }

            // 将Bitmap转换为像素数组
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            // 创建LuminanceSource
            val rgbLuminanceSource = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(rgbLuminanceSource))

            val reader = MultiFormatReader()
            val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                put(DecodeHintType.TRY_HARDER, true)
            }

            val result = reader.decode(binaryBitmap, hints)
            handleDecodeResult(result.text)
        } catch (e: Exception) {
            Toast.makeText(this, "No QR code found in the picture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDecodeResult(resultText: String) {
        val intent = Intent(this, ScanResultActivity::class.java).apply {
            putExtra("scan_result", resultText)
        }
        startActivity(intent)
        finish()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera permissions are required")
            .setMessage("Scan the QR code to use the camera permissions, please enable the camera permissions in the settings")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeView.isInitialized) {
            Log.d("ScanActivity", "Resuming barcode view")
            barcodeView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeView.isInitialized) {
            Log.d("ScanActivity", "Pausing barcode view")
            barcodeView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ScanActivity", "Destroying activity")
    }
}