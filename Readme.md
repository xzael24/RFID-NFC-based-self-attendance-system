# Sistem Manajemen Mahasiswa — RFID Attendance System

**Mata Kuliah:** Pemrograman Komputer 2  
**Semester:** 4  
**Tahun Akademik:** 2025/2026

---

## 5W + 1H Project

### What (Apa)
Aplikasi desktop berbasis Java Swing untuk manajemen data mahasiswa dan absensi berbasis kartu RFID/NFC. Mahasiswa dapat melakukan absensi dengan menempelkan kartu RFID ke reader Arduino, sementara admin dapat mengelola data mahasiswa, mendaftarkan kartu RFID, dan melihat riwayat absensi yang ditandatangani secara digital (RSA).

### Why (Kenapa?)
1. **Efisiensi** — Absensi manual (kertas/verbal) lambat, rawan kesalahan, dan sulit dilacak.
2. **Keamanan** — Data mahasiswa (password, email) dan rekam absensi perlu dilindungi dari kebocoran database.
3. **Otentikasi non-repudiation** — Absensi perlu ditandatangani digital agar mahasiswa tidak bisa menyangkal kehadirannya.
4. **Praktikum** — Menerapkan materi perkuliahan: DAO, JCA, enkripsi, multithreading, komunikasi serial, internasionalisasi.

### Who (Siapa?)
| Peran | Tindakan |
|---|---|
| **Mahasiswa** | Melakukan absensi — tap kartu RFID atau input NIM manual |
| **Admin** | Login via form → kelola CRUD mahasiswa, daftarkan RFID, lihat riwayat absensi |
| **Dosen** | Menilai implementasi materi pemrograman komputer 2 |

### Where (Di mana?)
- **Lingkungan**: Desktop (Windows) — Java Swing JFrame
- **Database**: MongoDB lokal (`localhost:27017`, database `DB_pemkom`)
- **Hardware**: Arduino + MFRC522 RFID reader via USB serial port (COM)
- **Repository**: Lokal Git

### When (Kapan?)
Aplikasi berjalan real-time. Digunakan saat:
- **Sebelum kuliah dimulai** — mahasiswa tap kartu untuk absen
- **Sesi admin** — pendaftaran kartu baru, manajemen data, review riwayat

### How (Bagaimana?)

#### Flow Aplikasi (Alur Lengkap)

```
START
  │
  ▼
┌──────────────────────────────────────────────┐
│         Halaman Mahasiswa (default)          │
│                                              │
│  ┌──────────────────────────────────┐       │
│  │  RFID: connect COM → tap kartu   │       │
│  │  atau: input NIM → klik Scan     │       │
│  └──────────────────────────────────┘       │
│  Hasil: info mahasiswa muncul +             │
│  record absensi masuk ke MongoDB +          │
│  signature RSA disimpan                     │
│                                              │
│  ┌───────────────────────────────┐          │
│  │  Tombol "Admin" (pojok kanan) │          │
│  └───────────┬───────────────────┘          │
└──────────────┼──────────────────────────────┘
               │ klik Admin
               ▼
┌───────────────────────────┐
│    Halaman LOGIN          │
│  - email (admin@kampus.id)│
│  - password (admin123)    │
│  - tombol bahasa ID/EN    │
│  - "Ingat saya" checkbox  │
└───────────┬───────────────┘
            │ sukses
            ▼
┌─────────────────────────────────────────────────────┐
│              ADMIN DASHBOARD                         │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ CRUD Mahasiswa:                               │  │
│  │ - Add: NIM, Nama, Jurusan, Password, Email    │  │
│  │ - Edit: klik Edit di kartu → ubah → Update    │  │
│  │ - Hapus: klik Hapus di kartu                  │  │
│  │ - Refresh: reload dari database                │  │
│  │ - Search: filter realtime (NIM/ID/Nama/Jurusan)│  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ RFID Registration:                             │  │
│  │  1. Pilih COM port                            │  │
│  │  2. Hubungkan → tap kartu → UID muncul        │  │
│  │  3. Masukkan NIM mahasiswa                   │  │
│  │  4. Klik "Daftarkan Kartu"                    │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │ Riwayat Absensi:                               │  │
│  │  Klik "Riwayat" di kartu mahasiswa →          │  │
│  │  lihat tabel absensi + verifikasi signature    │  │
│  │  (centang hijau = sah, silang = invalid)      │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## Teknologi & Tools

| Komponen | Spesifikasi |
|---|---|
| **Bahasa** | Java (target 1.8, kompilasi JDK 26) |
| **GUI Framework** | Swing (NetBeans GUI Builder — `.form` files) |
| **Database** | MongoDB 7.x (lokal, port 27017) |
| **DB Driver** | MongoDB Java Driver Sync 5.0.0 (POJO Codec) |
| **Build System** | Maven |
| **Serial Comms** | jSerialComm 2.11.4 |
| **Layout** | NetBeans AbsoluteLayout |
| **Logging** | SLF4J NOP (no-op, suppressed) |

### Dependencies (pom.xml)

```xml
<!-- MongoDB Driver -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.0.0</version>
</dependency>

<!-- NetBeans Absolute Layout -->
<dependency>
    <groupId>org.netbeans.external</groupId>
    <artifactId>AbsoluteLayout</artifactId>
    <version>RELEASE270</version>
</dependency>

<!-- Silent logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-nop</artifactId>
    <version>1.7.36</version>
</dependency>

<!-- Serial komunikasi Arduino RFID -->
<dependency>
    <groupId>com.fazecast</groupId>
    <artifactId>jSerialComm</artifactId>
    <version>2.11.4</version>
</dependency>
```

---

## Struktur Project (Lengkap)

```
sesuaiTugas/
├── pom.xml                          # Maven build configuration
├── README.md                        # Dokumentasi ini
├── .gitignore                       # ignore /target/
├── .vscode/
│   ├── launch.json                    # VS Code launch config
│   └── settings.json                # VS Code build settings
├── nb-configuration.xml             # NetBeans IDE config
└── src/
    └── main/
        ├── java/com/mycompany/sesuaitugas/
        │   ├── SesuaiTugas.java          # ENTRY POINT
        │   ├── dao/                        # Data Access Object Layer
        │   │   ├── Identifiable.java      # Interface: kontrak ID
        │   │   ├── BaseDAO.java           # Interface: CRUD generik
        │   │   └── GenericDAO.java        # Implementasi: MongoDB CRUD
        │   ├── objects/                   # Entity / Model Layer (POJO)
        │   │   ├── Mahasiswa.java         # Model mahasiswa
        │   │   ├── User.java             # Model user (admin)
        │   │   ├── Absensi.java          # Model record absensi
        │   │   └── DaftarJurusan.java    # Konstanta jurusan
        │   ├── services/                  # Service Layer (business logic)
        │   │   ├── LoginService.java     # Autentikasi admin & mahasiswa
        │   │   ├── MahasiswaService.java # CRUD + enkripsi/dekripsi
        │   │   └── AbsensiService.java   # Proses RFID & signature
        │   ├── gui/                       # GUI Layer (Swing + .form)
        │   │   ├── Login.java / .form    # Halaman login admin
        │   │   ├── Admin.java / .form    # Dashboard admin
        │   │   └── Mahasiswa.java / .form# Halaman absensi mahasiswa
        │   ├── util/                      # Utility Layer
        │   │   ├── MongoManager.java     # Singleton koneksi MongoDB
        │   │   ├── HashUtil.java         # SHA-256 + salt (password)
        │   │   ├── CryptoUtil.java       # AES-256-GCM + RSA sign/verify
        │   │   ├── I18nManager.java       # Internationalization manager
        │   │   ├── RfidSerialListener.java# Komunikasi serial Arduino
        │   │   ├── TesKoneksi.java       # Test koneksi MongoDB
        │   │   └── SeedDummyData.java    # Seeder data dummy
        │   └── Icons/                     # Aset icon PNG
        └── resources/
            ├── messages_id.properties    # Bahasa Indonesia
            └── messages_en.properties     # English
```

---

## Database Structure (MongoDB — `DB_pemkom`)

### Collection: `admin`

| Field | Type | Keterangan |
|---|---|---|
| `_id` / `email` | String | Primary key — email admin |
| `password` | String | SHA-256 + salt hash (format: `base64_salt:base64_hash`) |
| `role` | String | `"admin"` |
| `nama` | String | Nama tampilan |
| `nim` | String | NIM (opsional untuk admin) |
| `jurusan` | String | Jurusan (opsional) |
| `rsaPublicKey` | String | RSA public key (Base64 X.509) |
| `rsaEncryptedPrivateKey` | String | RSA private key terenkripsi AES (Base64) |

### Collection: `mahasiswa`

| Field | Type | Keterangan |
|---|---|---|
| `_id` / `idMahasiswa` | String | Primary key — auto-increment numeric |
| `nim` | String | Nomor Induk Mahasiswa (unique) |
| `nama` | String | Nama lengkap |
| `jurusan` | String | Program studi |
| `password` | String | SHA-256 + salt hash |
| `email` | String | **AES-256-GCM encrypted** (tidak plaintext) |
| `rfidHash` | String | SHA-256 hash dari UID kartu RFID (deterministik) |
| `rsaPublicKey` | String | RSA public key (Base64 X.509) |
| `rsaEncryptedPrivateKey` | String | RSA private key dienkripsi AES dengan password user |

### Collection: `absensi`

| Field | Type | Keterangan |
|---|---|---|
| `_id` / `idAbsensi` | String | ObjectId hex |
| `nim` | String | NIM yang absen |
| `nama` | String | Nama (denormalisasi untuk query cepat) |
| `jurusan` | String | Jurusan |
| `tanggal` | String | `yyyy-MM-dd` |
| `waktu` | String | `HH:mm:ss` |
| `status` | String | `"HADIR"` |
| `timestamp` | Long | Unix millis |
| `signature` | String | RSA digital signature (Base64) — `SHA256withRSA` |

---

## Implementasi 6 Materi Kuliah

### 1. Generic Programming: DAO (Data Access Object)

#### Konsep
DAO adalah pola desain yang memisahkan logika akses data dari business logic. Dengan menggunakan Java Generics (`<T>`), kita membuat satu implementasi DAO yang reusable untuk semua entitas (Mahasiswa, User, Absensi).

#### Kode & Penjelasan

**`Identifiable.java`** — Interface kontrak yang memastikan setiap entitas punya ID:
```java
public interface Identifiable {
    String getId();  // Setiap entitas harus punya ID unik
}
```

**`BaseDAO.java`** — Interface CRUD generik dengan type parameter `<T>`:
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
- `T extends Identifiable` adalah **bounded type parameter** — hanya entitas yang implements `Identifiable` yang bisa dipakai.
- Method `findOne`, `findMany` menggunakan `Bson filter` — filter dari MongoDB Java Driver.

**`GenericDAO.java`** — Implementasi konkrit untuk MongoDB:
```java
public class GenericDAO<T extends Identifiable> implements BaseDAO<T> {
    private final MongoCollection<T> collection;

    public GenericDAO(String collectionName, Class<T> clazz) {
        // Menggunakan POJO Codec — mapping otomatis Java ↔ BSON
        this.collection = MongoManager.getDatabase()
            .getCollection(collectionName, clazz);
    }

    @Override
    public void save(T entity) {
        collection.insertOne(entity);  // Insert ke MongoDB
    }

    @Override
    public List<T> findAll() {
        return collection.find().into(new ArrayList<>());
    }
    // ... method CRUD lainnya
}
```

#### Reusability
Dengan satu class `GenericDAO`, kita bisa mengelola 3 koleksi berbeda:
```java
GenericDAO<Mahasiswa> mahasiswaDAO = new GenericDAO<>("mahasiswa", Mahasiswa.class);
GenericDAO<User>      adminDAO     = new GenericDAO<>("admin", User.class);
GenericDAO<Absensi>   absensiDAO   = new GenericDAO<>("absensi", Absensi.class);
```

#### Penerapan di Project
- `MahasiswaService.java:25-28` — `GenericDAO<Mahasiswa>` untuk CRUD mahasiswa
- `LoginService.java:32-33` — `GenericDAO<User>` untuk autentikasi admin
- `AbsensiService.java:34-35` — `GenericDAO<Absensi>` untuk absensi

---

### 2. JCA & Hashing SHA

#### Konsep
**Java Cryptography Architecture (JCA)** adalah framework Java untuk kriptografi. `MessageDigest` adalah kelas JCA untuk hashing satu arah. SHA-256 menghasilkan hash 256-bit (32 byte) yang:
- **Satu arah** — tidak bisa dikembalikan ke teks asli
- **Deterministik** — input sama → hash sama
- **Avalanche effect** — perubahan sedikit pada input mengubah hash drastis

#### HashUtil.java — Detail Implementasi

**Mode 1: Salted Hash (untuk password)**
```java
public static String hash(String plaintext) {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[16];
    random.nextBytes(salt);                    // Salt acak 16 byte

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(salt);                       // Tambah salt sebelum hash
    byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

    // Format: BASE64_SALT:BASE64_HASH
    return Base64.getEncoder().encodeToString(salt)
         + ":" + Base64.getEncoder().encodeToString(hashBytes);
}
```
- Salt acak tiap panggilan → hash berbeda untuk input yang sama
- Menangkal **rainbow table** dan **brute-force parallel**

**Verifikasi:**
```java
public static boolean verify(String plaintext, String storedHash) {
    String[] parts = storedHash.split(":", 2);
    byte[] salt = Base64.getDecoder().decode(parts[0]);
    byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(salt);
    byte[] actualHash = digest.digest(plaintext.getBytes("UTF-8"));

    return MessageDigest.isEqual(expectedHash, actualHash); // Constant-time
}
```

**Mode 2: Deterministic Hash (untuk RFID UID)**
```java
public static String hashDeterministic(String plaintext) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

    // Output string hex 64 karakter (tanpa salt)
    StringBuilder sb = new StringBuilder();
    for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
}
```
- Tanpa salt → input sama selalu menghasilkan hash yang sama
- Bisa langsung dijadikan filter query MongoDB

#### Penerapan di Project
| File | Baris | Fungsi |
|---|---|---|
| `MahasiswaService.java` | 148-152 | Hash password sebelum save |
| `LoginService.java` | 74-83 | Verifikasi password saat login |
| `AbsensiService.java` | 67 | Hash deterministik UID RFID untuk pencarian |
| `HashUtil.java` | 96-111 | Method `isHashed()` untuk backward compat |

---

### 3. Asymmetric & Symmetric Encryption

#### Konsep
- **Symmetric Encryption** (AES-256-GCM): Kunci yang sama untuk enkripsi dan dekripsi. Cepat, cocok untuk data dalam jumlah besar.
- **Asymmetric Encryption** (RSA-2048): Sepasang kunci — public key (bisa dibagikan) dan private key (rahasia). Cocok untuk digital signature.

#### CryptoUtil.java — Detail

**Symmetric: AES-256-GCM (untuk Email)**

```java
// Enkripsi
public static String encrypt(String plaintext) {
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);   // IV acak tiap enkripsi

    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(),
            new GCMParameterSpec(128, iv));
    byte[] cipherBytes = cipher.doFinal(plaintext.getBytes());

    return b64(iv) + ":" + b64(cipherBytes);
}

// Key derivation dengan PBKDF2 (Password-Based Key Derivation)
private static SecretKey getSecretKey() {
    PBEKeySpec spec = new PBEKeySpec(
        PASSPHRASE.toCharArray(), KEY_SALT, 65536, 256);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    byte[] keyBytes = factory.generateSecret(spec).getEncoded();
    return new SecretKeySpec(keyBytes, "AES");
}
```

Kenapa AES-GCM?
- **Authenticated Encryption** — selain merahasiakan data, juga menjamin integritas (tidak bisa diubah tanpa terdeteksi)
- **GCM** (Galois/Counter Mode) menggunakan **IV 12-byte** dan menghasilkan **authentication tag** 128-bit
- **PBKDF2** untuk derivasi kunci — membuat serangan brute-force lebih mahal (65.536 iterasi)

**Asymmetric: RSA Digital Signature (untuk absensi)**

```java
// Sign — tanda tangani data dengan private key
public static String rsaSign(String data, PrivateKey privKey) {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initSign(privKey);
    sig.update(data.getBytes());
    return Base64.getEncoder().encodeToString(sig.sign());
}

// Verify — verifikasi signature dengan public key
public static boolean rsaVerify(String data, String signatureB64, PublicKey pubKey) {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initVerify(pubKey);
    sig.update(data.getBytes());
    return sig.verify(Base64.getDecoder().decode(signatureB64));
}

// Generate RSA 2048-bit key pair
public static KeyPair generateRsaKeyPair() {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048, new SecureRandom());
    return gen.generateKeyPair();
}
```

**Private Key Protection:**
Private key dienkripsi dengan AES-256-GCM (key derived dari password user via PBKDF2):
```java
public static String encryptPrivateKey(PrivateKey privKey, String password) {
    byte[] salt = new byte[16]; SecureRandom().nextBytes(salt);
    byte[] iv = new byte[12]; SecureRandom().nextBytes(iv);
    // Derive key dari password
    SecretKey aesKey = deriveKey(password, salt);
    // Enkripsi private key dengan AES-GCM
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
    byte[] encPriv = cipher.doFinal(privKey.getEncoded());
    return b64(salt) + ":" + b64(iv) + ":" + b64(encPriv);
}
```

#### Alur Keamanan Data di Project

```
SAVE Mahasiswa:
  Password → SHA-256 + salt (1 arah) → DB
  Email → AES-256-GCM (2 arah) → DB
  RSA Keypair → Public disimpan plain, Private dienkripsi AES dengan password

LOGIN Mahasiswa:
  Input password → verifikasi dengan SHA-256 hash di DB
  Jika cocok → decrypt private key RSA dengan password → simpan in-memory (transient)

ABSENSI (tap kartu):
  Data: nim|tanggal|waktu
  → RSA sign dengan private key (in-memory session)
  → Store signature di record absensi
  
VERIFIKASI ABSENSI (admin):
  Ambil public key mahasiswa dari DB
  → RSA verify(signature, data, publicKey)
  → Tampilkan ✓ Sah atau ✗ Invalid
```

#### Penerapan di Project
| File | Baris | Fungsi |
|---|---|---|
| `CryptoUtil.java` | 108-131 | AES encrypt email |
| `CryptoUtil.java` | 142-164 | AES decrypt email |
| `CryptoUtil.java` | 207-215 | Generate RSA keypair |
| `CryptoUtil.java` | 218-227 | RSA sign |
| `CryptoUtil.java` | 230-239 | RSA verify |
| `CryptoUtil.java` | 272-291 | AES encrypt private key |
| `CryptoUtil.java` | 297-317 | AES decrypt private key |
| `LoginService.java` | 124-133 | Decrypt private key saat login |
| `AbsensiService.java` | 103-113 | RSA sign saat absensi |
| `Admin.java` | 476-479 | RSA verify di riwayat absensi |

---

### 4. Concurrency & Multithreading

#### Konsep
Concurrency memungkinkan aplikasi menjalankan beberapa tugas secara bersamaan. Di Java:
- **Thread** — unit eksekusi ringan
- **`SwingWorker<T, V>`** — thread khusus untuk background task di Swing (aman untuk UI)
- **`SwingUtilities.invokeLater()`** — mengupdate UI dari thread lain
- **`javax.swing.Timer`** — tugas berulang di Event Dispatch Thread (EDT)

#### Kenapa Penting?
- **GUI tidak membeku** — operasi berat (serial read, database, hashing) dilakukan di background
- **Responsif** — user tetap bisa berinteraksi selama proses berjalan

#### Implementasi di Project

**1. `SwingWorker` — Background RFID Scan**
Di `Mahasiswa.java:152-163`:
```java
private void prosesUidDariSerial(String uid) {
    showStatus("Memproses kartu: " + uid + " ...", ...);
    new SwingWorker<HasilScan, Void>() {
        @Override
        protected HasilScan doInBackground() {
            // ⚡ INI JALAN DI BACKGROUND THREAD
            // Proses hash, query MongoDB, RSA sign
            return absensiService.prosesUid(uid, privateKey);
        }

        @Override
        protected void done() {
            // ✅ INI JALAN DI EDT (aman update UI)
            HasilScan hasil = get();
            tampilkanHasilScan(hasil);
        }
    }.execute(); // Mulai background thread
}
```

**2. `SwingWorker` — Manual NIM input**
Di `Mahasiswa.java:171-193` — sama, query DB dan RSA sign di background.

**3. `SwingWorker` — Registrasi RFID oleh Admin**
Di `Admin.java:262-281` — daftarkan kartu ke DB di background.

**4. `SwingUtilities.invokeLater()` — Serial Data Callback**
Di `Admin.java:234`:
```java
rfidListener = new RfidSerialListener(port,
    uid -> SwingUtilities.invokeLater(() -> {
        // Data dari serial thread → update UI di EDT
        txtUidResult.setText(uid);
    }));
```

**5. `javax.swing.Timer` — Update Jam Real-time**
Di `Mahasiswa.java:77-84`:
```java
private void setupClock() {
    updateClockLabels();
    new Timer(1000, e -> updateClockLabels()).start(); // Tiap 1 detik
}
```

**6. Serial Port Thread — jSerialComm**
`RfidSerialListener.java` menggunakan listener event `SerialPortDataListener` — event `LISTENING_EVENT_DATA_AVAILABLE` dipanggil di thread internal jSerialComm. Kita buffer data sampai menemukan newline (`\n`), lalu panggil callback.

#### Diagram Multithreading

```
┌─────────────────────────────────────────────────────┐
│             Event Dispatch Thread (EDT)             │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ Login.java  │  │ Admin.java   │  │Mahasiswa.java││
│  │ (Swing comp)│  │ (grid, form) │  │(jam, label) ││
│  └─────────────┘  └──────────────┘  └────────────┘│
│        ▲                ▲                 ▲         │
│        │ invokeLater    │ SwingWorker     │ Timer   │
│        ▼                ▼                 ▼         │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐│
│  │ Worker-1    │  │ Worker-2     │  │ Timer-1    ││
│  │ (serial    │  │ (RFID regis) │  │ (clock 1s) ││
│  │  → DB)     │  │  → DB)      │  │            ││
│  └─────────────┘  └──────────────┘  └────────────┘│
│           ▲                                          │
│           │ Data Event                                │
│           ▼                                          │
│  ┌──────────────────────────────────────────────────┐│
│  │        jSerialComm Thread Pool                   ││
│  │  (membaca data serial dari Arduino / COM port)  ││
│  └──────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

---

### 5. Integrasi Hardware & Komunikasi Serial

#### Konsep
Arduino MFRC522 RFID reader membaca UID kartu RFID (string hex 8-14 karakter) dan mengirimnya via serial USB ke PC. Java membaca data tersebut menggunakan library **jSerialComm**.

#### Protokol
```
Arduino (sketch MFRC522):
  ┌──────────────────────────────┐
  │ loop:                        │
  │   if kartu tertempel:        │
  │     uid = baca UID           │
  │     Serial.println(uid)      │
  │     delay(1000)              │
  └──────────────────────────────┘
              │ UART/USB
              ▼
Java (RfidSerialListener):
  ┌──────────────────────────────┐
  │ baca byte dari COM port     │
  │ buffer sampai ada '\n'     │
  │ callback(Uid)               │
  └──────────────────────────────┘
```

#### `RfidSerialListener.java` — Detail

```java
public class RfidSerialListener {
    private SerialPort port;
    private final StringBuilder buffer = new StringBuilder();

    public boolean start() {
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(9600);         // Sesuai sketch Arduino
        port.setNumDataBits(8);
        port.setNumStopBits(ONE_STOP_BIT);
        port.setParity(NO_PARITY);
        port.openPort();

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

                buffer.append(chunk);
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

    public void stop() {
        port.closePort();
    }
}
```

#### Flow Komunikasi Serial

```
Admin: Pilih COM → Hubungkan
  ↓
jSerialComm buka port (9600 baud, 8-N-1)
  ↓
Arduino kirim data tiap kali kartu ditempel
  ↓
Listener menerima data → buffer + parse per newline
  ↓
callback.onUidReceived("A3F2B1C4") ← di background thread
  ↓
SwingUtilities.invokeLater(() -> { update UI })
  ↓
Hash UID → cari di DB → buat absensi → save → sign RSA
```

#### Penerapan di Project
| File | Baris | Fungsi |
|---|---|---|
| `RfidSerialListener.java` | 94-145 | `start()` — buka port + mulai listen |
| `RfidSerialListener.java` | 150-156 | `stop()` — tutup port |
| `RfidSerialListener.java` | 170-183 | `getAvailablePorts()` — daftar COM port |
| `Mahasiswa.java` | 120-147 | `onToggleConnect()` — hubung/putus serial |
| `Admin.java` | 221-249 | `onToggleRfidRegistrasi()` — registrasi kartu |

---

### 6. Internationalization (i18n) & Localization (l10n)

#### Konsep
**i18n** = Internationalization: merancang aplikasi agar bisa diadaptasi ke berbagai bahasa/daerah.  
**l10n** = Localization: menerjemahkan teks UI ke bahasa tertentu.

Java menyediakan `ResourceBundle` — file `.properties` yang berisi pasangan `key=value`. Nama file mengikuti pola `messages_{locale}.properties`.

#### `I18nManager.java` — Detail

```java
public final class I18nManager {
    private static Locale currentLocale = new Locale("id", "ID");
    private static ResourceBundle bundle;

    // Load bundle sesuai locale
    private static void loadBundle() {
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    // Set locale baru dan reload
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadBundle();
    }

    // Ambil string berdasarkan key
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;  // Fallback: tampilkan key-nya
        }
    }
}
```

#### File Properties

**`messages_id.properties`** (Indonesia):
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
admin.buttonAdd=Tambah
admin.buttonEdit=Edit
admin.buttonDelete=Hapus
admin.buttonLogout=Logout
mahasiswa.title=Sistem Absensi RFID
mahasiswa.subtitle=Tap Kartu Anda untuk Absensi
mahasiswa.buttonScan=Scan
mahasiswa.buttonAdminLogin=Admin
mahasiswa.namaLabel=Nama
mahasiswa.nimLabel=NIM
# ... dan seterusnya
```

**`messages_en.properties`** (English):
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
admin.buttonAdd=Add
admin.buttonEdit=Edit
admin.buttonDelete=Delete
admin.buttonLogout=Logout
mahasiswa.title=RFID Attendance System
mahasiswa.subtitle=Tap Your Card to Attendance
mahasiswa.buttonScan=Scan
mahasiswa.buttonAdminLogin=Admin
mahasiswa.namaLabel=Name
mahasiswa.nimLabel=NIM
# ... dan seterusnya
```

#### Penerapan di GUI

**Login.java:**
```java
private void refreshUIText() {
    jLabel1.setText(I18nManager.get("login.title"));
    jLabel2.setText(I18nManager.get("login.subtitle"));
    jButton1.setText(I18nManager.get("login.buttonLogin"));
    // ...
}

// Tombol ID
private void btnLanguageIDActionPerformed(...) {
    I18nManager.setLocale(new Locale("id", "IN"));
    refreshUIText();
}

// Tombol EN
private void btnLanguageENActionPerformed(...) {
    I18nManager.setLocale(new Locale("en", "US"));
    refreshUIText();
}
```

Setiap layar (Login, Admin, Mahasiswa) memiliki tombol ID/EN dan method `refreshUIText()` sendiri.

---

## Struktur Arsitektur (Layered Architecture)

```
┌────────────────────────────────────────────────────────────┐
│                   GUI LAYER (Swing)                      │
│  Login.java      Admin.java      Mahasiswa.java          │
│  (JFrame)        (JPanel)        (JFrame)                │
├────────────────────────────────────────────────────────────┤
│                SERVICE LAYER (Business Logic)             │
│  LoginService     MahasiswaService     AbsensiService     │
│  - authenticate   - CRUD mahasiswa    - prosesUid()       │
│  - autentikasi    - encrypt/decrypt   - daftarkanRfid()   │
│    admin/mhs      - verify password   - RSA sign/verify   │
├────────────────────────────────────────────────────────────┤
│                  DAO LAYER (Data Access)                  │
│  BaseDAO<T> ───── GenericDAO<T> ──── MongoDB              │
│  (interface)      (implementasi)     (POJO Codec)          │
├────────────────────────────────────────────────────────────┤
│                  OBJECTS LAYER (POJO)                     │
│  Mahasiswa.java   User.java         Absensi.java           │
│  DaftarJurusan.java                                       │
├────────────────────────────────────────────────────────────┤
│                 UTILITY LAYER (Infrastructure)             │
│  MongoManager    HashUtil          CryptoUtil              │
│  I18nManager     RfidSerialListener                      │
│  TesKoneksi      SeedDummyData                             │
└────────────────────────────────────────────────────────────┘
```

### Alur Data Lengkap (Contoh: Tap RFID)

```
[Tap Kartu]
    │
    ▼
Arduino MFRC522 → baca UID (misal: "A3F2B1C4")
    │
    ▼ (via Serial USB, 9600 baud)
RfidSerialListener.java — callback.onUidReceived("A3F2B1C4")
    │
    ▼ (background thread serial)
SwingUtilities.invokeLater() → panggil prosesUidDariSerial()
    │
    ▼
SwingWorker.doInBackground() ← thread terpisah
    │
    ├─ HashUtil.hashDeterministic("A3F2B1C4") → "a1b2c3d4..."
    ├─ MahasiswaService.findByRfidHash(hash) → ambil Mahasiswa dari MongoDB
    ├─ Buat objek Absensi (NIM, nama, tanggal, waktu, "HADIR")
    ├─ CryptoUtil.rsaSign(nim|tanggal|waktu, privateKey) → signature
    ├─ GenericDAO<Absensi>.save(absensi) → simpan ke MongoDB
    │
    ▼ (kembali ke EDT via done())
SwingWorker.done() → tampilkan info mahasiswa + status "HADIR ✓ RSA"
```

---

## Cara Menjalankan

### Prasyarat
1. **MongoDB** — running di `localhost:27017`
   - Download & install MongoDB Community Server
   - Jalankan: `mongod` atau via MongoDB Compass
2. **Arduino** — upload sketch MFRC522 ke board Arduino
   - Sketch minimal ada di JavaDoc `RfidSerialListener.java:28-48`
3. **Java JDK 17+** — pastikan `JAVA_HOME` ter-set

### Build & Run
```bash
# Build
mvn clean package

# Jalankan
mvn exec:java

# Atau langsung .jar
java -jar target/sesuaiTugas-1.0-SNAPSHOT.jar
```

### Login Default
| Role | Username | Password |
|---|---|---|
| Admin | `admin@kampus.id` | `admin123` |
| Mahasiswa Demo 1 | NIM `DEMO001` | `student123` |
| Mahasiswa Demo 2 | NIM `DEMO002` | `student123` |
| Mahasiswa Demo 3 | NIM `DEMO003` | `student123` |

### Langkah-langkah Penggunaan

**1. Pertama Kali: Registrasi Kartu RFID**
1. Jalankan aplikasi → langsung ke halaman Mahasiswa
2. Klik tombol **"Admin"** pojok kanan atas
3. Login dengan `admin@kampus.id` / `admin123`
4. Di Admin Dashboard → pilih COM port Arduino di dropdown
5. Klik **Hubungkan** → tempelkan kartu RFID ke reader
6. UID muncul di field "UID Terakhir"
7. Masukkan NIM mahasiswa di "NIM Mahasiswa"
8. Klik **Daftarkan Kartu** → "Kartu berhasil didaftarkan"

**2. Absensi Harian**
1. Tutup Admin, balik ke Mahasiswa (atau restart app)
2. Pilih COM port yang sama → **Hubungkan**
3. Tempelkan kartu → info mahasiswa muncul + record absen tersimpan
4. Atau jika tanpa RFID: input NIM manual → klik **Scan**

**3. Admin — Review Absensi**
1. Login sebagai admin
2. Klik **Riwayat** di kartu mahasiswa mana pun
3. Lihat tabel absensi + status signature (✓ Sah / ✗ Invalid)

---

## Struktur Database — Detail Koleksi

### admin
```
{
  "_id": "admin@kampus.id",
  "password": "B8xR...Hsw=:2fVJ...9LJ=",   // salt:hash base64
  "role": "admin",
  "nama": "Administrator",
  "email": "admin@kampus.id"
}
```

### mahasiswa
```
{
  "_id": "1",
  "nim": "DEMO001",
  "nama": "Mahasiswa 1",
  "jurusan": "S1 Teknik Informatika",
  "password": "Kj2m...Fp0=:3aBc...5Xy=",   // SHA-256 + salt
  "email": "abc123...xyz==",                 // AES-256-GCM encrypted
  "rfidHash": "e3b0c44298fc1c14...",         // SHA-256 deterministic
  "rsaPublicKey": "MIIBIjANBgkqhkiG9w0B...",
  "rsaEncryptedPrivateKey": "salt:iv:cipher"
}
```

### absensi
```
{
  "_id": "67c43a1e2f4b6a8d0e1c2a3b",
  "nim": "DEMO001",
  "nama": "Mahasiswa 1",
  "jurusan": "S1 Teknik Informatika",
  "tanggal": "2026-07-04",
  "waktu": "08:03:21",
  "status": "HADIR",
  "timestamp": 1767423201000,
  "signature": "H2s0G5j8K...wMzXZQ=="      // RSA SHA256withRSA
}
```

---

## Class Diagram

```
┌─────────────────────┐     ┌──────────────────────────┐
│   <<interface>>     │     │  <<interface>>            │
│   Identifiable     │     │  BaseDAO<T>               │
│─────────────────────│     │──────────────────────────│
│ + getId(): String   │     │ + save(T)                 │
└────────┬────────────┘     │ + update(Bson, T)          │
         │ implements      │ + delete(Bson)              │
         ▼                 │ + findAll(): List<T>        │
┌───────────────────┐       │ + findOne(Bson): T         │
│ Mahasiswa        │       │ + findMany(Bson): List<T>   │
│ User             │       └──────────┬─────────────────┘
│ Absensi          │                  │ implements
└───────────────────┘                  ▼
                              ┌──────────────────────┐
                              │ GenericDAO<T>        │
                              │──────────────────────│
                              │ - collection: MongoCol│
                              │──────────────────────│
                              │ + save(T)             │
                              │ + update(Bson, T)     │
                              │ + updateFields(...)   │
                              │ + delete(Bson)        │
                              │ + findAll()           │
                              │ + findOne(Bson)       │
                              │ + findMany(Bson)      │
                              └──────────────────────┘

┌─────────────────────┐     ┌──────────────────────────┐
│ Mongomanager       │     │ HashUtil                 │
│─────────────────────│     │──────────────────────────│
│ - client: MongoCli │     │ + hash(String): String   │
│ - db: MongoDatabase│     │ + verify(...): boolean  │
│─────────────────────│     │ + hashDeterministic(...)│
│ + getDatabase():   │     │ + isHashed(...): boolean│
│   MongoDatabase    │     └──────────────────────────┘
│ + closeConnection()│
└─────────────────────┘     ┌──────────────────────────┐
                         │ CryptoUtil               │
┌─────────────────────┐  │──────────────────────────│
│ I18nManager        │  │ + encrypt(String)        │
│─────────────────────│  │ + decrypt(String)        │
│ + setLocale(Locale)│  │ + encryptPrivateKey(...) │
│ + get(key): String │  │ + decryptPrivateKey(...) │
│ + getLanguage()    │  │ + rsaSign(...)           │
└─────────────────────┘  │ + rsaVerify(...)         │
                         │ + generateRsaKeyPair()  │
┌─────────────────────┐  └──────────────────────────┘
│ RfidSerialListener  │
│─────────────────────│  ┌──────────────────────────┐
│ + start(): boolean  │  │ LoginService             │
│ + stop()            │  │ MahasiswaService        │
│ + getAvailablePorts │  │ AbsensiService          │
│ ── inner ──         │  └──────────────────────────┘
│ <<interface>>       │
│ UidCallback         │
└─────────────────────┘
```

---

## Anggota Tim

1. **Muhammad Zaim El Yafi**
2. **Gusti Rizqi Putra Hanif**
3. **Mohamad Naufal Arizal**

---

## Catatan Penting

1. **COM Port** — Kadang driver Arduino clone (CH340) perlu diinstall manual. Cek di Device Manager.
2. **jSerialComm Cache** — Jika error `Can't load ARM 64-bit .dll`, hapus folder `%TEMP%\jSerialComm` dan `%USERPROFILE%\.jSerialComm`.
3. **MongoDB** — Pastikan MongoDB service berjalan sebelum menjalankan aplikasi. Data seed otomatis dibuat saat pertama kali.
4. **Private Key** — RSA private key hanya ada di memory (transient) selama sesi login. Setelah logout, private key hilang.
5. **Flux Awal** — Aplikasi langsung ke halaman Mahasiswa (tanpa login) untuk kemudahan absensi RFID. Admin login via tombol kecil di pojok.