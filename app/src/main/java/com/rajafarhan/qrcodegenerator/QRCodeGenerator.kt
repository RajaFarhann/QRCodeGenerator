package com.rajafarhan.qrcodegenerator

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// Fungsi untuk generate QR Code dengan custom warna
fun generateCustomQRCode(
    text: String,
    size: Int = 512,
    foregroundColor: Int = 0xFF000000.toInt(),
    backgroundColor: Int = 0xFFFFFFFF.toInt()
): Bitmap? {
    return try {
        // Mengatur hint untuk error correction level H
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
        }
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) foregroundColor else backgroundColor)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun resizeBitmapWithAspectRatio(bitmap: Bitmap, maxSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val aspectRatio = width.toFloat() / height.toFloat()

    val newWidth: Int
    val newHeight: Int

    if (aspectRatio > 1) {
        // Lebar lebih besar dari tinggi (landscape)
        newWidth = maxSize
        newHeight = (maxSize / aspectRatio).toInt()
    } else {
        // Tinggi lebih besar atau sama dengan lebar (portrait atau square)
        newHeight = maxSize
        newWidth = (maxSize * aspectRatio).toInt()
    }

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

fun getRoundedBitmapUsingClip(bitmap: Bitmap, cornerRadius: Float): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Membuat path dengan sudut membulat
    val path = Path().apply {
        addRoundRect(
            RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            cornerRadius,
            cornerRadius,
            Path.Direction.CW
        )
    }

    // Memotong canvas agar hanya area path yang terlihat
    canvas.clipPath(path)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    return output
}

// Fungsi untuk menambahkan logo di tengah QR Code
fun addRoundedLogoToQRCode(qrBitmap: Bitmap, logoBitmap: Bitmap, logoSizeRatio: Float = 0.30f): Bitmap? {
    val qrSize = qrBitmap.width
    val maxLogoSize = (qrSize * logoSizeRatio).toInt()

    // Resize logo tanpa merusak aspek rasio
    val scaledLogo = resizeBitmapWithAspectRatio(logoBitmap, maxLogoSize)

    // Membulatkan sudut logo
    val roundedLogo = getRoundedBitmapUsingClip(scaledLogo, cornerRadius = (scaledLogo.width / 15).toFloat())

    val combinedBitmap = qrBitmap.config?.let { Bitmap.createBitmap(qrSize, qrSize, it) }
    val canvas = combinedBitmap?.let { Canvas(it) }

    // Gambar QR Code ke dalam canvas
    if (canvas != null) {
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)
    }

    // Hitung posisi tengah QR Code untuk logo
    val centerX = (qrSize - roundedLogo.width) / 2f
    val centerY = (qrSize - roundedLogo.height) / 2f

    // Tambahkan logo yang sudah berbentuk rounded ke QR Code
    if (canvas != null) {
        canvas.drawBitmap(roundedLogo, centerX, centerY, null)
    }

    return combinedBitmap
}

// Fungsi untuk menyimpan QR Code dan mengembalikan URI gambar
fun saveQRCodeToGalleryWithUri(
    context: Context,
    bitmap: Bitmap,
    fileName: String = "QRCode"): Uri? {
    val fileNameWithExtension = "$fileName.png"
    return try {
        var savedUri: Uri? = null
        val fos: OutputStream?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileNameWithExtension)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRCodeGenerator")
            }
            savedUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = savedUri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "QRCodeGenerator"
            )
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileNameWithExtension)
            fos = FileOutputStream(file)
            savedUri = Uri.fromFile(file)
        }
        fos?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
        Toast.makeText(context, "QR Code berhasil disimpan", Toast.LENGTH_SHORT).show()
        savedUri
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Gagal menyimpan QR Code", Toast.LENGTH_SHORT).show()
        null
    }
}

// Fungsi untuk membuka gambar yang telah disimpan menggunakan intent
fun openImage(context: Context, imageUri: Uri) {
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        setDataAndType(imageUri, "image/*")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}