package com.become.thing.past.qrcode

import android.graphics.Bitmap

class QRCodeApplication : android.app.Application() {
    private var tempQRCodeBitmap: Bitmap? = null

    fun setTempQRCodeBitmap(bitmap: Bitmap) {
        tempQRCodeBitmap = bitmap
    }

    fun getTempQRCodeBitmap(): Bitmap? {
        return tempQRCodeBitmap
    }

    fun clearTempQRCodeBitmap() {
        tempQRCodeBitmap = null
    }
}