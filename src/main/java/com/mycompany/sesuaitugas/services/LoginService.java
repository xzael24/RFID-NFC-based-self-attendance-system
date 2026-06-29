package com.mycompany.sesuaitugas.services;

import com.mycompany.sesuaitugas.dao.GenericDAO;
import com.mycompany.sesuaitugas.objects.DaftarJurusan;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.objects.User;
import com.mycompany.sesuaitugas.util.HashUtil;
import com.mongodb.client.model.Filters;
import java.util.List;

/**
 * Service layer untuk logika autentikasi.
 *
 * <p>
 * <b>Keamanan password:</b>
 * <ul>
 *   <li>Password disimpan sebagai hash SHA-256 + salt (1 arah).</li>
 *   <li>Verifikasi login menggunakan {@link HashUtil#verify} — password
 *       plaintext dari form di-hash ulang lalu dibandingkan dengan
 *       hash tersimpan.</li>
 *   <li>Password plaintext <b>tidak pernah</b> disimpan ke database.</li>
 * </ul>
 * </p>
 *
 * <ul>
 *   <li>Admin  → disimpan dan diautentikasi dari koleksi {@code admin}</li>
 *   <li>Mahasiswa → disimpan di koleksi {@code mahasiswa}</li>
 * </ul>
 */
public class LoginService {

    private final GenericDAO<User> adminDAO;
    private final MahasiswaService mahasiswaService;

    /** Password default untuk semua user mahasiswa (bukan admin). */
    private static final String DEFAULT_USER_PASSWORD = "student123";

    /** Password default admin. */
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    public LoginService() {
        this.adminDAO = new GenericDAO<>("admin", User.class);
        this.mahasiswaService = new MahasiswaService();
    }

    // ─── Autentikasi Admin ────────────────────────────────────────────────────

    /**
     * Memvalidasi email dan password admin.
     * Password di-compare menggunakan {@link HashUtil#verify} (hash 1 arah).
     *
     * @param email    email yang diinput user
     * @param password password plaintext yang diinput user
     * @return objek {@link User} jika valid, {@code null} jika gagal
     */
    public User authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return null;
        }

        User user = adminDAO.findOne(Filters.eq("email", email.trim()));
        if (user == null) {
            return null;
        }

        String stored = user.getPassword();
        if (stored == null) {
            return null;
        }

        // Cek apakah password sudah di-hash atau masih plaintext (data lama)
        boolean match;
        if (HashUtil.isHashed(stored)) {
            // Password sudah di-hash — verifikasi dengan HashUtil
            match = HashUtil.verify(password, stored);
        } else {
            // Data lama belum di-hash — bandingkan langsung, lalu migrasikan ke hash
            match = stored.equals(password);
            if (match) {
                // Migrasi: simpan ulang dengan hash
                user.setPassword(HashUtil.hash(password));
                adminDAO.update(Filters.eq("email", email.trim()), user);
            }
        }

        return match ? user : null;
    }

    // ─── Autentikasi Mahasiswa ────────────────────────────────────────────────

    /**
     * Autentikasi mahasiswa dari koleksi {@code mahasiswa} menggunakan
     * NIM atau Email + password.
     * Password di-compare menggunakan {@link HashUtil#verify} (hash 1 arah).
     *
     * @param nimOrEmail NIM atau email yang diinput
     * @param password   password plaintext yang diinput
     * @return objek {@link Mahasiswa} jika valid, {@code null} jika gagal
     */
    public Mahasiswa authenticateMahasiswa(String nimOrEmail, String password) {
        if (nimOrEmail == null || nimOrEmail.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return null;
        }

        String input = nimOrEmail.trim();

        // 1) Coba cari berdasarkan NIM
        Mahasiswa mhs = mahasiswaService.findByNim(input);

        // 2) Jika tidak ditemukan, coba cari berdasarkan Email
        if (mhs == null) {
            mhs = mahasiswaService.findByEmail(input);
        }

        if (mhs == null) {
            return null;
        }

        // Verifikasi password menggunakan MahasiswaService
        // (yang menangani hash / plaintext lama secara transparan)
        boolean match = mahasiswaService.verifyPassword(password, mhs.getPassword());
        if (match && mhs.getRsaEncryptedPrivateKey() != null && !mhs.getRsaEncryptedPrivateKey().isEmpty()) {
            try {
                java.security.PrivateKey privKey =
                        com.mycompany.sesuaitugas.util.CryptoUtil.decryptPrivateKey(
                                mhs.getRsaEncryptedPrivateKey(), password);
                mhs.setTransientPrivateKey(privKey);
            } catch (Exception e) {
                System.err.println("RSA decrypt gagal untuk " + mhs.getNim() + ": " + e.getMessage());
            }
        }
        return match ? mhs : null;
    }

    // ─── Seed Data Default ────────────────────────────────────────────────────

    /**
     * Membuat user default admin jika koleksi "admin" masih kosong.
     * Password admin disimpan sebagai hash SHA-256.
     * Email: admin@kampus.id  |  Password: admin123
     */
    public void ensureDefaultAdmin() {
        if (adminDAO.findAll().isEmpty()) {
            User admin = new User("admin@kampus.id", HashUtil.hash(DEFAULT_ADMIN_PASSWORD), "admin");
            admin.setNama("Administrator");
            adminDAO.save(admin);
            System.out.println("Default admin dibuat: admin@kampus.id / " + DEFAULT_ADMIN_PASSWORD
                    + " (password tersimpan sebagai hash SHA-256)");
        }
    }

    /**
     * Jika belum ada satupun mahasiswa di koleksi {@code mahasiswa},
     * buat beberapa data demo.
     * Password mahasiswa disimpan sebagai hash SHA-256.
     */
    public void ensureDefaultMahasiswa() {
        List<Mahasiswa> existing = mahasiswaService.findAll();

        if (existing.isEmpty()) {
            String[][] defaultStudents = {
                {"DEMO001", "Mahasiswa 1"},
                {"DEMO002", "Mahasiswa 2"},
                {"DEMO003", "Mahasiswa 3"}
            };

            for (int i = 0; i < defaultStudents.length; i++) {
                String jurusan = DaftarJurusan.PILIHAN[i % DaftarJurusan.PILIHAN.length];
                Mahasiswa m = new Mahasiswa(
                        defaultStudents[i][0],  // nim
                        "",                     // idMahasiswa (auto-generated oleh service)
                        defaultStudents[i][1],  // nama
                        jurusan,
                        DEFAULT_USER_PASSWORD   // password plaintext — akan di-hash oleh save()
                );
                mahasiswaService.save(m);
                System.out.println("Default mahasiswa dibuat: NIM " + defaultStudents[i][0]
                        + " - " + defaultStudents[i][1] + " (" + jurusan + ")"
                        + " [password di-hash SHA-256]");
            }
        }
    }
}
