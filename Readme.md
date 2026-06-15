# Sistem Manajemen Mahasiswa
**Mata Kuliah:** Pemrograman Komputer 2  
**Semester:** 4

## Anggota Tim
1. Muhammad Zaim El Yafi
2. Gusti Rizqi Putra Hanif
3. Mohamad Naufal Arizal

---

## Deskripsi Project
Aplikasi desktop berbasis Java Swing untuk manajemen data mahasiswa dengan sistem autentikasi dua peran (Admin dan Mahasiswa). Data disimpan di MongoDB dan dilindungi dengan enkripsi serta hashing.

---

## Scope / Fitur Utama

### Autentikasi
- Login dengan dua peran: **Admin** dan **Mahasiswa**
- Login mahasiswa menggunakan NIM atau Email + password
- Password disimpan sebagai **hash SHA-256 + salt** (tidak pernah disimpan plaintext)
- Migrasi otomatis password lama (plaintext → hash) saat login pertama kali

### Manajemen Data Mahasiswa (Admin)
- **Create** — tambah data mahasiswa baru (NIM, Nama, Jurusan, Email, Password)
- **Read** — tampilkan semua mahasiswa dalam grid kartu
- **Update** — edit data mahasiswa yang sudah terdaftar
- **Delete** — hapus mahasiswa dari database

### Searching
- Pencarian realtime di grid mahasiswa berdasarkan NIM, ID, Nama, dan Jurusan
- Filter langsung dari cache lokal (tanpa reload ke database)

### Keamanan Data
| Data | Metode | Keterangan |
|---|---|---|
| Password | SHA-256 + salt (1 arah) | Tidak bisa dibaca balik, aman dari rainbow table |
| Email | AES-256-GCM (2 arah) | Terenkripsi di DB, dapat didekripsi untuk ditampilkan |

### Arsitektur
- **DAO Layer** — `GenericDAO<T>` reusable untuk semua entitas
- **Service Layer** — `MahasiswaService`, `LoginService`
- **GUI Layer** — NetBeans Form (Swing): `Login`, `Admin`, `Mahasiswa`
- **Util Layer** — `MongoManager`, `HashUtil`, `CryptoUtil`, `TesKoneksi`

---

## Teknologi
- **Java** 17+
- **MongoDB** 7.x (localhost:27017)
- **MongoDB Java Driver** 5.x (POJO codec)
- **Swing** (NetBeans GUI Builder)
- **Maven** (build & dependency management)

---

## Cara Menjalankan
1. Pastikan MongoDB berjalan di `localhost:27017`
2. Build project: `mvn clean package`
3. Jalankan: `mvn exec:java` atau run `SesuaiTugas.java`
4. Login default admin: `admin@kampus.id` / `admin123`
5. Login default mahasiswa: NIM `DEMO001` / password `student123`

---

## Struktur Project
```
src/main/java/com/mycompany/sesuaitugas/
├── SesuaiTugas.java          # Entry point
├── dao/
│   ├── BaseDAO.java          # Interface CRUD generik
│   ├── GenericDAO.java       # Implementasi DAO untuk MongoDB
│   └── Identifiable.java     # Kontrak ID unik entitas
├── gui/
│   ├── Login.java / .form    # Halaman login
│   ├── Admin.java / .form    # Dashboard admin (CRUD mahasiswa)
│   └── Mahasiswa.java / .form# Halaman mahasiswa
├── objects/
│   ├── Mahasiswa.java        # Model data mahasiswa
│   ├── User.java             # Model user (admin)
│   └── DaftarJurusan.java    # Konstanta daftar jurusan
├── services/
│   ├── LoginService.java     # Logika autentikasi
│   └── MahasiswaService.java # CRUD + keamanan data mahasiswa
└── util/
    ├── MongoManager.java     # Singleton koneksi MongoDB
    ├── HashUtil.java         # Hashing 1 arah (SHA-256 + salt)
    ├── CryptoUtil.java       # Enkripsi 2 arah (AES-256-GCM)
    └── TesKoneksi.java       # Utilitas tes koneksi DB
```
