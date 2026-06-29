package com.mycompany.sesuaitugas.objects;

import com.mycompany.sesuaitugas.dao.Identifiable;
import org.bson.codecs.pojo.annotations.BsonIgnore;

/**
 * Record absensi satu kali tap kartu RFID.
 * Disimpan di koleksi {@code absensi} pada MongoDB.
 *
 * <p>Setiap tap kartu yang berhasil diidentifikasi menghasilkan
 * satu dokumen Absensi yang menyimpan NIM, nama, waktu tap,
 * dan status kehadiran.</p>
 */
public class Absensi implements Identifiable {

    /** ID unik record ini (auto-generated berupa string angka incremental). */
    private String idAbsensi;
    private String nim;
    private String nama;
    private String jurusan;
    /** Tanggal dalam format yyyy-MM-dd, misal "2026-06-15". */
    private String tanggal;
    /** Waktu dalam format HH:mm:ss, misal "08:03:21". */
    private String waktu;
    /** Status kehadiran, saat ini selalu "HADIR". */
    private String status;
    /** Timestamp Unix (milidetik) untuk pengurutan. */
    private long timestamp;
    /** RSA digital signature dari data absensi (nim+tanggal+waktu) — null jika belum ditandatangani. */
    private String signature;

    public Absensi() {
    }

    public Absensi(String nim, String nama, String jurusan,
                   String tanggal, String waktu, String status, long timestamp) {
        this.nim       = nim;
        this.nama      = nama;
        this.jurusan   = jurusan;
        this.tanggal   = tanggal;
        this.waktu     = waktu;
        this.status    = status;
        this.timestamp = timestamp;
    }

    @BsonIgnore
    @Override
    public String getId() {
        return idAbsensi;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public String getIdAbsensi() { return idAbsensi; }
    public void setIdAbsensi(String idAbsensi) { this.idAbsensi = idAbsensi; }

    public String getNim()  { return nim; }
    public void setNim(String nim) { this.nim = nim; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getJurusan() { return jurusan; }
    public void setJurusan(String jurusan) { this.jurusan = jurusan; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public String getWaktu() { return waktu; }
    public void setWaktu(String waktu) { this.waktu = waktu; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
