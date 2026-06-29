package com.mycompany.sesuaitugas.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitas hashing 1 arah menggunakan SHA-256.
 *
 * <p>Dua mode hashing tersedia:</p>
 * <ul>
 *   <li>{@link #hash(String)} — dengan salt acak (untuk password). Hasil berbeda
 *       tiap panggilan; verifikasi hanya lewat {@link #verify(String, String)}.</li>
 *   <li>{@link #hashDeterministic(String)} — tanpa salt (untuk UID RFID). Hasil
 *       selalu sama untuk input yang sama; bisa langsung dicari di DB.</li>
 * </ul>
 */
public final class HashUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final String SEPARATOR = ":";

    private HashUtil() {
    }

    // ─── Hash dengan salt (untuk password) ───────────────────────────────────

    /**
     * Menghasilkan hash SHA-256 dari {@code plaintext} dengan salt acak.
     * Format hasil: {@code BASE64_SALT:BASE64_HASH}
     *
     * @param plaintext teks asli yang akan di-hash
     * @return string hash, atau {@code null} jika input null/kosong
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

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
     * @param plaintext  teks asli dari form login
     * @param storedHash hash yang tersimpan di database (format {@code SALT:HASH})
     * @return {@code true} jika cocok
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || plaintext.isEmpty()
                || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            String[] parts = storedHash.split(SEPARATOR, 2);
            if (parts.length != 2) {
                return false;
            }
            byte[] salt         = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            byte[] actualHash = digest.digest(plaintext.getBytes("UTF-8"));

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mendeteksi apakah sebuah string sudah dalam format hash ({@code SALT:HASH}).
     * Berguna untuk backward-compatibility saat migrasi data plaintext lama.
     *
     * @param value string yang akan dicek
     * @return {@code true} jika sudah berbentuk hash
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
            // Hash SHA-256 = 32 byte = 44 karakter Base64
            return Base64.getDecoder().decode(parts[1]).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // ─── Hash deterministik tanpa salt (untuk UID RFID) ──────────────────────

    /**
     * Hash SHA-256 <b>deterministik tanpa salt</b> — khusus untuk UID kartu RFID.
     *
     * <p>Berbeda dengan {@link #hash(String)}, method ini menghasilkan hash yang
     * sama untuk input yang sama sehingga UID bisa langsung dicari di MongoDB
     * dengan membandingkan hash.</p>
     *
     * <p><b>Catatan keamanan:</b> Karena tanpa salt, rentan rainbow table jika
     * UID pendek. Untuk RFID ini masih dapat diterima — UID bukan password dan
     * serangan fisik ke kartu lebih relevan dari serangan DB.</p>
     *
     * @param plaintext UID mentah (sebaiknya normalized ke uppercase)
     * @return string hex 64 karakter (SHA-256), atau {@code null} jika input kosong
     */
    public static String hashDeterministic(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(plaintext.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Gagal hashing deterministik: " + e.getMessage(), e);
        }
    }
}
