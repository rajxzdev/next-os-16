# Next OS 16

Launcher Android (bukan web) bernama **Next OS 16**. Kode Kotlin + Java, layout XML, ikon vektor SVG. Tidak ada HTML.

Paket: `com.nextos.launcher`  
minSdk 26 · targetSdk 34 · Java 17

## Fitur

1. Liquid glass (sudut membulat, highlight spekular, dock kaca)
2. Menu widget gaya OS (jam, cuaca, kalender, musik, baterai)
3. Animasi halus + transisi elastis
4. Animasi paralel (jiggle rotasi+skala, spring scale+alpha)
5. Ikon SVG/vektor (tanpa emoji)
6. Menu pengaturan
7. Efek transisi activity (spring interpolator)
8. Wallpaper & lockscreen terintegrasi
9. Mode gelap (sistem / terang / gelap)
10. Kustomisasi ikon (plate pastel) & nama
11. App Library (bukan app drawer tradisional) — kategori + terbaru (Usage Stats)
12. Manajemen folder
13. Sembunyikan aplikasi
14. Kunci aplikasi (PIN + Accessibility gate)
15. Pusat Kontrol
16. **Next Island** (Dynamic Island)
17. Pencarian cepat (Smart Search)
18. Widget gaya OS di kisi beranda

## UI

- Kisi kaku 4×6 (bisa 5 / 6 kolom di pengaturan)
- Sudut 16dp / 22dp / 38dp
- Dock transparan
- Tanpa app drawer vertikal klasik — geser naik ke Perpustakaan
- Palet pastel
- Mode goyang (jiggle) + fisika pegas
- Font sans-serif, label singkat

## Gestur

| Gestur | Aksi |
| --- | --- |
| Geser turun dari atas | Pusat Kontrol |
| Geser naik dari dock | App Library |
| Geser turun di tengah / ketuk dua kali | Smart Search |
| Tahan ikon | Menu + mode goyang |
| Tahan area kosong | Edit beranda + widget / wallpaper |
| Ketuk Next Island | Perluas / ciutkan |

## Hak akses sistem

Buka **Pengaturan → Hak Akses** lalu aktifkan:

1. **Accessibility Service** — gerbang kunci aplikasi saat jendela berubah
2. **Notification Access** — isi Next Island dari notifikasi
3. **Usage Stats API** — kategori “Terbaru” di App Library

Lalu set Next OS 16 sebagai **launcher default**.

## Performa

- `IconCache.java` LRU (~10% heap), ikon diraster sekali di 60dp
- `largeHeap=false`
- Trim memory mengosongkan cache
- ViewPager offscreen limit 1

## Build

Buka folder `NextOS16` di Android Studio (Hedgehog / Iguana / Koala+), sync Gradle, jalankan ke perangkat.

```
File → Open → NextOS16
Run ▶ app
```

Setelah terpasang: **Settings Android → Apps → Default apps → Home app → Next OS 16**.

## Struktur penting

```
app/src/main/java/com/nextos/launcher/
  NextOSApplication.kt
  cache/IconCache.java
  data/LauncherPreferences.java  AppLockStore.java  HomeLayoutManager.kt
  services/NextAccessibilityService.kt  NextNotificationListener.kt
  ui/home/HomeActivity.kt
  ui/island/NextIslandView.kt
  ui/lock/LockScreenActivity.kt
  ui/settings/SettingsActivity.kt
  util/PermissionHelper.java  UsageStatsHelper.java  SpringMotion.java
```

Java dipakai untuk lapisan cache, preferensi, kunci aplikasi, usage stats, permission, dan fisika pegas. Kotlin untuk UI.
