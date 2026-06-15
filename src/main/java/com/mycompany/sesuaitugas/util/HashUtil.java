package com.mycompany.sesuaitugas.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitas hashing 1 arah menggunakan SHA-256.
 *
 * <p>
 * Digunakan untuk:
 * <ul>
 *   <li>Password mahasiswa dan admin — tidak perlu pernah dibaca balik,
 *       cukup dibandingkan hash-nya saat login.</li>
 *   <li>UID kartu RFID — disimpan sebagai hash sehingga raw UID tidak bocor
 *       meskipun database diakses pihak lain.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Format hash:</b> {@code SALT:HASH} — salt 16-byte acak di-encode Base64,
 * dipisahkan titik dua dari SHA-256 hash (juga Base64). Salt memastikan dua
 * password yang sama menghasilkan hash yang berbeda (anti rainbow-table).
 * </p>
 *
 * <p><b>Contoh penggunaan:</b></p>
 * <pre>
 * String hashed = HashUtil.hash("student123");
 * boolean cocok = HashUtil.verify("student123", hashed); // true
 * </pre>
 */
public final class HashUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final String SEPARATOR = ":";

    /** Cegah instantiasi. */
    private HashUtil() {
    }

    /**
     * Menghasilkan hash SHA-256 dari {@code plaintext} dengan salt acak.
     *
     * @param plaintext teks asli yang akan di-hash (password / UID RFID)
     * @return string format {@code BASE64_SALT:BASE64_HASH}, atau {@code null}
     *         jika input null/kosong
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            // Buat salt acak 16 byte
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash plaintext + salt
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

            // Encode ke Base64 dan gabungkan
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hashBytes);
            return saltB64 + SEPARATOR + hashB64;

        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Gagal melakukan hashing: " + e.getMessage(), e);
        }
    }

    /**
     * Memverifikasi apakah {@code plaintext} cocok dengan {@code storedHash}.
     *
     * <p>
     * Mengekstrak salt dari {@code storedHash}, lalu meng-hash ulang
     * {@code plaintext} dengan salt yang sama dan membandingkan hasilnya.
     * </p>
     *
     * @param plaintext  teks asli yang diinput pengguna
     * @param storedHash hash yang tersimpan di database (format {@code SALT:HASH})
     * @return {@code true} jika cocok, {@code false} jika tidak cocok atau input
     *         tidak valid
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || plaintext.isEmpty()
                || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            // Pisahkan salt dan hash
            String[] parts = storedHash.split(SEPARATOR, 2);
            if (parts.length != 2) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            // Hash ulang plaintext dengan salt yang sama
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] actualHash = digest.digest(plaintext.getBytes("UTF-8"));

            // Bandingkan byte per byte (constant-time untuk cegah timing attack)
            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mendeteksi apakah sebuah string sudah dalam format hash ({@code SALT:HASH})
     * atau masih plaintext.
     *
     * <p>
     * Berguna saat migrasi data lama yang masih plaintext — agar tidak di-hash
     * dua kali.
     * </p>
     *
     * @param value string yang akan dicek
     * @return {@code true} jika sudah berbentuk hash, {@code false} jika plaintext
     */
    public static boolean isHashed(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String[] parts = value.split(SEPARATOR, 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            Base64.getDecoder().decode(parts[0]);
            Base64.getDecoder().decode(parts[1]);
            // Hash SHA-256 = 32 byte = 44 karakter Base64
            return Base64.getDecoder().decode(parts[1]).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
