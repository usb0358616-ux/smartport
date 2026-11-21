package com.example.domain.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeHelper {

    fun generateQrBitmap(payload: String, sizePx: Int = 512): Bitmap? {
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        }.getOrNull()
    }

    /**
     * Builds a signed offline QR payload for InternalMovementPermit verification.
     */
    fun buildPermitQrPayload(permitCode: String, leaderPassport: String): String {
        val sig = PasswordHasher.sha256Hex("PERMIT:$permitCode:$leaderPassport").take(12)
        return "SBPS-PERMIT|$permitCode|$sig"
    }

    fun extractPermitCodeFromQr(rawQr: String): String {
        val trimmed = rawQr.trim()
        return if (trimmed.startsWith("SBPS-PERMIT|")) {
            trimmed.split("|").getOrElse(1) { trimmed }
        } else {
            trimmed
        }
    }
}
