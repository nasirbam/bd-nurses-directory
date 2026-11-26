package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.NurseRecord
import com.example.util.QrCodeHelper
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun NurseQrCodeDialog(
  nurse: NurseRecord,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val payload = remember(nurse.phone) { QrCodeHelper.buildNurseQrPayload(nurse.phone) }
  val qrBitmap = remember(payload) { QrCodeHelper.generateQrBitmap(payload, 560) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.QrCode2,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = nurse.name,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
    },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = "${nurse.designation} · ${nurse.workplace}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Phone: ${nurse.phone}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
        if (qrBitmap != null) {
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(top = 4.dp),
          ) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "QR code for ${nurse.name}",
              contentScale = ContentScale.Fit,
              modifier = Modifier.size(210.dp).padding(10.dp),
            )
          }
        }
        Text(
          text = "স্ক্যান করলে সরাসরি ডিরেক্টরিতে এই নার্সের বিস্তারিত তথ্য দেখাবে।",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedButton(
            onClick = {
              clipboard.setText(AnnotatedString(payload))
              Toast.makeText(context, "Profile লিংক কপি হয়েছে", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("copy_qr_link_button"),
          ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Copy")
          }
          Button(
            onClick = {
              val uri = qrBitmap?.let { QrCodeHelper.saveShareableQrCardUri(context, nurse, it) }
              val shareIntent =
                if (uri != null) {
                  Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                      Intent.EXTRA_TEXT,
                      "${nurse.name} (${nurse.designation})\n$payload",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  }
                } else {
                  Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                      Intent.EXTRA_TEXT,
                      "${nurse.name} (${nurse.designation})\n$payload",
                    )
                  }
                }
              context.startActivity(Intent.createChooser(shareIntent, "Share Nurse QR Code"))
            },
            modifier = Modifier.weight(1f).height(48.dp).testTag("share_qr_button"),
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Share")
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_qr_dialog_button"),
      ) {
        Text("Close")
      }
    },
  )
}

@Composable
fun NurseDetailDialog(
  nurse: NurseRecord,
  canEdit: Boolean,
  onEdit: () -> Unit,
  onShowQr: () -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Nurse Profile Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_nurse_detail_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "Close details")
          }
        }

        NurseProfileCard(
          nurse = nurse,
          footerContent = {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedButton(
                onClick = onShowQr,
                modifier = Modifier.weight(1f).height(48.dp).testTag("detail_show_qr_button"),
              ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("QR Code")
              }
              if (canEdit) {
                Button(
                  onClick = onEdit,
                  modifier = Modifier.weight(1f).height(48.dp).testTag("detail_edit_button"),
                ) {
                  Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.size(6.dp))
                  Text("Edit")
                }
              }
            }
          },
        )
      }
    }
  }
}

@Composable
fun QrScannerDialog(
  onQrScanned: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var scanStatusMessage by remember { mutableStateOf("") }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
      if (!granted) {
        scanStatusMessage = "Camera অনুমতি পাওয়া যায়নি। চাইলে গ্যালারি থেকে QR ছবি বেছে নিতে পারেন।"
      }
    }

  val galleryQrLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        val decoded = QrCodeHelper.decodeQrFromUri(context, uri)
        if (!decoded.isNullOrBlank()) {
          onQrScanned(decoded)
        } else {
          scanStatusMessage = "এই ছবিতে কোনো বৈধ QR কোড পাওয়া যায়নি।"
        }
      }
    }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Scan Nurse QR Code",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_qr_scanner_button"),
          ) {
            Icon(Icons.Filled.Close, contentDescription = "Close scanner")
          }
        }

        if (hasCameraPermission) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
          ) {
            CameraQrPreview(
              lifecycleOwner = lifecycleOwner,
              onQrDetected = onQrScanned,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier.size(210.dp)
                  .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                  )
            )
          }
          Text(
            text = "ক্যামেরার সামনে নার্সের QR কোডটি ধরুন",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  MaterialTheme.colorScheme.surfaceVariant,
                  RoundedCornerShape(14.dp),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CameraAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "লাইভ QR স্ক্যানের জন্য Camera অনুমতি দিন, অথবা নিচের বাটনে চাপ দিয়ে QR ছবি আপলোড করুন।",
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center,
            )
            Button(
              onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("grant_camera_button"),
            ) {
              Text("Allow Camera")
            }
          }
        }

        if (scanStatusMessage.isNotBlank()) {
          Text(
            text = scanStatusMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
        }

        OutlinedButton(
          onClick = {
            galleryQrLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scan_qr_from_image_button"),
        ) {
          Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
          Spacer(modifier = Modifier.size(8.dp))
          Text("গ্যালারি থেকে QR ছবি স্ক্যান করুন")
        }
      }
    }
  }
}

@Composable
private fun CameraQrPreview(
  lifecycleOwner: androidx.lifecycle.LifecycleOwner,
  onQrDetected: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val previewView = remember { PreviewView(context) }
  val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
  val detectedOnce = remember { AtomicBoolean(false) }

  DisposableEffect(lifecycleOwner, previewView) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val mainExecutor = ContextCompat.getMainExecutor(context)

    val listener = Runnable {
      try {
        val cameraProvider = cameraProviderFuture.get()
        val preview =
          Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }

        val imageAnalysis =
          ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
          try {
            if (!detectedOnce.get()) {
              val yPlane = imageProxy.planes.firstOrNull()
              if (yPlane != null) {
                val buffer = yPlane.buffer
                val rowStride = yPlane.rowStride
                val w = imageProxy.width
                val h = imageProxy.height
                val yData =
                  if (rowStride == w) {
                    ByteArray(buffer.remaining()).also { buffer.get(it) }
                  } else {
                    ByteArray(w * h).also { out ->
                      val rowBytes = ByteArray(rowStride)
                      for (row in 0 until h) {
                        val toRead = minOf(rowStride, buffer.remaining())
                        if (toRead <= 0) break
                        buffer.get(rowBytes, 0, toRead)
                        System.arraycopy(rowBytes, 0, out, row * w, minOf(w, toRead))
                      }
                    }
                  }
                val text = QrCodeHelper.decodeQrFromYuv(yData, w, h)
                if (!text.isNullOrBlank() && detectedOnce.compareAndSet(false, true)) {
                  mainExecutor.execute { onQrDetected(text) }
                }
              }
            }
          } finally {
            imageProxy.close()
          }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
          lifecycleOwner,
          CameraSelector.DEFAULT_BACK_CAMERA,
          preview,
          imageAnalysis,
        )
      } catch (_: Exception) {
        // Camera unavailable on device/emulator
      }
    }

    cameraProviderFuture.addListener(listener, mainExecutor)

    onDispose {
      try {
        if (cameraProviderFuture.isDone) {
          cameraProviderFuture.get().unbindAll()
        }
      } catch (_: Exception) {
      }
      analysisExecutor.shutdown()
    }
  }

  AndroidView(
    factory = { previewView },
    modifier = modifier,
  )
}
