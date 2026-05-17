package com.mycompany.sesuaitugas.services;

import com.mycompany.sesuaitugas.dao.GenericDAO;
import com.mycompany.sesuaitugas.objects.DaftarJurusan;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.objects.User;
import com.mongodb.client.model.Filters;
import java.util.List;

/**
 * Service layer untuk logika autentikasi.
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

    public LoginService() {
        this.adminDAO = new GenericDAO<>("admin", User.class);
        this.mahasiswaService = new MahasiswaService();
    }

    /**
     * Memvalidasi email dan password.
     * Mengecek koleksi admin terlebih dahulu.
     *
     * @param email    email yang diinput user
     * @param password password yang diinput user
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
        if (stored != null && stored.equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Autentikasi mahasiswa dari koleksi {@code mahasiswa} menggunakan
     * NIM atau Email + password.
     *
     * @param nimOrEmail NIM atau email yang diinput
     * @param password   password yang diinput
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

        String stored = mhs.getPassword();
        if (stored != null && stored.equals(password)) {
            return mhs;
        }
        return null;
    }

    /**
     * Membuat user default admin jika koleksi "admin" masih kosong.
     * Email: admin@university.edu  |  Password: admin123
     */
    public void ensureDefaultAdmin() {
        if (adminDAO.findAll().isEmpty()) {
            User admin = new User("admin@kampus.id", "admin123", "admin");
            admin.setNama("Administrator");
            adminDAO.save(admin);
            System.out.println("Default admin user telah dibuat: admin@kampus.id / admin123");
        }
    }

    /**
     * Jika belum ada satupun mahasiswa di koleksi {@code mahasiswa},
     * buat beberapa data demo.
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
                        DEFAULT_USER_PASSWORD   // password
                );
                mahasiswaService.save(m);
                System.out.println("Default mahasiswa dibuat: NIM " + defaultStudents[i][0]
                        + " - " + defaultStudents[i][1] + " (" + jurusan + ")");
            }
        }
    }
}
