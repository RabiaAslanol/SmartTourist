package com.example.smarttourist

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast

@Composable
fun TahminEkrani() {
    val context = LocalContext.current
    val classifier = remember { ImageClassifierHelper(context) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var prediction by remember { mutableStateOf<String>("Henüz tahmin yapılmadı") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Fotoğraf izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        uri?.let {
            val resultBitmap = try {
                if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            bitmap = resultBitmap
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("📷 Fotoğrafla Yer Türü Tahmini", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Galeriden Fotoğraf Seç")
        }

        Spacer(modifier = Modifier.height(16.dp))

        bitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(250.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                bitmap?.let {
                    try {
                        prediction = classifier.classify(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        prediction = "Tahmin sırasında hata oluştu"
                    }
                } ?: run {
                    prediction = "Fotoğraf seçilmedi"
                }
            }) {
                Text("Tahmin Et")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Tahmin Sonucu: $prediction")
        }
    }
}
