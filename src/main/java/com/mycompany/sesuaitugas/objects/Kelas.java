package com.mycompany.sesuaitugas.objects;

/**
 * Kelas / mata kuliah (koleksi {@code kelas}).
 */
public class Kelas {

    private String kodeKelas;
    private String namaMataKuliah;
    private int sks;
    private String dosen;
    private String ruangan;

    public Kelas() {
    }

    public Kelas(String kodeKelas, String namaMataKuliah, int sks, String dosen, String ruangan) {
        this.kodeKelas = kodeKelas;
        this.namaMataKuliah = namaMataKuliah;
        this.sks = sks;
        this.dosen = dosen;
        this.ruangan = ruangan;
    }

    public String getKodeKelas() {
        return kodeKelas;
    }

    public void setKodeKelas(String kodeKelas) {
        this.kodeKelas = kodeKelas;
    }

    public String getNamaMataKuliah() {
        return namaMataKuliah;
    }

    public void setNamaMataKuliah(String namaMataKuliah) {
        this.namaMataKuliah = namaMataKuliah;
    }

    public int getSks() {
        return sks;
    }

    public void setSks(int sks) {
        this.sks = sks;
    }

    public String getDosen() {
        return dosen;
    }

    public void setDosen(String dosen) {
        this.dosen = dosen;
    }

    public String getRuangan() {
        return ruangan;
    }

    public void setRuangan(String ruangan) {
        this.ruangan = ruangan;
    }
}
