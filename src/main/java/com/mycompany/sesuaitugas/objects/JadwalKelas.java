package com.mycompany.sesuaitugas.objects;

/**
 * Slot jadwal untuk satu kelas (koleksi {@code jadwal_kelas}).
 */
public class JadwalKelas {

    /** ID unik dokumen (UUID). */
    private String id;
    private String kodeKelas;
    private String hari;
    private String jamMulai;
    private String jamSelesai;
    private String ruangan;

    public JadwalKelas() {
    }

    public JadwalKelas(String id, String kodeKelas, String hari, String jamMulai, String jamSelesai, String ruangan) {
        this.id = id;
        this.kodeKelas = kodeKelas;
        this.hari = hari;
        this.jamMulai = jamMulai;
        this.jamSelesai = jamSelesai;
        this.ruangan = ruangan;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKodeKelas() {
        return kodeKelas;
    }

    public void setKodeKelas(String kodeKelas) {
        this.kodeKelas = kodeKelas;
    }

    public String getHari() {
        return hari;
    }

    public void setHari(String hari) {
        this.hari = hari;
    }

    public String getJamMulai() {
        return jamMulai;
    }

    public void setJamMulai(String jamMulai) {
        this.jamMulai = jamMulai;
    }

    public String getJamSelesai() {
        return jamSelesai;
    }

    public void setJamSelesai(String jamSelesai) {
        this.jamSelesai = jamSelesai;
    }

    public String getRuangan() {
        return ruangan;
    }

    public void setRuangan(String ruangan) {
        this.ruangan = ruangan;
    }
}
