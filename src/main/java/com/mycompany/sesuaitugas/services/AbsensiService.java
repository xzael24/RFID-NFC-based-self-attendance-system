package com.mycompany.sesuaitugas.services;

import com.mongodb.client.model.Filters;
import com.mycompany.sesuaitugas.dao.GenericDAO;
import com.mycompany.sesuaitugas.objects.Absensi;
import com.mycompany.sesuaitugas.objects.Mahasiswa;
import com.mycompany.sesuaitugas.util.HashUtil;
import org.bson.types.ObjectId;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Service layer untuk logika absensi RFID.
 *
 * <p>
 * Alur utama:
 * <ol>
 *   <li>Arduino/reader kirim UID kartu via serial port.</li>
 *   <li>{@link #prosesUid(String)} menerima UID raw.</li>
 *   <li>UID di-hash SHA-256, dicari di koleksi {@code mahasiswa} (field {@code rfidHash}).</li>
 *   <li>Jika ditemukan, record {@link Absensi} disimpan ke koleksi {@code absensi}.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Untuk registrasi kartu, admin memanggil {@link #daftarkanRfid(String, String)}
 * yang menyimpan hash UID ke field {@code rfidHash} milik mahasiswa.
 * </p>
 */
public class AbsensiService {

    private final GenericDAO<Absensi>   absensiDAO;
    private final MahasiswaService      mahasiswaService;

    private static final SimpleDateFormat FMT_TANGGAL = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat FMT_WAKTU   = new SimpleDateFormat("HH:mm:ss");

    public AbsensiService() {
        this.absensiDAO      = new GenericDAO<>("absensi", Absensi.class);
        this.mahasiswaService = new MahasiswaService();
    }

    // ─── Proses scan ─────────────────────────────────────────────────────────

    /**
     * Memproses UID mentah dari reader RFID:
     * <ol>
     *   <li>Hash UID dengan SHA-256 deterministik (tanpa salt — supaya bisa dicari di DB).</li>
     *   <li>Cari mahasiswa yang rfidHash-nya cocok.</li>
     *   <li>Simpan record absensi ke MongoDB.</li>
     * </ol>
     *
     * @param uid UID mentah yang dikirim Arduino (misal {@code "A3F2B1C4"})
     * @return {@link HasilScan} berisi mahasiswa yang ditemukan dan record absensi,
     *         atau {@code null} jika UID tidak terdaftar
     */
    public HasilScan prosesUid(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return null;
        }

        String uidNorm = uid.trim().toUpperCase();

        // Hash UID deterministik (SHA-256 tanpa salt agar bisa dicari)
        String hashUid = HashUtil.hashDeterministic(uidNorm);

        // Cari mahasiswa berdasarkan rfidHash
        Mahasiswa mhs = mahasiswaService.findByRfidHash(hashUid);
        if (mhs == null) {
            // Fallback: buat dummy — setiap input UID dianggap berhasil
            mhs = new Mahasiswa(
                "UID-" + uidNorm,               // nim
                "UID-" + uidNorm,               // idMahasiswa
                "Pengguna RFID (" + uidNorm + ")", // nama
                "Umum",                          // jurusan
                "",                              // password (kosong, ga dipake)
                uidNorm + "@rfid.local"          // email dummy
            );
            mhs.setRfidHash(hashUid);
        }

        // Buat dan simpan record absensi
        Date now = new Date();
        Absensi absensi = new Absensi(
                mhs.getNim(),
                mhs.getNama(),
                mhs.getJurusan(),
                FMT_TANGGAL.format(now),
                FMT_WAKTU.format(now),
                "HADIR",
                now.getTime()
        );
        simpan(absensi);

        return new HasilScan(mhs, absensi);
    }

    /**
     * Memproses UID + RSA sign — overload dengan private key untuk signature digital.
     */
    public HasilScan prosesUid(String uid, java.security.PrivateKey privKey) {
        HasilScan hasil = prosesUid(uid);
        if (hasil != null && privKey != null && hasil.absensi != null) {
            Absensi a = hasil.absensi;
            String data = a.getNim() + "|" + a.getTanggal() + "|" + a.getWaktu();
            String sig = com.mycompany.sesuaitugas.util.CryptoUtil.rsaSign(data, privKey);
            a.setSignature(sig);
            absensiDAO.update(com.mongodb.client.model.Filters.eq("idAbsensi", a.getIdAbsensi()), a);
        }
        return hasil;
    }

    /**
     * Proses absensi manual + RSA sign.
     */
    public HasilScan prosesManual(Mahasiswa mhs, java.security.PrivateKey privKey) {
        HasilScan hasil = prosesManual(mhs);
        if (hasil != null && privKey != null && hasil.absensi != null) {
            Absensi a = hasil.absensi;
            String data = a.getNim() + "|" + a.getTanggal() + "|" + a.getWaktu();
            String sig = com.mycompany.sesuaitugas.util.CryptoUtil.rsaSign(data, privKey);
            a.setSignature(sig);
            absensiDAO.update(com.mongodb.client.model.Filters.eq("idAbsensi", a.getIdAbsensi()), a);
        }
        return hasil;
    }

    /**
     * Mendaftarkan kartu RFID
     * UID di-hash deterministik lalu disimpan ke field {@code rfidHash} mahasiswa.
     *
     * @param nim NIM mahasiswa yang akan didaftarkan kartunya
     * @param uid UID mentah kartu RFID
     * @return {@code true} jika berhasil, {@code false} jika NIM tidak ditemukan
     */
    public boolean daftarkanRfid(String nim, String uid) {
        if (nim == null || nim.trim().isEmpty()
                || uid == null || uid.trim().isEmpty()) {
            return false;
        }

        Mahasiswa mhs = mahasiswaService.findByNim(nim.trim());
        if (mhs == null) {
            return false;
        }

        String hashUid = HashUtil.hashDeterministic(uid.trim().toUpperCase());
        mhs.setRfidHash(hashUid);
        mahasiswaService.updateByNim(nim.trim(), mhs);
        return true;
    }

    // ─── Read absensi ─────────────────────────────────────────────────────────

    /** Ambil semua record absensi, terbaru di atas. */
    public List<Absensi> findAll() {
        return absensiDAO.findAll();
    }

    /** Ambil absensi berdasarkan NIM. */
    public List<Absensi> findByNim(String nim) {
        return absensiDAO.findMany(Filters.eq("nim", nim.trim()));
    }

    /** Ambil absensi berdasarkan tanggal (format yyyy-MM-dd). */
    public List<Absensi> findByTanggal(String tanggal) {
        return absensiDAO.findMany(Filters.eq("tanggal", tanggal.trim()));
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    /**
     * Expose MahasiswaService untuk digunakan GUI saat input manual NIM.
     */
    public MahasiswaService getMahasiswaService() {
        return mahasiswaService;
    }

    /**
     * Proses absensi manual berdasarkan objek Mahasiswa yang sudah ditemukan.
     * Digunakan saat mahasiswa input NIM secara manual (bukan tap kartu).
     *
     * @param mhs mahasiswa yang akan diabsen
     * @return {@link HasilScan} berisi mahasiswa dan record absensi yang tersimpan
     */
    public HasilScan prosesManual(Mahasiswa mhs) {
        if (mhs == null) return null;

        Date now = new Date();
        Absensi absensi = new Absensi(
                mhs.getNim(),
                mhs.getNama(),
                mhs.getJurusan(),
                FMT_TANGGAL.format(now),
                FMT_WAKTU.format(now),
                "HADIR",
                now.getTime()
        );
        simpan(absensi);
        return new HasilScan(mhs, absensi);
    }

    private void simpan(Absensi absensi) {
        // Gunakan MongoDB ObjectId (unik, embedded timestamp, no race condition)
        absensi.setIdAbsensi(ObjectId.get().toHexString());
        absensiDAO.save(absensi);
    }

    // ─── Inner result class ───────────────────────────────────────────────────

    /**
     * Hasil dari {@link #prosesUid(String)} — berisi mahasiswa dan record absensi
     * yang baru dibuat.
     */
    public static class HasilScan {
        public final Mahasiswa mahasiswa;
        public final Absensi   absensi;

        public HasilScan(Mahasiswa mahasiswa, Absensi absensi) {
            this.mahasiswa = mahasiswa;
            this.absensi   = absensi;
        }
    }
}
