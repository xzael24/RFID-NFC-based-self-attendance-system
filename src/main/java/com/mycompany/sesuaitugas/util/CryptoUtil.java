package com.mycompany.sesuaitugas.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitas enkripsi/dekripsi 2 arah menggunakan AES-256-GCM.
 *
 * <p>
 * Digunakan untuk data yang perlu ditampilkan kembali ke pengguna,
 * khususnya <b>alamat email mahasiswa</b>. Email disimpan terenkripsi
 * di MongoDB sehingga tidak terbaca langsung jika database bocor,
 * namun admin tetap dapat melihat email asli di dashboard.
 * </p>
 *
 * <p>
 * <b>Algoritma:</b> AES-256 mode GCM (authenticated encryption) —
 * memberikan kerahasiaan sekaligus integritas data. IV (initialization
 * vector) 12-byte acak di-generate tiap enkripsi dan disimpan bersama
 * ciphertext.
 * </p>
 *
 * <p>
 * <b>Format ciphertext:</b> {@code BASE64_IV:BASE64_CIPHERTEXT}
 * </p>
 *
 * <p><b>Contoh penggunaan:</b></p>
 * <pre>
 * String enc = CryptoUtil.encrypt("budi@gmail.com");
 * String dec = CryptoUtil.decrypt(enc); // "budi@gmail.com"
 * </pre>
 *
 * <p>
 * <b>Catatan keamanan:</b> Secret key diturunkan dari passphrase + salt
 * menggunakan PBKDF2. Pada produksi, passphrase sebaiknya diambil dari
 * environment variable, bukan di-hardcode.
 * </p>
 */
public final class CryptoUtil {

    // ─── Konfigurasi AES-GCM ─────────────────────────────────────────────────
    private static final String CIPHER_ALGO   = "AES/GCM/NoPadding";
    private static final String KEY_ALGO      = "AES";
    private static final String KDF_ALGO      = "PBKDF2WithHmacSHA256";
    private static final int    GCM_TAG_BITS  = 128;
    private static final int    IV_LENGTH     = 12;   // 96-bit IV untuk GCM
    private static final int    KEY_LENGTH    = 256;  // AES-256
    private static final int    ITERATIONS    = 65_536;
    private static final String SEPARATOR     = ":";

    /**
     * Passphrase untuk derivasi kunci.
     * TODO: Pada deployment produksi, ganti dengan System.getenv("CRYPTO_SECRET")
     */
    private static final String PASSPHRASE =
            "PemKom2-SesuaiTugas-AES-Secret-Key-2026";

    /** Salt tetap untuk derivasi kunci (bukan salt enkripsi — IV sudah acak tiap kali). */
    private static final byte[] KEY_SALT =
            "SesuaiTugasSalt!".getBytes(StandardCharsets.UTF_8);

    /** Singleton secret key — dihitung sekali saat pertama digunakan. */
    private static SecretKey secretKey;

    /** Cegah instantiasi. */
    private CryptoUtil() {
    }

    // ─── Key Derivation ───────────────────────────────────────────────────────

    /**
     * Mendapatkan secret key AES-256 yang diturunkan dari {@link #PASSPHRASE}.
     * Menggunakan lazy initialization agar biaya PBKDF2 hanya dibayar sekali.
     */
    private static synchronized SecretKey getSecretKey() {
        if (secretKey == null) {
            try {
                PBEKeySpec spec = new PBEKeySpec(
                        PASSPHRASE.toCharArray(), KEY_SALT, ITERATIONS, KEY_LENGTH);
                SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
                byte[] keyBytes = factory.generateSecret(spec).getEncoded();
                secretKey = new SecretKeySpec(keyBytes, KEY_ALGO);
                spec.clearPassword();
            } catch (Exception e) {
                throw new RuntimeException("Gagal membuat secret key: " + e.getMessage(), e);
            }
        }
        return secretKey;
    }

    // ─── Enkripsi ─────────────────────────────────────────────────────────────

    /**
     * Mengenkripsi {@code plaintext} menggunakan AES-256-GCM.
     *
     * @param plaintext teks asli yang akan dienkripsi (mis. email)
     * @return string format {@code BASE64_IV:BASE64_CIPHERTEXT},
     *         atau {@code null} jika input null/kosong
     * @throws RuntimeException jika enkripsi gagal
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            // Generate IV acak 12 byte
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Enkripsi
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Gabungkan IV + ciphertext dalam format Base64
            String ivB64     = Base64.getEncoder().encodeToString(iv);
            String cipherB64 = Base64.getEncoder().encodeToString(cipherBytes);
            return ivB64 + SEPARATOR + cipherB64;

        } catch (Exception e) {
            throw new RuntimeException("Enkripsi gagal: " + e.getMessage(), e);
        }
    }

    // ─── Dekripsi ─────────────────────────────────────────────────────────────

    /**
     * Mendekripsi {@code ciphertext} yang dihasilkan oleh {@link #encrypt(String)}.
     *
     * @param ciphertext string terenkripsi format {@code BASE64_IV:BASE64_CIPHERTEXT}
     * @return teks asli, atau {@code null} jika input null/kosong
     * @throws RuntimeException jika dekripsi gagal (data rusak / kunci salah)
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return null;
        }
        // Jika belum terenkripsi (data lama plaintext), kembalikan apa adanya
        if (!isEncrypted(ciphertext)) {
            return ciphertext;
        }
        try {
            String[] parts = ciphertext.split(SEPARATOR, 2);
            byte[] iv          = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Dekripsi gagal: " + e.getMessage(), e);
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Mendeteksi apakah sebuah string sudah dalam format terenkripsi
     * ({@code BASE64_IV:BASE64_CIPHERTEXT}).
     *
     * <p>
     * Berguna untuk backward compatibility — data email lama yang masih
     * plaintext tidak akan dicoba didekripsi.
     * </p>
     *
     * @param value string yang akan dicek
     * @return {@code true} jika sudah terenkripsi, {@code false} jika plaintext
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String[] parts = value.split(SEPARATOR, 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            byte[] ivBytes = Base64.getDecoder().decode(parts[0]);
            Base64.getDecoder().decode(parts[1]);
            // IV GCM harus 12 byte
            return ivBytes.length == IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RSA Asymmetric Encryption — Digital Signature untuk Absensi
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String RSA_SIGN_ALGO = "SHA256withRSA";
    private static final String RSA_CIPHER = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int RSA_KEY_SIZE = 2048;

    /** Generate RSA-2048 keypair. */
    public static java.security.KeyPair generateRsaKeyPair() {
        try {
            java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("RSA");
            gen.initialize(RSA_KEY_SIZE, new SecureRandom());
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Gagal generate RSA keypair: " + e.getMessage(), e);
        }
    }

    /** Tanda tangani data dengan private key RSA (SHA256withRSA). */
    public static String rsaSign(String data, java.security.PrivateKey privKey) {
        try {
            java.security.Signature sig = java.security.Signature.getInstance(RSA_SIGN_ALGO);
            sig.initSign(privKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            throw new RuntimeException("Gagal RSA sign: " + e.getMessage(), e);
        }
    }

    /** Verifikasi signature digital dengan public key RSA. */
    public static boolean rsaVerify(String data, String signatureB64, java.security.PublicKey pubKey) {
        try {
            java.security.Signature sig = java.security.Signature.getInstance(RSA_SIGN_ALGO);
            sig.initVerify(pubKey);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signatureB64));
        } catch (Exception e) {
            return false;
        }
    }

    /** Serialisasi PublicKey atau PrivateKey ke Base64 (X.509 / PKCS8). */
    public static String keyToString(java.security.Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /** Deserialisasi PublicKey dari Base64 (X.509). */
    public static java.security.PublicKey stringToPublicKey(String b64) {
        try {
            byte[] encoded = Base64.getDecoder().decode(b64);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            return kf.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new RuntimeException("Gagal parse public key: " + e.getMessage(), e);
        }
    }

    /** Deserialisasi PrivateKey dari Base64 (PKCS8). */
    public static java.security.PrivateKey stringToPrivateKey(String b64) {
        try {
            byte[] encoded = Base64.getDecoder().decode(b64);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(encoded));
        } catch (Exception e) {
            throw new RuntimeException("Gagal parse private key: " + e.getMessage(), e);
        }
    }

    /**
     * Enkripsi private key dengan password menggunakan AES-256-GCM (PBKDF2 derived key).
     * Format output: base64(salt):base64(iv):base64(encrypted_private_key)
     */
    public static String encryptPrivateKey(java.security.PrivateKey privKey, String password) {
        try {
            byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
            byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv);
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance(KDF_ALGO);
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            javax.crypto.SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(keyBytes, KEY_ALGO);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CIPHER_ALGO);
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encPriv = cipher.doFinal(privKey.getEncoded());
            return Base64.getEncoder().encodeToString(salt) + SEPARATOR
                 + Base64.getEncoder().encodeToString(iv) + SEPARATOR
                 + Base64.getEncoder().encodeToString(encPriv);
        } catch (Exception e) {
            throw new RuntimeException("Gagal encrypt private key: " + e.getMessage(), e);
        }
    }

    /**
     * Dekripsi private key yang dienkripsi dengan {@link #encryptPrivateKey}.
     * Format input: base64(salt):base64(iv):base64(cipher)
     */
    public static java.security.PrivateKey decryptPrivateKey(String encryptedB64, String password) {
        try {
            String[] parts = encryptedB64.split(SEPARATOR, 3);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encPriv = Base64.getDecoder().decode(parts[2]);
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance(KDF_ALGO);
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            javax.crypto.SecretKey aesKey = new javax.crypto.spec.SecretKeySpec(keyBytes, KEY_ALGO);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(CIPHER_ALGO);
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, new javax.crypto.spec.GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] privBytes = cipher.doFinal(encPriv);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            return kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(privBytes));
        } catch (Exception e) {
            throw new RuntimeException("Gagal decrypt private key (password salah?): " + e.getMessage(), e);
        }
    }
}
