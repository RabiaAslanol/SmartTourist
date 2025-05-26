package com.example.smarttourist

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.smarttourist.ui.theme.SmartTouristTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.Manifest
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.launch
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.isSystemInDarkTheme


enum class BottomNavItem(val title: String, val icon: Int) {
    Kesfet("Keşfet", R.drawable.explore),
    Harita("Harita", R.drawable.map),
    Favoriler("Favoriler", R.drawable.baseline_favorite_border_24),
    Ayarlar("Ayarlar", R.drawable.settings),
    Tahmin("Tahmin", R.drawable.ic_camera)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var karanlikTemaAktif by rememberSaveable { mutableStateOf(false) }
            var selectedTab by rememberSaveable { mutableStateOf(BottomNavItem.Kesfet) }
            var seciliYer by rememberSaveable { mutableStateOf<Yer?>(null) }

            SmartTouristTheme(darkTheme = karanlikTemaAktif) {
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(selectedItem = selectedTab) {
                            selectedTab = it
                        }
                    }
                ) { padding ->
                    BackgroundContainer {
                        Box(modifier = Modifier.padding(padding)) {
                            when (selectedTab) {
                                BottomNavItem.Kesfet -> {
                                    seciliYer?.let {
                                        YerDetayEkrani(yer = it, onGeriDon = { seciliYer = null })
                                    } ?: YerListesiEkrani(onYerSecildi = { seciliYer = it })
                                }
                                BottomNavItem.Harita -> HaritaEkrani()
                                BottomNavItem.Favoriler -> FavoriYerListesiEkrani()
                                BottomNavItem.Ayarlar -> AyarlarEkrani(
                                    temaAktif = karanlikTemaAktif,
                                    onTemaDegistir = { karanlikTemaAktif = it }
                                )
                                BottomNavItem.Tahmin -> TahminEkrani()
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SayfaBasligi(text: String) {
    val isDarkTheme = isSystemInDarkTheme()

    Text(
        text = text,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (isDarkTheme) Color(0xFFEEEEEE) else Color(0xFF800080),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    )
}

@Composable
fun BottomNavigationBar(
    selectedItem: BottomNavItem,
    onItemSelected: (BottomNavItem) -> Unit
) {
    NavigationBar {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = item == selectedItem,
                onClick = { onItemSelected(item) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) }
            )
        }
    }
}

data class Yer(
    val ad: String,
    val aciklama: String,
    val resimUrl: String,
    val latitude: Double,
    val longitude: Double,
    val kisaTarihce: String,
    val kategori: String,
    val zamanDilimi: String,
    val favori: Boolean = false
) {
    val konum: LatLng get() = LatLng(latitude, longitude)
}

suspend fun veriCek(): List<Yer> = withContext(Dispatchers.IO) {
    val url = "https://raw.githubusercontent.com/RabiaAslanol/gezi-rehberi/refs/heads/main/geziYerleri.json"
    val json = java.net.URL(url).readText()
    com.google.gson.Gson().fromJson(json, object : com.google.gson.reflect.TypeToken<List<Yer>>() {}.type)
}
@Composable
fun YerListesiEkrani(
    onYerSecildi: (Yer) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val db = remember { AppDatabase.getDatabase(context) }
    val favoriYerDao = db.favoriYerDao()
    var userLocation by remember { mutableStateOf<Location?>(null) }
    val scope = rememberCoroutineScope()
    var favoriYerler by remember { mutableStateOf<List<FavoriYer>>(emptyList()) }
    var yerlerListesi by remember { mutableStateOf<List<Yer>>(emptyList()) }

    // JSON verisini çek
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                userLocation = it
            }
        }

        scope.launch {
            favoriYerler = favoriYerDao.tumFavoriler()
            yerlerListesi = veriCek() // JSON verisi burada yükleniyor
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp))
    {
        SayfaBasligi("Gezilecek Yerler")
        LazyColumn {
            items(yerlerListesi) { yer ->
                val favorideMi = favoriYerler.any { it.ad == yer.ad }

                val mesafeMetin = userLocation?.let { konum ->
                    val yerKonum = Location("").apply {
                        latitude = yer.latitude
                        longitude = yer.longitude
                    }
                    val distance = konum.distanceTo(yerKonum) / 1000
                    String.format(Locale.getDefault(), "%.1f km", distance)
                } ?: "Mesafe hesaplanıyor..."

                YerKart(
                    yer = yer,
                    mesafe = mesafeMetin,
                    favoriYerDao = favoriYerDao,
                    favorideMi = favorideMi,
                    onFavoriDegisti = {
                        scope.launch {
                            favoriYerler = favoriYerDao.tumFavoriler()
                        }
                    },
                    onDetayTiklandi = { onYerSecildi(yer) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun YerKart(
    yer: Yer,
    mesafe: String,
    favoriYerDao: FavoriYerDao,
    favorideMi: Boolean,
    onFavoriDegisti: () -> Unit,
    onDetayTiklandi: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetayTiklandi() } // 👈
            .padding(bottom = 12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(12.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(model = yer.resimUrl, error = painterResource(id = R.drawable.ic_launcher_foreground)),
                    contentDescription = yer.ad,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = yer.ad, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = yer.aciklama)
                Text(text = "Mesafe: $mesafe", fontSize = 12.sp)
            }
            IconButton(
                onClick = {
                    val favoriYer = FavoriYer(
                        ad = yer.ad,
                        aciklama = yer.aciklama,
                        resimUrl = yer.resimUrl,
                        latitude = yer.konum.latitude,
                        longitude = yer.konum.longitude
                    )

                    scope.launch {
                        if (favorideMi) {
                            favoriYerDao.sil(favoriYer)
                        } else {
                            favoriYerDao.ekle(favoriYer)
                        }
                        onFavoriDegisti()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (favorideMi) R.drawable.ic_favorite else R.drawable.baseline_favorite_border_24
                    ),
                    contentDescription = "Favori",
                    tint = if (favorideMi) Color.Red else Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YerDetayEkrani(yer: Yer, onGeriDon: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(yer.ad, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { onGeriDon() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = rememberAsyncImagePainter(yer.resimUrl),
                contentDescription = yer.ad,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = yer.ad,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = yer.aciklama,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kategori: ${yer.kategori}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "Zaman Dilimi: ${yer.zamanDilimi}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Kısa Tarihçe",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = yer.kisaTarihce,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Konum: %.4f, %.4f".format(yer.konum.latitude, yer.konum.longitude),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HaritaEkrani() {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val geocoder = Geocoder(context, Locale.getDefault())

    val istanbulLatLng = LatLng(41.0082, 28.9784)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(istanbulLatLng, 13f)
    }

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Konum izni kontrolü
    LaunchedEffect(permissionState.status) {
        if (permissionState.status is PermissionStatus.Granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 14f)
                }
            }
        } else if (permissionState.status is PermissionStatus.Denied) {
            permissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Harita
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionState.status is PermissionStatus.Granted
            ),
            onMapClick = { latLng -> selectedLocation = latLng }
        ) {
            selectedLocation?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "Seçilen Konum"
                )
            }
        }

        //  Arama çubuğu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Konum Ara") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    val addresses = geocoder.getFromLocationName(searchQuery, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val latLng = LatLng(address.latitude, address.longitude)
                        selectedLocation = latLng
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 14f)
                    }
                }) {
                    Icon(Icons.Filled.Search, contentDescription = "Ara")
                }
            }
        )

        //  Konuma git butonu
        IconButton(
            onClick = {
                if (permissionState.status is PermissionStatus.Granted) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        location?.let {
                            val userLatLng = LatLng(it.latitude, it.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp) 
                .background(Color.White, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Konuma Git"
            )
        }

        //  Snackbar - konum izni verilmemişse
        if (permissionState.status is PermissionStatus.Denied) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = Color(0xFF323232),
                contentColor = Color.White
            ) {
                Text("Konum izni verilmediği için varsayılan olarak İstanbul gösteriliyor.")
            }
        }
    }
}
@Composable
fun FavoriYerListesiEkrani() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val favoriYerDao = db.favoriYerDao()
    var favoriYerler by remember { mutableStateOf<List<FavoriYer>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            favoriYerler = favoriYerDao.tumFavoriler()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SayfaBasligi("Favori Yerler")
        LazyColumn {
            items(favoriYerler) { yer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter(model = yer.resimUrl),
                                contentDescription = yer.ad,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = yer.ad, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(text = yer.aciklama)
                        }

                        // 🗑 Silme butonu
                        IconButton(
                            onClick = {
                                scope.launch {
                                    favoriYerDao.sil(yer)
                                    favoriYerler = favoriYerDao.tumFavoriler()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete), // Çöp kutusu ikonu
                                contentDescription = "Favoriden Kaldır",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AyarlarEkrani(
    temaAktif: Boolean,
    onTemaDegistir: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val favoriYerDao = db.favoriYerDao()

    var bildirimAcik by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SayfaBasligi("Ayarlar")
        Spacer(modifier = Modifier.height(16.dp))
        // 🔲 Karanlık Tema Kartı
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Karanlık Tema", modifier = Modifier.weight(1f))
                Switch(checked = temaAktif, onCheckedChange = { onTemaDegistir(it) })
            }
        }

        // 🔲 Bildirimler Kartı
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bildirimler", modifier = Modifier.weight(1f))
                Switch(checked = bildirimAcik, onCheckedChange = { bildirimAcik = it })
            }
        }

        // 🧹 Tüm Favorileri Temizle Butonu
        Button(
            onClick = {
                scope.launch {
                    favoriYerDao.tumFavorileriSil()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Tüm Favorileri Temizle", color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "SmartTourist © 2025",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            "Geliştiriciler:\nRabia Aslanol, Nilüfer Ekşinar, Ömer Onur Çamlı",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Onizleme() {
    SmartTouristTheme {
        YerListesiEkrani(onYerSecildi = {})
    }
}
