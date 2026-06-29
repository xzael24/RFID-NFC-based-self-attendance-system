package com.mycompany.sesuaitugas.services;

import com.mycompany.sesuaitugas.dao.GenericDAO;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.util.CryptoUtil;
import com.mycompany.sesuaitugas.util.HashUtil;
import com.mongodb.client.model.Filters;
import java.util.List;

/**
 * CRUD mahasiswa pada koleksi {@code mahasiswa}.
 *
 * <p>
 * <b>Keamanan data:</b>
 * <ul>
 *   <li><b>Password</b> — di-hash 1 arah (SHA-256 + salt) sebelum disimpan.
 *       Tidak dapat dibaca balik; verifikasi dilakukan dengan {@link HashUtil#verify}.</li>
 *   <li><b>Email</b> — dienkripsi 2 arah (AES-256-GCM) sebelum disimpan.
 *       Dapat didekripsi kembali untuk ditampilkan di dashboard admin.</li>
 * </ul>
 * </p>
 */
public class MahasiswaService {

    private final GenericDAO<Mahasiswa> dao;

    public MahasiswaService() {
        this.dao = new GenericDAO<>("mahasiswa", Mahasiswa.class);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Mengembalikan semua mahasiswa dengan email sudah didekripsi
     * (siap ditampilkan di UI).
     */
    public List<Mahasiswa> findAll() {
        List<Mahasiswa> list = dao.findAll();
        list.forEach(this::decryptFields);
        return list;
    }

    /**
     * Cari mahasiswa berdasarkan NIM; email hasil query akan didekripsi.
     */
    public Mahasiswa findByNim(String nim) {
        if (nim == null || nim.trim().isEmpty()) {
            return null;
        }
        Mahasiswa m = dao.findOne(Filters.eq("nim", nim.trim()));
        if (m != null) {
            decryptFields(m);
        }
        return m;
    }

    /**
     * Cari mahasiswa berdasarkan email.
     * Email input dienkripsi terlebih dahulu sebelum query,
     * karena yang tersimpan di DB sudah dalam bentuk terenkripsi.
     *
     * <p>
     * Karena AES-GCM menggunakan IV acak per enkripsi, tidak bisa
     * langsung query ke MongoDB dengan email terenkripsi (IV berbeda tiap kali).
     * Solusinya: load semua, dekripsi satu per satu, lalu bandingkan.
     * Untuk skala besar sebaiknya gunakan deterministic encryption atau indeks
     * hash email terpisah.
     * </p>
     */
    /**
     * Cari mahasiswa berdasarkan hash UID RFID.
     * Query langsung ke MongoDB karena rfidHash bersifat deterministik.
     */
    public Mahasiswa findByRfidHash(String rfidHash) {
        if (rfidHash == null || rfidHash.trim().isEmpty()) {
            return null;
        }
        Mahasiswa m = dao.findOne(Filters.eq("rfidHash", rfidHash.trim()));
        if (m != null) {
            decryptFields(m);
        }
        return m;
    }

    public Mahasiswa findByEmail(String email) {        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        String emailNormalized = email.trim().toLowerCase();
        // Ambil semua, dekripsi, bandingkan
        List<Mahasiswa> all = dao.findAll();
        for (Mahasiswa m : all) {
            decryptFields(m);
            if (emailNormalized.equalsIgnoreCase(m.getEmail())) {
                return m;
            }
        }
        return null;
    }

    // ─── Write ────────────────────────────────────────────────────────────────

    /**
     * Simpan mahasiswa baru.
     * Password di-hash 1 arah; email dienkripsi 2 arah sebelum masuk DB.
     */
    public void save(Mahasiswa m) {
        if (m.getIdMahasiswa() == null || m.getIdMahasiswa().trim().isEmpty()) {
            m.setIdMahasiswa(String.valueOf(getNextIdMahasiswa()));
        }
        encryptFields(m);
        dao.save(m);
    }

    /**
     * Update berdasarkan NIM lama.
     * Password dan email juga diamankan ulang sebelum disimpan.
     */
    public void updateByNim(String nimLama, Mahasiswa dataBaru) {
        if (dataBaru.getIdMahasiswa() == null || dataBaru.getIdMahasiswa().trim().isEmpty()) {
            Mahasiswa old = dao.findOne(Filters.eq("nim", nimLama.trim()));
            if (old != null && old.getIdMahasiswa() != null && !old.getIdMahasiswa().trim().isEmpty()) {
                dataBaru.setIdMahasiswa(old.getIdMahasiswa());
            } else {
                dataBaru.setIdMahasiswa(String.valueOf(getNextIdMahasiswa()));
            }
        }
        encryptFields(dataBaru);
        dao.update(Filters.eq("nim", nimLama.trim()), dataBaru);
    }

    /** Hapus mahasiswa berdasarkan NIM. */
    public void deleteByNim(String nim) {
        dao.delete(Filters.eq("nim", nim.trim()));
    }

    // ─── Enkripsi / Dekripsi ──────────────────────────────────────────────────

    /**
     * Mengamankan field sensitif sebelum disimpan ke database:
     * <ul>
     *   <li>Password: hash 1 arah (SHA-256 + salt) — jika belum di-hash.</li>
     *   <li>Email: enkripsi 2 arah (AES-256-GCM) — jika belum terenkripsi.</li>
     * </ul>
     *
     * @param m entitas yang akan dimodifikasi in-place
     */
    private void encryptFields(Mahasiswa m) {
        // Hash password (1 arah) — hanya jika belum di-hash
        if (m.getPassword() != null && !m.getPassword().isEmpty()
                && !HashUtil.isHashed(m.getPassword())) {
            m.setPassword(HashUtil.hash(m.getPassword()));
        }

        // Enkripsi email (2 arah) — hanya jika belum terenkripsi
        if (m.getEmail() != null && !m.getEmail().isEmpty()
                && !CryptoUtil.isEncrypted(m.getEmail())) {
            m.setEmail(CryptoUtil.encrypt(m.getEmail()));
        }
    }

    /**
     * Mendekripsi field-field yang terenkripsi agar dapat ditampilkan di UI.
     * Password tidak didekripsi (1 arah — tidak bisa).
     *
     * @param m entitas yang akan dimodifikasi in-place
     */
    private void decryptFields(Mahasiswa m) {
        // Dekripsi email (2 arah)
        if (m.getEmail() != null && !m.getEmail().isEmpty()) {
            m.setEmail(CryptoUtil.decrypt(m.getEmail()));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Verifikasi password input terhadap hash yang tersimpan di DB.
     * Digunakan oleh {@link LoginService} untuk autentikasi mahasiswa.
     *
     * @param inputPassword password plaintext dari form login
     * @param storedHash    hash yang tersimpan di database
     * @return {@code true} jika cocok
     */
    public boolean verifyPassword(String inputPassword, String storedHash) {
        return HashUtil.verify(inputPassword, storedHash);
    }

    private int getNextIdMahasiswa() {
        List<Mahasiswa> all = dao.findAll();
        int maxId = 0;
        for (Mahasiswa m : all) {
            try {
                int current = Integer.parseInt(m.getIdMahasiswa());
                if (current > maxId) {
                    maxId = current;
                }
            } catch (NumberFormatException e) {
                // skip non-numeric IDs
            }
        }
        return maxId + 1;
    }
}
