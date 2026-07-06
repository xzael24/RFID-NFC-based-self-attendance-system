# CODE.md — Dokumentasi Teknis & Source Code

Dokumen ini membahas **semua kode** dalam project secara teknis: penjelasan fungsi per-fungsi, parameter, return value, logika internal, dan hubungan antar-kelas.

---

## Daftar Isi

1. [Entry Point: SesuaiTugas.java](#1-entry-point-sesuaitugasjava)
2. [DAO Layer](#2-dao-layer)
3. [Objects Layer](#3-objects-layer)
4. [Services Layer](#4-services-layer)
5. [GUI Layer](#5-gui-layer)
6. [Utility Layer](#6-utility-layer)
7. [Resource Files](#7-resource-files)

---

## 1. Entry Point: SesuaiTugas.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/SesuaiTugas.java`  
**Baris:** 17

```java
package com.mycompany.sesuaitugas;

import com.mycompany.sesuaitugas.gui.Mahasiswa;

public class SesuaiTugas {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new Mahasiswa().setVisible(true));
    }
}
```

### Fungsi
- **`main(String[] args)`** — Method utama yang dipanggil JVM saat aplikasi dijalankan.
- **`EventQueue.invokeLater(Runnable)`** — Menjadwalkan pembuatan GUI di **Event Dispatch Thread (EDT)**. Semua operasi Swing HARUS dilakukan di EDT untuk menghindari race condition dan deadlock.
- **`new Mahasiswa().setVisible(true)`** — Membuat instance frame Mahasiswa dan menampilkannya. Karena aplikasi langsung ke halaman mahasiswa (tanpa login), tidak ada pemanggilan `Login` di sini.

### Alur Eksekusi
```
JVM → main() → EventQueue.invokeLater() → EDT → new Mahasiswa() → constructor
```

---

## 2. DAO Layer

### 2.1 Identifiable.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/dao/Identifiable.java`  
**Baris:** 11

```java
package com.mycompany.sesuaitugas.dao;

public interface Identifiable {
    /** Kembalikan ID unik dokumen (digunakan sebagai kunci filter). */
    String getId();
}
```

### Fungsi
- Interface ini berfungsi sebagai **generic bound** — memastikan hanya entitas yang memiliki ID unik yang bisa dikelola oleh `GenericDAO`.
- Method `getId()` mengembalikan ID entitas sebagai `String`. Di MongoDB, ID ini dipetakan ke field `_id` menggunakan anotasi `@BsonId`.
- Semua entitas (Mahasiswa, User, Absensi) mengimplementasikan interface ini.

### Kenapa Perlu?
Tanpa `Identifiable`, `GenericDAO<T>` tidak bisa menjamin bahwa tipe `T` memiliki ID, sehingga operasi update dan delete tidak bisa dilakukan dengan filter yang aman.

---

### 2.2 BaseDAO.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/dao/BaseDAO.java`  
**Baris:** 58

```java
public interface BaseDAO<T extends Identifiable> {
    void save(T entity);
    void update(Bson filter, T entity);
    void updateFields(Bson filter, Document updates);
    void delete(Bson filter);
    List<T> findAll();
    T findOne(Bson filter);
    List<T> findMany(Bson filter);
}
```

### Method & Parameter

| Method | Parameter | Return | Kegunaan |
|---|---|---|---|
| `save` | `T entity` | `void` | Insert dokumen baru ke MongoDB |
| `update` | `Bson filter, T entity` | `void` | Replace seluruh dokumen yang cocok dengan filter |
| `updateFields` | `Bson filter, Document updates` | `void` | Update parsial — hanya field tertentu |
| `delete` | `Bson filter` | `void` | Hapus dokumen yang cocok filter |
| `findAll` | — | `List<T>` | Ambil SEMUA dokumen dalam koleksi |
| `findOne` | `Bson filter` | `T` atau `null` | Cari SATU dokumen |
| `findMany` | `Bson filter` | `List<T>` | Cari BANYAK dokumen |

### Detail Teknis
- **`Bson filter`** — Menggunakan filter dari MongoDB Java Driver seperti `Filters.eq("nim", "DEMO001")`.
- **`Document updates`** — Objek berisi field yang akan diupdate, misal `new Document("nama", "Budi")`.
- Interface ini **tidak bergantung pada implementasi database** — bisa diganti ke SQL, file, dsb.

---

### 2.3 GenericDAO.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/dao/GenericDAO.java`  
**Baris:** 100

```java
public class GenericDAO<T extends Identifiable> implements BaseDAO<T> {

    private final MongoCollection<T> collection;

    public GenericDAO(String collectionName, Class<T> clazz) {
        this.collection = MongoManager.getDatabase()
            .getCollection(collectionName, clazz);
    }

    @Override
    public void save(T entity) {
        collection.insertOne(entity);
    }

    @Override
    public void update(Bson filter, T entity) {
        collection.replaceOne(filter, entity);
    }

    @Override
    public void updateFields(Bson filter, Document updates) {
        collection.updateOne(filter, new Document("$set", updates),
            new UpdateOptions().upsert(false));
    }

    @Override
    public void delete(Bson filter) {
        collection.deleteOne(filter);
    }

    @Override
    public List<T> findAll() {
        return collection.find().into(new ArrayList<>());
    }

    @Override
    public T findOne(Bson filter) {
        return collection.find(filter).first();
    }

    @Override
    public List<T> findMany(Bson filter) {
        return collection.find(filter).into(new ArrayList<>());
    }
}
```

### Penjelasan Detail

#### Constructor: `GenericDAO(String collectionName, Class<T> clazz)`

```java
this.collection = MongoManager.getDatabase()
    .getCollection(collectionName, clazz);
```

- `MongoManager.getDatabase()` — Mengambil koneksi MongoDB singleton (database `DB_pemkom`).
- `getCollection(collectionName, clazz)` — Mendapatkan koleksi dengan **POJO Codec** otomatis. Parameter `clazz` memberitahu MongoDB Java Driver bahwa dokumen BSON akan langsung dipetakan ke objek Java tipe `T`.
- **POJO Codec** menggunakan refleksi untuk mapping otomatis: field Java `nim` ↔ field BSON `nim`. Anotasi `@BsonId` menandai field mana yang menjadi `_id`.

#### `save(T entity)`
```java
collection.insertOne(entity);
```
- `insertOne` — Insert satu dokumen ke MongoDB.
- Jika entitas memiliki field dengan `@BsonId` yang null, MongoDB akan generate `_id` secara otomatis (ObjectId).
- Operasi ini **atomik** — sukses semua atau gagal.

#### `update(Bson filter, T entity)`
```java
collection.replaceOne(filter, entity);
```
- `replaceOne` — **Replace seluruh dokumen** yang cocok dengan `filter`. 
- Berbeda dengan `updateOne`, method ini mengganti SEMUA field dengan data dari `entity`.
- Hanya dokumen **pertama** yang cocok yang akan diganti.

#### `updateFields(Bson filter, Document updates)`
```java
collection.updateOne(filter, new Document("$set", updates),
    new UpdateOptions().upsert(false));
```
- Menggunakan operator MongoDB `$set` — hanya mengubah field yang disebutkan dalam `updates`.
- Field lain tetap utuh tidak berubah.
- `UpdateOptions().upsert(false)` — jika filter tidak cocok, jangan buat dokumen baru.

#### `delete(Bson filter)`
```java
collection.deleteOne(filter);
```
- Hapus satu dokumen yang cocok dengan filter.
- Gunakan `deleteMany` jika ingin hapus banyak.

#### `findAll()`
```java
return collection.find().into(new ArrayList<>());
```
- `collection.find()` — cursor MongoDB yang melakukan iterasi ke database.
- `.into(new ArrayList<>())` — mengumpulkan semua hasil cursor ke dalam `ArrayList<T>`.

#### `findOne(Bson filter)`
```java
return collection.find(filter).first();
```
- `.first()` — mengembalikan dokumen pertama yang cocok atau `null`.

#### `findMany(Bson filter)`
```java
return collection.find(filter).into(new ArrayList<>());
```
- Sama seperti `findAll` tapi dengan filter.

### Contoh Instantiasi
```java
// Satu class GenericDAO bisa untuk 3 koleksi berbeda:
GenericDAO<Mahasiswa> mahasiswaDAO = new GenericDAO<>("mahasiswa", Mahasiswa.class);
GenericDAO<User>      adminDAO     = new GenericDAO<>("admin", User.class);
GenericDAO<Absensi>   absensiDAO   = new GenericDAO<>("absensi", Absensi.class);
```

---

## 3. Objects Layer

### 3.1 Mahasiswa.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/objects/Mahasiswa.java`  
**Baris:** 137

```java
public class Mahasiswa implements Identifiable {
    private String nim;
    private String idMahasiswa;     // → @BsonId → _id di MongoDB
    private String nama;
    private String jurusan;
    private String password;         // SHA-256 hash (1 arah)
    private String email;            // AES-256-GCM encrypted
    private String rfidHash;         // SHA-256 deterministic hash dari UID
    private String rsaPublicKey;     // RSA public key Base64
    private String rsaEncryptedPrivateKey; // Private key dienkripsi AES
    private transient java.security.PrivateKey transientPrivateKey; // in-memory only
    // ...
}
```

### Field Penting dengan Penjelasan

| Field | Tipe | Anotasi | Kegunaan |
|---|---|---|---|
| `idMahasiswa` | String | `@BsonId` | Primary key di MongoDB (`_id`). Auto-increment numeric. |
| `nim` | String | — | Nomor Induk Mahasiswa, unique key untuk pencarian. |
| `password` | String | — | SHA-256 hash, TIDAK PERNAH plaintext. Format: `base64_salt:base64_hash`. |
| `email` | String | — | **AES-256-GCM encrypted**. Format: `base64_iv:base64_ciphertext`. |
| `rfidHash` | String | — | SHA-256 deterministic (tanpa salt) dari UID kartu RFID. |
| `rsaPublicKey` | String | — | Public key RSA 2048-bit encoding X.509 Base64. Bisa disimpan di DB. |
| `rsaEncryptedPrivateKey` | String | — | Private key RSA dienkripsi AES-256-GCM dengan password user. Format: `salt:iv:cipher`. |
| `transientPrivateKey` | PrivateKey | `@BsonIgnore` | Private key sudah didekripsi — **HANYA di memory**, tidak disimpan ke DB. |

### Method `getId()` — Mapping ke MongoDB
```java
@BsonId
@Override
public String getId() {
    return idMahasiswa;
}
```
- `@BsonId` — Memberitahu POJO Codec bahwa field `idMahasiswa` dipetakan ke `_id` di MongoDB.
- Getter `getId()` dan setter `setId()` digunakan untuk mapping BSON.

### Transient Private Key
```java
private transient java.security.PrivateKey transientPrivateKey;

@BsonIgnore
public java.security.PrivateKey getTransientPrivateKey() { return transientPrivateKey; }

@BsonIgnore
public void setTransientPrivateKey(java.security.PrivateKey k) { this.transientPrivateKey = k; }
```
- `transient` — Keyword Java: field ini TIDAK akan diserialisasi.
- `@BsonIgnore` — Memberitahu POJO Codec untuk mengabaikan field ini saat mapping BSON.
- Private key hanya ada di memory selama sesi login. Begitu logout/gc, key hilang.

---

### 3.2 User.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/objects/User.java`  
**Baris:** 124

```java
public class User implements Identifiable {
    private String email;           // → @BsonId → _id
    private String password;        // SHA-256 hash
    private String role;            // "admin" atau "user"
    private String jurusan;
    private String nama;
    private String nim;
    private String rsaPublicKey;
    private String rsaEncryptedPrivateKey;
}
```

### Perbedaan dengan Mahasiswa
- **`email`** adalah ID (bukan `idMahasiswa`). Admin login menggunakan email.
- **`role`** — Membedakan admin (`"admin"`) dengan user biasa (`"user"`).
- Tidak memiliki `rfidHash` — admin tidak perlu RFID.

### Method `getId()` — Email sebagai Primary Key
```java
@BsonId
@Override
public String getId() {
    return email;
}
```

---

### 3.3 Absensi.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/objects/Absensi.java`  
**Baris:** 80

```java
public class Absensi implements Identifiable {
    private String idAbsensi;    // → @BsonIgnore untuk getId(), ObjectId hex
    private String nim;
    private String nama;
    private String jurusan;
    private String tanggal;      // "yyyy-MM-dd"
    private String waktu;        // "HH:mm:ss"
    private String status;       // "HADIR"
    private long timestamp;      // Unix millis
    private String signature;    // RSA digital signature Base64
}
```

### Field Signature
```java
/** RSA digital signature dari data absensi (nim+tanggal+waktu). */
private String signature;
```
- Diisi saat absensi berhasil jika mahasiswa memiliki private key.
- Data yang ditandatangani: `nim + "|" + tanggal + "|" + waktu`
- Format: Base64 dari output `Signature.sign()`.
- Verifikasi: admin melihat signature → verify dengan public key mahasiswa.

### Method `getId()`
```java
@BsonIgnore
@Override
public String getId() {
    return idAbsensi;
}
```
- `@BsonIgnore` — Karena ID diset manual sebagai ObjectId hex, bukan auto dari MongoDB.

---

### 3.4 DaftarJurusan.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/objects/DaftarJurusan.java`  
**Baris:** 76

```java
public final class DaftarJurusan {
    public static final String[] PILIHAN = {
        "S1 Teknik Mesin",
        "S1 Teknik Informatika",
        "S1 Sistem Informasi",
        // ... 22 jurusan total
    };

    public static String toCanonical(String jurusan) { /* ... */ }
    public static boolean isValidSelection(String jurusan) { /* ... */ }
}
```

### Fungsi Method

#### `toCanonical(String jurusan)`
- Membersihkan input jurusan — menghapus spasi berlebih, mencocokkan dengan daftar resmi.
- Return `null` jika tidak cocok dengan jurusan mana pun.

#### `isValidSelection(String jurusan)`
- Delegasi ke `toCanonical()` — return `true` jika jurusan valid.

### Kenapa Butuh Class Ini?
- Mencegah data kotor masuk ke database (misal: "teknik informatika" vs "S1 Teknik Informatika").
- Combo box di GUI menggunakan array ini → user hanya bisa pilih dari daftar.

---

## 4. Services Layer

### 4.1 LoginService.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/services/LoginService.java`  
**Baris:** 185

```java
public class LoginService {
    private final GenericDAO<User> adminDAO;
    private final MahasiswaService mahasiswaService;
    // ...
}
```

#### Method `authenticate(String email, String password)` — Login Admin

```java
public User authenticate(String email, String password) {
    User user = adminDAO.findOne(Filters.eq("email", email.trim()));
    if (user == null) return null;

    String stored = user.getPassword();
    boolean match;

    if (HashUtil.isHashed(stored)) {
        match = HashUtil.verify(password, stored);
    } else {
        // Migrasi: data lama plaintext → hash
        match = stored.equals(password);
        if (match) {
            user.setPassword(HashUtil.hash(password));
            adminDAO.update(Filters.eq("email", email.trim()), user);
        }
    }
    return match ? user : null;
}
```

**Alur Login Admin:**
1. Cari user by email di koleksi `admin`
2. Jika tidak ditemukan → return `null` (gagal)
3. Cek apakah password di DB sudah hash atau masih plaintext (data lama)
4. Jika sudah hash → `HashUtil.verify()` bandingkan hash input dengan hash stored
5. Jika masih plaintext → bandingkan langsung, lalu **migrasi otomatis** ke hash
6. Cocok → return `User`, tidak → return `null`

#### Method `authenticateMahasiswa(String nimOrEmail, String password)` — Login Mahasiswa

```java
public Mahasiswa authenticateMahasiswa(String nimOrEmail, String password) {
    // 1) Coba cari berdasarkan NIM
    Mahasiswa mhs = mahasiswaService.findByNim(input);
    // 2) Jika tidak, coba cari berdasarkan Email
    if (mhs == null) {
        mhs = mahasiswaService.findByEmail(input);
    }
    if (mhs == null) return null;

    boolean match = mahasiswaService.verifyPassword(password, mhs.getPassword());

    // Jika cocok DAN punya RSA key → decrypt private key
    if (match && mhs.getRsaEncryptedPrivateKey() != null) {
        java.security.PrivateKey privKey =
            CryptoUtil.decryptPrivateKey(mhs.getRsaEncryptedPrivateKey(), password);
        mhs.setTransientPrivateKey(privKey);
    }
    return match ? mhs : null;
}
```

**Alur Login Mahasiswa:**
1. Input bisa NIM atau Email
2. Coba cari by NIM dulu (lebih cepat — indexed)
3. Jika tidak ketemu, cari by Email (scan semua, dekripsi, bandingkan)
4. Verifikasi password dengan hash
5. Jika punya RSA keypair → decrypt private key dari `rsaEncryptedPrivateKey` menggunakan password
6. Private key disimpan di `transientPrivateKey` **hanya di memory**

#### Method `ensureDefaultAdmin()` & `ensureDefaultMahasiswa()` — Seed Data

```java
public void ensureDefaultAdmin() {
    if (adminDAO.findAll().isEmpty()) {
        User admin = new User("admin@kampus.id",
            HashUtil.hash("admin123"), "admin");
        admin.setNama("Administrator");
        adminDAO.save(admin);
    }
}

public void ensureDefaultMahasiswa() {
    List<Mahasiswa> existing = mahasiswaService.findAll();
    if (existing.isEmpty()) {
        // Buat DEMO001, DEMO002, DEMO003
        for (...) {
            Mahasiswa m = new Mahasiswa(nim, "", nama, jurusan, "student123");
            mahasiswaService.save(m);  // Hash password di sini
        }
    }
}
```

---

### 4.2 MahasiswaService.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/services/MahasiswaService.java`  
**Baris:** 203

```java
public class MahasiswaService {
    private final GenericDAO<Mahasiswa> dao;
}
```

#### Method `findAll()` — Ambil Semua + Dekripsi

```java
public List<Mahasiswa> findAll() {
    List<Mahasiswa> list = dao.findAll();
    list.forEach(this::decryptFields);
    return list;
}
```
- `dao.findAll()` — Ambil data mentah dari MongoDB (email masih terenkripsi)
- `decryptFields(m)` — Dekripsi email AES jadi plaintext untuk ditampilkan

#### Method `findByNim(String nim)`

```java
public Mahasiswa findByNim(String nim) {
    Mahasiswa m = dao.findOne(Filters.eq("nim", nim.trim()));
    if (m != null) decryptFields(m);
    return m;
}
```
- Filter MongoDB: `{ nim: "DEMO001" }`
- Dekripsi email sebelum return

#### Method `findByEmail(String email)` — Pencarian Email Terenkripsi

```java
public Mahasiswa findByEmail(String email) {
    String emailNormalized = email.trim().toLowerCase();
    List<Mahasiswa> all = dao.findAll();       // Ambil semua
    for (Mahasiswa m : all) {
        decryptFields(m);                       // Dekripsi satu per satu
        if (emailNormalized.equalsIgnoreCase(m.getEmail())) {
            return m;                           // Bandingkan
        }
    }
    return null;
}
```

**PENTING:** Karena AES-GCM menggunakan IV acak tiap enkripsi, hash email tidak deterministic. Solusinya adalah **load semua, dekripsi satu per satu**. Ini O(n) — untuk skala produksi perlu deterministic encryption atau field `emailHash` terpisah.

#### Method `save(Mahasiswa m)` — Simpan dengan Enkripsi

```java
public void save(Mahasiswa m) {
    // Auto-generate ID jika belum punya
    if (m.getIdMahasiswa() == null || m.getIdMahasiswa().trim().isEmpty()) {
        m.setIdMahasiswa(String.valueOf(getNextIdMahasiswa()));
    }
    encryptFields(m);     // Hash password + enkripsi email
    dao.save(m);
}
```

#### Method `encryptFields(Mahasiswa m)` — Amanin Data Sebelum DB

```java
private void encryptFields(Mahasiswa m) {
    // Hash password — hanya jika belum di-hash
    if (m.getPassword() != null && !m.getPassword().isEmpty()
            && !HashUtil.isHashed(m.getPassword())) {
        m.setPassword(HashUtil.hash(m.getPassword()));
    }

    // Enkripsi email — hanya jika belum terenkripsi
    if (m.getEmail() != null && !m.getEmail().isEmpty()
            && !CryptoUtil.isEncrypted(m.getEmail())) {
        m.setEmail(CryptoUtil.encrypt(m.getEmail()));
    }
}
```

**Logika:**
- `isHashed()` — mendeteksi apakah password sudah dalam format `salt:hash`
- `isEncrypted()` — mendeteksi apakah email sudah dalam format `iv:cipher`
- Method ini **idempotent** — aman dipanggil berulang kali

#### Method `getNextIdMahasiswa()` — Auto Increment ID

```java
private int getNextIdMahasiswa() {
    List<Mahasiswa> all = dao.findAll();
    int maxId = 0;
    for (Mahasiswa m : all) {
        try {
            int current = Integer.parseInt(m.getIdMahasiswa());
            if (current > maxId) maxId = current;
        } catch (NumberFormatException e) {
            // skip non-numeric IDs
        }
    }
    return maxId + 1;
}
```
- Cari ID numerik tertinggi di koleksi
- Return `maxId + 1`
- **Catatan:** Tidak thread-safe untuk concurrent writes. Untuk produksi, gunakan AtomicInteger atau sequence collection MongoDB.

---

### 4.3 AbsensiService.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/services/AbsensiService.java`  
**Baris:** 226

```java
public class AbsensiService {
    private final GenericDAO<Absensi> absensiDAO;
    private final MahasiswaService mahasiswaService;
}
```

#### Inner Class `HasilScan`

```java
public static class HasilScan {
    public final Mahasiswa mahasiswa;
    public final Absensi absensi;

    public HasilScan(Mahasiswa mahasiswa, Absensi absensi) {
        this.mahasiswa = mahasiswa;
        this.absensi = absensi;
    }
}
```
- Data class sederhana untuk mengembalikan dua objek sekaligus (mahasiswa + absensi).
- Field `public final` — immutable setelah dibuat.

#### Method `prosesUid(String uid)` — Proses Tap RFID

```java
public HasilScan prosesUid(String uid) {
    String uidNorm = uid.trim().toUpperCase();

    // Hash UID deterministik (tanpa salt, agar bisa dicari di DB)
    String hashUid = HashUtil.hashDeterministic(uidNorm);

    // Cari mahasiswa berdasarkan rfidHash
    Mahasiswa mhs = mahasiswaService.findByRfidHash(hashUid);

    if (mhs == null) {
        // Fallback: buat dummy
        mhs = new Mahasiswa("UID-" + uidNorm, "UID-" + uidNorm,
            "Pengguna RFID (" + uidNorm + ")", "Umum", "", uidNorm + "@rfid.local");
        mhs.setRfidHash(hashUid);
    }

    // Buat record absensi
    Date now = new Date();
    Absensi absensi = new Absensi(
        mhs.getNim(), mhs.getNama(), mhs.getJurusan(),
        FMT_TANGGAL.format(now), FMT_WAKTU.format(now), "HADIR", now.getTime()
    );
    simpan(absensi);

    return new HasilScan(mhs, absensi);
}
```

**Alur lengkap:**
1. Normalisasi UID → uppercase + trim
2. Hash deterministik SHA-256 (tanpa salt) → `hashUid`
3. Cari mahasiswa di MongoDB berdasarkan `rfidHash` field
4. Jika tidak ditemukan → buat objek dummy (untuk testing/flexibility)
5. Buat objek `Absensi` dengan timestamp sekarang
6. Generate ObjectId sebagai ID
7. Simpan ke MongoDB
8. Return `HasilScan` berisi mahasiswa + absensi

#### Method `prosesUid(String uid, PrivateKey privKey)` — Dengan RSA Sign

```java
public HasilScan prosesUid(String uid, java.security.PrivateKey privKey) {
    HasilScan hasil = prosesUid(uid);   // Panggil tanpa sign dulu
    if (hasil != null && privKey != null && hasil.absensi != null) {
        Absensi a = hasil.absensi;
        String data = a.getNim() + "|" + a.getTanggal() + "|" + a.getWaktu();
        String sig = CryptoUtil.rsaSign(data, privKey);   // Sign
        a.setSignature(sig);
        // Update signature di DB
        absensiDAO.update(Filters.eq("idAbsensi", a.getIdAbsensi()), a);
    }
    return hasil;
}
```

**Overloading:** Dua method `prosesUid` dengan parameter berbeda — satu tanpa sign, satu dengan sign. Java memilih method yang tepat berdasarkan argumen.

#### Method `daftarkanRfid(String nim, String uid)`

```java
public boolean daftarkanRfid(String nim, String uid) {
    Mahasiswa mhs = mahasiswaService.findByNim(nim.trim());
    if (mhs == null) return false;

    String hashUid = HashUtil.hashDeterministic(uid.trim().toUpperCase());
    mhs.setRfidHash(hashUid);
    mahasiswaService.updateByNim(nim.trim(), mhs);
    return true;
}
```
- Cari mahasiswa by NIM
- Hash UID deterministic
- Simpan hash ke field `rfidHash`
- Return `true` jika berhasil

#### Method `simpan(Absensi absensi)`

```java
private void simpan(Absensi absensi) {
    absensi.setIdAbsensi(ObjectId.get().toHexString());
    absensiDAO.save(absensi);
}
```
- `ObjectId.get()` — buat ObjectId baru (embedded timestamp + uniqueness).
- `.toHexString()` — konversi ke string hex 24 karakter.

---

## 5. GUI Layer

### 5.1 Login.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/gui/Login.java`  
**Baris:** 533 (termasuk generated code)

#### Constructor

```java
public Login() {
    this(null);
}

public Login(javax.swing.JFrame parentFrameToClose) {
    this.parentFrameToClose = parentFrameToClose;
    initComponents();
    loginService = new LoginService();
    loginService.ensureDefaultAdmin();
    loginService.ensureDefaultMahasiswa();
    SeedDummyData.seed();
    setupListeners();
    setupPlaceholders();
    restoreRememberMe();
    refreshUIText();
}
```

**Parameter `parentFrameToClose`:**
- Digunakan saat Login dipanggil dari tombol "Admin" di halaman Mahasiswa
- Ketika admin login sukses → `parentFrameToClose.dispose()` (tutup halaman Mahasiswa)

#### Method `setupPlaceholders()` — Placeholder di Text Field

```java
private void setupPlaceholders() {
    final String emailPlaceholder = "nim@kampus.id";
    final String passwordPlaceholder = "Masukkan kata sandi";
    final Color placeholderColor = new Color(153, 153, 153);
    final Color normalColor = Color.BLACK;

    // Email field
    jTextField1.setForeground(placeholderColor);
    jTextField1.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
            if (jTextField1.getText().equals(emailPlaceholder)) {
                jTextField1.setText("");
                jTextField1.setForeground(normalColor);
            }
        }
        @Override
        public void focusLost(FocusEvent e) {
            if (jTextField1.getText().trim().isEmpty()) {
                jTextField1.setForeground(placeholderColor);
                jTextField1.setText(emailPlaceholder);
            }
        }
    });
    // Mirror untuk password field ...
}
```

**Teknik Placeholder:**
- Java Swing `JTextField` dan `JPasswordField` tidak memiliki properti placeholder bawaan.
- Implementasi manual: set text awal sebagai placeholder, ganti foreground jadi abu-abu.
- `FocusListener` — saat fokus masuk, hapus placeholder; saat fokus keluar, restore jika kosong.
- `JPasswordField` — echo char di-set `(char)0` saat placeholder tampil, `\u2022` saat password aktif.

#### Method `performLogin()` — Logika Login

```java
private void performLogin() {
    String email = jTextField1.getText().trim();
    String password = new String(jPasswordField1.getPassword());

    // 1) Validasi kosong
    if (email.isEmpty() || email.equals("nim@kampus.id")) { /* error */ }
    if (password.isEmpty() || password.equals("Masukkan kata sandi")) { /* error */ }

    // 2) Coba admin
    User adminUser = loginService.authenticate(email, password);
    if (adminUser != null) {
        simpanRememberMe(email);
        openAdminDashboard(adminUser);
        return;
    }

    // 3) Coba mahasiswa
    Mahasiswa mhs = loginService.authenticateMahasiswa(email, password);
    if (mhs != null) {
        simpanRememberMe(email);
        openMahasiswaPage(mhs);
        return;
    }

    // 4) Gagal
    JOptionPane.showMessageDialog(this, "Email/NIM atau password salah.");
}
```

#### Method `openAdminDashboard(User user)` — Bukak Admin

```java
private void openAdminDashboard(User user) {
    JFrame adminFrame = new JFrame("Admin Dashboard");
    adminFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    adminFrame.setContentPane(new Admin(user));
    adminFrame.pack();
    adminFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    adminFrame.setVisible(true);

    if (parentFrameToClose != null) {
        parentFrameToClose.dispose();  // Tutup halaman Mahasiswa
    }
    this.dispose();  // Tutup Login
}
```

---

### 5.2 Admin.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/gui/Admin.java`  
**Baris:** 801 (termasuk generated code)

#### Constructor

```java
public Admin(User sessionUser) {
    this.sessionUser = sessionUser;
    initComponents();
    installSessionBanner();
    addLanguageButtonsAdmin();
    CB_Jurusan.setModel(new DefaultComboBoxModel<>(DaftarJurusan.PILIHAN));
    setupMahasiswaGrid();
    setupSearchField();
    populateRfidPortCombo();
    wireActions();
    reloadMahasiswaFromDb();
    refreshUITextAdmin();
}
```

#### Method `setupSearchField()` — Search Realtime

```java
private void setupSearchField() {
    jTextField3.getDocument().addDocumentListener(new DocumentListener() {
        @Override public void insertUpdate(DocumentEvent e) { applySearchFilter(); }
        @Override public void removeUpdate(DocumentEvent e) { applySearchFilter(); }
        @Override public void changedUpdate(DocumentEvent e) { applySearchFilter(); }
    });
}
```

**Kenapa `DocumentListener`?**
- `ActionListener` hanya triggered saat user tekan Enter
- `DocumentListener` triggered setiap kali teks berubah (setiap karakter)
- Efek: filter grid realtime saat user mengetik

#### Method `applySearchFilter()` — Filter dari Cache

```java
private void applySearchFilter() {
    String q = searchQueryNormalized();  // Ambil query + lowercase
    List<Mahasiswa> filtered = new ArrayList<>();

    for (Mahasiswa m : mahasiswaCache) {   // Filter dari CACHE (tidak ke DB)
        if (rowMatchesSearch(m, q)) {
            filtered.add(m);
        }
    }

    // Render kartu untuk yang cocok
    renderMahasiswaCards(filtered);
}

private boolean rowMatchesSearch(Mahasiswa m, String q) {
    if (q.isEmpty()) return true;
    String nim = m.getNim() != null ? m.getNim().toLowerCase() : "";
    String id = m.getIdMahasiswa() != null ? m.getIdMahasiswa().toLowerCase() : "";
    String nama = m.getNama() != null ? m.getNama().toLowerCase() : "";
    String jur = m.getJurusan() != null ? m.getJurusan().toLowerCase() : "";
    return nim.contains(q) || id.contains(q) || nama.contains(q) || jur.contains(q);
}
```

**Optimasi:**
- `mahasiswaCache` di-load sekali dari DB (`reloadMahasiswaFromDb()`)
- Filter dilakukan dari **cache lokal**, bukan query MongoDB ulang
- Jauh lebih cepat untuk interaksi realtime

#### Method `buildMahasiswaCard(Mahasiswa m)` — Membuat Kartu

```java
private JPanel buildMahasiswaCard(Mahasiswa m) {
    JPanel card = new JPanel(new BorderLayout(0, 6));
    card.putClientProperty("nim", nim);   // Simpan NIM sebagai property
    card.setPreferredSize(new Dimension(220, 168));

    // Nama di bagian atas
    JLabel nameLb = new JLabel(m.getNama());
    card.add(nameLb, BorderLayout.NORTH);

    // Info tengah (NIM, ID, Jurusan, Email) — HTML dengan styling
    StringBuilder sb = new StringBuilder();
    sb.append("<html><body style='width:190px;color:#555;font-size:11px'>");
    sb.append("<b>NIM</b> ").append(nim).append("<br/>");
    sb.append("<b>RFID</b> ").append(m.getRfidHash() != null
        ? "<span style='color:green'>✓ Terdaftar</span>"
        : "<span style='color:red'>✗ Belum</span>");
    card.add(new JLabel(sb.toString()), BorderLayout.CENTER);

    // Tombol aksi di bagian bawah
    JPanel actions = new JPanel(new FlowLayout());
    actions.add(new JButton("Edit"));
    actions.add(new JButton("Riwayat"));
    actions.add(new JButton("Hapus"));
    card.add(actions, BorderLayout.SOUTH);

    return card;
}
```

**`putClientProperty`:**
- Menyimpan data NIM dalam komponen tanpa perlu subclass JPanel
- Berguna untuk: `card.getClientProperty("nim")` saat tombol diklik

#### Method `showRiwayatAbsensi(String nim, String nama)` — Verifikasi Signature

```java
private void showRiwayatAbsensi(String nim, String nama) {
    // Load public key mahasiswa
    Mahasiswa mhs = mahasiswaService.findByNim(nim);
    PublicKey pubKey = CryptoUtil.stringToPublicKey(mhs.getRsaPublicKey());

    List<Absensi> list = absensiService.findByNim(nim);
    for (Absensi a : list) {
        String data = a.getNim() + "|" + a.getTanggal() + "|" + a.getWaktu();
        boolean valid = CryptoUtil.rsaVerify(data, a.getSignature(), pubKey);
        data[i][4] = valid ? "✓ Sah (RSA)" : "✗ Invalid";
    }
    // Tampilkan di JTable dalam JOptionPane
}
```

**Alur Verifikasi:**
1. Ambil `rsaPublicKey` mahasiswa dari DB
2. Untuk setiap record absensi: ambil `data = nim|tanggal|waktu`
3. Panggil `CryptoUtil.rsaVerify(data, signature, publicKey)`
4. Jika valid → ✓ Sah, Jika tidak → ✗ Invalid

---

### 5.3 Mahasiswa.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/gui/Mahasiswa.java`  
**Baris:** 563 (termasuk generated code)

#### Constructor

```java
public Mahasiswa() {
    initComponents();
    java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    setSize(screen.width, screen.height);
    seedInitialData();         // Seed admin & mahasiswa default
    setupClock();              // Timer untuk jam realtime
    setupLogTable();           // Setup model tabel
    setupPortCombo();          // List COM port available
    setupListeners();          // Wire action listeners
    refreshUITextMahasiswa();  // Set teks sesuai locale
    txtRfidInput.requestFocusInWindow();
}
```

#### Method `prosesUidDariSerial(String uid)` — Dengan SwingWorker

```java
private void prosesUidDariSerial(String uid) {
    showStatus("Memproses kartu: " + uid + " ...", new Color(0, 102, 204));
    PrivateKey privKey = (loggedInMhs != null) ? loggedInMhs.getTransientPrivateKey() : null;

    new SwingWorker<HasilScan, Void>() {
        @Override
        protected HasilScan doInBackground() {
            // Thread background: hash → DB → sign
            return (privKey != null)
                ? absensiService.prosesUid(uid, privKey)
                : absensiService.prosesUid(uid);
        }

        @Override
        protected void done() {
            // EDT: update UI dengan hasil
            HasilScan hasil = get();
            if (hasil == null) {
                showStatus("Kartu tidak terdaftar: " + uid, new Color(204, 0, 0));
            } else {
                tampilkanHasilScan(hasil);
            }
        }
    }.execute();
}
```

**Alur:**
1. Ambil transientPrivateKey dari `loggedInMhs` (mungkin null jika no login)
2. Buat `SwingWorker` baru
3. `doInBackground()` — proses di background thread (tidak memblokir UI)
4. `done()` — update UI di EDT setelah selesai
5. Private key opsional — jika null, absensi tetap jalan tanpa signature

#### Method `onToggleConnect()` — Hubung/Putus Serial

```java
private void onToggleConnect() {
    if (rfidListener != null && rfidListener.isRunning()) {
        rfidListener.stop();           // Putus koneksi
        rfidListener = null;
        btnConnect.setText("Hubungkan");
    } else {
        String port = (String) cbPort.getSelectedItem();
        rfidListener = new RfidSerialListener(port,
            uid -> SwingUtilities.invokeLater(() -> prosesUidDariSerial(uid)));

        if (rfidListener.start()) {
            btnConnect.setText("Putuskan");
        } else {
            // Gagal konek
        }
    }
}
```

**Callback Pattern:**
- `RfidSerialListener` menerima callback `UidCallback`
- Dalam callback: `SwingUtilities.invokeLater()` → pindah ke EDT untuk update UI
- `prosesUidDariSerial()` kemudian menggunakan `SwingWorker` lagi untuk background DB ops

#### Method `setupClock()` — Realtime Clock

```java
private void setupClock() {
    updateClockLabels();
    new Timer(1000, e -> updateClockLabels()).start();
}

private void updateClockLabels() {
    Date now = new Date();
    lblClock.setText(new SimpleDateFormat("HH:mm:ss").format(now));
    lblDate.setText(SimpleDateFormat("EEEE, dd MMMM yyyy",
        new Locale("id", "ID")).format(now));
}
```

- `javax.swing.Timer` — berbeda dengan `java.util.Timer`, timer ini menjalankan task di EDT.
- Interval 1000ms = 1 detik.
- Format tanggal menggunakan `Locale("id", "ID")` untuk nama hari Indonesia.

#### Method `onAdminLogin()` — Tombol Admin

```java
private void onAdminLogin() {
    Login loginFrame = new Login(this);   // this = Mahasiswa frame
    loginFrame.setVisible(true);
}
```

- `this` diteruskan sebagai `parentFrameToClose`
- Saat admin login sukses → Login menutup `this` (halaman Mahasiswa)
- Jika login ditutup tanpa sukses → Mahasiswa tetap terbuka

---

## 6. Utility Layer

### 6.1 MongoManager.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/util/MongoManager.java`  
**Baris:** 79

```java
public class MongoManager {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static final String DATABASE_NAME = "DB_pemkom";

    public static MongoDatabase getDatabase() {
        if (database == null) {
            CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(
                    PojoCodecProvider.builder().automatic(true).build()));

            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase(DATABASE_NAME)
                .withCodecRegistry(pojoCodecRegistry);
        }
        return database;
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}
```

### Singleton Pattern
```java
if (database == null) { /* init */ }
return database;
```
- Lazy initialization — koneksi hanya dibuat saat pertama kali dibutuhkan
- Thread-safe? Sebenarnya tidak — dua thread bisa masuk `if` bersamaan. Tapi MongoDB client sendiri thread-safe, jadi hanya double-initialisasi kecil yang tidak berbahaya.

### POJO Codec Registry
```java
CodecRegistries.fromRegistries(
    MongoClientSettings.getDefaultCodecRegistry(),  // Default BSON types
    CodecRegistries.fromProviders(
        PojoCodecProvider.builder().automatic(true).build()) // POJO mapping
);
```
- `automatic(true)` — secara otomatis mendaftarkan semua kelas Java yang digunakan
- Tanpa ini, MongoDB tidak bisa mapping antara objek Java dan BSON Document

### Connection String
```
mongodb://localhost:27017
```
- Koneksi ke MongoDB lokal (default port)
- Tidak ada autentikasi (sesuai lingkungan development)

---

### 6.2 HashUtil.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/util/HashUtil.java`  
**Baris:** 147

#### Method `hash(String plaintext)` — Salted Hash

```java
public static String hash(String plaintext) {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);                    // 16 byte salt acak

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(salt);                       // digest.update() = update message
    byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

    return b64(salt) + ":" + b64(hashBytes);  // Format: SALT:HASH
}
```

**Mengapa `digest.update(salt)` sebelum `digest()`?**
- `MessageDigest` bekerja secara berantai (streaming)
- Setiap panggilan `update()` menambahkan data ke message buffer
- Panggilan `digest()` menghitung hash dari semua data yang sudah di-`update`

**Format Output Contoh:**
```
B8xR5G2Hsw=:2fVJ9LJ39XyZ...
├─ salt ─┤├────── hash ──────┤
```

#### Method `verify(String plaintext, String storedHash)` — Verifikasi

```java
public static boolean verify(String plaintext, String storedHash) {
    String[] parts = storedHash.split(":", 2);
    byte[] salt = Base64.getDecoder().decode(parts[0]);
    byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(salt);
    byte[] actualHash = digest.digest(plaintext.getBytes("UTF-8"));

    return MessageDigest.isEqual(expectedHash, actualHash);
}
```

**`MessageDigest.isEqual()` — Constant-time Comparison:**
```java
boolean isEqual(byte[] a, byte[] b)
```
Berbeda dengan `a.equals(b)` yang bisa **short-circuit** (return false di byte pertama yang beda), `isEqual()` membandingkan SEMUA byte, mencegah **timing attack**.

#### Method `hashDeterministic(String plaintext)` — Tanpa Salt

```java
public static String hashDeterministic(String plaintext) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

    StringBuilder sb = new StringBuilder();
    for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}
```

- Tanpa salt → selalu menghasilkan hash yang sama untuk input yang sama
- Output hex (0-9, a-f), bukan Base64
- **Panjang:** 64 karakter (32 byte × 2)
- Digunakan untuk: `rfidHash` di MongoDB agar bisa dicari langsung

#### Method `isHashed(String value)` — Deteksi Format Hash

```java
public static boolean isHashed(String value) {
    String[] parts = value.split(":", 2);
    if (parts.length != 2) return false;
    try {
        Base64.getDecoder().decode(parts[0]);
        // Hash SHA-256 = 32 byte = 44 karakter Base64 (padding)
        return Base64.getDecoder().decode(parts[1]).length == 32;
    } catch (IllegalArgumentException e) {
        return false;  // Bukan Base64 valid
    }
}
```

---

### 6.3 CryptoUtil.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/util/CryptoUtil.java`  
**Baris:** 318

#### Konfigurasi

```java
private static final String CIPHER_ALGO   = "AES/GCM/NoPadding";
private static final String KDF_ALGO      = "PBKDF2WithHmacSHA256";
private static final int    GCM_TAG_BITS  = 128;
private static final int    IV_LENGTH     = 12;    // 96-bit
private static final int    KEY_LENGTH    = 256;   // AES-256
private static final int    ITERATIONS    = 65_536;
```

#### Method `getSecretKey()` — Key Derivation dengan PBKDF2

```java
private static synchronized SecretKey getSecretKey() {
    if (secretKey == null) {
        PBEKeySpec spec = new PBEKeySpec(
            PASSPHRASE.toCharArray(), KEY_SALT, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        secretKey = new SecretKeySpec(keyBytes, "AES");
        spec.clearPassword();   // Hapus password dari memory
    }
    return secretKey;
}
```

**Penjelasan:**
- **PBKDF2** (Password-Based Key Derivation Function 2) — mengubah passphrase menjadi kunci AES.
- **65.536 iterasi** — memperlambat serangan brute-force.
- **`synchronized`** — thread-safe lazy initialization.
- **`spec.clearPassword()`** — hapus passphrase dari char array setelah selesai.

#### Method `encrypt(String plaintext)` — AES-256-GCM

```java
public static String encrypt(String plaintext) {
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);   // IV acak

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(),
        new GCMParameterSpec(GCM_TAG_BITS, iv));
    byte[] cipherBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));

    return b64(iv) + ":" + b64(cipherBytes);
}
```

**Format Output:**
```
cGtpOjEyMzQ1Njc4OTA=:a3yF9G2HswJ...
├────── IV ──────┤├─── ciphertext ─────┤
```

**AES-GCM Mode:**
- Mengenkripsi data + menghasilkan **authentication tag** (128-bit) dalam satu operasi
- Authentication tag menjamin **integritas** — ciphertext yang diubah akan gagal didekripsi
- IV 12-byet (96-bit) — cukup unik untuk setiap enkripsi

#### Method `decrypt(String ciphertext)` — AES-256-GCM

```java
public static String decrypt(String ciphertext) {
    if (!isEncrypted(ciphertext)) {
        return ciphertext;  // Backward compat: data lama plaintext
    }

    String[] parts = ciphertext.split(":", 2);
    byte[] iv = Base64.getDecoder().decode(parts[0]);
    byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(),
        new GCMParameterSpec(GCM_TAG_BITS, iv));
    byte[] plainBytes = cipher.doFinal(cipherBytes);
    return new String(plainBytes, "UTF-8");
}
```

**Jika authentication gagal** — `cipher.doFinal()` throw `AEADBadTagException`. Data dinyatakan rusak atau bukan untuk kunci ini.

#### Method `isEncrypted(String value)` — Deteksi Format

```java
public static boolean isEncrypted(String value) {
    String[] parts = value.split(":", 2);
    if (parts.length != 2) return false;
    try {
        byte[] ivBytes = Base64.getDecoder().decode(parts[0]);
        Base64.getDecoder().decode(parts[1]);
        return ivBytes.length == 12;  // IV GCM harus 12 byte
    } catch (IllegalArgumentException e) {
        return false;
    }
}
```

#### — RSA Asymmetric Encryption —

#### Method `generateRsaKeyPair()`

```java
public static KeyPair generateRsaKeyPair() {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048, new SecureRandom());
    return gen.generateKeyPair();
}
```

- **RSA 2048-bit** — standar industri untuk signature digital (secure hingga ~2030)
- `SecureRandom` untuk sumber entropi yang aman

#### Method `rsaSign(String data, PrivateKey privKey)`

```java
public static String rsaSign(String data, PrivateKey privKey) {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initSign(privKey);
    sig.update(data.getBytes("UTF-8"));
    return Base64.getEncoder().encodeToString(sig.sign());
}
```

- **SHA256withRSA** — hash data dengan SHA-256 dulu, lalu enkripsi hash dengan RSA private key
- Output: Base64 string

#### Method `rsaVerify(String data, String signatureB64, PublicKey pubKey)`

```java
public static boolean rsaVerify(String data, String signatureB64, PublicKey pubKey) {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initVerify(pubKey);
    sig.update(data.getBytes("UTF-8"));
    return sig.verify(Base64.getDecoder().decode(signatureB64));
}
```

#### Method `encryptPrivateKey(PrivateKey privKey, String password)`

```java
public static String encryptPrivateKey(PrivateKey privKey, String password) {
    byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
    byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);

    // Derive AES key dari password
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
    byte[] keyBytes = factory.generateSecret(spec).getEncoded();
    spec.clearPassword();
    SecretKey aesKey = new SecretKeySpec(keyBytes, "AES");

    // Enkripsi private key dengan AES-GCM
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
    byte[] encPriv = cipher.doFinal(privKey.getEncoded());

    return b64(salt) + ":" + b64(iv) + ":" + b64(encPriv);
}
```

**Format Output:**
```
base64(salt):base64(iv):base64(encrypted_private_key)
```

#### Method `decryptPrivateKey(String encryptedB64, String password)`

```java
public static PrivateKey decryptPrivateKey(String encryptedB64, String password) {
    String[] parts = encryptedB64.split(":", 3);
    byte[] salt = Base64.getDecoder().decode(parts[0]);
    byte[] iv = Base64.getDecoder().decode(parts[1]);
    byte[] encPriv = Base64.getDecoder().decode(parts[2]);

    // Derive AES key dari password (sama seperti encrypt)
    SecretKey aesKey = deriveKey(password, salt);

    // Dekripsi
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
    byte[] privBytes = cipher.doFinal(encPriv);

    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
}
```

---

### 6.4 RfidSerialListener.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/util/RfidSerialListener.java`  
**Baris:** 184

#### Interface `UidCallback`

```java
public interface UidCallback {
    void onUidReceived(String uid);
}
```

- Functional interface — parameter callback untuk kelas pemanggil
- Bisa digunakan dengan lambda: `uid -> prosesUidDariSerial(uid)`

#### Constructor

```java
public RfidSerialListener(String portName, int baudRate, UidCallback callback) {
    this.portName = portName;
    this.baudRate = baudRate;       // Default: 9600
    this.callback = callback;
}
```

#### Method `start()` — Buka Port & Listener

```java
public boolean start() {
    port = SerialPort.getCommPort(portName);
    port.setBaudRate(baudRate);
    port.setNumDataBits(8);
    port.setNumStopBits(ONE_STOP_BIT);
    port.setParity(NO_PARITY);

    if (!port.openPort()) return false;

    port.addDataListener(new SerialPortDataListener() {
        @Override
        public int getListeningEvents() {
            return LISTENING_EVENT_DATA_AVAILABLE;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            int available = port.bytesAvailable();
            byte[] bytes = new byte[available];
            port.readBytes(bytes, available);
            String chunk = new String(bytes, US_ASCII);

            buffer.append(chunk);          // Akumulasi data
            int idx;
            while ((idx = buffer.indexOf("\n")) >= 0) {
                String uid = buffer.substring(0, idx).trim();
                buffer.delete(0, idx + 1);
                if (!uid.isEmpty()) {
                    callback.onUidReceived(uid.toUpperCase());
                }
            }
        }
    });
    return true;
}
```

**Parsing Data Serial:**
```
Arduino kirim:      "A3F2\n"
                  chunk = "A3F2\n"
                  buffer = "" → "A3F2\n"
                  idx "\n" = 4
                  uid = buffer[0..4] = "A3F2"
                  buffer = buffer[5..] = ""
                  callback("A3F2")
```

**Kenapa pakai `StringBuilder` buffer?**
- Data serial bisa tiba dalam potongan-potongan kecil
- Satu pesan `"A3F2\n"` bisa tiba sebagai `"A3"` lalu `"F2\n"`
- Buffer mengakumulasi sampai newline ditemukan

**Event-driven:**
- Tidak perlu polling — jSerialComm panggil `serialEvent()` otomatis saat data tersedia
- Event listener berjalan di thread internal library

#### Method `stop()`

```java
public void stop() {
    port.removeDataListener();
    port.closePort();
}
```

#### Method `getAvailablePorts()` — Static Utility

```java
public static String[] getAvailablePorts() {
    SerialPort[] ports = SerialPort.getCommPorts();
    String[] names = new String[ports.length];
    for (int i = 0; i < ports.length; i++) {
        names[i] = ports[i].getSystemPortName();
    }
    return names;
}
```

---

### 6.5 I18nManager.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/util/I18nManager.java`  
**Baris:** 96

```java
public final class I18nManager {
    private static Locale currentLocale = new Locale("id", "ID");
    private static ResourceBundle bundle;

    static {
        loadBundle();  // Load saat class pertama kali di-load
    }

    private static void loadBundle() {
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadBundle();  // Reload bundle dengan locale baru
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;  // Fallback: tampilkan key-nya saja
        }
    }
}
```

**`ResourceBundle.getBundle("messages", locale)`:**
- Mencari file `messages_id_ID.properties` jika locale `id_ID`
- Mencari file `messages_id.properties` jika locale `id`
- Mencari file `messages.properties` (default) jika tidak ditemukan

**Static Initializer `static { loadBundle(); }`:**
- Bundle di-load saat pertama kali class diakses
- Tanpa ini, `get()` bisa dipanggil sebelum `loadBundle()` selesai

---

### 6.6 SeedDummyData.java

**Path:** `src/main/java/com/mycompany/sesuaitugas/util/SeedDummyData.java`  
**Baris:** 63

```java
public class SeedDummyData {
    public static void seed() {
        MahasiswaService service = new MahasiswaService();
        List<Mahasiswa> existing = service.findAll();

        // Cari mahasiswa dengan rfidHash null
        if (existing.stream().anyMatch(m -> m.getRfidHash() == null)) {
            // Tambah dummy RFID UID ke setiap mahasiswa yang belum punya
            String[] dummyUids = {"A1B2C3D4", "E5F6G7H8", "I9J0K1L2", /* ... */};

            int idx = 0;
            for (Mahasiswa m : existing) {
                if (m.getRfidHash() == null || m.getRfidHash().isEmpty()) {
                    String uid = dummyUids[idx % dummyUids.length];
                    String hash = HashUtil.hashDeterministic(uid);
                    m.setRfidHash(hash);
                    service.updateByNim(m.getNim(), m);
                    idx++;
                }
            }
        }
    }
}
```

**Tujuan:**
- Otomatis memberikan UID dummy ke mahasiswa yang belum punya RFID hash
- Memudahkan testing dengan kartu RFID simulasi (tombol "Simulasi RFID")

---

## 7. Resource Files

### 7.1 messages_id.properties

**Path:** `src/main/resources/messages_id.properties`  
**Baris:** 41

```properties
app.title=Sistem Manajemen Mahasiswa
login.title=Masuk Mahasiswa
login.subtitle=Selamat datang kembali! Silakan masukkan detail Anda.
login.nimLabel=Masukan NIM / Email
login.passwordLabel=Kata Sandi
login.showPassword=Tampilkan kata sandi
login.rememberMe=Ingat saya selama 30 hari
login.buttonLogin=Masuk
admin.title=Dashboard Admin
admin.subtitle=Manajemen Data Mahasiswa
admin.buttonAdd=Tambah
admin.buttonEdit=Edit
admin.buttonDelete=Hapus
admin.buttonLogout=Logout
admin.portLabel=Port COM
admin.buttonConnect=Hubungkan
admin.buttonRegisterCard=Daftar Kartu
admin.uidResult=UID Hasil Pembacaan
admin.nimToRegister=NIM untuk Didaftarkan
admin.email=Email
admin.password=Password
mahasiswa.title=Sistem Absensi RFID
mahasiswa.subtitle=Tap Kartu Anda untuk Absensi
mahasiswa.portLabel=Port COM
mahasiswa.rfidLabel=Input RFID/NFC
mahasiswa.buttonScan=Scan
mahasiswa.buttonSimulation=Simulasi
mahasiswa.buttonAdminLogin=Admin
mahasiswa.namaLabel=Nama
mahasiswa.nimLabel=NIM
mahasiswa.jurusanLabel=Jurusan
mahasiswa.idLabel=ID Kartu
mahasiswa.logTitle=Riwayat Absensi
```

### 7.2 messages_en.properties

**Path:** `src/main/resources/messages_en.properties`  
**Baris:** 41

```properties
app.title=Student Management System
login.title=Student Login
login.subtitle=Welcome back! Please enter your details.
login.nimLabel=Enter Student ID / Email
login.passwordLabel=Password
login.showPassword=Show password
login.rememberMe=Remember me for 30 days
login.buttonLogin=Login
admin.title=Admin Dashboard
admin.subtitle=Student Data Management
admin.buttonAdd=Add
admin.buttonEdit=Edit
admin.buttonDelete=Delete
admin.buttonLogout=Logout
admin.portLabel=COM Port
admin.buttonConnect=Connect
admin.buttonRegisterCard=Register Card
admin.uidResult=UID Read Result
admin.nimToRegister=NIM to Register
admin.email=Email
admin.password=Password
mahasiswa.title=RFID Attendance System
mahasiswa.subtitle=Tap Your Card to Attendance
mahasiswa.portLabel=COM Port
mahasiswa.rfidLabel=RFID/NFC Input
mahasiswa.buttonScan=Scan
mahasiswa.buttonSimulation=Simulation
mahasiswa.buttonAdminLogin=Admin
mahasiswa.namaLabel=Name
mahasiswa.nimLabel=NIM
mahasiswa.jurusanLabel=Major
mahasiswa.idLabel=Card ID
mahasiswa.logTitle=Attendance History
```

### Cara Key Mapping

Setiap key di GUI dipanggil dengan:
```java
I18nManager.get("admin.title")     // "Dashboard Admin" atau "Admin Dashboard"
I18nManager.get("login.buttonLogin") // "Masuk" atau "Login"
```

Saat locale berubah → `setLocale()` → reload bundle → semua GUI panggil `get()` ulang.

---

## Ringkasan Arsitektur Kode

| Layer | Package | Prinsip |
|---|---|---|
| **DAO** | `dao/` | Generic programming — satu implementasi untuk semua entitas |
| **Objects** | `objects/` | POJO dengan mapping MongoDB `@BsonId` |
| **Services** | `services/` | Business logic — hash, enkripsi, autentikasi, absensi |
| **GUI** | `gui/` | Swing + NetBeans `.form` — pemisahan generated vs manual code |
| **Util** | `util/` | Utility — koneksi DB, kriptografi, serial, i18n |

**Alur data tipikal:** GUI → Service → DAO → MongoDB → balik ke Service (dekripsi/enkripsi) → GUI

```java
// Contoh alur: Admin klik "Add Mahasiswa"
Admin.java:376 → onSaveMahasiswa()
  → MahasiswaService.java:106 → save(m)
    → encryptFields(m)           // Hash password + enkripsi email
    → GenericDAO.java:51 → save(m)
      → MongoCollection.insertOne(m)  // MongoDB
```