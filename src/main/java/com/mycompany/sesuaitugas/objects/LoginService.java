package com.mycompany.sesuaitugas.objects;

import com.mongodb.client.model.Filters;
import java.util.List;

/**
 * Service layer untuk logika autentikasi.
 * Memvalidasi kredensial user terhadap koleksi "users" di MongoDB.
 */
public class LoginService {

    private final GenericDAO<User> userDAO;

    /** Password default untuk semua user (bukan admin). */
    private static final String DEFAULT_USER_PASSWORD = "student123";

    public LoginService() {
        this.userDAO = new GenericDAO<>("admin", User.class);
    }

    /**
     * Memvalidasi email dan password.
     * @param email email yang diinput user
     * @param password password yang diinput user
     * @return objek User jika valid, null jika gagal
     */
    public User authenticate(String email, String password) {
        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            System.out.println("[LOGIN DEBUG] Input kosong — email atau password null/empty.");
            return null;
        }

        System.out.println("[LOGIN DEBUG] Mencari user dengan email: '" + email.trim() + "'");

        // Cari user berdasarkan email
        User user = userDAO.findOne(Filters.eq("email", email.trim()));

        if (user == null) {
            System.out.println("[LOGIN DEBUG] User TIDAK ditemukan di database.");
            return null;
        }
        System.out.println("[LOGIN DEBUG] User ditemukan: " + user);
        String stored = user.getPassword();
        System.out.println("[LOGIN DEBUG] Password di DB : '" + stored + "'");
        System.out.println("[LOGIN DEBUG] Password input : '" + password + "'");
        System.out.println("[LOGIN DEBUG] Match? " + (stored != null && stored.equals(password)));
        if (stored != null && stored.equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Membuat user default admin jika koleksi "users" masih kosong.
     * Email: admin@university.edu  |  Password: admin123
     */
    public void ensureDefaultAdmin() {
        if (userDAO.findAll().isEmpty()) {
            User admin = new User("admin@university.edu", "admin123", "admin");
            admin.setNama("Administrator");
            userDAO.save(admin);
            System.out.println("Default admin user telah dibuat: admin@university.edu / admin123");
        }
    }

    /**
     * Jika belum ada satupun user mahasiswa, buat beberapa akun demo (password student123).
     * Password akun yang sudah ada tidak diubah — supaya login dengan kata sandi daftar tetap valid.
     */
    public void ensureDefaultUsers() {
        List<User> users = userDAO.findMany(Filters.eq("role", "user"));

        if (users.isEmpty()) {
            String[][] defaultStudents = {
                {"mahasiswa1@student.kampus.id", "Mahasiswa 1"},
                {"mahasiswa2@student.kampus.id", "Mahasiswa 2"},
                {"mahasiswa3@student.kampus.id", "Mahasiswa 3"}
            };

            for (int i = 0; i < defaultStudents.length; i++) {
                String jurusan = DaftarJurusan.PILIHAN[i % DaftarJurusan.PILIHAN.length];
                User u = new User(defaultStudents[i][0], DEFAULT_USER_PASSWORD, "user", jurusan, defaultStudents[i][1]);
                u.setNim("DEMO" + String.format("%03d", i + 1));
                userDAO.save(u);
                System.out.println("Default user dibuat: " + defaultStudents[i][0] + " / " + DEFAULT_USER_PASSWORD + " (" + jurusan + ")");
            }
        }
    }
}
