#SmartTourist – Akıllı Turist Rehberi
SmartTourist, gezginler için tasarlanmış bir mobil uygulamadır. Kullanıcılar, çevrelerindeki gezilecek yerleri keşfedebilir, favori yerlerini kaydedebilir, harita üzerinden yön bulabilir ve yer türlerini yapay zeka ile tahmin edebilirler.

--- Özellikler

- Harita Entegrasyonu: Google Maps kullanılarak kullanıcılar harita üzerinde gezinebilir.
- Keşfet Sayfası: Gezilecek yerler listelenir ve detaylarına erişilebilir.
- Detay Sayfası: Keşfet sayfasında önerilen yerlerin tıklanarak detay sayfasına geçerek tarihi detay anlatımlara ulaşılabilir.
- Favori Yerler: Kullanıcılar beğendiği yerleri favorilerine ekleyebilir.
- Yer Türü Tahmini: Fotoğrafa göre yerin türü (doğal yapı, tarihi yapı, müze, plaj) TensorFlow Lite modeli ile tahmin edilir.
- Kamera & Galeri Desteği: Kullanıcı fotoğraf çekebilir veya galeriden seçebilir.
- Ayarlar: Uygulama içinde basit ayarlar menüsü ve bilgilendirme sistemi.

--- Kullanılan Teknolojiler

- Kotlin & Jetpack Compose
- TensorFlow Lite (.tflite model entegrasyonu)
- Room Veritabanı(Favori yerleri saklamak için)
- Google Maps SDK
- Gradle bağımlılık yönetimi
- Android ViewModel & LiveData
- Material Design bileşenleri

---Yapay Zeka Özelliği
Uygulamada eğitilmiş bir derin öğrenme modeli kullanılmıştır:
- Model: MobileNetV2 (Transfer Learning)
- Girdi Boyutu: 224x224 px
- Kategori Sayısı: 4 (Tarihi Yapı, Doğal Yapı, Müze, Plaj)
- Model Formatı: `.tflite`
- Tahmin ekranı üzerinden galeri/kamera ile fotoğraf seçimi yapılır ve sınıflandırma sonucu kullanıcıya gösterilir.

