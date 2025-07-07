package com.become.thing.past.qrcode

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.become.thing.past.qrcode.databinding.ActivityFlashlightBinding

class FlashActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFlashlightBinding
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var isTorchOn = false

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFlashlightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.flashlight)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()

        setupListeners()
        initCamera()
    }

    private fun setupListeners() {
        binding.llOff.setOnClickListener {
            if (checkPermissions()) {
                toggleFlashlight(true)
            }
        }

        binding.llOn.setOnClickListener {
            if (checkPermissions()) {
                toggleFlashlight(false)
            }
        }

        binding.tvFlash.setOnClickListener {
            binding.flFlash.visibility = View.GONE
            binding.llOff.visibility = View.VISIBLE
            binding.llOn.visibility = View.INVISIBLE
            binding.imgFlashlightOn.visibility = View.INVISIBLE
            binding.imgFlashlightOff.visibility = View.VISIBLE

            binding.tvFlash.setBackgroundResource(R.drawable.bg_flash)
            binding.tvFlash.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tvBright.setBackgroundResource(R.drawable.bg_bright)
            binding.tvFlash.setTextColor(ContextCompat.getColor(this, R.color.no_check))
        }

        binding.tvBright.setOnClickListener {
            toggleFlashlight(false)
            binding.flFlash.visibility = View.VISIBLE
            binding.llOff.visibility = View.INVISIBLE
            binding.llOn.visibility = View.INVISIBLE
            binding.imgFlashlightOn.visibility = View.INVISIBLE
            binding.imgFlashlightOff.visibility = View.INVISIBLE

            binding.tvFlash.setBackgroundResource(R.drawable.bg_bright)
            binding.tvFlash.setTextColor(ContextCompat.getColor(this, R.color.no_check))

            binding.tvBright.setBackgroundResource(R.drawable.bg_flash)
            binding.tvFlash.setTextColor(ContextCompat.getColor(this, R.color.white))

        }
        binding.flFlash.setOnClickListener {
            binding.inDialog.dialogFlash.isVisible = true
            setScreenBrightness(1.0f)
        }
        binding.inDialog.dialogFlash.setOnClickListener {

        }
        binding.inDialog.imgBright.setOnClickListener {
            setScreenBrightness(0.1f)
            binding.inDialog.dialogFlash.isVisible = false
        }
        binding.imgBack.setOnClickListener {
            finish()
        }
        onBackPressedDispatcher.addCallback {
            if (binding.inDialog.dialogFlash.isVisible) {
                setScreenBrightness(0.1f)
                binding.inDialog.dialogFlash.isVisible = false
            } else {
                toggleFlashlight(false)
                finish()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                showPermissionRationale()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
            return false
        }
        return true
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Camera permissions are required")
            .setMessage("The application requires access to the flashlight to use the flashlight function")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun initCamera() {
        try {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull {
                cameraManager?.getCameraCharacteristics(it)
                    ?.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize the camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFlashlight(on: Boolean) {
        if (isTorchOn == on) return

        cameraId?.let { id ->
            try {
                cameraManager?.setTorchMode(id, on)
                isTorchOn = on
                updateUIState(on)
                if (on) {
                    binding.imgFlashlightOn.visibility = View.VISIBLE
                    binding.imgFlashlightOff.visibility = View.INVISIBLE
                } else {
                    binding.imgFlashlightOn.visibility = View.INVISIBLE
                    binding.imgFlashlightOff.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to switch flash", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIState(isOn: Boolean) {
        binding.llOff.visibility = if (isOn) View.INVISIBLE else View.VISIBLE
        binding.llOn.visibility = if (isOn) View.VISIBLE else View.INVISIBLE
    }

    private fun setScreenBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleFlashlight(true)
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to use the flash",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保退出时关闭闪光灯
        toggleFlashlight(false)
        cameraManager = null
    }
}
