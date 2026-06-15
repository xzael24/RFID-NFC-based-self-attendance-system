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
}
