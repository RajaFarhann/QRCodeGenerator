package com.rajafarhan.qrcodegenerator

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun CustomQRCodeScreen() {
    val context = LocalContext.current
    var text by remember { mutableStateOf("Hello, Custom QR!") }
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Pastikan kamu memiliki logo yang valid di drawable.
    val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_logo_github)

    // Generate QR Code dengan custom warna dan tambahkan logo
    LaunchedEffect(text) {
        val baseQR = generateCustomQRCode(
            text,
            // Ini warna QR Codenya.
            foregroundColor = 0xFF000000.toInt(),
            // Ini warna backgroundnya.
            backgroundColor = 0xFFFFFFFF.toInt()
        )
        qrBitmap = baseQR?.let { addRoundedLogoToQRCode(it, logoBitmap) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Tampilkan QR Code jika berhasil di-generate
        qrBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Custom QR Code",
                modifier = Modifier.size(300.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Simpan dan dapatkan URI gambar
                val savedUri = saveQRCodeToGalleryWithUri(context, bitmap, "$text")
                // Jika penyimpanan berhasil, buka gambar secara otomatis
                savedUri?.let { uri ->
                    openImage(context, uri)
                }
            }) {
                Text(text = "Download & Buka QR Code")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TextField selalu ditampilkan
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}