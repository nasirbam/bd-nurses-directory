package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.DirectoryConstants
import com.example.data.NurseRecord
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QrCodeHelper {
  private const val BASE_WEB_URL = "https://nasirbam.github.io/bd-nurses-directory/"
  private val bdPhoneExtractRegex = Regex("(01[3-9]\\d{8})")

  fun buildNurseQrPayload(phone: String): String {
    val clean = phone.trim()
    return "$BASE_WEB_URL?nurse=${Uri.encode(clean)}"
  }

  fun extractNursePhone(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isBlank()) return null

    if (DirectoryConstants.isValidBdPhone(text)) {
      return text
    }

    try {
      val uri = Uri.parse(text)
      val byNurseParam = uri.getQueryParameter("nurse")?.trim()
      if (!byNurseParam.isNullOrBlank()) return byNurseParam
      val byPhoneParam = uri.getQueryParameter("phone")?.trim()
      if (!byPhoneParam.isNullOrBlank()) return byPhoneParam
      val lastSeg = uri.lastPathSegment?.trim()
      if (!lastSeg.isNullOrBlank() && DirectoryConstants.isValidBdPhone(lastSeg)) {
        return lastSeg
      }
    } catch (_: Exception) {
      // Fallback to regex below
    }

    return bdPhoneExtractRegex.find(text)?.groupValues?.getOrNull(1)
  }

  fun generateQrBitmap(content: String, sizePx: Int = 560): Bitmap? {
    if (content.isBlank()) return null
    return try {
      val hints =
        mapOf(
          EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
          EncodeHintType.MARGIN to 2,
          EncodeHintType.CHARACTER_SET to "UTF-8",
        )
      val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      val width = matrix.width
      val height = matrix.height
      val pixels = IntArray(width * height)
      for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
          pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
      }
      Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    } catch (_: Exception) {
      null
    }
  }

  fun decodeQrFromYuv(yuvBytes: ByteArray, width: Int, height: Int): String? {
    return try {
      val source =
        PlanarYUVLuminanceSource(
          yuvBytes,
          width,
          height,
          0,
          0,
          width,
          height,
          false,
        )
      val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
      val hints = mapOf(DecodeHintType.TRY_HARDER to true)
      QRCodeReader().decode(binaryBitmap, hints)?.text
    } catch (_: Exception) {
      null
    }
  }

  fun decodeQrFromUri(context: Context, uri: Uri): String? {
    return try {
      val rawBitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val source = ImageDecoder.createSource(context.contentResolver, uri)
          ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDim = maxOf(info.size.width, info.size.height)
            if (maxDim > 1200) {
              val scale = 1200f / maxDim
              decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
              )
            }
          }
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

      val w = rawBitmap.width
      val h = rawBitmap.height
      val intArray = IntArray(w * h)
      rawBitmap.getPixels(intArray, 0, w, 0, 0, w, h)
      val source = RGBLuminanceSource(w, h, intArray)
      val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
      val hints = mapOf(DecodeHintType.TRY_HARDER to true)
      QRCodeReader().decode(binaryBitmap, hints)?.text
    } catch (_: Exception) {
      null
    }
  }

  fun saveShareableQrCardUri(context: Context, nurse: NurseRecord, qrBitmap: Bitmap): Uri? {
    return try {
      val cardW = 720
      val cardH = 880
      val outBitmap = Bitmap.createBitmap(cardW, cardH, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(outBitmap)
      canvas.drawColor(Color.WHITE)

      val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F766E") }
      canvas.drawRect(0f, 0f, cardW.toFloat(), 110f, headerPaint)

      val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.WHITE
          textSize = 36f
          textAlign = Paint.Align.CENTER
          isFakeBoldText = true
        }
      canvas.drawText("BD Nurses Directory", cardW / 2f, 68f, titlePaint)

      val qrSize = 460
      val qrLeft = (cardW - qrSize) / 2
      val qrTop = 145
      canvas.drawBitmap(
        qrBitmap,
        null,
        Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
        null,
      )

      val namePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#132A25")
          textSize = 32f
          textAlign = Paint.Align.CENTER
          isFakeBoldText = true
        }
      val subPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = Color.parseColor("#4E6B64")
          textSize = 24f
          textAlign = Paint.Align.CENTER
        }
      canvas.drawText(nurse.name.take(36), cardW / 2f, 665f, namePaint)
      canvas.drawText(nurse.designation.take(42), cardW / 2f, 708f, subPaint)
      canvas.drawText(nurse.workplace.take(46), cardW / 2f, 746f, subPaint)
      canvas.drawText("Phone: ${nurse.phone}", cardW / 2f, 792f, namePaint)
      canvas.drawText("Scan QR to open profile in directory", cardW / 2f, 842f, subPaint)

      val dir = File(context.cacheDir, "qr_cards").apply { mkdirs() }
      val safePhone = nurse.phone.replace(Regex("[^A-Za-z0-9_-]"), "_")
      val file = File(dir, "nurse_$safePhone.png")
      FileOutputStream(file).use { fos ->
        outBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
      }
      outBitmap.recycle()

      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
      null
    }
  }
}
