package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ImageHelper {
  fun encodeAvatarFromUri(context: Context, uri: Uri): String? {
    return try {
      val sourceBitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val source = ImageDecoder.createSource(context.contentResolver, uri)
          ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
          }
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

      val side = min(sourceBitmap.width, sourceBitmap.height)
      val xOffset = (sourceBitmap.width - side) / 2
      val yOffset = (sourceBitmap.height - side) / 2
      val square = Bitmap.createBitmap(sourceBitmap, xOffset, yOffset, side, side)
      val scaled = Bitmap.createScaledBitmap(square, 240, 240, true)

      val baos = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
      val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

      if (square !== sourceBitmap) square.recycle()
      if (scaled !== square) scaled.recycle()

      "data:image/jpeg;base64,$b64"
    } catch (_: Exception) {
      null
    }
  }

  fun decodeBase64Avatar(dataUrl: String?): ImageBitmap? {
    if (dataUrl.isNullOrBlank()) return null
    return try {
      val payload = dataUrl.substringAfter("base64,", dataUrl)
      val bytes = Base64.decode(payload, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }
}
