# CHANGELOG

Semua perubahan signifikan pada project ini didokumentasikan di file ini.  
Format mengacu pada [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — 2026-06-15

### Added

#### `util/HashUtil.java` *(file baru)*
- Utilitas hashing **1 arah** menggunakan SHA-256 + salt acak (SecureRandom 16 byte).
- Method `hash(String plaintext)` — menghasilkan hash format `BASE64_SALT:BASE64_HASH`.
- Method `verify(String plaintext, String storedHash)` — verifikasi input terhadap hash tersimpan menggunakan constant-time comparison (`MessageDigest.isEqual`) untuk mencegah timing attack.
- Method `isHashed(String value)` — mendeteksi apakah string sudah berformat hash (berguna untuk migrasi data lama).

#### `util/CryptoUtil.java` *(file baru)*
- Utilitas enkripsi/dekripsi **2 arah** menggunakan AES-256-GCM (authenticated encryption).
- Secret key diturunkan dari passphrase menggunakan PBKDF2WithHmacSHA256 (65.536 iterasi) — hanya dihitung sekali (lazy singleton).
- IV (initialization vector) 12-byte acak di-generate setiap enkripsi untuk keamanan maksimal.
- Method `encrypt(String plaintext)` — menghasilkan ciphertext format `BASE64_IV:BASE64_CIPHERTEXT`.
- Method `decrypt(String ciphertext)` — mendekripsi kembali ke plaintext; transparan untuk data lama (plaintext dikembalikan apa adanya).
- Method `isEncrypted(String value)` — mendeteksi apakah string sudah terenkripsi (backward compatibility).

---

### Changed

#### `services/MahasiswaService.java`
- **`save()`** — password kini di-hash dengan `HashUtil.hash()` sebelum disimpan ke MongoDB; email dienkripsi dengan `CryptoUtil.encrypt()` sebelum disimpan.
- **`updateByNim()`** — password dan email juga diamankan ulang sebelum update.
- **`findAll()`** — email di setiap entitas hasil query didekripsi otomatis sebelum dikembalikan ke caller (siap ditampilkan di UI).
- **`findByNim()`** — email hasil query didekripsi sebelum dikembalikan.
- **`findByEmail()`** — implementasi diubah: karena AES-GCM menggunakan IV acak (ciphertext berbeda tiap enkripsi yang sama), pencarian dilakukan dengan load-all + dekripsi + compare, bukan query langsung ke MongoDB.
- **Tambah method `verifyPassword()`** — wrapper publik untuk `HashUtil.verify()`, digunakan oleh `LoginService`.
- **Tambah method private `encryptFields()`** — memusatkan logika pengamanan field sensitif sebelum simpan.
- **Tambah method private `decryptFields()`** — memusatkan logika dekripsi setelah baca dari DB.
- Import ditambahkan: `HashUtil`, `CryptoUtil`.

#### `services/LoginService.java`
- **`authenticate()` (admin)** — verifikasi password menggunakan `HashUtil.verify()` sebagai ganti perbandingan `equals()` plaintext. Ditambahkan logika migrasi otomatis: jika password admin masih plaintext (data lama), saat login berhasil password langsung di-hash dan disimpan ulang ke DB.
- **`authenticateMahasiswa()`** — verifikasi password didelegasikan ke `mahasiswaService.verifyPassword()` (menggunakan `HashUtil.verify()`).
- **`ensureDefaultAdmin()`** — password default admin (`admin123`) kini di-hash dengan `HashUtil.hash()` sebelum disimpan (sebelumnya disimpan plaintext).
- **`ensureDefaultMahasiswa()`** — password default mahasiswa (`student123`) kini di-hash melalui `mahasiswaService.save()` secara otomatis.
- Import ditambahkan: `HashUtil`.

---

### Documentation

#### `Readme.md`
- Ditambahkan deskripsi lengkap project.
- Ditambahkan tabel fitur keamanan (hashing 1 arah & enkripsi 2 arah).
- Ditambahkan penjelasan scope/fitur: autentikasi, CRUD, searching, keamanan data.
- Ditambahkan penjelasan arsitektur layer (DAO, Service, GUI, Util).
- Ditambahkan daftar teknologi yang digunakan.
- Ditambahkan instruksi cara menjalankan aplikasi.
- Ditambahkan struktur direktori project.

#### `CHANGELOG.md` *(file baru)*
- File ini dibuat untuk mendokumentasikan seluruh perubahan project.

---

## Catatan Migrasi Data

Jika sudah ada data mahasiswa/admin di MongoDB dengan password plaintext:
- **Admin** — password akan otomatis di-migrasikan ke hash saat login pertama kali setelah update ini.
- **Mahasiswa** — password lama (plaintext) tidak akan bisa login karena `HashUtil.verify()` tidak mengenali format plaintext. Solusi: admin reset password mahasiswa lewat fitur Update di dashboard, atau jalankan script migrasi manual.

> Untuk deployment baru (database kosong), tidak ada masalah — seed data default langsung dibuat dengan password ter-hash.
